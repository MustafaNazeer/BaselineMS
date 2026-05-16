package com.mustafanazeer.baselinems.dsp.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PraatPitchTest {

    private val sampleRateHz: Int = 44_100

    @Test
    fun analyze_voicedClean_detectsF0WithinTolerance() {
        val fixture = PreCannedVoiceFixtures.voicedClean
        val result = PraatPitch.analyze(fixture.samples, sampleRateHz)

        val voicedFrames = result.frames.filter { it.isVoiced }
        assertTrue(
            "voicedClean should produce many voiced frames; got ${voicedFrames.size}",
            voicedFrames.size > 100,
        )
        val f0Mean = voicedFrames.mapNotNull { it.f0Hz }.average()
        assertEquals(
            "voicedClean F0 mean should be within 1 Hz of 110.0",
            110.0,
            f0Mean,
            fixture.f0ToleranceHz,
        )
    }

    @Test
    fun analyze_voicedClean_isVoicedOnEveryFrame() {
        val fixture = PreCannedVoiceFixtures.voicedClean
        val result = PraatPitch.analyze(fixture.samples, sampleRateHz)
        val voicedFraction = result.frames.count { it.isVoiced }.toDouble() / result.frames.size
        assertTrue(
            "voicedClean voiced fraction should be approximately 1.0, was $voicedFraction",
            voicedFraction > 0.95,
        )
    }

    @Test
    fun analyze_voicedClean_periodCountIsConsistentWithF0AndDuration() {
        val fixture = PreCannedVoiceFixtures.voicedClean
        val result = PraatPitch.analyze(fixture.samples, sampleRateHz)
        val expected = 110.0 * 30.0
        val tolerance = expected * 0.03
        assertEquals(
            "voicedClean period count should be approximately 30 s * 110 Hz = 3300",
            expected,
            result.periods.size.toDouble(),
            tolerance,
        )
        assertEquals(
            "periods and periodPeakAmplitudes must align 1 to 1",
            result.periods.size,
            result.periodPeakAmplitudes.size,
        )
    }

    @Test
    fun analyze_voicedClean_autocorrelationPeakIsHighOnVoicedFrames() {
        val fixture = PreCannedVoiceFixtures.voicedClean
        val result = PraatPitch.analyze(fixture.samples, sampleRateHz)
        val meanPeak = result.frames.filter { it.isVoiced }
            .map { it.autocorrelationPeak }
            .average()
        assertTrue(
            "voicedClean mean autocorrelation peak should exceed 0.9, was $meanPeak",
            meanPeak > 0.9,
        )
    }

    @Test
    fun analyze_voicedPerturbed_detectsF0NearNominalAndF0SdInTolerance() {
        val fixture = PreCannedVoiceFixtures.voicedPerturbed
        val result = PraatPitch.analyze(fixture.samples, sampleRateHz)
        val f0List = result.frames.filter { it.isVoiced }.mapNotNull { it.f0Hz }
        assertTrue("voicedPerturbed should yield voiced frames", f0List.isNotEmpty())
        val mean = f0List.average()
        val sd = kotlin.math.sqrt(f0List.sumOf { (it - mean) * (it - mean) } / f0List.size)
        assertEquals(
            "voicedPerturbed F0 mean should be approximately 110 Hz",
            110.0,
            mean,
            fixture.f0ToleranceHz,
        )
        val expectedSd = fixture.expectedF0SdHz!!
        assertEquals(
            "voicedPerturbed F0 SD should be approximately ${expectedSd} Hz, was $sd",
            expectedSd,
            sd,
            fixture.f0SdToleranceHz,
        )
    }

    @Test
    fun analyze_voicedPerturbed_periodCountIsConsistentWithCycleStitching() {
        val fixture = PreCannedVoiceFixtures.voicedPerturbed
        val result = PraatPitch.analyze(fixture.samples, sampleRateHz)
        val expected = 110.0 * 30.0
        val tolerance = expected * 0.05
        assertEquals(
            "voicedPerturbed period count should be approximately 3300",
            expected,
            result.periods.size.toDouble(),
            tolerance,
        )
    }

    @Test
    fun analyze_pinkNoise_classifiesMostFramesAsUnvoiced() {
        val fixture = PreCannedVoiceFixtures.pinkNoise
        val result = PraatPitch.analyze(fixture.samples, sampleRateHz)
        val voicedFraction = result.frames.count { it.isVoiced }.toDouble() / result.frames.size
        assertTrue(
            "pinkNoise voiced fraction should be below 5 percent, was $voicedFraction",
            voicedFraction < 0.05,
        )
    }

    @Test
    fun analyze_mixedVoicedUnvoiced_voicedFractionIsApproximately50Percent() {
        val fixture = PreCannedVoiceFixtures.mixedVoicedUnvoiced
        val result = PraatPitch.analyze(fixture.samples, sampleRateHz)
        val voicedFraction = result.frames.count { it.isVoiced }.toDouble() / result.frames.size
        assertEquals(
            "mixedVoicedUnvoiced voiced fraction should be approximately 0.5",
            0.5,
            voicedFraction,
            0.10,
        )
    }

    @Test
    fun analyze_mixedVoicedUnvoiced_detectsF0InVoicedSegments() {
        val fixture = PreCannedVoiceFixtures.mixedVoicedUnvoiced
        val result = PraatPitch.analyze(fixture.samples, sampleRateHz)
        val f0List = result.frames.filter { it.isVoiced }.mapNotNull { it.f0Hz }
        assertTrue("mixedVoicedUnvoiced should yield voiced frames", f0List.isNotEmpty())
        val mean = f0List.average()
        assertEquals(
            "mixedVoicedUnvoiced F0 mean in voiced regions should be approximately 110 Hz",
            110.0,
            mean,
            fixture.f0ToleranceHz,
        )
    }

    @Test
    fun analyze_silence_yieldsAllUnvoicedAndEmptyPeriods() {
        val sampleCount = sampleRateHz / 2
        val silence = ShortArray(sampleCount)
        val result = PraatPitch.analyze(silence, sampleRateHz)
        val voicedFraction = if (result.frames.isEmpty()) 0.0 else {
            result.frames.count { it.isVoiced }.toDouble() / result.frames.size
        }
        assertEquals(
            "silence should classify every frame as unvoiced",
            0.0,
            voicedFraction,
            0.0,
        )
        assertEquals(
            "silence should produce no stitched periods",
            0,
            result.periods.size,
        )
    }

    @Test
    fun analyze_voicedNoisy_detectsF0NearNominal() {
        val fixture = PreCannedVoiceFixtures.voicedNoisy
        val result = PraatPitch.analyze(fixture.samples, sampleRateHz)
        val voiced = result.frames.filter { it.isVoiced }
        assertTrue("voicedNoisy should yield voiced frames", voiced.size > 100)
        val mean = voiced.mapNotNull { it.f0Hz }.average()
        assertEquals(
            "voicedNoisy F0 mean should be approximately 110 Hz",
            110.0,
            mean,
            fixture.f0ToleranceHz,
        )
    }

    @Test
    fun analyze_rejectsInvalidArguments() {
        val samples = ShortArray(44_100)
        var caught = 0
        try {
            PraatPitch.analyze(samples, sampleRateHz = -1)
        } catch (e: IllegalArgumentException) {
            caught++
        }
        try {
            PraatPitch.analyze(samples, voicingThreshold = -0.1)
        } catch (e: IllegalArgumentException) {
            caught++
        }
        try {
            PraatPitch.analyze(samples, f0MinHz = 400.0, f0MaxHz = 200.0)
        } catch (e: IllegalArgumentException) {
            caught++
        }
        assertEquals("All three invalid argument paths should throw", 3, caught)
    }

    @Test
    fun analyze_voicedClean_periodValuesAreNearNominalPeriod() {
        val fixture = PreCannedVoiceFixtures.voicedClean
        val result = PraatPitch.analyze(fixture.samples, sampleRateHz)
        assertTrue("expected non zero periods", result.periods.isNotEmpty())
        val nominal = 1.0 / 110.0
        val firstFew = result.periods.copyOfRange(0, minOf(20, result.periods.size))
        firstFew.forEach { p ->
            assertTrue(
                "voicedClean cycle period $p should be within 10 percent of $nominal",
                abs(p - nominal) < 0.10 * nominal,
            )
        }
        assertNotNull("first frame should be present", result.frames.firstOrNull())
    }
}
