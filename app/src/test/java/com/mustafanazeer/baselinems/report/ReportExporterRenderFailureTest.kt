package com.mustafanazeer.baselinems.report

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mustafanazeer.baselinems.data.PdfReportSnapshot
import com.mustafanazeer.baselinems.report.pdf.ReportPdfRenderer
import com.mustafanazeer.baselinems.report.pdf.ShadowPdfDocumentForTests
import java.io.File
import java.io.IOException
import java.io.OutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [ShadowPdfDocumentForTests::class])
class ReportExporterRenderFailureTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun export_pdfRenderThrows_leavesNoTempFileBehind() = runBlocking {
        val exportsDir = File(context.cacheDir, "exports").apply {
            mkdirs()
            listFiles()?.forEach { it.delete() }
        }
        val source = StubSource(PdfReportSnapshot(
            generatedAtEpochMs = 1_700_000_000_000L,
            totalCompletedSessions = 0,
            earliestSessionEpochMs = null,
            latestSessionEpochMs = null,
            perTestSections = emptyList()
        ))
        val exporter = ReportExporter(
            context = context,
            source = source,
            pdfRenderer = ThrowingRenderer(context)
        )

        var caught: Throwable? = null
        try {
            exporter.export()
            fail("Expected the throwing renderer to surface its exception")
        } catch (t: Throwable) {
            caught = t
        }

        assertNotNull(caught)
        val leftoverTmpFiles = exportsDir.listFiles { f -> f.name.endsWith(".tmp") } ?: emptyArray()
        assertEquals(
            "no .tmp file should remain after a render failure, found: ${leftoverTmpFiles.joinToString { it.name }}",
            0,
            leftoverTmpFiles.size
        )
        val canonicalPdf = File(exportsDir, "BaselineMS_report_2023-11-14.pdf")
        assertFalse("no canonical pdf should land on render failure", canonicalPdf.exists())
    }

    private class ThrowingRenderer(context: Context) : ReportPdfRenderer(context) {
        override fun render(snapshot: PdfReportSnapshot, output: OutputStream) {
            throw IOException("simulated render failure")
        }
    }

    private class StubSource(val snapshot: PdfReportSnapshot) : PdfReportSnapshotProvider {
        override suspend fun snapshot(): PdfReportSnapshot = snapshot
    }
}
