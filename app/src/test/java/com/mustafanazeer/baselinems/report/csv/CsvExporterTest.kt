package com.mustafanazeer.baselinems.report.csv

import com.mustafanazeer.baselinems.data.PdfReportSnapshot
import com.mustafanazeer.baselinems.data.PdfTestSection
import com.mustafanazeer.baselinems.data.TestType
import com.mustafanazeer.baselinems.ui.reports.FeatureSeries
import com.mustafanazeer.baselinems.ui.reports.QualityBand
import com.mustafanazeer.baselinems.ui.reports.SummaryRow
import com.mustafanazeer.baselinems.ui.reports.TimedPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

    @Test
    fun export_emptySnapshot_emitsHeaderOnly() {
        val snap = PdfReportSnapshot(
            generatedAtEpochMs = 0L,
            totalCompletedSessions = 0,
            earliestSessionEpochMs = null,
            latestSessionEpochMs = null,
            perTestSections = emptyList()
        )
        val csv = CsvExporter().toCsv(snap, sessionIdByResultId = emptyMap())
        val lines = csv.lines().filter { it.isNotEmpty() }
        assertEquals(1, lines.size)
        assertTrue(lines.first().startsWith("session_id,session_started_at_iso,"))
    }

    @Test
    fun export_oneSection_emitsHeaderPlusOneRowPerFeatureValue() {
        val snap = PdfReportSnapshot(
            generatedAtEpochMs = 1_700_000_000_000L,
            totalCompletedSessions = 1,
            earliestSessionEpochMs = 1_700_000_000_000L,
            latestSessionEpochMs = 1_700_000_000_000L,
            perTestSections = listOf(
                PdfTestSection(
                    testType = TestType.TAP,
                    displayName = "Bilateral Tap Test",
                    sessionCount = 1,
                    latestQualityBand = QualityBand.HIGH,
                    featureSeries = listOf(
                        FeatureSeries(
                            key = "dominant_tap_rate_hz",
                            displayName = "",
                            unit = "",
                            points = listOf(TimedPoint(1_700_000_000_000L, 5.2, QualityBand.HIGH))
                        ),
                        FeatureSeries(
                            key = "miss_rate",
                            displayName = "",
                            unit = "",
                            points = listOf(TimedPoint(1_700_000_000_000L, 0.02, QualityBand.HIGH))
                        )
                    ),
                    summaryRows = emptyList()
                )
            )
        )
        val csv = CsvExporter().toCsv(
            snap,
            sessionIdByResultId = mapOf("derived" to "sess-1")
        )
        val lines = csv.lines().filter { it.isNotEmpty() }
        assertEquals(3, lines.size)
    }
}
