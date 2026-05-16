package com.mustafanazeer.baselinems.dsp.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class VoiceFeaturesTest {

    private val sampleRateHz: Int = 44_100
    private val passageWordCount: Int = 132

    @Test
    fun compute_voicedClean_jitterApproachesZero() {
        val fixture = PreCannedVoiceFixtures.voicedClean
        val pitch = PraatPitch.analyze(fixture.samples, sampleRateHz)
        val features = VoiceFeatures.compute(pitch, sampleRateHz, passageWordCount)
        assertNotNull("jitter should be defined on voicedClean", features.jitterLocal)
        assertEquals(
            "voicedClean jitter local should be approximately 0.0",
            0.0,
            features.jitterLocal!!,
            fixture.jitterTolerance,
        )
    }

    @Test
    fun compute_voicedClean_shimmerApproachesZero() {
        val fixture = PreCannedVoiceFixtures.voicedClean
        val pitch = PraatPitch.analyze(fixture.samples, sampleRateHz)
        val features = VoiceFeatures.compute(pitch, sampleRateHz, passageWordCount)
        assertNotNull("shimmer should be defined on voicedClean", features.shimmerLocal)
        assertEquals(
            "voicedClean shimmer local should be approximately 0.0",
            0.0,
            features.shimmerLocal!!,
            fixture.shimmerTolerance,
        )
    }

    @Test
    fun compute_voicedClean_hnrIsAboveFloor() {
        val fixture = PreCannedVoiceFixtures.voicedClean
        val pitch = PraatPitch.analyze(fixture.samples, sampleRateHz)
        val features = VoiceFeatures.compute(pitch, sampleRateHz, passageWordCount)
        assertNotNull("HNR should be defined on voicedClean", features.hnrDb)
        val floor = fixture.expectedHnrDbMin ?: error("voicedClean must declare HNR floor")
        assertTrue(
            "voicedClean HNR ${features.hnrDb} should exceed floor $floor dB",
            features.hnrDb!! > floor,
        )
    }

    @Test
    fun compute_voicedClean_f0MeanAndSd() {
        val fixture = PreCannedVoiceFixtures.voicedClean
        val pitch = PraatPitch.analyze(fixture.samples, sampleRateHz)
        val features = VoiceFeatures.compute(pitch, sampleRateHz, passageWordCount)
        assertEquals(
            "voicedClean F0 mean should be approximately 110 Hz",
            110.0,
            features.f0MeanHz!!,
            fixture.f0ToleranceHz,
        )
        assertEquals(
            "voicedClean F0 SD should be approximately 0.0 Hz",
            0.0,
            features.f0SdHz!!,
            fixture.f0SdToleranceHz,
        )
    }

    @Test
    fun compute_voicedClean_pauseFractionIsApproximatelyZero() {
        val fixture = PreCannedVoiceFixtures.voicedClean
        val pitch = PraatPitch.analyze(fixture.samples, sampleRateHz)
        val features = VoiceFeatures.compute(pitch, sampleRateHz, passageWordCount)
        assertEquals(
            "voicedClean pause fraction should be approximately 0.0",
            0.0,
            features.pauseFraction,
            fixture.pauseFractionTolerance,
        )
    }

    @Test
    fun compute_voicedPerturbed_jitterMatchesAnalyticGroundTruth() {
        val fixture = PreCannedVoiceFixtures.voicedPerturbed
        val pitch = PraatPitch.analyze(fixture.samples, sampleRateHz)
        val features = VoiceFeatures.compute(pitch, sampleRateHz, passageWordCount)
        val expected = fixture.expectedJitterLocal!!
        assertNotNull("jitter should be defined on voicedPerturbed", features.jitterLocal)
        assertEquals(
            "voicedPerturbed jitter local should be approximately $expected (was ${features.jitterLocal})",
            expected,
            features.jitterLocal!!,
            fixture.jitterTolerance,
        )
    }

    @Test
    fun compute_voicedPerturbed_shimmerMatchesAnalyticGroundTruth() {
        val fixture = PreCannedVoiceFixtures.voicedPerturbed
        val pitch = PraatPitch.analyze(fixture.samples, sampleRateHz)
        val features = VoiceFeatures.compute(pitch, sampleRateHz, passageWordCount)
        val expected = fixture.expectedShimmerLocal!!
        assertNotNull("shimmer should be defined on voicedPerturbed", features.shimmerLocal)
        assertEquals(
            "voicedPerturbed shimmer local should be approximately $expected (was ${features.shimmerLocal})",
            expected,
            features.shimmerLocal!!,
            fixture.shimmerTolerance,
        )
    }

    @Test
    fun compute_voicedPerturbed_f0SdMatchesRealizedGroundTruth() {
        val fixture = PreCannedVoiceFixtures.voicedPerturbed
        val pitch = PraatPitch.analyze(fixture.samples, sampleRateHz)
        val features = VoiceFeatures.compute(pitch, sampleRateHz, passageWordCount)
        val expected = fixture.expectedF0SdHz!!
        assertEquals(
            "voicedPerturbed F0 SD should be approximately $expected Hz",
            expected,
            features.f0SdHz!!,
            fixture.f0SdToleranceHz,
        )
    }

    @Test
    fun compute_voicedNoisy_hnrIsNearCalibratedSnr() {
        val fixture = PreCannedVoiceFixtures.voicedNoisy
        val pitch = PraatPitch.analyze(fixture.samples, sampleRateHz)
        val features = VoiceFeatures.compute(pitch, sampleRateHz, passageWordCount)
        val expected = fixture.expectedHnrDbCenter!!
        assertNotNull("HNR should be defined on voicedNoisy", features.hnrDb)
        assertEquals(
            "voicedNoisy HNR should be approximately $expected dB (was ${features.hnrDb})",
            expected,
            features.hnrDb!!,
            fixture.hnrToleranceDb,
        )
    }

    @Test
    fun compute_voicedNoisy_f0MeanIsNearNominal() {
        val fixture = PreCannedVoiceFixtures.voicedNoisy
        val pitch = PraatPitch.analyze(fixture.samples, sampleRateHz)
        val features = VoiceFeatures.compute(pitch, sampleRateHz, passageWordCount)
        assertEquals(
            "voicedNoisy F0 mean should be approximately 110 Hz",
            110.0,
            features.f0MeanHz!!,
            fixture.f0ToleranceHz,
        )
    }

    @Test
    fun compute_pinkNoise_perturbationFeaturesAreNull() {
        val fixture = PreCannedVoiceFixtures.pinkNoise
        val pitch = PraatPitch.analyze(fixture.samples, sampleRateHz)
        val features = VoiceFeatures.compute(pitch, sampleRateHz, passageWordCount)
        assertNull("pinkNoise jitter should be null", features.jitterLocal)
        assertNull("pinkNoise shimmer should be null", features.shimmerLocal)
        assertNull("pinkNoise HNR should be null", features.hnrDb)
        assertNull("pinkNoise F0 mean should be null", features.f0MeanHz)
        assertNull("pinkNoise F0 SD should be null", features.f0SdHz)
    }

    @Test
    fun compute_pinkNoise_pauseFractionIsApproximatelyOne() {
        val fixture = PreCannedVoiceFixtures.pinkNoise
        val pitch = PraatPitch.analyze(fixture.samples, sampleRateHz)
        val features = VoiceFeatures.compute(pitch, sampleRateHz, passageWordCount)
        assertEquals(
            "pinkNoise pause fraction should be approximately 1.0",
            1.0,
            features.pauseFraction,
            fixture.pauseFractionTolerance,
        )
    }

    @Test
    fun compute_mixedVoicedUnvoiced_pauseFractionIsApproximatelyHalf() {
        val fixture = PreCannedVoiceFixtures.mixedVoicedUnvoiced
        val pitch = PraatPitch.analyze(fixture.samples, sampleRateHz)
        val features = VoiceFeatures.compute(pitch, sampleRateHz, passageWordCount)
        assertEquals(
            "mixedVoicedUnvoiced pause fraction should be approximately 0.5 (was ${features.pauseFraction})",
            0.5,
            features.pauseFraction,
            0.10,
        )
    }

    @Test
    fun compute_mixedVoicedUnvoiced_f0MeanIsNearNominal() {
        val fixture = PreCannedVoiceFixtures.mixedVoicedUnvoiced
        val pitch = PraatPitch.analyze(fixture.samples, sampleRateHz)
        val features = VoiceFeatures.compute(pitch, sampleRateHz, passageWordCount)
        assertEquals(
            "mixedVoicedUnvoiced F0 mean should be approximately 110 Hz",
            110.0,
            features.f0MeanHz!!,
            fixture.f0ToleranceHz,
        )
    }

    @Test
    fun compute_mixedVoicedUnvoiced_speakingRateMatchesAnalyticGroundTruth() {
        val fixture = PreCannedVoiceFixtures.mixedVoicedUnvoiced
        val pitch = PraatPitch.analyze(fixture.samples, sampleRateHz)
        val features = VoiceFeatures.compute(pitch, sampleRateHz, passageWordCount)
        val expected = fixture.expectedSpeakingRateWpm!!
        assertNotNull(
            "speaking rate should be defined on mixedVoicedUnvoiced",
            features.speakingRateWpm,
        )
        val tolerance = expected * 0.05
        assertEquals(
            "mixedVoicedUnvoiced speaking rate should be approximately $expected WPM",
            expected,
            features.speakingRateWpm!!,
            tolerance,
        )
    }

    @Test
    fun compute_emptyPitchResult_returnsAllNullPerturbationAndPauseFractionOne() {
        val empty = PraatPitch.Result(emptyList(), DoubleArray(0), DoubleArray(0))
        val features = VoiceFeatures.compute(empty, sampleRateHz, passageWordCount)
        assertNull(features.jitterLocal)
        assertNull(features.shimmerLocal)
        assertNull(features.hnrDb)
        assertNull(features.f0MeanHz)
        assertNull(features.f0SdHz)
        assertNull(features.speakingRateWpm)
        assertEquals(1.0, features.pauseFraction, 0.0)
        assertEquals(0, features.periodCount)
    }

    @Test
    fun compute_voicedClean_speakingRateUsesVoicedSecondsDenominator() {
        val fixture = PreCannedVoiceFixtures.voicedClean
        val pitch = PraatPitch.analyze(fixture.samples, sampleRateHz)
        val features = VoiceFeatures.compute(pitch, sampleRateHz, passageWordCount)
        assertNotNull("speaking rate should be defined when voiced", features.speakingRateWpm)
        val expected = passageWordCount / features.voicedSeconds * 60.0
        assertEquals(
            "speaking rate must divide by voicedSeconds, not totalSeconds",
            expected,
            features.speakingRateWpm!!,
            1.0,
        )
    }

    @Test
    fun compute_rejectsNegativeSampleRate() {
        val pitch = PraatPitch.Result(emptyList(), DoubleArray(0), DoubleArray(0))
        var threw = false
        try {
            VoiceFeatures.compute(pitch, -1, passageWordCount)
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue("negative sample rate should throw", threw)
    }

    @Test
    fun compute_speakingRateIsNullWhenPassageWordCountIsZero() {
        val fixture = PreCannedVoiceFixtures.voicedClean
        val pitch = PraatPitch.analyze(fixture.samples, sampleRateHz)
        val features = VoiceFeatures.compute(pitch, sampleRateHz, passageWordCount = 0)
        assertNull(
            "speaking rate should be null when passage word count is zero",
            features.speakingRateWpm,
        )
    }

    @Test
    fun compute_voicedPerturbed_periodCountAboveFiftyForPerturbationFeatures() {
        val fixture = PreCannedVoiceFixtures.voicedPerturbed
        val pitch = PraatPitch.analyze(fixture.samples, sampleRateHz)
        val features = VoiceFeatures.compute(pitch, sampleRateHz, passageWordCount)
        assertTrue(
            "voicedPerturbed period count ${features.periodCount} must be at least 50 for perturbation features",
            features.periodCount >= 50,
        )
        assertNotNull(features.jitterLocal)
        assertNotNull(features.shimmerLocal)
    }

    @Test
    fun compute_jitterAndShimmerWithinAnalyticToleranceOverlap() {
        val fixture = PreCannedVoiceFixtures.voicedPerturbed
        val pitch = PraatPitch.analyze(fixture.samples, sampleRateHz)
        val features = VoiceFeatures.compute(pitch, sampleRateHz, passageWordCount)
        val expectedJitter = fixture.expectedJitterLocal!!
        val expectedShimmer = fixture.expectedShimmerLocal!!
        assertTrue(
            "jitter should be non negative",
            features.jitterLocal!! >= 0.0,
        )
        assertTrue(
            "shimmer should be non negative",
            features.shimmerLocal!! >= 0.0,
        )
        assertTrue(
            "jitter should lie within tolerance of analytic ground truth",
            abs(features.jitterLocal!! - expectedJitter) <= fixture.jitterTolerance,
        )
        assertTrue(
            "shimmer should lie within tolerance of analytic ground truth",
            abs(features.shimmerLocal!! - expectedShimmer) <= fixture.shimmerTolerance,
        )
    }

    private fun featureSet(
        jitter: Double?,
        shimmer: Double?,
    ): VoiceFeatureSet {
        return VoiceFeatureSet(
            jitterLocal = jitter,
            shimmerLocal = shimmer,
            hnrDb = 20.0,
            f0MeanHz = 130.0,
            f0SdHz = 1.5,
            speakingRateWpm = 120.0,
            pauseFraction = 0.1,
            voicedSeconds = 28.0,
            totalSeconds = 30.0,
            periodCount = 3000,
        )
    }

    @Test
    fun steadinessBand_belowSteadyCutoffsReturnsSteady() {
        val band = VoiceFeatures.steadinessBand(featureSet(jitter = 0.005, shimmer = 0.020))
        assertEquals(VoiceSteadinessBand.STEADY, band)
    }

    @Test
    fun steadinessBand_atSteadyCutoffsReturnsMostlySteady() {
        val band = VoiceFeatures.steadinessBand(featureSet(jitter = 0.010, shimmer = 0.040))
        assertEquals(VoiceSteadinessBand.MOSTLY_STEADY, band)
    }

    @Test
    fun steadinessBand_betweenCutoffsReturnsMostlySteady() {
        val band = VoiceFeatures.steadinessBand(featureSet(jitter = 0.015, shimmer = 0.055))
        assertEquals(VoiceSteadinessBand.MOSTLY_STEADY, band)
    }

    @Test
    fun steadinessBand_aboveMostlySteadyCutoffsReturnsVaried() {
        val band = VoiceFeatures.steadinessBand(featureSet(jitter = 0.025, shimmer = 0.080))
        assertEquals(VoiceSteadinessBand.VARIED, band)
    }

    @Test
    fun steadinessBand_jitterNullReturnsUnmeasurable() {
        val band = VoiceFeatures.steadinessBand(featureSet(jitter = null, shimmer = 0.040))
        assertEquals(VoiceSteadinessBand.UNMEASURABLE, band)
    }

    @Test
    fun steadinessBand_shimmerNullReturnsUnmeasurable() {
        val band = VoiceFeatures.steadinessBand(featureSet(jitter = 0.010, shimmer = null))
        assertEquals(VoiceSteadinessBand.UNMEASURABLE, band)
    }
}
