package com.mustafanazeer.baselinems.report

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.mustafanazeer.baselinems.data.PdfReportSnapshot
import com.mustafanazeer.baselinems.report.csv.CsvExporter
import com.mustafanazeer.baselinems.report.pdf.ReportPdfRenderer
import java.io.File
import java.io.FileOutputStream
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ExportResult(val pdfUri: Uri, val csvUri: Uri)

interface PdfReportSnapshotProvider {
    suspend fun snapshot(): PdfReportSnapshot
}

class ReportExporter(
    private val context: Context,
    private val source: PdfReportSnapshotProvider,
    private val pdfRenderer: ReportPdfRenderer = ReportPdfRenderer(context),
    private val csvExporter: CsvExporter = CsvExporter()
) {

    suspend fun export(): ExportResult = withContext(Dispatchers.IO) {
        val snap = source.snapshot()
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val pdfFile = File(dir, ExportFileNaming.pdfFor(snap.generatedAtEpochMs, tz = TimeZone.getDefault()))
        val csvFile = File(dir, ExportFileNaming.csvFor(snap.generatedAtEpochMs, tz = TimeZone.getDefault()))

        val pdfTmp = File(dir, pdfFile.name + ".tmp")
        FileOutputStream(pdfTmp).use { pdfRenderer.render(snap, it) }
        if (pdfFile.exists()) pdfFile.delete()
        pdfTmp.renameTo(pdfFile)

        val csvTmp = File(dir, csvFile.name + ".tmp")
        csvTmp.writeText(csvExporter.toCsv(snap, sessionIdByResultId = emptyMap()))
        if (csvFile.exists()) csvFile.delete()
        csvTmp.renameTo(csvFile)

        val authority = "${context.packageName}.fileprovider"
        ExportResult(
            pdfUri = FileProvider.getUriForFile(context, authority, pdfFile),
            csvUri = FileProvider.getUriForFile(context, authority, csvFile)
        )
    }
}
