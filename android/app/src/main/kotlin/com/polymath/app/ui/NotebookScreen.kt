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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.polymath.data.*
import com.polymath.model.*

@Composable fun NotebookScreen(notes: List<NoteEntity>, open: (NoteEntity) -> Unit, capture: (NoteMode) -> Unit) {
    var mode by rememberSaveable { mutableStateOf<NoteMode?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageHeading("CAPTURE / CONNECT / APPLY", "Your notebook") }
        item { Text("Give a thought somewhere to grow.", style = MaterialTheme.typography.bodyLarge, color = Muted) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(mode == null, { mode = null }, label = { Text("All") })
            NoteMode.entries.forEach { value -> FilterChip(mode == value, { mode = value }, label = { Text(value.label) }) }
        } }
        if (notes.none { mode == null || it.mode == mode }) item {
            EmptyPanel("Start with a small thought.", "Capture a thought, investigate a question, or give an idea a plan.") {
                NoteMode.entries.forEach { value -> OutlinedButton({ capture(value) }, Modifier.fillMaxWidth()) { Text("New ${value.label.lowercase()}") } }
            }
        }
        items(notes.filter { mode == null || it.mode == mode }, key = { it.id }) { note ->
            FolioPanel(Modifier.fillMaxWidth().clickable { open(note) }) {
                Eyebrow("${note.mode.label.uppercase()} / ${Topics.title(note.topicId).uppercase()}")
                Text(note.title.ifBlank { note.body.take(80) }, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (note.title.isNotBlank()) Text(note.body, color = Muted, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text("Edited ${formatDate(note.updatedAt)}", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable fun CaptureSheet(note: NoteEntity, close: () -> Unit, save: (NoteEntity) -> Unit) {
    var mode by rememberSaveable(note.id) { mutableStateOf(note.mode) }
    var title by rememberSaveable(note.id) { mutableStateOf(note.title) }
    var body by rememberSaveable(note.id) { mutableStateOf(note.body) }
    var goal by rememberSaveable(note.id) { mutableStateOf(note.goal) }
    var context by rememberSaveable(note.id) { mutableStateOf(note.context) }
    var topic by rememberSaveable(note.id) { mutableStateOf(note.topicId) }
    var confirmClose by remember { mutableStateOf(false) }
    val changed = mode != note.mode || title != note.title || body != note.body || goal != note.goal || context != note.context || topic != note.topicId
    fun dismiss() { if (changed) confirmClose = true else close() }
    ModalBottomSheet(::dismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = CanvasColor) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PageHeading("A PLACE FOR THE UNFINISHED", "Capture")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { NoteMode.entries.forEach { value -> FilterChip(mode == value, { mode = value }, label = { Text(value.label) }) } }
            Text(when (mode) { NoteMode.THOUGHT -> "A quick observation. Give it words before it slips away."
                NoteMode.RESEARCH_TOPIC -> "Start with a question and keep evidence attached."
                NoteMode.IDEA -> "Name a useful outcome. Build a plan when you are ready." }, color = Muted)
            OutlinedTextField(title, { title = it.take(200) }, label = { Text(if (mode == NoteMode.RESEARCH_TOPIC) "Research question" else "Title (optional)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(body, { body = it.take(20_000) }, label = { Text(when (mode) { NoteMode.THOUGHT -> "What's on your mind?"; NoteMode.RESEARCH_TOPIC -> "What do you know so far?"; NoteMode.IDEA -> "The problem or idea" }) },
                modifier = Modifier.fillMaxWidth(), minLines = 5, maxLines = 10)
            if (mode != NoteMode.THOUGHT || goal.isNotBlank() || context.isNotBlank()) {
                OutlinedTextField(goal, { goal = it.take(2_000) }, label = { Text(if (mode == NoteMode.RESEARCH_TOPIC) "Hypothesis or question to test" else "Desired outcome") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(context, { context = it.take(4_000) }, label = { Text(if (mode == NoteMode.RESEARCH_TOPIC) "Evidence, sources, and open questions" else "Audience and constraints") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
            Eyebrow("TOPIC")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Topics.all.forEach { value -> FilterChip(topic == value.id, { topic = value.id }, label = { Text(value.title) }) } }
            if (note.sourceContentId != null) Text("Linked to the source card you captured.", color = Sage)
            Button({ save(note.copy(mode = mode, title = title.trim(), body = body.trim(), goal = goal.trim(), context = context.trim(), topicId = topic)) },
                enabled = title.isNotBlank() || body.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Save ${mode.label.lowercase()}") }
            Text("Saved on your device. Research does not run automatically.", color = Muted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
        }
    }
    if (confirmClose) AlertDialog(onDismissRequest = { confirmClose = false }, title = { Text("Discard this edit?") },
        text = { Text("Your changes have not been saved.") }, confirmButton = { TextButton(close) { Text("Discard") } },
        dismissButton = { TextButton({ confirmClose = false }) { Text("Keep writing") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun NoteSheet(note: NoteEntity, folio: FolioSnapshot, close: () -> Unit, edit: () -> Unit, delete: () -> Unit,
    makePlan: () -> Unit, acceptPlan: (String) -> Unit, discardPlan: (String) -> Unit,
    completeTask: (String, String, () -> Unit) -> Unit, reopenTask: (String) -> Unit) {
    var deleting by remember { mutableStateOf(false) }
    var completing by remember { mutableStateOf<TaskEntity?>(null) }
    val plan = folio.plans.firstOrNull { it.noteId == note.id }
    val tasks = folio.tasks.filter { it.planId == plan?.id }
    ModalBottomSheet(close, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = CanvasColor) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            PageHeading("${note.mode.label.uppercase()} / REVISION ${note.revision}", note.title.ifBlank { "An unfinished thought" })
            Text(note.body, style = MaterialTheme.typography.bodyLarge)
            if (note.goal.isNotBlank()) FolioPanel { Eyebrow(if (note.mode == NoteMode.RESEARCH_TOPIC) "HYPOTHESIS" else "DESIRED OUTCOME"); Text(note.goal) }
            if (note.context.isNotBlank()) FolioPanel { Eyebrow("CONTEXT & EVIDENCE"); Text(note.context) }
            note.sourceContentId?.let { id -> folio.cards.firstOrNull { it.id == id }?.let { source ->
                FolioPanel { Eyebrow("SOURCE CONNECTION"); Text(source.title, style = MaterialTheme.typography.titleLarge); Text(source.publisher, color = Muted) }
            } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(edit) { Text("Edit") }; TextButton({ deleting = true }) { Text("Delete") } }
            if (note.mode == NoteMode.IDEA && plan == null) FolioPanel {
                Text("Give the idea a first move.", style = MaterialTheme.typography.titleLarge)
                Text("Create a four-step planning template, then review it before starting.", color = Muted)
                Button(makePlan) { Text("Draft a plan") }
            }
            if (plan != null) {
                HorizontalDivider(color = BorderColor)
                Eyebrow("IMPLEMENTATION / ${plan.status}")
                Text(if (plan.status == "DRAFT") "Review your next steps" else "Small steps, visible progress", style = MaterialTheme.typography.headlineMedium)
                Text("Template plan · ${tasks.count { it.done }} of ${tasks.size} steps complete", color = Muted)
                if (plan.status == "DRAFT") {
                    if (plan.sourceRevision != note.revision) Text("The idea changed after this draft. Discard it and generate a fresh template.", color = MaterialTheme.colorScheme.error)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button({ acceptPlan(plan.id) }, enabled = plan.sourceRevision == note.revision) { Text("Accept plan") }
                        TextButton({ discardPlan(plan.id) }) { Text("Discard draft") }
                    }
                }
                tasks.forEach { task ->
                    val blocked = task.dependencyId != null && tasks.none { it.id == task.dependencyId && it.done }
                    FolioPanel {
                        Eyebrow("PHASE ${task.position + 1} / ${if (task.done) "COMPLETE" else if (blocked) "WAITING" else "READY"}")
                        Text(task.title, style = MaterialTheme.typography.titleLarge)
                        Text(task.acceptance, color = Muted)
                        if (task.reflection.isNotBlank()) Text(task.reflection, color = Sage)
                        if (plan.status == "ACTIVE") {
                            if (task.done) TextButton({ reopenTask(task.id) }) { Text("Reopen step") }
                            else Button({ completing = task }, enabled = !blocked) { Text("Complete & reflect") }
                        }
                    }
                }
                Text("Application EXP is self-reported and capped at 40 per day. Reopening reverses the award; repeating a step does not earn again.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (deleting) AlertDialog(onDismissRequest = { deleting = false }, title = { Text("Delete this note?") },
        text = { Text("The note, its revisions, and its plan will be removed. Past learning activity remains in your EXP ledger.") },
        confirmButton = { TextButton(delete) { Text("Delete") } }, dismissButton = { TextButton({ deleting = false }) { Text("Keep note") } })
    completing?.let { task ->
        var reflection by rememberSaveable(task.id) { mutableStateOf("") }
        AlertDialog(onDismissRequest = { completing = null }, title = { Text("What did you learn?") }, text = {
            OutlinedTextField(reflection, { reflection = it.take(2_000) }, label = { Text("Result and reflection") }, minLines = 3)
        }, confirmButton = { TextButton({ completeTask(task.id, reflection) { completing = null } }, enabled = reflection.trim().length >= 12) { Text("Complete step") } },
            dismissButton = { TextButton({ completing = null }) { Text("Cancel") } })
    }
}
