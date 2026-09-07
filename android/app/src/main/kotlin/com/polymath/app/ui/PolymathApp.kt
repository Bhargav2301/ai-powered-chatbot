package com.polymath.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polymath.app.*
import com.polymath.data.*
import com.polymath.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Destination(val label: String, val icon: ImageVector) {
    Discover("Discover", Icons.Outlined.Layers), Graph("Graph", Icons.Outlined.Hub),
    Notebook("Notebook", Icons.Outlined.EditNote), Vault("Vault", Icons.Outlined.Bookmarks), Desk("Desk", Icons.Outlined.SpaceDashboard)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun PolymathApp(vm: FolioViewModel, request: LaunchRequest) {
    val state by vm.ui.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(Destination.Discover) }
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    var capture by remember { mutableStateOf<NoteEntity?>(null) }
    var noteId by rememberSaveable { mutableStateOf<String?>(null) }
    var ask by rememberSaveable { mutableStateOf(false) }
    var reviewId by rememberSaveable { mutableStateOf<String?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun draft(mode: NoteMode = NoteMode.THOUGHT, text: String = "", card: Card? = null) = NoteEntity(
        FolioRepository.uuid(), mode, "", text, topicId = card?.topicId ?: "systems", sourceContentId = card?.id,
        createdAt = now, updatedAt = now)
    LaunchedEffect(Unit) {
        vm.messages.collect { notice ->
            scope.launch {
                if (notice.undoAction != null) {
                    val timer = launch { delay(5_000); snackbar.currentSnackbarData?.dismiss() }
                    val result = snackbar.showSnackbar(notice.message, "Undo", duration = SnackbarDuration.Indefinite)
                    timer.cancel()
                    if (result == SnackbarResult.ActionPerformed) vm.undo(notice.undoAction)
                } else snackbar.showSnackbar(notice.message)
            }
        }
    }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = System.currentTimeMillis() } }
    LaunchedEffect(request.token, state.ready) {
        if (state.ready) {
            request.cardId?.let { expandedId = it }
            if (request.sharedText != null || request.captureMode != null) capture = draft(
                runCatching { NoteMode.valueOf(request.captureMode ?: "THOUGHT") }.getOrDefault(NoteMode.THOUGHT), request.sharedText ?: "")
        }
    }
    LaunchedEffect(state.settings.liveNews, state.ready) { if (state.ready) NewsRefreshWorker.configure(context, state.settings.liveNews) }
    if (!state.ready) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (!state.settings.onboarded) {
        Onboarding(onContinue = vm::onboard)
        return
    }
    Scaffold(
        containerColor = CanvasColor,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = CanvasColor) {
                Destination.entries.forEach { tab -> NavigationBarItem(selected = destination == tab,
                    onClick = { destination = tab }, icon = { Icon(tab.icon, contentDescription = null) },
                    label = { Text(tab.label, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = SurfaceColor, selectedIconColor = Sage)) }
            }
        },
        floatingActionButton = {
            if (destination != Destination.Discover) FloatingActionButton(onClick = { capture = draft() }, containerColor = Sage) {
                Icon(Icons.Outlined.Add, "Capture a note")
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = 720.dp).fillMaxSize()) {
                when (destination) {
                    Destination.Discover -> DiscoverScreen(state.folio, state.settings, refreshing, now,
                        open = { expandedId = it.id }, judge = vm::judge,
                        capture = { capture = draft() }, refresh = vm::refresh,
                        enableNews = { vm.live(true) {} }, review = { reviewId = it.id })
                    Destination.Graph -> GraphScreen(state.folio, now, review = { reviewId = it.id })
                    Destination.Notebook -> NotebookScreen(state.folio.notes, open = { noteId = it.id }, capture = { capture = draft(it) })
                    Destination.Vault -> VaultScreen(query, { vm.query.value = it }, results, state.folio,
                        open = { result -> if (result.resourceKey.startsWith("note:")) noteId = result.resourceId else expandedId = result.resourceId },
                        removeSave = vm::removeSave, ask = { ask = true })
                    Destination.Desk -> DeskScreen(state.folio, state.settings, refreshing, now,
                        onLive = { enabled -> vm.live(enabled) {} }, refresh = vm::refresh,
                        preference = vm::preference, openNote = { noteId = it }, ask = { ask = true })
                }
            }
        }
    }
    state.folio.cards.firstOrNull { it.id == expandedId }?.let { card ->
        ReaderSheet(card, state.folio, now, close = { expandedId = null }, save = { vm.save(card) },
            capture = { expandedId = null; capture = draft(NoteMode.IDEA, card.title, card) },
            review = { expandedId = null; reviewId = card.id })
    }
    capture?.let { note -> CaptureSheet(note, close = { capture = null }, save = { edited ->
        vm.saveNote(edited) { capture = null; noteId = it.id; destination = Destination.Notebook }
    }) }
    state.folio.notes.firstOrNull { it.id == noteId }?.let { note ->
        NoteSheet(note, state.folio, close = { noteId = null }, edit = { noteId = null; capture = note },
            delete = { vm.deleteNote(note.id); noteId = null }, makePlan = { vm.plan(note.id) },
            acceptPlan = vm::acceptPlan, discardPlan = vm::discardPlan, completeTask = vm::completeTask, reopenTask = vm::reopenTask)
    }
    state.folio.cards.firstOrNull { it.id == reviewId }?.let { card ->
        RecallSheet(card, state.folio.reviews.firstOrNull { it.contentId == card.id }, close = { reviewId = null },
            answer = { selected, episode, result -> vm.answer(card, selected, episode, result) })
    }
    if (ask) EvidenceSheet(vm, close = { ask = false })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun Onboarding(onContinue: (Set<String>) -> Unit) {
    var selected by rememberSaveable { mutableStateOf(listOf<String>()) }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Spacer(Modifier.height(28.dp))
        Eyebrow("POLYMATH / YOUR PRIVATE FOLIO")
        Text("Follow your curiosity.\nBuild something with it.", style = MaterialTheme.typography.displaySmall)
        Text("Discover a useful idea, keep the source, and turn what you learn into a next step.", style = MaterialTheme.typography.bodyLarge, color = Muted)
        HorizontalDivider(color = BorderColor)
        Text("What should your folio follow?", style = MaterialTheme.typography.titleLarge)
        Text("Choose a few topics. You can change or mute them later.", color = Muted)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Topics.all.forEach { topic -> FilterChip(selected = topic.id in selected,
                onClick = { selected = if (topic.id in selected) selected - topic.id else selected + topic.id },
                label = { Text(topic.title) }, modifier = Modifier.heightIn(min = 48.dp)) }
        }
        Spacer(Modifier.weight(1f, fill = false))
        Text("Your notes stay on this device. No account is required.", color = Muted, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = { onContinue(selected.toSet()) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(if (selected.isEmpty()) "Explore all topics" else "Open my folio · ${selected.size} topics")
        }
    }
}

@Composable fun Eyebrow(text: String, modifier: Modifier = Modifier) = Text(text, modifier, color = Muted, style = MaterialTheme.typography.labelSmall)
@Composable fun PageHeading(eyebrow: String, title: String, trailing: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) { Eyebrow(eyebrow); Text(title, style = MaterialTheme.typography.headlineMedium) }
        trailing?.invoke()
    }
}
@Composable fun FolioPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(24.dp), color = SurfaceColor, border = BorderStroke(1.dp, BorderColor)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
@Composable fun EmptyPanel(title: String, body: String, action: @Composable (() -> Unit)? = null) {
    FolioPanel(Modifier.fillMaxWidth()) { Text(title, style = MaterialTheme.typography.titleLarge); Text(body, color = Muted); action?.invoke() }
}
