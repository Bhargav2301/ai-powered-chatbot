package com.polymath.data

import android.text.Html
import com.polymath.model.ContentKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.net.URI
import java.security.MessageDigest
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

data class FeedSource(val title: String, val url: String, val topicId: String)
data class RefreshResult(val fetched: Int, val failedSources: List<String>, val refreshedAt: Long)

class NewsFetcher(private val repository: FolioRepository) {
    private val client = OkHttpClient.Builder().connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS).callTimeout(25, TimeUnit.SECONDS)
        .followSslRedirects(false).build()
    suspend fun refresh(): RefreshResult = withContext(Dispatchers.IO) {
        val failures = mutableListOf<String>()
        var count = 0
        for (source in sources) {
            try {
                val request = Request.Builder().url(source.url).header("User-Agent", "Polymath/0.1 RSS reader")
                    .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml").build()
                val entries = client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Feed request failed")
                    val body = response.body ?: throw IOException("Empty feed")
                    if (body.contentLength() > MAX_BYTES) throw IOException("Feed too large")
                    val bytes = body.byteStream().use { input ->
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (output.size() <= MAX_BYTES) {
                            val read = input.read(buffer, 0, minOf(buffer.size, MAX_BYTES + 1 - output.size()))
                            if (read < 0) break
                            output.write(buffer, 0, read)
                        }
                        output.toByteArray()
                    }
                    if (bytes.size > MAX_BYTES) throw IOException("Feed too large")
                    FeedParser.parse(bytes.toString(Charsets.UTF_8), source, System.currentTimeMillis())
                }
                repository.ingest(entries)
                count += entries.size
            } catch (_: IOException) { failures += source.title }
              catch (_: org.xmlpull.v1.XmlPullParserException) { failures += source.title }
        }
        RefreshResult(count, failures, System.currentTimeMillis())
    }
    companion object {
        private const val MAX_BYTES = 2 * 1024 * 1024
        val sources = listOf(
            FeedSource("NASA", "https://www.nasa.gov/feed/", "science"),
            FeedSource("Android Developers", "https://android-developers.googleblog.com/feeds/posts/default", "craft"),
            FeedSource("arXiv AI", "https://rss.arxiv.org/rss/cs.AI", "ai"),
        )
    }
}

object FeedParser {
    fun parse(xml: String, source: FeedSource, fetchedAt: Long): List<ContentEntity> {
        require(URI(source.url).scheme == "https")
        // Reject declarations before parsing; feeds have no reason to define external entities.
        if (xml.contains("<!DOCTYPE", true) || xml.contains("<!ENTITY", true)) throw IOException("Unsupported XML declaration")
        val parser = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }.newPullParser()
        parser.setInput(StringReader(xml))
        val result = mutableListOf<ContentEntity>()
        var entryDepth = -1
        var title = ""; var link = ""; var description = ""; var published: String? = null
        var field: String? = null
        var fieldDepth = -1
        val text = StringBuilder()
        while (parser.eventType != XmlPullParser.END_DOCUMENT && result.size < 30) {
            val name = parser.name?.lowercase()
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    if (name == "item" || name == "entry") {
                        entryDepth = parser.depth; title = ""; link = ""; description = ""; published = null
                    } else if (entryDepth > 0 && parser.depth == entryDepth + 1) {
                        field = name; fieldDepth = parser.depth; text.clear()
                        if (name == "link" && parser.getAttributeValue(null, "rel") in listOf(null, "alternate")) {
                            parser.getAttributeValue(null, "href")?.let { link = it }
                        }
                    }
                }
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> if (field != null) text.append(parser.text)
                XmlPullParser.END_TAG -> {
                    if (parser.depth == fieldDepth && field != null) {
                        val value = text.toString().trim()
                        when (field) {
                            "title" -> title = clean(value).take(200)
                            "link" -> if (value.isNotBlank()) link = value
                            "description", "summary", "content", "encoded" -> if (description.isBlank()) description = clean(value).take(600)
                            "pubdate", "published" -> published = value
                            "updated" -> if (published == null) published = value
                        }
                        field = null; fieldDepth = -1
                    }
                    if (parser.depth == entryDepth && name in listOf("item", "entry")) {
                        val url = runCatching { URI(source.url).resolve(link).takeIf { it.scheme == "https" && it.host != null && it.userInfo == null }?.toString() }.getOrNull()
                        if (title.isNotBlank() && link.isNotBlank() && url != null) {
                            val id = MessageDigest.getInstance("SHA-256").digest(url.toByteArray()).joinToString("") { "%02x".format(it) }
                            val date = published?.let { raw -> runCatching { Instant.parse(raw).toEpochMilli() }.getOrNull()
                                ?: runCatching { ZonedDateTime.parse(raw, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() }.getOrNull() }
                            result += ContentEntity("news:$id", ContentKind.NEWS, title, description,
                                description, source.topicId, source.title, url, date, fetchedAt)
                        }
                        entryDepth = -1
                    }
                }
            }
            parser.next()
        }
        return result.distinctBy { it.id }
    }
    private fun clean(text: String) = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString().replace(Regex("\\s+"), " ").trim()
}
