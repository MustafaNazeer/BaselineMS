package com.mustafanazeer.baselinems.battery.gait

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mustafanazeer.baselinems.dsp.GaitFeatures
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Compose smoke tests for the four gait screens. Each test verifies the screen renders without
 * crashing and exposes a semantic node for the primary CTA (or, for the countdown screen which
 * has no CTA, the visible numeric display). Same Robolectric pattern as
 * `BilateralTapTestRenderTest` so the JVM Compose toolchain is shared across modules.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class GaitTestRenderTest {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun instructionsRendersWithReadyAndSkipCtas() {
        composeRule.setContent {
            GaitInstructionsScreen(onStart = {}, onSkip = {})
        }
        composeRule.onNodeWithText("I am ready").assertIsDisplayed()
        composeRule.onNodeWithText("Skip for now").assertIsDisplayed()
    }

    @Test
    fun countdownRendersTheRemainingSeconds() {
        composeRule.setContent {
            GaitCountdownScreen(state = GaitTestState.Countdown(secondsRemaining = 3))
        }
        composeRule.onNodeWithText("3").assertIsDisplayed()
    }

    @Test
    fun captureRendersWalkingIndicatorAndCancel() {
        composeRule.setContent {
            GaitCaptureScreen(
                state = GaitTestState.Capturing(progressMillis = 12_000),
                onCancel = {}
            )
        }
        composeRule.onNodeWithText("Walking").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun doneRendersConfidenceLineAndContinueCta() {
        val features = GaitFeatures(
            cadenceStepsPerMinute = 110.0,
            meanStrideLengthMeters = 1.2,
            stepTimeCv = 0.05,
            strideAsymmetryIndex = 0.02,
            doubleSupportTimeSeconds = 0.2,
            qualityScore = 0.9,
            detectedStepCount = 55
        )
        composeRule.setContent {
            GaitDoneScreen(state = GaitTestState.Done(features = features), onContinue = {})
        }
        composeRule.onNodeWithText("Reliable").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").assertIsDisplayed()
    }
}
