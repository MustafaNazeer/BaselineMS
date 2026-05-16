package com.mustafanazeer.baselinems.dsp.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceQualityTest {

    private val sampleRateHz: Int = 44_100
    private val passageWordCount: Int = 132

    private fun featuresFor(fixture: VoiceFixture): VoiceFeatureSet {
        val pitch = PraatPitch.analyze(fixture.samples, sampleRateHz)
        return VoiceFeatures.compute(pitch, sampleRateHz, passageWordCount)
    }

    @Test
    fun score_voicedClean_qualityScoreIsOneAndAllFlagsOk() {
        val fixture = PreCannedVoiceFixtures.voicedClean
        val features = featuresFor(fixture)
        val q = VoiceQuality.score(features, fixture.samples, sampleRateHz)
        assertEquals("voicedClean quality should be 1.0", 1.0, q.qualityScore, 0.01)
        assertTrue(q.engagementOk)
        assertFalse(q.clippingDetected)
        assertTrue(q.snrAdequate)
        q.perFeatureFlags.forEach { (key, flag) ->
            assertEquals("$key flag should be OK on voicedClean", FeatureQualityFlag.OK, flag)
        }
    }

    @Test
    fun score_voicedPerturbed_qualityScoreIsOneAndAllFlagsOk() {
        val fixture = PreCannedVoiceFixtures.voicedPerturbed
        val features = featuresFor(fixture)
        val q = VoiceQuality.score(features, fixture.samples, sampleRateHz)
        assertEquals("voicedPerturbed quality should be 1.0", 1.0, q.qualityScore, 0.01)
        q.perFeatureFlags.forEach { (key, flag) ->
            assertEquals("$key flag should be OK on voicedPerturbed", FeatureQualityFlag.OK, flag)
        }
    }

    @Test
    fun score_voicedNoisy_qualityScoreIsOneAndAllFlagsOk() {
        val fixture = PreCannedVoiceFixtures.voicedNoisy
        val features = featuresFor(fixture)
        val q = VoiceQuality.score(features, fixture.samples, sampleRateHz)
        assertEquals("voicedNoisy quality should be 1.0", 1.0, q.qualityScore, 0.01)
        q.perFeatureFlags.forEach { (key, flag) ->
            assertEquals("$key flag should be OK on voicedNoisy", FeatureQualityFlag.OK, flag)
        }
    }

    @Test
    fun score_pinkNoise_engagementFailsAndQualityIsZero() {
        val fixture = PreCannedVoiceFixtures.pinkNoise
        val features = featuresFor(fixture)
        val q = VoiceQuality.score(features, fixture.samples, sampleRateHz)
        assertFalse("pinkNoise engagement should fail", q.engagementOk)
        assertEquals("pinkNoise quality should be 0.0", 0.0, q.qualityScore, 0.0)
        assertFalse("pinkNoise SNR should not be adequate", q.snrAdequate)
    }

    @Test
    fun score_mixedVoicedUnvoiced_engagementOkAtFloorAndSpeakingRateFlagOk() {
        val fixture = PreCannedVoiceFixtures.mixedVoicedUnvoiced
        val features = featuresFor(fixture)
        // The fixture is analytically 15.0 seconds voiced (5 voiced blocks of 3 s each); the
        // autocorrelation detector with 40 ms windows discretizes the boundary and realizes a
        // voicedSeconds within roughly 0.1 s of 15.0. The quality scorer applies a small
        // frame width tolerance to the floor so this boundary case is accepted as engagement ok.
        val q = VoiceQuality.score(features, fixture.samples, sampleRateHz)
        assertTrue(
            "mixedVoicedUnvoiced engagement should be ok at the 15 s floor (voiced=${features.voicedSeconds})",
            q.engagementOk,
        )
        assertEquals(
            "speaking rate flag should be OK",
            FeatureQualityFlag.OK,
            q.perFeatureFlags[VoiceQuality.FEATURE_KEY_SPEAKING_RATE],
        )
    }

    @Test
    fun score_clippedSamples_dropsQualityToZeroAndFlagsAllAsClipped() {
        val clipped = ShortArray(sampleRateHz) { Short.MAX_VALUE }
        val features = VoiceFeatureSet(
            jitterLocal = 0.01,
            shimmerLocal = 0.1,
            hnrDb = 20.0,
            f0MeanHz = 110.0,
            f0SdHz = 1.0,
            speakingRateWpm = 200.0,
            pauseFraction = 0.0,
            voicedSeconds = 25.0,
            totalSeconds = 30.0,
            periodCount = 2500,
        )
        val q = VoiceQuality.score(features, clipped, sampleRateHz)
        assertTrue("clipping should be detected", q.clippingDetected)
        assertEquals("clipped recording quality should be 0.0", 0.0, q.qualityScore, 0.0)
        q.perFeatureFlags.forEach { (key, flag) ->
            assertEquals("$key flag should be CLIPPED", FeatureQualityFlag.CLIPPED, flag)
        }
    }

    @Test
    fun score_belowFifteenSecondsVoiced_engagementFailsAndQualityIsZero() {
        val benignSamples = ShortArray(sampleRateHz * 30)
        val features = VoiceFeatureSet(
            jitterLocal = 0.01,
            shimmerLocal = 0.1,
            hnrDb = 20.0,
            f0MeanHz = 110.0,
            f0SdHz = 1.0,
            speakingRateWpm = 200.0,
            pauseFraction = 0.6667,
            voicedSeconds = 10.0,
            totalSeconds = 30.0,
            periodCount = 1100,
        )
        val q = VoiceQuality.score(features, benignSamples, sampleRateHz)
        assertFalse("engagement should fail below 15 s voiced", q.engagementOk)
        assertEquals(0.0, q.qualityScore, 0.0)
        assertEquals(
            "jitter flag should be INSUFFICIENT_VOICING",
            FeatureQualityFlag.INSUFFICIENT_VOICING,
            q.perFeatureFlags[VoiceQuality.FEATURE_KEY_JITTER],
        )
        assertEquals(
            "HNR flag should be INSUFFICIENT_VOICING",
            FeatureQualityFlag.INSUFFICIENT_VOICING,
            q.perFeatureFlags[VoiceQuality.FEATURE_KEY_HNR],
        )
    }

    @Test
    fun score_belowFiftyPeriods_dropsPerturbationFlagsToInsufficientPeriods() {
        val benignSamples = ShortArray(sampleRateHz * 30)
        val features = VoiceFeatureSet(
            jitterLocal = null,
            shimmerLocal = null,
            hnrDb = 20.0,
            f0MeanHz = 110.0,
            f0SdHz = null,
            speakingRateWpm = 200.0,
            pauseFraction = 0.0,
            voicedSeconds = 20.0,
            totalSeconds = 30.0,
            periodCount = 30,
        )
        val q = VoiceQuality.score(features, benignSamples, sampleRateHz)
        assertEquals(
            "jitter flag should be INSUFFICIENT_PERIODS",
            FeatureQualityFlag.INSUFFICIENT_PERIODS,
            q.perFeatureFlags[VoiceQuality.FEATURE_KEY_JITTER],
        )
        assertEquals(
            "shimmer flag should be INSUFFICIENT_PERIODS",
            FeatureQualityFlag.INSUFFICIENT_PERIODS,
            q.perFeatureFlags[VoiceQuality.FEATURE_KEY_SHIMMER],
        )
        assertEquals(
            "F0 SD flag should be INSUFFICIENT_PERIODS",
            FeatureQualityFlag.INSUFFICIENT_PERIODS,
            q.perFeatureFlags[VoiceQuality.FEATURE_KEY_F0_SD],
        )
        assertTrue("HNR flag should remain OK", q.perFeatureFlags[VoiceQuality.FEATURE_KEY_HNR] == FeatureQualityFlag.OK)
    }

    @Test
    fun score_engagementAtFifteenSecondsExactlyDoesNotFail() {
        val benignSamples = ShortArray(sampleRateHz * 30)
        val features = VoiceFeatureSet(
            jitterLocal = 0.01,
            shimmerLocal = 0.1,
            hnrDb = 20.0,
            f0MeanHz = 110.0,
            f0SdHz = 1.0,
            speakingRateWpm = 528.0,
            pauseFraction = 0.5,
            voicedSeconds = 15.0,
            totalSeconds = 30.0,
            periodCount = 1650,
        )
        val q = VoiceQuality.score(features, benignSamples, sampleRateHz)
        assertTrue("engagement should be ok at exactly 15.0 s", q.engagementOk)
        assertEquals("quality at the floor should be 1.0", 1.0, q.qualityScore, 0.0)
    }

    @Test
    fun score_nullFeatures_dropQualityByTenPercentEach() {
        val benignSamples = ShortArray(sampleRateHz * 30)
        val features = VoiceFeatureSet(
            jitterLocal = null,
            shimmerLocal = null,
            hnrDb = 20.0,
            f0MeanHz = 110.0,
            f0SdHz = 1.0,
            speakingRateWpm = 200.0,
            pauseFraction = 0.0,
            voicedSeconds = 20.0,
            totalSeconds = 30.0,
            periodCount = 2200,
        )
        val q = VoiceQuality.score(features, benignSamples, sampleRateHz)
        // null jitter + null shimmer = 2 NULL flags at 0.1 each = 0.8
        assertEquals("two null features should drop score to 0.8", 0.8, q.qualityScore, 0.001)
        assertEquals(
            FeatureQualityFlag.NULL,
            q.perFeatureFlags[VoiceQuality.FEATURE_KEY_JITTER],
        )
        assertEquals(
            FeatureQualityFlag.NULL,
            q.perFeatureFlags[VoiceQuality.FEATURE_KEY_SHIMMER],
        )
    }

    @Test
    fun score_emptySamples_isNotTreatedAsClipping() {
        val features = VoiceFeatureSet(
            jitterLocal = 0.01,
            shimmerLocal = 0.1,
            hnrDb = 20.0,
            f0MeanHz = 110.0,
            f0SdHz = 1.0,
            speakingRateWpm = 200.0,
            pauseFraction = 0.0,
            voicedSeconds = 20.0,
            totalSeconds = 30.0,
            periodCount = 2200,
        )
        val q = VoiceQuality.score(features, ShortArray(0), sampleRateHz)
        assertFalse("empty buffer must not trigger clipping path", q.clippingDetected)
    }

    @Test
    fun score_rejectsNegativeSampleRate() {
        val features = VoiceFeatureSet(
            jitterLocal = 0.01,
            shimmerLocal = 0.1,
            hnrDb = 20.0,
            f0MeanHz = 110.0,
            f0SdHz = 1.0,
            speakingRateWpm = 200.0,
            pauseFraction = 0.0,
            voicedSeconds = 20.0,
            totalSeconds = 30.0,
            periodCount = 2200,
        )
        var threw = false
        try {
            VoiceQuality.score(features, ShortArray(10), -1)
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue("negative sample rate should throw", threw)
    }
}
