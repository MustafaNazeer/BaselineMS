package com.mustafanazeer.baselinems.signals

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.mustafanazeer.baselinems.dsp.ImuSample
import com.mustafanazeer.baselinems.dsp.Madgwick
import com.mustafanazeer.baselinems.dsp.Quaternion
import com.mustafanazeer.baselinems.dsp.Vector3
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.math.max
import kotlin.math.sqrt

/**
 * SensorManager backed `ImuSource`. Registers `Sensor.TYPE_LINEAR_ACCELERATION`,
 * `Sensor.TYPE_GYROSCOPE`, and `Sensor.TYPE_ROTATION_VECTOR` at a 100 Hz target sampling period.
 *
 * The linear acceleration sensor is the primary clock. On every linear acceleration event the
 * source emits one `ImuSample` carrying the latest gyroscope reading and the latest rotation
 * vector reading via zero order hold (Android does not synchronize triggers across sensor types).
 *
 * Fallback path: when the device does not expose `Sensor.TYPE_ROTATION_VECTOR`, the source runs
 * the from scratch `Madgwick` filter (`dsp/Madgwick.kt`) inline using gyroscope plus raw
 * accelerometer (`Sensor.TYPE_ACCELEROMETER`) to produce a per sample quaternion, keeping the
 * sensor concern out of the `dsp/` layer. The fallback registers a separate
 * `Sensor.TYPE_ACCELEROMETER` listener because `Sensor.TYPE_LINEAR_ACCELERATION` is gravity
 * removed and would not let Madgwick observe the gravity reference vector it needs for tilt
 * correction.
 */
class AndroidImuSource(
    context: Context,
    private val samplingPeriodMicros: Int = 10_000,
    private val nowMs: () -> Long = { System.currentTimeMillis() }
) : ImuSource {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val linearAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val rawAccel: Sensor? = if (rotation == null) {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    } else {
        null
    }

    private val emissions = MutableSharedFlow<ImuSample>(
        replay = 0,
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val sensorHandlerThread: HandlerThread =
        HandlerThread("BaselineMS-Sensor").apply { start() }
    private val sensorHandler: Handler = Handler(sensorHandlerThread.looper)

    /**
     * The looper that the `SensorManager` callbacks fire on. Exposed so unit tests can confirm
     * the listener is not bound to the main looper, which would let the writer's gzip work stall
     * the producer.
     */
    internal val sensorListenerLooper: Looper get() = sensorHandlerThread.looper

    private val fallbackMadgwick: Madgwick? = if (rotation == null) Madgwick(beta = 0.1) else null

    private var lastLinear: Vector3? = null
    private var lastAccel: Vector3? = null
    private var lastGyro: Vector3? = null
    private var lastRotation: Quaternion? = null
    private var lastLinearTimestampNanos: Long = 0L
    private var prevLinearTimestampNanos: Long = 0L

    // PE M2 (Phase 4 review, carried to v1.1 polish): debug only sample rate accounting.
    // The runbook driven `adb pull` plus awk pipeline at `docs/observability/sensor-runbook.md`
    // remains the source of truth for per device sample rate validation; these counters surface
    // the same mean rate in app for a future debug or About screen so a user can verify their
    // device's sample rate against the nominal 100 Hz target without pulling the trace by hand.
    // Two `Long` fields keep the allocation footprint at zero on the capture hot path.
    @Volatile private var emittedSampleCount: Long = 0L
    @Volatile private var firstEmittedTimestampNanos: Long = 0L

    // SPE Phase 4 I2: on the very first fallback emission `lastAccel` may be null because
    // TYPE_ACCELEROMETER has not fired yet. Hold emission for up to FIRST_SAMPLE_HOLD_MS to let
    // the raw accelerometer catch up so Madgwick sees the gravity reference it expects; if the
    // hold window elapses without an accel reading, fall back to feeding `lastLinear` (gravity
    // removed) into the filter so the pipeline does not stall.
    private var firstSampleHoldStartedAtMs: Long? = null

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_LINEAR_ACCELERATION -> {
                    lastLinear = Vector3(
                        event.values[0].toDouble(),
                        event.values[1].toDouble(),
                        event.values[2].toDouble()
                    )
                    prevLinearTimestampNanos = lastLinearTimestampNanos
                    lastLinearTimestampNanos = event.timestamp
                    emitIfReady()
                }
                Sensor.TYPE_GYROSCOPE -> {
                    lastGyro = Vector3(
                        event.values[0].toDouble(),
                        event.values[1].toDouble(),
                        event.values[2].toDouble()
                    )
                }
                Sensor.TYPE_ROTATION_VECTOR -> {
                    lastRotation = quaternionFromAndroidRotationVector(event.values)
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    lastAccel = Vector3(
                        event.values[0].toDouble(),
                        event.values[1].toDouble(),
                        event.values[2].toDouble()
                    )
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* no op */ }
    }

    override fun start() {
        emittedSampleCount = 0L
        firstEmittedTimestampNanos = 0L
        if (linearAccel != null) {
            sensorManager.registerListener(listener, linearAccel, samplingPeriodMicros, sensorHandler)
        }
        if (gyro != null) {
            sensorManager.registerListener(listener, gyro, samplingPeriodMicros, sensorHandler)
        }
        if (rotation != null) {
            sensorManager.registerListener(listener, rotation, samplingPeriodMicros, sensorHandler)
        }
        if (rawAccel != null) {
            sensorManager.registerListener(listener, rawAccel, samplingPeriodMicros, sensorHandler)
        }
    }

    override fun stop() {
        sensorManager.unregisterListener(listener)
    }

    override fun stream(): Flow<ImuSample> = emissions.asSharedFlow()

    private fun emitIfReady() {
        val linear = lastLinear ?: return
        val gyroNow = lastGyro ?: Vector3.ZERO

        val platformRotation = lastRotation
        val rotationNow: Quaternion
        if (platformRotation != null) {
            rotationNow = platformRotation
        } else if (fallbackMadgwick != null) {
            val accel = lastAccel
            if (accel != null) {
                // Real accelerometer reading is available; clear any pending hold and step the
                // filter normally so the first emission carries the gravity included observation.
                firstSampleHoldStartedAtMs = null
                val dt = computeDtSeconds()
                fallbackMadgwick.update(gyroNow, accel, dt)
                rotationNow = fallbackMadgwick.orientation()
            } else {
                // SPE Phase 4 I2: hold up to FIRST_SAMPLE_HOLD_MS for the raw accelerometer to
                // arrive before falling back to the gravity removed surrogate.
                val startedAt = firstSampleHoldStartedAtMs
                if (startedAt == null) {
                    firstSampleHoldStartedAtMs = nowMs()
                    return
                }
                val elapsed = nowMs() - startedAt
                if (elapsed < FIRST_SAMPLE_HOLD_MS) {
                    return
                }
                // Hold window elapsed without an accelerometer reading. Step the filter with the
                // gravity removed `linear` as a degraded surrogate so the pipeline does not stall.
                val dt = computeDtSeconds()
                fallbackMadgwick.update(gyroNow, linear, dt)
                rotationNow = fallbackMadgwick.orientation()
            }
        } else {
            // No fallback configured (platform rotation sensor was present at construction time
            // but has not fired yet). Identity matches the prior behavior.
            rotationNow = Quaternion.IDENTITY
        }

        // ImuSample.accelerometer carries the raw, gravity included acceleration. The fallback
        // path registers TYPE_ACCELEROMETER and stamps the most recent reading; the platform
        // path leaves it equal to the gravity removed linear acceleration because the gait
        // pipeline only consumes `linearAcceleration` and `rotationVector`.
        val accelForSample = lastAccel ?: linear

        emissions.tryEmit(
            ImuSample(
                timestampNanos = lastLinearTimestampNanos,
                accelerometer = accelForSample,
                gyroscope = gyroNow,
                linearAcceleration = linear,
                rotationVector = rotationNow
            )
        )
        if (emittedSampleCount == 0L) {
            firstEmittedTimestampNanos = lastLinearTimestampNanos
        }
        emittedSampleCount += 1L
    }

    /**
     * Derives the sampling delta for the fallback Madgwick from successive linear acceleration
     * timestamps (event.timestamp is already in nanoseconds per the Android `SensorEvent` contract).
     * On the very first emission `prevLinearTimestampNanos` is zero; we substitute the nominal
     * sampling period so Madgwick still advances. A non positive measured delta also falls back
     * to the nominal period so the filter never sees `dt <= 0`.
     */
    private fun computeDtSeconds(): Double {
        val nominal = samplingPeriodMicros.toDouble() / 1_000_000.0
        if (prevLinearTimestampNanos <= 0L) return nominal
        val deltaNs = lastLinearTimestampNanos - prevLinearTimestampNanos
        if (deltaNs <= 0L) return nominal
        return deltaNs.toDouble() / 1_000_000_000.0
    }

    /**
     * Converts an Android `Sensor.TYPE_ROTATION_VECTOR` event payload into a `Quaternion`
     * (w, x, y, z) following the same device to world convention as `dsp/Madgwick.kt`.
     *
     * The sensor reports `values[0..2] = (x, y, z) * sin(theta / 2)`. On API level 18 and above
     * `values[3] = cos(theta / 2)`. When `values[3]` is absent we recover it as
     * `w = sqrt(1 - (x^2 + y^2 + z^2))` clamped to the non negative root, which is the same
     * formula `SensorManager.getQuaternionFromVector` uses internally. The result is normalized
     * so downstream consumers can rely on unit norm regardless of platform precision.
     */
    private fun quaternionFromAndroidRotationVector(values: FloatArray): Quaternion {
        val x = values[0].toDouble()
        val y = values[1].toDouble()
        val z = values[2].toDouble()
        val rawW = if (values.size >= 4) {
            values[3].toDouble()
        } else {
            val v2 = x * x + y * y + z * z
            sqrt(max(0.0, 1.0 - v2))
        }
        val norm = sqrt(rawW * rawW + x * x + y * y + z * z)
        if (norm == 0.0) return Quaternion.IDENTITY
        return Quaternion(rawW / norm, x / norm, y / norm, z / norm)
    }

    /**
     * Returns the mean observed sample rate, in hertz, across every `ImuSample` emitted on
     * `stream()` since the most recent `start()` invocation. Returns `0.0` when fewer than two
     * samples have been emitted or when the span between the first and the latest sample is
     * non positive (an edge case that only occurs if the platform delivers a non monotonic
     * timestamp on the very first event).
     *
     * The accessor is debug only. The Performance Engineer's Phase 4 review listed the missing
     * in app sample rate accessor as finding PE M2 so a future Settings debug screen can surface
     * the same numbers the runbook pipeline at `docs/observability/sensor-runbook.md` produces
     * via `adb pull` plus an `awk` reduction. It is not wired into any production user facing
     * surface in v1.1; the captured CSV remains the source of truth for cross device sample
     * rate validation.
     *
     * Thread safety: the two backing counters are `@Volatile` reads; the value reflects samples
     * emitted up to the most recent atomic publication of the count. Concurrent emission may
     * race the read by at most one sample, which is acceptable for a debug surface.
     */
    fun debugMeanSampleRateHz(): Double {
        val count = emittedSampleCount
        if (count < 2L) return 0.0
        val first = firstEmittedTimestampNanos
        val last = lastLinearTimestampNanos
        val spanNanos = last - first
        if (spanNanos <= 0L) return 0.0
        val intervals = count - 1L
        return intervals.toDouble() * 1_000_000_000.0 / spanNanos.toDouble()
    }

    /**
     * Returns the number of `ImuSample` values emitted on `stream()` since the most recent
     * `start()` invocation. Pairs with `debugMeanSampleRateHz()` to let a debug screen surface
     * both the rate and the sample count, mirroring the headline numbers the
     * `docs/observability/sensor-runbook.md` `awk` pipeline reports.
     */
    fun debugEmittedSampleCount(): Long = emittedSampleCount

    companion object {
        private const val FIRST_SAMPLE_HOLD_MS: Long = 50L
    }
}
