package com.polymath.local

import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.security.MessageDigest

class ModelPackTest {
    @get:Rule val folder = TemporaryFolder()
    private val bytes = "GGUF-test-pack".toByteArray()
    private val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
    @Test fun validFileIsPromotedAndVerified() {
        val target = folder.newFile("model")
        VerifiedModelFile.install(bytes.inputStream(), target, bytes.size.toLong(), hash)
        VerifiedModelFile.verify(target, bytes.size.toLong(), hash)
        assertArrayEquals(bytes, target.readBytes())
    }
    @Test fun corruptAndTruncatedInstallPreservePreviousModel() {
        val target = folder.newFile("model").apply { writeText("previous model") }
        for (input in listOf(bytes.dropLast(1).toByteArray(), ByteArray(bytes.size))) {
            assertThrows(IllegalArgumentException::class.java) { VerifiedModelFile.install(input.inputStream(), target, bytes.size.toLong(), hash) }
            assertEquals("previous model", target.readText())
            assertFalse(java.io.File(target.parentFile, "model.part").exists())
        }
    }
    @Test fun cancelledInstallDoesNotPromotePartialFile() {
        val target = folder.newFile("model").apply { writeText("previous") }
        assertThrows(CancellationException::class.java) { VerifiedModelFile.install(bytes.inputStream(), target, bytes.size.toLong(), hash, active = { false }) }
        assertEquals("previous", target.readText())
    }
    @Test fun resourcePolicyBlocksSmallBusyHotOrUnsupportedDevices() {
        val gib = 1024L * 1024 * 1024
        assertNull(DevicePolicy.reason("arm64-v8a", 4*gib, 2*gib, 200*1024*1024, false, 0))
        assertNotNull(DevicePolicy.reason("armeabi-v7a", 4*gib, 2*gib, 0, false, 0))
        assertNotNull(DevicePolicy.reason("arm64-v8a", 2*gib, 2*gib, 0, false, 0))
        assertNotNull(DevicePolicy.reason("arm64-v8a", 4*gib, gib, 0, false, 0))
        assertNotNull(DevicePolicy.reason("arm64-v8a", 4*gib, 2*gib, 0, false, 3))
    }
}
