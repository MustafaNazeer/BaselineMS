package com.mustafanazeer.baselinems.battery.gait

import android.util.Log
import com.mustafanazeer.baselinems.battery.TestResultPayload
import com.mustafanazeer.baselinems.dsp.GaitPipeline
import com.mustafanazeer.baselinems.dsp.ImuSample
import com.mustafanazeer.baselinems.signals.ImuSource
import com.mustafanazeer.baselinems.signals.SensorTraceWriter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * View model that orchestrates the gait test state machine and the sensor capture.
 *
 * The state sequence on the happy path is `Instructions` -> `Countdown(3)` -> `Countdown(2)` ->
 * `Countdown(1)` -> `Capturing(0)` -> ... -> `Capturing(30000)` -> `Done(features)`. From
 * `Capturing` the user may also cancel, transitioning to `Cancelled` and stopping the
 * `ImuSource`. From `Done`, `onContinue` produces a `TestResultPayload` whose
 * `rawSensorRelativePath` is the destination file's path relative to `filesDir`.
 *
 * The view model is a plain class rather than a `androidx.lifecycle.ViewModel` so that the
 * test suite can inject a `kotlinx.coroutines.test.TestScope` and drive the state machine with
 * `advanceTimeBy`. The production wiring in `GaitTest.Content` constructs a fresh view model
 * per `composable("session")` entry and bins it to the composition's `CoroutineScope`.
 */
class GaitTestViewModel(
    private val imuSource: ImuSource,
    private val gaitPipeline: GaitPipeline,
    private val rawSensorWriter: SensorTraceWriter,
    private val destinationFile: File,
    private val filesDir: File,
    private val scope: CoroutineScope,
    internal val writerDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val _state = MutableStateFlow<GaitTestState>(GaitTestState.Instructions)
    val state: StateFlow<GaitTestState> = _state.asStateFlow()

    private val capturedSamples: MutableList<ImuSample> = mutableListOf()
    private var captureJob: Job? = null
    private var writerJob: Job? = null

    fun onStart() {
        if (_state.value !is GaitTestState.Instructions) return
        _state.value = GaitTestState.Countdown(secondsRemaining = COUNTDOWN_SECONDS)
        scope.launch {
            for (s in (COUNTDOWN_SECONDS - 1) downTo 1) {
                delay(1000)
                _state.value = GaitTestState.Countdown(secondsRemaining = s)
            }
            delay(1000)
            beginCapture()
        }
    }

    private fun beginCapture() {
        capturedSamples.clear()
        _state.value = GaitTestState.Capturing(progressMillis = 0)
        imuSource.start()
        writerJob = scope.launch(writerDispatcher) {
            try {
                imuSource.stream().collect { sample ->
                    capturedSamples += sample
                    rawSensorWriter.appendRow(sample)
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (other: Throwable) {
                // The flow ended unexpectedly. The error surfaces through the pipeline's
                // quality score rather than a separate UI state, because the recovery path is
                // "process whatever samples we captured". Log to logcat so the failure is
                // diagnosable from a captured trace even though it does not abort the test.
                Log.w(TAG, "gait writer flow ended unexpectedly", other)
            } finally {
                // SPE Phase 4 I1: close in finally so the cancel path flushes the writer's
                // pending rows to disk (or to the test fake) before releasing the handle.
                // CancellationException already propagated above; the finally still runs.
                rawSensorWriter.close()
            }
        }
        captureJob = scope.launch {
            var elapsed = 0
            while (elapsed < CAPTURE_DURATION_MILLIS) {
                delay(TICK_INTERVAL_MILLIS.toLong())
                elapsed = (elapsed + TICK_INTERVAL_MILLIS).coerceAtMost(CAPTURE_DURATION_MILLIS)
                _state.value = GaitTestState.Capturing(progressMillis = elapsed)
            }
            finishCapture()
        }
    }

    private suspend fun finishCapture() {
        // SPE Phase 4 I1: stop the producer first so no new emissions enter the buffer, then
        // cancel and join the writer so any samples still in the shared flow buffer are flushed
        // through appendRow before the writer's finally block closes the handle.
        imuSource.stop()
        try {
            writerJob?.cancelAndJoin()
        } catch (_: CancellationException) {
            // Expected when the parent scope is already cancelling.
        }
        writerJob = null
        val features = gaitPipeline.process(capturedSamples.toList())
        _state.value = GaitTestState.Done(features = features)
    }

    fun onCancel() {
        if (_state.value !is GaitTestState.Capturing) return
        val capture = captureJob
        val writer = writerJob
        captureJob = null
        writerJob = null
        // SPE Phase 4 I1: stop the producer first so no new samples enter the buffer, then
        // cancel and join the writer job so the flow collector drains any in flight samples
        // and the writer's finally block closes the handle before we transition to Cancelled.
        imuSource.stop()
        scope.launch {
            capture?.cancel()
            try {
                writer?.cancelAndJoin()
            } catch (_: CancellationException) {
                // Expected when the parent scope is already cancelling.
            }
            _state.value = GaitTestState.Cancelled
        }
    }

    fun onContinue(callback: (TestResultPayload) -> Unit) {
        val current = _state.value
        if (current !is GaitTestState.Done) return
        val relative = destinationFile.relativeTo(filesDir).path
        callback(
            GaitTestResult(
                qualityScore = current.features.qualityScore,
                features = current.features.toMap(),
                rawSensorRelativePath = relative
            )
        )
    }

    private data class GaitTestResult(
        override val qualityScore: Double,
        override val features: Map<String, Double>,
        override val rawSensorRelativePath: String
    ) : TestResultPayload

    companion object {
        const val COUNTDOWN_SECONDS: Int = 3
        const val CAPTURE_DURATION_MILLIS: Int = 30_000
        const val TICK_INTERVAL_MILLIS: Int = 100
        private const val TAG: String = "GaitTestViewModel"
    }
}
