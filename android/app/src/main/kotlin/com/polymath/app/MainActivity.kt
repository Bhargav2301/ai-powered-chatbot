package com.polymath.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polymath.app.ui.PolymathApp
import com.polymath.app.ui.PolymathTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

data class LaunchRequest(val token: String = UUID.randomUUID().toString(), val captureMode: String? = null,
    val sharedText: String? = null, val cardId: String? = null)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: FolioViewModel by viewModels()
    private val launch = MutableStateFlow(LaunchRequest())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        readIntent(intent)
        setContent {
            val request by launch.collectAsStateWithLifecycle()
            PolymathTheme { PolymathApp(viewModel, request) }
        }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); readIntent(intent) }
    private fun readIntent(intent: Intent) {
        val shared = if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") intent.getStringExtra(Intent.EXTRA_TEXT)?.take(20_000) else null
        launch.value = LaunchRequest(captureMode = intent.getStringExtra("capture_mode"), sharedText = shared,
            cardId = intent.getStringExtra("card_id"))
    }
}
