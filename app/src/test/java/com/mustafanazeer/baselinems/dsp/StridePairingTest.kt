package com.mustafanazeer.baselinems.dsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StridePairingTest {
    @Test
    fun `assigns alternating foot labels when lateral sign alternates`() {
        val steps = listOf(
            StepEvent(timeSeconds = 0.5, amplitude = 5.0, sampleIndex = 50, gapToPreviousExceededMaxInterval = false),
            StepEvent(timeSeconds = 1.0, amplitude = 5.0, sampleIndex = 100, gapToPreviousExceededMaxInterval = false),
            StepEvent(timeSeconds = 1.5, amplitude = 5.0, sampleIndex = 150, gapToPreviousExceededMaxInterval = false),
            StepEvent(timeSeconds = 2.0, amplitude = 5.0, sampleIndex = 200, gapToPreviousExceededMaxInterval = false)
        )
        val lateralAtStep = doubleArrayOf(0.5, -0.5, 0.5, -0.5)
        val labelled = StridePairing().assignFeet(steps, lateralAtStep)
        assertEquals(4, labelled.size)
        assertNotEquals(labelled[0].foot, labelled[1].foot)
        assertNotEquals(labelled[1].foot, labelled[2].foot)
        assertNotEquals(labelled[2].foot, labelled[3].foot)
    }

    @Test
    fun `same lateral sign produces same foot label`() {
        val steps = listOf(
            StepEvent(timeSeconds = 0.5, amplitude = 5.0, sampleIndex = 50, gapToPreviousExceededMaxInterval = false),
            StepEvent(timeSeconds = 1.0, amplitude = 5.0, sampleIndex = 100, gapToPreviousExceededMaxInterval = false)
        )
        val lateralAtStep = doubleArrayOf(0.5, 0.7)
        val labelled = StridePairing().assignFeet(steps, lateralAtStep)
        assertEquals(labelled[0].foot, labelled[1].foot)
    }

    @Test
    fun `groups successive same foot steps into strides`() {
        val labelled = listOf(
            FootStep(StepEvent(timeSeconds = 0.5, amplitude = 5.0, sampleIndex = 50, gapToPreviousExceededMaxInterval = false), Foot.LEFT),
            FootStep(StepEvent(timeSeconds = 1.0, amplitude = 5.0, sampleIndex = 100, gapToPreviousExceededMaxInterval = false), Foot.RIGHT),
            FootStep(StepEvent(timeSeconds = 1.5, amplitude = 5.0, sampleIndex = 150, gapToPreviousExceededMaxInterval = false), Foot.LEFT),
            FootStep(StepEvent(timeSeconds = 2.0, amplitude = 5.0, sampleIndex = 200, gapToPreviousExceededMaxInterval = false), Foot.RIGHT),
            FootStep(StepEvent(timeSeconds = 2.5, amplitude = 5.0, sampleIndex = 250, gapToPreviousExceededMaxInterval = false), Foot.LEFT)
        )
        val strides = StridePairing().pairStrides(labelled)
        assertEquals(3, strides.size)
        assertEquals(Foot.LEFT, strides[0].foot)
        assertEquals(0.5, strides[0].startTimeSeconds, 1e-9)
        assertEquals(1.5, strides[0].endTimeSeconds, 1e-9)
        assertEquals(Foot.RIGHT, strides[1].foot)
        assertEquals(1.0, strides[1].startTimeSeconds, 1e-9)
        assertEquals(2.0, strides[1].endTimeSeconds, 1e-9)
        assertEquals(Foot.LEFT, strides[2].foot)
        assertEquals(1.5, strides[2].startTimeSeconds, 1e-9)
        assertEquals(2.5, strides[2].endTimeSeconds, 1e-9)
    }

    @Test
    fun `assignFeet output length matches input length`() {
        val steps = (0 until 10).map {
            StepEvent(timeSeconds = it * 0.5, amplitude = 5.0, sampleIndex = it * 50, gapToPreviousExceededMaxInterval = false)
        }
        val lateralAtStep = DoubleArray(10) { if (it % 2 == 0) 0.5 else -0.5 }
        val labelled = StridePairing().assignFeet(steps, lateralAtStep)
        assertTrue(labelled.size == steps.size)
    }
}
