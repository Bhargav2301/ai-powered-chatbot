package com.polymath.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.polymath.data.*
import com.polymath.model.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ReaderSheet(card: Card, folio: FolioSnapshot, now: Long, close: () -> Unit,
    save: () -> Unit, capture: () -> Unit, review: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    var sourceError by remember { mutableStateOf(false) }
    val schedule = folio.reviews.firstOrNull { it.contentId == card.id }
    ModalBottomSheet(close, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = CanvasColor) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Eyebrow(if (card.kind == ContentKind.NEWS) "NEWS / ${Topics.title(card.topicId).uppercase()}" else "KNOWLEDGE PILL / ${Topics.title(card.topicId).uppercase()}")
            Text(card.title, style = MaterialTheme.typography.headlineLarge)
            SourceImages(card.images)
            Text(card.summary, color = Muted, style = MaterialTheme.typography.bodyLarge)
            HorizontalDivider(color = BorderColor)
            if (card.body != card.summary) Text(card.body, style = MaterialTheme.typography.bodyLarge)
            if (card.kind == ContentKind.NEWS) Text("Publisher feed excerpt. Open the source for the full article.", color = Muted)
            Text("${card.publisher} · ${ageLabel(card, now)}", color = Muted)
            if (card.kind == ContentKind.NEWS) Text("Fetched ${formatDate(card.fetchedAt)}", style = MaterialTheme.typography.bodySmall, color = Muted)
            TextButton(onClick = { try { uriHandler.openUri(card.sourceUrl) } catch (_: Exception) { sourceError = true } }) {
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, null); Spacer(Modifier.width(8.dp)); Text("Read the source")
            }
            if (sourceError) Text("No browser is available to open this source.", color = MaterialTheme.colorScheme.error)
            if (card.question != null) FolioPanel {
                Text("Can you bring it back?", style = MaterialTheme.typography.titleLarge)
                Text(if (schedule == null || schedule.dueAt <= now) "Close the lesson and test one idea. Correct recall earns EXP." else "Your next recall is scheduled for ${formatDate(schedule.dueAt)}.", color = Muted)
                Button(review, enabled = schedule == null || schedule.dueAt <= now) { Text("Practice recall") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(save, Modifier.weight(1f)) { Text(if (folio.saves.any { it.contentId == card.id }) "Saved" else "Save") }
                Button(capture, Modifier.weight(1f)) { Text("Turn into idea") }
            }
            TextButton(close) { Text("Back to my folio") }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun RecallSheet(card: Card, schedule: ReviewEntity?, close: () -> Unit,
    answer: (Int, Int, (AttemptEntity) -> Unit) -> Unit) {
    var selected by rememberSaveable(card.id) { mutableIntStateOf(-1) }
    val episode = remember(card.id) { schedule?.episode ?: 0 }
    var result by remember(card.id) { mutableStateOf<AttemptEntity?>(null) }
    var submitting by remember { mutableStateOf(false) }
    ModalBottomSheet(close, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = CanvasColor) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Eyebrow("RECALL / ${Topics.title(card.topicId).uppercase()}")
            Text(card.question ?: "", style = MaterialTheme.typography.headlineMedium)
            Text("Choose before revealing the explanation.", color = Muted)
            card.answers.forEachIndexed { index, option ->
                OutlinedCard(onClick = { if (result == null && !submitting) selected = index }, modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, if (selected == index) Sage else BorderColor)) {
                    Row(Modifier.padding(12.dp)) {
                        RadioButton(selected == index, onClick = null, enabled = result == null && !submitting)
                        Text(option, Modifier.padding(top = 12.dp, start = 8.dp), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            if (result == null) Button(onClick = {
                submitting = true
                answer(selected, episode) { result = it; submitting = false }
            }, enabled = selected >= 0 && !submitting, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(if (submitting) "Checking…" else "Check my answer") }
            result?.let { attempt ->
                FolioPanel {
                    Text(if (attempt.correct) "+${attempt.awardedExp} EXP · Recalled" else "A useful gap to revisit", style = MaterialTheme.typography.titleLarge, color = Sage)
                    Text(card.explanation, style = MaterialTheme.typography.bodyLarge)
                    Text("This records one recall attempt. Your next practice is scheduled automatically.", color = Muted)
                    Button(close) { Text("Continue learning") }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

fun formatDate(time: Long): String = DateTimeFormatter.ofPattern("d MMM, HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(time))
