package com.mustafanazeer.baselinems.ui.reports

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.mustafanazeer.baselinems.data.TestType
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TestDetailScreenRenderTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun readySingleFeature(testType: TestType, key: String, displayName: String): TestDetailScreenState.Ready {
        return TestDetailScreenState.Ready(
            testType = testType,
            sessionCount = 3,
            sessionsSinceLabel = "5 December 2025",
            latestQualityBand = QualityBand.HIGH,
            featureSeries = listOf(
                FeatureSeries(
                    key = key,
                    displayName = displayName,
                    unit = "",
                    points = listOf(
                        TimedPoint(epochMs = 1L, value = 1.0, qualityBand = QualityBand.HIGH),
                        TimedPoint(epochMs = 2L, value = 1.2, qualityBand = QualityBand.MEDIUM),
                        TimedPoint(epochMs = 3L, value = 1.1, qualityBand = QualityBand.HIGH)
                    )
                )
            ),
            summaryRows = emptyList()
        )
    }

    @Test
    fun tap_detail_screen_renders_title() {
        composeRule.setContent {
            TapDetailScreen(
                state = readySingleFeature(TestType.TAP, TapFeatureKeys.DOMINANT_RATE, "Taps per second (dominant hand)"),
                onBack = {},
                onAbout = {}
            )
        }
        composeRule.onNodeWithText("Bilateral Tap Test").assertIsDisplayed()
    }

    @Test
    fun gait_detail_screen_renders_title_and_stride_length_caveat() {
        val state = TestDetailScreenState.Ready(
            testType = TestType.GAIT,
            sessionCount = 3,
            sessionsSinceLabel = "5 December 2025",
            latestQualityBand = QualityBand.HIGH,
            featureSeries = listOf(
                FeatureSeries(
                    key = GaitFeatureKeys.CADENCE,
                    displayName = "Cadence (steps per minute)",
                    unit = "",
                    points = listOf(
                        TimedPoint(epochMs = 1L, value = 96.0, qualityBand = QualityBand.HIGH)
                    )
                ),
                FeatureSeries(
                    key = GaitFeatureKeys.STRIDE_LENGTH,
                    displayName = "Stride length (cm)",
                    unit = "",
                    points = listOf(
                        TimedPoint(epochMs = 1L, value = 65.0, qualityBand = QualityBand.HIGH)
                    )
                )
            ),
            summaryRows = emptyList()
        )
        composeRule.setContent {
            GaitDetailScreen(state = state, onBack = {}, onAbout = {})
        }
        composeRule.onNodeWithText("Gait Test").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Stride length values are calibrated for within device trend comparison only; " +
                "they are not directly comparable to clinical gait laboratory measurements."
        ).assertExists()
    }

    @Test
    fun vision_detail_screen_renders_title() {
        composeRule.setContent {
            VisionDetailScreen(
                state = readySingleFeature(TestType.VISION, VisionFeatureKeys.SLOAN_TOTAL, "Total letters correct (out of 120)"),
                onBack = {},
                onAbout = {}
            )
        }
        composeRule.onNodeWithText("Vision Test").assertIsDisplayed()
    }

    @Test
    fun sdmt_detail_screen_renders_title() {
        composeRule.setContent {
            SdmtDetailScreen(
                state = readySingleFeature(TestType.SDMT, SdmtFeatureKeys.CORRECT_IN_90S, "Symbols matched in 90 seconds"),
                onBack = {},
                onAbout = {}
            )
        }
        composeRule.onNodeWithText("Symbol Digit Modalities Test").assertIsDisplayed()
    }

    @Test
    fun voice_detail_screen_renders_title() {
        composeRule.setContent {
            VoiceDetailScreen(
                state = readySingleFeature(TestType.VOICE, VoiceFeatureKeys.JITTER_LOCAL, "Voice cycle to cycle frequency variation"),
                onBack = {},
                onAbout = {}
            )
        }
        composeRule.onNodeWithText("Voice Reading Test").assertIsDisplayed()
    }

    @Test
    fun voice_detail_screen_progressive_disclosure_default_closed_hides_non_primary_charts() {
        val state = TestDetailScreenState.Ready(
            testType = TestType.VOICE,
            sessionCount = 3,
            sessionsSinceLabel = null,
            latestQualityBand = QualityBand.HIGH,
            featureSeries = listOf(
                FeatureSeries(
                    key = VoiceFeatureKeys.JITTER_LOCAL,
                    displayName = "Voice cycle to cycle frequency variation",
                    unit = "",
                    points = listOf(TimedPoint(1L, 0.012, QualityBand.HIGH))
                ),
                FeatureSeries(
                    key = VoiceFeatureKeys.SHIMMER_LOCAL,
                    displayName = "Voice cycle to cycle amplitude variation",
                    unit = "",
                    points = listOf(TimedPoint(1L, 0.045, QualityBand.HIGH))
                ),
                FeatureSeries(
                    key = VoiceFeatureKeys.HNR_DB,
                    displayName = "Voice harmonics to noise ratio (dB)",
                    unit = "",
                    points = listOf(TimedPoint(1L, 20.3, QualityBand.HIGH))
                )
            ),
            summaryRows = emptyList()
        )
        composeRule.setContent {
            VoiceDetailScreen(
                state = state,
                onBack = {},
                onAbout = {},
                primaryFeatureKey = VoiceFeatureKeys.JITTER_LOCAL
            )
        }
        composeRule.onNodeWithText("Voice cycle to cycle frequency variation").assertExists()
        composeRule.onNodeWithText("Show all voice measures").assertExists()
        composeRule.onNodeWithText("Voice cycle to cycle amplitude variation").assertDoesNotExist()
        composeRule.onNodeWithText("Voice harmonics to noise ratio (dB)").assertDoesNotExist()
    }

    @Test
    fun voice_detail_screen_expander_reveals_non_primary_charts() {
        val state = TestDetailScreenState.Ready(
            testType = TestType.VOICE,
            sessionCount = 3,
            sessionsSinceLabel = null,
            latestQualityBand = QualityBand.HIGH,
            featureSeries = listOf(
                FeatureSeries(
                    key = VoiceFeatureKeys.JITTER_LOCAL,
                    displayName = "Voice cycle to cycle frequency variation",
                    unit = "",
                    points = listOf(TimedPoint(1L, 0.012, QualityBand.HIGH))
                ),
                FeatureSeries(
                    key = VoiceFeatureKeys.SHIMMER_LOCAL,
                    displayName = "Voice cycle to cycle amplitude variation",
                    unit = "",
                    points = listOf(TimedPoint(1L, 0.045, QualityBand.HIGH))
                )
            ),
            summaryRows = emptyList()
        )
        composeRule.setContent {
            VoiceDetailScreen(
                state = state,
                onBack = {},
                onAbout = {},
                primaryFeatureKey = VoiceFeatureKeys.JITTER_LOCAL
            )
        }
        composeRule.onNodeWithText("Show all voice measures").performScrollTo().performClick()
        composeRule.onNodeWithText("Voice cycle to cycle amplitude variation").assertExists()
    }

    @Test
    fun detail_screen_renders_about_link() {
        composeRule.setContent {
            TapDetailScreen(
                state = readySingleFeature(TestType.TAP, TapFeatureKeys.DOMINANT_RATE, "Taps per second (dominant hand)"),
                onBack = {},
                onAbout = {}
            )
        }
        composeRule.onNodeWithText("About these charts").assertExists()
    }

    @Test
    fun empty_state_renders_body_copy() {
        composeRule.setContent {
            TapDetailScreen(
                state = TestDetailScreenState.Empty,
                onBack = {},
                onAbout = {}
            )
        }
        composeRule.onNodeWithText(
            "No sessions recorded for this test yet. Run a check in to start seeing your trends."
        ).assertIsDisplayed()
    }

    @Test
    fun empty_state_renders_back_to_trends_button() {
        composeRule.setContent {
            TapDetailScreen(
                state = TestDetailScreenState.Empty,
                onBack = {},
                onAbout = {}
            )
        }
        composeRule.onNodeWithText("Back to trends").assertIsDisplayed()
    }

    @Test
    fun empty_state_back_to_trends_fires_callback() {
        var fired = false
        composeRule.setContent {
            TapDetailScreen(
                state = TestDetailScreenState.Empty,
                onBack = { fired = true },
                onAbout = {}
            )
        }
        composeRule.onNodeWithText("Back to trends").performClick()
        assertTrue(fired)
    }

    @Test
    fun detail_screen_with_empty_summary_rows_does_not_crash() {
        composeRule.setContent {
            TapDetailScreen(
                state = TestDetailScreenState.Ready(
                    testType = TestType.TAP,
                    sessionCount = 1,
                    sessionsSinceLabel = null,
                    latestQualityBand = QualityBand.HIGH,
                    featureSeries = emptyList(),
                    summaryRows = emptyList()
                ),
                onBack = {},
                onAbout = {}
            )
        }
        composeRule.onNodeWithText("Bilateral Tap Test").assertIsDisplayed()
        composeRule.onNodeWithText("Run another check in to see how this changes over time.").assertIsDisplayed()
    }
}
