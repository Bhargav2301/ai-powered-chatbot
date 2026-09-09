package com.polymath.local

import android.app.ActivityManager
import android.content.*
import android.os.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object DevicePolicy {
    fun reason(abi: String, total: Long, available: Long, threshold: Long, lowRam: Boolean, thermal: Int): String? = when {
        abi !in setOf("arm64-v8a", "x86_64") -> "Local AI needs a 64-bit ARM device. Other features remain available."
        lowRam || total < 3584L * 1024 * 1024 -> "Local AI needs a device with about 4 GB RAM or more."
        available < 1536L * 1024 * 1024 + threshold -> "There is not enough free memory for local AI. Close other apps and try again."
        thermal >= 3 -> "Let your device cool before running local AI."
        else -> null
    }
    fun check(context: Context) {
        val manager = context.getSystemService(ActivityManager::class.java)
        val memory = ActivityManager.MemoryInfo().also(manager::getMemoryInfo)
        val thermal = if (Build.VERSION.SDK_INT >= 29) context.getSystemService(PowerManager::class.java).currentThermalStatus else 0
        val reason = reason(Build.SUPPORTED_ABIS.firstOrNull().orEmpty(), memory.totalMem, memory.availMem, memory.threshold, manager.isLowRamDevice || memory.lowMemory, thermal)
        require(reason == null) { reason.orEmpty() }
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (battery != null) {
            val level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
            require(plugged || level < 0 || scale <= 0 || level.toDouble() / scale >= .15) { "Charge above 15% before running local AI." }
        }
    }
}

/** Only model bytes and the bounded prompt cross Binder. No socket or localhost server. */
class LocalInference(private val context: Context) {
    private val mutex = Mutex()
    suspend fun generate(model: ParcelFileDescriptor, prompt: String): String = mutex.withLock {
        require(prompt.toByteArray(Charsets.UTF_8).size <= 24000)
        withContext(Dispatchers.Main.immediate) {
            withTimeout(125_000) {
                var bound = false
                var connection: ServiceConnection? = null
                var thermalListener: PowerManager.OnThermalStatusChangedListener? = null
                val power = context.getSystemService(PowerManager::class.java)
                try {
                    suspendCancellableCoroutine<String> { continuation ->
                        fun fail(message: String) { if (continuation.isActive) continuation.resumeWithException(IllegalStateException(message)) }
                        val replies = Messenger(Handler(Looper.getMainLooper()) { message ->
                            if (continuation.isActive) {
                                if (message.what == LocalLlmService.RESULT) continuation.resume(message.data.getString("text").orEmpty())
                                else fail(message.data.getString("error") ?: "Local inference stopped. Your notes are safe; try again.")
                            }
                            true
                        })
                        connection = object : ServiceConnection {
                            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                                if (!continuation.isActive) return
                                try {
                                    val request = Message.obtain(null, LocalLlmService.GENERATE).apply {
                                        replyTo = replies
                                        data = Bundle().apply { putParcelable("model", model); putString("prompt", prompt) }
                                    }
                                    Messenger(binder).send(request)
                                } catch (_: Exception) { fail("Could not start local inference. Please try again.") }
                            }
                            override fun onServiceDisconnected(name: ComponentName) = fail("The local AI process stopped. Free memory and try again; your notes are safe.")
                            override fun onBindingDied(name: ComponentName) = fail("Local AI was interrupted. Please retry.")
                            override fun onNullBinding(name: ComponentName) = fail("Local AI could not start on this device.")
                        }
                        if (Build.VERSION.SDK_INT >= 29) {
                            thermalListener = PowerManager.OnThermalStatusChangedListener { level ->
                                if (level >= PowerManager.THERMAL_STATUS_SEVERE) fail("Local AI stopped to let your device cool.")
                            }
                            power.addThermalStatusListener(context.mainExecutor, thermalListener!!)
                        }
                        try {
                            bound = context.bindService(Intent(context, LocalLlmService::class.java), connection!!, Context.BIND_AUTO_CREATE)
                            if (!bound) fail("This device could not bind the local AI service.")
                        } catch (_: Exception) { fail("Local AI could not start on this device.") }
                    }
                } finally {
                    if (Build.VERSION.SDK_INT >= 29) thermalListener?.let(power::removeThermalStatusListener)
                    if (bound) connection?.let { context.unbindService(it) }
                    // Last unbind terminates the isolated worker, even after a timeout or native failure.
                }
            }
        }
    }
}
