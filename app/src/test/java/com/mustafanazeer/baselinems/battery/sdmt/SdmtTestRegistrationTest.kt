package com.mustafanazeer.baselinems.battery.sdmt

import com.mustafanazeer.baselinems.data.TestType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SdmtTestRegistrationTest {

    @Test
    fun `testType is SDMT`() {
        val module = SdmtTest()
        assertEquals(TestType.SDMT, module.testType)
    }

    @Test
    fun `display name pairs the full SDMT name with the smartphone variant framing`() {
        val module = SdmtTest()
        assertTrue(
            "display name should reference SDMT: ${module.displayName}",
            module.displayName.contains("Symbol Digit Modalities Test") &&
                module.displayName.contains("smartphone variant")
        )
    }

    @Test
    fun `estimated duration is 110 seconds (90 plus 20 for chrome)`() {
        val module = SdmtTest()
        assertEquals(110, module.estimatedDurationSeconds)
    }
}
