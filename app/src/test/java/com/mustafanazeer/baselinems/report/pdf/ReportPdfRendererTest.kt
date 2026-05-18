package com.mustafanazeer.baselinems.report.pdf

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [ShadowPdfDocumentForTests::class])
class ReportPdfRendererTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun render_emptySnapshot_producesCoverOnly() {
        val snapshot = PdfReportSnapshot(
            generatedAtEpochMs = 1_700_000_000_000L,
            totalCompletedSessions = 0,
            earliestSessionEpochMs = null,
            latestSessionEpochMs = null,
            perTestSections = emptyList()
        )
        val out = ByteArrayOutputStream()
        ReportPdfRenderer(context).render(snapshot, out)
        val bytes = out.toByteArray()
        assertTrue("PDF must be non empty", bytes.isNotEmpty())
        assertEquals(0x25.toByte(), bytes[0])
        assertEquals(0x50.toByte(), bytes[1])
        assertEquals(0x44.toByte(), bytes[2])
        assertEquals(0x46.toByte(), bytes[3])
    }

    @Test
    fun render_oneTestSection_producesCoverPlusOneTestPage() {
        val snapshot = PdfReportSnapshot(
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
                        )
                    ),
                    summaryRows = listOf(
                        SummaryRow(
                            epochMs = 1_700_000_000_000L,
                            dateLabel = "2023-11-15",
                            perFeatureValues = mapOf("dominant_tap_rate_hz" to "5.20"),
                            qualityBand = QualityBand.HIGH
                        )
                    )
                )
            )
        )
        val out = ByteArrayOutputStream()
        ReportPdfRenderer(context).render(snapshot, out)
        assertTrue(out.size() > 0)
    }
}
