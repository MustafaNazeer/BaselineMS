package com.mustafanazeer.baselinems.battery.vision

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatContrastPercentTest {

    @Test
    fun renders_whole_number_without_trailing_zero() {
        assertEquals("100", formatContrastPercent(100.0))
    }

    @Test
    fun renders_fractional_value_unchanged() {
        assertEquals("2.5", formatContrastPercent(2.5))
        assertEquals("1.25", formatContrastPercent(1.25))
    }
}
