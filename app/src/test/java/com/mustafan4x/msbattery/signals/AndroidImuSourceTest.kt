package com.mustafan4x.msbattery.signals

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mustafan4x.msbattery.dsp.ImuSample
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSensor
import org.robolectric.shadows.ShadowSensorManager
import org.robolectric.shadows.SensorEventBuilder

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
    fun `stop unregisters all listeners`() {
        addAllSensors()
        val source = AndroidImuSource(context)
        source.start()
        assertEquals(1, shadow.listeners.size)

        source.stop()

        assertEquals(0, shadow.listeners.size)
    }

    @Test
    fun `when rotation vector sensor is absent, fallback Madgwick fills rotationVector`() = runTest {
        // Add only linear acceleration and gyroscope; intentionally omit the rotation vector sensor.
        shadow.addSensor(linearSensor)
        shadow.addSensor(gyroSensor)

        val source = AndroidImuSource(context)
        source.start()

        val collector = TestScope(StandardTestDispatcher(testScheduler))
        val collected = mutableListOf<ImuSample>()
        val job = collector.launch {
            source.stream().collect { collected += it }
        }
        advanceUntilIdle()

        // Feed a static gravity reading on the +z axis (m/s^2) and zero rotation rate. The fallback
        // Madgwick should converge toward identity quickly because the device sits flat.
        repeat(20) { i ->
            val ts = (i + 1) * 10_000_000L
            shadow.sendSensorEventToListeners(
                event(gyroSensor, floatArrayOf(0.0f, 0.0f, 0.0f), ts)
            )
            shadow.sendSensorEventToListeners(
                event(linearSensor, floatArrayOf(0.0f, 0.0f, 9.81f), ts)
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
