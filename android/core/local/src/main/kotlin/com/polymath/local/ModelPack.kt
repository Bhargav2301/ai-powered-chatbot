package com.polymath.local

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import java.io.*
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object QwenPack {
    const val fileName = "Qwen3-0.6B-Q4_K_M.gguf"
    const val revision = "50968a4468ef4233ed78cd7c3de230dd1d61a56b"
    const val sha256 = "ac2d97712095a558e31573f62f466a3f9d93990898b0ec79d7c974c1780d524a"
    const val bytes = 396705472L
    const val url = "https://huggingface.co/unsloth/Qwen3-0.6B-GGUF/resolve/$revision/$fileName"
}

/** All install sources use the same bounded hash check and atomic promotion. No arbitrary models. */
object VerifiedModelFile {
    fun install(input: InputStream, target: File, bytes: Long, sha256: String,
                active: () -> Boolean = { true }, progress: (Long) -> Unit = {}) {
        target.parentFile!!.mkdirs()
        val partial = File(target.parentFile, target.name + ".part")
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            FileOutputStream(partial).use { output ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    if (!active()) throw CancellationException("Model installation cancelled")
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    require(total <= bytes) { "The model file exceeds the approved size." }
                    output.write(buffer, 0, read); digest.update(buffer, 0, read); progress(total)
                }
                output.fd.sync()
            }
            require(total == bytes && hex(digest.digest()) == sha256) { "The model failed its size or SHA-256 check. Download the approved Qwen pack." }
            if (!active()) throw CancellationException("Model installation cancelled")
            check(partial.renameTo(target)) { "Could not finish installing the model." }
        } finally { partial.delete() }
    }
    fun verify(file: File, bytes: Long, sha256: String) {
        require(file.length() == bytes) { "Install the local Qwen model first." }
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
        }
        require(hex(digest.digest()) == sha256) { "The installed model is damaged. Remove it and install the approved pack again." }
    }
    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it.toInt() and 255) }
}

class ModelPackStore(private val context: Context) {
    private val mutex = Mutex()
    private val file get() = File(context.noBackupFilesDir, "models/${QwenPack.fileName}")
    private val http = OkHttpClient.Builder().followSslRedirects(false)
        .connectTimeout(20, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.MINUTES).build()
    fun installed() = file.isFile && file.length() == QwenPack.bytes
    fun bundled() = runCatching { context.assets.list("models")?.contains(QwenPack.fileName) == true }.getOrDefault(false)
    suspend fun remove() = withContext(Dispatchers.IO) { mutex.withLock { file.delete(); Unit } }
    suspend fun openVerified(): ParcelFileDescriptor = withContext(Dispatchers.IO) {
        mutex.withLock {
            ensureActive()
            VerifiedModelFile.verify(file, QwenPack.bytes, QwenPack.sha256)
            ensureActive()
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        }
    }
    private fun checkSpace() {
        file.parentFile!!.mkdirs()
        require(file.parentFile!!.usableSpace > QwenPack.bytes + 64L * 1024 * 1024) { "Free at least 460 MB of storage before installing this model." }
    }
    suspend fun import(uri: Uri?, progress: (Long) -> Unit) = withContext(Dispatchers.IO) {
        mutex.withLock {
            checkSpace()
            val job = currentCoroutineContext()[Job]!!
            val input = if (uri == null) context.assets.open("models/${QwenPack.fileName}")
                        else requireNotNull(context.contentResolver.openInputStream(uri)) { "Could not open this model file." }
            input.use { VerifiedModelFile.install(it, file, QwenPack.bytes, QwenPack.sha256, { job.isActive }, progress) }
        }
    }
    suspend fun download(progress: (Long) -> Unit) = mutex.withLock {
        withContext(Dispatchers.IO) { checkSpace() }
        val completed = CompletableDeferred<Unit>()
        try { suspendCancellableCoroutine<Unit> { continuation ->
            val call = http.newCall(Request.Builder().url(QwenPack.url).build())
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    try { if (continuation.isActive) continuation.resumeWithException(e) } finally { completed.complete(Unit) }
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        response.use {
                            require(it.isSuccessful) { "Model download returned HTTP ${it.code}. Try again or import the approved GGUF file." }
                            val body = requireNotNull(it.body)
                            require(body.contentLength() == -1L || body.contentLength() == QwenPack.bytes) { "Unexpected model download size." }
                            body.byteStream().use { stream -> VerifiedModelFile.install(stream, file, QwenPack.bytes, QwenPack.sha256, { continuation.isActive }, progress) }
                        }
                        if (continuation.isActive) continuation.resume(Unit)
                    } catch (e: Exception) { if (continuation.isActive) continuation.resumeWithException(e) }
                    finally { completed.complete(Unit) }
                }
            })
        } } finally {
            // Hold the install mutex until the cancelled OkHttp reader has closed and removed its partial file.
            withContext(NonCancellable) { completed.await() }
        }
    }
}
