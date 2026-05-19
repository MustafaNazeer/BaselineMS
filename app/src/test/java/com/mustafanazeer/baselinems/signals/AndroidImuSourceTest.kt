package com.mustafanazeer.baselinems.signals

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mustafanazeer.baselinems.dsp.ImuSample
import com.mustafanazeer.baselinems.dsp.Quaternion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSensor
import org.robolectric.shadows.ShadowSensorManager
import org.robolectric.shadows.SensorEventBuilder
import kotlin.math.sqrt

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class AndroidImuSourceTest {

    private lateinit var context: Context
    private lateinit var sensorManager: SensorManager
    private lateinit var shadow: ShadowSensorManager

    private lateinit var linearSensor: Sensor
    private lateinit var gyroSensor: Sensor
    private lateinit var rotationSensor: Sensor

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        shadow = Shadows.shadowOf(sensorManager)
        linearSensor = ShadowSensor.newInstance(Sensor.TYPE_LINEAR_ACCELERATION)
        gyroSensor = ShadowSensor.newInstance(Sensor.TYPE_GYROSCOPE)
        rotationSensor = ShadowSensor.newInstance(Sensor.TYPE_ROTATION_VECTOR)
    }

    @After
    fun teardown() {
        // Robolectric resets the SensorManager between tests, but explicit unregister helps if a
        // future test adds a long lived AndroidImuSource that survives the @Before reset.
    }

    private fun addAllSensors() {
        shadow.addSensor(linearSensor)
        shadow.addSensor(gyroSensor)
        shadow.addSensor(rotationSensor)
    }

    private fun event(sensor: Sensor, values: FloatArray, timestamp: Long) =
        SensorEventBuilder.newBuilder()
            .setSensor(sensor)
            .setValues(values)
            .setTimestamp(timestamp)
            .build()

    @Test
    fun `start registers listeners for linear acceleration, gyroscope, and rotation vector`() {
        addAllSensors()
        val source = AndroidImuSource(context)

        assertEquals(0, shadow.listeners.size)
        source.start()

        assertEquals(1, shadow.listeners.size)
        val firstListener = shadow.listeners.first()
        assertTrue(shadow.hasListener(firstListener, linearSensor))
        assertTrue(shadow.hasListener(firstListener, gyroSensor))
        assertTrue(shadow.hasListener(firstListener, rotationSensor))

        source.stop()
    }

    @Test
    fun `stream emits an ImuSample on each linear acceleration event with held gyro and rotation values`() = runTest {
        addAllSensors()
        val source = AndroidImuSource(context)
        source.start()

        val collector = TestScope(StandardTestDispatcher(testScheduler))
        val collected = mutableListOf<ImuSample>()
        val job = collector.launch {
            source.stream().collect { collected += it }
        }
        advanceUntilIdle()

        shadow.sendSensorEventToListeners(
            event(gyroSensor, floatArrayOf(0.1f, 0.2f, 0.3f), 1_000_000L)
        )
        // Rotation vector with explicit w (API 18+) so the conversion is unambiguous.
        shadow.sendSensorEventToListeners(
            event(rotationSensor, floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f), 2_000_000L)
        )
        shadow.sendSensorEventToListeners(
            event(linearSensor, floatArrayOf(1.0f, 2.0f, 3.0f), 3_000_000L)
        )
        advanceUntilIdle()

        assertEquals(1, collected.size)
        val sample = collected.first()
        assertEquals(3_000_000L, sample.timestampNanos)
        assertEquals(1.0, sample.linearAcceleration.x, 1e-6)
        assertEquals(2.0, sample.linearAcceleration.y, 1e-6)
        assertEquals(3.0, sample.linearAcceleration.z, 1e-6)
        assertEquals(0.1, sample.gyroscope.x, 1e-6)
        assertEquals(0.2, sample.gyroscope.y, 1e-6)
        assertEquals(0.3, sample.gyroscope.z, 1e-6)
        val rot = sample.rotationVector
        assertNotNull(rot)
        rot!!
        // Identity rotation: w = 1, (x, y, z) = 0.
        assertEquals(1.0, rot.w, 1e-6)
        assertEquals(0.0, rot.x, 1e-6)
        assertEquals(0.0, rot.y, 1e-6)
        assertEquals(0.0, rot.z, 1e-6)

        job.cancel()
        source.stop()
    }

    @Test
    fun `sensor listener runs on a dedicated thread, not the main thread`() {
        addAllSensors()
        val source = AndroidImuSource(context)
        source.start()

        val listenerLooper = source.sensorListenerLooper
        assertNotSame(
            "sensor listener must not share the main looper (PE M3)",
            Looper.getMainLooper(),
            listenerLooper
        )
        assertNotNull("sensor listener looper must be non-null", listenerLooper)

        source.stop()
    }

    @Test
    fun `stop unregisters all listeners`() {
        addAllSensors()
        val source = AndroidImuSource(context)
        source.start()
        assertEquals(1, shadow.listeners.size)

        source.stop()

        assertEquals(0, shadow.listeners.size)
    }

    @Test
    fun `fallback Madgwick holds first emission until raw accelerometer arrives within 50 ms`() = runTest {
        // SPE Phase 4 I2: on the first fallback emission `lastAccel` may be null because
        // TYPE_ACCELEROMETER has not fired yet. The source must hold emission for up to
        // FIRST_SAMPLE_HOLD_MS so the filter sees the real gravity reference on the very first
        // step. Once the accelerometer arrives within the hold window, the held emission is
        // released using the real accel observation (not the gravity removed linear surrogate).
        shadow.addSensor(linearSensor)
        shadow.addSensor(gyroSensor)
        // Rotation vector intentionally omitted so the source enters fallback mode and
        // registers TYPE_ACCELEROMETER.
        val accelSensor = ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER)
        shadow.addSensor(accelSensor)

        var virtualNowMs = 0L
        val source = AndroidImuSource(context, nowMs = { virtualNowMs })
        source.start()

        val collector = TestScope(StandardTestDispatcher(testScheduler))
        val collected = mutableListOf<ImuSample>()
        val job = collector.launch {
            source.stream().collect { collected += it }
        }
        advanceUntilIdle()

        // Fire gyro and linear first, before any accelerometer event.
        shadow.sendSensorEventToListeners(
            event(gyroSensor, floatArrayOf(0.0f, 0.0f, 0.0f), 1_000_000L)
        )
        shadow.sendSensorEventToListeners(
            event(linearSensor, floatArrayOf(0.0f, 0.0f, 0.0f), 2_000_000L)
        )
        advanceUntilIdle()
        assertEquals(
            "no emission while the first sample hold is active and accelerometer has not arrived",
            0,
            collected.size
        )

        // Accel arrives 10 ms later (within the 50 ms hold window). The next linear acceleration
        // event releases the held emission with the real gravity reference.
        virtualNowMs = 10L
        shadow.sendSensorEventToListeners(
            event(accelSensor, floatArrayOf(0.0f, 0.0f, 9.81f), 11_000_000L)
        )
        shadow.sendSensorEventToListeners(
            event(linearSensor, floatArrayOf(0.0f, 0.0f, 0.0f), 12_000_000L)
        )
        advanceUntilIdle()

        assertEquals(
            "first emission lands once the real accelerometer reading is available",
            1,
            collected.size
        )
        val rot = collected.first().rotationVector
        assertNotNull(rot)
        rot!!
        // The Madgwick filter saw a non degenerate gravity reading (9.81 m/s^2 on +z) on its
        // first step, so the resulting orientation must be unit norm (a valid quaternion) and
        // must not be the surrogate-driven output. We verify the quaternion is unit norm; exact
        // values depend on the Madgwick beta and dt, which are filter internals.
        val norm = sqrt(rot.w * rot.w + rot.x * rot.x + rot.y * rot.y + rot.z * rot.z)
        assertEquals("first fallback rotation must be a unit norm quaternion", 1.0, norm, 1e-6)
        // Sanity: the accelerometer column on the emitted sample reflects the real accel reading.
        assertEquals(9.81, collected.first().accelerometer.z, 1e-6)

        job.cancel()
        source.stop()
    }

    @Test
    fun `fallback Madgwick falls back to linear surrogate after 50 ms with no accelerometer`() = runTest {
        // SPE Phase 4 I2 fallback preserved: when the raw accelerometer never arrives, the
        // source still steps the Madgwick filter using `lastLinear` as a degraded gravity
        // surrogate after the FIRST_SAMPLE_HOLD_MS hold window elapses. The pipeline must not
        // stall.
        shadow.addSensor(linearSensor)
        shadow.addSensor(gyroSensor)
        val accelSensor = ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER)
        shadow.addSensor(accelSensor)

        var virtualNowMs = 0L
        val source = AndroidImuSource(context, nowMs = { virtualNowMs })
        source.start()

        val collector = TestScope(StandardTestDispatcher(testScheduler))
        val collected = mutableListOf<ImuSample>()
        val job = collector.launch {
            source.stream().collect { collected += it }
        }
        advanceUntilIdle()

        // First linear event: starts the hold timer at virtualNowMs == 0.
        shadow.sendSensorEventToListeners(
            event(gyroSensor, floatArrayOf(0.0f, 0.0f, 0.0f), 1_000_000L)
        )
        shadow.sendSensorEventToListeners(
            event(linearSensor, floatArrayOf(1.5f, 0.7f, 0.3f), 2_000_000L)
        )
        advanceUntilIdle()
        assertEquals("hold window blocks the first emission", 0, collected.size)

        // Advance the virtual clock past the hold window without sending an accelerometer event.
        virtualNowMs = 60L
        shadow.sendSensorEventToListeners(
            event(linearSensor, floatArrayOf(1.5f, 0.7f, 0.3f), 12_000_000L)
        )
        advanceUntilIdle()

        assertEquals(
            "after the hold window the source emits using linear as the gravity surrogate",
            1,
            collected.size
        )
        val rot = collected.first().rotationVector
        assertNotNull("fallback rotation must be filled even with no accel", rot)
        rot!!
        val norm = sqrt(rot.w * rot.w + rot.x * rot.x + rot.y * rot.y + rot.z * rot.z)
        assertEquals("rotation quaternion must be unit norm", 1.0, norm, 1e-6)

        job.cancel()
        source.stop()
    }

    @Test
    fun `when rotation vector sensor is absent, fallback Madgwick fills rotationVector`() = runTest {
        // Add linear acceleration, gyroscope, and the raw accelerometer (the fallback path
        // registers TYPE_ACCELEROMETER when TYPE_ROTATION_VECTOR is absent); intentionally omit
        // the rotation vector sensor so the fallback Madgwick filter drives orientation.
        shadow.addSensor(linearSensor)
        shadow.addSensor(gyroSensor)
        val accelSensor = ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER)
        shadow.addSensor(accelSensor)

        val source = AndroidImuSource(context)
        source.start()

        val collector = TestScope(StandardTestDispatcher(testScheduler))
        val collected = mutableListOf<ImuSample>()
        val job = collector.launch {
            source.stream().collect { collected += it }
        }
        advanceUntilIdle()

        // Feed a static gravity reading on the +z axis (m/s^2) on TYPE_ACCELEROMETER and zero
        // rotation rate. The fallback Madgwick should converge toward identity quickly because
        // the device sits flat. TYPE_LINEAR_ACCELERATION is gravity removed, so it stays at 0.
        repeat(20) { i ->
            val ts = (i + 1) * 10_000_000L
            shadow.sendSensorEventToListeners(
                event(gyroSensor, floatArrayOf(0.0f, 0.0f, 0.0f), ts)
            )
            shadow.sendSensorEventToListeners(
                event(accelSensor, floatArrayOf(0.0f, 0.0f, 9.81f), ts)
            )
            shadow.sendSensorEventToListeners(
                event(linearSensor, floatArrayOf(0.0f, 0.0f, 0.0f), ts)
            )
        }
        advanceUntilIdle()

        assertTrue("expected emissions, got ${collected.size}", collected.isNotEmpty())
        for (sample in collected) {
            assertNotNull("rotationVector must be non-null on fallback path", sample.rotationVector)
        }

        job.cancel()
        source.stop()
    }
}
