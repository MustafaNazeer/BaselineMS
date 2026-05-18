package com.mustafanazeer.baselinems.data

import com.mustafanazeer.baselinems.ui.reports.FeatureSeries
import com.mustafanazeer.baselinems.ui.reports.QualityBand
import com.mustafanazeer.baselinems.ui.reports.SummaryRow

data class PdfReportSnapshot(
    val generatedAtEpochMs: Long,
    val totalCompletedSessions: Int,
    val earliestSessionEpochMs: Long?,
    val latestSessionEpochMs: Long?,
    val perTestSections: List<PdfTestSection>
)

data class PdfTestSection(
    val testType: TestType,
    val displayName: String,
    val sessionCount: Int,
    val latestQualityBand: QualityBand?,
    val featureSeries: List<FeatureSeries>,
    val summaryRows: List<SummaryRow>
)
