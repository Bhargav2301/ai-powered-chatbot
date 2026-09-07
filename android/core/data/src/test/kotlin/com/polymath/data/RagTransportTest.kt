package com.polymath.data

import kotlinx.coroutines.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RagTransportTest {
    @Test fun `Android client sends only the selected corpus and parses service NDJSON`() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val d = RagDocument("note:a", "vault", 1, "Circuit", "Voltage is 12 volts.", null, emptyList())
            val citation = JSONObject().put("id", "S1").put("document_id", d.id).put("dataset_id", "vault")
                .put("revision", 1).put("start", 0).put("end", d.text.length).put("excerpt", d.text)
            val final = JSONObject().put("type", "answer").put("status", "answered").put("answer", "12 volts [S1].")
                .put("citations", JSONArray().put(citation))
            server.enqueue(MockResponse().setHeader("Content-Type", "application/x-ndjson").setBody("{\"type\":\"status\",\"message\":\"Retrieving\"}\n$final\n"))
            val statuses = mutableListOf<String>()
            val request = RagCorpus.request("What voltage?", "vault", listOf(d), emptyList())
            val reply = RagClient(true).answer(server.url("/").toString(), "test-key-long-enough-for-transport", request, listOf(d)) { statuses += it }
            assertEquals("12 volts [S1].", reply.text)
            assertEquals(listOf("Retrieving"), statuses)
            val sent = server.takeRequest(1, TimeUnit.SECONDS)!!
            assertEquals("/v1/chat", sent.path)
            assertNotNull(sent.getHeader("Authorization"))
            val json = JSONObject(sent.body.readUtf8())
            assertEquals(1, json.getJSONArray("documents").length())
            assertEquals("vault", json.getJSONArray("dataset_ids").getString(0))
        } finally { server.shutdown() }
    }

    @Test fun `an authentication failure is actionable and cancellation closes a waiting call`() = runBlocking {
        val server = MockWebServer(); server.start()
        try {
            val request = RagCorpus.request("Question", "vault", emptyList(), emptyList())
            server.enqueue(MockResponse().setResponseCode(401))
            val failure = runCatching { RagClient(true).answer(server.url("/").toString(), "test-key-long-enough-for-transport", request, emptyList()) {} }.exceptionOrNull()
            assertTrue(failure!!.message!!.contains("API key"))
            server.enqueue(MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.NO_RESPONSE))
            val job = launch { RagClient(true).answer(server.url("/").toString(), "test-key-long-enough-for-transport", request, emptyList()) {} }
            delay(100); job.cancelAndJoin()
            assertTrue(job.isCancelled)
        } finally { server.shutdown() }
    }
}
