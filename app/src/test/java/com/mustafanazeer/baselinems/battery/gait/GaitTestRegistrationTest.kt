package com.mustafanazeer.baselinems.battery.gait

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mustafanazeer.baselinems.data.TestType
import com.mustafanazeer.baselinems.dsp.GaitPipeline
import com.mustafanazeer.baselinems.signals.AndroidImuSource
import com.mustafanazeer.baselinems.signals.RawSensorWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
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
import java.io.File

/**
 * Smoke test for the gait test wiring in `BaselineMSApp` plus `RootScreen.composable("session")`.
 * Verifies that constructing a `GaitTest` with the production-shaped dependencies (the
 * `AndroidImuSource` reading from a Robolectric-backed `SensorManager`, a real `GaitPipeline`,
 * a real `RawSensorWriter` pointed at a temp file under a synthetic `filesDir`) does not throw.
 *
 * Mirrors `AndroidImuSourceTest`'s `AndroidJUnit4` plus `Config(sdk = [33])` pattern so the
 * module construction path is exercised on the same Robolectric SDK target as the other signals
 * layer tests.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class GaitTestRegistrationTest {

    private lateinit var context: Context
    private lateinit var sensorManager: SensorManager
    private lateinit var shadow: ShadowSensorManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        shadow = Shadows.shadowOf(sensorManager)
        shadow.addSensor(ShadowSensor.newInstance(Sensor.TYPE_LINEAR_ACCELERATION))
        shadow.addSensor(ShadowSensor.newInstance(Sensor.TYPE_GYROSCOPE))
        shadow.addSensor(ShadowSensor.newInstance(Sensor.TYPE_ROTATION_VECTOR))
    }

    @Test
    fun `GaitTest constructs with production wiring without crashing`() {
        val imuSource = AndroidImuSource(context)
        val sessionId = "test-session-${System.currentTimeMillis()}"
        val filesDir = File.createTempFile("gait_files_dir", "").also {
            it.delete()
            it.mkdirs()
            it.deleteOnExit()
        }
        val targetFile = File(filesDir, "sensor_traces/$sessionId/${TestType.GAIT.name}.csv.gz")
        targetFile.parentFile?.mkdirs()
        targetFile.deleteOnExit()

        val factory: (CoroutineScope) -> GaitTestViewModel = { scope ->
            GaitTestViewModel(
                imuSource = imuSource,
                gaitPipeline = GaitPipeline(),
                rawSensorWriter = RawSensorWriter(targetFile),
                destinationFile = targetFile,
                filesDir = filesDir,
                scope = scope
            )
        }

        val gaitTest = GaitTest(viewModelFactory = factory)

        assertEquals(TestType.GAIT, gaitTest.testType)
        assertEquals("Gait", gaitTest.displayName)
        assertTrue(gaitTest.instructions.isNotEmpty())
        assertTrue(gaitTest.estimatedDurationSeconds > 0)

        // Construct a view model from the factory so the lambda's closure is exercised end to end.
        val viewModel = factory(TestScope())
        assertNotNull(viewModel)
        assertEquals(GaitTestState.Instructions, viewModel.state.value)
    }
}
