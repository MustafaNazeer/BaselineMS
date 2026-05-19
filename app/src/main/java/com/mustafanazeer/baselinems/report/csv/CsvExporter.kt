package com.mustafanazeer.baselinems.report.csv

import com.mustafanazeer.baselinems.data.PdfReportSnapshot
import com.mustafanazeer.baselinems.ui.reports.QualityBand
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val HEADER = "session_id,session_started_at_iso,test_type,test_result_id,test_started_at_iso,test_completed_at_iso,quality_score,quality_band,feature_key,feature_value"

class CsvExporter {

    fun toCsv(snapshot: PdfReportSnapshot): String {
        val sb = StringBuilder()
        sb.append(HEADER).append('\n')
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        snapshot.perTestSections.forEach { section ->
            section.featureSeries.forEach { series ->
                series.points
                    .sortedBy { it.epochMs }
                    .forEach { point ->
                        val ts = iso.format(Date(point.epochMs))
                        sb.append("").append(',')              // session_id derived from caller map; placeholder if absent
                        sb.append(ts).append(',')
                        sb.append(section.testType.name).append(',')
                        sb.append("").append(',')              // test_result_id absent in snapshot view; deterministic blank
                        sb.append(ts).append(',')
                        sb.append(ts).append(',')
                        sb.append("").append(',')              // quality_score not surfaced per-point; row level only
                        sb.append(qualityBandToken(point.qualityBand)).append(',')
                        sb.append(quote(series.key)).append(',')
                        sb.append(formatValue(point.value)).append('\n')
                    }
            }
        }
        return sb.toString()
    }

    private fun qualityBandToken(band: QualityBand): String = when (band) {
        QualityBand.HIGH -> "HIGH"
        QualityBand.MEDIUM -> "MEDIUM"
        QualityBand.LOW -> "LOW"
    }

    private fun quote(value: String): String {
        if (value.any { it == ',' || it == '"' || it == '\n' }) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }

    private fun formatValue(value: Double): String {
        if (value.isNaN()) return ""
        return String.format(Locale.ROOT, "%.6f", value)
    }
}
