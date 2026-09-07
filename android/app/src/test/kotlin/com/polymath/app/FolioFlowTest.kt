package com.polymath.app

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Runs the actual Hilt Activity and Room repositories under Android framework simulation. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = PolymathApplication::class, qualifiers = "w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FolioFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun `onboard save a card and capture a persistent thought`() {
        waitFor("Explore all topics")
        screenshot("01-onboarding")
        compose.onNodeWithText("Explore all topics").performScrollTo().performClick()
        waitFor("Today's folio")
        screenshot("02-discover")
        compose.onNodeWithText("Like & save").performScrollTo().performClick()
        compose.onNodeWithText("Vault").performClick()
        waitFor("1 saved sources · 0 notes")
        screenshot("03-vault")
        compose.onNodeWithText("Graph").performClick()
        waitFor("Your learning graph")
        screenshot("04-graph")
        compose.onNodeWithContentDescription("Capture a note").performClick()
        compose.onNodeWithText("What's on your mind?").performTextInput("A useful observation to keep for my next project.")
        compose.onNodeWithText("Save thought").performScrollTo().performClick()
        waitFor("THOUGHT / REVISION 1")
        screenshot("05-notebook")
        compose.activityRule.scenario.recreate()
        waitFor("THOUGHT / REVISION 1")
        compose.onAllNodesWithText("A useful observation to keep for my next project.").onFirst().assertExists()
    }
    private fun waitFor(text: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun screenshot(name: String) {
        val folder = File(requireNotNull(System.getProperty("polymath.screenshots")))
        folder.mkdirs()
        // PixelCopy waits for a hardware frame that Robolectric does not supply.
        // Draw the Activity's real view tree into the native software canvas instead.
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
