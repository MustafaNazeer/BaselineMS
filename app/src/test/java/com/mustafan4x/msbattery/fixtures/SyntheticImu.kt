package com.mustafan4x.msbattery.fixtures

import com.mustafan4x.msbattery.dsp.ImuSample
import com.mustafan4x.msbattery.dsp.Quaternion
import com.mustafan4x.msbattery.dsp.Vector3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Parameterized synthetic IMU trace generator.
 *
 * The phone is modeled as fixed in the front pocket with the screen facing the body. The world
 * frame Z axis is up. The device frame at rest is x to the right edge of the phone, y to the
 * top of the phone, z out of the screen. With the phone in the front pocket, the device z
 * axis points forward in the world frame, the device y axis points down, and the device x
 * axis points to the body's right.
 *
 * Three signals are superposed in the world frame:
 *  1. Heel strike spikes on the world Z axis (vertical), one per step, modeled as a narrow
 *     Gaussian centered at the step time with amplitude proportional to gravity.
 *  2. Lateral sway on the world X axis, alternating sign per step (left foot to right foot).
 *  3. Forward propulsion on the world Y axis, modeled as a piecewise sinusoidal acceleration
 *     calibrated so that the double integral over one stride (two steps) equals
 *     `strideLengthMeters`. The trial timeline is partitioned into inter mid stance segments
 *     bounded by mid stance instants placed midway between consecutive heel strikes (with the
 *     first and last mid stance extrapolated by half a step beyond the trial's first and last
 *     heel strike). Within each segment of duration `T_seg`, the forward velocity follows a
 *     sin squared profile `v(tau) = (strideLengthMeters / (2 * T_seg)) * (1 - cos(2 pi tau /
 *     T_seg))`, so velocity is exactly zero at every mid stance and the per segment displacement
 *     is exactly `strideLengthMeters / 2` (one step length). Two consecutive segments cover one
 *     full stride and integrate to `strideLengthMeters`. The acceleration is the time derivative
 *     of this profile, `a(tau) = (pi * strideLengthMeters / T_seg^2) * sin(2 pi tau / T_seg)`,
 *     which is continuous (no step jumps at segment boundaries) and which is exactly zero at
 *     each heel strike (heel strikes lie at the segment midpoints `tau = T_seg / 2`).
 *
 * Asymmetry is implemented by setting `dominantStepTime = baseMean * sqrt(asymmetryRatio)` and
 * `nonDominantStepTime = baseMean / sqrt(asymmetryRatio)`. This makes
 * `dominantStepTime / nonDominantStepTime = asymmetryRatio` exactly while preserving the
 * geometric mean cadence, so the documented per fixture cadence value remains accurate even at
 * non unit asymmetry ratios.
 *
 * Step time CV is implemented by jittering each step time by a Gaussian with the requested
 * coefficient of variation around the mean step interval.
 *
 * Noise is additive Gaussian on each accelerometer and gyroscope axis at the requested level.
 */
class SyntheticImu(
    private val cadenceStepsPerMinute: Double,
    private val strideLengthMeters: Double,
    private val asymmetryRatio: Double,
    private val stepTimeCv: Double,
    private val durationSeconds: Double,
    private val noiseLevelMps2: Double,
    private val sampleRateHz: Double,
    private val seed: Long
) {
    private val rng = Random(seed)

    fun generate(): Sequence<ImuSample> = sequence {
        val totalSamples = (durationSeconds * sampleRateHz).toInt()
        val sampleDeltaNanos = (1_000_000_000.0 / sampleRateHz).toLong()

        val meanStepIntervalSeconds = 60.0 / cadenceStepsPerMinute
        val stepTimes = computeStepTimes(meanStepIntervalSeconds)
        val midStances = computeMidStances(stepTimes)

        val orientation = pocketOrientationQuaternion()

        for (i in 0 until totalSamples) {
            val t = i / sampleRateHz
            val world = worldFrameAccelerationAt(t, stepTimes, midStances)
            val gravityWorld = Vector3(0.0, 0.0, 9.80665)
            val deviceLinear = orientation.inverse().rotate(world)
            val deviceGravity = orientation.inverse().rotate(gravityWorld)
            val deviceRaw = deviceLinear + deviceGravity
            val gyro = Vector3.ZERO
            val noisyAccel = deviceRaw.addNoise()
            val noisyGyro = gyro.addNoise()
            val noisyLinear = deviceLinear.addNoise()
            yield(
                ImuSample(
                    timestampNanos = i.toLong() * sampleDeltaNanos,
                    accelerometer = noisyAccel,
                    gyroscope = noisyGyro,
                    linearAcceleration = noisyLinear,
                    rotationVector = orientation
                )
            )
        }
    }

    /**
     * Compute heel strike times. Step `i` is dominant when `i % 2 == 0`. Dominant step time and
     * non dominant step time are the geometric square root pair `(baseMean * sqrt(r),
     * baseMean / sqrt(r))` so that `dominantStepTime / nonDominantStepTime = asymmetryRatio`
     * exactly while the geometric mean stays at `baseMean` (preserving cadence). Step time
     * jitter, when `stepTimeCv > 0`, is Gaussian with std `mean * stepTimeCv` per step.
     */
    internal fun computeStepTimes(meanStepIntervalSeconds: Double): DoubleArray {
        val nSteps = (cadenceStepsPerMinute * durationSeconds / 60.0).toInt()
        val out = DoubleArray(nSteps)
        val rootRatio = sqrt(asymmetryRatio)
        val dominantMean = meanStepIntervalSeconds * rootRatio
        val nonDominantMean = meanStepIntervalSeconds / rootRatio
        var t = 0.0
        for (i in 0 until nSteps) {
            val isDominant = i % 2 == 0
            val mean = if (isDominant) dominantMean else nonDominantMean
            val jitter = if (stepTimeCv > 0.0) gaussian() * stepTimeCv * mean else 0.0
            t += mean + jitter
            out[i] = t
        }
        return out
    }

    /**
     * Mid stance instants placed at the midpoint between consecutive heel strikes, with the
     * first and last mid stance extrapolated by half the adjacent inter step interval beyond
     * the first and last heel strike. The result is `nSteps + 1` mid stances bounding `nSteps`
     * inter mid stance segments, each containing exactly one heel strike at its midpoint.
     */
    internal fun computeMidStances(stepTimes: DoubleArray): DoubleArray {
        val n = stepTimes.size
        if (n < 2) return DoubleArray(0)
        val out = DoubleArray(n + 1)
        out[0] = (3.0 * stepTimes[0] - stepTimes[1]) / 2.0
        for (k in 1 until n) {
            out[k] = (stepTimes[k - 1] + stepTimes[k]) / 2.0
        }
        out[n] = (3.0 * stepTimes[n - 1] - stepTimes[n - 2]) / 2.0
        return out
    }

    private fun worldFrameAccelerationAt(
        t: Double,
        stepTimes: DoubleArray,
        midStances: DoubleArray
    ): Vector3 {
        val sigma = 0.04
        var az = 0.0
        for (st in stepTimes) {
            val dt = t - st
            az += 5.0 * exp(-(dt * dt) / (2.0 * sigma * sigma))
        }
        val cadenceHz = cadenceStepsPerMinute / 60.0
        val ax = 1.0 * sin(2.0 * PI * cadenceHz * t / 2.0)
        val ay = forwardAcceleration(t, midStances)
        return Vector3(ax, ay, az)
    }

    /**
     * Closed form forward acceleration from the sin squared velocity profile per inter mid
     * stance segment described in the class KDoc. Outside the trial's first and last mid stance
     * the body is treated as stationary so the trial begins and ends with zero forward velocity.
     */
    private fun forwardAcceleration(t: Double, midStances: DoubleArray): Double {
        if (midStances.size < 2) return 0.0
        if (t < midStances[0] || t >= midStances[midStances.size - 1]) return 0.0
        var k = 0
        while (k < midStances.size - 1 && midStances[k + 1] <= t) {
            k++
        }
        val tSeg = midStances[k + 1] - midStances[k]
        val tau = t - midStances[k]
        return (PI * strideLengthMeters / (tSeg * tSeg)) * sin(2.0 * PI * tau / tSeg)
    }

    private fun pocketOrientationQuaternion(): Quaternion =
        Quaternion(w = cos(PI / 4.0), x = -sin(PI / 4.0), y = 0.0, z = 0.0)

    private fun gaussian(): Double {
        val u1 = rng.nextDouble().coerceAtLeast(1e-12)
        val u2 = rng.nextDouble()
        return kotlin.math.sqrt(-2.0 * kotlin.math.ln(u1)) * cos(2.0 * PI * u2)
    }

    private fun Vector3.addNoise(): Vector3 {
        if (noiseLevelMps2 <= 0.0) return this
        return Vector3(
            x + gaussian() * noiseLevelMps2,
            y + gaussian() * noiseLevelMps2,
            z + gaussian() * noiseLevelMps2
        )
    }
}
