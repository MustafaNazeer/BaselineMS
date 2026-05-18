package com.mustafanazeer.baselinems.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mustafanazeer.baselinems.R

private val ChartLineColor = Color(0xFF1F3C88)
private val ReferenceBandColor = Color(0xFF1F3C88)

@Composable
fun TrendLineChart(
    series: FeatureSeries,
    modifier: Modifier = Modifier,
    isSparkline: Boolean = false,
    referenceBand: ReferenceRange? = null,
    displayLabel: String? = null
) {
    val chartHeight = if (isSparkline) 48.dp else 200.dp
    val visiblePoints = series.points.filterNot { it.omittedFromChart }
    val resolvedLabel = displayLabel ?: series.displayName
    val contentDescription = if (isSparkline) "" else buildChartContentDescription(series, resolvedLabel)

    Column(modifier = modifier.fillMaxWidth()) {
        if (!isSparkline) {
            Text(
                text = resolvedLabel,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp)
                .then(
                    if (contentDescription.isNotEmpty()) {
                        Modifier.semantics { this.contentDescription = contentDescription }
                    } else {
                        Modifier
                    }
                )
        ) {
            if (visiblePoints.isEmpty()) return@Canvas
            val axisRange = computeAxisRange(visiblePoints, referenceBand)
            val span = (axisRange.max - axisRange.min).takeIf { it > 0.0 } ?: 1.0

            referenceBand?.let { band ->
                val bandLowNorm = ((band.mean - band.sd - axisRange.min) / span)
                    .toFloat().coerceIn(0f, 1f)
                val bandHighNorm = ((band.mean + band.sd - axisRange.min) / span)
                    .toFloat().coerceIn(0f, 1f)
                val topY = size.height * (1f - bandHighNorm)
                val bottomY = size.height * (1f - bandLowNorm)
                drawRect(
                    color = ReferenceBandColor.copy(alpha = 0.2f),
                    topLeft = Offset(0f, topY),
                    size = Size(size.width, bottomY - topY)
                )
            }

            val widthStep = if (visiblePoints.size > 1) {
                size.width / (visiblePoints.size - 1)
            } else {
                0f
            }
            val offsets = visiblePoints.mapIndexed { index, point ->
                val x = if (visiblePoints.size > 1) index * widthStep else size.width / 2f
                val normalized = ((point.value - axisRange.min) / span)
                    .toFloat().coerceIn(0f, 1f)
                val y = size.height * (1f - normalized)
                Offset(x, y)
            }
            for (i in 1 until offsets.size) {
                drawLine(
                    color = ChartLineColor,
                    start = offsets[i - 1],
                    end = offsets[i],
                    strokeWidth = 4f
                )
            }
            if (!isSparkline) {
                offsets.forEachIndexed { index, offset ->
                    val point = visiblePoints[index]
                    val isLow = point.qualityBand == QualityBand.LOW
                    val pointColor = if (isLow) ChartLineColor.copy(alpha = 0.6f) else ChartLineColor
                    if (isLow) {
                        drawCircle(
                            color = pointColor,
                            radius = 8f,
                            center = offset,
                            style = Stroke(width = 3f)
                        )
                    } else {
                        drawCircle(
                            color = pointColor,
                            radius = 8f,
                            center = offset
                        )
                    }
                }
            }
        }
        referenceBand?.let { band ->
            Text(
                text = stringResource(band.captionResId),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

internal data class AxisRange(val min: Double, val max: Double)

internal fun computeAxisRange(
    visiblePoints: List<TimedPoint>,
    referenceBand: ReferenceRange? = null
): AxisRange {
    val highMediumValues = visiblePoints
        .filter { it.qualityBand != QualityBand.LOW }
        .map { it.value }
    val sourceValues = if (highMediumValues.isNotEmpty()) {
        highMediumValues
    } else {
        visiblePoints.map { it.value }
    }
    if (sourceValues.isEmpty()) return AxisRange(0.0, 1.0)
    var min = sourceValues.min()
    var max = sourceValues.max()
    referenceBand?.let { band ->
        min = minOf(min, band.mean - band.sd)
        max = maxOf(max, band.mean + band.sd)
    }
    if (min == max) {
        return AxisRange(min - 0.5, max + 0.5)
    }
    return AxisRange(min, max)
}

@Composable
private fun buildChartContentDescription(series: FeatureSeries, displayLabel: String): String {
    val context = LocalContext.current
    val payload = remember(series, displayLabel, context) {
        val included = series.points.filterNot { it.omittedFromChart }
        if (included.isEmpty()) return@remember null
        val valueStrings = included.joinToString(", ") { point ->
            series.valueFormatter?.invoke(point.value) ?: formatValueForDescription(point.value)
        }
        val unitSuffix = series.unitSuffixResId?.let { context.getString(it) } ?: series.unit
        ChartContentDescriptionPayload(included.size, valueStrings, unitSuffix)
    } ?: return ""
    return stringResource(
        R.string.phase9_chart_content_description,
        displayLabel,
        payload.pointCount,
        payload.valueStrings,
        payload.unitSuffix
    )
}

private data class ChartContentDescriptionPayload(
    val pointCount: Int,
    val valueStrings: String,
    val unitSuffix: String
)

internal fun formatValueForDescription(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format(java.util.Locale.US, "%.2f", value)
    }
}
