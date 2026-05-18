package com.mustafanazeer.baselinems.ui.reports

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mustafanazeer.baselinems.data.TestType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReportsScreenShareTest {

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
            )
        )
    )

    @Test
    fun share_button_enabled_when_state_is_ready() {
        composeRule.setContent {
            ReportsScreen(
                state = readyState,
                exportState = ReportsExportState.Idle,
                onBack = {},
                onCardSelected = {},
                onRunFirstCheckIn = {},
                onShareClicked = {},
                onShareConsumed = {}
            )
        }
        composeRule.onNodeWithText("Share report")
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun share_button_disabled_when_state_is_empty() {
        composeRule.setContent {
            ReportsScreen(
                state = ReportsScreenState.Empty,
                exportState = ReportsExportState.Idle,
                onBack = {},
                onCardSelected = {},
                onRunFirstCheckIn = {},
                onShareClicked = {},
                onShareConsumed = {}
            )
        }
        composeRule.onNodeWithText("Share report")
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }
}
