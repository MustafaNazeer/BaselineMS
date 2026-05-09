package com.mustafanazeer.baselinems.dsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class ButterworthLowPassTest {
    @Test
    fun `passes a 1 Hz tone with near unity amplitude`() {
        val fs = 100.0
        val n = 1000
        val signal = DoubleArray(n) { i -> sin(2.0 * PI * 1.0 * i / fs) }
        val filtered = ButterworthLowPass(cutoffHz = 20.0, sampleRateHz = fs).filtfilt(signal)
        val rmsIn = rms(signal)
        val rmsOut = rms(filtered)
        assertTrue(abs(rmsOut / rmsIn - 1.0) < 0.05)
    }

    @Test
    fun `attenuates a 40 Hz tone by at least 12 dB`() {
        val fs = 100.0
        val n = 1000
        val signal = DoubleArray(n) { i -> sin(2.0 * PI * 40.0 * i / fs) }
        val filtered = ButterworthLowPass(cutoffHz = 20.0, sampleRateHz = fs).filtfilt(signal)
        val rmsIn = rms(signal)
        val rmsOut = rms(filtered)
        val ratio = rmsOut / rmsIn
        assertTrue("ratio = $ratio", ratio < 0.25)
    }

    @Test
    fun `is approximately zero phase on a delta input`() {
        val n = 200
        val signal = DoubleArray(n).also { it[100] = 1.0 }
        val filtered = ButterworthLowPass(cutoffHz = 20.0, sampleRateHz = 100.0).filtfilt(signal)
        val argMax = filtered.indices.maxByOrNull { filtered[it] } ?: -1
        assertEquals(100, argMax)
    }

    private fun rms(x: DoubleArray): Double {
        var s = 0.0
        for (v in x) s += v * v
        return kotlin.math.sqrt(s / x.size)
    }
}
