package com.mustafan4x.msbattery.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun testTypeRoundTrip() {
        assertEquals(TestType.GAIT, converters.toTestType(converters.fromTestType(TestType.GAIT)))
    }

    @Test
    fun sexRoundTrip() {
        assertEquals(Sex.FEMALE, converters.toSex(converters.fromSex(Sex.FEMALE)))
    }

    @Test
    fun handRoundTrip() {
        assertEquals(Hand.LEFT, converters.toHand(converters.fromHand(Hand.LEFT)))
    }

    @Test
    fun msTypeRoundTrip() {
        assertEquals(MSType.RRMS, converters.toMSType(converters.fromMSType(MSType.RRMS)))
    }

    @Test
    fun nullEnumRoundTrip() {
        assertNull(converters.toTestType(null))
    }
}
