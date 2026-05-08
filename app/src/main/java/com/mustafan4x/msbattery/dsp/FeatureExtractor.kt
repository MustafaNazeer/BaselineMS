package com.mustafan4x.msbattery.dsp

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Computes the five SPEC.md Section 7.1 step 8 features plus the quality score from step 9 from
 * the outputs of StepDetector, StridePairing, and Zupt.
 *
 * - Cadence: steps detected divided by trial duration in minutes.
 * - Mean stride length: arithmetic mean of supplied per stride lengths from Zupt.
 * - Step time CV: coefficient of variation (sd / mean) of inter step intervals.
 * - Stride asymmetry index: (meanLeftStepInterval - meanRightStepInterval) / meanOfMeans, the
 *   symmetric mean denominator convention ratified for the Tap Test in the Clinical Validator's
 *   2026-05-07 Phase 2 sign off (`docs/source/clinical-references.md`) and reused here for
 *   cross module consistency.
 * - Double support time: estimated as the mean overlap window between successive opposite foot
 *   steps. The synthetic generator does not model true double support phase boundaries, so this
 *   feature is computed as half the mean step interval, which approximates the published double
 *   support fraction (about 20 percent of stride time in healthy adults, slightly higher in MS).
 *
 * Quality score combines four factors per SPEC.md Section 7.1 step 9:
 *  1. At least 20 steps detected (binary: zero quality if not).
 *  2. Orientation residual stable (penalty if the mean residual angle between Madgwick and the
 *     platform rotation vector exceeds 5 degrees over the trial).
 *  3. Trajectory approximately straight (penalty if cumulative yaw drift exceeds 30 degrees over
 *     the trial).
 *  4. No inter peak gap exceeded the 800 ms maximum (penalty per flagged gap).
 *
 * The 5 degree residual threshold and the 30 degree yaw drift threshold are smartphone
 * adaptation choices not from a single MS paper; they are the heuristic values the SPE chose
 * for Phase 3 quality gating, subject to refinement after the Phase 5 real walking course
 * recordings calibrate the typical residual and drift distributions on a real device.
 */
class FeatureExtractor {

    fun extract(
        steps: List<StepEvent>,
        footSteps: List<FootStep>,
        strideLengthsMeters: DoubleArray,
        trialDurationSeconds: Double,
        orientationResidualMeanDegrees: Double,
        cumulativeYawDriftDegrees: Double
    ): GaitFeatures {
        val cadenceStepsPerMinute = if (trialDurationSeconds > 0.0) {
            steps.size.toDouble() * 60.0 / trialDurationSeconds
        } else {
            0.0
        }

        val meanStrideLengthMeters = if (strideLengthsMeters.isNotEmpty()) {
            strideLengthsMeters.average()
        } else {
            0.0
        }

        val stepIntervals = if (steps.size >= 2) {
            DoubleArray(steps.size - 1) { i -> steps[i + 1].timeSeconds - steps[i].timeSeconds }
        } else {
            DoubleArray(0)
        }
        val stepTimeCv = coefficientOfVariation(stepIntervals)

        val strideAsymmetryIndex = computeAsymmetry(footSteps)
        val doubleSupportTimeSeconds = computeDoubleSupport(stepIntervals)

        val flaggedGapCount = steps.count { it.gapToPreviousExceededMaxInterval }
        val qualityScore = computeQualityScore(
            stepCount = steps.size,
            orientationResidualMeanDegrees = orientationResidualMeanDegrees,
            cumulativeYawDriftDegrees = cumulativeYawDriftDegrees,
            flaggedGapCount = flaggedGapCount
        )

        return GaitFeatures(
            cadenceStepsPerMinute = cadenceStepsPerMinute,
            meanStrideLengthMeters = meanStrideLengthMeters,
            stepTimeCv = stepTimeCv,
            strideAsymmetryIndex = strideAsymmetryIndex,
            doubleSupportTimeSeconds = doubleSupportTimeSeconds,
            qualityScore = qualityScore,
            detectedStepCount = steps.size
        )
    }

    private fun coefficientOfVariation(values: DoubleArray): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        if (mean == 0.0) return 0.0
        var ss = 0.0
        for (v in values) ss += (v - mean) * (v - mean)
        val sd = sqrt(ss / values.size)
        return sd / abs(mean)
    }

    private fun computeAsymmetry(footSteps: List<FootStep>): Double {
        if (footSteps.size < 4) return 0.0
        val leftIntervals = ArrayList<Double>()
        val rightIntervals = ArrayList<Double>()
        for (i in 1 until footSteps.size) {
            val gap = footSteps[i].step.timeSeconds - footSteps[i - 1].step.timeSeconds
            when (footSteps[i].foot) {
                Foot.LEFT -> leftIntervals.add(gap)
                Foot.RIGHT -> rightIntervals.add(gap)
            }
        }
        if (leftIntervals.isEmpty() || rightIntervals.isEmpty()) return 0.0
        val meanLeft = leftIntervals.average()
        val meanRight = rightIntervals.average()
        val denominator = (meanLeft + meanRight) / 2.0
        if (denominator == 0.0) return 0.0
        return (meanLeft - meanRight) / denominator
    }

    private fun computeDoubleSupport(stepIntervals: DoubleArray): Double {
        if (stepIntervals.isEmpty()) return 0.0
        val meanStepInterval = stepIntervals.average()
        return 0.2 * (2.0 * meanStepInterval)
    }

    private fun computeQualityScore(
        stepCount: Int,
        orientationResidualMeanDegrees: Double,
        cumulativeYawDriftDegrees: Double,
        flaggedGapCount: Int
    ): Double {
        if (stepCount < 20) return 0.0

        val residualPenalty = min(1.0, max(0.0, (orientationResidualMeanDegrees - 5.0) / 25.0))
        val yawPenalty = min(1.0, max(0.0, (cumulativeYawDriftDegrees - 30.0) / 60.0))
        val gapPenalty = min(1.0, flaggedGapCount * 0.1)

        val residualScore = 1.0 - residualPenalty
        val yawScore = 1.0 - yawPenalty
        val gapScore = 1.0 - gapPenalty

        val combined = 0.4 * residualScore + 0.3 * yawScore + 0.3 * gapScore
        return min(1.0, max(0.0, combined))
    }
}
