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
            val totalPages = 1 + snapshot.perTestSections.size
            renderCover(pdfDoc, snapshot, totalPages = totalPages)
            snapshot.perTestSections.forEachIndexed { index, section ->
                renderTestPage(pdfDoc, section, pageNumber = index + 2, totalPages = totalPages)
            }
            pdfDoc.writeTo(output)
        } finally {
            pdfDoc.close()
        }
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

    private fun renderTestPage(
        pdfDoc: PdfDocument,
        section: PdfTestSection,
        pageNumber: Int,
        totalPages: Int
    ) {
        val info = PdfDocument.PageInfo.Builder(
            PdfPageLayout.PAGE_WIDTH_POINTS,
            PdfPageLayout.PAGE_HEIGHT_POINTS,
            pageNumber
        ).create()
        val page = pdfDoc.startPage(info)
        val canvas = page.canvas
        val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = PdfPageLayout.FONT_SIZE_PAGE_HEADING
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = PdfPageLayout.FONT_SIZE_PAGE_BODY
            typeface = Typeface.SANS_SERIF
        }
        var cursorY = PdfPageLayout.MARGIN_TOP + PdfPageLayout.FONT_SIZE_PAGE_HEADING
        canvas.drawText(section.displayName, PdfPageLayout.MARGIN_LEFT, cursorY, headingPaint)
        cursorY += PdfPageLayout.FONT_SIZE_PAGE_HEADING * PdfPageLayout.LINE_HEIGHT_MULTIPLIER

        section.featureSeries.forEach { series ->
            val axisLabel = series.unitPhraseResId?.let { context.getString(it) } ?: series.key
            val chartBounds = RectF(
                PdfPageLayout.MARGIN_LEFT,
                cursorY,
                PdfPageLayout.PAGE_WIDTH_POINTS - PdfPageLayout.MARGIN_RIGHT,
                cursorY + PdfPageLayout.CHART_HEIGHT_POINTS
            )
            chartRenderer.render(canvas, chartBounds, series, axisLabel)
            cursorY += PdfPageLayout.CHART_HEIGHT_POINTS + PdfPageLayout.FONT_SIZE_PAGE_BODY
            if (cursorY > PdfPageLayout.PAGE_HEIGHT_POINTS - PdfPageLayout.MARGIN_BOTTOM - PdfPageLayout.CHART_HEIGHT_POINTS) {
                return@forEach
            }
        }

        drawFooter(canvas, pageNumber = pageNumber, totalPages = totalPages)
        pdfDoc.finishPage(page)
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
