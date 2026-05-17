package com.mustafanazeer.baselinems.ui.reports

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.mustafanazeer.baselinems.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SummaryTableTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val gaitFeatureKeys = listOf(
        "cadence_steps_per_minute",
        "mean_stride_length_meters"
    )

    private val gaitFeatureLabels = mapOf(
        "cadence_steps_per_minute" to "Cadence",
        "mean_stride_length_meters" to "Stride length"
    )

    @Test
    fun em_dash_renders_when_feature_value_missing() {
        val rows = listOf(
            SummaryRow(
                epochMs = 1L,
                dateLabel = "2026-05-16",
                perFeatureValues = mapOf("cadence_steps_per_minute" to "96"),
                qualityBand = QualityBand.HIGH
            )
        )
        composeRule.setContent {
            SummaryTable(rows = rows, perFeatureKeys = gaitFeatureKeys, perFeatureLabels = gaitFeatureLabels)
        }
        composeRule.onAllNodesWithText("—").assertCountEquals(1)
    }

    @Test
    fun context_cell_renders_for_low_quality_row_only() {
        val rows = listOf(
            SummaryRow(
                epochMs = 1L,
                dateLabel = "2026-05-10",
                perFeatureValues = mapOf(
                    "cadence_steps_per_minute" to "85",
                    "mean_stride_length_meters" to "0.95"
                ),
                qualityBand = QualityBand.HIGH
            ),
            SummaryRow(
                epochMs = 2L,
                dateLabel = "2026-05-16",
                perFeatureValues = mapOf(
                    "cadence_steps_per_minute" to "82",
                    "mean_stride_length_meters" to "0.91"
                ),
                qualityBand = QualityBand.LOW,
                contextCellResId = R.string.phase9_context_cell_gait_short
            )
        )
        composeRule.setContent {
            SummaryTable(rows = rows, perFeatureKeys = gaitFeatureKeys, perFeatureLabels = gaitFeatureLabels)
        }
        composeRule.onNodeWithText("Short capture. Fewer than 20 detected steps; the data is kept.").assertExists()
    }

    @Test
    fun voice_steadiness_column_appears_only_when_include_flag_is_true() {
        val voiceKeys = listOf("jitter_local", "shimmer_local")
        val voiceLabels = mapOf("jitter_local" to "Jitter", "shimmer_local" to "Shimmer")
        val rows = listOf(
            SummaryRow(
                epochMs = 1L,
                dateLabel = "2026-05-16",
                perFeatureValues = mapOf("jitter_local" to "0.012", "shimmer_local" to "0.045"),
                qualityBand = QualityBand.HIGH,
                steadinessBandLabel = "Steady"
            )
        )
        composeRule.setContent {
            SummaryTable(
                rows = rows,
                perFeatureKeys = voiceKeys,
                perFeatureLabels = voiceLabels,
                includeSteadinessColumn = true
            )
        }
        composeRule.onNodeWithText("Steadiness").assertIsDisplayed()
        composeRule.onNodeWithText("Steady").assertIsDisplayed()
    }

    @Test
    fun steadiness_column_absent_when_include_flag_false() {
        val rows = listOf(
            SummaryRow(
                epochMs = 1L,
                dateLabel = "2026-05-16",
                perFeatureValues = mapOf("cadence_steps_per_minute" to "96"),
                qualityBand = QualityBand.HIGH
            )
        )
        composeRule.setContent {
            SummaryTable(
                rows = rows,
                perFeatureKeys = gaitFeatureKeys,
                perFeatureLabels = gaitFeatureLabels,
                includeSteadinessColumn = false
            )
        }
        composeRule.onNodeWithText("Steadiness").assertDoesNotExist()
    }

    @Test
    fun show_all_sessions_expander_appears_when_row_count_exceeds_default_limit() {
        val rows = (1..15).map { index ->
            SummaryRow(
                epochMs = index.toLong(),
                dateLabel = "2026-05-${index.toString().padStart(2, '0')}",
                perFeatureValues = mapOf(
                    "cadence_steps_per_minute" to "9$index",
                    "mean_stride_length_meters" to "0.9$index"
                ),
                qualityBand = QualityBand.HIGH
            )
        }
        composeRule.setContent {
            SummaryTable(rows = rows, perFeatureKeys = gaitFeatureKeys, perFeatureLabels = gaitFeatureLabels)
        }
        composeRule.onNodeWithText("Show all sessions").assertExists()
    }

    @Test
    fun show_all_sessions_expander_hidden_when_row_count_under_default_limit() {
        val rows = (1..5).map { index ->
            SummaryRow(
                epochMs = index.toLong(),
                dateLabel = "2026-05-${index.toString().padStart(2, '0')}",
                perFeatureValues = mapOf(
                    "cadence_steps_per_minute" to "9$index",
                    "mean_stride_length_meters" to "0.9$index"
                ),
                qualityBand = QualityBand.HIGH
            )
        }
        composeRule.setContent {
            SummaryTable(rows = rows, perFeatureKeys = gaitFeatureKeys, perFeatureLabels = gaitFeatureLabels)
        }
        composeRule.onNodeWithText("Show all sessions").assertDoesNotExist()
    }

    @Test
    fun per_chart_summary_line_renders_one_session_variant() {
        val series = FeatureSeries(
            key = "cadence_steps_per_minute",
            displayName = "Cadence",
            unit = "",
            points = listOf(
                TimedPoint(epochMs = 1L, value = 96.0, qualityBand = QualityBand.HIGH)
            ),
            unitPhraseResId = R.string.phase9_unit_phrase_gait_cadence,
            unitSuffixResId = R.string.phase9_unit_suffix_steps_per_minute
        )
        composeRule.setContent {
            PerChartSummaryLine(series = series)
        }
        composeRule.onNodeWithText("Your latest cadence: 96.").assertExists()
    }

    @Test
    fun per_chart_summary_line_renders_2_3_sessions_variant() {
        val series = FeatureSeries(
            key = "cadence_steps_per_minute",
            displayName = "Cadence",
            unit = "",
            points = listOf(
                TimedPoint(epochMs = 1_700_000_000_000L, value = 94.0, qualityBand = QualityBand.HIGH),
                TimedPoint(epochMs = 1_700_500_000_000L, value = 95.0, qualityBand = QualityBand.HIGH),
                TimedPoint(epochMs = 1_701_000_000_000L, value = 96.0, qualityBand = QualityBand.HIGH)
            ),
            unitPhraseResId = R.string.phase9_unit_phrase_gait_cadence,
            unitSuffixResId = R.string.phase9_unit_suffix_steps_per_minute
        )
        composeRule.setContent {
            PerChartSummaryLine(series = series)
        }
        composeRule.onAllNodesWithText("Your latest cadence: 96", substring = true)
            .assertCountEquals(1)
        composeRule.onAllNodesWithText("Past 3 sessions: median 95", substring = true)
            .assertCountEquals(1)
    }

    @Test
    fun per_chart_summary_line_renders_4plus_sessions_variant() {
        val series = FeatureSeries(
            key = "cadence_steps_per_minute",
            displayName = "Cadence",
            unit = "",
            points = (0L until 6L).map { i ->
                TimedPoint(
                    epochMs = 1_700_000_000_000L + i * 86_400_000L,
                    value = 90.0 + i,
                    qualityBand = QualityBand.HIGH
                )
            },
            unitPhraseResId = R.string.phase9_unit_phrase_gait_cadence,
            unitSuffixResId = R.string.phase9_unit_suffix_steps_per_minute
        )
        composeRule.setContent {
            PerChartSummaryLine(series = series)
        }
        composeRule.onAllNodesWithText("Your latest cadence: 95", substring = true)
            .assertCountEquals(1)
        composeRule.onAllNodesWithText("Last 4 sessions: median", substring = true)
            .assertCountEquals(1)
    }

    @Test
    fun voice_directive_clipping_annotation_renders_when_resId_present() {
        val voiceKeys = listOf("jitter_local", "shimmer_local")
        val voiceLabels = mapOf("jitter_local" to "Jitter", "shimmer_local" to "Shimmer")
        val rows = listOf(
            SummaryRow(
                epochMs = 1L,
                dateLabel = "2026-05-16",
                perFeatureValues = mapOf("jitter_local" to "0.012", "shimmer_local" to "0.045"),
                qualityBand = QualityBand.HIGH,
                steadinessBandLabel = "Steady",
                sessionAnnotationResIds = listOf(R.string.phase9_voice_directive_clipping)
            )
        )
        composeRule.setContent {
            SummaryTable(
                rows = rows,
                perFeatureKeys = voiceKeys,
                perFeatureLabels = voiceLabels,
                includeSteadinessColumn = true
            )
        }
        composeRule.onNodeWithText(
            "Voice was too loud. Try a normal speaking volume next time."
        ).assertExists()
    }

    @Test
    fun voice_directive_noisy_and_short_annotations_render_together() {
        val voiceKeys = listOf("jitter_local")
        val voiceLabels = mapOf("jitter_local" to "Jitter")
        val rows = listOf(
            SummaryRow(
                epochMs = 1L,
                dateLabel = "2026-05-16",
                perFeatureValues = mapOf("jitter_local" to "0.012"),
                qualityBand = QualityBand.MEDIUM,
                sessionAnnotationResIds = listOf(
                    R.string.phase9_voice_directive_noisy,
                    R.string.phase9_voice_directive_short
                )
            )
        )
        composeRule.setContent {
            SummaryTable(
                rows = rows,
                perFeatureKeys = voiceKeys,
                perFeatureLabels = voiceLabels,
                includeSteadinessColumn = true
            )
        }
        composeRule.onNodeWithText("Background was noisy. Try a quieter room next time.")
            .assertExists()
        composeRule.onNodeWithText("Reading was too short. Try reading the full passage next time.")
            .assertExists()
    }

    @Test
    fun voice_per_feature_flag_annotation_renders_when_resId_present() {
        val voiceKeys = listOf("jitter_local")
        val voiceLabels = mapOf("jitter_local" to "Jitter")
        val rows = listOf(
            SummaryRow(
                epochMs = 1L,
                dateLabel = "2026-05-16",
                perFeatureValues = mapOf("jitter_local" to "0.025"),
                qualityBand = QualityBand.HIGH,
                perFeatureAnnotationResIds = mapOf(
                    "jitter_local" to R.string.phase9_voice_flag_jitter_unreliable
                )
            )
        )
        composeRule.setContent {
            SummaryTable(
                rows = rows,
                perFeatureKeys = voiceKeys,
                perFeatureLabels = voiceLabels,
                includeSteadinessColumn = true
            )
        }
        composeRule.onNodeWithText("Pitch variation measurement was unreliable this session.")
            .assertExists()
    }
}
