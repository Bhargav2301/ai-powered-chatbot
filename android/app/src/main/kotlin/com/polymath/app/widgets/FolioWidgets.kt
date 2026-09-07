package com.polymath.app.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.polymath.app.MainActivity
import com.polymath.model.NoteMode

private val Revealed = booleanPreferencesKey("public_pill_revealed")
private val WidgetInk = ColorProvider(Color(0xFFF1F0EB))
private val WidgetMuted = ColorProvider(Color(0xFFA9AFB7))

class PillWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val revealed = currentState<androidx.datastore.preferences.core.Preferences>()[Revealed] ?: false
            Column(GlanceModifier.fillMaxSize().background(Color(0xFF171A1E)).padding(16.dp)) {
                Text("POLYMATH · ONE PILL", style = TextStyle(color = WidgetMuted, fontSize = 11.sp))
                Spacer(GlanceModifier.height(10.dp))
                Text(if (revealed) "Close the source. Explain the idea. Then check what you missed." else "What makes recall different from rereading?",
                    style = TextStyle(color = WidgetInk, fontSize = 17.sp, fontWeight = FontWeight.Bold), maxLines = 4)
                Spacer(GlanceModifier.defaultWeight())
                Text("Source: The Learning Scientists", style = TextStyle(color = WidgetMuted, fontSize = 10.sp), maxLines = 1)
                Row(GlanceModifier.fillMaxWidth()) {
                    Button(if (revealed) "Question" else "Reveal", actionRunCallback<RevealPillAction>(), modifier = GlanceModifier.defaultWeight())
                    Button("Practice", actionStartActivity(Intent(context, MainActivity::class.java).putExtra("card_id", "starter:retrieval")), modifier = GlanceModifier.defaultWeight())
                }
            }
        }
    }
}
class RevealPillAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) { it[Revealed] = !(it[Revealed] ?: false) }
        PillWidget().update(context, glanceId)
    }
}
class PillWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = PillWidget() }

class CaptureWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Column(GlanceModifier.fillMaxSize().background(Color(0xFF171A1E)).padding(12.dp)) {
                Text("POLYMATH · CAPTURE", style = TextStyle(color = WidgetMuted, fontSize = 11.sp))
                Spacer(GlanceModifier.height(8.dp))
                Row(GlanceModifier.fillMaxWidth()) {
                    NoteMode.entries.forEach { mode -> Button(mode.label,
                        actionStartActivity(Intent(context, MainActivity::class.java).putExtra("capture_mode", mode.name)),
                        modifier = GlanceModifier.defaultWeight()) }
                }
            }
        }
    }
}
class CaptureWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = CaptureWidget() }
