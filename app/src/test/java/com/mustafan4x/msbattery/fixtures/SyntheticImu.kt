package com.mustafan4x.msbattery.fixtures

import com.mustafan4x.msbattery.dsp.ImuSample
import com.mustafan4x.msbattery.dsp.Quaternion
import com.mustafan4x.msbattery.dsp.Vector3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
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
 *  3. Forward propulsion on the world Y axis, modeled as a sinusoid at the step frequency.
 *
 * Asymmetry is implemented by scaling the time between consecutive same foot strides; the
 * asymmetryRatio is the ratio of mean dominant step time to mean non dominant step time.
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

        val orientation = pocketOrientationQuaternion()

        for (i in 0 until totalSamples) {
            val t = i / sampleRateHz
            val world = worldFrameAccelerationAt(t, stepTimes)
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

    private fun computeStepTimes(meanStepIntervalSeconds: Double): DoubleArray {
        val nSteps = (cadenceStepsPerMinute * durationSeconds / 60.0).toInt()
        val out = DoubleArray(nSteps)
        var t = 0.0
        for (i in 0 until nSteps) {
            val isDominant = i % 2 == 0
            val mean = if (isDominant) meanStepIntervalSeconds * asymmetryRatio
                       else meanStepIntervalSeconds * (2.0 - asymmetryRatio)
            val jitter = if (stepTimeCv > 0.0) gaussian() * stepTimeCv * mean else 0.0
            t += mean + jitter
            out[i] = t
        }
        return out
    }

    private fun worldFrameAccelerationAt(t: Double, stepTimes: DoubleArray): Vector3 {
        val sigma = 0.04
        var az = 0.0
        for (st in stepTimes) {
            val dt = t - st
            az += 5.0 * exp(-(dt * dt) / (2.0 * sigma * sigma))
        }
        val cadenceHz = cadenceStepsPerMinute / 60.0
        val ax = 1.0 * sin(2.0 * PI * cadenceHz * t / 2.0)
        val ay = 0.5 * cos(2.0 * PI * cadenceHz * t)
        return Vector3(ax, ay, az)
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

    private fun Quaternion.inverse(): Quaternion = Quaternion(w, -x, -y, -z).normalized()
}
