package com.mustafanazeer.baselinems.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.tan

/**
 * 4th order zero phase Butterworth low pass filter.
 *
 * Implementation: two cascaded biquads designed by the bilinear transform of the analog
 * Butterworth prototype. The two Q values come from the canonical 4th order pole locations
 * (Q1 = 1/(2 cos(pi/8)), Q2 = 1/(2 cos(3 pi/8))). Zero phase is achieved by running the
 * cascade forward over the input then again backward over the output (the filtfilt method).
 *
 * The design point this project ships is 20 Hz cutoff at a 100 Hz sample rate. The constructor
 * accepts cutoffHz and sampleRateHz to keep tests parameterizable.
 *
 * Allocation behavior: per call to filtfilt, allocates two temporary DoubleArray buffers
 * sized to the input plus a small fixed amount of edge padding for transient suppression.
 * The biquad state itself lives in mutable fields and does not allocate during the per sample
 * inner loop. Callers that need allocation free per sample processing should provide their own
 * outer buffer; the gait capture path collects samples for 30 seconds before processing, so a
 * single batch allocation is acceptable.
 */
class ButterworthLowPass(
    cutoffHz: Double,
    sampleRateHz: Double
) {
    private val stage1: Biquad
    private val stage2: Biquad

    init {
        require(cutoffHz > 0.0 && cutoffHz < sampleRateHz / 2.0) {
            "cutoffHz must satisfy 0 < cutoffHz < Nyquist (sampleRateHz / 2)"
        }
        val q1 = 1.0 / (2.0 * cos(PI / 8.0))
        val q2 = 1.0 / (2.0 * cos(3.0 * PI / 8.0))
        stage1 = Biquad.lowPass(cutoffHz, sampleRateHz, q1)
        stage2 = Biquad.lowPass(cutoffHz, sampleRateHz, q2)
    }

    fun filtfilt(input: DoubleArray): DoubleArray {
        val n = input.size
        if (n == 0) return DoubleArray(0)

        val padLen = minOf(64, n)
        val firstValue = input[0]
        val lastValue = input[n - 1]

        val padded = DoubleArray(n + 2 * padLen)
        for (i in 0 until padLen) padded[i] = 2.0 * firstValue - input[padLen - i]
        for (i in 0 until n) padded[padLen + i] = input[i]
        for (i in 0 until padLen) padded[padLen + n + i] = 2.0 * lastValue - input[n - 2 - i]

        val forward = DoubleArray(padded.size)
        stage1.reset()
        for (i in padded.indices) forward[i] = stage1.process(padded[i])
        stage2.reset()
        for (i in forward.indices) forward[i] = stage2.process(forward[i])

        val reverse = DoubleArray(padded.size)
        stage1.reset()
        for (i in forward.indices.reversed()) reverse[i] = stage1.process(forward[i])
        stage2.reset()
        for (i in reverse.indices.reversed()) reverse[i] = stage2.process(reverse[i])

        val out = DoubleArray(n)
        for (i in 0 until n) out[i] = reverse[padLen + i]
        return out
    }

    private class Biquad(
        private val b0: Double,
        private val b1: Double,
        private val b2: Double,
        private val a1: Double,
        private val a2: Double
    ) {
        private var z1: Double = 0.0
        private var z2: Double = 0.0

        fun reset() {
            z1 = 0.0
            z2 = 0.0
        }

        fun process(x: Double): Double {
            val out = b0 * x + z1
            z1 = b1 * x - a1 * out + z2
            z2 = b2 * x - a2 * out
            return out
        }

        companion object {
            fun lowPass(cutoffHz: Double, sampleRateHz: Double, q: Double): Biquad {
                val w0 = 2.0 * PI * cutoffHz / sampleRateHz
                val k = tan(w0 / 2.0)
                val k2 = k * k
                val norm = 1.0 + k / q + k2
                val b0 = k2 / norm
                val b1 = 2.0 * k2 / norm
                val b2 = k2 / norm
                val a1 = 2.0 * (k2 - 1.0) / norm
                val a2 = (1.0 - k / q + k2) / norm
                return Biquad(b0, b1, b2, a1, a2)
            }
        }
    }
}
