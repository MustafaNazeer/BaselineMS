package com.mustafanazeer.baselinems.dsp.vision

import com.mustafanazeer.baselinems.dsp.vision.SloanScoring.ContrastLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class SloanScoringTest {

    @Test
    fun `all correct yields max score`() {
        val responses = ContrastLevel.values().flatMap { level ->
            (1..40).map { LetterResponse(shown = 'C', tapped = 'C', contrast = level) }
        }
        val score = SloanScoring.compute(responses)

        assertEquals(120, score.totalCorrect)
        assertEquals(40, score.correctAt100Pct)
        assertEquals(40, score.correctAt2Pt5Pct)
        assertEquals(40, score.correctAt1Pt25Pct)
    }

    @Test
    fun `all wrong yields zero`() {
        val responses = ContrastLevel.values().flatMap { level ->
            (1..40).map { LetterResponse(shown = 'C', tapped = 'D', contrast = level) }
        }
        val score = SloanScoring.compute(responses)

        assertEquals(0, score.totalCorrect)
        assertEquals(0, score.correctAt100Pct)
        assertEquals(0, score.correctAt1Pt25Pct)
    }

    @Test
    fun `skip counts as wrong`() {
        val responses = (1..40).map { LetterResponse(shown = 'H', tapped = null, contrast = ContrastLevel.C2Pt5) }
        val score = SloanScoring.compute(responses)

        assertEquals(0, score.correctAt2Pt5Pct)
        assertEquals(0, score.totalCorrect)
    }

    @Test
    fun `per-contrast counts add up to total`() {
        val responses = listOf(
            LetterResponse(shown = 'C', tapped = 'C', contrast = ContrastLevel.C100),
            LetterResponse(shown = 'C', tapped = 'D', contrast = ContrastLevel.C100),
            LetterResponse(shown = 'H', tapped = 'H', contrast = ContrastLevel.C2Pt5),
            LetterResponse(shown = 'K', tapped = null, contrast = ContrastLevel.C2Pt5),
            LetterResponse(shown = 'N', tapped = 'N', contrast = ContrastLevel.C1Pt25)
        )
        val score = SloanScoring.compute(responses)

        assertEquals(1, score.correctAt100Pct)
        assertEquals(1, score.correctAt2Pt5Pct)
        assertEquals(1, score.correctAt1Pt25Pct)
        assertEquals(3, score.totalCorrect)
    }

    @Test
    fun `case insensitive comparison`() {
        val responses = listOf(
            LetterResponse(shown = 'c', tapped = 'C', contrast = ContrastLevel.C100),
            LetterResponse(shown = 'H', tapped = 'h', contrast = ContrastLevel.C2Pt5)
        )
        val score = SloanScoring.compute(responses)

        assertEquals(2, score.totalCorrect)
    }
}
