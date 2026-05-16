package com.mustafanazeer.baselinems.battery.voice

import com.mustafanazeer.baselinems.dsp.voice.VoiceFeatureSet
import com.mustafanazeer.baselinems.dsp.voice.VoiceQualityScore

enum class NoiseBand { Pending, Green, Yellow, Red }

sealed interface VoiceTestState {
    data object Instructions : VoiceTestState
    data object RecordAudioRequested : VoiceTestState
    data object RecordAudioDenied : VoiceTestState
    data class AudioQualityCheck(
        val noiseFloorRmsDbFs: Double? = null,
        val band: NoiseBand = NoiseBand.Pending
    ) : VoiceTestState
    data class Running(val elapsedMs: Long) : VoiceTestState
    data class Done(
        val features: VoiceFeatureSet,
        val quality: VoiceQualityScore
    ) : VoiceTestState
    data object Cancelled : VoiceTestState
}
