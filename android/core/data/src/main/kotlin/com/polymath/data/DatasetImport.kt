package com.polymath.data

import android.text.Html
import com.polymath.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

object SourceMedia {
    fun https(raw: String, base: String = ""): String? = runCatching {
        URI(base).resolve(raw.trim()).takeIf {
            it.scheme == "https" && !it.host.isNullOrBlank() && it.userInfo == null && it.toString().length <= 2048
        }?.toASCIIString()
    }.getOrNull()

    fun fromHtml(html: String, base: String): List<SourceImage> = Regex("<img\\b[^>]*>", RegexOption.IGNORE_CASE)
        .findAll(html).mapNotNull { tag ->
            fun attr(name: String): String = Regex("\\b$name\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))", RegexOption.IGNORE_CASE)
                .find(tag.value)?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }?.let {
                    Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY).toString()
                } ?: ""
            val raw = attr("src").ifBlank { attr("srcset").substringBefore(',').trim().substringBefore(' ') }
            https(raw, base)?.takeIf { raw.isNotBlank() }?.let {
                SourceImage(it, attr("alt").ifBlank { "Source illustration" }.take(500), attr("title").take(1000))
            }
        }.distinctBy { it.url }.take(6).toList()
}

data class DatasetImport(val dataset: DatasetEntity, val cards: List<ContentEntity>)

/** JSON is read via Android's document picker; no remote URL crawling or file-path execution. */
object DatasetParser {
    const val MAX_BYTES = 2 * 1024 * 1024
    fun parse(json: String, now: Long = System.currentTimeMillis()): DatasetImport {
        require(json.toByteArray().size <= MAX_BYTES) { "Dataset exceeds 2 MiB." }
        val root = JSONObject(json)
        require(root.getInt("schema_version") == 1) { "Unsupported dataset format." }
        val id = root.getString("id")
        require(id.matches(Regex("[a-zA-Z0-9_-]{1,64}")) && id !in setOf("vault", "public")) { "Invalid dataset ID." }
        val title = root.getString("title").trim()
        require(title.length in 1..100) { "Dataset title must have 1–100 characters." }
        val input = root.getJSONArray("documents")
        require(input.length() in 1..150) { "Import 1–150 documents per dataset." }
        val cards = List(input.length()) { i ->
            val d = input.getJSONObject(i)
            val documentId = d.getString("id")
            require(documentId.matches(Regex("[a-zA-Z0-9_-]{1,64}"))) { "Invalid document ID." }
            val documentTitle = d.getString("title").trim()
            require(documentTitle.length in 1..200)
            val topic = d.getString("topic_id")
            require(Topics.all.any { it.id == topic }) { "Unknown topic: $topic" }
            val raw = d.getString("text")
            require(raw.length in 1..20000) { "Document text must have 1–20,000 characters." }
            val url = requireNotNull(SourceMedia.https(d.getString("source_url"))) { "Use a valid HTTPS source URL." }
            val images = d.optJSONArray("images") ?: JSONArray()
            require(images.length() <= 6) { "Use at most 6 images per document." }
            val explicit = List(images.length()) { imageIndex -> images.getJSONObject(imageIndex).let { image ->
                SourceImage(requireNotNull(SourceMedia.https(image.getString("url"), url)) { "Use HTTPS image URLs." },
                    image.optString("alt", "Source illustration").take(500), image.optString("caption").take(1000))
            } }
            val media = (explicit + SourceMedia.fromHtml(raw, url)).distinctBy { it.url }
            require(media.size <= 6) { "Use at most 6 images per document, including embedded images." }
            val text = if (d.optString("format", "text") == "html") Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString().trim() else raw.trim()
            require(text.isNotBlank())
            val quiz = d.optJSONObject("quiz")
            val answers = quiz?.getJSONArray("answers")?.let { a -> List(a.length()) { a.getString(it) } } ?: emptyList()
            val correct = quiz?.getInt("correct_answer") ?: -1
            if (quiz != null) require(answers.size in 2..6 && correct in answers.indices && quiz.getString("question").isNotBlank()) { "Invalid recall question." }
            ContentEntity(id = "dataset:$id:$documentId", kind = ContentKind.KNOWLEDGE_PILL,
                title = documentTitle, summary = d.optString("summary", text.take(350)).take(600), body = text,
                topicId = topic, publisher = d.optString("publisher", title).take(120), sourceUrl = url,
                publishedAt = null, fetchedAt = now, question = quiz?.getString("question"), answers = answers,
                correctAnswer = correct, explanation = quiz?.optString("explanation") ?: "", images = media, datasetId = id)
        }
        require(cards.map { it.id }.distinct().size == cards.size) { "Duplicate document IDs." }
        require(cards.sumOf { it.body.length + it.title.length } <= 300000) { "Dataset exceeds 300,000 characters. Split it into smaller files." }
        return DatasetImport(DatasetEntity(id, title, now), cards)
    }
}
