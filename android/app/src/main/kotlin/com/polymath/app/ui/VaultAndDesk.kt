package com.polymath.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.polymath.app.FolioViewModel
import com.polymath.app.widgets.*
import com.polymath.data.*
import com.polymath.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable fun VaultScreen(query: String, onQuery: (String) -> Unit, results: List<SearchDocument>, folio: FolioSnapshot,
    open: (SearchDocument) -> Unit, removeSave: (String) -> Unit, ask: () -> Unit) {
    var filter by rememberSaveable { mutableStateOf("All") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageHeading("YOUR SECOND BRAIN", "The vault") { IconButton(ask) { Icon(Icons.Outlined.ChatBubbleOutline, "Ask your vault") } } }
        item { Text("${folio.saves.size} saved sources · ${folio.notes.size} notes", color = Muted) }
        item { OutlinedTextField(query, onQuery, Modifier.fillMaxWidth(), label = { Text("Search your words") }, singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Search, null) }, trailingIcon = { if (query.isNotBlank()) IconButton({ onQuery("") }) { Icon(Icons.Outlined.Close, "Clear search") } }) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("All", "News", "Pills", "Notes").forEach { value -> FilterChip(filter == value, { filter = value }, label = { Text(value) }) } } }
        val visible = results.filter { when (filter) { "News" -> it.kind == "NEWS"; "Pills" -> it.kind == "KNOWLEDGE_PILL"; "Notes" -> it.resourceKey.startsWith("note:"); else -> true } }
        if (visible.isEmpty()) item { EmptyPanel(if (query.isBlank()) "Keep something worth returning to." else "No matching words yet.",
            if (query.isBlank()) "Save a card or capture a note. Both will appear here." else "Try a shorter keyword or a different phrase from your notes.") }
        items(visible, key = { it.resourceKey }) { result ->
            FolioPanel(Modifier.fillMaxWidth().clickable { open(result) }) {
                Eyebrow("${result.kind.replace('_', ' ')} / ${Topics.title(result.topicId).uppercase()}")
                Text(result.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(result.body, color = Muted, maxLines = 3, overflow = TextOverflow.Ellipsis)
                if (result.resourceKey.startsWith("content:")) TextButton({ removeSave(result.resourceId) }) { Text("Remove from vault") }
            }
        }
    }
}

@Composable fun DeskScreen(folio: FolioSnapshot, settings: Settings, refreshing: Boolean, now: Long,
    onLive: (Boolean) -> Unit, refresh: () -> Unit, preference: (String, Boolean, Boolean) -> Unit,
    openNote: (String) -> Unit, ask: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var widgetMessage by remember { mutableStateOf<String?>(null) }
    val activePlans = folio.plans.filter { it.status == "ACTIVE" }
    val next = folio.tasks.firstOrNull { task -> !task.done && activePlans.any { it.id == task.planId } &&
        (task.dependencyId == null || folio.tasks.any { it.id == task.dependencyId && it.done }) }
    fun pin(pill: Boolean) {
        scope.launch {
            val manager = GlanceAppWidgetManager(context)
            val requested = if (pill) manager.requestPinGlanceAppWidget(PillWidgetReceiver::class.java) else manager.requestPinGlanceAppWidget(CaptureWidgetReceiver::class.java)
            widgetMessage = if (requested) "Choose where to place the widget." else "Your launcher does not support pinning here. Add it from the home-screen widget picker."
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { PageHeading("A LITTLE LEARNING, APPLIED", "Your desk") { IconButton(ask) { Icon(Icons.Outlined.ChatBubbleOutline, "Open AI chat") } } }
        item { FolioPanel(Modifier.fillMaxWidth()) {
            Eyebrow("TODAY'S REFLECTION")
            Text("What could you try with what you already know?", style = MaterialTheme.typography.headlineMedium)
            Text("Choose one idea from your vault and make its next step small enough to finish.", color = Muted)
        } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FolioPanel(Modifier.weight(1f)) { Eyebrow("TOTAL EXP"); Text("${folio.totalExp}", style = MaterialTheme.typography.headlineLarge, color = Sage) }
            FolioPanel(Modifier.weight(1f)) { Eyebrow("RECALLS DUE"); Text("${folio.reviews.count { it.dueAt <= now }}", style = MaterialTheme.typography.headlineLarge) }
        } }
        item { FolioPanel(Modifier.fillMaxWidth()) {
            Eyebrow("NEXT ACTION")
            Text(next?.title ?: "Give an idea a first move.", style = MaterialTheme.typography.titleLarge)
            Text(next?.acceptance ?: "Capture an idea in your notebook, draft a plan, and accept the steps you want to try.", color = Muted)
            if (next != null) Button({ openNote(activePlans.first { it.id == next.planId }.noteId) }) { Text("Open project") }
        } }
        item { FolioPanel(Modifier.fillMaxWidth()) {
            Eyebrow("ON YOUR HOME SCREEN")
            Text("A useful moment, within reach.", style = MaterialTheme.typography.titleLarge)
            Text("Add a public knowledge pill or shortcuts to the three capture modes.", color = Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton({ pin(true) }) { Text("One pill") }; OutlinedButton({ pin(false) }) { Text("Quick capture") } }
            widgetMessage?.let { Text(it, color = Sage) }
            Text("Lock-screen placement depends on your device's widget host.", color = Muted, style = MaterialTheme.typography.bodySmall)
        } }
        item { FolioPanel(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) { Text("Fetch news", style = MaterialTheme.typography.titleLarge); Text("Public RSS feeds only", color = Muted) }
                Switch(settings.liveNews, onLive)
            }
            Text(settings.refreshMessage, color = Muted)
            if (settings.lastRefresh > 0) Text("Last checked ${formatDate(settings.lastRefresh)}", color = Muted, style = MaterialTheme.typography.bodySmall)
            if (settings.liveNews) OutlinedButton(refresh, enabled = !refreshing) { Text(if (refreshing) "Refreshing…" else "Refresh now") }
        } }
        item { PageHeading("YOU SET THE DIRECTION", "Your interests") }
        items(Topics.all, key = { it.id }) { topic ->
            val pref = folio.preferences.firstOrNull { it.topicId == topic.id } ?: Preference(topic.id, false)
            FolioPanel(Modifier.fillMaxWidth()) {
                Text(topic.title, style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(pref.followed, { preference(topic.id, !pref.followed, false) }, label = { Text("Follow") })
                    FilterChip(pref.muted, { preference(topic.id, false, !pref.muted) }, label = { Text("Mute") })
                }
            }
        }
        item { FolioPanel(Modifier.fillMaxWidth()) {
            Eyebrow("PRIVATE BY DEFAULT")
            Text("Your folio lives on this device.", style = MaterialTheme.typography.titleLarge)
            Text("No account or analytics. AI is optional: only the selected scope is sent to a service you configure. Uninstalling removes your local folio. Export and restore are planned.", color = Muted)
            OutlinedButton(ask) { Text("Chat with my sources") }
            Text("Version 0.2 · Keyword search works offline. Semantic retrieval and cited answers use your configured open-source AI service.", color = Muted, style = MaterialTheme.typography.bodySmall)
        } }
    }
}

