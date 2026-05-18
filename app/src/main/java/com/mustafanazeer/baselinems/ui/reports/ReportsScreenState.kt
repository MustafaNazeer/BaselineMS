package com.mustafanazeer.baselinems.ui.reports

import com.mustafanazeer.baselinems.data.TestType

enum class QualityBand { HIGH, MEDIUM, LOW }

data class TestSummaryCard(
    val testType: TestType,
    val displayName: String,
    val sessionCount: Int,
    val latestQualityBand: QualityBand?,
    val primaryFeatureSparkline: List<Double>
)

sealed interface ReportsScreenState {
    data object Loading : ReportsScreenState
    data object Empty : ReportsScreenState
    data class Ready(val testSummaries: List<TestSummaryCard>) : ReportsScreenState
}
