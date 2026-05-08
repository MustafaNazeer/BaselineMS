package com.mustafan4x.msbattery.dsp

/**
 * One IMU sample produced by the Sensor Integration Engineer's signals layer (Phase 4).
 *
 * Coordinate frames are device frame: x to the right edge of the phone, y to the top edge,
 * z out of the screen. Phase 4 will document orientation when the phone is in the front pocket;
 * the DSP module operates on whatever frame it is fed and uses the orientation filter to
 * reconstruct the world frame.
 */
data class ImuSample(
    val timestampNanos: Long,
    val accelerometer: Vector3,
    val gyroscope: Vector3,
    val linearAcceleration: Vector3,
    val rotationVector: Quaternion?
)
