package com.mustafanazeer.baselinems.dsp.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class PreCannedVoiceFixturesTest {

    @Test
    fun voicedClean_loadsExpectedSampleCountAndSensibleAmplitudeRange() {
        val fixture = PreCannedVoiceFixtures.voicedClean
        assertEquals(PreCannedVoiceFixtures.TOTAL_SAMPLES, fixture.samples.size)
        assertEquals(44100, fixture.sampleRateHz)
        assertEquals(30.0, fixture.durationSeconds, 0.0)

        val nonZero = fixture.samples.count { it != 0.toShort() }
        assertTrue(
            "voicedClean should not be all zeros (was $nonZero non zero samples)",
            nonZero > PreCannedVoiceFixtures.TOTAL_SAMPLES / 2,
        )

        val clippedHigh = fixture.samples.count { it == Short.MAX_VALUE }
        val clippedLow = fixture.samples.count { it == Short.MIN_VALUE }
        val clippedFraction =
            (clippedHigh + clippedLow).toDouble() / PreCannedVoiceFixtures.TOTAL_SAMPLES
        assertTrue(
            "voicedClean should not be clipped (clipped fraction was $clippedFraction)",
            clippedFraction < 0.001,
        )

        val peakAbs = fixture.samples.maxOf { kotlin.math.abs(it.toInt()) }
        assertTrue(
            "voicedClean peak absolute amplitude $peakAbs should be > 1000",
            peakAbs > 1000,
        )
    }

    @Test
    fun voicedPerturbed_loadsExpectedSampleCount() {
        val fixture = PreCannedVoiceFixtures.voicedPerturbed
        assertEquals(PreCannedVoiceFixtures.TOTAL_SAMPLES, fixture.samples.size)
        val nonZero = fixture.samples.count { it != 0.toShort() }
        assertTrue(
            "voicedPerturbed should not be all zeros",
            nonZero > PreCannedVoiceFixtures.TOTAL_SAMPLES / 2,
        )
    }

    @Test
    fun voicedNoisy_loadsExpectedSampleCount() {
        val fixture = PreCannedVoiceFixtures.voicedNoisy
        assertEquals(PreCannedVoiceFixtures.TOTAL_SAMPLES, fixture.samples.size)
        val nonZero = fixture.samples.count { it != 0.toShort() }
        assertTrue(
            "voicedNoisy should not be all zeros",
            nonZero > PreCannedVoiceFixtures.TOTAL_SAMPLES / 2,
        )
    }

    @Test
    fun pinkNoise_loadsExpectedSampleCountAndRmsMatchesCalibrationTarget() {
        val fixture = PreCannedVoiceFixtures.pinkNoise
        assertEquals(PreCannedVoiceFixtures.TOTAL_SAMPLES, fixture.samples.size)

        val rms = sqrt(
            fixture.samples
                .asSequence()
                .map { it.toDouble() }
                .map { it * it }
                .average(),
        )
        val targetRms = 0.05 * 32767.0
        val tolerance = targetRms * 0.10
        assertEquals(
            "pink_noise RMS should be close to the script's float RMS 0.05 scaled to int16",
            targetRms,
            rms,
            tolerance,
        )

        val clipped = fixture.samples.count {
            it == Short.MAX_VALUE || it == Short.MIN_VALUE
        }
        val clippedFraction = clipped.toDouble() / PreCannedVoiceFixtures.TOTAL_SAMPLES
        assertTrue(
            "pink_noise should not be clipped (clipped fraction was $clippedFraction)",
            clippedFraction < 0.001,
        )
    }

    @Test
    fun mixedVoicedUnvoiced_loadsExpectedSampleCountAndHasBothVoicedAndSilentRegions() {
        val fixture = PreCannedVoiceFixtures.mixedVoicedUnvoiced
        assertEquals(PreCannedVoiceFixtures.TOTAL_SAMPLES, fixture.samples.size)

        val firstSecond = fixture.samples.copyOfRange(0, 44100)
        val fourthSecond = fixture.samples.copyOfRange(3 * 44100, 4 * 44100)

        val firstSecondRms = sqrt(
            firstSecond.asSequence().map { it.toDouble() * it.toDouble() }.average(),
        )
        val fourthSecondRms = sqrt(
            fourthSecond.asSequence().map { it.toDouble() * it.toDouble() }.average(),
        )

        assertTrue(
            "First 1 s of mixed fixture (voiced block) should have RMS > 1000, was $firstSecondRms",
            firstSecondRms > 1000.0,
        )
        assertTrue(
            "4th second of mixed fixture (silence block) should have RMS < 100, was $fourthSecondRms",
            fourthSecondRms < 100.0,
        )
    }

    @Test
    fun all_fivefixtures_areLoadable() {
        assertEquals(5, PreCannedVoiceFixtures.all.size)
        PreCannedVoiceFixtures.all.forEach { fx ->
            assertNotNull("Fixture ${fx.resourceFileName} samples must not be null", fx.samples)
            assertEquals(
                "Fixture ${fx.resourceFileName} sample count",
                PreCannedVoiceFixtures.TOTAL_SAMPLES,
                fx.samples.size,
            )
            assertFalse(
                "Fixture ${fx.resourceFileName} resource file name should not be blank",
                fx.resourceFileName.isBlank(),
            )
        }
    }
}
