package com.mustafanazeer.baselinems.signals

import com.mustafanazeer.baselinems.battery.gait.GaitTestState
import com.mustafanazeer.baselinems.battery.gait.GaitTestViewModel
import com.mustafanazeer.baselinems.dsp.GaitFeatures
import com.mustafanazeer.baselinems.dsp.GaitPipeline
import com.mustafanazeer.baselinems.dsp.ImuSample
import com.mustafanazeer.baselinems.dsp.Quaternion
import com.mustafanazeer.baselinems.dsp.Vector3
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Verifies SPE Phase 4 finding I1: on capture cancellation, every sample that the producer has
 * emitted into the IMU stream must reach the `SensorTraceWriter` before `close` is invoked.
 *
 * The fake writer records the rolling `appendCount` at the moment `close` is first called; the
 * contract is that this snapshot equals the number of samples emitted, not a smaller value.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SensorCancelTailEdgeTest {

    private class FakeImuSource : ImuSource {
        var startCount: Int = 0
        var stopCount: Int = 0
        private val emissions = MutableSharedFlow<ImuSample>(
            replay = 0,
            extraBufferCapacity = 4096,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        override fun start() { startCount += 1 }
        override fun stop() { stopCount += 1 }
        override fun stream(): Flow<ImuSample> = emissions.asSharedFlow()

        fun emit(sample: ImuSample): Boolean = emissions.tryEmit(sample)
    }

    private class FakeGaitPipeline : GaitPipeline() {
        override fun process(samples: List<ImuSample>): GaitFeatures = GaitFeatures(
            cadenceStepsPerMinute = 100.0,
            meanStrideLengthMeters = 1.0,
            stepTimeCv = 0.05,
            strideAsymmetryIndex = 0.0,
            doubleSupportTimeSeconds = 0.2,
            qualityScore = 0.5,
            detectedStepCount = samples.size
        )
    }

    private fun syntheticSample(timestampNanos: Long): ImuSample = ImuSample(
        timestampNanos = timestampNanos,
        accelerometer = Vector3(0.0, 0.0, 9.81),
        gyroscope = Vector3.ZERO,
        linearAcceleration = Vector3.ZERO,
        rotationVector = Quaternion.IDENTITY
    )

    private fun makeFiles(): Pair<File, File> {
        val filesDir = File.createTempFile("sensor_cancel_tail_edge", "").also {
            it.delete()
            it.mkdirs()
            it.deleteOnExit()
        }
        val sessionDir = File(filesDir, "sensor_traces/session-cancel").also {
            it.mkdirs()
            it.deleteOnExit()
        }
        val target = File(sessionDir, "GAIT.csv.gz").also { it.deleteOnExit() }
        return filesDir to target
    }

    @Test
    fun `cancel drains every emitted sample to the writer before close is called`() = runTest {
        val (filesDir, target) = makeFiles()
        val source = FakeImuSource()
        val writer = FakeSensorTraceWriter()
        val vm = GaitTestViewModel(
            imuSource = source,
            gaitPipeline = FakeGaitPipeline(),
            rawSensorWriter = writer,
            destinationFile = target,
            filesDir = filesDir,
            scope = this,
            writerDispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        vm.onStart()
        runCurrent()
        advanceTimeBy(3000); runCurrent()
        assertEquals(GaitTestState.Capturing(progressMillis = 0), vm.state.value)

        repeat(10) { i ->
            val emitted = source.emit(syntheticSample(timestampNanos = (i + 1) * 10_000_000L))
            assertTrue("emit $i should land in the buffer", emitted)
        }
        runCurrent()

        vm.onCancel()
        advanceUntilIdle()

        assertEquals(GaitTestState.Cancelled, vm.state.value)
        assertEquals(
            "every emitted sample must reach the writer before close is called",
            10,
            writer.appendCountAtClose
        )
        assertEquals("writer must observe all 10 emitted samples", 10, writer.appendCount)
        assertTrue("writer.close must have been invoked at least once", writer.closeCount >= 1)
        assertEquals(
            "ImuSource.stop must be called on cancel",
            1,
            source.stopCount
        )
    }
}
