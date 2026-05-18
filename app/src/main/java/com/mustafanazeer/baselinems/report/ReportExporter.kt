package com.mustafanazeer.baselinems.report

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.mustafanazeer.baselinems.data.PdfReportSnapshot
import com.mustafanazeer.baselinems.report.csv.CsvExporter
import com.mustafanazeer.baselinems.report.pdf.ReportPdfRenderer
import java.io.File
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicLong
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
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        cleanupStaleTempFiles(dir)

        val snap = source.snapshot()
        val pdfFile = File(dir, ExportFileNaming.pdfFor(snap.generatedAtEpochMs, tz = TimeZone.getDefault()))
        val csvFile = File(dir, ExportFileNaming.csvFor(snap.generatedAtEpochMs, tz = TimeZone.getDefault()))

        val pdfTmp = File(dir, pdfFile.name + ".tmp")
        try {
            FileOutputStream(pdfTmp).use { pdfRenderer.render(snap, it) }
            if (pdfFile.exists()) pdfFile.delete()
            atomicMove(pdfTmp, pdfFile)
        } finally {
            if (pdfTmp.exists()) pdfTmp.delete()
        }

        val csvTmp = File(dir, csvFile.name + ".tmp")
        try {
            csvTmp.writeText(csvExporter.toCsv(snap))
            if (csvFile.exists()) csvFile.delete()
            atomicMove(csvTmp, csvFile)
        } finally {
            if (csvTmp.exists()) csvTmp.delete()
        }

        val authority = "${context.packageName}.fileprovider"
        ExportResult(
            pdfUri = FileProvider.getUriForFile(context, authority, pdfFile),
            csvUri = FileProvider.getUriForFile(context, authority, csvFile)
        )
    }

    private fun cleanupStaleTempFiles(exportsDir: File) {
        val now = System.currentTimeMillis()
        exportsDir.listFiles { f ->
            f.name.endsWith(".tmp") && now - f.lastModified() > STALE_TMP_AGE_MS
        }?.forEach { it.delete() }
    }

    private fun atomicMove(source: File, target: File) {
        try {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (e: AtomicMoveNotSupportedException) {
            atomicMoveFallbackCount.incrementAndGet()
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    companion object {
        private const val STALE_TMP_AGE_MS = 60_000L

        private val atomicMoveFallbackCount: AtomicLong = AtomicLong(0L)

        fun atomicMoveFallbackCount(): Long = atomicMoveFallbackCount.get()
    }
}
