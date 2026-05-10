package com.mustafanazeer.baselinems.battery.gait

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.mustafanazeer.baselinems.battery.TestModule
import com.mustafanazeer.baselinems.battery.TestResultPayload
import com.mustafanazeer.baselinems.data.TestType
import com.mustafanazeer.baselinems.dsp.GaitFeatures
import kotlinx.coroutines.CoroutineScope

/**
 * `TestModule` implementation for the gait test. The Compose `Content(onComplete)` builds a
 * `GaitTestViewModel`, observes its state, and renders the appropriate screen.
 *
 * DI shape: the constructor takes a `viewModelFactory` lambda parameterized by the active
 * `CoroutineScope` (`rememberCoroutineScope` inside the composable). The factory is responsible
 * for resolving the sensor source, the DSP pipeline, the raw writer, the destination file, and
 * the application's `filesDir` at the moment Content composes. This keeps the module Hilt free
 * while still letting `BaselineMSApp` swap the wiring for tests if needed.
 */
class GaitTest(
    private val viewModelFactory: (CoroutineScope) -> GaitTestViewModel
) : TestModule {

    override val testType: TestType = TestType.GAIT
    override val displayName: String = "Gait"
    override val instructions: String =
        "Walk in a straight line for 30 seconds with the phone in a front pocket. " +
            "Use a flat, unobstructed surface; have a wall or counter within reach if you need it."
    override val estimatedDurationSeconds: Int = 45

    @Composable
    override fun Content(onComplete: (TestResultPayload) -> Unit) {
        val scope = rememberCoroutineScope()
        val viewModel = remember(scope) { viewModelFactory(scope) }
        val state by viewModel.state.collectAsState()

        Box(modifier = Modifier.fillMaxSize()) {
            when (val current = state) {
                is GaitTestState.Instructions -> GaitInstructionsScreen(
                    onStart = { viewModel.onStart() },
                    onSkip = {
                        onComplete(SkippedGaitPayload)
                    }
                )
                is GaitTestState.Countdown -> GaitCountdownScreen(state = current)
                is GaitTestState.Capturing -> GaitCaptureScreen(
                    state = current,
                    onCancel = { viewModel.onCancel() }
                )
                is GaitTestState.Done -> GaitDoneScreen(
                    state = current,
                    onContinue = { viewModel.onContinue(onComplete) }
                )
                is GaitTestState.Cancelled -> GaitDoneScreen(
                    state = GaitTestState.Done(features = GaitFeatures.EMPTY),
                    onContinue = { onComplete(SkippedGaitPayload) }
                )
            }
        }
    }

    private object SkippedGaitPayload : TestResultPayload {
        override val qualityScore: Double = 0.0
        override val features: Map<String, Double> = emptyMap()
        override val rawSensorRelativePath: String? = null
    }
}
