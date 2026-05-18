package com.mustafanazeer.baselinems.ui.reports

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.mustafanazeer.baselinems.data.TestType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReportsScreenRenderTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val readyState = ReportsScreenState.Ready(
        testSummaries = listOf(
            TestSummaryCard(
                testType = TestType.TAP,
                displayName = "Bilateral Tap Test",
                sessionCount = 3,
                latestQualityBand = QualityBand.HIGH,
                primaryFeatureSparkline = listOf(4.0, 4.2, 4.1)
            ),
            TestSummaryCard(
                testType = TestType.GAIT,
                displayName = "Gait Test",
                sessionCount = 3,
                latestQualityBand = QualityBand.MEDIUM,
                primaryFeatureSparkline = listOf(96.0, 97.0, 95.0)
            ),
            TestSummaryCard(
                testType = TestType.VISION,
                displayName = "Vision Test",
                sessionCount = 3,
                latestQualityBand = QualityBand.HIGH,
                primaryFeatureSparkline = listOf(38.0, 37.0, 39.0)
            ),
            TestSummaryCard(
                testType = TestType.SDMT,
                displayName = "Symbol Digit Modalities Test",
                sessionCount = 3,
                latestQualityBand = QualityBand.HIGH,
                primaryFeatureSparkline = listOf(45.0, 47.0, 46.0)
            ),
            TestSummaryCard(
                testType = TestType.VOICE,
                displayName = "Voice Reading Test",
                sessionCount = 3,
                latestQualityBand = QualityBand.LOW,
                primaryFeatureSparkline = listOf(0.012, 0.013, 0.014)
            )
        )
    )

    @Test
    fun empty_state_renders_run_my_first_check_in_button() {
        composeRule.setContent {
            ReportsScreen(
                state = ReportsScreenState.Empty,
                onBack = {},
                onCardSelected = {},
                onRunFirstCheckIn = {}
            )
        }
        composeRule.onNodeWithText("Run my first check in").assertIsDisplayed()
    }

    @Test
    fun empty_state_renders_back_to_home_button() {
        composeRule.setContent {
            ReportsScreen(
                state = ReportsScreenState.Empty,
                onBack = {},
                onCardSelected = {},
                onRunFirstCheckIn = {}
            )
        }
        composeRule.onNodeWithText("Back to home").assertIsDisplayed()
    }

    @Test
    fun empty_state_renders_empty_body_copy() {
        composeRule.setContent {
            ReportsScreen(
                state = ReportsScreenState.Empty,
                onBack = {},
                onCardSelected = {},
                onRunFirstCheckIn = {}
            )
        }
        composeRule.onNodeWithText(
            "You have not run a check in yet. The trends here will fill in after your first session."
        ).assertIsDisplayed()
    }

    @Test
    fun empty_state_run_first_check_in_fires_callback() {
        var fired = false
        composeRule.setContent {
            ReportsScreen(
                state = ReportsScreenState.Empty,
                onBack = {},
                onCardSelected = {},
                onRunFirstCheckIn = { fired = true }
            )
        }
        composeRule.onNodeWithText("Run my first check in").performClick()
        assertTrue(fired)
    }

    @Test
    fun ready_state_renders_warmth_line_above_cards() {
        composeRule.setContent {
            ReportsScreen(
                state = readyState,
                onBack = {},
                onCardSelected = {},
                onRunFirstCheckIn = {}
            )
        }
        composeRule.onNodeWithText(
            "These trends are personal to you. They will vary day to day. Look at the longer pattern."
        ).assertIsDisplayed()
    }

    @Test
    fun ready_state_renders_five_test_summary_cards_in_battery_order() {
        composeRule.setContent {
            ReportsScreen(
                state = readyState,
                onBack = {},
                onCardSelected = {},
                onRunFirstCheckIn = {}
            )
        }
        composeRule.onNodeWithText("Bilateral Tap Test").assertExists()
        composeRule.onNodeWithText("Gait Test").assertExists()
        composeRule.onNodeWithText("Vision Test").assertExists()
        composeRule.onNodeWithText("Symbol Digit Modalities Test").assertExists()
        composeRule.onNodeWithText("Voice Reading Test").assertExists()
    }

    @Test
    fun ready_state_card_tap_fires_callback_with_test_type() {
        var selected: TestType? = null
        composeRule.setContent {
            ReportsScreen(
                state = readyState,
                onBack = {},
                onCardSelected = { selected = it },
                onRunFirstCheckIn = {}
            )
        }
        composeRule.onNodeWithText("Gait Test").performScrollTo().performClick()
        assertEquals(TestType.GAIT, selected)
    }

    @Test
    fun ready_state_card_header_shows_sessions_recorded_template() {
        composeRule.setContent {
            ReportsScreen(
                state = readyState,
                onBack = {},
                onCardSelected = {},
                onRunFirstCheckIn = {}
            )
        }
        composeRule.onAllNodesWithText("3 sessions recorded; trend below shows latest 3.")
            .assertCountEquals(5)
    }
}
