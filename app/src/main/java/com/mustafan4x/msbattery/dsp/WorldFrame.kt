package com.mustafan4x.msbattery.dsp

/**
 * World frame transforms and the gravity sanity check.
 *
 * Per the Android Sensor.TYPE_ROTATION_VECTOR convention used throughout the project, the
 * supplied orientation quaternion rotates device frame vectors into the world frame, that is
 * `q.rotate(deviceVec) = worldVec`. The world frame is right handed with Z pointing up against
 * gravity. The forward and lateral axes are determined by the body's heading at session start;
 * the gait pipeline does not depend on a specific yaw alignment because step detection runs on
 * world Z (vertical) and stride pairing on the sign of the lateral component independent of its
 * absolute world frame label.
 */
object WorldFrame {

    fun toWorld(orientation: Quaternion, deviceLinearAcceleration: Vector3): Vector3 =
        orientation.rotate(deviceLinearAcceleration)

    /**
     * Estimate the gravity vector in the device frame from a static window of raw accelerometer
     * samples. The mean over the window cancels white sensor noise and yields the gravity
     * vector that the device sees while at rest. Used in Phase 3 Task 13 quality scoring to
     * confirm orientation tracking has not drifted: at rest the recovered gravity magnitude
     * should remain near 9.80665 m per second squared.
     */
    fun estimateGravity(rawAccelerometerSamples: List<Vector3>): Vector3 {
        if (rawAccelerometerSamples.isEmpty()) return Vector3.ZERO
        var sx = 0.0
        var sy = 0.0
        var sz = 0.0
        for (s in rawAccelerometerSamples) {
            sx += s.x
            sy += s.y
            sz += s.z
        }
        val n = rawAccelerometerSamples.size.toDouble()
        return Vector3(sx / n, sy / n, sz / n)
    }
}
