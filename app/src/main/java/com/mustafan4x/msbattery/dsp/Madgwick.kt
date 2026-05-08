package com.mustafan4x.msbattery.dsp

import kotlin.math.sqrt

/**
 * Madgwick (2010) IMU orientation filter, implemented from scratch per SPEC.md Section 7.1
 * step 2 ("teaching exercise"). Reference: Madgwick S O H. 2010. An efficient orientation
 * filter for inertial and inertial / magnetic sensor arrays. University of Bristol technical
 * report. The IMU only variant (no magnetometer) is used here.
 *
 * Convention: the filter's quaternion rotates device frame vectors into the world frame, i.e.
 * `orientation().rotate(deviceVec) = worldVec`. This matches the Android Sensor.TYPE_ROTATION_VECTOR
 * convention so the parallel platform fused estimate is a drop in reference for the quality score
 * residual computed in Phase 3 Task 13.
 *
 * State per instance: a single Quaternion (mutable through reassignment). The update method does
 * not allocate Vector3 or Quaternion objects on the per sample path apart from the one new
 * Quaternion that replaces the field; primitive Doubles carry the rest of the work.
 *
 * The default beta gain (0.1) is the value Madgwick reports as a reasonable starting point in
 * the paper. The test suite runs at higher beta (0.5) for fast convergence on the static test
 * and at zero beta for pure gyro integration on the rotation test.
 */
class Madgwick(private val beta: Double = 0.1) {

    private var q0: Double = 1.0
    private var q1: Double = 0.0
    private var q2: Double = 0.0
    private var q3: Double = 0.0

    fun orientation(): Quaternion = Quaternion(q0, q1, q2, q3)

    fun reset() {
        q0 = 1.0
        q1 = 0.0
        q2 = 0.0
        q3 = 0.0
    }

    fun update(gyro: Vector3, accel: Vector3, dtSeconds: Double) {
        val gx = gyro.x
        val gy = gyro.y
        val gz = gyro.z

        var qDotW = 0.5 * (-q1 * gx - q2 * gy - q3 * gz)
        var qDotX = 0.5 * (q0 * gx + q2 * gz - q3 * gy)
        var qDotY = 0.5 * (q0 * gy - q1 * gz + q3 * gx)
        var qDotZ = 0.5 * (q0 * gz + q1 * gy - q2 * gx)

        val accelNorm = sqrt(accel.x * accel.x + accel.y * accel.y + accel.z * accel.z)
        if (accelNorm > 0.0 && beta > 0.0) {
            val ax = accel.x / accelNorm
            val ay = accel.y / accelNorm
            val az = accel.z / accelNorm

            val twoQ0 = 2.0 * q0
            val twoQ1 = 2.0 * q1
            val twoQ2 = 2.0 * q2
            val twoQ3 = 2.0 * q3
            val fourQ1 = 4.0 * q1
            val fourQ2 = 4.0 * q2

            val f1 = twoQ1 * q3 - twoQ0 * q2 - ax
            val f2 = twoQ0 * q1 + twoQ2 * q3 - ay
            val f3 = 1.0 - 2.0 * q1 * q1 - 2.0 * q2 * q2 - az

            val gradW = -twoQ2 * f1 + twoQ1 * f2
            val gradX = twoQ3 * f1 + twoQ0 * f2 - fourQ1 * f3
            val gradY = -twoQ0 * f1 + twoQ3 * f2 - fourQ2 * f3
            val gradZ = twoQ1 * f1 + twoQ2 * f2

            val gradNorm = sqrt(gradW * gradW + gradX * gradX + gradY * gradY + gradZ * gradZ)
            if (gradNorm > 0.0) {
                qDotW -= beta * gradW / gradNorm
                qDotX -= beta * gradX / gradNorm
                qDotY -= beta * gradY / gradNorm
                qDotZ -= beta * gradZ / gradNorm
            }
        }

        val newW = q0 + qDotW * dtSeconds
        val newX = q1 + qDotX * dtSeconds
        val newY = q2 + qDotY * dtSeconds
        val newZ = q3 + qDotZ * dtSeconds

        val n = sqrt(newW * newW + newX * newX + newY * newY + newZ * newZ)
        if (n > 0.0) {
            q0 = newW / n
            q1 = newX / n
            q2 = newY / n
            q3 = newZ / n
        }
    }
}
