package com.mustafanazeer.baselinems.battery.voice

import com.mustafanazeer.baselinems.signals.VoiceWavWriter
import java.io.File

/**
 * Decides whether a completed voice recording's raw PCM is persisted for personal review and, if
 * so, writes it. Implements the ADR 0006 Section 7 opt in retention path.
 *
 * The seam exists so `VoiceTestViewModel` can hand the captured buffer to a single collaborator
 * before the SE2 buffer zeroing `finally` runs, without the ViewModel needing to know about the
 * Settings toggle, the lock screen state, the session id, or the on disk path. The gating
 * decision (toggle on AND `KeyguardManager.isDeviceSecure()` at session start) is resolved by the
 * caller that constructs the implementation; this layer only acts on the resolved decision.
 */
interface VoiceAudioRetention {
    /**
     * Persists [pcm] if retention is enabled and permitted. Must never throw: any I/O failure
     * degrades to the discard path so a failed retention write cannot crash the test or block
     * feature extraction and result persistence (ADR 0006 Section 7). Must not mutate [pcm];
     * the caller still owns the buffer and zeroes it afterward (SE2).
     */
    fun persist(pcm: ShortArray)
}

/** Discard path. Used when the toggle is off or the device is no longer secure. */
object NoOpVoiceAudioRetention : VoiceAudioRetention {
    override fun persist(pcm: ShortArray) = Unit
}

/**
 * Writes the gzipped WAV to `${filesDir}/audio_traces/<sessionId>/VOICE.wav.gz` when [enabled]
 * and [deviceSecureAtStart] both hold. The [deviceSecureAtStart] flag is the
 * `KeyguardManager.isDeviceSecure()` value captured at the start of the voice session: if the
 * user opted in earlier but has since removed their lock screen credential, this is false and the
 * session falls back to discard per ADR 0006 Section 7.
 */
class FileVoiceAudioRetention(
    private val enabled: Boolean,
    private val deviceSecureAtStart: Boolean,
    private val sessionId: String,
    private val filesDir: File,
    private val sampleRateHz: Int,
) : VoiceAudioRetention {

    override fun persist(pcm: ShortArray) {
        if (!enabled || !deviceSecureAtStart) return
        val target = File(
            filesDir,
            "${VoiceSettings.AUDIO_TRACES_DIR}/$sessionId/$VOICE_FILE_NAME",
        )
        runCatching {
            VoiceWavWriter.writeGzippedWav(pcm = pcm, sampleRateHz = sampleRateHz, target = target)
        }
    }

    companion object {
        const val VOICE_FILE_NAME: String = "VOICE.wav.gz"
    }
}
