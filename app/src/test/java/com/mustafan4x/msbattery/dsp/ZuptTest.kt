package com.mustafan4x.msbattery.dsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ZuptTest {
    @Test
    fun `recovers stride length within 2 percent on a half sine velocity profile`() {
        // Hand constructed synthetic stride: forward acceleration is a cosine that integrates
        // to a half sine velocity profile. The closed form stride displacement is the integral
        // of v(t) = peakVelocity * sin(pi * t / T) from 0 to T, which is 2 * peakVelocity * T / pi.
        val strideDurationSeconds = 1.0
        val expected = 1.442
        val peakVelocity = expected * Math.PI / (2.0 * strideDurationSeconds)
        val sampleRateHz = 100.0
        val nSamples = (strideDurationSeconds * sampleRateHz).toInt() + 1
        val forwardAccel = DoubleArray(nSamples) { i ->
            val t = i / sampleRateHz
            peakVelocity * (Math.PI / strideDurationSeconds) * kotlin.math.cos(Math.PI * t / strideDurationSeconds)
        }
        val timestampSeconds = DoubleArray(nSamples) { it / sampleRateHz }
        val midStanceIndices = intArrayOf(0, nSamples - 1)

        val recovered = Zupt().integrateStrideLengths(forwardAccel, timestampSeconds, midStanceIndices)
        assertEquals(1, recovered.size)
        val percentError = 100.0 * abs(recovered[0] - expected) / expected
        assertTrue("percent error $percentError exceeded 2 (recovered ${recovered[0]}, expected $expected)", percentError <= 2.0)
    }

    @Test
    fun `recovers multiple stride lengths in a sequence`() {
        // Three identical strides back to back. Each stride has the same closed form length
        // computed from a half sine velocity profile.
        val strideDurationSeconds = 1.0
        val expected = 1.0
        val peakVelocity = expected * Math.PI / (2.0 * strideDurationSeconds)
        val sampleRateHz = 100.0
        val perStride = (strideDurationSeconds * sampleRateHz).toInt()
        val totalSamples = perStride * 3 + 1
        val forwardAccel = DoubleArray(totalSamples) { i ->
            val tInStride = (i % perStride).toDouble() / sampleRateHz
            peakVelocity * (Math.PI / strideDurationSeconds) * kotlin.math.cos(Math.PI * tInStride / strideDurationSeconds)
        }
        val timestampSeconds = DoubleArray(totalSamples) { it / sampleRateHz }
        val midStanceIndices = intArrayOf(0, perStride, 2 * perStride, 3 * perStride)

        val recovered = Zupt().integrateStrideLengths(forwardAccel, timestampSeconds, midStanceIndices)
        assertEquals(3, recovered.size)
        for (r in recovered) {
            assertTrue("recovered $r vs expected $expected", abs(r - expected) / expected <= 0.02)
        }
    }

    @Test
    fun `returns empty list when fewer than two mid stance indices supplied`() {
        val recovered = Zupt().integrateStrideLengths(
            forwardAccel = DoubleArray(100),
            timestampSeconds = DoubleArray(100) { it * 0.01 },
            midStanceIndices = intArrayOf(50)
        )
        assertTrue(recovered.isEmpty())
    }
}
