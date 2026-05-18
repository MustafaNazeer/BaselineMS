package com.mustafanazeer.baselinems.report

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ExportFileNaming {
    fun pdfFor(epochMs: Long, locale: Locale = Locale.ROOT, tz: TimeZone = TimeZone.getDefault()): String =
        "BaselineMS_report_${format(epochMs, locale, tz)}.pdf"

    fun csvFor(epochMs: Long, locale: Locale = Locale.ROOT, tz: TimeZone = TimeZone.getDefault()): String =
        "BaselineMS_report_${format(epochMs, locale, tz)}.csv"

    private fun format(epochMs: Long, locale: Locale, tz: TimeZone): String {
        val df = SimpleDateFormat("yyyy-MM-dd", locale).apply { timeZone = tz }
        return df.format(Date(epochMs))
    }
}
