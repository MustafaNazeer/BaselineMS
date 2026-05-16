package com.mustafanazeer.baselinems.battery.voice

import com.mustafanazeer.baselinems.dsp.voice.PraatPitch
import com.mustafanazeer.baselinems.dsp.voice.VoiceFeatures
import com.mustafanazeer.baselinems.dsp.voice.VoiceQuality
import com.mustafanazeer.baselinems.signals.AudioCapture
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Arrays

class VoiceTestViewModel(
    private val audioCapture: AudioCapture,
    private val scope: CoroutineScope,
    private val passageWordCount: Int = PASSAGE_WORD_COUNT_DEFAULT,
    private val recordingDurationSec: Int = RECORDING_DURATION_SEC,
    private val sampleRateHz: Int = SAMPLE_RATE_HZ,
    private val clockMs: () -> Long = { System.currentTimeMillis() }
) {

    private val _state: MutableStateFlow<VoiceTestState> =
        MutableStateFlow(VoiceTestState.Instructions)
    val state: StateFlow<VoiceTestState> = _state.asStateFlow()

    private var recordingJob: Job? = null
    private var timerJob: Job? = null
    private var deniedThisSession: Boolean = false

    fun onStart() {
        if (_state.value !is VoiceTestState.Instructions) return
        if (deniedThisSession) {
            _state.value = VoiceTestState.RecordAudioDenied
            return
        }
        _state.value = VoiceTestState.RecordAudioRequested
    }

    fun onPermissionResult(granted: Boolean) {
        if (_state.value !is VoiceTestState.RecordAudioRequested) return
        if (granted) {
            _state.value = VoiceTestState.AudioQualityCheck(
                noiseFloorRmsDbFs = null,
                band = NoiseBand.Pending
            )
        } else {
            deniedThisSession = true
            _state.value = VoiceTestState.RecordAudioDenied
        }
    }

    fun onNoiseFloorMeasured(rmsDbFs: Double) {
        val cur = _state.value as? VoiceTestState.AudioQualityCheck ?: return
        val band = when {
            rmsDbFs <= NOISE_FLOOR_GREEN_DBFS -> NoiseBand.Green
            rmsDbFs <= NOISE_FLOOR_YELLOW_DBFS -> NoiseBand.Yellow
            else -> NoiseBand.Red
        }
        _state.value = cur.copy(noiseFloorRmsDbFs = rmsDbFs, band = band)
    }

    fun onQualityCheckPassed() {
        val cur = _state.value as? VoiceTestState.AudioQualityCheck ?: return
        if (cur.band != NoiseBand.Green && cur.band != NoiseBand.Yellow) return
        _state.value = VoiceTestState.Running(elapsedMs = 0L)
        startTimer()
        recordingJob = scope.launch { runRecording() }
    }

    fun onCancel() {
        recordingJob?.cancel()
        recordingJob = null
        timerJob?.cancel()
        timerJob = null
        _state.value = VoiceTestState.Cancelled
    }

    private fun startTimer() {
        timerJob = scope.launch {
            val startMs = clockMs()
            val totalMs = recordingDurationSec * 1000L
            while (isActive) {
                delay(TIMER_TICK_MS)
                val cur = _state.value as? VoiceTestState.Running ?: return@launch
                val elapsed = clockMs() - startMs
                if (elapsed >= totalMs) {
                    _state.value = cur.copy(elapsedMs = totalMs)
                    return@launch
                }
                _state.value = cur.copy(elapsedMs = elapsed)
            }
        }
    }

    private suspend fun runRecording() {
        var buffer: ShortArray? = null
        try {
            val captured = audioCapture.record(durationSec = recordingDurationSec)
            buffer = captured
            val pitchResult = PraatPitch.analyze(captured, sampleRateHz = sampleRateHz)
            val features = VoiceFeatures.compute(
                pitchResult = pitchResult,
                sampleRateHz = sampleRateHz,
                passageWordCount = passageWordCount
            )
            val quality = VoiceQuality.score(
                features = features,
                samples = captured,
                sampleRateHz = sampleRateHz
            )
            timerJob?.cancel()
            timerJob = null
            _state.value = VoiceTestState.Done(features = features, quality = quality)
        } catch (e: CancellationException) {
            _state.value = VoiceTestState.Cancelled
            throw e
        } catch (e: SecurityException) {
            deniedThisSession = true
            _state.value = VoiceTestState.RecordAudioDenied
        } catch (e: IllegalStateException) {
            _state.value = VoiceTestState.Cancelled
        } finally {
            buffer?.let { Arrays.fill(it, 0.toShort()) }
        }
    }

    companion object {
        const val RECORDING_DURATION_SEC: Int = 30
        const val SAMPLE_RATE_HZ: Int = 44_100
        const val PASSAGE_WORD_COUNT_DEFAULT: Int = 132
        const val NOISE_FLOOR_GREEN_DBFS: Double = -50.0
        const val NOISE_FLOOR_YELLOW_DBFS: Double = -35.0
        const val TIMER_TICK_MS: Long = 100L
    }
}
