package com.polymath.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.polymath.data.UserSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Tests the distributable APK through the user interface; no model file is injected by adb. */
@RunWith(AndroidJUnit4::class)
class OfflineSetupInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun bundledModelIsPreparedAndAnswersThroughChatWithoutCredentials() {
        assertEquals("com.polymath.app.offline", context.packageName)
        assertEquals("0.3.1", BuildConfig.VERSION_NAME)
        assertEquals(1, android.provider.Settings.Global.getInt(context.contentResolver, "airplane_mode_on"))
        waitFor("Explore all topics")
        compose.onNodeWithText("Explore all topics").performScrollTo().performClick()
        waitFor("Today's folio")
        compose.onNodeWithText("Desk").performClick()
        compose.onNodeWithContentDescription("Open AI chat").performClick()
        waitFor("Set up offline AI")
        compose.onNodeWithText("Polymath 0.3.1 · Offline preview").assertExists()
        screenshot("01-offline-chat")
        compose.onNodeWithText("Set up offline AI").performClick()
        waitFor("Prepare included AI")
        compose.onNodeWithText("HTTPS service origin").assertDoesNotExist()
        compose.onNodeWithText("Service API key").assertDoesNotExist()
        compose.onNodeWithText("Download Qwen · 397 MB").assertDoesNotExist()
        screenshot("02-included-model")
        compose.onNodeWithText("Prepare included AI").performScrollTo().performClick()
        waitFor("Installed · ready for offline chat", 120_000)
        screenshot("03-ready")
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Datasets").performClick()
        waitFor("Knowledge datasets")
        compose.onNodeWithText("Add example dataset").performClick()
        waitFor("Polymath foundations · 4 sources")
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Polymath foundations").performClick()
        compose.onNodeWithText("Ask your selected sources").performTextInput("What voltage is needed for 2 amps through 6 ohms?")
        compose.onNodeWithContentDescription("Send question").assertIsEnabled().performClick()
        waitFor("POLYMATH / QWEN · CHECK THE EVIDENCE", 180_000)
        compose.onAllNodesWithText("12", substring = true).onFirst().assertExists()
        compose.onNodeWithText("[S1] A small circuit, three connected quantities").assertExists()
        screenshot("04-cited-offline-answer")
        val settings = runBlocking { UserSettings(context).values.first() }
        assertTrue(settings.localAi)
        assertFalse(settings.aiEnabled)
        assertEquals("", settings.aiEndpoint)
        println("OFFLINE_UI_VERIFIED: bundled installation, normal chat, cited answer, no service configuration")
    }

    private fun waitFor(text: String, timeout: Long = 20_000) {
        compose.waitUntil(timeout) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun screenshot(name: String) {
        val image = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        val folder = File(context.filesDir, "offline-ui-evidence").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
    @After fun captureFinalScreen() {
        runCatching { screenshot("05-final-screen") }
        // The runner starts with empty app storage and uses only the synthetic example dataset.
        val app = context.applicationContext as PolymathApplication
        println("TEST_CHAT_RECORD: " + runBlocking { app.repository.snapshot().chat.map { it.role to (it.status + ": " + it.text) } })
    }
}
