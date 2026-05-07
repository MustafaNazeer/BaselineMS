package com.mustafan4x.msbattery.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class HomeScreenFormattersTest {

    private fun atLocalNoon(year: Int, month: Int, day: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeZone = TimeZone.getDefault()
        cal.set(year, month, day, 12, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    @Test fun todayShowsTodayPrefix() {
        val now = System.currentTimeMillis()
        val label = formatRelative(now, locale = Locale.US)
        assertEquals(true, label.startsWith("Today, "))
    }

    @Test fun yesterdayShowsYesterdayPrefix() {
        val now = System.currentTimeMillis()
        val yesterday = now - 24L * 60 * 60 * 1000
        val label = formatRelative(yesterday, locale = Locale.US)
        assertEquals(true, label.startsWith("Yesterday, "))
    }

    @Test fun weekAgoShowsAbsoluteMonthDay() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -10)
        val label = formatRelative(cal.timeInMillis, locale = Locale.US)
        assertEquals(true, label.matches(Regex("[A-Z][a-z]+ \\d{1,2}")))
    }
}
