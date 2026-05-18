package com.mustafanazeer.baselinems.report

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

class ExportFileNamingTest {

    @Test
    fun pdf_namesByDateOnly() {
        val name = ExportFileNaming.pdfFor(epochMs = 1_700_000_000_000L, locale = Locale.ROOT, tz = TimeZone.getTimeZone("UTC"))
        assertEquals("BaselineMS_report_2023-11-14.pdf", name)
    }

    @Test
    fun csv_namesByDateOnly() {
        val name = ExportFileNaming.csvFor(epochMs = 1_700_000_000_000L, locale = Locale.ROOT, tz = TimeZone.getTimeZone("UTC"))
        assertEquals("BaselineMS_report_2023-11-14.csv", name)
    }
}
