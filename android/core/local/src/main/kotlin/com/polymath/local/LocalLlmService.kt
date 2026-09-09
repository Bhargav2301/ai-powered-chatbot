package com.polymath.local

import android.app.Service
import android.content.Intent
import android.os.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

object NativeLlm {
    init { System.loadLibrary("polymath_llm") }
    external fun generate(fd: Int, prompt: ByteArray, threads: Int): ByteArray
}

/** A permissionless, disposable process. Kill only our own isolated process on final unbind. */
class LocalLlmService : Service() {
    private val executor = Executors.newSingleThreadExecutor()
    private val busy = AtomicBoolean(false)
    private val messenger = Messenger(Handler(Looper.getMainLooper()) { request ->
        if (request.what == GENERATE) {
            val reply = request.replyTo
            @Suppress("DEPRECATION")
            val descriptor = request.data.getParcelable<ParcelFileDescriptor>("model")
            val prompt = request.data.getString("prompt").orEmpty()
            if (descriptor == null || !busy.compareAndSet(false, true)) {
                descriptor?.close(); send(reply, ERROR, "error", "The local model is busy or unavailable.")
            } else executor.execute {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                try {
                    descriptor.use {
                        require(Process.isIsolated()) { "Local inference requires an isolated process." }
                        require(checkSelfPermission(android.Manifest.permission.INTERNET) == android.content.pm.PackageManager.PERMISSION_DENIED) { "Local inference must have no internet permission." }
                        val response = NativeLlm.generate(it.fd, prompt.toByteArray(Charsets.UTF_8), 2).toString(Charsets.UTF_8)
                        send(reply, RESULT, "text", response)
                    }
                } catch (error: LinkageError) { send(reply, ERROR, "error", "This build has no compatible local inference library.") }
                  catch (error: Exception) { send(reply, ERROR, "error", error.message?.take(220) ?: "Local inference failed.") }
                finally { busy.set(false) }
            }
        }
        true
    })
    private fun send(reply: Messenger?, what: Int, key: String, text: String) {
        try { reply?.send(Message.obtain(null, what).apply { data = Bundle().apply { putString(key, text) } }) }
        catch (_: RemoteException) { terminate() }
    }
    override fun onBind(intent: Intent): IBinder = messenger.binder
    override fun onUnbind(intent: Intent): Boolean { terminate(); return false }
    override fun onLowMemory() { terminate() }
    @Deprecated("Framework callback")
    override fun onTrimMemory(level: Int) {
        if (level == TRIM_MEMORY_RUNNING_LOW || level == TRIM_MEMORY_RUNNING_CRITICAL || level >= TRIM_MEMORY_COMPLETE) terminate()
    }
    private fun terminate() {
        executor.shutdownNow()
        if (Process.isIsolated()) Process.killProcess(Process.myPid())
    }
    companion object { const val GENERATE = 1; const val RESULT = 2; const val ERROR = 3 }
}
