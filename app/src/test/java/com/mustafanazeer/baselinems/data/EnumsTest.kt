package com.mustafanazeer.baselinems.data

import org.junit.Assert.assertEquals
import org.junit.Test

class EnumsTest {

    @Test
    fun testTypeHasFiveCases() {
        assertEquals(5, TestType.values().size)
    }

    @Test
    fun testTypeNames() {
        assertEquals("TAP", TestType.TAP.name)
        assertEquals("GAIT", TestType.GAIT.name)
        assertEquals("VISION", TestType.VISION.name)
        assertEquals("SDMT", TestType.SDMT.name)
        assertEquals("VOICE", TestType.VOICE.name)
    }

    @Test
    fun sexHasFourCases() { assertEquals(4, Sex.values().size) }

    @Test
    fun handHasThreeCases() { assertEquals(3, Hand.values().size) }

    @Test
    fun msTypeHasFiveCases() { assertEquals(5, MSType.values().size) }
}
