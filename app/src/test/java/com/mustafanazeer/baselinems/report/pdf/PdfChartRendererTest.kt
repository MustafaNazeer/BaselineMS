package com.mustafanazeer.baselinems.report.pdf

import android.graphics.Picture
import android.graphics.RectF
import com.mustafanazeer.baselinems.ui.reports.FeatureSeries
import com.mustafanazeer.baselinems.ui.reports.QualityBand
import com.mustafanazeer.baselinems.ui.reports.TimedPoint
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PdfChartRendererTest {

    @Test
    fun render_emptySeries_doesNotThrow() {
        val series = FeatureSeries(
            key = "cadence_steps_per_minute",
            displayName = "Cadence",
            unit = "steps per minute",
            points = emptyList()
        )
        val picture = Picture()
        val canvas = picture.beginRecording(400, 200)
        PdfChartRenderer().render(canvas, RectF(0f, 0f, 400f, 200f), series, axisLabel = "Cadence")
        picture.endRecording()
        assertTrue(picture.width == 400)
    }

    @Test
    fun render_threeHighBandPoints_emitsLineAndMarkers() {
        val series = FeatureSeries(
            key = "cadence_steps_per_minute",
            displayName = "Cadence",
            unit = "steps per minute",
            points = listOf(
                TimedPoint(1_700_000_000_000L, 110.0, QualityBand.HIGH),
                TimedPoint(1_700_604_800_000L, 112.0, QualityBand.HIGH),
                TimedPoint(1_701_209_600_000L, 109.0, QualityBand.HIGH)
            )
        )
        val picture = Picture()
        val canvas = picture.beginRecording(400, 200)
        PdfChartRenderer().render(canvas, RectF(0f, 0f, 400f, 200f), series, axisLabel = "Cadence")
        picture.endRecording()
        assertTrue(picture.width == 400)
    }
}
