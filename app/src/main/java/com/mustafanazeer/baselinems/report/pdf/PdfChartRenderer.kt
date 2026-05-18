package com.mustafanazeer.baselinems.report.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.mustafanazeer.baselinems.ui.reports.FeatureSeries
import com.mustafanazeer.baselinems.ui.reports.QualityBand

class PdfChartRenderer {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = PdfPageLayout.CHART_LINE_WIDTH
        style = Paint.Style.STROKE
    }

    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }

    private val markerNotchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = PdfPageLayout.FONT_SIZE_CHART_AXIS
        typeface = Typeface.SANS_SERIF
    }

    fun render(canvas: Canvas, bounds: RectF, series: FeatureSeries, axisLabel: String) {
        val plottable = series.points.filter { !it.omittedFromChart && !it.value.isNaN() }
        drawAxisLabel(canvas, bounds, axisLabel)
        if (plottable.isEmpty()) return

        val minValue = plottable.minOf { it.value }
        val maxValue = plottable.maxOf { it.value }
        val valueRange = (maxValue - minValue).takeIf { it > 0 } ?: 1.0
        val plotArea = RectF(
            bounds.left + PdfPageLayout.CHART_INNER_PADDING,
            bounds.top + PdfPageLayout.CHART_INNER_PADDING + PdfPageLayout.FONT_SIZE_CHART_AXIS + 4f,
            bounds.right - PdfPageLayout.CHART_INNER_PADDING,
            bounds.bottom - PdfPageLayout.CHART_INNER_PADDING
        )

        var previousX: Float? = null
        var previousY: Float? = null
        plottable.forEachIndexed { index, point ->
            val xFraction = if (plottable.size == 1) 0.5f else index.toFloat() / (plottable.size - 1)
            val x = plotArea.left + xFraction * plotArea.width()
            val yFraction = ((point.value - minValue) / valueRange).toFloat()
            val y = plotArea.bottom - yFraction * plotArea.height()
            if (previousX != null && previousY != null) {
                canvas.drawLine(previousX!!, previousY!!, x, y, linePaint)
            }
            drawMarker(canvas, x, y, point.qualityBand)
            previousX = x
            previousY = y
        }
    }

    private fun drawMarker(canvas: Canvas, x: Float, y: Float, band: QualityBand) {
        when (band) {
            QualityBand.HIGH -> {
                markerPaint.style = Paint.Style.FILL
                markerPaint.strokeWidth = 0f
                canvas.drawCircle(x, y, PdfPageLayout.CHART_MARKER_RADIUS_HIGH, markerPaint)
            }
            QualityBand.MEDIUM -> {
                markerPaint.style = Paint.Style.FILL
                markerPaint.strokeWidth = 0f
                canvas.drawCircle(x, y, PdfPageLayout.CHART_MARKER_RADIUS_MEDIUM, markerPaint)
                canvas.drawRect(
                    x,
                    y - PdfPageLayout.CHART_MARKER_RADIUS_MEDIUM,
                    x + PdfPageLayout.CHART_MARKER_RADIUS_MEDIUM,
                    y + PdfPageLayout.CHART_MARKER_RADIUS_MEDIUM,
                    markerNotchPaint
                )
            }
            QualityBand.LOW -> {
                markerPaint.style = Paint.Style.STROKE
                markerPaint.strokeWidth = 1.5f
                canvas.drawCircle(x, y, PdfPageLayout.CHART_MARKER_RADIUS_LOW, markerPaint)
            }
        }
    }

    private fun drawAxisLabel(canvas: Canvas, bounds: RectF, axisLabel: String) {
        canvas.drawText(axisLabel, bounds.left, bounds.top + PdfPageLayout.FONT_SIZE_CHART_AXIS, axisPaint)
    }
}
