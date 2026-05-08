package com.mustafan4x.msbattery.dsp

/**
 * Zero velocity update (ZUPT) stride length integrator per SPEC.md Section 7.1 step 8 and the
 * Skog et al. 2010 ZUPT method (Skog I, Handel P, Nilsson J O, Rantakokko J. 2010. Zero velocity
 * detection: an algorithm evaluation. IEEE Trans Biomed Eng 57(11):2657 to 2666).
 *
 * Given a forward (world frame Y) linear acceleration time series and a list of mid stance
 * sample indices, integrate forward acceleration twice using trapezoidal integration with the
 * velocity reset to zero at each mid stance. The integral over a single stride (one mid stance
 * to the next mid stance of the same foot) yields the stride length in meters.
 *
 * The integration is allocation tolerant: one DoubleArray of stride lengths is allocated per
 * call. The per sample inner loop uses primitive Doubles only.
 */
class Zupt {

    fun integrateStrideLengths(
        forwardAccel: DoubleArray,
        timestampSeconds: DoubleArray,
        midStanceIndices: IntArray
    ): DoubleArray {
        require(forwardAccel.size == timestampSeconds.size) {
            "forwardAccel and timestampSeconds must have the same length"
        }
        if (midStanceIndices.size < 2) return DoubleArray(0)
        val out = DoubleArray(midStanceIndices.size - 1)
        for (k in 0 until midStanceIndices.size - 1) {
            val startIndex = midStanceIndices[k]
            val endIndex = midStanceIndices[k + 1]
            out[k] = integrateOne(forwardAccel, timestampSeconds, startIndex, endIndex)
        }
        return out
    }

    private fun integrateOne(
        forwardAccel: DoubleArray,
        timestampSeconds: DoubleArray,
        startIndex: Int,
        endIndex: Int
    ): Double {
        if (endIndex <= startIndex) return 0.0
        var velocity = 0.0
        var displacement = 0.0
        for (i in startIndex + 1..endIndex) {
            val dt = timestampSeconds[i] - timestampSeconds[i - 1]
            val accelMid = 0.5 * (forwardAccel[i] + forwardAccel[i - 1])
            val velocityNext = velocity + accelMid * dt
            displacement += 0.5 * (velocity + velocityNext) * dt
            velocity = velocityNext
        }
        return kotlin.math.abs(displacement)
    }
}
