package com.mustafan4x.msbattery.fixtures

import com.mustafan4x.msbattery.dsp.Vector3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.roundToInt

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

    @Test
    fun `strideLengthRoundTrip recovers strideLengthMeters within 1 percent`() {
        val cadence = 100.0
        val sampleRate = 100.0
        val durationSeconds = 30.0
        val strideLength = 1.40
        val gen = SyntheticImu(
            cadenceStepsPerMinute = cadence,
            strideLengthMeters = strideLength,
            asymmetryRatio = 1.0,
            stepTimeCv = 0.0,
            durationSeconds = durationSeconds,
            noiseLevelMps2 = 0.0,
            sampleRateHz = sampleRate,
            seed = 42L
        )
        val samples = gen.generate().toList()

        val meanStepIntervalSeconds = 60.0 / cadence
        val stepTimes = gen.computeStepTimes(meanStepIntervalSeconds)
        val midStances = gen.computeMidStances(stepTimes)

        val timestampSeconds = DoubleArray(samples.size) {
            samples[it].timestampNanos / 1_000_000_000.0
        }
        val forwardWorld = DoubleArray(samples.size) { samples[it].linearAcceleration.z }

        val sameFootStartIndex = nearestSampleIndex(midStances[2], sampleRate)
        val sameFootEndIndex = nearestSampleIndex(midStances[4], sampleRate)
        val recovered = trapezoidalDoubleIntegrate(
            forward = forwardWorld,
            timestamp = timestampSeconds,
            startIndex = sameFootStartIndex,
            endIndex = sameFootEndIndex
        )
        val tolerance = 0.01 * strideLength
        assertTrue(
            "expected stride length within 1%% of $strideLength, got $recovered",
            abs(recovered - strideLength) <= tolerance
        )
    }

    @Test
    fun `asymmetryRatioRoundTrip recovers dominant over nonDominant ratio within 1 percent`() {
        val cadence = 100.0
        val asymmetryRatio = 1.10
        val gen = SyntheticImu(
            cadenceStepsPerMinute = cadence,
            strideLengthMeters = 1.30,
            asymmetryRatio = asymmetryRatio,
            stepTimeCv = 0.0,
            durationSeconds = 30.0,
            noiseLevelMps2 = 0.0,
            sampleRateHz = 100.0,
            seed = 11L
        )
        gen.generate().toList()

        val meanStepIntervalSeconds = 60.0 / cadence
        val stepTimes = gen.computeStepTimes(meanStepIntervalSeconds)

        var sumDominant = 0.0
        var countDominant = 0
        var sumNonDominant = 0.0
        var countNonDominant = 0
        for (i in 1 until stepTimes.size) {
            val stepTime = stepTimes[i] - stepTimes[i - 1]
            if (i % 2 == 0) {
                sumDominant += stepTime
                countDominant++
            } else {
                sumNonDominant += stepTime
                countNonDominant++
            }
        }
        val meanDominant = sumDominant / countDominant
        val meanNonDominant = sumNonDominant / countNonDominant
        val recoveredRatio = meanDominant / meanNonDominant
        val tolerance = 0.01 * asymmetryRatio
        assertTrue(
            "expected dominant/nonDominant within 1%% of $asymmetryRatio, got $recoveredRatio",
            abs(recoveredRatio - asymmetryRatio) <= tolerance
        )
    }

    private fun nearestSampleIndex(timeSeconds: Double, sampleRateHz: Double): Int {
        return (timeSeconds * sampleRateHz).roundToInt()
    }

    private fun trapezoidalDoubleIntegrate(
        forward: DoubleArray,
        timestamp: DoubleArray,
        startIndex: Int,
        endIndex: Int
    ): Double {
        if (endIndex <= startIndex) return 0.0
        var velocity = 0.0
        var displacement = 0.0
        for (i in startIndex + 1..endIndex) {
            val dt = timestamp[i] - timestamp[i - 1]
            val accelMid = 0.5 * (forward[i] + forward[i - 1])
            val velocityNext = velocity + accelMid * dt
            displacement += 0.5 * (velocity + velocityNext) * dt
            velocity = velocityNext
        }
        return abs(displacement)
    }
}
