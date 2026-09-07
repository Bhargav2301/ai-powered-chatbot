package com.polymath.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.polymath.model.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RagAndMediaTest {
    private fun dataset() = """{"schema_version":1,"id":"physics","title":"Physics","documents":[{"id":"circuit","title":"Voltage","topic_id":"engineering","source_url":"https://example.org/articles/circuit","format":"html","text":"<p>Voltage is current times resistance.</p><img src='../circuit.png' alt='Circuit diagram'>","quiz":{"question":"2 A times 6 ohms?","answers":["12 V","3 V"],"correct_answer":0,"explanation":"V = IR"}}]}"""

    @Test fun `imported HTML images are resolved against their own source`() {
        val card = DatasetParser.parse(dataset(), 1).cards.single()
        assertEquals("https://example.org/circuit.png", card.images.single().url)
        assertEquals("Circuit diagram", card.images.single().alt)
        assertFalse(card.body.contains("<img"))
        assertEquals("physics", card.card().datasetId)
        assertEquals(0, card.correctAnswer)
    }

    @Test fun `unsafe images unknown topics and duplicate document IDs reject import`() {
        assertTrue(runCatching { DatasetParser.parse(dataset().replace("../circuit.png", "https://u:p@example.org/image.png").replace("\"format\":\"html\",", "\"images\":[{\"url\":\"file:///secret\"}],")) }.isFailure)
        assertTrue(runCatching { DatasetParser.parse(dataset().replace("engineering", "unknown")) }.isFailure)
        val json = JSONObject(dataset()); val docs = json.getJSONArray("documents"); docs.put(docs.getJSONObject(0))
        assertTrue(runCatching { DatasetParser.parse(json.toString()) }.isFailure)
    }

    @Test fun `feed images stay with their own entry and channel logo is excluded`() {
        val xml = """<rss xmlns:media="http://search.yahoo.com/mrss/"><channel><image><url>https://example.org/logo.png</url></image>
            <item><title>First</title><link>https://example.org/first</link><media:group><media:thumbnail url="https://example.org/first.png"/></media:group><description><![CDATA[<img src='/other.png' alt='Other view'>]]></description></item>
            <item><title>Second</title><link>https://example.org/second</link><enclosure type="image/jpeg" url="https://example.org/second.jpg"/></item>
            <item><title>Third</title><link>https://example.org/third</link></item></channel></rss>"""
        val cards = FeedParser.parse(xml, FeedSource("Publisher", "https://example.org/feed", "science"), 1)
        assertEquals(listOf("https://example.org/first.png", "https://example.org/other.png"), cards[0].images.map { it.url })
        assertEquals("https://example.org/second.jpg", cards[1].images.single().url)
        assertTrue(cards[2].images.isEmpty())
    }

    @Test fun `Atom enclosure image is retained and audio is ignored`() {
        val xml = """<feed xmlns="http://www.w3.org/2005/Atom"><entry><title>Space</title><link href="https://example.org/space"/><link rel="enclosure" type="image/png" href="https://example.org/space.png"/><link rel="enclosure" type="audio/mpeg" href="https://example.org/audio.mp3"/></entry></feed>"""
        val card = FeedParser.parse(xml, FeedSource("P", "https://example.org/feed", "science"), 1).single()
        assertEquals("https://example.org/space", card.sourceUrl)
        assertEquals("https://example.org/space.png", card.images.single().url)
    }

    @Test fun `dataset scope cannot include other datasets or unsaved public cards`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, FolioDatabase::class.java).allowMainThreadQueries().build()
        try {
            val repository = FolioRepository(db)
            repository.initialize(); repository.importDataset(DatasetParser.parse(dataset()))
            assertTrue(RagCorpus.documents(repository.snapshot(), "vault").isEmpty())
            assertEquals(1, RagCorpus.documents(repository.snapshot(), "physics").size)
            repository.save("dataset:physics:circuit")
            assertEquals("https://example.org/circuit.png", RagCorpus.documents(repository.snapshot(), "vault").single().images.single().url)
            repository.addMessage(ChatMessageEntity("message", "physics", "assistant", "An excerpt", createdAt = 1))
            repository.deleteDataset("physics")
            val current = repository.snapshot()
            assertTrue(current.datasets.isEmpty()); assertTrue(current.saves.isEmpty()); assertTrue(current.chat.isEmpty())
            assertTrue(RagCorpus.documents(current, "vault").isEmpty())
            assertTrue(repository.search("Voltage").isEmpty())
        } finally { db.close() }
    }

    @Test fun `client rejects fabricated excerpt or stale revision and restores trusted media`() {
        val document = RagDocument("note:a", "vault", 2, "Test", "🌱 A saved observation.", null, emptyList())
        val citation = JSONObject().put("id", "S1").put("document_id", "note:a").put("dataset_id", "vault").put("revision", 2)
            .put("title", "Fake title").put("excerpt", document.text).put("start", 0).put("end", document.text.codePointCount(0, document.text.length))
            .put("source_url", "https://attacker.example").put("images", JSONArray().put(JSONObject().put("url", "https://attacker.example/image")))
        val reply = JSONObject().put("status", "answered").put("answer", "An observation [S1].").put("citations", JSONArray().put(citation))
        val verified = RagCorpus.verify(reply, listOf(document))
        val trusted = JSONArray(verified.citations).getJSONObject(0)
        assertTrue(trusted.isNull("source_url")); assertEquals("Test", trusted.getString("title")); assertEquals(0, trusted.getJSONArray("images").length())
        citation.put("revision", 1)
        assertTrue(runCatching { RagCorpus.verify(reply, listOf(document)) }.isFailure)
        citation.put("revision", 2).put("excerpt", "Invented")
        assertTrue(runCatching { RagCorpus.verify(reply, listOf(document)) }.isFailure)
    }

    @Test fun `production connection rejects insecure origins and credential URLs`() {
        val client = RagClient()
        listOf("http://example.org", "http://127.0.0.1:8000", "https://u:p@example.org", "https://example.org?key=x", "https://example.org/v1").forEach {
            assertTrue(runCatching { client.endpoint(it) }.isFailure)
        }
        assertEquals("https://example.org", client.endpoint("https://example.org/"))
        assertEquals("http://10.0.2.2:8000", RagClient(true).endpoint("http://10.0.2.2:8000"))
        assertTrue(runCatching { RagClient(true).endpoint("http://192.168.1.2") }.isFailure)
    }

    @Test fun `old recommendation events still replay after domain expansion`() {
        val event = SwipeEntity(actionId = "old", contentId = "old-card", action = "SAVE", features = "1.0,0.0,0.0,0.0,0.0,0.0,-1.0,1.0", createdAt = 0, modelVersion = 1)
        val snapshot = FolioSnapshot(cards = StarterContent.items().map { it.card() }, swipes = listOf(event))
        assertTrue(snapshot.deck(LearningRules.DAY).isNotEmpty())
    }
}
