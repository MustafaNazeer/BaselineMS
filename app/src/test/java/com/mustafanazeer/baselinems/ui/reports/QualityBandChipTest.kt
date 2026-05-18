package com.mustafanazeer.baselinems.ui.reports

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QualityBandChipTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun chip_high_renders_reliable_label() {
        composeRule.setContent {
            QualityBandChip(band = QualityBand.HIGH)
        }
        composeRule.onNodeWithText("Reliable").assertIsDisplayed()
    }

    @Test
    fun chip_medium_renders_mostly_reliable_label() {
        composeRule.setContent {
            QualityBandChip(band = QualityBand.MEDIUM)
        }
        composeRule.onNodeWithText("Mostly reliable").assertIsDisplayed()
    }

    @Test
    fun chip_low_renders_less_reliable_label() {
        composeRule.setContent {
            QualityBandChip(band = QualityBand.LOW)
        }
        composeRule.onNodeWithText("Less reliable").assertIsDisplayed()
    }

    @Test
    fun chip_high_does_not_render_quality_high_wording() {
        composeRule.setContent {
            QualityBandChip(band = QualityBand.HIGH)
        }
        composeRule.onNodeWithText("Quality: High").assertDoesNotExist()
        composeRule.onNodeWithText("Session quality: High").assertDoesNotExist()
    }

    @Test
    fun chip_medium_does_not_render_quality_medium_wording() {
        composeRule.setContent {
            QualityBandChip(band = QualityBand.MEDIUM)
        }
        composeRule.onNodeWithText("Quality: Medium").assertDoesNotExist()
        composeRule.onNodeWithText("Session quality: Medium").assertDoesNotExist()
    }

    @Test
    fun chip_low_does_not_render_quality_low_wording() {
        composeRule.setContent {
            QualityBandChip(band = QualityBand.LOW)
        }
        composeRule.onNodeWithText("Quality: Low").assertDoesNotExist()
        composeRule.onNodeWithText("Session quality: Low").assertDoesNotExist()
    }
}
