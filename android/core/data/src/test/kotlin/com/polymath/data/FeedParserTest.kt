package com.polymath.data

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FeedParserTest {
    private val source = FeedSource("Test publisher", "https://example.org/feed", "science")
    @Test fun `RSS preserves source dates and strips HTML from excerpts`() {
        val xml = """<rss><channel><item><title>Discovery</title><link>https://example.org/article</link><description><![CDATA[<p>A <b>useful</b> update.</p>]]></description><pubDate>Mon, 01 Sep 2025 12:00:00 GMT</pubDate></item></channel></rss>"""
        val item = FeedParser.parse(xml, source, 123).single()
        assertEquals("A useful update.", item.summary)
        assertNotNull(item.publishedAt)
        assertEquals(123L, item.fetchedAt)
    }
    @Test fun `Atom uses alternate link rather than self link`() {
        val xml = """<feed xmlns="http://www.w3.org/2005/Atom"><entry><title>An update</title><link rel="self" href="https://example.org/api"/><link rel="alternate" href="https://example.org/article"/><summary>A short summary</summary><published>2025-09-01T12:00:00Z</published></entry></feed>"""
        assertEquals("https://example.org/article", FeedParser.parse(xml, source, 0).single().sourceUrl)
    }
    @Test fun `insecure and executable source URLs are not ingested`() {
        listOf("http://example.org/article", "javascript:alert(1)", "file:///secret").forEach { url ->
            assertTrue(FeedParser.parse("<rss><channel><item><title>Bad URL</title><link>$url</link></item></channel></rss>", source, 0).isEmpty())
        }
    }
    @Test fun `XML external entity declarations are rejected`() {
        assertTrue(runCatching { FeedParser.parse("<!DOCTYPE rss [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><rss/>", source, 0) }.isFailure)
    }
    @Test fun `duplicate entries collapse to one canonical URL`() {
        val entry = "<item><title>One</title><link>https://example.org/one</link></item>"
        assertEquals(1, FeedParser.parse("<rss><channel>$entry$entry</channel></rss>", source, 0).size)
    }
}
