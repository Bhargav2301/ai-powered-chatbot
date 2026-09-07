package com.polymath.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.polymath.data.*
import com.polymath.model.*
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable fun DiscoverScreen(folio: FolioSnapshot, settings: Settings, refreshing: Boolean, now: Long,
    open: (Card) -> Unit, judge: (Card, Judgment) -> Unit, capture: () -> Unit, refresh: () -> Unit,
    enableNews: () -> Unit, review: (Card) -> Unit) {
    var filter by rememberSaveable { mutableStateOf("Mix") }
    val deck = remember(folio, now, filter) { folio.deck(now).filter { filter == "Mix" || (filter == "News") == (it.kind == ContentKind.NEWS) } }
    val due = folio.reviews.filter { it.dueAt <= now }.mapNotNull { schedule -> folio.cards.firstOrNull { it.id == schedule.contentId } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageHeading("POLYMATH / DISCOVER", "Today's folio") {
            IconButton(onClick = capture) { Icon(Icons.Outlined.EditNote, "Capture a thought") }
        } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Mix", "News", "Pills").forEach { value -> FilterChip(filter == value, { filter = value }, label = { Text(value) }, modifier = Modifier.heightIn(min = 48.dp)) }
                Spacer(Modifier.weight(1f))
                if (settings.liveNews) IconButton(onClick = refresh, enabled = !refreshing) {
                    if (refreshing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Icon(Icons.Outlined.Refresh, "Refresh news")
                }
            }
        }
        if (due.isNotEmpty()) item {
            Surface(onClick = { review(due.first()) }, shape = RoundedCornerShape(20.dp), color = Sage.copy(alpha = .12f)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AutoAwesome, null, tint = Sage)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text("Bring an idea back", color = Sage); Text("${due.size} recall${if (due.size == 1) "" else "s"} ready", color = Muted) }
                    Icon(Icons.Outlined.ChevronRight, "Practice")
                }
            }
        }
        if (deck.isEmpty()) item {
            EmptyPanel("A little space to think.", if (filter == "News" && !settings.liveNews)
                "Connect to public RSS feeds for news from NASA, Android Developers, and arXiv. Your notes stay on your device."
                else "You've reached the end of this deck. Revisit a saved idea, practice a lesson, or capture your next step.") {
                if (!settings.liveNews) Button(enableNews) { Text("Enable news fetching") }
                else OutlinedButton(refresh, enabled = !refreshing) { Text(if (refreshing) "Refreshing…" else "Check for news") }
            }
        } else item(key = deck.first().id) {
            val card = deck.first()
            val reason = if (folio.preferences.any { it.topicId == card.topicId && it.followed })
                "Because you follow ${Topics.title(card.topicId).lowercase()}" else "Exploring ${Topics.title(card.topicId).lowercase()}"
            SwipeCard(card, now, reason, onOpen = { open(card) }, onJudge = { judge(card, it) })
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Eyebrow("${deck.size} IN YOUR DECK")
                Text("Swipe to shape your mix", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (!settings.liveNews && filter != "News") item {
            TextButton(enableNews) { Icon(Icons.Outlined.RssFeed, null); Spacer(Modifier.width(8.dp)); Text("Add live news to your folio") }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable private fun SwipeCard(card: Card, now: Long, reason: String, onOpen: () -> Unit, onJudge: (Judgment) -> Unit) {
    val offset = remember(card.id) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var dragging by remember(card.id) { mutableFloatStateOf(0f) }
    var committing by remember(card.id) { mutableStateOf(false) }
    val threshold = with(LocalDensity.current) { 90.dp.toPx() }
    fun commit(judgment: Judgment) {
        if (committing) return
        committing = true
        onJudge(judgment)
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth().graphicsLayer {
                translationX = dragging + offset.value
                rotationZ = ((dragging + offset.value) / threshold * 5f).coerceIn(-8f, 8f)
            }.pointerInput(card.id) {
                detectHorizontalDragGestures(onDragEnd = {
                    if (abs(dragging) > threshold) commit(if (dragging > 0) Judgment.SAVE else Judgment.DISMISS)
                    else {
                        val start = dragging; dragging = 0f
                        scope.launch { offset.snapTo(start); offset.animateTo(0f) }
                    }
                }, onDragCancel = { dragging = 0f }) { change, delta -> change.consume(); if (!committing) dragging += delta }
            }.clickable(onClick = onOpen).semantics {
                customActions = listOf(CustomAccessibilityAction("Like and save") { commit(Judgment.SAVE); true },
                    CustomAccessibilityAction("Dismiss") { commit(Judgment.DISMISS); true })
            }, shape = RoundedCornerShape(28.dp), color = SurfaceColor,
            border = BorderStroke(1.dp, if (dragging > threshold / 2) Sage else BorderColor)
        ) {
            Column(Modifier.padding(24.dp).heightIn(min = 350.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Eyebrow(if (card.kind == ContentKind.NEWS) "NEWS" else "KNOWLEDGE PILL")
                    Eyebrow(if (dragging > threshold / 2) "SAVE" else if (dragging < -threshold / 2) "DISMISS" else "1 MIN")
                }
                Text(card.title, style = MaterialTheme.typography.headlineLarge)
                Text(card.summary.ifBlank { "Open the source to read this update." }, style = MaterialTheme.typography.bodyLarge, color = Muted)
                Spacer(Modifier.height(20.dp))
                Surface(shape = CircleShape, color = CanvasColor) { Text(Topics.title(card.topicId), Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = Sage, style = MaterialTheme.typography.labelLarge) }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${card.publisher} · ${ageLabel(card, now)}", style = MaterialTheme.typography.bodySmall, color = Muted)
                    Text(reason, style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { commit(Judgment.DISMISS) }, enabled = !committing, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                Icon(Icons.Outlined.Close, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Dismiss")
            }
            Button(onClick = { commit(Judgment.SAVE) }, enabled = !committing, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                Icon(Icons.Outlined.BookmarkBorder, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Like & save")
            }
        }
    }
}

fun ageLabel(card: Card, now: Long): String {
    if (card.kind == ContentKind.KNOWLEDGE_PILL) return "Starter lesson"
    val time = card.publishedAt ?: return "Publication date unavailable"
    val hours = ((now - time).coerceAtLeast(0) / 3_600_000)
    return when { hours < 1 -> "Published recently"; hours < 24 -> "${hours}h ago"; else -> "${hours / 24}d ago" }
}
