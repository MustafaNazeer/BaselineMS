package com.mustafanazeer.baselinems.report

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mustafanazeer.baselinems.data.PdfReportSnapshot
import com.mustafanazeer.baselinems.report.pdf.ShadowPdfDocumentForTests
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [ShadowPdfDocumentForTests::class])
class ReportExporterTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun export_emptySnapshot_writesBothFilesIntoCacheExports() = runBlocking {
        val source = StubSource(PdfReportSnapshot(
            generatedAtEpochMs = 1_700_000_000_000L,
            totalCompletedSessions = 0,
            earliestSessionEpochMs = null,
            latestSessionEpochMs = null,
            perTestSections = emptyList()
        ))
        val exporter = ReportExporter(context, source)
        val result = exporter.export()
        val exportsDir = File(context.cacheDir, "exports")
        assertTrue(exportsDir.exists())
        assertTrue(File(exportsDir, "BaselineMS_report_2023-11-14.pdf").exists())
        assertTrue(File(exportsDir, "BaselineMS_report_2023-11-14.csv").exists())
        assertTrue(result.pdfUri.toString().contains("fileprovider"))
        assertTrue(result.csvUri.toString().contains("fileprovider"))
    }

    private class StubSource(val snapshot: PdfReportSnapshot) : PdfReportSnapshotProvider {
        override suspend fun snapshot(): PdfReportSnapshot = snapshot
    }
}
