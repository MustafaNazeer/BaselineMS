package com.mustafanazeer.baselinems.dsp

import kotlin.math.sqrt

/**
 * Madgwick (2010) IMU orientation filter, implemented from scratch as a teaching exercise.
 * Reference: Madgwick S O H. 2010. An efficient orientation filter for inertial and inertial /
 * magnetic sensor arrays. University of Bristol technical report. The IMU only variant (no
 * magnetometer) is used here.
 *
 * Convention: the filter's quaternion rotates device frame vectors into the world frame, i.e.
 * `orientation().rotate(deviceVec) = worldVec`. This matches the Android
 * Sensor.TYPE_ROTATION_VECTOR convention so the parallel platform fused estimate is a drop in
 * reference for the quality score residual.
 *
 * State per instance: a single Quaternion (mutable through reassignment). The update method does
 * not allocate Vector3 or Quaternion objects on the per sample path apart from the one new
 * Quaternion that replaces the field; primitive Doubles carry the rest of the work.
 *
 * The default beta gain (0.1) is the project's chosen default, deliberately above the paper's
 * IMU optimal value. Madgwick (2010) Section 5 "Results" reports β = 0.033 for the IMU
 * implementation and β = 0.041 for the MARG implementation as the optimal steady state values,
 * with an initial β = 2.5 used for the first 10 seconds of any experiment to accelerate
 * convergence of algorithm states from initial conditions. The project's β = 0.1 sits between
 * the paper's optimal and warm up values; it was retained because the GaitPipeline pre warms
 * the filter with the time averaged static gravity vector for 5000 virtual samples before the
 * real input runs (see GaitPipeline.orientationQualitySignals), so the run time filter starts
 * already aligned with gravity and the slightly higher β trades a small amount of gyro tracking
 * smoothness for faster correction against accelerometer drift during walking. The load bearing
 * empirical evidence backing this choice is the Phase 3 GaitPipelineIntegrationTest fixture
 * accuracy (healthyControlNormal 1.01 percent cadence error, msTypicalNormal 2.51 percent,
 * noisyMsNormal 0.39 percent) and the Phase 5 NONAN GaitPrint result (cadence MAE 0.53 percent,
 * ICC(3,1) 0.946 across n = 35 participants), both produced at β = 0.1.
 *
 * v1.1 polish empirical re evaluation (2026-05-19). Per the Citation Auditor's optional polish
 * recommendation in audit log entry P11.2, the production pipeline was run against the seven
 * synthetic gait fixtures at both β = 0.1 and β = 0.033 using a temporary harness wired to a
 * one shot constructor parameter on GaitPipeline. The cadence and stride length numbers were
 * identical to four decimal places across all seven fixtures (healthyControlNormal 1.0087
 * percent cadence and 0.2556 percent stride at both betas; msTypicalNormal 2.5099 and 0.1810
 * at both; noisyMsNormal 0.3905 and 0.3912 at both; slowWalk 2.4675 and 0.2707 at both;
 * briskWalk 1.5056 and 0.3885 at both; mildAsymmetry 1.9673 and 29.7708 at both; severeAsymmetry
 * 2.1896 and 0.7825 at both). The quality score residual against the platform rotationVector
 * differs slightly between the two betas on five of the seven fixtures (within ±0.06 of each
 * other) but neither direction is uniform. The comparison is therefore a wash on the load
 * bearing fixture accuracy metrics; β = 0.1 is retained as the least disruptive choice because
 * (a) the Phase 5 NONAN GaitPrint headline number (cadence MAE 0.53 percent, ICC 0.946) was
 * computed at β = 0.1 and the NONAN raw data is staged for disk reclamation per STATUS.md, so
 * re running NONAN at β = 0.033 is not free, and (b) the synthetic fixtures evidently do not
 * generate enough gyro signal divergence to discriminate between the two β values on the
 * accelerometer corrected outputs. The deliberate deviation framing above is preserved as
 * historical context; the empirical re evaluation closes the optional P11.2 polish item with a
 * "kept β = 0.1 with documentation update" verdict. See ADR 0002 for the tuning revisit
 * conditions; the next real device run that flags a Madgwick tracking gap can re open the
 * tuning question with NONAN re run as a follow up.
 *
 * The test suite runs at higher beta (0.5) for fast convergence on the static test and at zero
 * beta for pure gyro integration on the rotation test.
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
