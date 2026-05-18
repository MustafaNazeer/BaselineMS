package com.mustafanazeer.baselinems.ui.reports

import com.mustafanazeer.baselinems.data.TestType

data class TimedPoint(
    val epochMs: Long,
    val value: Double,
    val qualityBand: QualityBand,
    val omittedFromChart: Boolean = false,
    val omissionReason: String? = null
)

data class ReferenceRange(
    val mean: Double,
    val sd: Double,
    val captionResId: Int,
    val unitsLabel: String
)

data class FeatureSeries(
    val key: String,
    val displayName: String,
    val unit: String,
    val points: List<TimedPoint>,
    val referenceRange: ReferenceRange? = null,
    val guardRailAnnotation: String? = null,
    val unitPhraseResId: Int? = null,
    val unitSuffixResId: Int? = null,
    val valueFormatter: ((Double) -> String)? = null
)

data class SummaryRow(
    val epochMs: Long,
    val dateLabel: String,
    val perFeatureValues: Map<String, String>,
    val qualityBand: QualityBand,
    val contextCellResId: Int? = null,
    val steadinessBandLabel: String? = null,
    val perFeatureAnnotations: Map<String, String> = emptyMap(),
    val perFeatureAnnotationResIds: Map<String, Int> = emptyMap(),
    val sessionAnnotationResIds: List<Int> = emptyList()
)

sealed interface TestDetailScreenState {
    data object Loading : TestDetailScreenState
    data object Empty : TestDetailScreenState
    data class Ready(
        val testType: TestType,
        val sessionCount: Int,
        val sessionsSinceLabel: String?,
        val latestQualityBand: QualityBand?,
        val featureSeries: List<FeatureSeries>,
        val summaryRows: List<SummaryRow>
    ) : TestDetailScreenState
}
