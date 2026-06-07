package com.mustafanazeer.baselinems.signals

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream

class VoiceWavWriterTest {

    @get:Rule
    val tempFolder: TemporaryFolder = TemporaryFolder()

    private fun gunzip(file: File): ByteArray =
        GZIPInputStream(file.inputStream()).use { it.readBytes() }

    private fun littleEndianInt(bytes: ByteArray, offset: Int): Int =
        ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int

    private fun littleEndianShort(bytes: ByteArray, offset: Int): Int =
        ByteBuffer.wrap(bytes, offset, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()

    @Test
    fun `writes a gzip stream with the standard magic bytes`() {
        val target = File(tempFolder.newFolder("files"), "audio_traces/s1/VOICE.wav.gz")
        VoiceWavWriter.writeGzippedWav(
            pcm = ShortArray(10) { (it * 100).toShort() },
            sampleRateHz = 44_100,
            target = target,
        )

        assertTrue("target file should exist", target.exists())
        val raw = target.readBytes()
        assertEquals(0x1f, raw[0].toInt() and 0xff)
        assertEquals(0x8b, raw[1].toInt() and 0xff)
    }

    @Test
    fun `creates parent directories lazily`() {
        val filesDir = tempFolder.newFolder("files")
        val target = File(filesDir, "audio_traces/session-x/VOICE.wav.gz")
        assertTrue("parent should not exist before write", !target.parentFile!!.exists())

        VoiceWavWriter.writeGzippedWav(
            pcm = ShortArray(4),
            sampleRateHz = 44_100,
            target = target,
        )

        assertTrue(target.parentFile!!.exists())
        assertTrue(target.exists())
    }

    @Test
    fun `RIFF header fields are correct for 44_1 kHz mono 16 bit`() {
        val pcm = ShortArray(100) { (it - 50).toShort() }
        val target = File(tempFolder.newFolder("files"), "VOICE.wav.gz")
        VoiceWavWriter.writeGzippedWav(pcm = pcm, sampleRateHz = 44_100, target = target)

        val wav = gunzip(target)
        val dataBytes = pcm.size * 2

        assertEquals("RIFF", String(wav, 0, 4, Charsets.US_ASCII))
        assertEquals("expected RIFF chunk size = 36 + dataBytes", 36 + dataBytes, littleEndianInt(wav, 4))
        assertEquals("WAVE", String(wav, 8, 4, Charsets.US_ASCII))

        assertEquals("fmt ", String(wav, 12, 4, Charsets.US_ASCII))
        assertEquals("fmt subchunk size = 16 (PCM)", 16, littleEndianInt(wav, 16))
        assertEquals("audio format = 1 (PCM)", 1, littleEndianShort(wav, 20))
        assertEquals("num channels = 1 (mono)", 1, littleEndianShort(wav, 22))
        assertEquals("sample rate = 44100", 44_100, littleEndianInt(wav, 24))
        assertEquals("byte rate = 44100 * 1 * 2", 44_100 * 2, littleEndianInt(wav, 28))
        assertEquals("block align = channels * bytesPerSample = 2", 2, littleEndianShort(wav, 32))
        assertEquals("bits per sample = 16", 16, littleEndianShort(wav, 34))

        assertEquals("data", String(wav, 36, 4, Charsets.US_ASCII))
        assertEquals("data subchunk size = dataBytes", dataBytes, littleEndianInt(wav, 40))
        assertEquals("total file = 44 header + dataBytes", 44 + dataBytes, wav.size)
    }

    @Test
    fun `PCM samples round trip little endian through gzip`() {
        val pcm = shortArrayOf(0, 1, -1, 32767, -32768, 12345, -9999)
        val target = File(tempFolder.newFolder("files"), "VOICE.wav.gz")
        VoiceWavWriter.writeGzippedWav(pcm = pcm, sampleRateHz = 44_100, target = target)

        val wav = gunzip(target)
        val data = wav.copyOfRange(44, wav.size)
        val readBack = ShortArray(pcm.size)
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(readBack)

        assertArrayEquals(pcm, readBack)
    }

    @Test
    fun `empty PCM produces a valid header only WAV with zero data`() {
        val target = File(tempFolder.newFolder("files"), "VOICE.wav.gz")
        VoiceWavWriter.writeGzippedWav(pcm = ShortArray(0), sampleRateHz = 44_100, target = target)

        val wav = gunzip(target)
        assertEquals(44, wav.size)
        assertEquals(0, littleEndianInt(wav, 40))
    }

    @Test
    fun `gzip output is smaller than raw wav for a compressible signal`() {
        val pcm = ShortArray(44_100) { 0 }
        val target = File(tempFolder.newFolder("files"), "VOICE.wav.gz")
        VoiceWavWriter.writeGzippedWav(pcm = pcm, sampleRateHz = 44_100, target = target)

        val gzSize = target.length()
        val rawWavSize = 44L + pcm.size * 2L
        assertTrue("gzip of silence should be far smaller than raw wav", gzSize < rawWavSize)
    }

    @Test
    fun `gunzip via ByteArrayInputStream confirms stream is well formed`() {
        val target = File(tempFolder.newFolder("files"), "VOICE.wav.gz")
        VoiceWavWriter.writeGzippedWav(pcm = ShortArray(8) { 7 }, sampleRateHz = 44_100, target = target)

        val bytes = GZIPInputStream(ByteArrayInputStream(target.readBytes())).use { it.readBytes() }
        assertEquals("RIFF", String(bytes, 0, 4, Charsets.US_ASCII))
    }
}
