package com.mustafan4x.msbattery.fixtures

import com.mustafan4x.msbattery.dsp.Vector3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SyntheticImuTest {
    @Test
    fun `generator produces the expected sample count for 30 seconds at 100 Hz`() {
        val samples = SyntheticImu(
            cadenceStepsPerMinute = 100.0,
            strideLengthMeters = 1.40,
            asymmetryRatio = 1.0,
            stepTimeCv = 0.0,
            durationSeconds = 30.0,
            noiseLevelMps2 = 0.0,
            sampleRateHz = 100.0,
            seed = 42L
        ).generate().toList()
        assertEquals(3000, samples.size)
    }

    @Test
    fun `generator produces evenly spaced timestamps`() {
        val samples = SyntheticImu(
            cadenceStepsPerMinute = 100.0,
            strideLengthMeters = 1.40,
            asymmetryRatio = 1.0,
            stepTimeCv = 0.0,
            durationSeconds = 1.0,
            noiseLevelMps2 = 0.0,
            sampleRateHz = 100.0,
            seed = 42L
        ).generate().toList()
        val expectedDeltaNanos = 10_000_000L
        for (i in 1 until samples.size) {
            assertEquals(expectedDeltaNanos, samples[i].timestampNanos - samples[i - 1].timestampNanos)
        }
    }

    @Test
    fun `linearAcceleration plus gravity equals raw accelerometer in the device frame`() {
        val samples = SyntheticImu(
            cadenceStepsPerMinute = 100.0,
            strideLengthMeters = 1.40,
            asymmetryRatio = 1.0,
            stepTimeCv = 0.0,
            durationSeconds = 1.0,
            noiseLevelMps2 = 0.0,
            sampleRateHz = 100.0,
            seed = 42L
        ).generate().toList()
        for (s in samples) {
            val q = s.rotationVector!!
            val qInv = com.mustafan4x.msbattery.dsp.Quaternion(q.w, -q.x, -q.y, -q.z).normalized()
            val deviceGravity = qInv.rotate(Vector3(0.0, 0.0, 9.80665))
            val reconstructed = s.linearAcceleration + deviceGravity
            assertTrue(abs(reconstructed.x - s.accelerometer.x) < 1e-9)
            assertTrue(abs(reconstructed.y - s.accelerometer.y) < 1e-9)
            assertTrue(abs(reconstructed.z - s.accelerometer.z) < 1e-9)
        }
    }

    @Test
    fun `peak count of vertical linear acceleration matches expected step count`() {
        val cadence = 100.0
        val durationSeconds = 30.0
        val expectedSteps = (cadence * durationSeconds / 60.0).toInt()
        val samples = SyntheticImu(
            cadenceStepsPerMinute = cadence,
            strideLengthMeters = 1.40,
            asymmetryRatio = 1.0,
            stepTimeCv = 0.0,
            durationSeconds = durationSeconds,
            noiseLevelMps2 = 0.0,
            sampleRateHz = 100.0,
            seed = 42L
        ).generate().toList()
        val peaks = countLocalMaxima(samples.map { it.linearAcceleration.z })
        val tolerance = 1
        assertTrue(
            "expected approximately $expectedSteps peaks, got $peaks",
            abs(peaks - expectedSteps) <= tolerance
        )
    }

    private fun countLocalMaxima(series: List<Double>): Int {
        var count = 0
        for (i in 1 until series.size - 1) {
            if (series[i] > series[i - 1] && series[i] > series[i + 1] && series[i] > 0.5) {
                count++
            }
        }
        return count
    }
}
