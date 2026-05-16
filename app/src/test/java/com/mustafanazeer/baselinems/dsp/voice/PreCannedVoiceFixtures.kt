package com.mustafanazeer.baselinems.dsp.voice

import java.io.DataInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Synthetic voice fixtures consumed by the Phase 8 `dsp/voice/` TDD suites.
 *
 * The five WAV files in `app/src/test/resources/voice_fixtures/` were generated
 * by `scripts/generate-voice-fixtures.py` with a documented integer seed
 * (20260516) so the bytes are reproducible byte for byte across machines that
 * run the same numpy version. The script is the source of truth for every
 * ground truth scalar exposed here; if a scalar in this loader disagrees with
 * the script's stdout YAML, the script wins and this file is corrected.
 *
 * Each fixture is 44.1 kHz mono 16 bit PCM and 30 seconds long. The WAV files
 * each carry a standard 44 byte RIFF header followed by 2,646,000 bytes of
 * little endian 16 bit PCM. The loader parses the RIFF chunks defensively
 * rather than skipping a fixed 44 bytes; any future regeneration that produces
 * a slightly different header layout (extra chunks, list metadata) loads
 * without modification.
 *
 * Synthetic versus real human voice caveat. The fixtures here are mathematically
 * tractable harmonic stacks plus calibrated noise; they are deliberately
 * simpler than real speech. A real reading of the Grandfather Passage by a
 * human introduces formant transitions, voicing onsets and offsets at every
 * consonant, prosodic amplitude modulation across stressed and unstressed
 * syllables, and per cycle period microstructure that the synthetic generator
 * does not model. The TDD assertions written against these fixtures verify
 * the implemented algorithms (jitter local, shimmer local, HNR per Boersma
 * 1993 equation 4, F0 SD via 1.0 over periodSeconds, pause fraction via energy
 * threshold VAD) against known analytic ground truth. Real microphone
 * calibration against typical Pixel 6 microphone gain levels is deferred to
 * the user driven Phase 8 AVD walkthrough at phase close per the Phase 6 and
 * Phase 7 convention.
 *
 * Ground truth derivations.
 *
 * Jitter local: per the Praat manual Voice 2 page, jitter local equals
 * `mean_i(|T_{i+1} - T_i|) / mean_i(T_i)` over voiced cycle periods. The
 * perturbed fixture draws each cycle's period from `T0 * (1 + p_i)` with
 * `p_i ~ Uniform(-0.02, +0.02)`, and the analytic expected value of
 * `|p_{i+1} - p_i|` for two iid uniforms on `[-a, +a]` is `(2 a) / 3`. So
 * the analytic expected jitter local is `0.02 * 2 / 3 ~= 0.01333`. The
 * realized value from the script's measured periods is 0.01320.
 *
 * Shimmer local: per the Praat manual Voice 3 page, shimmer local equals
 * `mean_i(|A_{i+1} - A_i|) / mean_i(A_i)` over voiced cycle peak amplitudes.
 * The perturbed fixture scales each cycle's amplitude by
 * `s_i ~ Uniform(0.85, 1.15)`. The analytic expected value of
 * `|s_{i+1} - s_i|` is `(2 * 0.15) / 3 = 0.10`. The realized value from the
 * script's measured peak sequence is 0.10370.
 *
 * HNR: per Boersma 1993 IFA Proceedings 17:97 to 110 equation 4,
 * `HNR_dB = 10 * log10[ rx_max / (1 - rx_max) ]` where rx_max is the local
 * maximum of the normalized autocorrelation at the fundamental period lag.
 * For the voiced_noisy fixture the additive white noise variance was
 * calibrated to a broadband SNR target of 8 dB; Boersma's autocorrelation
 * HNR is not numerically identical to broadband SNR but converges to within
 * about 1 to 3 dB for moderate SNR. The expected HNR center is 8.0 dB and
 * the tolerance is +/- 3 dB to cover the estimator bias.
 *
 * F0 SD: defined as the standard deviation of `1.0 / periodSeconds` across
 * voiced frames. The clean fixture has zero F0 SD (single steady fundamental).
 * The perturbed fixture has a small F0 SD reflecting the period jitter;
 * realized value 1.27 Hz over 3300 cycles.
 *
 * Pause fraction: defined as the proportion of frames classified silence by
 * the energy threshold VAD (`silence_frame_count / total_frame_count`). For
 * the mixed fixture, 15 of 30 seconds are voiced (3 second voiced segments
 * alternated with 3 second silence segments), so the analytic pause fraction
 * is exactly 0.50.
 *
 * Speaking rate: per the Phase 8 ADR Section 4 Feature 5, speaking rate WPM
 * is `(passageWordCount / voicedDurationSeconds) * 60`. The voiced_clean
 * fixture is continuously voiced with no words read (synthetic tone, not
 * speech), so the expected WPM in the speaking rate test is undefined for
 * this fixture; the speaking rate test uses the mixed fixture (15 seconds
 * voiced) and the 132 word Grandfather Passage word count, yielding
 * 528 WPM as the analytic ground truth for that test only.
 *
 * Tolerances.
 *
 * Per feature tolerances are documented per fixture below; the downstream
 * `VoiceFeaturesTest` and `PraatPitchTest` suites SHOULD reference the named
 * `*Tolerance*` constants here rather than introduce magic numbers in the
 * test code. The Signal Processing Engineer may relax a tolerance with a
 * documented reason; tightening a tolerance below the values here is allowed
 * only with a corroborating fixture analysis.
 */
object PreCannedVoiceFixtures {

    const val SAMPLE_RATE_HZ: Int = 44100
    const val DURATION_SECONDS: Double = 30.0
    const val TOTAL_SAMPLES: Int = 1_323_000

    const val GENERATOR_SCRIPT_SEED: Long = 20_260_516L
    const val GENERATOR_SCRIPT_PATH: String = "scripts/generate-voice-fixtures.py"

    private const val FIXTURE_RESOURCE_ROOT: String = "/voice_fixtures"

    /**
     * Pure 110 Hz fundamental plus four harmonics, no jitter, no shimmer, no
     * additive noise.
     *
     * Expected ground truth:
     *   F0 mean: 110.0 Hz (exact)
     *   F0 SD: 0.0 Hz (single steady fundamental)
     *   Jitter local: 0.0 (no period perturbation)
     *   Shimmer local: 0.0 (no amplitude perturbation)
     *   HNR: greater than 25 dB (no additive noise; the residual is the
     *        autocorrelation estimator's finite window leakage only)
     *   Pause fraction: 0.0 (continuously voiced for the full 30 seconds)
     *   Speaking rate WPM: 0.0 in the synthetic content sense (no words);
     *        the speaking rate test does not use this fixture.
     */
    val voicedClean: VoiceFixture by lazy { loadFixture(FixtureSpec.voicedClean()) }

    /**
     * 110 Hz fundamental plus four harmonics with each cycle period drawn
     * from `T0 * (1 + Uniform(-0.02, 0.02))` and each cycle amplitude scaled
     * by `Uniform(0.85, 1.15)`.
     *
     * Expected ground truth:
     *   F0 mean: 110.0 Hz, realized 110.0024 Hz over 3300 cycles
     *   F0 SD: ~1.27 Hz (realized; reflects the period jitter)
     *   Jitter local: analytic 0.01333, realized 0.01320; tolerance +/- 0.005
     *   Shimmer local: analytic 0.10, realized 0.10370; tolerance +/- 0.02
     *   HNR: in `[10, 25] dB` (no additive noise but the per cycle
     *        perturbation reduces autocorrelation peak height)
     *   Pause fraction: 0.0 (continuously voiced)
     */
    val voicedPerturbed: VoiceFixture by lazy { loadFixture(FixtureSpec.voicedPerturbed()) }

    /**
     * The voicedClean harmonic stack with additive white Gaussian noise
     * calibrated to a broadband SNR of 8.0 dB (realized 8.007 dB).
     *
     * Expected ground truth:
     *   F0 mean: 110.0 Hz (the underlying periodic content is unchanged)
     *   HNR: center 8.0 dB, tolerance +/- 3 dB to cover the Boersma 1993
     *        autocorrelation estimator bias relative to broadband SNR
     *   Pause fraction: 0.0 (continuously voiced)
     */
    val voicedNoisy: VoiceFixture by lazy { loadFixture(FixtureSpec.voicedNoisy()) }

    /**
     * Pure pink noise (1/f spectrum) with no periodic content, calibrated to
     * a float RMS of 0.05 (int16 RMS approximately 1638).
     *
     * Expected ground truth:
     *   F0: undefined (the VAD should classify every frame as unvoiced)
     *   HNR: less than 0 dB (no periodic harmonic content)
     *   Pause fraction: 1.0 (energy is below the speech threshold in the
     *        Phase 8 VAD model; the noise is broadband but does not exceed
     *        the calibration window's noise floor plus 6 dB anywhere)
     */
    val pinkNoise: VoiceFixture by lazy { loadFixture(FixtureSpec.pinkNoise()) }

    /**
     * Five 3 second voiced segments (the voicedClean harmonic stack)
     * alternated with five 3 second silence segments (Gaussian noise floor),
     * totaling 30 seconds.
     *
     * Expected ground truth:
     *   F0 in voiced segments: 110.0 Hz
     *   Pause fraction: 0.5 (exactly 15 seconds voiced, 15 seconds silence)
     *   Speaking rate WPM at 132 word passage: 528.0 (132 / 15 * 60)
     *        WITH the Phase 8 contract that voicedDurationSeconds is the
     *        denominator, not the full 30 second recording duration.
     */
    val mixedVoicedUnvoiced: VoiceFixture by lazy { loadFixture(FixtureSpec.mixedVoicedUnvoiced()) }

    val all: List<VoiceFixture> by lazy {
        listOf(voicedClean, voicedPerturbed, voicedNoisy, pinkNoise, mixedVoicedUnvoiced)
    }

    private fun loadFixture(spec: FixtureSpec): VoiceFixture {
        val resourcePath = "$FIXTURE_RESOURCE_ROOT/${spec.fileName}"
        val bytes = PreCannedVoiceFixtures::class.java.getResourceAsStream(resourcePath)?.use {
            it.readBytes()
        } ?: error(
            "Voice fixture resource not found: $resourcePath. " +
                "Regenerate via `python3 $GENERATOR_SCRIPT_PATH`.",
        )
        val samples = parseWavToShortArray(bytes, spec.fileName)
        require(samples.size == TOTAL_SAMPLES) {
            "Voice fixture ${spec.fileName} parsed ${samples.size} samples; expected $TOTAL_SAMPLES"
        }
        return VoiceFixture(spec, samples)
    }

    private fun parseWavToShortArray(bytes: ByteArray, fileNameForErrors: String): ShortArray {
        val stream = DataInputStream(bytes.inputStream())
        val riff = ByteArray(4).also { stream.readFully(it) }
        require(riff.contentEquals("RIFF".toByteArray(Charsets.US_ASCII))) {
            "Voice fixture $fileNameForErrors is not a RIFF file"
        }
        stream.readFully(ByteArray(4))
        val wave = ByteArray(4).also { stream.readFully(it) }
        require(wave.contentEquals("WAVE".toByteArray(Charsets.US_ASCII))) {
            "Voice fixture $fileNameForErrors is not a WAVE file"
        }
        var fmtFound = false
        var dataChunkOffset = -1
        var dataChunkSize = -1
        var sampleRate = -1
        var bitsPerSample = -1
        var numChannels = -1
        var cursor = 12
        while (cursor < bytes.size - 8) {
            val chunkId = String(bytes, cursor, 4, Charsets.US_ASCII)
            val chunkSize = ByteBuffer.wrap(bytes, cursor + 4, 4)
                .order(ByteOrder.LITTLE_ENDIAN).int
            when (chunkId) {
                "fmt " -> {
                    val fmtBuf = ByteBuffer.wrap(bytes, cursor + 8, chunkSize)
                        .order(ByteOrder.LITTLE_ENDIAN)
                    fmtBuf.short
                    numChannels = fmtBuf.short.toInt()
                    sampleRate = fmtBuf.int
                    fmtBuf.int
                    fmtBuf.short
                    bitsPerSample = fmtBuf.short.toInt()
                    fmtFound = true
                }
                "data" -> {
                    dataChunkOffset = cursor + 8
                    dataChunkSize = chunkSize
                }
            }
            cursor += 8 + chunkSize
            if (chunkSize % 2 == 1) cursor += 1
            if (dataChunkOffset > 0 && fmtFound) break
        }
        require(fmtFound && dataChunkOffset > 0 && dataChunkSize > 0) {
            "Voice fixture $fileNameForErrors missing fmt or data chunk"
        }
        require(sampleRate == SAMPLE_RATE_HZ) {
            "Voice fixture $fileNameForErrors sample rate $sampleRate; expected $SAMPLE_RATE_HZ"
        }
        require(bitsPerSample == 16) {
            "Voice fixture $fileNameForErrors bits per sample $bitsPerSample; expected 16"
        }
        require(numChannels == 1) {
            "Voice fixture $fileNameForErrors channels $numChannels; expected 1"
        }
        val sampleCount = dataChunkSize / 2
        val out = ShortArray(sampleCount)
        val buf = ByteBuffer.wrap(bytes, dataChunkOffset, dataChunkSize)
            .order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until sampleCount) {
            out[i] = buf.short
        }
        return out
    }
}

/**
 * One loaded synthetic voice fixture with its raw PCM samples and the
 * documented ground truth scalars the TDD suites assert against.
 *
 * Fields documented `null` are intentionally undefined for the given fixture
 * (for example, F0 is undefined on the pink_noise fixture; the VAD should
 * classify every frame unvoiced and no F0 is recoverable).
 */
data class VoiceFixture(
    val spec: FixtureSpec,
    val samples: ShortArray,
) {
    val sampleRateHz: Int get() = PreCannedVoiceFixtures.SAMPLE_RATE_HZ
    val durationSeconds: Double get() = PreCannedVoiceFixtures.DURATION_SECONDS

    val resourceFileName: String get() = spec.fileName
    val expectedF0Hz: Double? get() = spec.expectedF0Hz
    val expectedF0SdHz: Double? get() = spec.expectedF0SdHz
    val expectedJitterLocal: Double? get() = spec.expectedJitterLocal
    val expectedShimmerLocal: Double? get() = spec.expectedShimmerLocal
    val expectedHnrDbCenter: Double? get() = spec.expectedHnrDbCenter
    val expectedHnrDbMin: Double? get() = spec.expectedHnrDbMin
    val expectedHnrDbMax: Double? get() = spec.expectedHnrDbMax
    val expectedSpeakingRateWpm: Double? get() = spec.expectedSpeakingRateWpm
    val expectedPauseFraction: Double get() = spec.expectedPauseFraction
    val f0ToleranceHz: Double get() = spec.f0ToleranceHz
    val f0SdToleranceHz: Double get() = spec.f0SdToleranceHz
    val jitterTolerance: Double get() = spec.jitterTolerance
    val shimmerTolerance: Double get() = spec.shimmerTolerance
    val hnrToleranceDb: Double get() = spec.hnrToleranceDb
    val pauseFractionTolerance: Double get() = spec.pauseFractionTolerance

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VoiceFixture) return false
        return spec == other.spec && samples.contentEquals(other.samples)
    }

    override fun hashCode(): Int = 31 * spec.hashCode() + samples.contentHashCode()
}

/**
 * Per fixture immutable metadata block. Kept as a `data class` separate from
 * the `samples` `ShortArray` so the metadata is cheap to compare in tests.
 */
data class FixtureSpec(
    val fileName: String,
    val expectedF0Hz: Double?,
    val expectedF0SdHz: Double?,
    val expectedJitterLocal: Double?,
    val expectedShimmerLocal: Double?,
    val expectedHnrDbCenter: Double?,
    val expectedHnrDbMin: Double?,
    val expectedHnrDbMax: Double?,
    val expectedSpeakingRateWpm: Double?,
    val expectedPauseFraction: Double,
    val f0ToleranceHz: Double,
    val f0SdToleranceHz: Double,
    val jitterTolerance: Double,
    val shimmerTolerance: Double,
    val hnrToleranceDb: Double,
    val pauseFractionTolerance: Double,
) {
    companion object {
        fun voicedClean(): FixtureSpec = FixtureSpec(
            fileName = "voiced_clean.wav",
            expectedF0Hz = 110.0,
            expectedF0SdHz = 0.0,
            expectedJitterLocal = 0.0,
            expectedShimmerLocal = 0.0,
            expectedHnrDbCenter = null,
            expectedHnrDbMin = 25.0,
            expectedHnrDbMax = null,
            expectedSpeakingRateWpm = null,
            expectedPauseFraction = 0.0,
            f0ToleranceHz = 1.0,
            f0SdToleranceHz = 0.5,
            jitterTolerance = 0.002,
            shimmerTolerance = 0.005,
            hnrToleranceDb = 5.0,
            pauseFractionTolerance = 0.05,
        )

        fun voicedPerturbed(): FixtureSpec = FixtureSpec(
            fileName = "voiced_perturbed.wav",
            expectedF0Hz = 110.0,
            expectedF0SdHz = 1.27,
            expectedJitterLocal = 0.01333,
            expectedShimmerLocal = 0.10,
            expectedHnrDbCenter = null,
            expectedHnrDbMin = 10.0,
            expectedHnrDbMax = 25.0,
            expectedSpeakingRateWpm = null,
            expectedPauseFraction = 0.0,
            f0ToleranceHz = 2.0,
            f0SdToleranceHz = 1.5,
            jitterTolerance = 0.005,
            shimmerTolerance = 0.02,
            hnrToleranceDb = 7.5,
            pauseFractionTolerance = 0.05,
        )

        fun voicedNoisy(): FixtureSpec = FixtureSpec(
            fileName = "voiced_noisy.wav",
            expectedF0Hz = 110.0,
            expectedF0SdHz = null,
            expectedJitterLocal = null,
            expectedShimmerLocal = null,
            expectedHnrDbCenter = 8.0,
            expectedHnrDbMin = null,
            expectedHnrDbMax = null,
            expectedSpeakingRateWpm = null,
            expectedPauseFraction = 0.0,
            f0ToleranceHz = 2.0,
            f0SdToleranceHz = 5.0,
            jitterTolerance = 0.05,
            shimmerTolerance = 0.10,
            hnrToleranceDb = 3.0,
            pauseFractionTolerance = 0.10,
        )

        fun pinkNoise(): FixtureSpec = FixtureSpec(
            fileName = "pink_noise.wav",
            expectedF0Hz = null,
            expectedF0SdHz = null,
            expectedJitterLocal = null,
            expectedShimmerLocal = null,
            expectedHnrDbCenter = null,
            expectedHnrDbMin = null,
            expectedHnrDbMax = 0.0,
            expectedSpeakingRateWpm = null,
            expectedPauseFraction = 1.0,
            f0ToleranceHz = Double.NaN,
            f0SdToleranceHz = Double.NaN,
            jitterTolerance = Double.NaN,
            shimmerTolerance = Double.NaN,
            hnrToleranceDb = 5.0,
            pauseFractionTolerance = 0.05,
        )

        fun mixedVoicedUnvoiced(): FixtureSpec = FixtureSpec(
            fileName = "mixed_voiced_unvoiced.wav",
            expectedF0Hz = 110.0,
            expectedF0SdHz = null,
            expectedJitterLocal = null,
            expectedShimmerLocal = null,
            expectedHnrDbCenter = null,
            expectedHnrDbMin = null,
            expectedHnrDbMax = null,
            expectedSpeakingRateWpm = 528.0,
            expectedPauseFraction = 0.5,
            f0ToleranceHz = 2.0,
            f0SdToleranceHz = Double.NaN,
            jitterTolerance = Double.NaN,
            shimmerTolerance = Double.NaN,
            hnrToleranceDb = Double.NaN,
            pauseFractionTolerance = 0.05,
        )
    }
}
