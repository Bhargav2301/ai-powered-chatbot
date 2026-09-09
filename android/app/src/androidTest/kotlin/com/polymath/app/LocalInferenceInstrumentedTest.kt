package com.polymath.app

import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.polymath.data.*
import com.polymath.local.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LocalInferenceInstrumentedTest {
    @Test fun isolatedNativeProcessAnswersFromEvidenceInAirplaneMode() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.filesDir, QwenPack.fileName)
        assertTrue("Push the approved model into app files before this integration test", file.isFile)
        assertEquals(1, android.provider.Settings.Global.getInt(context.contentResolver, "airplane_mode_on"))
        VerifiedModelFile.verify(file, QwenPack.bytes, QwenPack.sha256)
        val document = RagDocument("circuit", "test", 1, "Ohm's law", "A resistor carries 2 amps through 6 ohms. Ohm's law gives voltage = current times resistance, so the voltage is 12 volts.", null, emptyList())
        val inference = LocalInference(context)
        val reply = LocalRag.answer("What voltage is needed for 2 amps through 6 ohms?", listOf(document), emptyList()) { prompt ->
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { inference.generate(it, prompt) }
        }
        assertEquals("answered", reply.status)
        assertTrue(reply.text.contains("12"))
        assertTrue(reply.text.contains("[S1]"))
        // Unbind kills only the worker; Room and the foreground app remain available.
        val application = context.applicationContext as PolymathApplication
        application.repository.initialize()
        assertTrue(application.repository.snapshot().cards.isNotEmpty())
    }
    @Test fun cancellationDoesNotKillTheApplication() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.filesDir, QwenPack.fileName)
        assertTrue("This test requires the approved model pack", file.isFile)
        val job = launch {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use {
                LocalInference(context).generate(it, "<|im_start|>user\nList facts about physics.<|im_end|>\n<|im_start|>assistant\n")
            }
        }
        delay(500)
        withTimeout(5_000) { job.cancelAndJoin() }
        assertTrue(job.isCancelled)
        assertEquals("com.polymath.app", context.packageName)
    }
}
