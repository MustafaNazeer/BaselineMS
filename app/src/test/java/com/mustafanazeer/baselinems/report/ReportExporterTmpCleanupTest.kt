package com.mustafanazeer.baselinems.report

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mustafanazeer.baselinems.data.PdfReportSnapshot
import com.mustafanazeer.baselinems.report.pdf.ShadowPdfDocumentForTests
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [ShadowPdfDocumentForTests::class])
class ReportExporterTmpCleanupTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun export_deletesTmpFilesOlderThanSixtySeconds_keepsFresherOnes() = runBlocking {
        val exportsDir = File(context.cacheDir, "exports").apply {
            mkdirs()
            listFiles()?.forEach { it.delete() }
        }

        val now = System.currentTimeMillis()
        val staleTmp = File(exportsDir, "BaselineMS_report_2020-01-01.pdf.tmp").apply {
            writeText("stranded from a prior crashed export")
            setLastModified(now - 120_000L)
        }
        val freshTmp = File(exportsDir, "BaselineMS_report_2020-01-02.pdf.tmp").apply {
            writeText("recent export still in flight")
            setLastModified(now - 30_000L)
        }

        val source = StubSource(PdfReportSnapshot(
            generatedAtEpochMs = 1_700_000_000_000L,
            totalCompletedSessions = 0,
            earliestSessionEpochMs = null,
            latestSessionEpochMs = null,
            perTestSections = emptyList()
        ))
        val exporter = ReportExporter(context, source)
        try {
            exporter.export()
        } catch (e: IllegalArgumentException) {
            // The FileProvider.getUriForFile call at the tail of export() can throw under
            // Robolectric when a sibling test class has cached a different application data
            // root on FileProvider's static path strategy cache. The cleanup happens at the
            // top of export() before any FileProvider interaction so the assertions below
            // are valid regardless of which exit path export() takes.
        }

        assertFalse(
            "stale .tmp (120 seconds old) should be deleted at export start",
            staleTmp.exists()
        )
        assertTrue(
            "fresh .tmp (30 seconds old) must be preserved at export start",
            freshTmp.exists()
        )
    }

    private class StubSource(val snapshot: PdfReportSnapshot) : PdfReportSnapshotProvider {
        override suspend fun snapshot(): PdfReportSnapshot = snapshot
    }
}
