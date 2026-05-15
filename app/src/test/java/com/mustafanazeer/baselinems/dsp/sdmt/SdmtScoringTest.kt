package com.mustafanazeer.baselinems.dsp.sdmt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SdmtScoringTest {

    private val fixedKey: Map<SdmtSymbol, Int> = SdmtSymbol.entries.mapIndexed { idx, sym ->
        sym to (idx + 1)
    }.toMap()

    private fun correctDigit(sym: SdmtSymbol): Int = fixedKey.getValue(sym)

    @Test
    fun `all correct 30 responses produce qualityScore near 1`() {
        val responses = (0 until 30).map { i ->
            val sym = SdmtSymbol.entries[i % SdmtSymbol.entries.size]
            SdmtResponse(
                prompted = sym,
                tappedDigit = correctDigit(sym),
                promptShownAtMs = (i * 2_900L),
                tappedAtMs = (i * 2_900L) + 800L
            )
        }
        val score = SdmtScoring.compute(responses, fixedKey, testDurationMs = 90_000L)

        assertEquals(30, score.totalCorrect)
        assertEquals(30, score.totalAttempted)
        assertEquals(0, score.totalErrors)
        assertEquals(0.0, score.errorRate, 1e-9)
        assertTrue("quality should be near 1.0, was ${score.qualityScore}", score.qualityScore >= 0.99)
    }

    @Test
    fun `20 correct responses pass engagement floor`() {
        val responses = (0 until 20).map { i ->
            val sym = SdmtSymbol.entries[i % SdmtSymbol.entries.size]
            SdmtResponse(
                prompted = sym,
                tappedDigit = correctDigit(sym),
                promptShownAtMs = (i * 4_000L),
                tappedAtMs = (i * 4_000L) + 1_000L
            )
        }
        val score = SdmtScoring.compute(responses, fixedKey, testDurationMs = 90_000L)

        assertEquals(20, score.totalCorrect)
        assertTrue("20 correct should pass floor, was ${score.qualityScore}", score.qualityScore >= 0.8)
    }

    @Test
    fun `19 correct responses fail engagement floor`() {
        val responses = (0 until 19).map { i ->
            val sym = SdmtSymbol.entries[i % SdmtSymbol.entries.size]
            SdmtResponse(
                prompted = sym,
                tappedDigit = correctDigit(sym),
                promptShownAtMs = (i * 4_000L),
                tappedAtMs = (i * 4_000L) + 1_000L
            )
        }
        val score = SdmtScoring.compute(responses, fixedKey, testDurationMs = 90_000L)

        assertEquals(19, score.totalCorrect)
        assertTrue("19 correct should fail floor, was ${score.qualityScore}", score.qualityScore < 0.8)
    }

    @Test
    fun `6 correct responses drop qualityScore to zero per Pham 2021 absolute floor`() {
        val responses = (0 until 6).map { i ->
            val sym = SdmtSymbol.entries[i % SdmtSymbol.entries.size]
            SdmtResponse(
                prompted = sym,
                tappedDigit = correctDigit(sym),
                promptShownAtMs = (i * 10_000L),
                tappedAtMs = (i * 10_000L) + 1_500L
            )
        }
        val score = SdmtScoring.compute(responses, fixedKey, testDurationMs = 90_000L)

        assertEquals(6, score.totalCorrect)
        assertEquals(0.0, score.qualityScore, 1e-9)
    }

    @Test
    fun `single 16-second inter-response gap fails no-long-pause flag`() {
        val responses = mutableListOf<SdmtResponse>()
        var t = 0L
        repeat(15) { i ->
            val sym = SdmtSymbol.entries[i % SdmtSymbol.entries.size]
            responses += SdmtResponse(
                prompted = sym,
                tappedDigit = correctDigit(sym),
                promptShownAtMs = t,
                tappedAtMs = t + 800L
            )
            t += 1_500L
        }
        // 16 second gap to next response
        t += 16_000L
        repeat(15) { i ->
            val sym = SdmtSymbol.entries[i % SdmtSymbol.entries.size]
            responses += SdmtResponse(
                prompted = sym,
                tappedDigit = correctDigit(sym),
                promptShownAtMs = t,
                tappedAtMs = t + 800L
            )
            t += 1_500L
        }
        val score = SdmtScoring.compute(responses, fixedKey, testDurationMs = 90_000L)
        assertTrue("30 correct, should pass engagement floor on count", score.totalCorrect == 30)
        assertTrue("single 16 second gap should drop quality", score.qualityScore < 0.8)
    }

    @Test
    fun `last response at t=70s fails sustained-engagement flag`() {
        val responses = (0 until 25).map { i ->
            val t = (i * 2_500L) // last tap at i=24 -> t=60_000ms; tappedAt=60_800
            val sym = SdmtSymbol.entries[i % SdmtSymbol.entries.size]
            SdmtResponse(
                prompted = sym,
                tappedDigit = correctDigit(sym),
                promptShownAtMs = t,
                tappedAtMs = t + 800L
            )
        }
        // Confirm the last tappedAtMs is well below 75 seconds
        val score = SdmtScoring.compute(responses, fixedKey, testDurationMs = 90_000L)
        assertTrue("25 correct passes count floor", score.totalCorrect == 25)
        assertTrue("last tap before 75 s should drop quality, was ${score.qualityScore}", score.qualityScore < 0.8)
    }

    @Test
    fun `RT mean and SD are computed on correct responses only`() {
        val s1 = SdmtSymbol.SYMBOL_1
        val s2 = SdmtSymbol.SYMBOL_2
        val responses = listOf(
            SdmtResponse(prompted = s1, tappedDigit = correctDigit(s1), promptShownAtMs = 0L, tappedAtMs = 1_000L),
            SdmtResponse(prompted = s1, tappedDigit = correctDigit(s1), promptShownAtMs = 1_000L, tappedAtMs = 2_000L),
            SdmtResponse(prompted = s2, tappedDigit = correctDigit(s1), promptShownAtMs = 2_000L, tappedAtMs = 12_000L)
        )
        val score = SdmtScoring.compute(responses, fixedKey, testDurationMs = 90_000L)
        assertEquals(2, score.totalCorrect)
        assertEquals(1, score.totalErrors)
        assertEquals(1_000.0, score.responseTimeMeanMs, 1e-6)
        assertEquals(0.0, score.responseTimeSdMs, 1e-6)
        assertEquals(listOf(1_000L, 1_000L), score.responseTimes)
    }

    @Test
    fun `error rate is totalErrors divided by totalAttempted`() {
        val s1 = SdmtSymbol.SYMBOL_1
        val s2 = SdmtSymbol.SYMBOL_2
        val responses = listOf(
            SdmtResponse(prompted = s1, tappedDigit = correctDigit(s1), promptShownAtMs = 0L, tappedAtMs = 500L),
            SdmtResponse(prompted = s1, tappedDigit = correctDigit(s2), promptShownAtMs = 500L, tappedAtMs = 1_000L),
            SdmtResponse(prompted = s2, tappedDigit = correctDigit(s2), promptShownAtMs = 1_000L, tappedAtMs = 1_500L),
            SdmtResponse(prompted = s2, tappedDigit = correctDigit(s1), promptShownAtMs = 1_500L, tappedAtMs = 2_000L)
        )
        val score = SdmtScoring.compute(responses, fixedKey, testDurationMs = 90_000L)
        assertEquals(4, score.totalAttempted)
        assertEquals(2, score.totalCorrect)
        assertEquals(2, score.totalErrors)
        assertEquals(0.5, score.errorRate, 1e-9)
    }

    @Test
    fun `empty response list yields zeroed score and quality zero`() {
        val score = SdmtScoring.compute(emptyList(), fixedKey, testDurationMs = 90_000L)
        assertEquals(0, score.totalCorrect)
        assertEquals(0, score.totalAttempted)
        assertEquals(0, score.totalErrors)
        assertEquals(0.0, score.errorRate, 1e-9)
        assertEquals(0.0, score.qualityScore, 1e-9)
        assertTrue(score.responseTimes.isEmpty())
    }

    @Test
    fun `single error response yields finite NaN-free RT stats`() {
        val s1 = SdmtSymbol.SYMBOL_1
        val responses = listOf(
            SdmtResponse(prompted = s1, tappedDigit = correctDigit(SdmtSymbol.SYMBOL_2), promptShownAtMs = 0L, tappedAtMs = 1_000L)
        )
        val score = SdmtScoring.compute(responses, fixedKey, testDurationMs = 90_000L)
        assertEquals(0, score.totalCorrect)
        assertEquals(1, score.totalAttempted)
        assertEquals(1, score.totalErrors)
        assertFalse("mean RT should not be NaN", score.responseTimeMeanMs.isNaN())
        assertFalse("SD RT should not be NaN", score.responseTimeSdMs.isNaN())
    }
}
