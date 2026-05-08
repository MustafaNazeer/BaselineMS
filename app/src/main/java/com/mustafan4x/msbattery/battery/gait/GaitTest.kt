package com.mustafan4x.msbattery.battery.gait

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.mustafan4x.msbattery.battery.TestModule
import com.mustafan4x.msbattery.battery.TestResultPayload
import com.mustafan4x.msbattery.data.TestType
import kotlinx.coroutines.CoroutineScope

/**
 * Phase 4 `TestModule` implementation for the gait test. The Compose `Content(onComplete)`
 * builds a `GaitTestViewModel`, observes its state, and renders the appropriate screen.
 *
 * DI shape: the constructor takes a `viewModelFactory` lambda parameterized by the active
 * `CoroutineScope` (`rememberCoroutineScope` inside the composable). The factory is responsible
 * for resolving the sensor source, the DSP pipeline, the raw writer, the destination file, and
 * the application's `filesDir` at the moment Content composes. This keeps the module Hilt free
 * (matching the project's "light Hilt usage" stance per `SPEC.md` Section 12) while still
 * letting `MSBatteryApp` swap the wiring for tests if needed.
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
                        onComplete(skippedPayload())
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
                    state = GaitTestState.Done(features = emptyFeatures()),
                    onContinue = { onComplete(skippedPayload()) }
                )
            }
        }
    }

    private fun skippedPayload(): TestResultPayload = object : TestResultPayload {
        override val qualityScore: Double = 0.0
        override val features: Map<String, Double> = emptyMap()
        override val rawSensorRelativePath: String? = null
    }

    private fun emptyFeatures() = com.mustafan4x.msbattery.dsp.GaitFeatures(
        cadenceStepsPerMinute = 0.0,
        meanStrideLengthMeters = 0.0,
        stepTimeCv = 0.0,
        strideAsymmetryIndex = 0.0,
        doubleSupportTimeSeconds = 0.0,
        qualityScore = 0.0,
        detectedStepCount = 0
    )
}
