#!/usr/bin/env python3
"""
Generate the Phase 8 synthetic voice fixtures consumed by `dsp/voice/` TDD.

Outputs five WAV files under
    app/src/test/resources/voice_fixtures/
each at 30 seconds, 44.1 kHz mono, 16 bit signed PCM:

    voiced_clean.wav             pure harmonic stack, no perturbation, no noise
    voiced_perturbed.wav         harmonic stack with uniform jitter and shimmer
    voiced_noisy.wav             harmonic stack plus calibrated additive white Gaussian noise
    pink_noise.wav               1/f spectrum, no periodic content
    mixed_voiced_unvoiced.wav    alternating 3 s voiced and 3 s silence segments

The script is reproducible: it seeds numpy's random generator with the
documented integer seed below. Running the script with the same seed on the
same numpy version produces the same bytes.

The script also prints, to stdout, a YAML block of the expected ground truth
scalars for each fixture so the downstream Kotlin loader (PreCannedVoiceFixtures.kt)
and the downstream Signal Processing Engineer's TDD assertions reference values
that come from one place. The script is intentionally the source of truth: the
Kotlin loader transcribes these values; the Praat algorithm port in
dsp/voice/VoiceFeatures.kt asserts against them within documented tolerances.

Run:
    python3 scripts/generate-voice-fixtures.py

Required:
    numpy
    scipy.io.wavfile

This script writes binary fixture files plus prints the ground truth YAML on
stdout. It does not touch any other tracked file.

Analytic derivations
====================

Jitter local definition (Praat manual Voice 2):

    jitter_local = mean_i(|T_{i+1} - T_i|) / mean_i(T_i)

If each period T_i is the nominal period T0 perturbed by a uniform random
variable scaled to fraction p in [-a, +a] of T0 (so T_i = T0 * (1 + p_i)),
then T_{i+1} - T_i = T0 * (p_{i+1} - p_i). For two independent uniforms on
[-a, +a], the expectation E[|X - Y|] is exactly (2 a) / 3 by the symmetric
triangular distribution of X - Y on [-2 a, +2 a]. Therefore

    E[jitter_local] = E[|p_{i+1} - p_i|] = (2 a) / 3

For a = 0.02 (the perturbed fixture's jitter amplitude), the analytic expected
jitter_local is 0.02 * 2 / 3 ~= 0.01333. The sample grid quantization at
44.1 kHz introduces a small additional perturbation on the realized periods
(periods are rounded to integer sample counts before the waveform is built),
which can push the realized jitter_local slightly above the analytic 0.01333
on a finite cycle population; the Kotlin loader documents a +/- 0.005 tolerance
to cover this and the finite N cycle estimator variance.

Shimmer local definition (Praat manual Voice 3):

    shimmer_local = mean_i(|A_{i+1} - A_i|) / mean_i(A_i)

If each cycle's peak amplitude is A0 * s_i where s_i is uniform on
[1 - b, 1 + b], then A_{i+1} - A_i = A0 * (s_{i+1} - s_i). For two independent
uniforms on [1 - b, 1 + b] (centered at 1), E[|s_{i+1} - s_i|] = (2 b) / 3 and
E[s_i] = 1. So

    E[shimmer_local] = (2 b) / 3

For b = 0.15 (the perturbed fixture's shimmer amplitude), the analytic expected
shimmer_local is 0.15 * 2 / 3 = 0.10. Same +/- tolerance considerations apply
on finite N cycles.

HNR definition (Boersma 1993, IFA Proceedings 17:97-110, equation 4):

    HNR_dB = 10 * log10[ rx_max / (1 - rx_max) ]

where rx_max is the local maximum of the normalized autocorrelation at the
fundamental period lag. For the voiced_noisy fixture we target the broadband
SNR (signal RMS over noise RMS) as a calibration anchor:

    SNR_dB = 20 * log10(R_signal / R_noise)

Boersma's autocorrelation HNR is not numerically identical to the broadband
SNR (the autocorrelation is computed over windowed segments and has its own
estimator bias), but for moderate SNR (5 to 20 dB) the two converge to within
roughly 1 to 3 dB. The Kotlin loader documents a +/- 3 dB tolerance on the
voiced_noisy expected HNR to accommodate this estimator bias.
"""

import os
import sys
from pathlib import Path

import numpy as np
from scipy.io import wavfile

SEED = 20260516
RNG = np.random.default_rng(SEED)

SAMPLE_RATE_HZ = 44100
DURATION_SECONDS = 30.0
TOTAL_SAMPLES = int(SAMPLE_RATE_HZ * DURATION_SECONDS)

F0_HZ = 110.0
HARMONIC_AMPLITUDES = [1.0, 0.5, 0.33, 0.25, 0.2]

JITTER_AMPLITUDE = 0.02
SHIMMER_AMPLITUDE = 0.15

CLEAN_FUNDAMENTAL_AMPLITUDE = 0.5

VOICED_NOISY_TARGET_SNR_DB = 8.0

PINK_NOISE_RMS = 0.05


def synth_harmonic_stack_clean(num_samples: int, sample_rate_hz: int, f0_hz: float) -> np.ndarray:
    """Produce a stationary harmonic stack with five components.

    Returns a float64 array of normalized amplitude. The fundamental and four
    harmonics are summed with the documented amplitude ratios; the overall
    amplitude is scaled so the peak does not exceed CLEAN_FUNDAMENTAL_AMPLITUDE
    times the sum-of-harmonic-amplitudes safety margin (avoids 16 bit clipping).
    """
    t = np.arange(num_samples, dtype=np.float64) / sample_rate_hz
    signal = np.zeros(num_samples, dtype=np.float64)
    for k, amp in enumerate(HARMONIC_AMPLITUDES, start=1):
        signal += amp * np.sin(2.0 * np.pi * f0_hz * k * t)
    peak_envelope = float(np.sum(HARMONIC_AMPLITUDES))
    signal = signal * (CLEAN_FUNDAMENTAL_AMPLITUDE / peak_envelope)
    return signal


def synth_harmonic_stack_perturbed(
    num_samples: int,
    sample_rate_hz: int,
    f0_hz: float,
    jitter_amplitude: float,
    shimmer_amplitude: float,
    rng: np.random.Generator,
) -> tuple[np.ndarray, dict]:
    """Produce a harmonic stack where each fundamental period is jittered and
    each period's amplitude is scaled by a shimmer factor.

    The waveform is built cycle by cycle. Each cycle i has a period T_i =
    T_nominal * (1 + p_i) with p_i drawn uniform on [-jitter_amplitude,
    +jitter_amplitude], quantized to integer samples. The peak amplitude of
    each cycle is scaled by s_i drawn uniform on [1 - shimmer_amplitude,
    1 + shimmer_amplitude]. The harmonic stack within each cycle uses the
    cycle's local instantaneous fundamental f0_local = 1.0 / T_i_seconds so
    the five harmonic frequencies track f0_local. This produces a waveform
    whose period sequence and per cycle peak sequence match the analytic
    formulas in the module docstring.

    Returns the waveform and a diagnostics dict containing the realized
    period sequence (in seconds) and the realized peak sequence (so the
    realized jitter_local and shimmer_local can be measured for the ground
    truth report).
    """
    nominal_period_samples = sample_rate_hz / f0_hz
    realized_periods_samples: list[int] = []
    realized_peaks: list[float] = []
    waveform = np.zeros(num_samples, dtype=np.float64)
    peak_envelope = float(np.sum(HARMONIC_AMPLITUDES))
    base_amp = CLEAN_FUNDAMENTAL_AMPLITUDE / peak_envelope
    cursor = 0
    while cursor < num_samples:
        p = float(rng.uniform(-jitter_amplitude, jitter_amplitude))
        s = float(rng.uniform(1.0 - shimmer_amplitude, 1.0 + shimmer_amplitude))
        period_samples = int(round(nominal_period_samples * (1.0 + p)))
        if period_samples < 8:
            period_samples = 8
        end = min(cursor + period_samples, num_samples)
        local_n = end - cursor
        t_local = np.arange(local_n, dtype=np.float64) / sample_rate_hz
        f0_local = 1.0 / (period_samples / sample_rate_hz)
        cycle = np.zeros(local_n, dtype=np.float64)
        for k, amp in enumerate(HARMONIC_AMPLITUDES, start=1):
            cycle += amp * np.sin(2.0 * np.pi * f0_local * k * t_local)
        cycle = cycle * (base_amp * s)
        waveform[cursor:end] = cycle
        if end - cursor >= 8:
            realized_periods_samples.append(period_samples)
            realized_peaks.append(float(base_amp * s * peak_envelope))
        cursor = end
    diagnostics = {
        "periods_seconds": [p / sample_rate_hz for p in realized_periods_samples],
        "peaks": realized_peaks,
    }
    return waveform, diagnostics


def measure_jitter_local(periods_seconds: list[float]) -> float:
    if len(periods_seconds) < 2:
        return 0.0
    arr = np.array(periods_seconds, dtype=np.float64)
    diffs = np.abs(np.diff(arr))
    return float(np.mean(diffs) / np.mean(arr))


def measure_shimmer_local(peaks: list[float]) -> float:
    if len(peaks) < 2:
        return 0.0
    arr = np.array(peaks, dtype=np.float64)
    diffs = np.abs(np.diff(arr))
    return float(np.mean(diffs) / np.mean(arr))


def calibrate_white_noise_for_target_snr(
    signal_rms: float, target_snr_db: float
) -> float:
    """Return the noise RMS that yields the requested SNR against a signal RMS."""
    noise_rms = signal_rms / (10.0 ** (target_snr_db / 20.0))
    return float(noise_rms)


def synth_pink_noise(num_samples: int, target_rms: float, rng: np.random.Generator) -> np.ndarray:
    """Generate pink noise via the Voss-McCartney style frequency domain shaping.

    We synthesize white Gaussian noise, transform to the frequency domain,
    scale magnitudes by 1 / sqrt(f), and inverse transform. The output is
    rescaled to the requested RMS so the calibration target is met exactly.
    """
    white = rng.standard_normal(num_samples)
    spectrum = np.fft.rfft(white)
    freqs = np.fft.rfftfreq(num_samples, d=1.0 / SAMPLE_RATE_HZ)
    scale = np.ones_like(freqs)
    scale[1:] = 1.0 / np.sqrt(freqs[1:])
    shaped = spectrum * scale
    pink = np.fft.irfft(shaped, n=num_samples)
    current_rms = float(np.sqrt(np.mean(pink * pink)))
    if current_rms > 0.0:
        pink = pink * (target_rms / current_rms)
    return pink


def synth_mixed_voiced_unvoiced(
    num_samples: int,
    sample_rate_hz: int,
    voiced_block_seconds: float,
    silence_block_seconds: float,
    silence_noise_floor: float,
    rng: np.random.Generator,
) -> tuple[np.ndarray, int]:
    """Alternate voiced and silence blocks until the total duration is filled.

    Returns the waveform and the count of voiced blocks that fit in the
    requested duration.
    """
    voiced_samples = int(round(voiced_block_seconds * sample_rate_hz))
    silence_samples = int(round(silence_block_seconds * sample_rate_hz))
    voiced_template = synth_harmonic_stack_clean(voiced_samples, sample_rate_hz, F0_HZ)
    waveform = np.zeros(num_samples, dtype=np.float64)
    cursor = 0
    voiced_blocks = 0
    while cursor < num_samples:
        end = min(cursor + voiced_samples, num_samples)
        waveform[cursor:end] = voiced_template[: end - cursor]
        if end - cursor >= voiced_samples // 2:
            voiced_blocks += 1
        cursor = end
        if cursor >= num_samples:
            break
        sil_end = min(cursor + silence_samples, num_samples)
        sil_len = sil_end - cursor
        waveform[cursor:sil_end] = rng.normal(0.0, silence_noise_floor, size=sil_len)
        cursor = sil_end
    return waveform, voiced_blocks


def to_int16(samples: np.ndarray) -> np.ndarray:
    clipped = np.clip(samples, -1.0, 1.0)
    scaled = (clipped * 32767.0).astype(np.int16)
    return scaled


def rms(samples: np.ndarray) -> float:
    return float(np.sqrt(np.mean(samples.astype(np.float64) ** 2)))


def write_wav(path: Path, samples_int16: np.ndarray) -> int:
    path.parent.mkdir(parents=True, exist_ok=True)
    wavfile.write(str(path), SAMPLE_RATE_HZ, samples_int16)
    return path.stat().st_size


def main() -> int:
    repo_root = Path(__file__).resolve().parent.parent
    out_dir = repo_root / "app/src/test/resources/voice_fixtures"

    rng_clean = np.random.default_rng(SEED)
    rng_perturbed = np.random.default_rng(SEED + 1)
    rng_noisy = np.random.default_rng(SEED + 2)
    rng_pink = np.random.default_rng(SEED + 3)
    rng_mixed = np.random.default_rng(SEED + 4)

    clean = synth_harmonic_stack_clean(TOTAL_SAMPLES, SAMPLE_RATE_HZ, F0_HZ)
    clean_int16 = to_int16(clean)
    clean_size = write_wav(out_dir / "voiced_clean.wav", clean_int16)

    perturbed, perturbed_diag = synth_harmonic_stack_perturbed(
        TOTAL_SAMPLES,
        SAMPLE_RATE_HZ,
        F0_HZ,
        JITTER_AMPLITUDE,
        SHIMMER_AMPLITUDE,
        rng_perturbed,
    )
    perturbed_int16 = to_int16(perturbed)
    perturbed_size = write_wav(out_dir / "voiced_perturbed.wav", perturbed_int16)
    realized_jitter = measure_jitter_local(perturbed_diag["periods_seconds"])
    realized_shimmer = measure_shimmer_local(perturbed_diag["peaks"])
    perturbed_periods = perturbed_diag["periods_seconds"]
    perturbed_f0_hz = [1.0 / p for p in perturbed_periods]
    realized_f0_mean = float(np.mean(perturbed_f0_hz)) if perturbed_f0_hz else 0.0
    realized_f0_sd = float(np.std(perturbed_f0_hz, ddof=0)) if perturbed_f0_hz else 0.0
    realized_cycle_count = len(perturbed_periods)

    clean_for_noisy = synth_harmonic_stack_clean(TOTAL_SAMPLES, SAMPLE_RATE_HZ, F0_HZ)
    signal_rms = float(np.sqrt(np.mean(clean_for_noisy * clean_for_noisy)))
    noise_rms = calibrate_white_noise_for_target_snr(signal_rms, VOICED_NOISY_TARGET_SNR_DB)
    noise = rng_noisy.normal(0.0, noise_rms, size=TOTAL_SAMPLES)
    noisy = clean_for_noisy + noise
    realized_noise_rms = float(np.sqrt(np.mean(noise * noise)))
    realized_snr_db = 20.0 * float(np.log10(signal_rms / realized_noise_rms))
    noisy_int16 = to_int16(noisy)
    noisy_size = write_wav(out_dir / "voiced_noisy.wav", noisy_int16)

    pink = synth_pink_noise(TOTAL_SAMPLES, PINK_NOISE_RMS, rng_pink)
    realized_pink_rms = float(np.sqrt(np.mean(pink * pink)))
    pink_int16 = to_int16(pink)
    pink_size = write_wav(out_dir / "pink_noise.wav", pink_int16)

    mixed, voiced_block_count = synth_mixed_voiced_unvoiced(
        TOTAL_SAMPLES,
        SAMPLE_RATE_HZ,
        voiced_block_seconds=3.0,
        silence_block_seconds=3.0,
        silence_noise_floor=0.0005,
        rng=rng_mixed,
    )
    mixed_int16 = to_int16(mixed)
    mixed_size = write_wav(out_dir / "mixed_voiced_unvoiced.wav", mixed_int16)
    voiced_seconds = voiced_block_count * 3.0
    mixed_pause_fraction = 1.0 - (voiced_seconds / DURATION_SECONDS)
    grandfather_word_count = 132
    expected_mixed_wpm = (
        (grandfather_word_count / voiced_seconds) * 60.0 if voiced_seconds > 0 else 0.0
    )

    analytic_jitter_local = (2.0 * JITTER_AMPLITUDE) / 3.0
    analytic_shimmer_local = (2.0 * SHIMMER_AMPLITUDE) / 3.0

    print("# voice fixture ground truth (generated by scripts/generate-voice-fixtures.py)")
    print(f"seed: {SEED}")
    print(f"sample_rate_hz: {SAMPLE_RATE_HZ}")
    print(f"duration_seconds: {DURATION_SECONDS}")
    print(f"total_samples: {TOTAL_SAMPLES}")
    print("")
    print("voiced_clean:")
    print(f"  path: app/src/test/resources/voice_fixtures/voiced_clean.wav")
    print(f"  bytes: {clean_size}")
    print(f"  expected_f0_hz: {F0_HZ}")
    print(f"  expected_f0_sd_hz: 0.0")
    print(f"  expected_jitter_local: 0.0")
    print(f"  expected_shimmer_local: 0.0")
    print(f"  expected_hnr_db_min: 25.0")
    print(f"  expected_speaking_rate_wpm: 0.0")
    print(f"  expected_pause_fraction: 0.0")
    print(f"  realized_signal_rms: {rms(clean_int16):.4f}")
    print("")
    print("voiced_perturbed:")
    print(f"  path: app/src/test/resources/voice_fixtures/voiced_perturbed.wav")
    print(f"  bytes: {perturbed_size}")
    print(f"  jitter_amplitude_used: {JITTER_AMPLITUDE}")
    print(f"  shimmer_amplitude_used: {SHIMMER_AMPLITUDE}")
    print(f"  analytic_expected_jitter_local: {analytic_jitter_local:.6f}")
    print(f"  analytic_expected_shimmer_local: {analytic_shimmer_local:.6f}")
    print(f"  realized_jitter_local_from_periods: {realized_jitter:.6f}")
    print(f"  realized_shimmer_local_from_peaks: {realized_shimmer:.6f}")
    print(f"  realized_cycle_count: {realized_cycle_count}")
    print(f"  realized_f0_mean_hz: {realized_f0_mean:.4f}")
    print(f"  realized_f0_sd_hz: {realized_f0_sd:.4f}")
    print(f"  expected_hnr_db_range: [10.0, 25.0]")
    print(f"  expected_pause_fraction: 0.0")
    print("")
    print("voiced_noisy:")
    print(f"  path: app/src/test/resources/voice_fixtures/voiced_noisy.wav")
    print(f"  bytes: {noisy_size}")
    print(f"  target_snr_db: {VOICED_NOISY_TARGET_SNR_DB}")
    print(f"  realized_signal_rms: {signal_rms:.6f}")
    print(f"  realized_noise_rms: {realized_noise_rms:.6f}")
    print(f"  realized_snr_db: {realized_snr_db:.4f}")
    print(f"  expected_hnr_db_center: {realized_snr_db:.4f}")
    print(f"  expected_hnr_db_tolerance: 3.0")
    print(f"  expected_f0_hz: {F0_HZ}")
    print(f"  expected_pause_fraction: 0.0")
    print("")
    print("pink_noise:")
    print(f"  path: app/src/test/resources/voice_fixtures/pink_noise.wav")
    print(f"  bytes: {pink_size}")
    print(f"  calibrated_rms_float: {PINK_NOISE_RMS}")
    print(f"  realized_rms_float: {realized_pink_rms:.6f}")
    print(f"  realized_rms_int16: {rms(pink_int16):.4f}")
    print(f"  expected_f0_hz: null")
    print(f"  expected_voiced_classification: false")
    print(f"  expected_hnr_db_max: 0.0")
    print(f"  expected_pause_fraction: 1.0")
    print("")
    print("mixed_voiced_unvoiced:")
    print(f"  path: app/src/test/resources/voice_fixtures/mixed_voiced_unvoiced.wav")
    print(f"  bytes: {mixed_size}")
    print(f"  voiced_block_count: {voiced_block_count}")
    print(f"  voiced_block_seconds: 3.0")
    print(f"  silence_block_seconds: 3.0")
    print(f"  voiced_duration_seconds: {voiced_seconds}")
    print(f"  expected_pause_fraction: {mixed_pause_fraction:.4f}")
    print(f"  expected_f0_hz_in_voiced_segments: {F0_HZ}")
    print(f"  expected_speaking_rate_wpm_at_132_words: {expected_mixed_wpm:.2f}")
    print("")

    return 0


if __name__ == "__main__":
    sys.exit(main())
