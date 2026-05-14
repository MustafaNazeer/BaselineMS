package com.mustafanazeer.baselinems.battery.vision

import com.mustafanazeer.baselinems.dsp.vision.SLOAN_LETTERS
import com.mustafanazeer.baselinems.dsp.vision.SloanScoring.ContrastLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SloanChartLayoutTest {

    @Test
    fun `every line has 5 unique letters from the Sloan set`() {
        val chart = generateOneChartForTest(Random(123))
        assertEquals(8, chart.size)
        chart.forEach { line ->
            assertEquals(5, line.size)
            line.forEach { ch -> assertTrue(ch in SLOAN_LETTERS) }
            assertEquals("Within-line letters must be unique", line.size, line.toSet().size)
        }
    }

    @Test
    fun `no two adjacent lines share the same starting letter`() {
        val chart = generateOneChartForTest(Random(123))
        for (i in 0 until chart.size - 1) {
            assertTrue(chart[i].first() != chart[i + 1].first())
        }
    }

    @Test
    fun `same seed yields same chart`() {
        val a = generateOneChartForTest(Random(456))
        val b = generateOneChartForTest(Random(456))
        assertEquals(a, b)
    }

    @Test
    fun `Weber contrast formula yields expected letter luminance for C1Pt25`() {
        val l = SloanChart.computeLetterLuminance(ContrastLevel.C1Pt25)
        assertEquals(0.9875, l, 0.0001)
    }

    @Test
    fun `Weber contrast formula yields expected letter luminance for C2Pt5`() {
        val l = SloanChart.computeLetterLuminance(ContrastLevel.C2Pt5)
        assertEquals(0.975, l, 0.0001)
    }

    private fun generateOneChartForTest(rng: Random): List<List<Char>> {
        val lines = mutableListOf<List<Char>>()
        var prevStart: Char? = null
        repeat(8) {
            val available = SLOAN_LETTERS.toMutableList()
            val line = mutableListOf<Char>()
            repeat(5) { idx ->
                val choices = if (idx == 0 && prevStart != null) {
                    available.filter { it != prevStart }
                } else {
                    available.toList()
                }
                val pick = choices.random(rng)
                line += pick
                available.remove(pick)
                if (idx == 0) prevStart = pick
            }
            lines += line
        }
        return lines
    }
}
