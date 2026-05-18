package com.mustafanazeer.baselinems.ui.reports

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrendLineChartTest {

    private fun point(value: Double, band: QualityBand, omitted: Boolean = false, reason: String? = null) =
        TimedPoint(epochMs = 0L, value = value, qualityBand = band, omittedFromChart = omitted, omissionReason = reason)

    @Test
    fun axis_range_uses_high_and_medium_only_when_a_low_outlier_is_present() {
        val points = listOf(
            point(92.0, QualityBand.HIGH),
            point(94.0, QualityBand.MEDIUM),
            point(40.0, QualityBand.LOW),
            point(96.0, QualityBand.HIGH)
        )
        val range = computeAxisRange(points.filterNot { it.omittedFromChart })
        assertEquals(92.0, range.min, 0.0001)
        assertEquals(96.0, range.max, 0.0001)
    }

    @Test
    fun axis_range_falls_back_to_all_points_when_every_point_is_low() {
        val points = listOf(
            point(40.0, QualityBand.LOW),
            point(50.0, QualityBand.LOW),
            point(45.0, QualityBand.LOW)
        )
        val range = computeAxisRange(points.filterNot { it.omittedFromChart })
        assertEquals(40.0, range.min, 0.0001)
        assertEquals(50.0, range.max, 0.0001)
    }

    @Test
    fun axis_range_includes_reference_band_when_provided() {
        val points = listOf(
            point(96.0, QualityBand.HIGH),
            point(98.0, QualityBand.HIGH)
        )
        val band = ReferenceRange(
            mean = 115.2,
            sd = 5.0,
            captionResId = 0,
            unitsLabel = "steps per minute"
        )
        val range = computeAxisRange(points.filterNot { it.omittedFromChart }, band)
        assertTrue(range.min <= 96.0)
        assertTrue(range.max >= 120.2)
    }

    @Test
    fun axis_range_handles_single_constant_value_without_collapsing_to_zero_span() {
        val points = listOf(point(96.0, QualityBand.HIGH))
        val range = computeAxisRange(points)
        assertTrue(range.min < range.max)
    }

    @Test
    fun omitted_point_is_filtered_from_visible_points_before_axis_computation() {
        val all = listOf(
            point(92.0, QualityBand.HIGH),
            point(500.0, QualityBand.HIGH, omitted = true, reason = "Speech window too short"),
            point(94.0, QualityBand.HIGH)
        )
        val visible = all.filterNot { it.omittedFromChart }
        val range = computeAxisRange(visible)
        assertEquals(92.0, range.min, 0.0001)
        assertEquals(94.0, range.max, 0.0001)
    }

    @Test
    fun all_low_axis_includes_the_only_low_point() {
        val points = listOf(point(60.0, QualityBand.LOW))
        val range = computeAxisRange(points)
        assertTrue(range.min <= 60.0)
        assertTrue(range.max >= 60.0)
    }

    @Test
    fun formatted_integer_value_drops_decimal_point() {
        assertEquals("96", formatValueForDescription(96.0))
    }

    @Test
    fun formatted_non_integer_value_keeps_two_decimals() {
        assertEquals("96.50", formatValueForDescription(96.5))
    }

    @Test
    fun timed_point_with_speaking_rate_guard_rail_omission_is_excluded() {
        val omitted = point(500.0, QualityBand.HIGH, omitted = true, reason = "Speech window too short")
        assertEquals("Speech window too short", omitted.omissionReason)
        assertTrue(omitted.omittedFromChart)
    }

    @Test
    fun missing_feature_point_is_marked_omitted_with_null_reason() {
        val omitted = point(Double.NaN, QualityBand.HIGH, omitted = true, reason = null)
        assertNull(omitted.omissionReason)
        assertTrue(omitted.omittedFromChart)
    }

    @Test
    fun reference_band_does_not_widen_range_when_band_already_inside_span() {
        val points = listOf(
            point(50.0, QualityBand.HIGH),
            point(200.0, QualityBand.HIGH)
        )
        val band = ReferenceRange(
            mean = 110.0,
            sd = 5.0,
            captionResId = 0,
            unitsLabel = "spm"
        )
        val range = computeAxisRange(points, band)
        assertEquals(50.0, range.min, 0.0001)
        assertEquals(200.0, range.max, 0.0001)
    }

    @Test
    fun empty_visible_points_returns_a_non_degenerate_axis_range() {
        val range = computeAxisRange(emptyList())
        assertNotNull(range)
        assertTrue(range.min < range.max)
    }
}
