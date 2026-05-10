package com.mustafanazeer.baselinems.dsp

/**
 * One IMU sample produced by the signals layer.
 *
 * Coordinate frames are device frame: x to the right edge of the phone, y to the top edge,
 * z out of the screen. The DSP module operates on whatever frame it is fed and uses the
 * orientation filter to reconstruct the world frame.
 *
 * `rotationVector` follows Android `Sensor.TYPE_ROTATION_VECTOR` convention: it is the rotation
 * from device frame to world frame. That is, `rotationVector.rotate(deviceVec)` produces the
 * same vector expressed in the world frame, and `rotationVector.inverse().rotate(worldVec)`
 * produces the device frame equivalent. World frame is right handed with Z pointing up against
 * gravity. The synthetic IMU fixtures in `app/src/test/.../fixtures/SyntheticImu.kt` follow the
 * same convention so unit tests match the production wire format.
 */
data class ImuSample(
    val timestampNanos: Long,
    val accelerometer: Vector3,
    val gyroscope: Vector3,
    val linearAcceleration: Vector3,
    val rotationVector: Quaternion?
)
