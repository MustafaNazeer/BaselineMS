package com.mustafanazeer.baselinems.dsp.voice

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Six acoustic features extracted from a `PraatPitch.Result`.
 *
 * Operational definitions:
 *
 * `jitter_local` (Praat manual Voice 2): average absolute difference between
 * consecutive voiced cycle periods divided by the average period.
 *
 * `shimmer_local` (Praat manual Voice 3): average absolute difference between
 * the peak amplitudes of consecutive voiced cycles divided by the average
 * amplitude.
 *
 * `hnr_db` (Boersma 1993 *IFA Proceedings* 17:97 to 110 equation 4):
 * `10 * log10(r / (1 - r))` where `r` is the mean normalized autocorrelation
 * peak across voiced frames.
 *
 * `f0_mean_hz` and `f0_sd_hz`: arithmetic mean and population standard
 * deviation of the per voiced frame F0 values from `PraatPitch.Result.frames`.
 *
 * `speaking_rate_wpm`: `passageWordCount / voicedSeconds * 60`. Caller passes
 * the known passage word count (132 for the Grandfather Passage). The estimator
 * assumes the user read the full passage; partial reads inflate WPM. The
 * Phase 9 Reporting layer surfaces a caveat.
 *
 * `pause_fraction`: `(totalSeconds - voicedSeconds) / totalSeconds`. Always
 * defined; no null path.
 *
 * Null semantics. `jitter`, `shimmer`, `hnr`, `f0_mean`, and `f0_sd` return
 * null when the underlying pitch evidence is insufficient (fewer than 50
 * voiced periods for jitter and shimmer; voiced fraction below 5 percent for
 * HNR; no voiced frames for F0). The quality layer in `VoiceQuality.kt`
 * surfaces the per feature flag rather than overloading these scalars with
 * sentinel values.
 *
 * Pure Kotlin. No Android imports. No mutable global state.
 */
data class VoiceFeatureSet(
    val jitterLocal: Double?,
    val shimmerLocal: Double?,
    val hnrDb: Double?,
    val f0MeanHz: Double?,
    val f0SdHz: Double?,
    val speakingRateWpm: Double?,
    val pauseFraction: Double,
    val voicedSeconds: Double,
    val totalSeconds: Double,
    val periodCount: Int,
)

/**
 * Qualitative voice steadiness banding consumed by the Done screen.
 *
 * The cutoffs are anchored to the Praat manual Voice 2 (jitter) and Voice 3
 * (shimmer) thresholds widened for the project's connected speech paradigm.
 * See the Clinical Outcomes Reviewer Phase 8 phase close sign off
 * (`docs/source/clinical-references.md` lines 905 to 909) for the cutoff
 * derivation and the option (a) recommendation this enum realizes.
 */
enum class VoiceSteadinessBand { STEADY, MOSTLY_STEADY, VARIED, UNMEASURABLE }

object VoiceFeatures {

    private const val MIN_VOICED_PERIODS_FOR_PERTURBATION: Int = 50
    private const val MIN_VOICED_FRACTION_FOR_HNR: Double = 0.05
    private const val LN_TEN: Double = 2.302585092994046

    fun compute(
        pitchResult: PraatPitch.Result,
        sampleRateHz: Int,
        passageWordCount: Int,
    ): VoiceFeatureSet {
        require(sampleRateHz > 0) { "sampleRateHz must be positive" }
        require(passageWordCount >= 0) { "passageWordCount must be non negative" }

        val frames = pitchResult.frames
        val voicedFrames = frames.filter { it.isVoiced }
        val totalFrames = frames.size

        val totalSeconds = if (frames.size >= 2) {
            frames.last().timeSec - frames.first().timeSec +
                (frames[1].timeSec - frames[0].timeSec)
        } else {
            0.0
        }
        val voicedFraction = if (totalFrames == 0) 0.0 else {
            voicedFrames.size.toDouble() / totalFrames
        }
        val voicedSeconds = totalSeconds * voicedFraction
        val pauseFraction = if (totalFrames == 0) 1.0 else 1.0 - voicedFraction

        val periods = pitchResult.periods
        val peaks = pitchResult.periodPeakAmplitudes
        val periodCount = periods.size
        val hasMinimumVoicing = voicedFraction >= MIN_VOICED_FRACTION_FOR_HNR

        val jitter = if (hasMinimumVoicing && periodCount >= MIN_VOICED_PERIODS_FOR_PERTURBATION) {
            jitterLocal(periods)
        } else {
            null
        }
        val shimmer = if (hasMinimumVoicing && peaks.size >= MIN_VOICED_PERIODS_FOR_PERTURBATION) {
            shimmerLocal(peaks)
        } else {
            null
        }

        val hnr = if (hasMinimumVoicing && voicedFrames.isNotEmpty()) {
            hnrDb(voicedFrames.map { it.autocorrelationPeak })
        } else {
            null
        }

        val f0Mean: Double?
        val f0Sd: Double?
        if (!hasMinimumVoicing || voicedFrames.isEmpty()) {
            f0Mean = null
            f0Sd = null
        } else {
            val f0List = voicedFrames.mapNotNull { it.f0Hz }
            if (f0List.isEmpty()) {
                f0Mean = null
                f0Sd = null
            } else {
                val mean = f0List.average()
                f0Mean = mean
                var sumSq = 0.0
                for (v in f0List) sumSq += (v - mean) * (v - mean)
                f0Sd = sqrt(sumSq / f0List.size)
            }
        }

        val speakingRate = if (hasMinimumVoicing && passageWordCount > 0 && voicedSeconds > 0.0) {
            passageWordCount / voicedSeconds * 60.0
        } else {
            null
        }

        return VoiceFeatureSet(
            jitterLocal = jitter,
            shimmerLocal = shimmer,
            hnrDb = hnr,
            f0MeanHz = f0Mean,
            f0SdHz = f0Sd,
            speakingRateWpm = speakingRate,
            pauseFraction = pauseFraction,
            voicedSeconds = voicedSeconds,
            totalSeconds = totalSeconds,
            periodCount = periodCount,
        )
    }

    /**
     * Derives a qualitative steadiness band from `jitterLocal` and `shimmerLocal`.
     *
     * Cutoffs per Clinical Outcomes Reviewer Phase 8 phase close sign off
     * (`docs/source/clinical-references.md` lines 905 to 909): the STEADY band
     * rounds the Farago sustained vowel pathology threshold down for the
     * connected speech paradigm; the MOSTLY_STEADY band approximates two times
     * the Farago thresholds; values outside both bands fall into VARIED. When
     * either feature is null (insufficient voicing, insufficient periods, or
     * the pitch detector returned no usable evidence), the band reports
     * UNMEASURABLE so the Done screen copy does not assert a steadiness
     * judgement the data cannot support.
     */
    fun steadinessBand(features: VoiceFeatureSet): VoiceSteadinessBand {
        val j = features.jitterLocal
        val s = features.shimmerLocal
        if (j == null || s == null) return VoiceSteadinessBand.UNMEASURABLE
        return when {
            j < 0.010 && s < 0.040 -> VoiceSteadinessBand.STEADY
            j < 0.020 && s < 0.070 -> VoiceSteadinessBand.MOSTLY_STEADY
            else -> VoiceSteadinessBand.VARIED
        }
    }

    private fun jitterLocal(periods: DoubleArray): Double {
        if (periods.size < 2) return 0.0
        var absDiffSum = 0.0
        var meanSum = 0.0
        for (i in periods.indices) {
            meanSum += periods[i]
            if (i > 0) absDiffSum += abs(periods[i] - periods[i - 1])
        }
        val meanPeriod = meanSum / periods.size
        val meanAbsDiff = absDiffSum / (periods.size - 1)
        if (meanPeriod <= 0.0) return 0.0
        return meanAbsDiff / meanPeriod
    }

    private fun shimmerLocal(peaks: DoubleArray): Double {
        if (peaks.size < 2) return 0.0
        var absDiffSum = 0.0
        var meanSum = 0.0
        for (i in peaks.indices) {
            meanSum += peaks[i]
            if (i > 0) absDiffSum += abs(peaks[i] - peaks[i - 1])
        }
        val meanPeak = meanSum / peaks.size
        val meanAbsDiff = absDiffSum / (peaks.size - 1)
        if (meanPeak <= 0.0) return 0.0
        return meanAbsDiff / meanPeak
    }

    private fun hnrDb(autocorrelationPeaks: List<Double>): Double {
        if (autocorrelationPeaks.isEmpty()) return Double.NEGATIVE_INFINITY
        var sum = 0.0
        var n = 0
        for (r in autocorrelationPeaks) {
            if (r.isFinite() && r >= 0.0) {
                sum += r
                n++
            }
        }
        if (n == 0) return Double.NEGATIVE_INFINITY
        val r = sum / n
        // Clamp to keep log argument strictly positive. Boersma 1993 equation 4
        // diverges as r approaches 1.0; in practice synthetic clean signals
        // reach r approximately 0.999 and produce HNR around 30 dB. Clamping at
        // r = 0.99999 caps the headline HNR at approximately 50 dB which is well
        // above any clinically meaningful threshold.
        val rClamped = when {
            r >= 0.99999 -> 0.99999
            r <= 0.0 -> return Double.NEGATIVE_INFINITY
            else -> r
        }
        return 10.0 * (ln(rClamped / max(1e-12, 1.0 - rClamped)) / LN_TEN)
    }
}
