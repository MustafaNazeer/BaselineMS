# Synthetic IMU fixture library

Owner: Test Fixture Engineer (`agents/20-test-fixture-engineer.md`).
Phase introduced: Phase 3.
Source code: `app/src/test/java/com/mustafan4x/msbattery/fixtures/`.

## 1. Purpose and scope

This document is the canonical reference for the project's synthetic IMU fixture library. The fixtures drive every TDD assertion in the gait digital signal processing pipeline (Phase 3 onward). The validity of every gait pipeline test ultimately rests on the validity of the fixture it runs against, so this document serves three audiences.

First, the Signal Processing Engineer, who consumes fixtures in tests for the Butterworth filter, the Madgwick orientation filter, the world frame transform, the step detector, the stride pairing, the ZUPT integrator, and the feature extractor. The fixture library exposes a parameterized generator (`SyntheticImu`) and a set of pre canned configurations (`PreCannedFixtures`). Each pre canned configuration has a documented ground truth (number of steps, mean stride length, mean step time, asymmetry index) that is derivable directly from the input parameters, so a passing pipeline test against a fixture is a passing test against a known correct answer.

Second, the QA Engineer in Phase 5, who reconciles the synthetic fixture library against real recordings from the measured walking course. The "Known limitations" section near the end of this document lists what the synthetic generator does not model, so the Phase 5 reviewer can identify which fixture parameters need calibration after real signals are in hand.

Third, future phases that extend the library. The "How to add a new fixture" section names the steps and the verification checks any new fixture must pass before it is added.

The scope of this document is the gait fixtures only. The audio fixture library introduced in Phase 8 will be documented in a separate section of this same file by the Test Fixture Engineer at that time.

## 2. Generator API summary

The parameterized generator is `SyntheticImu` in package `com.mustafan4x.msbattery.fixtures`. It is a value class whose constructor accepts the gait parameters listed below, and whose `generate()` method returns a `Sequence<ImuSample>` representing the deterministic IMU trace that those parameters produce.

| Parameter | Type | Units | Meaning |
|---|---|---|---|
| `cadenceStepsPerMinute` | `Double` | steps per minute | Mean cadence over the trial. Two steps per stride. |
| `strideLengthMeters` | `Double` | meters | Mean stride length (one full gait cycle, i.e. two steps). |
| `asymmetryRatio` | `Double` | dimensionless | Ratio of mean dominant step time to mean non dominant step time. 1.0 is perfectly symmetric. |
| `stepTimeCv` | `Double` | dimensionless | Coefficient of variation of step times around the mean. 0.0 is metronome perfect. |
| `durationSeconds` | `Double` | seconds | Trial duration. The project's gait trial is 30 seconds per `SPEC.md` Section 6.2. |
| `noiseLevelMps2` | `Double` | meters per second squared | Standard deviation of additive Gaussian noise on each accelerometer axis. |
| `sampleRateHz` | `Double` | hertz | Sample rate. The project's nominal capture rate is 100 Hz per `SPEC.md` Section 7.1. |
| `seed` | `Long` | (none) | Seed for the noise random number generator. Two generators with the same parameters and the same seed produce identical sample streams. |

Each emitted `ImuSample` carries a monotonic timestamp (in nanoseconds), a raw `accelerometer` reading (gravity included, device frame), a `gyroscope` reading (device frame), a `linearAcceleration` reading (gravity removed by the modeled platform, device frame), and a `rotationVector` quaternion. The `rotationVector` follows Android `Sensor.TYPE_ROTATION_VECTOR` convention, that is, device frame to world frame (`rotationVector.rotate(deviceVec) = worldVec`); the synthetic generator stores the same convention so unit tests match the Phase 4 wire format.

The phone is modeled as fixed in the front pocket with the screen facing the body. World Z is up. World X is body right. World Y is body forward. Three accelerations are superposed in the world frame: a heel strike Gaussian on world Z at each step time, a lateral sway sinusoid on world X at half the step frequency (one cycle per stride pair), and a piecewise sinusoidal forward propulsion on world Y calibrated so the double integral over one stride equals `strideLengthMeters`. The world frame acceleration is then rotated into device frame using a fixed pocket orientation; gravity is added back to produce the raw accelerometer signal, and the gravity removed signal is preserved in `linearAcceleration`.

### 2.1 Forward propulsion encoding (sin squared velocity per inter mid stance segment)

The forward propulsion model is the centerpiece of the gait pipeline's Phase 3 stride length validation, so its closed form is documented here in detail.

Let `n` be the number of heel strikes the generator emits over the trial. The generator places `n + 1` mid stance instants `m_0, m_1, ..., m_n` such that segment `k` (from `m_k` to `m_{k+1}`) contains exactly one heel strike `t_k` at its midpoint. The interior mid stances are placed at the midpoint between consecutive heel strikes (`m_k = (t_{k-1} + t_k) / 2` for `k = 1, ..., n - 1`); the boundary mid stances `m_0` and `m_n` are extrapolated by half the adjacent inter step interval beyond the first and last heel strike.

Within each segment of duration `T_seg`, the forward velocity profile is a sin squared shape that starts and ends at zero:

```
v(tau) = (strideLengthMeters / (2 * T_seg)) * (1 - cos(2 * pi * tau / T_seg))
```

where `tau = t - m_k` is the time within the segment. The closed form forward acceleration is the time derivative:

```
a(tau) = (pi * strideLengthMeters / T_seg^2) * sin(2 * pi * tau / T_seg)
```

**Peak forward velocity per segment:** `v_peak = strideLengthMeters / T_seg`, reached at the segment midpoint (`tau = T_seg / 2`), which is the heel strike instant.

**Per segment displacement:** `int(v dtau) = strideLengthMeters / 2` (one step length).

**Per stride displacement:** Two consecutive segments cover one full stride, so `int(v dtau) = strideLengthMeters` exactly (continuous form), or within roughly 0.3 percent for the trapezoidal double integration applied at the project's 100 Hz sample rate over typical stride durations of 0.92 to 1.50 seconds.

**Boundary conditions:** Forward velocity is exactly zero at every mid stance instant `m_k`, which is the boundary condition the project's ZUPT integrator (`SPEC.md` Section 7.1 step 8) assumes. Forward acceleration is continuous (no step jumps at segment boundaries) and is exactly zero at every heel strike (which lie at segment midpoints `tau = T_seg / 2`, where `sin(pi) = 0`).

**Caveat on amplitude.** The forward acceleration peak amplitude scales as `pi * strideLengthMeters / T_seg^2`; for the `healthyControlNormal` fixture this is approximately 16.7 m per second squared, larger than the world Z heel strike Gaussian peak of 5.0 m per second squared. The consequence is that the local minima of the synthetic signal's three axis acceleration magnitude fall AT the heel strike instants rather than between them (because the forward axis is comparatively quiet there). The pipeline's mid stance detection (`SPEC.md` Section 7.1 step 8: "mid stance instants are detected from local minima of acceleration magnitude") therefore needs to either operate on a low pass filtered or band passed magnitude signal that suppresses the high frequency forward propulsion content, or detect mid stances on the smoothed forward velocity envelope rather than on raw magnitude. This is a known synthetic versus real signal divergence and is listed in the Known limitations section below for the Phase 5 reviewer.

The generator is allocation tolerant rather than allocation free. It is intended for tests, not for production hot paths. The Performance Engineer's allocation discipline (`docs/perf/latency-budgets.md`) applies to the production DSP code, not to the fixture library.

## 3. Pre canned fixtures

The following table lists every pre canned fixture that exists today. Each entry names the function in `PreCannedFixtures`, the parameter values, the derived ground truth values, the published reference range that justifies the parameter choice, and the realism caveats specific to that fixture.

The Givon et al. 2009 (`Gait Posture` 29(1):138 to 142) MS versus healthy control gait reference values are the primary clinical anchor. Givon et al. report MS step length 45.3 cm and cadence 94.4 steps per minute, versus healthy control step length 72.1 cm and cadence 115.2 steps per minute. Stride length is twice step length per gait literature convention, so the Givon healthy control stride length is 1.442 m and the Givon MS stride length is 0.906 m. The Bea et al. 2025 systematic review (`Journal of Functional Morphology and Kinesiology` 10(2):133, DOI 10.3390/jfmk10020133) reports validity for spatial parameters of r = 0.42 to 0.90 against gold standard reference systems and standard error of measurement of 2.1 to 7.8 percent of walking distance (in two of 54 reviewed studies); these ranges are used as realism context, not as parameter values, since the project's per fixture parameter choices are deterministic ground truth rather than measurement uncertainty distributions.

### 3.1 `healthyControlNormal`

| Field | Value |
|---|---|
| `cadenceStepsPerMinute` | 115.2 |
| `strideLengthMeters` | 1.442 |
| `asymmetryRatio` | 1.0 |
| `stepTimeCv` | 0.03 |
| `durationSeconds` | 30.0 |
| `noiseLevelMps2` | 0.0 |
| `sampleRateHz` | 100.0 |
| Default `seed` | 1L |

Ground truth derivable from the parameters:

- Total steps: 115.2 * 30 / 60 = 57 steps over the trial.
- Mean step time: 60 / 115.2 = 0.521 seconds.
- Mean stride time: 1.042 seconds.
- Stride asymmetry index (symmetric mean denominator): 0.0.
- Total distance: 57 * (1.442 / 2) = 41.1 m.

Realism rationale: cadence 115.2 and stride length 1.442 m are the Givon et al. 2009 healthy control reference values, restated in `docs/source/clinical-references.md` and `SPEC.md` Section 7.1.1. A step time CV of 3 percent is at the low end of normal adult gait variability; clean fixed cadence walking on a level surface with no perturbations is the cleanest signal the gait pipeline will see, which is what this fixture represents.

Caveats: zero noise. The synthetic signal is cleaner than any real recording. The pipeline's tolerance to noise is exercised by `noisyMsNormal` and by the noise parameter on the generator directly.

### 3.2 `msTypicalNormal`

| Field | Value |
|---|---|
| `cadenceStepsPerMinute` | 94.4 |
| `strideLengthMeters` | 0.906 |
| `asymmetryRatio` | 1.0 |
| `stepTimeCv` | 0.05 |
| `durationSeconds` | 30.0 |
| `noiseLevelMps2` | 0.0 |
| `sampleRateHz` | 100.0 |
| Default `seed` | 2L |

Ground truth:

- Total steps: 94.4 * 30 / 60 = 47 steps over the trial.
- Mean step time: 60 / 94.4 = 0.636 seconds.
- Mean stride time: 1.271 seconds.
- Stride asymmetry index: 0.0.
- Total distance: 47 * (0.906 / 2) = 21.3 m.

Realism rationale: cadence 94.4 and stride length 0.906 m are the Givon et al. 2009 MS reference values. The Clinical Validator's Phase 0 sign off for the Gait Test (`docs/source/clinical-references.md`, "Gait Test, status: approved with caveats") notes that 30 seconds at this cadence yields about 47 steps, which is approximately 23 strides per leg, near the lower bound of stride count for stable mean and CV estimation. The 5 percent step time CV is slightly higher than the healthy control reference, consistent with MS gait being more variable; the project does not have a single MS specific step time CV citation, so this 5 percent value is a conservative middle estimate within the broader gait variability literature.

Caveats: zero noise. The Clinical Validator's caveat that any real session with fewer than 20 detected strides should be excluded from feature trend calculations applies here at the synthetic level too; pipeline tests that depend on stride count statistics should prefer this fixture over slower configurations.

### 3.3 `slowWalk`

| Field | Value |
|---|---|
| `cadenceStepsPerMinute` | 80.0 |
| `strideLengthMeters` | 0.85 |
| `asymmetryRatio` | 1.0 |
| `stepTimeCv` | 0.06 |
| `durationSeconds` | 30.0 |
| `noiseLevelMps2` | 0.0 |
| `sampleRateHz` | 100.0 |
| Default `seed` | 3L |

Ground truth:

- Total steps: 80 * 30 / 60 = 40 steps over the trial.
- Mean step time: 60 / 80 = 0.750 seconds.
- Mean stride time: 1.500 seconds.
- Stride asymmetry index: 0.0.
- Total distance: 40 * (0.85 / 2) = 17.0 m.

Realism rationale: this fixture brackets the slow tail of the project's target population. Cadence 80 is below the Givon MS mean of 94.4, representing an MS patient with more pronounced gait slowing but still within the project's target scope (mild to moderate MS, per `SPEC.md` Section 3). The 750 ms mean step time is comfortably below the project's pipeline maximum inter peak distance ceiling of 800 ms (per `SPEC.md` Section 7.1 step 5). A patient walking at a cadence below 75 steps per minute would have a mean step time above 800 ms and would fall outside the pipeline's design envelope. The Clinical Validator's Phase 0 caveat 4 for the Gait Test names this constraint explicitly. The 6 percent step time CV is consistent with MS gait variability in the slower tail of the population.

Caveats: this fixture is at the boundary of the pipeline's design envelope. Tests using this fixture are exercising the slow end of the supported range, not the typical case.

### 3.4 `briskWalk`

| Field | Value |
|---|---|
| `cadenceStepsPerMinute` | 130.0 |
| `strideLengthMeters` | 1.55 |
| `asymmetryRatio` | 1.0 |
| `stepTimeCv` | 0.025 |
| `durationSeconds` | 30.0 |
| `noiseLevelMps2` | 0.0 |
| `sampleRateHz` | 100.0 |
| Default `seed` | 4L |

Ground truth:

- Total steps: 130 * 30 / 60 = 65 steps over the trial.
- Mean step time: 60 / 130 = 0.462 seconds.
- Mean stride time: 0.923 seconds.
- Stride asymmetry index: 0.0.
- Total distance: 65 * (1.55 / 2) = 50.4 m.

Realism rationale: this fixture brackets the brisk tail of the project's target population. Cadence 130 is above the Givon healthy control mean of 115.2, representing a healthy participant walking briskly during a self selected pace experiment (the Phase 5 walking course validates at slow, normal, and brisk paces per `SPEC.md` Section 7.2). A 2.5 percent step time CV is at the low end of normal adult gait variability, consistent with confident brisk walking on a level surface. The 462 ms mean step time is comfortably above the pipeline minimum inter peak distance of 250 ms.

Caveats: this fixture exercises the upper tail of cadence the pipeline is expected to handle. The synthetic heel strike Gaussian width parameter (sigma = 40 ms) is fixed across all fixtures and could in principle interact with very fast cadences; the `SyntheticImuTest.kt` peak count test verifies that the fixture at cadence 100 produces the expected number of distinguishable peaks, but cadence 130 is closer to the merging threshold. If a future fixture pushes cadence further (above 140), the heel strike sigma may need to be tightened.

### 3.5 `mildAsymmetry`

| Field | Value |
|---|---|
| `cadenceStepsPerMinute` | 100.0 |
| `strideLengthMeters` | 1.30 |
| `asymmetryRatio` | 1.10 |
| `stepTimeCv` | 0.04 |
| `durationSeconds` | 30.0 |
| `noiseLevelMps2` | 0.0 |
| `sampleRateHz` | 100.0 |
| Default `seed` | 5L |

Ground truth:

- Total steps: 100 * 30 / 60 = 50 steps over the trial.
- Geometric mean step time across both feet: 60 / 100 = 0.600 seconds.
- Dominant foot mean step time: 0.600 * sqrt(1.10) = 0.62929 seconds.
- Non dominant foot mean step time: 0.600 / sqrt(1.10) = 0.57207 seconds.
- Dominant over non dominant ratio: 0.62929 / 0.57207 = 1.10000 (matches `asymmetryRatio` exactly).
- Stride asymmetry index (symmetric mean denominator): (0.62929 - 0.57207) / 0.60068 = 0.0953.
- Total distance: 50 * (1.30 / 2) = 32.5 m.

Realism rationale: cadence and stride length sit between the Givon MS and healthy control reference values, representing an MS patient with mild gait slowing and mild asymmetry. The asymmetry ratio of 1.10 means the dominant foot's mean step time is exactly 10 percent longer than the non dominant foot's mean step time (the geometric square root pairing `dominantStepTime = baseMean * sqrt(asymmetryRatio)` and `nonDominantStepTime = baseMean / sqrt(asymmetryRatio)` makes the ratio exact while preserving the geometric mean cadence). This level of asymmetry is plausible in mild MS gait but is not from a single MS specific paper; the project does not have a published smartphone tap or gait asymmetry distribution to anchor on. The Clinical Validator's Phase 2 sign off (`docs/source/clinical-references.md`, "Phase 2 sign off, Bilateral Tap Test", Open question 2) notes that the symmetric mean denominator formula is the convention the project uses for asymmetry indices, including in the gait pipeline; this fixture's ground truth uses that convention.

Caveats: the 1.10 asymmetry ratio is a smartphone adaptation choice, not a published MS gait reference. Pipeline tests that depend on a precise asymmetry index ground truth should treat this fixture as a deterministic synthetic anchor, not as a clinical reference.

### 3.6 `severeAsymmetry`

| Field | Value |
|---|---|
| `cadenceStepsPerMinute` | 90.0 |
| `strideLengthMeters` | 1.05 |
| `asymmetryRatio` | 1.30 |
| `stepTimeCv` | 0.08 |
| `durationSeconds` | 30.0 |
| `noiseLevelMps2` | 0.0 |
| `sampleRateHz` | 100.0 |
| Default `seed` | 6L |

Ground truth:

- Total steps: 90 * 30 / 60 = 45 steps over the trial.
- Geometric mean step time across both feet: 60 / 90 = 0.66667 seconds.
- Dominant foot mean step time: 0.66667 * sqrt(1.30) = 0.76012 seconds.
- Non dominant foot mean step time: 0.66667 / sqrt(1.30) = 0.58471 seconds.
- Dominant over non dominant ratio: 0.76012 / 0.58471 = 1.30000 (matches `asymmetryRatio` exactly).
- Stride asymmetry index (symmetric mean denominator): (0.76012 - 0.58471) / 0.67242 = 0.2609.
- Total distance: 45 * (1.05 / 2) = 23.6 m.

Realism rationale: this fixture represents the high end of asymmetry the project plausibly sees. Cadence 90 is slightly below the Givon MS mean, and stride length 1.05 m is above the Givon MS mean of 0.906 m, reflecting a participant whose primary gait deficit is asymmetry rather than overall slowing. The asymmetry ratio of 1.30 means the dominant foot's mean step time is exactly 30 percent longer than the non dominant foot's (the geometric square root pairing `dominantStepTime = baseMean * sqrt(asymmetryRatio)` and `nonDominantStepTime = baseMean / sqrt(asymmetryRatio)` makes the ratio exact while preserving the geometric mean cadence). The 8 percent step time CV reflects the higher variability typical of asymmetric gait. With the geometric square root pairing, the dominant foot mean step time of 760 ms remains under the pipeline maximum inter peak distance ceiling of 800 ms, so the full 45 step count is recovered without max gap rejections (subject to the `stepTimeCv` jitter, which can occasionally push individual step times above the ceiling).

Caveats: the 1.30 asymmetry ratio is a smartphone adaptation choice that brackets the high end of asymmetry the project plausibly handles within its envelope rather than a published MS gait reference. Pipeline tests that depend on a precise asymmetry index ground truth should treat this fixture as a deterministic synthetic anchor, not as a clinical reference.

### 3.7 `noisyMsNormal`

| Field | Value |
|---|---|
| `cadenceStepsPerMinute` | 94.4 |
| `strideLengthMeters` | 0.906 |
| `asymmetryRatio` | 1.0 |
| `stepTimeCv` | 0.05 |
| `durationSeconds` | 30.0 |
| `noiseLevelMps2` | 0.5 |
| `sampleRateHz` | 100.0 |
| Default `seed` | 7L |

Ground truth:

- Same gait ground truth as `msTypicalNormal` (same cadence, stride length, asymmetry, step time CV).
- Plus additive Gaussian noise of standard deviation 0.5 m per second squared on each accelerometer axis.

Realism rationale: this fixture pairs with `msTypicalNormal` to exercise the pipeline's robustness to sensor noise. A 0.5 m per second squared noise level is roughly 5 percent of gravity, which is the order of magnitude of consumer phone accelerometer noise as quantified in the smartphone gait literature (see Bea et al. 2025 for the broader noise context across 54 reviewed studies). The ground truth gait parameters are unchanged from `msTypicalNormal`, so the pipeline's feature outputs on this fixture should converge to the `msTypicalNormal` outputs as the noise level approaches zero, and the Phase 3 tests that exercise both fixtures together verify that property.

Caveats: 0.5 m per second squared is a single calibration point. Real consumer phone IMUs have spectrally colored noise (low frequency drift plus high frequency shot noise) that this generator does not model. Phase 5 fixture calibration should compare the synthetic noise against measured device noise from the real walking course recordings and adjust the noise model if the pipeline's behavior diverges between synthetic and real.

## 4. How to add a new fixture

When a future phase needs a fixture configuration not covered above, the Test Fixture Engineer (or the agent dispatched into the Test Fixture Engineer role) should follow these steps in order.

1. State the use case in plain prose. Why does the existing library not cover this need? Examples of valid use cases: a Phase 5 fixture matching a specific real recording's mean cadence and stride length within 1 percent, so that pipeline output can be compared between the synthetic anchor and the real recording; a Phase 11 fixture representing the slowest cadence the project commits to supporting after empirical real world data shifts the design envelope.
2. Identify the published reference, real recording, or mathematically constructed signal that justifies each parameter value. Per the role brief, fictional ground truth produces tests that pass for the wrong reasons. If a parameter cannot be justified, the fixture cannot be added.
3. Derive the ground truth values by hand or by formula from the parameters, in the same form as the entries above. The ground truth must be reproducible by inspection; it must not be observed by running the generator and reading the output.
4. Add the new function to `PreCannedFixtures.kt` using the same call shape as the existing fixtures. Choose a seed that does not collide with any existing fixture's default seed (1L through 7L are taken).
5. Add a corresponding entry to this document under section 3, including parameters, ground truth, realism rationale, and caveats.
6. Run `JAVA_HOME=/snap/android-studio/209/jbr ./gradlew :app:testDebugUnitTest --tests "com.mustafan4x.msbattery.fixtures.*"` to confirm the existing test suite still passes.
7. If the new fixture exercises a part of the parameter space the existing `SyntheticImuTest.kt` does not cover (for example, a noise level much higher than 0.5, or a duration not equal to 30 seconds), add a round trip test in `SyntheticImuTest.kt` that verifies the parameter set produces the expected ground truth. Do not assert the round trip by reading the output of the generator; assert it against the closed form ground truth derived in step 3.
8. Commit the new fixture and the documentation update in a single commit named `fixtures(gait): add <fixture-name>`.

A new fixture that fails any of these steps does not enter the library.

## 5. Known limitations

These are the things the synthetic IMU generator does not model. They are listed so the Phase 5 reviewer can reconcile against real recordings without re reading the Phase 3 plan, and so the Signal Processing Engineer knows where the test coverage ends and the real world begins.

1. **Turn dynamics.** The generator models straight line walking only. A real walking course recording at the start, the end, or a mid trial 180 degree turn will have lateral acceleration and yaw rate signatures that the synthetic generator does not produce. The project's gait module is specified for straight line walking only (`SPEC.md` Section 7.1, with cumulative yaw drift treated as a quality score factor); turn analysis is captured in `future-ideas.md` as a deferred feature.
2. **Surface variation.** The generator assumes a flat level surface. Real recordings on carpet, on uneven pavement, or on a graded surface will have different vertical acceleration profiles than the synthetic Gaussian heel strikes. The Phase 5 walking course is specified as a flat marked corridor (`SPEC.md` Section 7.2), so this limitation is consistent with the project's validation envelope.
3. **Device specific noise spectra.** The generator's noise model is white Gaussian noise with the same standard deviation on all axes. Real consumer phone IMUs have spectrally colored noise: low frequency drift from temperature and bias, and high frequency shot noise. Different phone models have different noise characteristics. The Phase 5 review should compare the synthetic noise to noise measured during the real walking course recordings and decide whether the noise model needs to be replaced with a more realistic colored noise model.
4. **Heel strike pulse shape.** The generator models each heel strike as a narrow Gaussian (sigma = 40 ms) on the world Z axis. Real heel strikes have an asymmetric profile with a sharper rising edge than falling edge, and the precise shape varies with footwear, body mass, and walking surface. The pipeline's Butterworth low pass at 20 Hz should attenuate most of the high frequency content that distinguishes real from synthetic pulses, but step detection tests that rely on the precise pulse shape (rather than just the timing of the peak) are sensitive to this difference.
5. **Lateral sway and forward propulsion amplitudes.** The lateral sway uses a fixed amplitude of 1.0 m per second squared on world X. The forward propulsion amplitude is no longer fixed: it is determined by the closed form `pi * strideLengthMeters / T_seg^2` per inter mid stance segment, so it varies with cadence and stride length per fixture. For the `healthyControlNormal` fixture this peak is roughly 16.7 m per second squared, larger than the 5.0 m per second squared world Z heel strike Gaussian peak. The lateral 1.0 amplitude is a placeholder that produces a recognizable left versus right alternation signature; the pipeline's stride pairing logic depends on the sign of the lateral acceleration, not on its absolute magnitude, so the placeholder amplitude is sufficient for the Phase 3 tests. The forward amplitude is constrained by the requirement that the per stride displacement integrate to `strideLengthMeters` under the project's ZUPT modeling assumption (velocity zero at every mid stance). The consequence (documented in Section 2.1) is that magnitude minima of the synthetic three axis acceleration fall AT heel strike instants rather than between them, so a pipeline mid stance detector that operates on raw magnitude minima would mislocate mid stances on this fixture; the Phase 3 GaitPipeline integration is expected to detect mid stances on a smoothed or band passed signal that suppresses the high frequency forward propulsion content. The Phase 5 review should compare this synthetic forward amplitude against measured device forward acceleration during the real walking course recordings; if real forward amplitudes are smaller than the model's, the Phase 5 reviewer should consider whether the project's ZUPT boundary assumption (`v = 0` at mid stance) needs to be replaced with a non zero residual velocity assumption that better matches biomechanical reality.
6. **Stride length variability.** The generator produces strides of constant length (the parameter `strideLengthMeters` is the mean and is also the value for every inter mid stance segment, so each segment integrates to exactly `strideLengthMeters / 2` and each stride to exactly `strideLengthMeters`). Real walkers have stride to stride length variation that the project's pipeline should ultimately measure. The current fixture library does not exercise stride length variability; if a Phase 3 test needs to verify the pipeline's stride length CV output, a new parameter (`strideLengthCv`) would need to be added to the generator (it would jitter the per segment displacement target by a Gaussian with the requested coefficient of variation around `strideLengthMeters / 2`). This is recorded as a gap for the Test Fixture Engineer's Phase 5 review.
7. **`rotationVector` quaternion convention is Android's, not custom.** The generator stores `rotationVector` as the device to world rotation (i.e., `rotationVector.rotate(deviceVec) = worldVec`), matching Android `Sensor.TYPE_ROTATION_VECTOR`. To recover gravity in the device frame from the synthetic samples, callers use `rotationVector.inverse().rotate(Vector3(0, 0, 9.80665))`; that is what `SyntheticImuTest.kt`'s round trip test does. This is documented because it is the load bearing convention every consumer of `ImuSample.rotationVector` (DSP code in Phase 3 and the Sensor Integration Engineer in Phase 4) follows.
8. **Single fixed orientation.** The generator's pocket orientation is fixed for the entire trial. Real recordings have small orientation drift as the phone shifts in the pocket and as the leg swings during walking. The Madgwick orientation filter is the project's defense against orientation drift; a fixture that exercises drift directly is a Phase 5 calibration item, not a Phase 3 baseline.

The Phase 5 walking course experiment is the correct venue for revisiting this list. The Test Fixture Engineer's Phase 5 role is to compare the synthetic library against real recordings and to tune the generator's parameters where the synthetic and real signals diverge in a way that affects pipeline behavior.

## 6. References

- Givon U, Zeilig G, Achiron A. 2009. Gait analysis in multiple sclerosis: characterization of temporal spatial parameters using GAITRite functional ambulation system. `Gait Posture` 29(1):138 to 142. Source of MS versus healthy control cadence and step length reference values used in the `healthyControlNormal` and `msTypicalNormal` fixtures.
- Bea T, Chaabene H, Freitag CW, Schega L. 2025. Open access systematic review of psychometric characteristics of smartphone based gait analyses in chronic health conditions. `Journal of Functional Morphology and Kinesiology` 10(2):133. DOI 10.3390/jfmk10020133. PMID 40566429. Source of the validity and standard error of measurement ranges used as realism context for the noise level parameter.
- Comber et al. 2017, `Gait Posture`. Meta analysis of MS gait spatiotemporal parameters. Complementary to Givon 2009; cited in `SPEC.md` Section 7.1.1 as a complementary source for the per stride feature design choice.
- Clinical Validator Phase 0 Gait Test sign off (`docs/source/clinical-references.md`). Source of the stride count adequacy caveat (47 steps in 30 seconds at the Givon MS mean) and the maximum inter peak distance ceiling (800 ms).
- Clinical Validator Phase 2 Bilateral Tap Test sign off (`docs/source/clinical-references.md`). Source of the symmetric mean denominator asymmetry index convention used across both the tap and gait modules.
- `SPEC.md` Section 6.2 (gait test summary) and Section 7 (gait module deep dive). Source of the 30 second trial duration, the 100 Hz capture rate, the 250 ms minimum and 800 ms maximum inter peak distance constraints, and the straight line walking scope.
