package com.polymath.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URI
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Only the configured service key is encrypted here. Notes remain in the app-private Room DB. */
class ServiceSecret(context: Context) {
    private val prefs = context.getSharedPreferences("ai_service_secret", Context.MODE_PRIVATE)
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (store.getKey("polymath-service", null) as? SecretKey) ?: KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("polymath-service", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun save(value: String) {
        if (value.isBlank()) { prefs.edit().clear().commit(); return }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val bytes = cipher.iv + cipher.doFinal(value.toByteArray())
        check(prefs.edit().putString("value", Base64.encodeToString(bytes, Base64.NO_WRAP)).commit())
    }
    fun read(): String {
        val bytes = prefs.getString("value", null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return ""
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12))) }
        return cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8)
    }
}

data class RagDocument(val id: String, val datasetId: String, val revision: Int, val title: String,
    val text: String, val sourceUrl: String?, val images: List<com.polymath.model.SourceImage>) {
    fun json() = JSONObject().put("id", id).put("dataset_id", datasetId).put("revision", revision)
        .put("title", title.take(200)).put("text", text).put("source_url", sourceUrl ?: JSONObject.NULL)
        .put("images", JSONArray(Converters().images(images)))
}
data class RagReply(val text: String, val citations: String, val status: String)

object RagCorpus {
    fun documents(snapshot: FolioSnapshot, scope: String): List<RagDocument> {
        if (scope == "vault") {
            val savedIds = snapshot.saves.map { it.contentId }.toSet()
            return snapshot.cards.filter { it.id in savedIds }.map {
                RagDocument("content:${it.id}", "vault", it.edition, it.title, it.body.ifBlank { it.summary }, it.sourceUrl, it.images)
            } + snapshot.notes.map { RagDocument("note:${it.id}", "vault", it.revision,
                it.title.ifBlank { "Untitled ${it.mode.label}" }, listOf(it.body, it.goal, it.context).filter(String::isNotBlank).joinToString("\n").ifBlank { it.title }, null, emptyList()) }
        }
        require(snapshot.datasets.any { it.id == scope }) { "This dataset is no longer available." }
        return snapshot.cards.filter { it.datasetId == scope }.map {
            RagDocument("content:${it.id}", scope, it.edition, it.title, it.body, it.sourceUrl, it.images)
        }
    }

    fun request(question: String, scope: String, documents: List<RagDocument>, history: List<String>): JSONObject {
        require(question.trim().length in 1..2000) { "Ask a question of up to 2,000 characters." }
        require(documents.size <= 150 && documents.sumOf { it.text.length + it.title.length } <= 300000 && documents.all { it.text.length <= 20000 }) {
            "This scope is too large. Select a smaller imported dataset (150 sources / 300,000 characters maximum)."
        }
        return JSONObject().put("question", question.trim()).put("dataset_ids", JSONArray(listOf(scope)))
            .put("documents", JSONArray().apply { documents.forEach { put(it.json()) } })
            .put("previous_questions", JSONArray(history.takeLast(3)))
    }

    fun verify(value: JSONObject, documents: List<RagDocument>): RagReply {
        val status = value.getString("status")
        require(status in listOf("answered", "insufficient_evidence", "unverified"))
        val answer = value.getString("answer")
        require(answer.length in 1..6000)
        val citations = value.getJSONArray("citations")
        require(citations.length() <= 4)
        val ids = mutableSetOf<String>()
        for (i in 0 until citations.length()) {
            val citation = citations.getJSONObject(i)
            val id = citation.getString("id")
            require(id.matches(Regex("S[1-4]")) && ids.add(id))
            val source = requireNotNull(documents.firstOrNull { it.id == citation.getString("document_id") })
            require(source.datasetId == citation.getString("dataset_id") && source.revision == citation.getInt("revision"))
            // Server offsets use Unicode code points; Kotlin indexes UTF-16 code units.
            val start = citation.getInt("start"); val end = citation.getInt("end")
            require(start >= 0 && end > start && end <= source.text.codePointCount(0, source.text.length))
            require(source.text.substring(source.text.offsetByCodePoints(0, start), source.text.offsetByCodePoints(0, end)) == citation.getString("excerpt"))
            // Links and media always come from this device's imported/saved source, never server text.
            citation.put("title", source.title).put("source_url", source.sourceUrl ?: JSONObject.NULL)
                .put("images", JSONArray(Converters().images(source.images)))
        }
        if (status == "answered") require(ids.isNotEmpty() && Regex("\\[(S\\d+)\\]").findAll(answer).map { it.groupValues[1] }.toSet() == ids)
        return RagReply(answer, citations.toString(), status)
    }
}

class RagClient(private val allowLocalHttp: Boolean = false) {
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(140, TimeUnit.SECONDS)
        .callTimeout(150, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build()

    fun endpoint(value: String): String {
        val uri = runCatching { URI(value.trim()) }.getOrNull()
        require(uri != null && !uri.host.isNullOrBlank() && uri.userInfo == null && uri.query == null && uri.fragment == null && uri.path in listOf("", "/")) {
            "Enter the service origin, such as https://polymath.example.org, without a path or credentials."
        }
        require(uri.scheme == "https" || (allowLocalHttp && uri.scheme == "http" && uri.host in listOf("127.0.0.1", "localhost", "10.0.2.2"))) {
            "Use HTTPS. Debug builds also support localhost and the Android emulator host."
        }
        return uri.toString().trimEnd('/')
    }

    suspend fun answer(url: String, key: String, payload: JSONObject, documents: List<RagDocument>, onStatus: (String) -> Unit): RagReply {
        require(key.length >= 24) { "Enter the service API key in Connection settings." }
        val body = payload.toString()
        require(body.toByteArray().size <= 2 * 1024 * 1024) { "Select a smaller dataset." }
        val request = Request.Builder().url(endpoint(url) + "/v1/chat").header("Authorization", "Bearer $key")
            .header("Accept", "application/x-ndjson").post(body.toRequestBody("application/json".toMediaType())).build()
        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(IOException("Could not reach the AI service. Check the connection and retry."))
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val reply = response.use {
                            check(it.isSuccessful) { when (it.code) {
                                401 -> "The service API key was rejected. Update Connection settings."
                                429 -> "The model is busy. Please retry shortly."
                                413, 422 -> "The dataset exceeds the service limits or uses an unsupported format."
                                else -> "The AI service returned HTTP ${it.code}."
                            } }
                            val source = requireNotNull(it.body).source()
                            var result: RagReply? = null
                            var lines = 0
                            while (!source.exhausted() && lines++ < 16) {
                                val event = JSONObject(source.readUtf8LineStrict(65536))
                                when (event.getString("type")) {
                                    "status" -> onStatus(event.getString("message").take(100))
                                    "answer" -> { result = RagCorpus.verify(event, documents); break }
                                    "error" -> error("The model could not finish. Check the service and retry.")
                                }
                            }
                            result ?: error("The answer was interrupted. Please retry.")
                        }
                        if (continuation.isActive) continuation.resume(reply)
                    } catch (e: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }
                }
            })
        }
    }
}
