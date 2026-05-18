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
        assertEquals(1, parsePageCount(bytes))
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
        assertEquals(2, parsePageCount(out.toByteArray()))
    }

    @Test
    fun render_sectionWithMultipleSummaryRows_drawsMoreContentThanZeroRowVariant() {
        val series = FeatureSeries(
            key = "dominant_tap_rate_hz",
            displayName = "",
            unit = "",
            points = (0..4).map {
                TimedPoint(
                    epochMs = 1_700_000_000_000L + it * 86_400_000L,
                    value = 5.0 + it * 0.1,
                    qualityBand = QualityBand.HIGH
                )
            }
        )
        val summaryRows = (0..4).map { idx ->
            SummaryRow(
                epochMs = 1_700_000_000_000L + idx * 86_400_000L,
                dateLabel = "2023-11-${15 + idx}",
                perFeatureValues = mapOf("dominant_tap_rate_hz" to "%.2f".format(5.0 + idx * 0.1)),
                qualityBand = if (idx == 4) QualityBand.MEDIUM else QualityBand.HIGH,
                contextCellResId = null
            )
        }
        val baseSection = PdfTestSection(
            testType = TestType.TAP,
            displayName = "Bilateral Tap Test",
            sessionCount = 5,
            latestQualityBand = QualityBand.MEDIUM,
            featureSeries = listOf(series),
            summaryRows = summaryRows
        )
        val zeroRowsSection = baseSection.copy(summaryRows = emptyList())
        val snapshot = PdfReportSnapshot(
            generatedAtEpochMs = 1_700_500_000_000L,
            totalCompletedSessions = 5,
            earliestSessionEpochMs = 1_700_000_000_000L,
            latestSessionEpochMs = 1_700_500_000_000L,
            perTestSections = listOf(baseSection)
        )
        val zeroSnapshot = snapshot.copy(perTestSections = listOf(zeroRowsSection))
        val populatedOut = ByteArrayOutputStream()
        val emptyOut = ByteArrayOutputStream()
        ReportPdfRenderer(context).render(snapshot, populatedOut)
        ReportPdfRenderer(context).render(zeroSnapshot, emptyOut)
        val populatedPageBytes = parsePageByteCount(populatedOut.toByteArray(), pageIndex = 2)
        val emptyPageBytes = parsePageByteCount(emptyOut.toByteArray(), pageIndex = 2)
        assertTrue(
            "Test page must record more draw bytes when summary rows are present " +
                "(populated=$populatedPageBytes, empty=$emptyPageBytes)",
            populatedPageBytes > emptyPageBytes
        )
    }

    @Test
    fun render_sectionWithSixFeatureSeries_growsBeyondOneTestPage() {
        val series = (0 until 6).map { idx ->
            FeatureSeries(
                key = "voice_feature_$idx",
                displayName = "",
                unit = "",
                points = listOf(
                    TimedPoint(1_700_000_000_000L + idx * 1000L, 1.0 + idx, QualityBand.HIGH)
                )
            )
        }
        val singleSeries = listOf(series[0])
        val singleSection = PdfTestSection(
            testType = TestType.VOICE,
            displayName = "Voice Reading Test",
            sessionCount = 1,
            latestQualityBand = QualityBand.HIGH,
            featureSeries = singleSeries,
            summaryRows = listOf(
                SummaryRow(
                    epochMs = 1_700_000_000_000L,
                    dateLabel = "2023-11-15",
                    perFeatureValues = mapOf("voice_feature_0" to "1.00"),
                    qualityBand = QualityBand.HIGH,
                    steadinessBandLabel = "Steady"
                )
            )
        )
        val sixSection = singleSection.copy(featureSeries = series)
        val baseSnapshot = PdfReportSnapshot(
            generatedAtEpochMs = 1_700_000_000_000L,
            totalCompletedSessions = 1,
            earliestSessionEpochMs = 1_700_000_000_000L,
            latestSessionEpochMs = 1_700_000_000_000L,
            perTestSections = listOf(singleSection)
        )
        val sixSnapshot = baseSnapshot.copy(perTestSections = listOf(sixSection))
        val baseOut = ByteArrayOutputStream()
        val sixOut = ByteArrayOutputStream()
        ReportPdfRenderer(context).render(baseSnapshot, baseOut)
        ReportPdfRenderer(context).render(sixSnapshot, sixOut)
        val basePages = parsePageCount(baseOut.toByteArray())
        val sixPages = parsePageCount(sixOut.toByteArray())
        assertEquals(2, basePages)
        assertTrue(
            "Six feature series should require more pages than a single series; got base=$basePages six=$sixPages",
            sixPages > basePages
        )
    }

    @Test
    fun render_voiceSectionWithoutSteadinessLabel_doesNotCrash() {
        val series = FeatureSeries(
            key = "jitter_local",
            displayName = "",
            unit = "",
            points = listOf(TimedPoint(1_700_000_000_000L, 0.015, QualityBand.MEDIUM))
        )
        val snapshot = PdfReportSnapshot(
            generatedAtEpochMs = 1_700_000_000_000L,
            totalCompletedSessions = 1,
            earliestSessionEpochMs = 1_700_000_000_000L,
            latestSessionEpochMs = 1_700_000_000_000L,
            perTestSections = listOf(
                PdfTestSection(
                    testType = TestType.VOICE,
                    displayName = "Voice Reading Test",
                    sessionCount = 1,
                    latestQualityBand = QualityBand.MEDIUM,
                    featureSeries = listOf(series),
                    summaryRows = listOf(
                        SummaryRow(
                            epochMs = 1_700_000_000_000L,
                            dateLabel = "2023-11-15",
                            perFeatureValues = mapOf("jitter_local" to "0.015"),
                            qualityBand = QualityBand.MEDIUM,
                            steadinessBandLabel = null
                        )
                    )
                )
            )
        )
        val out = ByteArrayOutputStream()
        ReportPdfRenderer(context).render(snapshot, out)
        assertTrue(out.size() > 0)
    }

    private fun parsePageCount(bytes: ByteArray): Int {
        val text = String(bytes, Charsets.US_ASCII)
        val match = Regex("% pages=(\\d+)").find(text)
            ?: throw AssertionError("page count marker not present in PDF body")
        return match.groupValues[1].toInt()
    }

    private fun parsePageByteCount(bytes: ByteArray, pageIndex: Int): Int {
        val text = String(bytes, Charsets.US_ASCII)
        val match = Regex("% page${pageIndex}_text_draws=(\\d+)").find(text)
            ?: throw AssertionError("page text draw marker for page $pageIndex not present")
        return match.groupValues[1].toInt()
    }
}
