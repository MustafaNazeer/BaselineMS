package com.mustafanazeer.baselinems.ui.reports

import android.text.format.DateFormat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import java.util.Date
import java.util.Locale
import com.mustafanazeer.baselinems.R

@Composable
fun PerChartSummaryLine(
    series: FeatureSeries,
    modifier: Modifier = Modifier,
    displayLabel: String? = null
) {
    val text = buildPerChartSummaryText(series, displayLabel) ?: return
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
internal fun buildPerChartSummaryText(series: FeatureSeries, displayLabel: String? = null): String? {
    val included = series.points.filterNot { it.omittedFromChart }
    if (included.isEmpty()) return null
    val resolvedLabel = displayLabel ?: series.displayName
    val unitPhrase = series.unitPhraseResId?.let { stringResource(it) }
        ?: resolvedLabel.lowercase(Locale.getDefault())
    val latestPoint = included.last()
    val latestValueText = formatSummaryValue(series, latestPoint.value)

    return when (included.size) {
        1 -> stringResource(R.string.phase9_summary_line_one_session, unitPhrase, latestValueText)
        2, 3 -> {
            val medianValueText = formatSummaryValue(series, median(included.map { it.value }))
            val firstDate = formatLongDate(included.first().epochMs)
            val latestDate = formatLongDate(latestPoint.epochMs)
            stringResource(
                R.string.phase9_summary_line_2_3_sessions,
                unitPhrase,
                latestValueText,
                included.size,
                medianValueText,
                firstDate,
                latestDate
            )
        }
        else -> {
            val lastFour = included.takeLast(4)
            val medianValueText = formatSummaryValue(series, median(lastFour.map { it.value }))
            val firstDate = formatLongDate(included.first().epochMs)
            val latestDate = formatLongDate(latestPoint.epochMs)
            stringResource(
                R.string.phase9_summary_line_4plus_sessions,
                unitPhrase,
                latestValueText,
                medianValueText,
                firstDate,
                latestDate
            )
        }
    }
}

internal fun median(values: List<Double>): Double {
    if (values.isEmpty()) return 0.0
    val sorted = values.sorted()
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 1) {
        sorted[mid]
    } else {
        (sorted[mid - 1] + sorted[mid]) / 2.0
    }
}

internal fun formatSummaryValue(series: FeatureSeries, value: Double): String {
    series.valueFormatter?.let { return it(value) }
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format(Locale.getDefault(), "%.2f", value)
    }
}

internal fun formatLongDate(epochMs: Long): String {
    val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "d MMMM y")
    val formatter = java.text.SimpleDateFormat(pattern, Locale.getDefault())
    return formatter.format(Date(epochMs))
}
