package com.polymath.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import coil.Coil
import coil.ImageLoader
import coil.decode.DataSource
import coil.intercept.Interceptor
import coil.request.SuccessResult
import com.polymath.app.ui.PolymathTheme
import com.polymath.app.ui.SourceImages
import com.polymath.model.SourceImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SourceImageTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun `source image pixels and attribution render in the flashcard gallery`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bitmap = javaClass.classLoader!!.getResourceAsStream("ohms-law.png")!!.use { BitmapFactory.decodeStream(it) }
        var requested: Any? = null
        val loader = ImageLoader.Builder(context).components {
            add(Interceptor { chain ->
                requested = chain.request.data
                SuccessResult(BitmapDrawable(context.resources, bitmap), chain.request, DataSource.MEMORY_CACHE)
            })
        }.build()
        Coil.setImageLoader(loader)
        compose.setContent { PolymathTheme { Column {
            Text("A circuit, with its source image")
            SourceImages(listOf(SourceImage("https://example.org/circuit.png", "A 12 volt circuit with a 6 ohm resistor", "Polymath original illustration")))
        } } }
        compose.waitUntil(10_000) { requested != null }
        compose.waitForIdle()
        assertEquals("https://example.org/circuit.png", requested)
        compose.onNodeWithContentDescription("A 12 volt circuit with a 6 ohm resistor").assertIsDisplayed()
        compose.onNodeWithText("Polymath original illustration").assertIsDisplayed()
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val rendered = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(rendered))
            val folder = File(System.getProperty("polymath.screenshots")!!).apply { mkdirs() }
            File(folder, "09-source-image.png").outputStream().use { rendered.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        loader.shutdown()
    }
}
