package com.mustafanazeer.baselinems.dsp

import com.mustafanazeer.baselinems.fixtures.PreCannedFixtures
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class StepDetectorTest {
    @Test
    fun `detects approximately the expected number of steps in a healthy control normal walk`() {
        val samples = PreCannedFixtures.healthyControlNormal().generate().toList()
        val verticalWorld = samples.map {
            val q = it.rotationVector!!
            q.rotate(it.linearAcceleration).z
        }
        val filtered = ButterworthLowPass(cutoffHz = 20.0, sampleRateHz = 100.0)
            .filtfilt(verticalWorld.toDoubleArray())
        val timestamps = samples.map { it.timestampNanos / 1_000_000_000.0 }
        val steps = StepDetector(
            minPeakProminence = 1.0,
            minIntervalSeconds = 0.25,
            maxIntervalSeconds = 0.80
        ).detect(filtered, timestamps.toDoubleArray())
        val expected = (115.2 * 30.0 / 60.0).toInt()
        assertTrue("expected approximately $expected steps, got ${steps.size}", abs(steps.size - expected) <= 2)
    }

    @Test
    fun `respects the minimum inter peak distance`() {
        val signal = DoubleArray(1000)
        for (i in signal.indices) {
            if (i % 5 == 0) signal[i] = 5.0
        }
        val timestamps = DoubleArray(1000) { it * 0.01 }
        val steps = StepDetector(
            minPeakProminence = 1.0,
            minIntervalSeconds = 0.25,
            maxIntervalSeconds = 0.80
        ).detect(signal, timestamps)
        for (i in 1 until steps.size) {
            assertTrue(steps[i].timeSeconds - steps[i - 1].timeSeconds >= 0.25 - 1e-9)
        }
    }
}
