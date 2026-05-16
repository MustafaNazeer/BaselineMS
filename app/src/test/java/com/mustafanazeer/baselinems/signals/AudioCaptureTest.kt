package com.mustafanazeer.baselinems.signals

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AudioCaptureTest {

    @Before
    fun setUp() {
        mockkStatic(AudioRecord::class)
        every {
            AudioRecord.getMinBufferSize(
                44_100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
        } returns DEFAULT_MIN_BUFFER_BYTES
    }

    @After
    fun tearDown() {
        unmockkStatic(AudioRecord::class)
    }

    @Test
    fun `constructor rejects 16000 Hz`() {
        assertConstructorRejects(16_000)
    }

    @Test
    fun `constructor rejects 22050 Hz`() {
        assertConstructorRejects(22_050)
    }

    @Test
    fun `constructor rejects 48000 Hz`() {
        assertConstructorRejects(48_000)
    }

    @Test
    fun `getMinBufferSize ERROR_BAD_VALUE throws IllegalStateException`() = runTest {
        every {
            AudioRecord.getMinBufferSize(any(), any(), any())
        } returns AudioRecord.ERROR_BAD_VALUE
        val capture = newCapture(factory = { _, _, _, _, _ -> mockk(relaxed = true) })

        try {
            capture.record(durationSec = 1)
            fail("Expected IllegalStateException for ERROR_BAD_VALUE")
        } catch (ise: IllegalStateException) {
            assertTrue(ise.message?.contains("getMinBufferSize") == true)
        }
    }

    @Test
    fun `record throws when AudioRecord state is uninitialized`() = runTest {
        val audioRecord = mockk<AudioRecord>(relaxed = true)
        every { audioRecord.state } returns AudioRecord.STATE_UNINITIALIZED
        val capture = newCapture(factory = { _, _, _, _, _ -> audioRecord })

        try {
            capture.record(durationSec = 1)
            fail("Expected IllegalStateException when AudioRecord is uninitialized")
        } catch (ise: IllegalStateException) {
            assertTrue(ise.message?.contains("failed to initialize") == true)
        }
        verify(exactly = 1) { audioRecord.release() }
    }

    @Test
    fun `record throws when startRecording fails to enter RECORDING state`() = runTest {
        val audioRecord = mockk<AudioRecord>(relaxed = true)
        every { audioRecord.state } returns AudioRecord.STATE_INITIALIZED
        every { audioRecord.recordingState } returns AudioRecord.RECORDSTATE_STOPPED
        val capture = newCapture(factory = { _, _, _, _, _ -> audioRecord })

        try {
            capture.record(durationSec = 1)
            fail("Expected IllegalStateException when startRecording fails")
        } catch (ise: IllegalStateException) {
            assertTrue(ise.message?.contains("failed to start") == true)
        }
        verify(exactly = 1) { audioRecord.release() }
    }

    @Test
    fun `record returns ShortArray of expected length`() = runTest {
        val audioRecord = newRecordingMock(sampleValue = 0)
        val capture = newCapture(factory = { _, _, _, _, _ -> audioRecord })

        val result = capture.record(durationSec = 1)

        assertEquals(44_100, result.size)
        verify(exactly = 1) { audioRecord.stop() }
        verify(exactly = 1) { audioRecord.release() }
    }

    @Test
    fun `stop and release are called even when read returns an error`() = runTest {
        val audioRecord = mockk<AudioRecord>(relaxed = true)
        every { audioRecord.state } returns AudioRecord.STATE_INITIALIZED
        every { audioRecord.recordingState } returns AudioRecord.RECORDSTATE_RECORDING
        every {
            audioRecord.read(any<ShortArray>(), any(), any())
        } returns AudioRecord.ERROR_DEAD_OBJECT
        val capture = newCapture(factory = { _, _, _, _, _ -> audioRecord })

        try {
            capture.record(durationSec = 1)
            fail("Expected IllegalStateException for ERROR_DEAD_OBJECT")
        } catch (ise: IllegalStateException) {
            assertTrue(ise.message?.contains("error code") == true)
        }
        verify(exactly = 1) { audioRecord.release() }
    }

    @Test
    fun `record honors coroutine cancellation and still releases`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val audioRecord = newRecordingMock(sampleValue = 7)
        val capture = newCapture(
            ioDispatcher = dispatcher,
            factory = { _, _, _, _, _ -> audioRecord },
        )

        val job = launch(dispatcher) {
            try {
                capture.record(durationSec = 30)
            } catch (_: Throwable) {
            }
        }
        advanceTimeBy(1)
        job.cancel()
        advanceUntilIdle()

        verify(atLeast = 1) { audioRecord.release() }
    }

    @Test
    fun `factory is invoked with MIC source and mono PCM_16BIT format`() = runTest {
        val audioRecord = newRecordingMock(sampleValue = 0)
        var capturedSource = -1
        var capturedRate = -1
        var capturedChannel = -1
        var capturedFormat = -1
        var capturedBufferBytes = -1
        val capture = newCapture(factory = { source, rate, channel, format, bytes ->
            capturedSource = source
            capturedRate = rate
            capturedChannel = channel
            capturedFormat = format
            capturedBufferBytes = bytes
            audioRecord
        })

        capture.record(durationSec = 1)

        assertEquals(MediaRecorder.AudioSource.MIC, capturedSource)
        assertEquals(44_100, capturedRate)
        assertEquals(AudioFormat.CHANNEL_IN_MONO, capturedChannel)
        assertEquals(AudioFormat.ENCODING_PCM_16BIT, capturedFormat)
        assertEquals(DEFAULT_MIN_BUFFER_BYTES * 2, capturedBufferBytes)
    }

    @Test
    fun `record returns buffer populated by the read loop`() = runTest {
        val audioRecord = newRecordingMock(sampleValue = 123)
        val capture = newCapture(factory = { _, _, _, _, _ -> audioRecord })

        val result = capture.record(durationSec = 1)

        assertNotNull(result)
        assertEquals(44_100, result.size)
        assertTrue(
            "Expected captured buffer to contain at least one non zero sample written by read loop",
            result.any { it == 123.toShort() }
        )
    }

    private fun assertConstructorRejects(sampleRateHz: Int) {
        try {
            AndroidAudioCapture(sampleRateHz = sampleRateHz)
            fail("Expected IllegalArgumentException for sampleRateHz=$sampleRateHz")
        } catch (iae: IllegalArgumentException) {
            assertTrue(iae.message?.contains("44") == true)
        }
    }

    private fun newCapture(
        ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = UnconfinedTestDispatcher(),
        factory: (Int, Int, Int, Int, Int) -> AudioRecord,
    ): AndroidAudioCapture {
        val af = object : AndroidAudioCapture.AudioRecordFactory {
            override fun create(
                audioSource: Int,
                sampleRateHz: Int,
                channelConfig: Int,
                audioFormat: Int,
                bufferSizeInBytes: Int,
            ): AudioRecord = factory(
                audioSource,
                sampleRateHz,
                channelConfig,
                audioFormat,
                bufferSizeInBytes,
            )
        }
        return AndroidAudioCapture(
            sampleRateHz = AndroidAudioCapture.SAMPLE_RATE_HZ,
            ioDispatcher = ioDispatcher,
            audioRecordFactory = af,
        )
    }

    private fun newRecordingMock(sampleValue: Short): AudioRecord {
        val audioRecord = mockk<AudioRecord>(relaxed = true)
        every { audioRecord.state } returns AudioRecord.STATE_INITIALIZED
        every { audioRecord.recordingState } returns AudioRecord.RECORDSTATE_RECORDING
        every {
            audioRecord.read(any<ShortArray>(), any(), any())
        } answers {
            val dest = arg<ShortArray>(0)
            val offsetInShorts = arg<Int>(1)
            val sizeInShorts = arg<Int>(2)
            for (i in 0 until sizeInShorts) {
                dest[offsetInShorts + i] = sampleValue
            }
            sizeInShorts
        }
        return audioRecord
    }

    private companion object {
        const val DEFAULT_MIN_BUFFER_BYTES = 4_096
    }
}
