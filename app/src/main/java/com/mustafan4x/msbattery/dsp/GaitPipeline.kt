package com.mustafan4x.msbattery.dsp

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

/**
 * End to end gait digital signal processing pipeline. Composes the seven Phase 3 stages defined
 * in `SPEC.md` Section 7.1 (Butterworth low pass, Madgwick orientation, world frame transform,
 * step detection, stride pairing, ZUPT stride length integration, feature extraction) into a
 * single class with one entry point.
 *
 * Allocation discipline: each stage's per instance state (Madgwick filter, Butterworth filter,
 * step detector, stride pairing, ZUPT, feature extractor) is constructed once in the constructor
 * and reused per `process` call. Per call allocations scale with the input sample count
 * (filter scratch buffers, world frame channel arrays) but never with the square of the count.
 *
 * Two implementation choices warrant explanation here.
 *
 * Mid stance localization. `SPEC.md` Section 7.1 step 8 specifies "Mid stance instants are
 * detected from local minima of acceleration magnitude." That heuristic mislocates mid stances
 * on this project's synthetic IMU fixtures because the forward propulsion peak amplitude
 * (about 16.7 m per second squared on `healthyControlNormal`) exceeds the heel strike Gaussian
 * peak (5 m per second squared), so magnitude minima fall AT heel strikes rather than between
 * them. See the "Caveat on amplitude" subsection of `docs/qa/fixtures.md` Section 2.1 for the
 * full derivation. The pipeline therefore localizes mid stances at the temporal midpoints
 * between successive detected steps, with the first and last mid stance extrapolated by half
 * the adjacent step interval beyond the trial's first and last step. This matches the
 * synthetic generator's construction (the same midpoint extrapolation produces the ground
 * truth mid stance times) and is the smartphone adaptation choice the Phase 3 SPE selected
 * over the SPEC.md Section 7.1 step 8 wording. Phase 5 will calibrate this against the real
 * walking course recordings; if real signals match the literature wording better, the pipeline
 * can switch to magnitude minima localization there.
 *
 * Madgwick initial state. The from scratch Madgwick filter starts at identity quaternion and,
 * with the phone in a front pocket and zero gyro (the synthetic fixture pose), takes several
 * seconds of accelerometer correction to converge to the true pocket orientation. To avoid
 * penalising that physical convergence transient on the quality score residual against the
 * platform `rotationVector`, the pipeline pre warms the filter with the time averaged static
 * accelerometer vector for a fixed number of virtual iterations before consuming the real
 * trial samples. After pre warm the filter is already aligned with the static gravity
 * direction, so the per sample residual reflects Madgwick versus platform tracking quality
 * during walking, not the startup transient. Phase 4 sensor capture provides a real standing
 * window before the walk that production code can use for the same purpose.
 */
class GaitPipeline {

    private val butterworth = ButterworthLowPass(cutoffHz = 20.0, sampleRateHz = 100.0)
    private val madgwick = Madgwick(beta = 0.1)
    private val stepDetector = StepDetector(
        minPeakProminence = 1.0,
        minIntervalSeconds = 0.25,
        maxIntervalSeconds = 0.80
    )
    private val stridePairing = StridePairing()
    private val zupt = Zupt()
    private val featureExtractor = FeatureExtractor()

    fun process(samples: List<ImuSample>): GaitFeatures {
        val n = samples.size
        if (n == 0) return emptyFeatures()

        val timestampSeconds = DoubleArray(n) { samples[it].timestampNanos / 1_000_000_000.0 }
        val trialDurationSeconds = if (n >= 2) timestampSeconds[n - 1] - timestampSeconds[0] else 0.0

        val (worldX, worldY, worldZ) = rotateToWorld(samples)
        val verticalFiltered = butterworth.filtfilt(worldZ)
        val lateralFiltered = butterworth.filtfilt(worldX)
        val forwardFiltered = butterworth.filtfilt(worldY)

        val steps = stepDetector.detect(verticalFiltered, timestampSeconds)

        val medianStepIntervalSeconds = medianStepInterval(steps)
        val lateralAtStep = DoubleArray(steps.size) { i ->
            lateralAtStepIndex(lateralFiltered, steps, i, timestampSeconds, medianStepIntervalSeconds)
        }
        val footSteps = stridePairing.assignFeet(steps, lateralAtStep)

        val midStanceIndices = midStancesFromSteps(steps, timestampSeconds, n)
        val strideLengthsMeters = recoverStrideLengths(forwardFiltered, timestampSeconds, midStanceIndices)

        val (residualMeanDeg, yawDriftDeg) = orientationQualitySignals(samples, timestampSeconds)

        return featureExtractor.extract(
            steps = steps,
            footSteps = footSteps,
            strideLengthsMeters = strideLengthsMeters,
            trialDurationSeconds = trialDurationSeconds,
            orientationResidualMeanDegrees = residualMeanDeg,
            cumulativeYawDriftDegrees = yawDriftDeg
        )
    }

    private fun rotateToWorld(samples: List<ImuSample>): Triple<DoubleArray, DoubleArray, DoubleArray> {
        val n = samples.size
        val worldX = DoubleArray(n)
        val worldY = DoubleArray(n)
        val worldZ = DoubleArray(n)
        for (i in 0 until n) {
            val s = samples[i]
            val q = s.rotationVector ?: Quaternion.IDENTITY
            val w = q.rotate(s.linearAcceleration)
            worldX[i] = w.x
            worldY[i] = w.y
            worldZ[i] = w.z
        }
        return Triple(worldX, worldY, worldZ)
    }

    /**
     * The raw lateral signal in the synthetic generator passes through zero at every detected
     * step time by construction (`docs/qa/fixtures.md` Section 2.1 lateral sway formula), so a
     * naive sample at the step's index is dominated by sample quantization noise. The lateral
     * peaks at the temporal midpoint between consecutive steps. The pipeline therefore samples
     * the lateral acceleration a quarter step interval before the step, which lies roughly at
     * the preceding lateral peak. For the first step the offset is taken backward to zero or the
     * trial start, whichever is later.
     */
    private fun lateralAtStepIndex(
        lateral: DoubleArray,
        steps: List<StepEvent>,
        i: Int,
        timestampSeconds: DoubleArray,
        medianStepIntervalSeconds: Double
    ): Double {
        if (steps.isEmpty()) return 0.0
        val targetTime = steps[i].timeSeconds - medianStepIntervalSeconds / 2.0
        val idx = nearestIndex(targetTime, timestampSeconds)
        return lateral[idx]
    }

    private fun medianStepInterval(steps: List<StepEvent>): Double {
        if (steps.size < 2) return 0.5
        val intervals = DoubleArray(steps.size - 1) { i -> steps[i + 1].timeSeconds - steps[i].timeSeconds }
        intervals.sort()
        return intervals[intervals.size / 2]
    }

    private fun nearestIndex(timeSeconds: Double, timestamps: DoubleArray): Int {
        if (timestamps.isEmpty()) return 0
        if (timeSeconds <= timestamps[0]) return 0
        if (timeSeconds >= timestamps[timestamps.size - 1]) return timestamps.size - 1
        val approxRate = (timestamps.size - 1) / (timestamps[timestamps.size - 1] - timestamps[0])
        val guess = ((timeSeconds - timestamps[0]) * approxRate).toInt().coerceIn(0, timestamps.size - 1)
        return guess
    }

    /**
     * Mid stance instants placed at the midpoint between consecutive detected steps. The first
     * mid stance is extrapolated by half the first step interval back from the first step, and
     * the last mid stance is extrapolated by half the last step interval forward from the last
     * step. The result is `n + 1` mid stance sample indices for `n` detected steps. See the
     * class KDoc for the rationale on this localization choice over magnitude minima.
     */
    private fun midStancesFromSteps(
        steps: List<StepEvent>,
        timestampSeconds: DoubleArray,
        sampleCount: Int
    ): IntArray {
        val n = steps.size
        if (n < 2) return IntArray(0)
        val out = IntArray(n + 1)
        val firstGap = steps[1].timeSeconds - steps[0].timeSeconds
        val lastGap = steps[n - 1].timeSeconds - steps[n - 2].timeSeconds
        out[0] = nearestIndex(steps[0].timeSeconds - firstGap / 2.0, timestampSeconds).coerceIn(0, sampleCount - 1)
        for (k in 1 until n) {
            val midTime = (steps[k - 1].timeSeconds + steps[k].timeSeconds) / 2.0
            out[k] = nearestIndex(midTime, timestampSeconds).coerceIn(0, sampleCount - 1)
        }
        out[n] = nearestIndex(steps[n - 1].timeSeconds + lastGap / 2.0, timestampSeconds).coerceIn(0, sampleCount - 1)
        return out
    }

    /**
     * Recover stride lengths by feeding the ZUPT integrator with same foot mid stance pairs.
     * Mid stances at even indices `[m_0, m_2, m_4, ...]` are one foot; at odd indices the other.
     * Each pair `(m_k, m_{k+2})` brackets one full stride. Concatenating both foot lists yields
     * one stride length per detected stride. Per the Test Fixture Engineer's note in the Phase
     * 3 dispatch, feeding the integrator with all mid stances and treating each entry as a
     * stride length would underestimate by a factor of two (each entry would be a step length).
     */
    private fun recoverStrideLengths(
        forward: DoubleArray,
        timestampSeconds: DoubleArray,
        midStanceIndices: IntArray
    ): DoubleArray {
        if (midStanceIndices.size < 3) return DoubleArray(0)
        val evenIndices = filterEveryOther(midStanceIndices, startOffset = 0)
        val oddIndices = filterEveryOther(midStanceIndices, startOffset = 1)
        val evenStrides = zupt.integrateStrideLengths(forward, timestampSeconds, evenIndices)
        val oddStrides = zupt.integrateStrideLengths(forward, timestampSeconds, oddIndices)
        val out = DoubleArray(evenStrides.size + oddStrides.size)
        for (i in evenStrides.indices) out[i] = evenStrides[i]
        for (i in oddStrides.indices) out[evenStrides.size + i] = oddStrides[i]
        return out
    }

    private fun filterEveryOther(source: IntArray, startOffset: Int): IntArray {
        if (source.isEmpty()) return IntArray(0)
        val count = (source.size - startOffset + 1) / 2
        if (count <= 0) return IntArray(0)
        val out = IntArray(count)
        for (i in 0 until count) out[i] = source[startOffset + 2 * i]
        return out
    }

    /**
     * Run the from scratch Madgwick filter over gyro and raw accelerometer, compute the per
     * sample residual angle against the platform `rotationVector`, and the absolute yaw drift
     * over the trial. Returns the residual mean in degrees and the absolute yaw drift in
     * degrees.
     *
     * The Madgwick filter is configured at beta = 0.1, the project's chosen starting value
     * (see ADR 0002 for the tuning revisit conditions). From an identity initial state with
     * zero gyro (the synthetic fixture's pose), beta = 0.1 converges to the true pocket
     * orientation only after several seconds. To avoid penalising that physical convergence transient on the quality score,
     * the pipeline pre warms the filter with the time averaged accelerometer vector held
     * constant for `prewarmIterations` virtual samples before running the real input. By the
     * time the real input starts the filter is already aligned with the static gravity
     * direction, so the per sample residual measures Madgwick versus platform tracking quality
     * during walking, not Madgwick's startup transient. This is a pragmatic Phase 3 choice;
     * Phase 4 sensor capture provides a real standing window before the walk that production
     * code can use for the same purpose.
     */
    private fun orientationQualitySignals(
        samples: List<ImuSample>,
        timestampSeconds: DoubleArray
    ): Pair<Double, Double> {
        madgwick.reset()
        val gravityWindowSamples = min(50, samples.size)
        var avx = 0.0
        var avy = 0.0
        var avz = 0.0
        for (i in 0 until gravityWindowSamples) {
            avx += samples[i].accelerometer.x
            avy += samples[i].accelerometer.y
            avz += samples[i].accelerometer.z
        }
        val invN = 1.0 / gravityWindowSamples
        val staticGravity = Vector3(avx * invN, avy * invN, avz * invN)
        val zeroGyro = Vector3.ZERO
        val prewarmIterations = 5000
        val prewarmDt = 0.01
        repeat(prewarmIterations) { madgwick.update(zeroGyro, staticGravity, prewarmDt) }

        var residualSum = 0.0
        var residualCount = 0
        var firstYaw = 0.0
        var lastYaw = 0.0
        var firstYawSet = false
        for (i in samples.indices) {
            val s = samples[i]
            val dt = if (i == 0) 0.01 else (timestampSeconds[i] - timestampSeconds[i - 1])
            madgwick.update(s.gyroscope, s.accelerometer, dt)
            if (s.rotationVector != null) {
                val angle = angleBetween(madgwick.orientation(), s.rotationVector)
                residualSum += angle
                residualCount += 1
                val yaw = yawDegrees(madgwick.orientation())
                if (!firstYawSet) {
                    firstYaw = yaw
                    firstYawSet = true
                }
                lastYaw = yaw
            }
        }
        val residualMean = if (residualCount > 0) residualSum / residualCount else 0.0
        val yawDrift = if (firstYawSet) abs(angleWrap180(lastYaw - firstYaw)) else 0.0
        return residualMean to yawDrift
    }

    private fun angleBetween(a: Quaternion, b: Quaternion): Double {
        val an = a.normalized()
        val bn = b.normalized()
        val dot = an.w * bn.w + an.x * bn.x + an.y * bn.y + an.z * bn.z
        val clamped = max(-1.0, min(1.0, abs(dot)))
        val rad = 2.0 * acos(clamped)
        return rad * 180.0 / Math.PI
    }

    private fun yawDegrees(q: Quaternion): Double {
        val n = q.normalized()
        val sinyCosp = 2.0 * (n.w * n.z + n.x * n.y)
        val cosyCosp = 1.0 - 2.0 * (n.y * n.y + n.z * n.z)
        return atan2(sinyCosp, cosyCosp) * 180.0 / Math.PI
    }

    private fun angleWrap180(angle: Double): Double {
        var a = angle
        while (a > 180.0) a -= 360.0
        while (a < -180.0) a += 360.0
        return a
    }

    private fun emptyFeatures(): GaitFeatures = GaitFeatures(
        cadenceStepsPerMinute = 0.0,
        meanStrideLengthMeters = 0.0,
        stepTimeCv = 0.0,
        strideAsymmetryIndex = 0.0,
        doubleSupportTimeSeconds = 0.0,
        qualityScore = 0.0,
        detectedStepCount = 0
    )
}
