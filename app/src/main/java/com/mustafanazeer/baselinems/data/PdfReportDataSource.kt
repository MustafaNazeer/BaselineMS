package com.mustafanazeer.baselinems.data

import com.mustafanazeer.baselinems.report.PdfReportSnapshotProvider
import com.mustafanazeer.baselinems.ui.reports.ReportsScreenState
import com.mustafanazeer.baselinems.ui.reports.TestDetailScreenState
import kotlinx.coroutines.flow.first

class PdfReportDataSource(
    private val sessionDao: SessionDao,
    private val testResultDao: TestResultDao,
    private val trendsRepository: TrendsRepository,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) : PdfReportSnapshotProvider {

    override suspend fun snapshot(): PdfReportSnapshot {
        val completedCount = sessionDao.observeCompletedSessionCount().first()
        val allResults = testResultDao.observeAll().first()
        val reportsState = trendsRepository.observeReportsState().first()

        if (reportsState is ReportsScreenState.Empty || completedCount == 0) {
            return PdfReportSnapshot(
                generatedAtEpochMs = nowProvider(),
                totalCompletedSessions = 0,
                earliestSessionEpochMs = null,
                latestSessionEpochMs = null,
                perTestSections = emptyList()
            )
        }

        val ready = reportsState as ReportsScreenState.Ready
        val sectionsWithData = ready.testSummaries
            .filter { it.sessionCount > 0 }
            .map { card ->
                val detail = trendsRepository.observeTestDetailState(card.testType).first()
                val detailReady = detail as TestDetailScreenState.Ready
                PdfTestSection(
                    testType = card.testType,
                    displayName = card.displayName,
                    sessionCount = card.sessionCount,
                    latestQualityBand = card.latestQualityBand,
                    featureSeries = detailReady.featureSeries,
                    summaryRows = detailReady.summaryRows
                )
            }

        val sortedResults = allResults.sortedBy { it.startedAtEpochMs }
        return PdfReportSnapshot(
            generatedAtEpochMs = nowProvider(),
            totalCompletedSessions = completedCount,
            earliestSessionEpochMs = sortedResults.firstOrNull()?.startedAtEpochMs,
            latestSessionEpochMs = sortedResults.lastOrNull()?.startedAtEpochMs,
            perTestSections = sectionsWithData
        )
    }
}
