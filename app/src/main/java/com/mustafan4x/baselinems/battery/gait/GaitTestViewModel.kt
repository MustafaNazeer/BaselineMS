package com.mustafan4x.baselinems.battery.gait

import com.mustafan4x.baselinems.battery.TestResultPayload
import com.mustafan4x.baselinems.dsp.GaitPipeline
import com.mustafan4x.baselinems.dsp.ImuSample
import com.mustafan4x.baselinems.signals.ImuSource
import com.mustafan4x.baselinems.signals.RawSensorWriter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File

/**
 * Phase 4 view model that orchestrates the gait test state machine and the sensor capture.
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
    private val rawSensorWriter: RawSensorWriter,
    private val destinationFile: File,
    private val filesDir: File,
    private val scope: CoroutineScope
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
        writerJob = scope.launch {
            try {
                rawSensorWriter.write(
                    imuSource.stream().onEach { capturedSamples += it }
                )
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (other: Throwable) {
                // The flow ended unexpectedly. Phase 4 surfaces the error through the
                // pipeline's quality score, not through a separate UI state, because the
                // recovery path is "process whatever samples we captured".
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

    private fun finishCapture() {
        imuSource.stop()
        writerJob?.cancel()
        writerJob = null
        val features = gaitPipeline.process(capturedSamples.toList())
        _state.value = GaitTestState.Done(features = features)
    }

    fun onCancel() {
        if (_state.value !is GaitTestState.Capturing) return
        captureJob?.cancel()
        writerJob?.cancel()
        captureJob = null
        writerJob = null
        imuSource.stop()
        _state.value = GaitTestState.Cancelled
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
    }
}
