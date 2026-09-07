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
    private val news: NewsFetcher) : ViewModel() {
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
    private fun action(block: suspend () -> Unit) = viewModelScope.launch {
        try { block() } catch (e: CancellationException) { throw e }
        catch (e: Exception) { notices.send(Notice(e.message?.take(180) ?: "That action could not finish. Try again.")) }
    }
}
