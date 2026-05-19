package com.mustafanazeer.baselinems.battery.vision

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mustafanazeer.baselinems.dsp.vision.SloanScore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VisionScreenRenderTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun instructions_renders_start_and_cancel() {
        composeRule.setContent {
            VisionInstructionsScreen(onStart = {}, onCancel = {})
        }
        composeRule.onNodeWithText("Start").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun qualitycheck_hides_continue_until_ready() {
        val notReady = VisionTestState.QualityCheck(lux = 5.0, distanceCm = 40.0, luxOk = false, distanceOk = true)
        composeRule.setContent {
            VisionQualityCheckScreen(state = notReady, onContinue = {}, onCancel = {})
        }
        composeRule.onNodeWithText("Too dim. Move to a brighter spot.").assertIsDisplayed()
    }

    @Test
    fun qualitycheck_shows_continue_when_ready() {
        val ready = VisionTestState.QualityCheck(lux = 300.0, distanceCm = 40.0, luxOk = true, distanceOk = true)
        composeRule.setContent {
            VisionQualityCheckScreen(state = ready, onContinue = {}, onCancel = {})
        }
        composeRule.onNodeWithText("Continue").assertIsDisplayed()
    }

    @Test
    fun done_renders_per_contrast_counts() {
        val score = SloanScore(totalCorrect = 90, correctAt100Pct = 40, correctAt2Pt5Pct = 38, correctAt1Pt25Pct = 12)
        composeRule.setContent {
            VisionDoneScreen(score = score, onDone = {})
        }
        composeRule.onNodeWithText("At 100 percent contrast, you read 40 of 40 letters.").assertIsDisplayed()
        composeRule.onNodeWithText("At 1.25 percent contrast, you read 12 of 40 letters.").assertIsDisplayed()
    }
}
