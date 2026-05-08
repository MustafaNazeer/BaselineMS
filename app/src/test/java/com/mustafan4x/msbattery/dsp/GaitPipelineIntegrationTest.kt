package com.mustafan4x.msbattery.dsp

import com.mustafan4x.msbattery.fixtures.PreCannedFixtures
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class GaitPipelineIntegrationTest {
    private val pipeline = GaitPipeline()

    @Test
    fun `healthy control normal recovers cadence within 3 percent and stride length within 2 percent`() {
        val samples = PreCannedFixtures.healthyControlNormal().generate().toList()
        val f = pipeline.process(samples)
        assertWithin(115.2, f.cadenceStepsPerMinute, 0.03)
        assertWithin(1.442, f.meanStrideLengthMeters, 0.02)
        assertTrue(f.qualityScore > 0.8)
    }

    @Test
    fun `MS typical normal recovers within the same envelope`() {
        val samples = PreCannedFixtures.msTypicalNormal().generate().toList()
        val f = pipeline.process(samples)
        assertWithin(94.4, f.cadenceStepsPerMinute, 0.03)
        assertWithin(0.906, f.meanStrideLengthMeters, 0.02)
        assertTrue(f.qualityScore > 0.7)
    }

    @Test
    fun `mild asymmetry recovers a positive asymmetry index of about 0_1`() {
        val samples = PreCannedFixtures.mildAsymmetry().generate().toList()
        val f = pipeline.process(samples)
        val expected = 0.10
        assertTrue("asymmetry $f.strideAsymmetryIndex", abs(f.strideAsymmetryIndex - expected) < 0.04)
    }

    @Test
    fun `noisy MS normal still recovers cadence within 5 percent and stride length within 4 percent`() {
        val samples = PreCannedFixtures.noisyMsNormal().generate().toList()
        val f = pipeline.process(samples)
        assertWithin(94.4, f.cadenceStepsPerMinute, 0.05)
        assertWithin(0.906, f.meanStrideLengthMeters, 0.04)
    }

    private fun assertWithin(expected: Double, actual: Double, fraction: Double) {
        val tolerance = expected * fraction
        assertTrue(
            "expected $expected within ${fraction * 100}%, got $actual (delta ${abs(actual - expected)}, tolerance $tolerance)",
            abs(actual - expected) <= tolerance
        )
    }
}
