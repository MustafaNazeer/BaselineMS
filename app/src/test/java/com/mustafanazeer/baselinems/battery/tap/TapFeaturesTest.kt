package com.mustafanazeer.baselinems.battery.tap

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

    @Test
    fun asymmetryIndexZeroWhenSymmetric() {
        val even = (0..10).map { i -> valid(i * 200L, if (i % 2 == 0) TapSide.LEFT else TapSide.RIGHT) }
        val dom = TapRound(HandRole.DOMINANT, 30_000L, even)
        val nondom = TapRound(HandRole.NON_DOMINANT, 30_000L, even)
        val s = TapFeatures.computeSession(dom, nondom)
        assertEquals(0.0, s.asymmetryIndex, 0.0001)
    }

    @Test
    fun asymmetryIndexPositiveWhenDominantFaster() {
        val fast = (0..149).map { i -> valid(i * 200L, if (i % 2 == 0) TapSide.LEFT else TapSide.RIGHT) }
        val slow = (0..49).map { i -> valid(i * 600L, if (i % 2 == 0) TapSide.LEFT else TapSide.RIGHT) }
        val dom = TapRound(HandRole.DOMINANT, 30_000L, fast)
        val nondom = TapRound(HandRole.NON_DOMINANT, 30_000L, slow)
        val s = TapFeatures.computeSession(dom, nondom)
        assertEquals(true, s.asymmetryIndex > 0.0)
    }

    @Test
    fun missRateCombinesNonAlternatingAndOffTarget() {
        val dom = TapRound(
            HandRole.DOMINANT, 30_000L,
            events = listOf(
                valid(0L, TapSide.LEFT),
                valid(200L, TapSide.RIGHT),
                nonAlt(300L, TapSide.RIGHT)
            ),
            offTargetTaps = 1
        )
        val nondom = TapRound(
            HandRole.NON_DOMINANT, 30_000L,
            events = listOf(
                valid(0L, TapSide.LEFT),
                nonAlt(100L, TapSide.LEFT)
            ),
            offTargetTaps = 0
        )
        val s = TapFeatures.computeSession(dom, nondom)
        assertEquals(3.0 / 6.0, s.missRate, 0.0001)
    }

    @Test
    fun qualityZeroWhenEitherRoundUnderTenValidTaps() {
        val full = (0..40).map { i -> valid(i * 200L, if (i % 2 == 0) TapSide.LEFT else TapSide.RIGHT) }
        val sparse = (0..5).map { i -> valid(i * 200L, if (i % 2 == 0) TapSide.LEFT else TapSide.RIGHT) }
        val s = TapFeatures.computeSession(
            TapRound(HandRole.DOMINANT, 30_000L, full),
            TapRound(HandRole.NON_DOMINANT, 30_000L, sparse)
        )
        assertEquals(0.0, s.qualityScore, 0.0001)
    }

    @Test
    fun qualityHighOnHealthySession() {
        val full = (0..149).map { i -> valid(i * 200L, if (i % 2 == 0) TapSide.LEFT else TapSide.RIGHT) }
        val s = TapFeatures.computeSession(
            TapRound(HandRole.DOMINANT, 30_000L, full),
            TapRound(HandRole.NON_DOMINANT, 30_000L, full)
        )
        assertEquals(true, s.qualityScore > 0.9)
    }

    @Test
    fun featureMapHasAllExpectedKeys() {
        val full = (0..149).map { i -> valid(i * 200L, if (i % 2 == 0) TapSide.LEFT else TapSide.RIGHT) }
        val s = TapFeatures.computeSession(
            TapRound(HandRole.DOMINANT, 30_000L, full),
            TapRound(HandRole.NON_DOMINANT, 30_000L, full)
        )
        val map = s.toFeatureMap()
        val expected = setOf(
            "dominant_tap_rate_hz", "non_dominant_tap_rate_hz",
            "dominant_iti_cv", "non_dominant_iti_cv",
            "asymmetry_index", "miss_rate",
            "dominant_in_target_taps", "non_dominant_in_target_taps",
            "dominant_off_target_taps", "non_dominant_off_target_taps"
        )
        assertEquals(expected, map.keys)
    }
}
