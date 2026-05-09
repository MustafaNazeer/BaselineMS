package com.mustafan4x.baselinems.battery.tap

import com.mustafan4x.baselinems.data.TestType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BilateralTapTestMetadataTest {

    @Test
    fun moduleMetadata() {
        val module = BilateralTapTest()
        assertEquals(TestType.TAP, module.testType)
        assertEquals("Bilateral Tap", module.displayName)
        assertTrue(module.instructions.isNotEmpty())
        assertTrue(module.instructions.length > 30)
        assertEquals(70, module.estimatedDurationSeconds)
    }
}
