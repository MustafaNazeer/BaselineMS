package com.mustafanazeer.baselinems.battery.sdmt

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mustafanazeer.baselinems.dsp.sdmt.SdmtScore
import com.mustafanazeer.baselinems.dsp.sdmt.SdmtSymbol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SdmtScreenRenderTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun instructions_renders_start_and_cancel() {
        composeRule.setContent {
            SdmtInstructionsScreen(onStart = {}, onCancel = {})
        }
        composeRule.onNodeWithText("Start").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel the test and discard this session.").assertIsDisplayed()
    }

    @Test
    fun instructions_renders_wps_caveat_short_form() {
        composeRule.setContent {
            SdmtInstructionsScreen(onStart = {}, onCancel = {})
        }
        composeRule.onNodeWithText(
            "This is a smartphone variant of the Symbol Digit Modalities Test (SDMT) developed " +
                "for personal tracking. The Symbol Digit Modalities Test is a registered " +
                "instrument of Western Psychological Services; the symbols used here are custom " +
                "abstract shapes, not the official SDMT stimulus materials, and scores from this " +
                "app are not directly comparable to a clinician administered SDMT."
        ).assertIsDisplayed()
    }

    @Test
    fun instructions_start_button_fires_callback() {
        var started = false
        composeRule.setContent {
            SdmtInstructionsScreen(onStart = { started = true }, onCancel = {})
        }
        composeRule.onNodeWithText("Start").performClick()
        assertTrue(started)
    }

    @Test
    fun runner_renders_all_nine_digit_buttons() {
        val state = SdmtTestState.Running(
            currentPrompt = SdmtSymbol.SYMBOL_1,
            promptShownAtMs = 0L,
            elapsedMs = 0L,
            totalAttempted = 0,
            totalCorrect = 0,
            consecutiveErrors = 0,
            showCountdown = false
        )
        composeRule.setContent {
            SdmtRunnerScreen(state = state, onDigitTap = {}, onCancel = {})
        }
        for (digit in 1..9) {
            composeRule.onAllNodesWithText(digit.toString()).assertCountEquals(2)
        }
    }

    @Test
    fun runner_digit_tap_fires_callback_with_correct_digit() {
        var tapped = -1
        val state = SdmtTestState.Running(
            currentPrompt = SdmtSymbol.SYMBOL_1,
            promptShownAtMs = 0L,
            elapsedMs = 0L,
            totalAttempted = 0,
            totalCorrect = 0,
            consecutiveErrors = 0,
            showCountdown = false
        )
        composeRule.setContent {
            SdmtRunnerScreen(state = state, onDigitTap = { tapped = it }, onCancel = {})
        }
        composeRule.onAllNodesWithText("5")
            .filter(hasClickAction())
            .assertCountEquals(1)[0]
            .performClick()
        assertEquals(5, tapped)
    }

    @Test
    fun runner_renders_reassurance_after_three_consecutive_errors() {
        val state = SdmtTestState.Running(
            currentPrompt = SdmtSymbol.SYMBOL_1,
            promptShownAtMs = 0L,
            elapsedMs = 10_000L,
            totalAttempted = 3,
            totalCorrect = 0,
            consecutiveErrors = 3,
            showCountdown = false
        )
        composeRule.setContent {
            SdmtRunnerScreen(state = state, onDigitTap = {}, onCancel = {})
        }
        composeRule.onNodeWithText(
            "The mapping takes a moment. The key is at the top of the screen."
        ).assertIsDisplayed()
    }

    @Test
    fun runner_does_not_show_countdown_when_disabled() {
        val state = SdmtTestState.Running(
            currentPrompt = SdmtSymbol.SYMBOL_1,
            promptShownAtMs = 0L,
            elapsedMs = 30_000L,
            totalAttempted = 5,
            totalCorrect = 4,
            consecutiveErrors = 0,
            showCountdown = false
        )
        composeRule.setContent {
            SdmtRunnerScreen(state = state, onDigitTap = {}, onCancel = {})
        }
        composeRule.onNodeWithText("60 s remaining").assertDoesNotExist()
    }

    @Test
    fun runner_shows_countdown_when_enabled() {
        val state = SdmtTestState.Running(
            currentPrompt = SdmtSymbol.SYMBOL_1,
            promptShownAtMs = 0L,
            elapsedMs = 30_000L,
            totalAttempted = 5,
            totalCorrect = 4,
            consecutiveErrors = 0,
            showCountdown = true
        )
        composeRule.setContent {
            SdmtRunnerScreen(state = state, onDigitTap = {}, onCancel = {})
        }
        composeRule.onNodeWithText("60 s remaining").assertIsDisplayed()
    }

    @Test
    fun done_renders_warmth_headline_and_per_feature_lines() {
        val score = SdmtScore(
            totalCorrect = 32,
            totalAttempted = 40,
            totalErrors = 8,
            errorRate = 0.2,
            responseTimeMeanMs = 1200.0,
            responseTimeSdMs = 250.0,
            responseTimes = emptyList(),
            qualityScore = 1.0
        )
        composeRule.setContent {
            SdmtDoneScreen(score = score, onDone = {})
        }
        composeRule.onNodeWithText("SDMT recorded").assertIsDisplayed()
        composeRule.onNodeWithText(
            "You answered 32 of 40 correctly in 90 seconds."
        ).assertIsDisplayed()
    }

    @Test
    fun cancelled_renders_no_per_feature_line() {
        composeRule.setContent {
            SdmtCancelledScreen(onDone = {})
        }
        composeRule.onNodeWithText("Test cancelled").assertIsDisplayed()
        composeRule.onNodeWithText("Nothing was saved for this session.").assertIsDisplayed()
        composeRule.onNodeWithText("Back to home").assertIsDisplayed()
    }

    @Test
    fun cancelled_back_to_home_fires_callback() {
        var done = false
        composeRule.setContent {
            SdmtCancelledScreen(onDone = { done = true })
        }
        composeRule.onNodeWithText("Back to home").performClick()
        assertTrue(done)
    }
}
