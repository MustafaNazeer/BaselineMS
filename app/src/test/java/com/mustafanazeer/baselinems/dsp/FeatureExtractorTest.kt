package com.mustafanazeer.baselinems.dsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class FeatureExtractorTest {
    @Test
    fun `cadence is steps per minute computed from step count and trial duration`() {
        val steps = (0 until 30).map {
            StepEvent(timeSeconds = it * 0.5, amplitude = 5.0, sampleIndex = it * 50, gapToPreviousExceededMaxInterval = false)
        }
        val features = FeatureExtractor().extract(
            steps = steps,
            footSteps = steps.mapIndexed { i, s -> FootStep(s, if (i % 2 == 0) Foot.LEFT else Foot.RIGHT) },
            strideLengthsMeters = doubleArrayOf(),
            trialDurationSeconds = 30.0,
            orientationResidualMeanDegrees = 0.0,
            cumulativeYawDriftDegrees = 0.0
        )
        assertEquals(60.0, features.cadenceStepsPerMinute, 1e-9)
    }

    @Test
    fun `mean stride length is the arithmetic mean of supplied stride lengths`() {
        val features = FeatureExtractor().extract(
            steps = synthSteps(20),
            footSteps = synthFootSteps(20),
            strideLengthsMeters = doubleArrayOf(1.40, 1.42, 1.44, 1.46),
            trialDurationSeconds = 10.0,
            orientationResidualMeanDegrees = 0.0,
            cumulativeYawDriftDegrees = 0.0
        )
        assertEquals(1.43, features.meanStrideLengthMeters, 1e-9)
    }

    @Test
    fun `step time CV is the coefficient of variation of step intervals`() {
        val intervals = doubleArrayOf(0.50, 0.52, 0.48, 0.50, 0.51, 0.49)
        val times = DoubleArray(intervals.size + 1)
        for (i in intervals.indices) times[i + 1] = times[i] + intervals[i]
        val steps = times.mapIndexed { i, t ->
            StepEvent(timeSeconds = t, amplitude = 5.0, sampleIndex = i * 50, gapToPreviousExceededMaxInterval = false)
        }
        val mean = intervals.average()
        val sd = kotlin.math.sqrt(intervals.map { (it - mean) * (it - mean) }.average())
        val expectedCv = sd / mean

        val features = FeatureExtractor().extract(
            steps = steps,
            footSteps = steps.mapIndexed { i, s -> FootStep(s, if (i % 2 == 0) Foot.LEFT else Foot.RIGHT) },
            strideLengthsMeters = doubleArrayOf(1.40),
            trialDurationSeconds = 10.0,
            orientationResidualMeanDegrees = 0.0,
            cumulativeYawDriftDegrees = 0.0
        )
        assertEquals(expectedCv, features.stepTimeCv, 1e-9)
    }

    @Test
    fun `asymmetry index uses symmetric mean denominator convention`() {
        val timesLeft = doubleArrayOf(0.0, 1.10, 2.20, 3.30, 4.40, 5.50)
        val timesRight = doubleArrayOf(0.55, 1.65, 2.75, 3.85, 4.95)
        val merged = (timesLeft.map { it to Foot.LEFT } + timesRight.map { it to Foot.RIGHT }).sortedBy { it.first }
        val steps = merged.mapIndexed { i, (t, _) ->
            StepEvent(timeSeconds = t, amplitude = 5.0, sampleIndex = i * 50, gapToPreviousExceededMaxInterval = false)
        }
        val footSteps = merged.mapIndexed { i, (_, f) -> FootStep(steps[i], f) }

        val features = FeatureExtractor().extract(
            steps = steps,
            footSteps = footSteps,
            strideLengthsMeters = doubleArrayOf(1.40),
            trialDurationSeconds = 6.0,
            orientationResidualMeanDegrees = 0.0,
            cumulativeYawDriftDegrees = 0.0
        )
        val leftStepIntervals = listOf(1.10 - 0.55, 2.20 - 1.65, 3.30 - 2.75, 4.40 - 3.85, 5.50 - 4.95)
        val rightStepIntervals = listOf(0.55 - 0.0, 1.65 - 1.10, 2.75 - 2.20, 3.85 - 3.30, 4.95 - 4.40)
        val meanLeft = leftStepIntervals.average()
        val meanRight = rightStepIntervals.average()
        val expected = (meanLeft - meanRight) / ((meanLeft + meanRight) / 2.0)
        assertEquals(expected, features.strideAsymmetryIndex, 1e-9)
    }

    @Test
    fun `quality score is zero when fewer than 20 steps detected`() {
        val features = FeatureExtractor().extract(
            steps = synthSteps(15),
            footSteps = synthFootSteps(15),
            strideLengthsMeters = doubleArrayOf(1.40),
            trialDurationSeconds = 30.0,
            orientationResidualMeanDegrees = 0.0,
            cumulativeYawDriftDegrees = 0.0
        )
        assertEquals(0.0, features.qualityScore, 1e-9)
    }

    @Test
    fun `quality score is one on a clean walk with 50 steps and zero drift and zero residual`() {
        val features = FeatureExtractor().extract(
            steps = synthSteps(50),
            footSteps = synthFootSteps(50),
            strideLengthsMeters = doubleArrayOf(1.40),
            trialDurationSeconds = 30.0,
            orientationResidualMeanDegrees = 0.0,
            cumulativeYawDriftDegrees = 0.0
        )
        assertTrue("quality score ${features.qualityScore} should be near 1", features.qualityScore > 0.95)
    }

    @Test
    fun `toMap exposes all seven features`() {
        val gf = GaitFeatures(
            cadenceStepsPerMinute = 100.0,
            meanStrideLengthMeters = 1.42,
            stepTimeCv = 0.05,
            strideAsymmetryIndex = 0.0,
            doubleSupportTimeSeconds = 0.12,
            qualityScore = 0.9,
            detectedStepCount = 50
        )
        val m = gf.toMap()
        assertEquals(7, m.size)
        assertEquals(100.0, m["cadence_steps_per_minute"]!!, 1e-9)
        assertEquals(1.42, m["mean_stride_length_meters"]!!, 1e-9)
        assertEquals(0.05, m["step_time_cv"]!!, 1e-9)
        assertEquals(0.0, m["stride_asymmetry_index"]!!, 1e-9)
        assertEquals(0.12, m["double_support_time_seconds"]!!, 1e-9)
        assertEquals(0.9, m["quality_score"]!!, 1e-9)
        assertEquals(50.0, m["detected_step_count"]!!, 1e-9)
    }

    private fun synthSteps(n: Int): List<StepEvent> = (0 until n).map {
        StepEvent(timeSeconds = it * 0.6, amplitude = 5.0, sampleIndex = it * 60, gapToPreviousExceededMaxInterval = false)
    }

    private fun synthFootSteps(n: Int): List<FootStep> = synthSteps(n).mapIndexed { i, s ->
        FootStep(s, if (i % 2 == 0) Foot.LEFT else Foot.RIGHT)
    }
}
