package com.mustafan4x.msbattery.battery

import com.mustafan4x.msbattery.data.TestType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TestModuleTest {
    @Test
    fun `default TestResultPayload has null rawSensorRelativePath`() {
        val payload = object : TestResultPayload {
            override val qualityScore: Double = 1.0
            override val features: Map<String, Double> = emptyMap()
        }
        assertNull(payload.rawSensorRelativePath)
    }

    @Test
    fun `TestResultPayload can override rawSensorRelativePath`() {
        val payload = object : TestResultPayload {
            override val qualityScore: Double = 0.9
            override val features: Map<String, Double> = emptyMap()
            override val rawSensorRelativePath: String = "sensor_traces/abc/gait.csv.gz"
        }
        assertEquals("sensor_traces/abc/gait.csv.gz", payload.rawSensorRelativePath)
    }
}
