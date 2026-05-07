package com.mustafan4x.msbattery.battery.tap

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.abs

class TapFeaturesTest {

    private fun valid(t: Long, side: TapSide) = TapEvent(t, side, TapKind.VALID)
    private fun nonAlt(t: Long, side: TapSide) = TapEvent(t, side, TapKind.NON_ALTERNATING)

    @Test
    fun tapRateMetronomic200msIs5Hz() {
        val events = (0..149 step 1).map { i ->
            valid(i * 200L, if (i % 2 == 0) TapSide.LEFT else TapSide.RIGHT)
        }
        val round = TapRound(HandRole.DOMINANT, durationMs = 30_000L, events = events)
        val r = TapFeatures.computeRound(round)
        assertEquals(5.0, r.tapRateHz, 0.01)
    }

    @Test
    fun tapRateIgnoresNonAlternatingAndOffTarget() {
        val events = listOf(
            valid(0L, TapSide.LEFT),
            valid(200L, TapSide.RIGHT),
            nonAlt(300L, TapSide.RIGHT)
        )
        val round = TapRound(HandRole.DOMINANT, durationMs = 30_000L, events = events, offTargetTaps = 5)
        val r = TapFeatures.computeRound(round)
        assertEquals(2.0 / 30.0, r.tapRateHz, 0.0001)
    }

    @Test
    fun itiCvMetronomicIsZero() {
        val events = (0..10).map { i -> valid(i * 200L, if (i % 2 == 0) TapSide.LEFT else TapSide.RIGHT) }
        val round = TapRound(HandRole.DOMINANT, durationMs = 30_000L, events = events)
        val r = TapFeatures.computeRound(round)
        assertEquals(0.0, r.itiCv, 0.0001)
    }

    @Test
    fun itiCvVariableIsPositiveAndCorrect() {
        val timestamps = listOf(0L, 100L, 300L, 600L, 1000L)
        val events = timestamps.mapIndexed { i, t -> valid(t, if (i % 2 == 0) TapSide.LEFT else TapSide.RIGHT) }
        val round = TapRound(HandRole.DOMINANT, durationMs = 30_000L, events = events)
        val r = TapFeatures.computeRound(round)
        val itis = listOf(100.0, 200.0, 300.0, 400.0)
        val mean = itis.average()
        val variance = itis.map { (it - mean) * (it - mean) }.average()
        val expected = Math.sqrt(variance) / mean
        assertEquals(true, abs(r.itiCv - expected) < 0.0001)
    }

    @Test
    fun roundWithFewerThanTwoValidTapsHasZeroItiCv() {
        val round = TapRound(HandRole.DOMINANT, durationMs = 30_000L, events = listOf(valid(0L, TapSide.LEFT)))
        val r = TapFeatures.computeRound(round)
        assertEquals(0.0, r.itiCv, 0.0001)
        assertEquals(1.0 / 30.0, r.tapRateHz, 0.0001)
    }

    @Test
    fun missCountsAreSurfaced() {
        val events = listOf(
            valid(0L, TapSide.LEFT),
            valid(100L, TapSide.RIGHT),
            nonAlt(150L, TapSide.RIGHT),
            valid(200L, TapSide.LEFT)
        )
        val round = TapRound(HandRole.DOMINANT, durationMs = 30_000L, events = events, offTargetTaps = 2)
        val r = TapFeatures.computeRound(round)
        assertEquals(3, r.validTaps)
        assertEquals(1, r.nonAlternatingTaps)
        assertEquals(2, r.offTargetTaps)
    }
}
