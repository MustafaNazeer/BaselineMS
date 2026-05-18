package com.mustafanazeer.baselinems.report.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.mustafanazeer.baselinems.R
import com.mustafanazeer.baselinems.data.PdfReportSnapshot
import com.mustafanazeer.baselinems.data.PdfTestSection
import com.mustafanazeer.baselinems.data.TestType
import com.mustafanazeer.baselinems.dsp.voice.VoiceFeatureSet
import com.mustafanazeer.baselinems.dsp.voice.VoiceFeatures
import com.mustafanazeer.baselinems.dsp.voice.VoiceSteadinessBand
import com.mustafanazeer.baselinems.ui.reports.QualityBand
import com.mustafanazeer.baselinems.ui.reports.SummaryRow
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportPdfRenderer(
    private val context: Context,
    private val locale: Locale = Locale.getDefault(),
    private val chartRenderer: PdfChartRenderer = PdfChartRenderer()
) {

    fun render(snapshot: PdfReportSnapshot, output: OutputStream) {
        val pdfDoc = PdfDocument()
        try {
            val pageNumbers = mutableMapOf<Int, Int>()
            val totalPages = planPageCount(snapshot, pageNumbers)
            renderCover(pdfDoc, snapshot, totalPages = totalPages)
            var nextPageNumber = 2
            snapshot.perTestSections.forEachIndexed { sectionIndex, section ->
                val sectionPageCount = pageNumbers.getValue(sectionIndex)
                renderTestSection(
                    pdfDoc = pdfDoc,
                    section = section,
                    firstPageNumber = nextPageNumber,
                    totalPages = totalPages
                )
                nextPageNumber += sectionPageCount
            }
            pdfDoc.writeTo(output)
        } finally {
            pdfDoc.close()
        }
    }

    private fun planPageCount(
        snapshot: PdfReportSnapshot,
        pageCounts: MutableMap<Int, Int>
    ): Int {
        var total = 1
        snapshot.perTestSections.forEachIndexed { sectionIndex, section ->
            val count = pagesNeededForSection(section)
            pageCounts[sectionIndex] = count
            total += count
        }
        return total
    }

    private fun pagesNeededForSection(section: PdfTestSection): Int {
        val chartsPerPage = chartsPerPageBudget()
        val chartCount = section.featureSeries.size
        val chartPages = if (chartCount == 0) 1 else {
            (chartCount + chartsPerPage - 1) / chartsPerPage
        }
        return chartPages
    }

    private fun chartsPerPageBudget(): Int {
        val available = PdfPageLayout.PAGE_HEIGHT_POINTS -
            PdfPageLayout.MARGIN_TOP -
            PdfPageLayout.MARGIN_BOTTOM -
            PdfPageLayout.FONT_SIZE_PAGE_HEADING * PdfPageLayout.LINE_HEIGHT_MULTIPLIER -
            PdfPageLayout.FONT_SIZE_PAGE_HEADING * PdfPageLayout.LINE_HEIGHT_MULTIPLIER
        val perChart = PdfPageLayout.CHART_HEIGHT_POINTS + PdfPageLayout.FONT_SIZE_PAGE_BODY
        val n = (available / perChart).toInt()
        return n.coerceAtLeast(1)
    }

    private fun renderCover(pdfDoc: PdfDocument, snapshot: PdfReportSnapshot, totalPages: Int) {
        val info = PdfDocument.PageInfo.Builder(
            PdfPageLayout.PAGE_WIDTH_POINTS,
            PdfPageLayout.PAGE_HEIGHT_POINTS,
            1
        ).create()
        val page = pdfDoc.startPage(info)
        val canvas = page.canvas
        val headlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = PdfPageLayout.FONT_SIZE_COVER_HEADLINE
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = PdfPageLayout.FONT_SIZE_COVER_BODY
            typeface = Typeface.SANS_SERIF
        }
        var cursorY = PdfPageLayout.MARGIN_TOP + PdfPageLayout.FONT_SIZE_COVER_HEADLINE
        canvas.drawText(
            context.getString(R.string.phase10_pdf_cover_title),
            PdfPageLayout.MARGIN_LEFT,
            cursorY,
            headlinePaint
        )
        cursorY += PdfPageLayout.FONT_SIZE_COVER_BODY * PdfPageLayout.LINE_HEIGHT_MULTIPLIER * 2

        val df = SimpleDateFormat("yyyy-MM-dd", locale)
        canvas.drawText(
            context.getString(R.string.phase10_pdf_cover_generated_on, df.format(Date(snapshot.generatedAtEpochMs))),
            PdfPageLayout.MARGIN_LEFT, cursorY, bodyPaint
        )
        cursorY += PdfPageLayout.FONT_SIZE_COVER_BODY * PdfPageLayout.LINE_HEIGHT_MULTIPLIER

        if (snapshot.earliestSessionEpochMs != null && snapshot.latestSessionEpochMs != null) {
            canvas.drawText(
                context.getString(
                    R.string.phase10_pdf_cover_date_range,
                    df.format(Date(snapshot.earliestSessionEpochMs)),
                    df.format(Date(snapshot.latestSessionEpochMs))
                ),
                PdfPageLayout.MARGIN_LEFT, cursorY, bodyPaint
            )
            cursorY += PdfPageLayout.FONT_SIZE_COVER_BODY * PdfPageLayout.LINE_HEIGHT_MULTIPLIER
        }

        val testCount = snapshot.perTestSections.size
        val sessionCountLine = if (snapshot.totalCompletedSessions == 1 && testCount == 1) {
            context.getString(R.string.phase10_pdf_cover_session_count_single)
        } else {
            context.getString(
                R.string.phase10_pdf_cover_session_count_template,
                snapshot.totalCompletedSessions,
                testCount
            )
        }
        canvas.drawText(sessionCountLine, PdfPageLayout.MARGIN_LEFT, cursorY, bodyPaint)
        cursorY += PdfPageLayout.FONT_SIZE_COVER_BODY * PdfPageLayout.LINE_HEIGHT_MULTIPLIER * 2

        canvas.drawText(
            context.getString(R.string.phase10_pdf_cover_user_facing_role),
            PdfPageLayout.MARGIN_LEFT, cursorY, bodyPaint
        )
        cursorY += PdfPageLayout.FONT_SIZE_COVER_BODY * PdfPageLayout.LINE_HEIGHT_MULTIPLIER * 2

        canvas.drawText(
            context.getString(R.string.phase9_about_section_disclaimer_body),
            PdfPageLayout.MARGIN_LEFT, cursorY, bodyPaint
        )

        drawFooter(canvas, pageNumber = 1, totalPages = totalPages)
        pdfDoc.finishPage(page)
    }

    private fun renderTestSection(
        pdfDoc: PdfDocument,
        section: PdfTestSection,
        firstPageNumber: Int,
        totalPages: Int
    ) {
        val chartsPerPage = chartsPerPageBudget()
        val chartChunks: List<List<com.mustafanazeer.baselinems.ui.reports.FeatureSeries>> =
            if (section.featureSeries.isEmpty()) {
                listOf(emptyList())
            } else {
                section.featureSeries.chunked(chartsPerPage)
            }

        chartChunks.forEachIndexed { chunkIndex, chartsForPage ->
            val pageNumber = firstPageNumber + chunkIndex
            val isLastPageForSection = chunkIndex == chartChunks.lastIndex
            val info = PdfDocument.PageInfo.Builder(
                PdfPageLayout.PAGE_WIDTH_POINTS,
                PdfPageLayout.PAGE_HEIGHT_POINTS,
                pageNumber
            ).create()
            val page = pdfDoc.startPage(info)
            val canvas = page.canvas
            val headingPaint = headingPaint()
            val bodyPaint = bodyPaint()

            var cursorY = PdfPageLayout.MARGIN_TOP + PdfPageLayout.FONT_SIZE_PAGE_HEADING
            canvas.drawText(section.displayName, PdfPageLayout.MARGIN_LEFT, cursorY, headingPaint)
            cursorY += PdfPageLayout.FONT_SIZE_PAGE_HEADING * PdfPageLayout.LINE_HEIGHT_MULTIPLIER

            if (chartsForPage.isNotEmpty()) {
                canvas.drawText(
                    context.getString(
                        R.string.phase10_pdf_section_trend,
                        section.sessionCount
                    ),
                    PdfPageLayout.MARGIN_LEFT,
                    cursorY,
                    bodyPaint
                )
                cursorY += PdfPageLayout.FONT_SIZE_PAGE_BODY * PdfPageLayout.LINE_HEIGHT_MULTIPLIER

                chartsForPage.forEach { series ->
                    val axisLabel = series.unitPhraseResId?.let { context.getString(it) } ?: series.key
                    val chartBounds = RectF(
                        PdfPageLayout.MARGIN_LEFT,
                        cursorY,
                        PdfPageLayout.PAGE_WIDTH_POINTS - PdfPageLayout.MARGIN_RIGHT,
                        cursorY + PdfPageLayout.CHART_HEIGHT_POINTS
                    )
                    chartRenderer.render(canvas, chartBounds, series, axisLabel)
                    cursorY += PdfPageLayout.CHART_HEIGHT_POINTS + PdfPageLayout.FONT_SIZE_PAGE_BODY
                }
            }

            if (isLastPageForSection && section.summaryRows.isNotEmpty()) {
                renderSummaryTable(
                    canvas = canvas,
                    section = section,
                    cursorY = cursorY
                )
            }

            drawFooter(canvas, pageNumber = pageNumber, totalPages = totalPages)
            pdfDoc.finishPage(page)
        }
    }

    private fun renderSummaryTable(
        canvas: Canvas,
        section: PdfTestSection,
        cursorY: Float
    ) {
        val headingPaint = headingPaint()
        val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = PdfPageLayout.FONT_SIZE_PAGE_BODY
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = PdfPageLayout.FONT_SIZE_PAGE_BODY
            typeface = Typeface.SANS_SERIF
        }

        var localY = cursorY
        canvas.drawText(
            context.getString(R.string.phase10_pdf_section_summary_table),
            PdfPageLayout.MARGIN_LEFT,
            localY,
            headingPaint
        )
        localY += PdfPageLayout.FONT_SIZE_PAGE_HEADING * PdfPageLayout.LINE_HEIGHT_MULTIPLIER

        val featureKeys = section.featureSeries.map { it.key }
        val featureHeaders = section.featureSeries.map { series ->
            series.unitPhraseResId?.let { context.getString(it) } ?: series.key
        }
        val includeSteadiness = section.testType == TestType.VOICE
        val emDash = context.getString(R.string.phase9_summary_table_em_dash_placeholder)

        val contentWidth = PdfPageLayout.PAGE_WIDTH_POINTS -
            PdfPageLayout.MARGIN_LEFT - PdfPageLayout.MARGIN_RIGHT
        val fixedDateWidth = 70f
        val fixedQualityWidth = 78f
        val fixedNotesWidth = 90f
        val fixedSteadinessWidth = if (includeSteadiness) 74f else 0f
        val fixedColumnsTotal = fixedDateWidth + fixedQualityWidth + fixedNotesWidth + fixedSteadinessWidth
        val flexibleColumnCount = featureKeys.size.coerceAtLeast(1)
        val perFeatureWidth = ((contentWidth - fixedColumnsTotal) / flexibleColumnCount)
            .coerceAtLeast(40f)

        val columnX = mutableListOf<Float>()
        val columnWidths = mutableListOf<Float>()
        var x = PdfPageLayout.MARGIN_LEFT
        columnX += x; columnWidths += fixedDateWidth; x += fixedDateWidth
        featureKeys.forEach { _ ->
            columnX += x; columnWidths += perFeatureWidth; x += perFeatureWidth
        }
        columnX += x; columnWidths += fixedQualityWidth; x += fixedQualityWidth
        columnX += x; columnWidths += fixedNotesWidth; x += fixedNotesWidth
        if (includeSteadiness) {
            columnX += x; columnWidths += fixedSteadinessWidth
        }

        val headerLabels = mutableListOf<String>()
        headerLabels += context.getString(R.string.phase9_summary_table_column_date)
        headerLabels += featureHeaders
        headerLabels += context.getString(R.string.phase9_summary_table_column_quality)
        headerLabels += context.getString(R.string.phase9_summary_table_column_notes)
        if (includeSteadiness) {
            headerLabels += context.getString(R.string.phase9_summary_table_column_steadiness)
        }

        drawRow(
            canvas = canvas,
            labels = headerLabels,
            xs = columnX,
            widths = columnWidths,
            paint = tableHeaderPaint,
            baselineY = localY
        )
        localY += PdfPageLayout.SUMMARY_TABLE_ROW_HEIGHT

        val bottomLimit = PdfPageLayout.PAGE_HEIGHT_POINTS -
            PdfPageLayout.MARGIN_BOTTOM -
            PdfPageLayout.FONT_SIZE_FOOTER * 3

        for (row in section.summaryRows) {
            if (localY + PdfPageLayout.SUMMARY_TABLE_ROW_HEIGHT > bottomLimit) break
            val cells = buildRowCells(
                row = row,
                featureKeys = featureKeys,
                includeSteadiness = includeSteadiness,
                emDash = emDash
            )
            drawRow(
                canvas = canvas,
                labels = cells,
                xs = columnX,
                widths = columnWidths,
                paint = cellPaint,
                baselineY = localY
            )
            localY += PdfPageLayout.SUMMARY_TABLE_ROW_HEIGHT
        }
    }

    private fun buildRowCells(
        row: SummaryRow,
        featureKeys: List<String>,
        includeSteadiness: Boolean,
        emDash: String
    ): List<String> {
        val cells = mutableListOf<String>()
        cells += row.dateLabel
        featureKeys.forEach { key ->
            cells += row.perFeatureValues[key] ?: emDash
        }
        cells += qualityChipLabel(row.qualityBand)
        cells += row.contextCellResId?.let { context.getString(it) } ?: ""
        if (includeSteadiness) {
            cells += row.steadinessBandLabel
                ?: voiceSteadinessLabelFromRow(row)
                ?: emDash
        }
        return cells
    }

    private fun voiceSteadinessLabelFromRow(row: SummaryRow): String? {
        val jitter = row.perFeatureValues["jitter_local"]?.toDoubleOrNullSafe()
        val shimmer = row.perFeatureValues["shimmer_local"]?.toDoubleOrNullSafe()
        if (jitter == null || shimmer == null) return null
        val features = VoiceFeatureSet(
            jitterLocal = jitter,
            shimmerLocal = shimmer,
            hnrDb = null,
            f0MeanHz = null,
            f0SdHz = null,
            speakingRateWpm = null,
            pauseFraction = 0.0,
            voicedSeconds = 0.0,
            totalSeconds = 0.0,
            periodCount = 0
        )
        val band = VoiceFeatures.steadinessBand(features)
        return context.getString(
            when (band) {
                VoiceSteadinessBand.STEADY -> R.string.phase9_voice_steadiness_steady
                VoiceSteadinessBand.MOSTLY_STEADY -> R.string.phase9_voice_steadiness_mostly_steady
                VoiceSteadinessBand.VARIED -> R.string.phase9_voice_steadiness_varied
                VoiceSteadinessBand.UNMEASURABLE -> R.string.phase9_voice_steadiness_unmeasurable
            }
        )
    }

    private fun String.toDoubleOrNullSafe(): Double? {
        return try {
            this.replace(',', '.').toDoubleOrNull()
        } catch (t: Throwable) {
            null
        }
    }

    private fun qualityChipLabel(band: QualityBand): String {
        val resId = when (band) {
            QualityBand.HIGH -> R.string.phase9_quality_chip_reliable
            QualityBand.MEDIUM -> R.string.phase9_quality_chip_mostly_reliable
            QualityBand.LOW -> R.string.phase9_quality_chip_less_reliable
        }
        return context.getString(resId)
    }

    private fun drawRow(
        canvas: Canvas,
        labels: List<String>,
        xs: List<Float>,
        widths: List<Float>,
        paint: Paint,
        baselineY: Float
    ) {
        labels.forEachIndexed { index, label ->
            val cellLeft = xs[index]
            val cellWidth = widths[index]
            val text = truncateToWidth(label, paint, cellWidth)
            canvas.drawText(text, cellLeft, baselineY, paint)
        }
    }

    private fun truncateToWidth(text: String, paint: Paint, width: Float): String {
        if (text.isEmpty()) return text
        if (paint.measureText(text) <= width) return text
        val ellipsis = "…"
        var trimmed = text
        while (trimmed.isNotEmpty() && paint.measureText(trimmed + ellipsis) > width) {
            trimmed = trimmed.dropLast(1)
        }
        return if (trimmed.isEmpty()) ellipsis else trimmed + ellipsis
    }

    private fun headingPaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = PdfPageLayout.FONT_SIZE_PAGE_HEADING
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private fun bodyPaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = PdfPageLayout.FONT_SIZE_PAGE_BODY
        typeface = Typeface.SANS_SERIF
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int, totalPages: Int) {
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = PdfPageLayout.FONT_SIZE_FOOTER
            typeface = Typeface.SANS_SERIF
        }
        val disclaimer = context.getString(R.string.phase9_test_detail_no_overlay_disclaimer)
        val pageLabel = context.getString(R.string.phase10_pdf_footer_page_template, pageNumber, totalPages)
        val baseline = PdfPageLayout.PAGE_HEIGHT_POINTS - PdfPageLayout.MARGIN_BOTTOM + PdfPageLayout.FONT_SIZE_FOOTER * 2
        canvas.drawText(disclaimer, PdfPageLayout.MARGIN_LEFT, baseline, footerPaint)
        canvas.drawText(
            pageLabel,
            PdfPageLayout.PAGE_WIDTH_POINTS - PdfPageLayout.MARGIN_RIGHT - footerPaint.measureText(pageLabel),
            baseline,
            footerPaint
        )
    }
}
