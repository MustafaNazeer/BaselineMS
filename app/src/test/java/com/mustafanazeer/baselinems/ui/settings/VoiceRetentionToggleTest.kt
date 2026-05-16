package com.mustafanazeer.baselinems.ui.settings

import com.mustafanazeer.baselinems.battery.voice.VoiceSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class VoiceRetentionToggleTest {

    @get:Rule
    val tempFolder: TemporaryFolder = TemporaryFolder()

    private lateinit var filesDir: File

    @Before
    fun setup() {
        filesDir = tempFolder.newFolder("files")
    }

    @After
    fun tearDown() {
        // TemporaryFolder cleans up itself
    }

    @Test
    fun `prefs key constant matches the Phase 7 idiom and ADR Section 7`() {
        assertEquals("voice_save_audio", VoiceSettings.KEY_VOICE_SAVE_AUDIO)
    }

    @Test
    fun `audio traces directory name matches the ADR Section 7 path`() {
        assertEquals("audio_traces", VoiceSettings.AUDIO_TRACES_DIR)
    }

    @Test
    fun `deleteAllRetainedAudio with no audio_traces directory returns zero and does not crash`() {
        assertEquals(0, VoiceSettings.deleteAllRetainedAudio(filesDir))
    }

    @Test
    fun `deleteAllRetainedAudio removes a single wav gz file synchronously`() {
        val tracesDir = File(filesDir, "audio_traces")
        tracesDir.mkdirs()
        val session = File(tracesDir, "session-a").apply { mkdirs() }
        val voice = File(session, "VOICE.wav.gz")
        voice.writeBytes(byteArrayOf(0x1f, 0x8b.toByte(), 0x00, 0x00))
        assertTrue(voice.exists())

        val deleted = VoiceSettings.deleteAllRetainedAudio(filesDir)

        assertTrue("expected at least one file deletion; got $deleted", deleted >= 1)
        assertFalse(voice.exists())
    }

    @Test
    fun `deleteAllRetainedAudio removes multiple wav gz files across session subdirectories`() {
        val tracesDir = File(filesDir, "audio_traces")
        tracesDir.mkdirs()
        val sessionA = File(tracesDir, "session-a").apply { mkdirs() }
        val sessionB = File(tracesDir, "session-b").apply { mkdirs() }
        val a = File(sessionA, "VOICE.wav.gz").apply { writeBytes(byteArrayOf(0x01)) }
        val b = File(sessionB, "VOICE.wav.gz").apply { writeBytes(byteArrayOf(0x02)) }

        VoiceSettings.deleteAllRetainedAudio(filesDir)

        assertFalse(a.exists())
        assertFalse(b.exists())
        assertFalse(tracesDir.exists())
    }

    @Test
    fun `deleteAllRetainedAudio leaves the parent filesDir intact`() {
        val tracesDir = File(filesDir, "audio_traces").apply { mkdirs() }
        File(tracesDir, "VOICE.wav.gz").writeBytes(byteArrayOf(0x00))

        VoiceSettings.deleteAllRetainedAudio(filesDir)

        assertTrue(filesDir.exists())
    }

    @Test
    fun `deleteAllRetainedAudio is idempotent across repeated invocations`() {
        val tracesDir = File(filesDir, "audio_traces").apply { mkdirs() }
        File(tracesDir, "VOICE.wav.gz").writeBytes(byteArrayOf(0x00))

        val firstPass = VoiceSettings.deleteAllRetainedAudio(filesDir)
        val secondPass = VoiceSettings.deleteAllRetainedAudio(filesDir)

        assertTrue(firstPass >= 1)
        assertEquals(0, secondPass)
    }

    @Test
    fun `deleteAllRetainedAudio does not touch unrelated app private files`() {
        val unrelatedFile = File(filesDir, "sensor_traces/session-x/GAIT.csv.gz")
        unrelatedFile.parentFile?.mkdirs()
        unrelatedFile.writeBytes(byteArrayOf(0x01, 0x02))

        val tracesDir = File(filesDir, "audio_traces").apply { mkdirs() }
        File(tracesDir, "VOICE.wav.gz").writeBytes(byteArrayOf(0x00))

        VoiceSettings.deleteAllRetainedAudio(filesDir)

        assertTrue(unrelatedFile.exists())
    }
}
