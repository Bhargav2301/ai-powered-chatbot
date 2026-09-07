package com.polymath.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polymath.data.*
import com.polymath.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class AppState(val folio: FolioSnapshot = FolioSnapshot(), val settings: Settings = Settings(), val ready: Boolean = false)
data class Notice(val message: String, val undoAction: String? = null)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class FolioViewModel @Inject constructor(val repository: FolioRepository, private val settings: UserSettings,
    private val news: NewsFetcher, private val rag: RagClient, private val secret: ServiceSecret) : ViewModel() {
    val datasetMessage = MutableStateFlow<String?>(null)
    val connectionMessage = MutableStateFlow<String?>(null)
    val chatScope = MutableStateFlow("vault")
    val chatBusy = MutableStateFlow(false)
    val chatStatus = MutableStateFlow("")
    val chatError = MutableStateFlow<String?>(null)
    private var chatJob: Job? = null
    private var lastQuestion = ""
    private var chatGeneration = 0
    private val initialized = MutableStateFlow(false)
    private val notices = Channel<Notice>(Channel.BUFFERED)
    val messages = notices.receiveAsFlow()
    val refreshing = MutableStateFlow(false)
    val query = MutableStateFlow("")
    val ui = combine(repository.snapshots, settings.values, initialized) { folio, settings, ready -> AppState(folio, settings, ready) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppState())
    val results = combine(repository.snapshots, query.debounce(150)) { _, text -> text }
        .mapLatest { repository.search(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init { action { repository.initialize(); initialized.value = true } }
    fun onboard(topics: Set<String>) = action { repository.preferences(topics); settings.finishOnboarding() }
    fun preference(topic: String, followed: Boolean, muted: Boolean) = action { repository.preference(topic, followed, muted) }
    fun judge(card: Card, judgment: Judgment) = action {
        val id = FolioRepository.uuid()
        if (repository.judge(card.id, judgment, id)) notices.send(Notice(if (judgment == Judgment.SAVE) "Saved to your vault" else "Dismissed", id))
    }
    fun undo(id: String) = action { if (!repository.undo(id)) notices.send(Notice("The undo window has closed.")) }
    fun save(card: Card) = action { repository.save(card.id); notices.send(Notice("Saved to your vault")) }
    fun removeSave(id: String) = action { repository.removeSave(id); notices.send(Notice("Removed from your vault")) }
    fun saveNote(note: NoteEntity, saved: (NoteEntity) -> Unit) = action { saved(repository.saveNote(note)) }
    fun deleteNote(id: String) = action { repository.deleteNote(id) }
    fun plan(noteId: String) = action { repository.makePlan(noteId) }
    fun acceptPlan(id: String) = action { repository.acceptPlan(id) }
    fun discardPlan(id: String) = action { repository.discardDraft(id) }
    fun completeTask(id: String, reflection: String, done: () -> Unit) = action { repository.completeTask(id, reflection); done() }
    fun reopenTask(id: String) = action { repository.reopenTask(id) }
    fun answer(card: Card, selected: Int, episode: Int, result: (AttemptEntity) -> Unit) = action {
        result(repository.answer(card.id, selected, episode))
    }
    fun live(enabled: Boolean, configured: () -> Unit) = action { settings.liveNews(enabled); configured(); if (enabled) refresh() }
    fun refresh() {
        if (refreshing.value) return
        action {
            refreshing.value = true
            try { settings.refreshed(news.refresh()) } finally { refreshing.value = false }
        }
    }
    fun configureAi(endpoint: String, apiKey: String, enabled: Boolean) = viewModelScope.launch {
        connectionMessage.value = null
        try {
            cancelChat()
            val validated = if (enabled) rag.endpoint(endpoint) else endpoint
            withContext(Dispatchers.IO) {
                if (enabled) {
                    require(apiKey.length >= 24 || secret.read().length >= 24) { "Enter the service's API key (at least 24 characters)." }
                    if (apiKey.isNotBlank()) secret.save(apiKey)
                } else secret.save("")
            }
            settings.aiConnection(validated, enabled)
            connectionMessage.value = if (enabled) "Connection saved. Return to chat to ask your sources." else "Disconnected; the stored service key was removed."
        } catch (e: CancellationException) { throw e }
          catch (e: Exception) { connectionMessage.value = e.message?.take(220) ?: "Could not save this connection. Please retry." }
    }
    fun selectScope(scope: String) { cancelChat(); chatScope.value = scope; chatError.value = null; lastQuestion = "" }
    fun cancelChat() { chatGeneration++; chatJob?.cancel(); chatBusy.value = false; chatStatus.value = "" }
    fun clearChat() = action { cancelChat(); repository.clearChat(chatScope.value); lastQuestion = "" }
    fun importDataset(json: String) = viewModelScope.launch {
        datasetMessage.value = null
        try {
            val parsed = withContext(Dispatchers.Default) { DatasetParser.parse(json) }
            repository.importDataset(parsed)
            datasetMessage.value = "Imported ${parsed.cards.size} sources into ${parsed.dataset.title}"
        } catch (e: CancellationException) { throw e }
          catch (e: Exception) { datasetMessage.value = e.message?.take(220) ?: "The dataset could not be imported." }
    }
    fun deleteDataset(id: String) = action {
        cancelChat(); repository.deleteDataset(id)
        if (chatScope.value == id) chatScope.value = "vault"
    }
    fun reportError(message: String) { datasetMessage.value = message }
    fun retryChat() { if (lastQuestion.isNotBlank()) sendChat(lastQuestion, retry = true) }
    fun sendChat(question: String, retry: Boolean = false) {
        if (chatBusy.value || question.isBlank()) return
        val scope = chatScope.value
        lastQuestion = question.trim()
        chatError.value = null; chatBusy.value = true; chatStatus.value = "Preparing selected sources"
        val generation = ++chatGeneration
        chatJob = viewModelScope.launch {
            var watcher: Job? = null
            try {
                val connection = settings.values.first()
                require(connection.aiEnabled) { "Open Connection and configure your private AI service first." }
                val snapshot = repository.snapshot()
                val documents = RagCorpus.documents(snapshot, scope)
                val previous = snapshot.chat.filter { it.scope == scope && it.role == "user" }.map { it.text }
                val payload = RagCorpus.request(question, scope, documents, if (retry) previous.dropLast(1) else previous)
                if (!retry) repository.addMessage(ChatMessageEntity(FolioRepository.uuid(), scope, "user", question.trim(), createdAt = System.currentTimeMillis()))
                // A source edit/removal invalidates in-flight context, not merely the next request.
                val job = coroutineContext[Job]!!
                watcher = launch {
                    repository.snapshots.collect { current ->
                        if (runCatching { RagCorpus.documents(current, scope) }.getOrNull() != documents) {
                            chatError.value = "The selected sources changed. Send the question again to use current evidence."
                            job.cancel()
                        }
                    }
                }
                val reply = rag.answer(connection.aiEndpoint, withContext(Dispatchers.IO) { secret.read() }, payload, documents) { if (generation == chatGeneration) chatStatus.value = it }
                require(RagCorpus.documents(repository.snapshot(), scope) == documents) { "Sources changed. Please retry." }
                repository.addMessage(ChatMessageEntity(FolioRepository.uuid(), scope, "assistant", reply.text,
                    reply.citations, reply.status, System.currentTimeMillis()))
            } catch (e: CancellationException) { throw e }
              catch (e: Exception) { if (generation == chatGeneration) chatError.value = e.message?.take(220) ?: "The AI request failed. Please retry." }
            finally { watcher?.cancel(); if (generation == chatGeneration) { chatBusy.value = false; chatStatus.value = "" } }
        }
    }
    private fun action(block: suspend () -> Unit) = viewModelScope.launch {
        try { block() } catch (e: CancellationException) { throw e }
        catch (e: Exception) { notices.send(Notice(e.message?.take(180) ?: "That action could not finish. Try again.")) }
    }
}
