package com.mustafanazeer.baselinems.battery.gait

import com.mustafanazeer.baselinems.battery.TestResultPayload
import com.mustafanazeer.baselinems.dsp.GaitFeatures
import com.mustafanazeer.baselinems.dsp.GaitPipeline
import com.mustafanazeer.baselinems.dsp.ImuSample
import com.mustafanazeer.baselinems.dsp.Quaternion
import com.mustafanazeer.baselinems.dsp.Vector3
import com.mustafanazeer.baselinems.signals.FakeSensorTraceWriter
import com.mustafanazeer.baselinems.signals.ImuSource
import kotlinx.coroutines.Dispatchers
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class GaitTestViewModelTest {

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

    private class FakeGaitPipeline(private val canned: GaitFeatures) : GaitPipeline() {
        var lastSamples: List<ImuSample>? = null
        override fun process(samples: List<ImuSample>): GaitFeatures {
            lastSamples = samples
            return canned
        }
    }

    private fun makeFiles(): Pair<File, File> {
        val filesDir = File.createTempFile("gait_files_dir", "").also {
            it.delete()
            it.mkdirs()
            it.deleteOnExit()
        }
        val sessionDir = File(filesDir, "sensor_traces/session-1").also {
            it.mkdirs()
            it.deleteOnExit()
        }
        val target = File(sessionDir, "GAIT.csv.gz").also { it.deleteOnExit() }
        return filesDir to target
    }

    private fun cannedFeatures(): GaitFeatures = GaitFeatures(
        cadenceStepsPerMinute = 110.0,
        meanStrideLengthMeters = 1.2,
        stepTimeCv = 0.05,
        strideAsymmetryIndex = 0.02,
        doubleSupportTimeSeconds = 0.2,
        qualityScore = 0.92,
        detectedStepCount = 55
    )

    private fun syntheticSample(timestampNanos: Long): ImuSample = ImuSample(
        timestampNanos = timestampNanos,
        accelerometer = Vector3(0.0, 0.0, 9.81),
        gyroscope = Vector3.ZERO,
        linearAcceleration = Vector3.ZERO,
        rotationVector = Quaternion.IDENTITY
    )

    @Test
    fun `writerDispatcher defaults to Dispatchers IO when not supplied`() = runTest {
        // Default constructor parameter pins the writer to IO so gzip and file work do not
        // run on the composition's Main scope.
        val (filesDir, target) = makeFiles()
        val vm = GaitTestViewModel(
            imuSource = FakeImuSource(),
            gaitPipeline = FakeGaitPipeline(cannedFeatures()),
            rawSensorWriter = FakeSensorTraceWriter(),
            destinationFile = target,
            filesDir = filesDir,
            scope = this
        )

        assertSame(
            "writerDispatcher must default to Dispatchers.IO",
            Dispatchers.IO,
            vm.writerDispatcher
        )
    }

    @Test
    fun `onStart transitions Instructions to Countdown three`() = runTest {
        val (filesDir, target) = makeFiles()
        val source = FakeImuSource()
        val pipeline = FakeGaitPipeline(cannedFeatures())
        val writer = FakeSensorTraceWriter()
        val vm = GaitTestViewModel(
            imuSource = source,
            gaitPipeline = pipeline,
            rawSensorWriter = writer,
            destinationFile = target,
            filesDir = filesDir,
            scope = this,
            writerDispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        assertEquals(GaitTestState.Instructions, vm.state.value)
        vm.onStart()
        runCurrent()

        assertEquals(GaitTestState.Countdown(secondsRemaining = 3), vm.state.value)

        // Tear down the launched coroutine so runTest does not flag pending work.
        vm.onCancel()
        advanceUntilIdle()
    }

    @Test
    fun `Countdown ticks to Capturing zero after three seconds`() = runTest {
        val (filesDir, target) = makeFiles()
        val source = FakeImuSource()
        val pipeline = FakeGaitPipeline(cannedFeatures())
        val writer = FakeSensorTraceWriter()
        val vm = GaitTestViewModel(
            imuSource = source,
            gaitPipeline = pipeline,
            rawSensorWriter = writer,
            destinationFile = target,
            filesDir = filesDir,
            scope = this,
            writerDispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        vm.onStart()
        runCurrent()
        assertEquals(GaitTestState.Countdown(secondsRemaining = 3), vm.state.value)

        advanceTimeBy(1000); runCurrent()
        assertEquals(GaitTestState.Countdown(secondsRemaining = 2), vm.state.value)

        advanceTimeBy(1000); runCurrent()
        assertEquals(GaitTestState.Countdown(secondsRemaining = 1), vm.state.value)

        advanceTimeBy(1000); runCurrent()
        assertEquals(GaitTestState.Capturing(progressMillis = 0), vm.state.value)

        // Tear down to keep runTest happy.
        vm.onCancel()
        advanceUntilIdle()
    }

    @Test
    fun `Capturing accumulates samples and starts the ImuSource and RawSensorWriter`() = runTest {
        val (filesDir, target) = makeFiles()
        val source = FakeImuSource()
        val pipeline = FakeGaitPipeline(cannedFeatures())
        val writer = FakeSensorTraceWriter()
        val vm = GaitTestViewModel(
            imuSource = source,
            gaitPipeline = pipeline,
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

        assertEquals("ImuSource start should be invoked at capture entry", 1, source.startCount)

        // Inject a few samples; advance just under 30 s so the capture coroutine does not
        // transition to Done before the assertion runs.
        repeat(5) { i ->
            val emitted = source.emit(syntheticSample(timestampNanos = (i + 1) * 10_000_000L))
            assertTrue("emit $i should land in the buffer", emitted)
        }
        advanceTimeBy(500); runCurrent()
        assertTrue(
            "writer should have collected the emitted samples (got ${writer.appendCount})",
            writer.appendCount >= 5
        )

        // progressMillis advances at the 100 ms tick interval.
        val midway = vm.state.value
        assertTrue("midway state should still be Capturing", midway is GaitTestState.Capturing)
        midway as GaitTestState.Capturing
        assertTrue(
            "progressMillis should advance during capture (got ${midway.progressMillis})",
            midway.progressMillis in 100..3500
        )

        vm.onCancel()
        advanceUntilIdle()
    }

    @Test
    fun `Capturing transitions to Done with non null features after 30 s`() = runTest {
        val (filesDir, target) = makeFiles()
        val source = FakeImuSource()
        val canned = cannedFeatures()
        val pipeline = FakeGaitPipeline(canned)
        val writer = FakeSensorTraceWriter()
        val vm = GaitTestViewModel(
            imuSource = source,
            gaitPipeline = pipeline,
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

        // Emit one sample per 10 ms over the 30 s capture window.
        var virtualTimestampNanos = 0L
        var elapsedMs = 0
        while (elapsedMs < 30_000) {
            virtualTimestampNanos += 10_000_000L
            source.emit(syntheticSample(timestampNanos = virtualTimestampNanos))
            advanceTimeBy(10); runCurrent()
            elapsedMs += 10
        }
        advanceUntilIdle()

        val done = vm.state.value
        assertTrue("state should be Done after 30 s", done is GaitTestState.Done)
        done as GaitTestState.Done
        assertEquals(canned, done.features)
        assertNotNull("pipeline should have been invoked with samples", pipeline.lastSamples)
        assertTrue(
            "pipeline should have received the emitted samples (got ${pipeline.lastSamples!!.size})",
            pipeline.lastSamples!!.isNotEmpty()
        )
        assertEquals("ImuSource stop should be called once at finishCapture", 1, source.stopCount)
    }

    @Test
    fun `onCancel from Capturing transitions to Cancelled and stops the ImuSource`() = runTest {
        val (filesDir, target) = makeFiles()
        val source = FakeImuSource()
        val pipeline = FakeGaitPipeline(cannedFeatures())
        val writer = FakeSensorTraceWriter()
        val vm = GaitTestViewModel(
            imuSource = source,
            gaitPipeline = pipeline,
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
        assertEquals(1, source.startCount)

        vm.onCancel()
        advanceUntilIdle()

        assertEquals(GaitTestState.Cancelled, vm.state.value)
        assertEquals("ImuSource stop should be called on cancel", 1, source.stopCount)
    }

    @Test
    fun `onContinue from Done invokes callback with the destination relative path`() = runTest {
        val (filesDir, target) = makeFiles()
        val source = FakeImuSource()
        val pipeline = FakeGaitPipeline(cannedFeatures())
        val writer = FakeSensorTraceWriter()
        val vm = GaitTestViewModel(
            imuSource = source,
            gaitPipeline = pipeline,
            rawSensorWriter = writer,
            destinationFile = target,
            filesDir = filesDir,
            scope = this,
            writerDispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        vm.onStart()
        runCurrent()
        advanceTimeBy(3000); runCurrent()
        // Drive the capture window to completion without emitting samples (the canned pipeline
        // returns the same fixed features regardless of input).
        advanceTimeBy(30_000); runCurrent()
        advanceUntilIdle()

        assertTrue(vm.state.value is GaitTestState.Done)

        var captured: TestResultPayload? = null
        vm.onContinue { captured = it }

        val payload = captured
        assertNotNull("onContinue must invoke the callback", payload)
        payload!!
        val expectedRelative = target.relativeTo(filesDir).path
        assertEquals(expectedRelative, payload.rawSensorRelativePath)
        assertEquals(0.92, payload.qualityScore, 1e-9)
        assertNotNull("features map must be populated", payload.features)
        assertNull(
            "rawSensorRelativePath must not contain the absolute filesDir prefix",
            payload.rawSensorRelativePath?.takeIf { it.startsWith(filesDir.absolutePath) }
        )
    }
}
