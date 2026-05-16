package com.mustafanazeer.baselinems.dsp.voice

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Autocorrelation pitch detector after Boersma 1993 *IFA Proceedings* 17:97 to 110.
 *
 * Implements Sections 2 through 4 of the paper: Hanning windowing (equation 6),
 * normalized autocorrelation of the windowed signal divided by the
 * autocorrelation of the window itself (equation 7), local maximum search for
 * the pitch period in the lag range corresponding to adult voice F0, and
 * voiced unvoiced classification by comparing the normalized autocorrelation
 * peak against a threshold (default 0.45 per Praat).
 *
 * The cosine windowing correction in equation 7 is the load bearing distinction
 * between Boersma's algorithm and naive autocorrelation pitch tracking. Without
 * the correction, the autocorrelation envelope decays with lag because the
 * windowed signal loses energy at large lags; dividing by the autocorrelation
 * of the window itself removes that systematic decay and the local maximum at
 * the true period is no longer competing with the (artificially boosted)
 * maximum at small lags.
 *
 * Per period extraction. The frame level analysis (40 ms windows, 10 ms hops at
 * 44.1 kHz) gives a per frame F0 estimate and an autocorrelation peak height.
 * Jitter and shimmer downstream need the actual cycle by cycle period sequence
 * within each voiced segment, not the smoothed per frame estimate. The stitched
 * `periods` and `periodPeakAmplitudes` arrays are produced by walking each
 * contiguous voiced segment cycle by cycle: starting at the first signal peak
 * inside the segment, repeatedly searching forward by approximately the locally
 * detected period (with a tolerance band) for the next signal peak, recording
 * each peak to peak interval as a cycle period and each peak height as the
 * cycle peak amplitude. The walker uses the per frame F0 of the nearest frame
 * as the local expected period.
 *
 * All arithmetic is `Double`; the package is JVM testable with no Android imports.
 */
object PraatPitch {

    private const val DEFAULT_SAMPLE_RATE_HZ: Int = 44_100
    private const val DEFAULT_WINDOW_MS: Double = 40.0
    private const val DEFAULT_HOP_MS: Double = 10.0
    private const val DEFAULT_F0_MIN_HZ: Double = 60.0
    private const val DEFAULT_F0_MAX_HZ: Double = 500.0
    private const val DEFAULT_VOICING_THRESHOLD: Double = 0.45

    /**
     * One frame of pitch analysis.
     *
     * @param timeSec center time of the analysis window in seconds.
     * @param f0Hz fundamental frequency in Hz; null if unvoiced.
     * @param periodSec period in seconds (`1.0 / f0Hz`); null if unvoiced.
     * @param isVoiced classification result against `voicingThreshold`.
     * @param autocorrelationPeak normalized autocorrelation peak in the F0
     *   search range; 0.0 if no peak exists in the band.
     */
    data class Frame(
        val timeSec: Double,
        val f0Hz: Double?,
        val periodSec: Double?,
        val isVoiced: Boolean,
        val autocorrelationPeak: Double,
    )

    /**
     * Full pitch analysis result.
     *
     * @param frames the per frame analysis; one entry per hop.
     * @param periods the stitched cycle by cycle period sequence in seconds,
     *   covering only voiced frames. Order is chronological; consecutive
     *   entries within the same voiced segment form the input to jitter local.
     * @param periodPeakAmplitudes the cycle by cycle peak amplitude sequence
     *   aligned 1 to 1 with `periods`. Order is chronological; consecutive
     *   entries within the same voiced segment form the input to shimmer local.
     */
    data class Result(
        val frames: List<Frame>,
        val periods: DoubleArray,
        val periodPeakAmplitudes: DoubleArray,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Result) return false
            return frames == other.frames &&
                periods.contentEquals(other.periods) &&
                periodPeakAmplitudes.contentEquals(other.periodPeakAmplitudes)
        }

        override fun hashCode(): Int {
            var h = frames.hashCode()
            h = 31 * h + periods.contentHashCode()
            h = 31 * h + periodPeakAmplitudes.contentHashCode()
            return h
        }
    }

    fun analyze(
        samples: ShortArray,
        sampleRateHz: Int = DEFAULT_SAMPLE_RATE_HZ,
        windowMs: Double = DEFAULT_WINDOW_MS,
        hopMs: Double = DEFAULT_HOP_MS,
        f0MinHz: Double = DEFAULT_F0_MIN_HZ,
        f0MaxHz: Double = DEFAULT_F0_MAX_HZ,
        voicingThreshold: Double = DEFAULT_VOICING_THRESHOLD,
    ): Result {
        require(sampleRateHz > 0) { "sampleRateHz must be positive" }
        require(windowMs > 0.0 && hopMs > 0.0) { "windowMs and hopMs must be positive" }
        require(f0MinHz > 0.0 && f0MaxHz > f0MinHz) {
            "f0MaxHz must exceed f0MinHz and both must be positive"
        }
        require(voicingThreshold in 0.0..1.0) { "voicingThreshold must lie in [0, 1]" }

        val windowSamples = max(2, (windowMs * 1e-3 * sampleRateHz).roundToInt())
        val hopSamples = max(1, (hopMs * 1e-3 * sampleRateHz).roundToInt())
        val minLag = max(1, (sampleRateHz / f0MaxHz).roundToInt())
        val maxLag = min(windowSamples - 1, (sampleRateHz / f0MinHz).roundToInt())

        if (samples.size < windowSamples || maxLag <= minLag) {
            return Result(emptyList(), DoubleArray(0), DoubleArray(0))
        }

        val window = hanningWindow(windowSamples)
        val windowAutocorr = autocorrelation(window, maxLag)

        val frames = ArrayList<Frame>()
        var frameStart = 0
        while (frameStart + windowSamples <= samples.size) {
            val frame = analyzeFrame(
                samples = samples,
                frameStart = frameStart,
                windowSamples = windowSamples,
                window = window,
                windowAutocorr = windowAutocorr,
                minLag = minLag,
                maxLag = maxLag,
                sampleRateHz = sampleRateHz,
                voicingThreshold = voicingThreshold,
            )
            frames.add(frame)
            frameStart += hopSamples
        }

        val (periods, peaks) = stitchPeriods(samples, frames, hopSamples, sampleRateHz)
        return Result(frames, periods, peaks)
    }

    private fun analyzeFrame(
        samples: ShortArray,
        frameStart: Int,
        windowSamples: Int,
        window: DoubleArray,
        windowAutocorr: DoubleArray,
        minLag: Int,
        maxLag: Int,
        sampleRateHz: Int,
        voicingThreshold: Double,
    ): Frame {
        val centerSec = (frameStart + windowSamples / 2.0) / sampleRateHz

        var mean = 0.0
        for (i in 0 until windowSamples) mean += samples[frameStart + i].toDouble()
        mean /= windowSamples

        val frame = DoubleArray(windowSamples)
        for (i in 0 until windowSamples) {
            frame[i] = (samples[frameStart + i].toDouble() - mean) * window[i]
        }

        val rx = autocorrelation(frame, maxLag)
        val rx0 = rx[0]
        if (rx0 <= 0.0) {
            return Frame(centerSec, null, null, false, 0.0)
        }

        var bestLag = -1
        var bestPeak = 0.0
        for (lag in minLag..maxLag) {
            val wAuto = windowAutocorr[lag]
            if (wAuto <= 0.0) continue
            val normalized = (rx[lag] / rx0) / (wAuto / windowAutocorr[0])
            if (normalized > bestPeak &&
                isLocalMaximum(rx, lag, minLag, maxLag)
            ) {
                bestPeak = normalized
                bestLag = lag
            }
        }

        if (bestLag < 0 || bestPeak < voicingThreshold) {
            return Frame(centerSec, null, null, false, max(0.0, bestPeak))
        }

        val refinedLag = parabolicRefine(rx, bestLag)
        val periodSec = refinedLag / sampleRateHz
        val f0Hz = 1.0 / periodSec
        return Frame(centerSec, f0Hz, periodSec, true, min(bestPeak, 1.0))
    }

    private fun isLocalMaximum(rx: DoubleArray, lag: Int, minLag: Int, maxLag: Int): Boolean {
        if (lag <= minLag) return rx[lag] > rx[lag + 1]
        if (lag >= maxLag) return rx[lag] > rx[lag - 1]
        return rx[lag] >= rx[lag - 1] && rx[lag] >= rx[lag + 1]
    }

    private fun parabolicRefine(rx: DoubleArray, lag: Int): Double {
        if (lag <= 0 || lag >= rx.size - 1) return lag.toDouble()
        val a = rx[lag - 1]
        val b = rx[lag]
        val c = rx[lag + 1]
        val denom = a - 2.0 * b + c
        if (denom == 0.0) return lag.toDouble()
        val delta = 0.5 * (a - c) / denom
        if (delta < -1.0 || delta > 1.0) return lag.toDouble()
        return lag + delta
    }

    private fun hanningWindow(n: Int): DoubleArray {
        val out = DoubleArray(n)
        if (n == 1) {
            out[0] = 1.0
            return out
        }
        val denom = (n - 1).toDouble()
        for (i in 0 until n) out[i] = 0.5 - 0.5 * cos(2.0 * PI * i / denom)
        return out
    }

    private fun autocorrelation(signal: DoubleArray, maxLag: Int): DoubleArray {
        val n = signal.size
        val cap = min(maxLag, n - 1)
        val out = DoubleArray(cap + 1)
        for (lag in 0..cap) {
            var s = 0.0
            val limit = n - lag
            var i = 0
            while (i < limit) {
                s += signal[i] * signal[i + lag]
                i++
            }
            out[lag] = s
        }
        return out
    }

    private fun stitchPeriods(
        samples: ShortArray,
        frames: List<Frame>,
        hopSamples: Int,
        sampleRateHz: Int,
    ): Pair<DoubleArray, DoubleArray> {
        if (frames.isEmpty()) return Pair(DoubleArray(0), DoubleArray(0))

        val periods = ArrayList<Double>()
        val peaks = ArrayList<Double>()

        var segmentStartFrame = -1
        for (i in frames.indices) {
            val voiced = frames[i].isVoiced
            if (voiced && segmentStartFrame < 0) {
                segmentStartFrame = i
            }
            val segmentEnd = (!voiced || i == frames.size - 1) && segmentStartFrame >= 0
            if (segmentEnd) {
                val lastVoicedFrame = if (voiced) i else i - 1
                walkSegment(
                    samples = samples,
                    frames = frames,
                    hopSamples = hopSamples,
                    sampleRateHz = sampleRateHz,
                    startFrame = segmentStartFrame,
                    endFrameInclusive = lastVoicedFrame,
                    outPeriods = periods,
                    outPeaks = peaks,
                )
                segmentStartFrame = -1
            }
        }

        return Pair(periods.toDoubleArray(), peaks.toDoubleArray())
    }

    private fun walkSegment(
        samples: ShortArray,
        frames: List<Frame>,
        hopSamples: Int,
        sampleRateHz: Int,
        startFrame: Int,
        endFrameInclusive: Int,
        outPeriods: MutableList<Double>,
        outPeaks: MutableList<Double>,
    ) {
        val segmentStartSample = startFrame * hopSamples
        val segmentEndSample = min(samples.size, (endFrameInclusive + 1) * hopSamples)
        if (segmentEndSample - segmentStartSample < 4) return

        val firstPeriodSec = frames[startFrame].periodSec ?: return
        val firstPeriodSamples = (firstPeriodSec * sampleRateHz).roundToInt()
        if (firstPeriodSamples < 2) return

        var cursor = findInitialPeak(samples, segmentStartSample, segmentEndSample, firstPeriodSamples)
        if (cursor < 0) return

        while (true) {
            val frameIndex = min(
                endFrameInclusive,
                max(startFrame, cursor / hopSamples),
            )
            val localPeriodSec = frames[frameIndex].periodSec
                ?: frames[startFrame].periodSec
                ?: break
            val localPeriodSamples = max(2, (localPeriodSec * sampleRateHz).roundToInt())
            val searchLo = cursor + (localPeriodSamples * 0.7).toInt()
            val searchHi = cursor + (localPeriodSamples * 1.3).toInt()
            if (searchHi >= segmentEndSample) break

            val nextPeak = findPeakInRange(samples, searchLo, searchHi)
            if (nextPeak < 0) break

            val periodSamples = nextPeak - cursor
            if (periodSamples < 2) break

            outPeriods.add(periodSamples.toDouble() / sampleRateHz)
            outPeaks.add(abs(samples[cursor].toInt().toDouble()))

            cursor = nextPeak
        }

        if (cursor in segmentStartSample until segmentEndSample) {
            outPeaks.add(abs(samples[cursor].toInt().toDouble()))
            outPeriods.add(outPeriods.lastOrNull() ?: 0.0)
            if (outPeriods.last() <= 0.0 && outPeriods.size >= 2) {
                outPeriods.removeAt(outPeriods.size - 1)
                outPeaks.removeAt(outPeaks.size - 1)
            }
        }
    }

    private fun findInitialPeak(
        samples: ShortArray,
        segmentStart: Int,
        segmentEnd: Int,
        expectedPeriodSamples: Int,
    ): Int {
        val scanEnd = min(segmentEnd, segmentStart + 2 * expectedPeriodSamples)
        return findPeakInRange(samples, segmentStart, scanEnd)
    }

    private fun findPeakInRange(samples: ShortArray, fromInclusive: Int, toInclusive: Int): Int {
        val lo = max(1, fromInclusive)
        val hi = min(samples.size - 2, toInclusive)
        if (lo > hi) return -1
        var bestIdx = -1
        var bestVal = Int.MIN_VALUE
        for (i in lo..hi) {
            val s = samples[i].toInt()
            if (s > samples[i - 1].toInt() && s >= samples[i + 1].toInt() && s > bestVal) {
                bestVal = s
                bestIdx = i
            }
        }
        return bestIdx
    }
}
