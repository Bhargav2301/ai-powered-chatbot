package com.polymath.app.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polymath.data.FolioSnapshot
import com.polymath.model.*

@Composable fun GraphScreen(folio: FolioSnapshot, now: Long, review: (Card) -> Unit) {
    var selected by rememberSaveable { mutableStateOf("systems") }
    var list by rememberSaveable { mutableStateOf(false) }
    val topicExp = folio.topicExp(selected)
    val level = LearningRules.level(topicExp)
    val due = folio.reviews.filter { it.dueAt <= now }.map { it.contentId }.toSet()
    val next = folio.cards.firstOrNull { it.topicId == selected && it.id in due }
        ?: folio.cards.firstOrNull { it.topicId == selected && it.question != null && folio.reviews.none { r -> r.contentId == it.id } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { PageHeading("CONNECTIONS THAT GROW", "Your learning graph") }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${folio.totalExp} EXP", style = MaterialTheme.typography.headlineMedium, color = Sage)
            TextButton({ list = !list }) { Text(if (list) "Show graph" else "Show topic list") }
        } }
        if (!list) item { TopicGraph(folio, selected, onSelect = { selected = it }) }
        item {
            Text("Lines show related topics. Filled nodes record earned EXP. Interest and learning progress are tracked separately.", color = Muted, style = MaterialTheme.typography.bodyMedium)
        }
        if (list) Topics.all.forEach { topic -> item {
            OutlinedCard(onClick = { selected = topic.id }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(topic.title); Text("${folio.topicExp(topic.id)} EXP", color = Sage)
                }
            }
        } }
        item { FolioPanel(Modifier.fillMaxWidth()) {
            Eyebrow("TOPIC FOCUS")
            Text(Topics.title(selected), style = MaterialTheme.typography.headlineMedium)
            Text("Level $level · $topicExp / ${LearningRules.nextLevelAt(topicExp)} EXP", color = Sage)
            val base = level * level * 100
            LinearProgressIndicator(progress = { (topicExp - base).toFloat() / (LearningRules.nextLevelAt(topicExp) - base) }, modifier = Modifier.fillMaxWidth())
            Text("Recall an idea or apply it to a project to grow this node.", color = Muted)
            if (next != null) Button({ review(next) }) { Text(if (next.id in due) "Review a due lesson" else "Practice a starter lesson") }
            else Text("No practice is due for this topic right now.", color = Muted)
        } }
    }
}

@Composable private fun TopicGraph(folio: FolioSnapshot, selected: String, onSelect: (String) -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val radius = with(density) { 25.dp.toPx() }
    val textSize = with(density) { 11.sp.toPx() }
    val labelPaint = remember(textSize) { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Ink.toArgb(); this.textSize = textSize; textAlign = Paint.Align.CENTER } }
    Canvas(Modifier.fillMaxWidth().height(300.dp).semantics {
        contentDescription = "Learning graph. ${Topics.all.joinToString { "${it.title}: ${folio.topicExp(it.id)} EXP" }}. Use the topic list for individual controls."
    }.pointerInput(scale, pan) {
        detectTapGestures { touch ->
            val hit = Topics.all.minByOrNull { topic -> (Offset(size.width * topic.x, size.height * topic.y) * scale + pan - touch).getDistance() }
            if (hit != null && (Offset(size.width * hit.x, size.height * hit.y) * scale + pan - touch).getDistance() < radius * scale * 1.6f) onSelect(hit.id)
        }
    }.pointerInput(Unit) {
        detectTransformGestures { centroid, delta, zoom, _ ->
            val next = (scale * zoom).coerceIn(.8f, 1.6f)
            pan = (centroid - (centroid - pan) * (next / scale) + delta).let {
                Offset(it.x.coerceIn(-size.width * .6f, size.width * .25f), it.y.coerceIn(-size.height * .6f, size.height * .25f))
            }
            scale = next
        }
    }) {
        fun point(id: String): Offset = Topics.all.first { it.id == id }.let { Offset(size.width * it.x, size.height * it.y) * scale + pan }
        Topics.related.forEach { (a, b) -> drawLine(BorderColor, point(a), point(b), strokeWidth = 2.dp.toPx()) }
        Topics.all.forEach { topic ->
            val center = point(topic.id)
            val exp = folio.topicExp(topic.id)
            drawCircle(if (exp > 0) Sage.copy(alpha = .24f) else SurfaceColor, radius * scale, center)
            drawCircle(if (topic.id == selected) Sage else BorderColor, radius * scale, center, style = Stroke(if (topic.id == selected) 2.dp.toPx() else 1.dp.toPx()))
            labelPaint.color = if (exp > 0) Sage.toArgb() else Ink.toArgb()
            drawContext.canvas.nativeCanvas.drawText("${LearningRules.level(exp)}", center.x, center.y + textSize / 3, labelPaint)
            labelPaint.color = Muted.toArgb()
            drawContext.canvas.nativeCanvas.drawText(when (topic.id) { "ai" -> "AI"; "systems" -> "Systems"; "craft" -> "Craft"; else -> topic.title }, center.x, center.y + radius * scale + textSize * 1.5f, labelPaint)
        }
    }
}
