package com.mustafan4x.baselinems.dsp

/**
 * One IMU sample produced by the Sensor Integration Engineer's signals layer (Phase 4).
 *
 * Coordinate frames are device frame: x to the right edge of the phone, y to the top edge,
 * z out of the screen. Phase 4 will document orientation when the phone is in the front pocket;
 * the DSP module operates on whatever frame it is fed and uses the orientation filter to
 * reconstruct the world frame.
 *
 * `rotationVector` follows Android `Sensor.TYPE_ROTATION_VECTOR` convention: it is the rotation
 * from device frame to world frame. That is, `rotationVector.rotate(deviceVec)` produces the
 * same vector expressed in the world frame, and `rotationVector.inverse().rotate(worldVec)`
 * produces the device frame equivalent. World frame is right handed with Z pointing up against
 * gravity. The synthetic IMU fixtures in `app/src/test/.../fixtures/SyntheticImu.kt` follow the
 * same convention so unit tests match the Phase 4 wire format.
 */
data class ImuSample(
    val timestampNanos: Long,
    val accelerometer: Vector3,
    val gyroscope: Vector3,
    val linearAcceleration: Vector3,
    val rotationVector: Quaternion?
)
