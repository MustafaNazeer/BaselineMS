package com.mustafanazeer.baselinems.battery.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream

class VoiceAudioRetentionTest {

    @get:Rule
    val tempFolder: TemporaryFolder = TemporaryFolder()

    private lateinit var filesDir: File

    @org.junit.Before
    fun setup() {
        filesDir = tempFolder.newFolder("files")
    }

    private fun expectedTarget(sessionId: String): File =
        File(filesDir, "audio_traces/$sessionId/VOICE.wav.gz")

    @Test
    fun `NoOp retention writes nothing`() {
        NoOpVoiceAudioRetention.persist(ShortArray(100) { 1 })
        assertFalse(File(filesDir, "audio_traces").exists())
    }

    @Test
    fun `enabled retention writes VOICE wav gz at the session contract path`() {
        val retention = FileVoiceAudioRetention(
            enabled = true,
            deviceSecureAtStart = true,
            sessionId = "session-7",
            filesDir = filesDir,
            sampleRateHz = 44_100,
        )

        retention.persist(ShortArray(50) { (it * 10).toShort() })

        val target = expectedTarget("session-7")
        assertTrue("expected ${target.path} to exist", target.exists())
        val wav = GZIPInputStream(target.inputStream()).use { it.readBytes() }
        assertEquals("RIFF", String(wav, 0, 4, Charsets.US_ASCII))
        assertEquals("WAVE", String(wav, 8, 4, Charsets.US_ASCII))
        assertEquals(
            44_100,
            ByteBuffer.wrap(wav, 24, 4).order(ByteOrder.LITTLE_ENDIAN).int,
        )
    }

    @Test
    fun `toggle off produces no file even when device is secure`() {
        val retention = FileVoiceAudioRetention(
            enabled = false,
            deviceSecureAtStart = true,
            sessionId = "session-off",
            filesDir = filesDir,
            sampleRateHz = 44_100,
        )

        retention.persist(ShortArray(50) { 5 })

        assertFalse(expectedTarget("session-off").exists())
    }

    @Test
    fun `lock screen removed since opt in falls back to discard`() {
        val retention = FileVoiceAudioRetention(
            enabled = true,
            deviceSecureAtStart = false,
            sessionId = "session-insecure",
            filesDir = filesDir,
            sampleRateHz = 44_100,
        )

        retention.persist(ShortArray(50) { 5 })

        assertFalse(
            "no file should be written when the lock screen credential was removed",
            expectedTarget("session-insecure").exists(),
        )
    }

    @Test
    fun `write failure degrades to discard and does not throw`() {
        val collision = File(filesDir, "audio_traces/session-collide")
        collision.parentFile?.mkdirs()
        collision.writeBytes(byteArrayOf(0x00))

        val retention = FileVoiceAudioRetention(
            enabled = true,
            deviceSecureAtStart = true,
            sessionId = "session-collide",
            filesDir = filesDir,
            sampleRateHz = 44_100,
        )

        retention.persist(ShortArray(50) { 5 })

        assertFalse(expectedTarget("session-collide").exists())
        assertTrue("the colliding regular file must remain untouched", collision.exists())
    }

    @Test
    fun `persist does not mutate the buffer it is handed`() {
        val pcm = ShortArray(20) { (it + 1).toShort() }
        val copy = pcm.copyOf()
        val retention = FileVoiceAudioRetention(
            enabled = true,
            deviceSecureAtStart = true,
            sessionId = "session-nomutate",
            filesDir = filesDir,
            sampleRateHz = 44_100,
        )

        retention.persist(pcm)

        org.junit.Assert.assertArrayEquals(copy, pcm)
    }
}
