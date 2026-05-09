package com.mustafanazeer.baselinems.battery

import com.mustafanazeer.baselinems.data.TestType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockTestModuleTest {

    @Test
    fun mockResultEncodesToJson() {
        val payload = MockTestModule.MockResult(qualityScore = 0.75, features = mapOf("fake_metric" to 1.5))
        assertEquals(0.75, payload.qualityScore, 0.0001)
        val json = payload.featuresAsJson()
        assertTrue(json.contains("fake_metric"))
        assertTrue(json.contains("1.5"))
    }

    @Test
    fun moduleMetadata() {
        val module = MockTestModule()
        assertEquals(TestType.TAP, module.testType)
        assertEquals(1, module.estimatedDurationSeconds)
        assertTrue(module.displayName.isNotEmpty())
        assertTrue(module.instructions.isNotEmpty())
    }
}
