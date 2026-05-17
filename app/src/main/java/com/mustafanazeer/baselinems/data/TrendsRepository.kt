package com.mustafanazeer.baselinems.data

import com.mustafanazeer.baselinems.R
import com.mustafanazeer.baselinems.ui.reports.FeatureSeries
import com.mustafanazeer.baselinems.ui.reports.QualityBand
import com.mustafanazeer.baselinems.ui.reports.ReportsScreenState
import com.mustafanazeer.baselinems.ui.reports.SummaryRow
import com.mustafanazeer.baselinems.ui.reports.TestDetailScreenState
import com.mustafanazeer.baselinems.ui.reports.TestSummaryCard
import com.mustafanazeer.baselinems.ui.reports.TimedPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val KEY_JITTER_LOCAL: String = "jitter_local"
private const val KEY_SHIMMER_LOCAL: String = "shimmer_local"
private const val KEY_HNR_DB: String = "hnr_db"
private const val KEY_F0_SD_HZ: String = "f0_sd_hz"
private const val KEY_PAUSE_FRACTION: String = "pause_fraction"

private const val STEADINESS_JITTER_STEADY_MAX: Double = 0.010
private const val STEADINESS_SHIMMER_STEADY_MAX: Double = 0.040
private const val STEADINESS_JITTER_MOSTLY_MAX: Double = 0.020
private const val STEADINESS_SHIMMER_MOSTLY_MAX: Double = 0.070

internal enum class SteadinessBandLabel { STEADY, MOSTLY_STEADY, VARIED, UNMEASURABLE }

internal fun deriveSteadinessBand(jitterLocal: Double?, shimmerLocal: Double?): SteadinessBandLabel {
    if (jitterLocal == null || shimmerLocal == null) return SteadinessBandLabel.UNMEASURABLE
    return when {
        jitterLocal < STEADINESS_JITTER_STEADY_MAX && shimmerLocal < STEADINESS_SHIMMER_STEADY_MAX ->
            SteadinessBandLabel.STEADY
        jitterLocal < STEADINESS_JITTER_MOSTLY_MAX && shimmerLocal < STEADINESS_SHIMMER_MOSTLY_MAX ->
            SteadinessBandLabel.MOSTLY_STEADY
        else -> SteadinessBandLabel.VARIED
    }
}

private val voicePerFeatureFlagAnnotationResIds: Map<String, Int> = mapOf(
    KEY_JITTER_LOCAL to R.string.phase9_voice_flag_jitter_unreliable,
    KEY_SHIMMER_LOCAL to R.string.phase9_voice_flag_shimmer_unreliable,
    KEY_HNR_DB to R.string.phase9_voice_flag_hnr_unreliable,
    KEY_F0_SD_HZ to R.string.phase9_voice_flag_f0_sd_unreliable,
    KEY_PAUSE_FRACTION to R.string.phase9_voice_flag_pause_fraction_unreliable
)

const val QUALITY_BAND_HIGH_THRESHOLD: Double = 0.7
const val QUALITY_BAND_MEDIUM_THRESHOLD: Double = 0.4

private const val SPEAKING_RATE_OMISSION_WPM_CEILING: Double = 400.0
private const val SPEAKING_RATE_OMISSION_VOICED_SECONDS_FLOOR: Double = 20.0

private const val KEY_QUALITY_FLAGS: String = "_qualityFlags"
private const val KEY_SESSION_FLAGS: String = "_sessionFlags"
private const val KEY_ENGAGEMENT_OK: String = "engagementOk"
private const val KEY_CLIPPING_DETECTED: String = "clippingDetected"
private const val KEY_SNR_ADEQUATE: String = "snrAdequate"
private const val KEY_SPEAKING_RATE_WPM: String = "speaking_rate_wpm"
private const val KEY_VOICED_SECONDS: String = "voiced_seconds"
private const val KEY_DOMINANT_VALID_TAPS: String = "dominant_in_target_taps"
private const val KEY_NON_DOMINANT_VALID_TAPS: String = "non_dominant_in_target_taps"
private const val KEY_DETECTED_STEP_COUNT: String = "detected_step_count"

fun deriveQualityBand(qualityScore: Double): QualityBand = when {
    qualityScore >= QUALITY_BAND_HIGH_THRESHOLD -> QualityBand.HIGH
    qualityScore >= QUALITY_BAND_MEDIUM_THRESHOLD -> QualityBand.MEDIUM
    else -> QualityBand.LOW
}

internal data class ParsedSession(
    val features: Map<String, Double>,
    val qualityFlags: Map<String, Boolean>,
    val engagementOk: Boolean,
    val clippingDetected: Boolean,
    val snrAdequate: Boolean
)

private val jsonParser: Json = Json { ignoreUnknownKeys = true }

internal fun parseFeaturesJson(featuresJson: String): ParsedSession {
    val root: JsonObject = try {
        jsonParser.parseToJsonElement(featuresJson).jsonObject
    } catch (t: Throwable) {
        return ParsedSession(
            features = emptyMap(),
            qualityFlags = emptyMap(),
            engagementOk = true,
            clippingDetected = false,
            snrAdequate = true
        )
    }
    val features = mutableMapOf<String, Double>()
    val qualityFlags = mutableMapOf<String, Boolean>()
    var engagementOk = true
    var clippingDetected = false
    var snrAdequate = true

    for ((key, element) in root) {
        if (key.startsWith("_")) continue
        val prim = (element as? JsonPrimitive) ?: continue
        prim.doubleOrNull?.let { features[key] = it }
    }

    (root[KEY_QUALITY_FLAGS] as? JsonObject)?.let { flagObj ->
        for ((key, element) in flagObj) {
            (element as? JsonPrimitive)?.booleanOrNull?.let { qualityFlags[key] = it }
        }
    }
    (root[KEY_SESSION_FLAGS] as? JsonObject)?.let { sessionObj ->
        (sessionObj[KEY_ENGAGEMENT_OK] as? JsonPrimitive)?.booleanOrNull?.let { engagementOk = it }
        (sessionObj[KEY_CLIPPING_DETECTED] as? JsonPrimitive)?.booleanOrNull?.let { clippingDetected = it }
        (sessionObj[KEY_SNR_ADEQUATE] as? JsonPrimitive)?.booleanOrNull?.let { snrAdequate = it }
    }

    return ParsedSession(
        features = features,
        qualityFlags = qualityFlags,
        engagementOk = engagementOk,
        clippingDetected = clippingDetected,
        snrAdequate = snrAdequate
    )
}

private val testDisplayOrder: List<TestType> = listOf(
    TestType.TAP,
    TestType.GAIT,
    TestType.VISION,
    TestType.SDMT,
    TestType.VOICE
)

private fun primaryFeatureKey(testType: TestType): String = when (testType) {
    TestType.TAP -> "dominant_tap_rate_hz"
    TestType.GAIT -> "cadence_steps_per_minute"
    TestType.VISION -> "sloan_total"
    TestType.SDMT -> "sdmt_total_correct"
    TestType.VOICE -> "jitter_local"
}

private fun featureKeysForChart(testType: TestType): List<String> = when (testType) {
    TestType.TAP -> listOf(
        "dominant_tap_rate_hz",
        "non_dominant_tap_rate_hz",
        "dominant_iti_cv",
        "non_dominant_iti_cv",
        "asymmetry_index",
        "miss_rate",
        "dominant_off_target_taps",
        "non_dominant_off_target_taps"
    )
    TestType.GAIT -> listOf(
        "cadence_steps_per_minute",
        "mean_stride_length_meters",
        "step_time_cv",
        "stride_asymmetry_index",
        "double_support_time_seconds"
    )
    TestType.VISION -> listOf(
        "sloan_100pct",
        "sloan_2pt5pct",
        "sloan_1pt25pct",
        "sloan_total"
    )
    TestType.SDMT -> listOf(
        "sdmt_total_correct",
        "sdmt_response_time_mean_ms",
        "sdmt_response_time_sd_ms",
        "sdmt_error_rate"
    )
    TestType.VOICE -> listOf(
        "jitter_local",
        "shimmer_local",
        "hnr_db",
        "f0_sd_hz",
        "speaking_rate_wpm",
        "pause_fraction"
    )
}

internal data class FeatureCopyTokens(
    val displayNameResId: Int,
    val unitPhraseResId: Int,
    val unitSuffixResId: Int
)

private val featureCopyTokens: Map<String, FeatureCopyTokens> = mapOf(
    "dominant_tap_rate_hz" to FeatureCopyTokens(
        R.string.phase9_chart_axis_tap_dominant,
        R.string.phase9_unit_phrase_tap_dominant,
        R.string.phase9_unit_suffix_taps_per_second
    ),
    "non_dominant_tap_rate_hz" to FeatureCopyTokens(
        R.string.phase9_chart_axis_tap_non_dominant,
        R.string.phase9_unit_phrase_tap_non_dominant,
        R.string.phase9_unit_suffix_taps_per_second
    ),
    "dominant_iti_cv" to FeatureCopyTokens(
        R.string.phase9_chart_axis_tap_inter_tap_cv,
        R.string.phase9_unit_phrase_tap_inter_tap_cv,
        R.string.phase9_unit_suffix_taps_per_second
    ),
    "non_dominant_iti_cv" to FeatureCopyTokens(
        R.string.phase9_chart_axis_tap_inter_tap_cv,
        R.string.phase9_unit_phrase_tap_inter_tap_cv,
        R.string.phase9_unit_suffix_taps_per_second
    ),
    "asymmetry_index" to FeatureCopyTokens(
        R.string.phase9_chart_axis_tap_asymmetry,
        R.string.phase9_unit_phrase_tap_asymmetry,
        R.string.phase9_unit_suffix_taps_per_second
    ),
    "miss_rate" to FeatureCopyTokens(
        R.string.phase9_chart_axis_tap_miss_rate,
        R.string.phase9_unit_phrase_tap_miss_rate,
        R.string.phase9_unit_suffix_taps_per_second
    ),
    "dominant_off_target_taps" to FeatureCopyTokens(
        R.string.phase9_chart_axis_tap_off_target,
        R.string.phase9_unit_phrase_tap_off_target,
        R.string.phase9_unit_suffix_taps_per_second
    ),
    "non_dominant_off_target_taps" to FeatureCopyTokens(
        R.string.phase9_chart_axis_tap_off_target,
        R.string.phase9_unit_phrase_tap_off_target,
        R.string.phase9_unit_suffix_taps_per_second
    ),
    "cadence_steps_per_minute" to FeatureCopyTokens(
        R.string.phase9_chart_axis_gait_cadence,
        R.string.phase9_unit_phrase_gait_cadence,
        R.string.phase9_unit_suffix_steps_per_minute
    ),
    "mean_stride_length_meters" to FeatureCopyTokens(
        R.string.phase9_chart_axis_gait_stride_length,
        R.string.phase9_unit_phrase_gait_stride_length,
        R.string.phase9_unit_suffix_centimeters
    ),
    "step_time_cv" to FeatureCopyTokens(
        R.string.phase9_chart_axis_gait_step_time_variability,
        R.string.phase9_unit_phrase_gait_step_time_variability,
        R.string.phase9_unit_suffix_seconds
    ),
    "stride_asymmetry_index" to FeatureCopyTokens(
        R.string.phase9_chart_axis_gait_stride_asymmetry,
        R.string.phase9_unit_phrase_gait_stride_asymmetry,
        R.string.phase9_unit_suffix_seconds
    ),
    "double_support_time_seconds" to FeatureCopyTokens(
        R.string.phase9_chart_axis_gait_double_support,
        R.string.phase9_unit_phrase_gait_double_support,
        R.string.phase9_unit_suffix_seconds
    ),
    "sloan_100pct" to FeatureCopyTokens(
        R.string.phase9_chart_axis_vision_100pct,
        R.string.phase9_unit_phrase_vision_100pct,
        R.string.phase9_unit_suffix_seconds
    ),
    "sloan_2pt5pct" to FeatureCopyTokens(
        R.string.phase9_chart_axis_vision_2pt5pct,
        R.string.phase9_unit_phrase_vision_2pt5pct,
        R.string.phase9_unit_suffix_seconds
    ),
    "sloan_1pt25pct" to FeatureCopyTokens(
        R.string.phase9_chart_axis_vision_1pt25pct,
        R.string.phase9_unit_phrase_vision_1pt25pct,
        R.string.phase9_unit_suffix_seconds
    ),
    "sloan_total" to FeatureCopyTokens(
        R.string.phase9_chart_axis_vision_total,
        R.string.phase9_unit_phrase_vision_total,
        R.string.phase9_unit_suffix_seconds
    ),
    "sdmt_total_correct" to FeatureCopyTokens(
        R.string.phase9_chart_axis_sdmt_correct,
        R.string.phase9_unit_phrase_sdmt_correct,
        R.string.phase9_unit_suffix_seconds
    ),
    "sdmt_response_time_mean_ms" to FeatureCopyTokens(
        R.string.phase9_chart_axis_sdmt_rt_mean,
        R.string.phase9_unit_phrase_sdmt_rt_mean,
        R.string.phase9_unit_suffix_milliseconds
    ),
    "sdmt_response_time_sd_ms" to FeatureCopyTokens(
        R.string.phase9_chart_axis_sdmt_rt_sd,
        R.string.phase9_unit_phrase_sdmt_rt_sd,
        R.string.phase9_unit_suffix_milliseconds
    ),
    "sdmt_error_rate" to FeatureCopyTokens(
        R.string.phase9_chart_axis_sdmt_error_rate,
        R.string.phase9_unit_phrase_sdmt_error_rate,
        R.string.phase9_unit_suffix_seconds
    ),
    "jitter_local" to FeatureCopyTokens(
        R.string.phase9_chart_axis_voice_jitter,
        R.string.phase9_unit_phrase_voice_jitter,
        R.string.phase9_unit_suffix_seconds
    ),
    "shimmer_local" to FeatureCopyTokens(
        R.string.phase9_chart_axis_voice_shimmer,
        R.string.phase9_unit_phrase_voice_shimmer,
        R.string.phase9_unit_suffix_seconds
    ),
    "hnr_db" to FeatureCopyTokens(
        R.string.phase9_chart_axis_voice_hnr,
        R.string.phase9_unit_phrase_voice_hnr,
        R.string.phase9_unit_suffix_decibels
    ),
    "f0_sd_hz" to FeatureCopyTokens(
        R.string.phase9_chart_axis_voice_f0_sd,
        R.string.phase9_unit_phrase_voice_f0_sd,
        R.string.phase9_unit_suffix_hertz
    ),
    "speaking_rate_wpm" to FeatureCopyTokens(
        R.string.phase9_chart_axis_voice_speaking_rate,
        R.string.phase9_unit_phrase_voice_speaking_rate,
        R.string.phase9_unit_suffix_words_per_minute
    ),
    "pause_fraction" to FeatureCopyTokens(
        R.string.phase9_chart_axis_voice_pause_fraction,
        R.string.phase9_unit_phrase_voice_pause_fraction,
        R.string.phase9_unit_suffix_seconds
    )
)

internal fun copyTokensFor(featureKey: String): FeatureCopyTokens? = featureCopyTokens[featureKey]

class TrendsRepository(
    private val sessionDao: SessionDao,
    private val testResultDao: TestResultDao,
    private val locale: Locale = Locale.getDefault(),
    private val speakingRateOmissionMessage: String = "Speech window too short",
    private val steadinessSteadyLabel: String = "Steady",
    private val steadinessMostlySteadyLabel: String = "Mostly steady",
    private val steadinessVariedLabel: String = "Varied",
    private val steadinessUnmeasurableLabel: String = "Unmeasurable",
    private val tapDisplayName: String = "Bilateral Tap Test",
    private val gaitDisplayName: String = "Gait Test",
    private val visionDisplayName: String = "Vision Test",
    private val sdmtDisplayName: String = "Symbol Digit Modalities Test",
    private val voiceDisplayName: String = "Voice Reading Test"
) {

    private fun displayNameForTest(testType: TestType): String = when (testType) {
        TestType.TAP -> tapDisplayName
        TestType.GAIT -> gaitDisplayName
        TestType.VISION -> visionDisplayName
        TestType.SDMT -> sdmtDisplayName
        TestType.VOICE -> voiceDisplayName
    }

    fun observeReportsState(): Flow<ReportsScreenState> {
        return combine(
            sessionDao.observeCompletedSessionCount(),
            testResultDao.observeAll()
        ) { completedCount, allResults ->
            if (completedCount == 0 && allResults.isEmpty()) {
                ReportsScreenState.Empty
            } else {
                val cards = testDisplayOrder.map { testType ->
                    val resultsForType = allResults.filter { it.testType == testType }
                    buildSummaryCard(testType, resultsForType)
                }
                ReportsScreenState.Ready(testSummaries = cards)
            }
        }
    }

    fun observeTestDetailState(testType: TestType): Flow<TestDetailScreenState> {
        return testResultDao.observeByType(testType).map { results ->
            if (results.isEmpty()) {
                TestDetailScreenState.Empty
            } else {
                buildDetailReady(testType, results)
            }
        }
    }

    private fun buildSummaryCard(
        testType: TestType,
        resultsForType: List<TestResultEntity>
    ): TestSummaryCard {
        if (resultsForType.isEmpty()) {
            return TestSummaryCard(
                testType = testType,
                displayName = displayNameForTest(testType),
                sessionCount = 0,
                latestQualityBand = null,
                primaryFeatureSparkline = emptyList()
            )
        }
        val sorted = resultsForType.sortedBy { it.startedAtEpochMs }
        val primaryKey = primaryFeatureKey(testType)
        val parsed = sorted.map { parseFeaturesJson(it.featuresJson) }
        val sparkline = sorted.mapIndexedNotNull { index, _ ->
            parsed[index].features[primaryKey]
        }.takeLast(SPARKLINE_MAX_POINTS)
        val latest = sorted.last()
        return TestSummaryCard(
            testType = testType,
            displayName = displayNameForTest(testType),
            sessionCount = sorted.size,
            latestQualityBand = deriveQualityBand(latest.qualityScore),
            primaryFeatureSparkline = sparkline
        )
    }

    private fun buildDetailReady(
        testType: TestType,
        results: List<TestResultEntity>
    ): TestDetailScreenState.Ready {
        val sorted = results.sortedBy { it.startedAtEpochMs }
        val parsed = sorted.map { parseFeaturesJson(it.featuresJson) }
        val featureKeys = featureKeysForChart(testType)
        val featureSeries = featureKeys.map { key ->
            val points = sorted.mapIndexed { index, entity ->
                projectPoint(testType, key, entity, parsed[index])
            }
            val tokens = copyTokensFor(key)
            FeatureSeries(
                key = key,
                displayName = "",
                unit = "",
                points = points,
                unitPhraseResId = tokens?.unitPhraseResId,
                unitSuffixResId = tokens?.unitSuffixResId
            )
        }
        val summaryRows = sorted.mapIndexed { index, entity ->
            buildSummaryRow(testType, entity, parsed[index])
        }
        val sessionsSinceLabel = sorted.firstOrNull()?.let { formatDateLabel(it.startedAtEpochMs) }
        return TestDetailScreenState.Ready(
            testType = testType,
            sessionCount = sorted.size,
            sessionsSinceLabel = sessionsSinceLabel,
            latestQualityBand = deriveQualityBand(sorted.last().qualityScore),
            featureSeries = featureSeries,
            summaryRows = summaryRows
        )
    }

    private fun projectPoint(
        testType: TestType,
        featureKey: String,
        entity: TestResultEntity,
        parsed: ParsedSession
    ): TimedPoint {
        val rawValue = parsed.features[featureKey]
        val band = deriveQualityBand(entity.qualityScore)
        if (rawValue == null) {
            return TimedPoint(
                epochMs = entity.startedAtEpochMs,
                value = Double.NaN,
                qualityBand = band,
                omittedFromChart = true,
                omissionReason = null
            )
        }
        if (testType == TestType.VOICE && featureKey == KEY_SPEAKING_RATE_WPM) {
            val voicedSeconds = parsed.features[KEY_VOICED_SECONDS]
            val omitForRate = rawValue > SPEAKING_RATE_OMISSION_WPM_CEILING
            val omitForVoiced = voicedSeconds != null &&
                voicedSeconds < SPEAKING_RATE_OMISSION_VOICED_SECONDS_FLOOR
            if (omitForRate || omitForVoiced) {
                return TimedPoint(
                    epochMs = entity.startedAtEpochMs,
                    value = rawValue,
                    qualityBand = band,
                    omittedFromChart = true,
                    omissionReason = speakingRateOmissionMessage
                )
            }
        }
        return TimedPoint(
            epochMs = entity.startedAtEpochMs,
            value = rawValue,
            qualityBand = band,
            omittedFromChart = false,
            omissionReason = null
        )
    }

    private fun buildSummaryRow(
        testType: TestType,
        entity: TestResultEntity,
        parsed: ParsedSession
    ): SummaryRow {
        val band = deriveQualityBand(entity.qualityScore)
        val featureKeys = featureKeysForChart(testType)
        val perFeatureValues = mutableMapOf<String, String>()
        val perFeatureAnnotations = mutableMapOf<String, String>()
        val perFeatureAnnotationResIds = mutableMapOf<String, Int>()
        for (key in featureKeys) {
            val value = parsed.features[key]
            if (value == null) {
                perFeatureValues[key] = "—"
            } else if (testType == TestType.VOICE && key == KEY_SPEAKING_RATE_WPM) {
                val voicedSeconds = parsed.features[KEY_VOICED_SECONDS]
                val omitForRate = value > SPEAKING_RATE_OMISSION_WPM_CEILING
                val omitForVoiced = voicedSeconds != null &&
                    voicedSeconds < SPEAKING_RATE_OMISSION_VOICED_SECONDS_FLOOR
                if (omitForRate || omitForVoiced) {
                    perFeatureValues[key] = "—"
                    perFeatureAnnotations[key] = speakingRateOmissionMessage
                } else {
                    perFeatureValues[key] = formatFeatureValue(value)
                }
            } else {
                perFeatureValues[key] = formatFeatureValue(value)
            }
            if (testType == TestType.VOICE) {
                val annotationResId = voicePerFeatureFlagAnnotationResIds[key]
                val flag = parsed.qualityFlags[key]
                if (annotationResId != null && flag == false) {
                    perFeatureAnnotationResIds[key] = annotationResId
                }
            }
        }
        val sessionAnnotationResIds = if (testType == TestType.VOICE) {
            buildVoiceSessionAnnotationResIds(parsed)
        } else {
            emptyList()
        }
        val steadinessBandLabel = if (testType == TestType.VOICE) {
            steadinessBandLabelString(parsed)
        } else {
            null
        }
        return SummaryRow(
            epochMs = entity.startedAtEpochMs,
            dateLabel = formatDateLabel(entity.startedAtEpochMs),
            perFeatureValues = perFeatureValues,
            qualityBand = band,
            contextCellResId = sessionContextCellResId(testType, band, entity.qualityScore, parsed),
            steadinessBandLabel = steadinessBandLabel,
            perFeatureAnnotations = perFeatureAnnotations,
            perFeatureAnnotationResIds = perFeatureAnnotationResIds,
            sessionAnnotationResIds = sessionAnnotationResIds
        )
    }

    private fun buildVoiceSessionAnnotationResIds(parsed: ParsedSession): List<Int> {
        val result = mutableListOf<Int>()
        if (parsed.clippingDetected) result += R.string.phase9_voice_directive_clipping
        if (!parsed.snrAdequate) result += R.string.phase9_voice_directive_noisy
        if (!parsed.engagementOk) result += R.string.phase9_voice_directive_short
        return result
    }

    private fun steadinessBandLabelString(parsed: ParsedSession): String {
        val jitter = parsed.features[KEY_JITTER_LOCAL]
        val shimmer = parsed.features[KEY_SHIMMER_LOCAL]
        return when (deriveSteadinessBand(jitter, shimmer)) {
            SteadinessBandLabel.STEADY -> steadinessSteadyLabel
            SteadinessBandLabel.MOSTLY_STEADY -> steadinessMostlySteadyLabel
            SteadinessBandLabel.VARIED -> steadinessVariedLabel
            SteadinessBandLabel.UNMEASURABLE -> steadinessUnmeasurableLabel
        }
    }

    private fun formatFeatureValue(value: Double): String {
        if (value.isNaN()) return "—"
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format(locale, "%.2f", value)
        }
    }

    private fun formatDateLabel(epochMs: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", locale)
        return fmt.format(Date(epochMs))
    }

    companion object {
        private const val SPARKLINE_MAX_POINTS: Int = 6
    }
}

internal fun sessionContextCellResId(
    testType: TestType,
    band: QualityBand,
    qualityScore: Double,
    parsed: ParsedSession
): Int? {
    if (band != QualityBand.LOW) return null
    val lowScore = qualityScore < QUALITY_BAND_MEDIUM_THRESHOLD
    return when (testType) {
        TestType.VOICE -> when {
            !parsed.engagementOk && lowScore -> R.string.phase9_context_cell_voice_short
            (!parsed.snrAdequate || parsed.clippingDetected) && lowScore ->
                R.string.phase9_context_cell_voice_noisy
            else -> R.string.phase9_context_cell_default_low
        }
        TestType.TAP -> {
            val dominantValid = parsed.features[KEY_DOMINANT_VALID_TAPS] ?: Double.MAX_VALUE
            val nonDominantValid = parsed.features[KEY_NON_DOMINANT_VALID_TAPS] ?: Double.MAX_VALUE
            if (lowScore && (dominantValid < TAP_VALID_FLOOR || nonDominantValid < TAP_VALID_FLOOR)) {
                R.string.phase9_context_cell_tap_short
            } else {
                R.string.phase9_context_cell_default_low
            }
        }
        TestType.GAIT -> {
            val detected = parsed.features[KEY_DETECTED_STEP_COUNT] ?: Double.MAX_VALUE
            if (lowScore && detected < GAIT_STEP_FLOOR) {
                R.string.phase9_context_cell_gait_short
            } else {
                R.string.phase9_context_cell_default_low
            }
        }
        TestType.VISION, TestType.SDMT -> R.string.phase9_context_cell_default_low
    }
}

private const val TAP_VALID_FLOOR: Double = 10.0
private const val GAIT_STEP_FLOOR: Double = 20.0
