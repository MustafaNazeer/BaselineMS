# Gait validation report, BaselineMS

**Owner:** Documentation Engineer (`agents/14-documentation-engineer.md`), with Signal Processing Engineer and QA Engineer co authors in Phase 5 Part B.
**Phase:** Phase 5. Scaffolded in Part A; result tables and discussion populated in Part B.
**Status (2026-05-08):** Part A scaffolding landed. Methodology and limitations sections written in full; result sections carry TBD placeholders that Part B replaces with measured numbers. The synthetic ground truth section (Section 3) and the references section (Section 10) are populated now from existing test artifacts and from the project's bibliography respectively.

This document is the project's canonical writeup of the gait pipeline's measured accuracy and reliability. It is written to be read on its own by a stranger to the project, with cross references into `SPEC.md`, `docs/qa/statistical-methods.md`, `docs/qa/fixtures.md`, and `docs/source/clinical-references.md` for any numbered claim a reader wants to audit.

## 1. Purpose

This report tells the user, the user's neurologist (if the user chooses to share the application's PDF report), and a future reader of the project (an interview reviewer, a code reviewer, a follow on contributor) what accuracy and what test retest reliability the gait pipeline achieves on synthetic ground truth, on a measured 25 meter walking course, and across 5 repeated weekly sessions per participant. It also tells the same readers, plainly, what the validation does not cover.

The report is positioned as a wellness validation, not a clinical claim. The application is not a medical device. Per `SPEC.md` Section 4 non goal 1 and Section 10 onboarding disclaimer, the validation numbers in this report describe how closely the application's outputs match measured ground truth in a small healthy or mixed cohort under controlled conditions; they do not justify a diagnostic, prognostic, or treatment claim. A neurologist who reads the user's exported PDF report should treat the application's outputs as supplementary context, not as a replacement for clinical assessment.

## 2. Pipeline summary

The gait pipeline takes a 30 second straight line walk recorded at 100 Hz from the phone in the user's front pocket and produces five per stride features: cadence, stride length, step time variability, stride asymmetry, and double support time. The capture path uses Android's `SensorManager` with `Sensor.TYPE_LINEAR_ACCELERATION`, `Sensor.TYPE_GYROSCOPE`, and `Sensor.TYPE_ROTATION_VECTOR`. Orientation is estimated both from the platform's fused `TYPE_ROTATION_VECTOR` quaternion and from a Madgwick filter run on raw gyro plus accelerometer; either estimate suffices to recover vertical, forward, and lateral acceleration components in the world frame. A 4th order zero phase Butterworth filter at 20 Hz suppresses high frequency noise without smearing step events. Step detection runs as a peak finder on vertical linear acceleration with a minimum prominence, a minimum inter peak distance of 250 ms, and a maximum inter peak distance of 800 ms. Left versus right pairing uses the sign of lateral acceleration. Stride length is recovered by the zero velocity update method between detected mid stance instants, with velocity reset to zero at each mid stance to bound integration drift.

The full per step pipeline, including the rationale for each choice and the relevant clinical references, is documented in `SPEC.md` Section 7 (Section 7.1 lists the nine pipeline stages, Section 7.1.1 states why the project reports per stride features rather than the global distance metric Floodlight Open's Two Minute Walk Test reports, and Section 7.2 states the three layer validation strategy this report measures against). The synthetic generator that drives the integration tests in Section 3 below is documented in `docs/qa/fixtures.md`. The pipeline source lives under `app/src/main/java/com/mustafan4x/baselinems/dsp/`. This section is a summary; this report does not duplicate the technical content of those references.

## 3. Synthetic ground truth results (Part A populated)

The synthetic ground truth layer is the first of the three validation layers in `SPEC.md` Section 7.2. The pipeline is run end to end against the seven pre canned fixtures in `app/src/test/java/com/mustafan4x/baselinems/fixtures/PreCannedFixtures.kt`, each of which carries a closed form ground truth derivable from its parameters per `docs/qa/fixtures.md` Section 3. The recovered features are compared to the ground truth and the percent error per fixture is asserted in `app/src/test/java/com/mustafan4x/baselinems/dsp/GaitPipelineIntegrationTest.kt`. The numbers below are transcribed from `STATUS.md` Phase 3 close (the four original fixtures: `healthyControlNormal`, `msTypicalNormal`, `mildAsymmetry`, `noisyMsNormal`) and from the Phase 5 Part A Task A1 commit at `25a0a27` plus `docs/qa/fixtures.md` Section 7 (the three new fixtures: `slowWalk`, `briskWalk`, `severeAsymmetry`).

This section is not a statistical estimate from a sample of human participants. It is deterministic recovery of the pipeline's output against synthetic ground truth, so no confidence interval is reported (per `docs/qa/statistical-methods.md` Section 1.2 item 3). The numbers are point recovery percentages.

### 3.1 Cadence and stride length recovery on the seven fixtures

| Fixture | Ground truth cadence (steps per minute) | Cadence percent error | Ground truth stride length (m) | Stride length percent error |
|---|---|---|---|---|
| `healthyControlNormal` | 115.2 | 1.01 percent | 1.442 | 0.26 percent |
| `msTypicalNormal` | 94.4 | 2.51 percent | 0.906 | 0.18 percent |
| `mildAsymmetry` | 100.0 | not asserted in this case (asymmetry index is the targeted feature; see Section 3.2) | 1.30 | not asserted in this case |
| `noisyMsNormal` | 94.4 | 0.39 percent | 0.906 | 0.39 percent |
| `slowWalk` | 80.0 | 2.47 percent | 0.85 | 0.27 percent |
| `briskWalk` | 130.0 | 1.51 percent | 1.55 | 0.39 percent |
| `severeAsymmetry` | 90.0 | 2.19 percent | 1.05 | 0.78 percent |

All seven cadence and stride length numbers are inside the Phase 3 acceptance bands of cadence within 5 percent and stride length within 5 percent. The largest cadence error is 2.51 percent on `msTypicalNormal`; the largest stride length error is 0.78 percent on `severeAsymmetry`. The synthetic recovery accuracy is therefore well inside the SPEC's target bands of 3 percent on cadence and 5 percent on stride length, on synthetic data; the more demanding test of the pipeline is the real walking course in Section 5, which is what determines whether the SPEC's success criterion 2 (`SPEC.md` Section 16.2) is met.

### 3.2 Stride asymmetry index recovery

| Fixture | Ground truth asymmetry index | Recovered asymmetry index | Within band (0.025 absolute) |
|---|---|---|---|
| `mildAsymmetry` | 0.0953 | 0.0881 | yes (0.0072 absolute, well inside 0.025) |
| `severeAsymmetry` | 0.2609 | 0.1127 | NO (0.1482 absolute, 5.9 times the 0.025 band; assertion deferred to Phase 5 calibration) |

The `mildAsymmetry` fixture's recovered asymmetry index is inside the 0.025 absolute Phase 3 acceptance band, with an error of 0.0072. The `severeAsymmetry` fixture's recovered asymmetry index falls outside the band, and the asymmetry assertion in `GaitPipelineIntegrationTest` is intentionally deferred to Phase 5 calibration rather than weakened to a passing tolerance. The divergence is documented in `docs/qa/fixtures.md` Section 7.3 and traced to two coupled causes: the synthetic generator's continuous half cadence lateral sway and the pipeline's median step interval lateral sampler interact poorly at high asymmetry, so the recovered left versus right step time means converge toward each other and shrink the recovered asymmetry index.

The Phase 5 calibration session that picks up the deferred item (when real walking course recordings arrive) chooses among three resolution paths laid out in `docs/qa/fixtures.md` Section 7.3: a step time aware lateral sway in the synthetic generator, a per step adaptive offset in the pipeline's stride pairing logic, or accepting the divergence and documenting it as a known synthetic only artifact if the pipeline recovers asymmetry correctly on real signals. The choice cannot be made in Part A because the real signal anchor does not yet exist.

### 3.3 Quality score thresholds

Every fixture in Section 3.1 above produced a quality score above the project's 0.7 floor for valid sessions, including the noisy fixture (`noisyMsNormal`) which the integration test asserts above 0.7 by transitive structure of the test's tolerance assertions on cadence and stride length. The clean fixtures (`healthyControlNormal` and `msTypicalNormal`) produced quality scores above 0.8 and 0.7 respectively per the assertions in `GaitPipelineIntegrationTest`. The quality score's empirical distribution on real walking course recordings is reported in Section 5 (Part B).

## 4. Real walking course methodology (Part A written in full)

The real walking course is the second of the three validation layers in `SPEC.md` Section 7.2. This section locks in the methodology so Phase 5 Part B's data collection follows a single fixed protocol; the methodology does not change once data collection begins.

### 4.1 Course design

A 25 meter straight line walking course is laid out on a flat, level, indoor surface (a hallway, a rented gym strip, or a long unobstructed corridor). Paper markers (printed labels or masking tape) are placed every 25 cm along the floor, forming a regular grid that the video processing in Section 4.4 below uses to convert observed footfalls into total distance walked. The course endpoints are marked clearly so the participant can walk to either end without interpretation, and a 1 meter run in zone is left on each end so the participant reaches steady state cadence before crossing the first marked footfall and decelerates after the last marked footfall.

The course is video recorded from a fixed tripod position at one end, framed so the entire 25 meter span and every paper marker is visible in frame for the entire trial. The video frame rate is 30 frames per second or higher, and the resolution is sufficient to distinguish individual paper markers in the video; a modern phone camera at 1080p or 4k is sufficient.

### 4.2 Participant requirements

Per `SPEC.md` Section 7.2 layer 2, the protocol requires at least 3 participants of varying heights, with at least 20 trials in total across the cohort. Each participant walks the course three times per session, once at each of three self selected paces: slow (the pace the participant would walk if they were tired), normal (the pace the participant would walk on a normal day to a destination), and brisk (the pace the participant would walk if they were running slightly late). The participant chooses each pace; the experimenter does not impose a target cadence or stride length.

The cohort is anchored in healthy controls. MS patient recruitment is aspirational per `SPEC.md` Section 13 (Risks and Mitigations row 3) and is not a precondition for Phase 5 close. If the cohort recruits MS patients, the Section 5 result tables include a per participant breakdown that flags MS status; if the cohort consists only of healthy controls, the report says so plainly in Section 5 and the comparison against the Givon et al. 2009 MS reference values in Section 8 is restricted to the healthy control comparison row.

Each participant signs a brief informed consent indicating that the data is recorded on the participant's own phone, retained on the participant's own device, and used only by the participant for the application's validation; no data leaves the participant's device unless the participant explicitly chooses to share it. The application does not declare the `INTERNET` permission per `SPEC.md` Section 10, so consent under this protocol is straightforward.

### 4.3 Data collection procedure

For each trial, the participant places the phone in the front pocket of their pants per `SPEC.md` Section 7 ("phone in the front pocket"). The phone screen is locked and the application is configured to start gait capture at the press of a hardware button or via a brief countdown; either path satisfies the requirement that the participant not be looking at the phone while walking. The application records 30 seconds of IMU data per trial at the configured 100 Hz nominal capture rate.

The video recording starts before the gait capture begins and ends after the gait capture ends, so the entire 30 seconds of walking is captured on video. The trial is timestamped on both the phone (the application records the gait session start and end times into the on device database) and on the video (the experimenter says the trial number and the pace aloud at the start of the recording, on camera). This dual timestamping lets the Part B video processing align the video derived ground truth to the phone derived recovered features unambiguously.

For each session, the participant's height is recorded (in centimeters), the participant's biological sex is recorded (or marked as undisclosed), the device model is recorded, and the Android OS version is recorded. A short note is appended for each trial describing any deviations from the protocol (a participant who paused mid trial, a marker that was bumped, etc.); deviations beyond a tolerance defined by the QA Engineer at Part B start (a participant who stopped walking, a recording with the phone visibly outside the pocket) cause the trial to be excluded from the headline analysis but are retained in the per trial CSV for transparency.

### 4.4 Video processing procedure

For each video, the QA Engineer plays the video back frame by frame in any standard video editor (`ffmpeg`, `kdenlive`, or equivalent) and counts the participant's heel strikes one at a time. Each heel strike is assigned a frame index. The frame indices, divided by the video frame rate, give a per heel strike timestamp; the count of heel strikes between the start of the gait capture window and the end of the gait capture window gives the ground truth step count for the trial; the ground truth cadence is the step count divided by the trial duration in minutes. The ground truth total distance walked is read off the paper markers in the video: the experimenter records the closest marker to the participant's heel at the start of the capture and the closest marker to the participant's heel at the end of the capture, and the total distance is the marker count multiplied by 25 cm.

A stopwatch validation is performed on a subset of trials as a sanity check: a manual stopwatch run is started at the participant's first heel strike and stopped at the last heel strike of the trial; the manual elapsed time should match the frame derived elapsed time within human reaction time precision (roughly 0.2 seconds). Trials where the manual and the frame derived times disagree by more than 0.5 seconds are flagged for re scoring.

### 4.5 Ground truth feature derivation

Per trial, the ground truth cadence is `step_count / (trial_duration_seconds / 60)` (steps per minute). The ground truth stride length is `total_distance_meters / (step_count / 2)` (meters per stride; one stride is two steps per gait literature convention). Asymmetry index, step time variability, and double support time are not directly measurable from the video at the precision the project's pipeline reports them, so the real walking course validation in Section 5 is restricted to cadence and stride length per `SPEC.md` Section 7.2 layer 2 ("stride length within 5 percent and cadence within 3 percent"). Asymmetry, step time variability, and double support time are validated by the test retest reliability layer in Section 6, which does not require independently measurable ground truth (only consistent estimates across repeated sessions).

## 5. Real walking course results (Part B, TBD)

This section's tables are populated in Phase 5 Part B by the QA Engineer (Task B2 video processing), the Signal Processing Engineer (Task B3 pipeline replay), and the Biostatistics Reviewer (Task B4 error metrics with confidence intervals). Until Part B runs, every cell carries `TBD`. Methodology references point at `docs/qa/statistical-methods.md` Section 4.2 (95 percent confidence interval method on mean percent error) and Section 3 (per trial percent error formula).

### 5.1 Stride length error vs ground truth, by participant and pace (Part B, TBD)

Per `docs/qa/statistical-methods.md` Section 3.2, the primary metric is mean percent error across trials. Per Section 4.2 of that document, the 95 percent CI is computed by t distribution if normality holds and by cluster bootstrap (cluster on participant) otherwise. Per Section 3.4 of that document, per pace and per participant CIs are reported only when the cell trial count supports them; otherwise the cell shows the point estimate alone with a footnote.

| Participant | Pace | Trials | Mean percent error | 95 percent CI lower | 95 percent CI upper |
|---|---|---|---|---|---|
| Participant 1 | slow | TBD | TBD | TBD | TBD |
| Participant 1 | normal | TBD | TBD | TBD | TBD |
| Participant 1 | brisk | TBD | TBD | TBD | TBD |
| Participant 2 | slow | TBD | TBD | TBD | TBD |
| Participant 2 | normal | TBD | TBD | TBD | TBD |
| Participant 2 | brisk | TBD | TBD | TBD | TBD |
| Participant 3 | slow | TBD | TBD | TBD | TBD |
| Participant 3 | normal | TBD | TBD | TBD | TBD |
| Participant 3 | brisk | TBD | TBD | TBD | TBD |
| Pooled (all participants, all paces) | (headline row) | TBD | TBD | TBD | TBD |

### 5.2 Cadence error vs ground truth, by participant and pace (Part B, TBD)

| Participant | Pace | Trials | Mean percent error | 95 percent CI lower | 95 percent CI upper |
|---|---|---|---|---|---|
| Participant 1 | slow | TBD | TBD | TBD | TBD |
| Participant 1 | normal | TBD | TBD | TBD | TBD |
| Participant 1 | brisk | TBD | TBD | TBD | TBD |
| Participant 2 | slow | TBD | TBD | TBD | TBD |
| Participant 2 | normal | TBD | TBD | TBD | TBD |
| Participant 2 | brisk | TBD | TBD | TBD | TBD |
| Participant 3 | slow | TBD | TBD | TBD | TBD |
| Participant 3 | normal | TBD | TBD | TBD | TBD |
| Participant 3 | brisk | TBD | TBD | TBD | TBD |
| Pooled (all participants, all paces) | (headline row) | TBD | TBD | TBD | TBD |

### 5.3 Headline pass or fail (Part B, TBD)

Per `SPEC.md` Section 16.2, the SPEC's success criterion is met when the pooled headline mean percent error for stride length is within 5 percent and the pooled headline mean percent error for cadence is within 3 percent, with the 95 percent CI lower bounds inside those targets per `docs/qa/statistical-methods.md` Section 5. If the point estimates meet the targets but the CI lower bounds do not, the report flags "target met by point estimate; CI lower bound straddles target" rather than declaring success. Per `SPEC.md` Section 13 row 1 and `docs/plans/phase-5-gait-validation-suite.md` Task B13, missing the targets means the report records the actual numbers honestly and the Code Reviewer's Phase 5 verdict is APPROVED WITH NOTES rather than REJECTED. There is no aspirational claim path.

| Feature | SPEC target | Pooled point estimate | 95 percent CI | Pass / straddle / fail |
|---|---|---|---|---|
| Stride length percent error | within 5 percent | TBD | TBD | TBD |
| Cadence percent error | within 3 percent | TBD | TBD | TBD |

## 6. Test retest reliability methodology (Part A written in full)

The test retest reliability layer is the third of the three validation layers in `SPEC.md` Section 7.2. Per layer 3, each participant performs 5 repeated weekly sessions within a short window (the SPEC's wording in Section 9 is "within a short time window"; Phase 5 uses the weekly variant per `docs/qa/statistical-methods.md` Section 2.1 because the application's primary use case is a weekly self administered ritual, and test retest reliability across weeks is the reliability the user actually experiences). Each session is one full gait test under the same conditions: same phone, same pocket placement, same indoor walking corridor, same self selected normal pace.

The chosen ICC variant is **ICC(3,1) absolute agreement**, justified in full in `docs/qa/statistical-methods.md` Section 2.2. Briefly: the rater (the application) is fixed across sessions per participant, so the model is two way mixed; the rater is the specific instrument under evaluation, not a sample of instruments; and the SPEC's use case (longitudinal trend tracking on the same person) is an absolute agreement question, not a consistency question. The threshold for a passing ICC is 0.75 per `SPEC.md` Section 7.2 layer 3 and Section 16.3, which corresponds to the lower bound of the "Good" reliability label in the Koo and Li 2016 banding cited in `docs/qa/statistical-methods.md` Section 2.4.

The 95 percent confidence interval on the ICC point estimate is computed by the analytic F distribution method per `docs/qa/statistical-methods.md` Section 4.1, with a cluster bootstrap as a sanity check. With 3 participants and 5 sessions per participant (the SPEC's stated minimum cohort size), the analytic CI half width is wide; the SPEC threshold is met when the CI lower bound is above 0.75, not when only the point estimate is above 0.75. Per Section 5.3 of the methods document, declaring the threshold met requires the CI lower bound to clear the threshold, and a "point estimate met but CI straddles" outcome is reported as such rather than treated as a pass.

The features reported with ICC are cadence (steps per minute), stride length (meters), step time variability (coefficient of variation of step times), stride asymmetry (asymmetry index), and double support time (seconds). The first three are explicitly named in `SPEC.md` Section 7.2 layer 3; stride asymmetry and double support time are added in this report because the pipeline produces them and Phase 5 Part B's repeated sessions per participant provide the data to estimate their reliability without additional cost. The validation report's discussion in Section 8 below comments on which feature ICCs are most informative.

## 7. Test retest reliability results (Part B, TBD)

This section's table is populated in Phase 5 Part B by the Biostatistics Reviewer (Task B4) per the methodology in Section 6 above and `docs/qa/statistical-methods.md` Section 4.1. Until Part B runs, every cell carries `TBD`.

### 7.1 ICC(3,1) absolute agreement, by feature (Part B, TBD)

| Feature | ICC point estimate | 95 percent CI lower | 95 percent CI upper | Koo and Li 2016 label | SPEC 0.75 threshold (pass / straddle / fail) |
|---|---|---|---|---|---|
| Cadence (steps per minute) | TBD | TBD | TBD | TBD | TBD |
| Stride length (meters) | TBD | TBD | TBD | TBD | TBD |
| Step time variability (CV) | TBD | TBD | TBD | TBD | TBD |
| Stride asymmetry (index) | TBD | TBD | TBD | TBD | TBD |
| Double support time (seconds) | TBD | TBD | TBD | TBD | TBD |

The "pass" verdict requires the 95 percent CI lower bound to be above 0.75 per `docs/qa/statistical-methods.md` Section 5.3. "Straddle" means the point estimate is above 0.75 but the CI lower bound is below 0.75. "Fail" means the point estimate is below 0.75. The verdict per feature is reported alongside a brief plain language sentence so a non statistical reader can interpret the row without reading the methods document.

## 8. Discussion (Part B, structured placeholder)

Phase 5 Part B fills in this section with the actual cohort numbers and a one paragraph synthesis per subsection. Part A leaves the section headers and one sentence prompts so the future session knows what to fill in.

### 8.1 Comparison against Givon et al. 2009 reference values (Part B, TBD)

**Prompt for Part B.** Compare the project's measured cohort means for cadence and stride length against the Givon et al. 2009 reference values: MS cadence 94.4 steps per minute, MS step length 45.3 cm (stride length 0.906 m); healthy control cadence 115.2 steps per minute, healthy control step length 72.1 cm (stride length 1.442 m). State plainly which Givon row the project's measured cohort mean is closer to and why, framed as descriptive context (per `docs/qa/statistical-methods.md` Section 6.4.1, the comparison is informational, not diagnostic). Cite `docs/source/clinical-references.md`, "Gait analysis on smartphone IMU" and "Gait Test, status: approved with caveats. Clinical Validator, Phase 0, 2026-05-07" for the reference value provenance and the Clinical Validator's Phase 0 ratification.

### 8.2 Comparison against Comber et al. 2017 meta analysis (Part B, TBD)

**Prompt for Part B.** Compare the project's measured cohort effect sizes (the standardized differences between MS participants and healthy controls in the cohort, if MS recruitment succeeds) against the standardized mean differences reported in the Comber et al. 2017 meta analysis. If MS recruitment did not succeed, restrict the discussion to the healthy control row and state plainly that the MS comparison is unavailable in this validation pass.

### 8.3 Caveats about synthetic fixture realism (Part B, TBD)

**Prompt for Part B.** Discuss whether the synthetic fixture library's recovered numbers in Section 3 above are predictive of the real walking course recovered numbers in Section 5. Specifically, address the three calibration items from `STATUS.md` Resume notes Phase 5 calibration item 10:

- Item 10(a): the lateral sampling offset (a quarter step interval before each detected step) is a synthetic only quirk because the synthetic generator's lateral sway evaluates to zero exactly at step times. Comment on whether the real walking course recordings show that this offset is needed in production.
- Item 10(b): the Madgwick pre warm with 5000 virtual iterations stands in for the user's standing window. Comment on whether asking the user to stand briefly before walking gives the same convergence opportunity from real samples, and whether the production application should formalize a standing window in the gait test instructions.
- Item 10(c): the FeatureExtractor double support time uses a 20 percent of stride time approximation because the synthetic generator does not model true double support phase boundaries. Comment on whether the test retest reliability of the double support time feature in Section 7 above suggests the approximation is stable enough to publish, or whether the feature should be marked experimental in the README and the in app About screen.

Cite `docs/qa/fixtures.md` Section 5 (Known limitations) for the catalog of synthetic versus real divergences the Phase 5 calibration session should reconcile against real recordings.

### 8.4 The severeAsymmetry asymmetry index miss (Part B, TBD)

**Prompt for Part B.** Discuss the resolution of the deferred `severeAsymmetry` asymmetry index assertion in Section 3.2 above. State which of the three resolution paths in `docs/qa/fixtures.md` Section 7.3 the Phase 5 calibration session chose, why, and whether the chosen path closes the gap. If the path chosen is "accept the divergence and document the limitation" because the real walking course recordings show that the pipeline recovers asymmetry correctly on real signals, state that plainly and cite the relevant Section 5 or Section 7 row of this report as the supporting evidence.

### 8.5 What the validation does and does not justify (Part B, TBD)

**Prompt for Part B.** Close the discussion by stating plainly what claim the validation results justify and what claim they do not. The claim the validation can justify, if the SPEC targets are met, is that the gait pipeline's cadence and stride length outputs are within the SPEC's stated bands of measured ground truth in a small cohort under controlled conditions, and that the test retest reliability of the headline features is at or above the SPEC's threshold across 5 weekly sessions per participant. The claim the validation does not justify, regardless of how well the targets are met, is that the application diagnoses MS, prognoses MS progression, or replaces clinical assessment. Cite `SPEC.md` Section 4 non goal 1 and Section 10 onboarding disclaimer.

## 9. Limitations (Part A written in full)

This section is independent of the actual numbers and is written in full in Part A. It is the reference the README's validation section, the in app About screen, and any future user facing summary of this report should defer to when communicating what the validation does not cover.

### 9.1 Small sample size

Per `SPEC.md` Section 7.2 layer 2, the walking course validation requires at least 20 trials by at least 3 people of varying heights. Per Section 7.2 layer 3, the test retest reliability validation requires 5 repeated weekly sessions per participant. With 3 participants and 5 sessions per participant the test retest sample is 15 measurements per feature, balanced. With 20 walking course trials split across 3 participants and 3 paces, the average per pace per participant cell holds about 2 trials, which is too thin to support per cell confidence intervals (`docs/qa/statistical-methods.md` Section 5.4 item 2). The validation report therefore reports per pace and per participant breakdowns as point estimates only, with the headline pooled CI as the primary inference. The SPEC's at least 3 participants minimum is treated as a floor, not a target, per `docs/qa/statistical-methods.md` Section 5.4 item 1: each additional participant meaningfully reduces the ICC CI half width, and the project recruits a 4th and 5th participant where accessible.

### 9.2 Healthy controls only at the start

The cohort is anchored in healthy controls. MS patient recruitment is aspirational per `SPEC.md` Section 13 (Risks and Mitigations row 3), which acknowledges that recruiting real MS patients for the beta is hard and that the project may rely on healthy controls for the validation pass. If the cohort recruits MS patients, the report's discussion in Section 8 includes the MS comparison rows; if not, the report restricts the discussion to the healthy control comparison row and states the MS comparison is unavailable. The validation results therefore characterize the pipeline's accuracy and reliability on a primarily or entirely healthy control cohort, not on the MS patient population the application is ultimately intended for. Generalization to MS patients is a future validation pass, not a claim of this report.

### 9.3 Single device per participant

Per `docs/qa/statistical-methods.md` Section 2.2.1, the test retest reliability experiment holds device fixed within participant (the same Android phone runs all 5 weekly sessions per participant). This isolates the application's reliability from the cross device variation in IMU sensor characteristics. Cross device generalization, the question of whether the same participant on a different Android phone produces the same numbers, is a separate question addressed by the Phase 4 close multi device sample rate validation in `docs/observability/sensor-runbook.md`, not by this report. The validation report's results therefore bound the application's within device test retest reliability, not its cross device reliability.

### 9.4 Short recording window

The gait test records 30 seconds of walking per trial per `SPEC.md` Section 6.2 and Section 7. Longer continuous walks (60 seconds, 2 minutes, 6 minutes) are a different test surface and are not exercised by the project's validation. The 30 second window is calibrated per `SPEC.md` Section 7.1.1 to capture enough strides for robust feature estimation (about 23 strides per leg at the Givon MS mean cadence) while staying short enough to be tolerable for fatigued MS patients per the Galati et al. 2024 Floodlight UX analysis. The validation does not characterize the pipeline's behavior on longer recordings.

### 9.5 Front pocket placement only

Per `SPEC.md` Section 7 ("phone in the front pocket"), the gait pipeline is designed for the phone in the participant's front pants pocket. Other placements (a hip mounted clip, a hand held phone, a back pocket, a chest pocket) are deferred to future ideas per `SPEC.md` Section 6.2 and are not exercised by the project's validation. The validation results therefore characterize the pipeline's accuracy and reliability with a front pocket placement, not with arbitrary phone positions.

### 9.6 Wellness validation, not a clinical study

The project is positioned as a wellness application per `SPEC.md` Section 4 non goal 3 (the application is wellness or research, not Software as a Medical Device). The validation methodology in this report follows the conventions of clinical biomarker validation (ICC for reliability, percent error for accuracy, comparison against published reference values for context) but the validation is not a clinical study: there is no Institutional Review Board protocol, no clinical investigator, no pre registered hypothesis, no power analysis sized to a clinical endpoint, and no claim made on a regulatory submission. The validation supports a defensible wellness claim, no more.

### 9.7 Synthetic fixture limitations

The synthetic fixture library has documented limitations per `docs/qa/fixtures.md` Section 5: turn dynamics, surface variation, device specific noise spectra, heel strike pulse shape, lateral and forward propulsion amplitudes, stride length variability, the rotationVector quaternion convention, and the single fixed orientation are simplifications that a real walking corridor recording will deviate from. The synthetic ground truth recovery numbers in Section 3 are therefore necessary but not sufficient evidence that the pipeline behaves correctly; the real walking course numbers in Section 5 are the primary evidence. The synthetic fixtures' role is to lock in the pipeline's behavior on a deterministic anchor so that pipeline regressions (a code change that breaks a feature on synthetic data) are caught in CI before the real walking course data has to detect them.

### 9.8 The severeAsymmetry asymmetry index divergence

Per Section 3.2 above and `docs/qa/fixtures.md` Section 7.3, the `severeAsymmetry` synthetic fixture's recovered asymmetry index of 0.1127 misses the ground truth 0.2609 by 5.9 times the 0.025 absolute Phase 3 acceptance band. The integration test for that fixture asserts cadence and stride length only; the asymmetry index assertion is deferred to Phase 5 calibration when real walking course recordings can disambiguate whether the divergence is a synthetic generator artifact, a pipeline limitation, or both. Until the calibration session runs, the validation report does not publish a stride asymmetry accuracy claim against synthetic ground truth at the high asymmetry end of the project's design envelope. The mild asymmetry case (the `mildAsymmetry` fixture, asymmetry index 0.0953 ground truth recovered as 0.0881) is inside the acceptance band and is the primary synthetic anchor for the asymmetry feature.

## 10. References (Part A populated; Part B citation audit pending)

The following references are cited in this document. Cross references against `docs/source/clinical-references.md` are noted for each entry. References newly introduced by this document (not previously listed in `docs/source/clinical-references.md`) are flagged for the Phase 5 Part B Citation Auditor pass; they are listed here as citations that the document needs but should not be treated as audited until that pass closes.

1. **Givon, U., Zeilig, G., Achiron, A. 2009.** Gait analysis in multiple sclerosis: characterization of temporal-spatial parameters using GAITRite functional ambulation system. *Gait Posture* 29(1):138 to 142. Source of the MS versus healthy control reference values used in Section 8.1 and in the synthetic fixture parameter choices for `healthyControlNormal` and `msTypicalNormal` (`docs/qa/fixtures.md` Sections 3.1 and 3.2). Already in `docs/source/clinical-references.md` under "Gait analysis on smartphone IMU" and ratified by the Clinical Validator's Phase 0 sign off for the Gait Test.

2. **Oh, J. et al. 2024.** Floodlight Open, an open access global longitudinal smartphone based study of multiple sclerosis: Cohort characteristics and study design. *Scientific Reports*. DOI 10.1038/s41598-023-49299-4. Source of the Two Minute Walk Test finding (the 2MWT did not significantly distinguish MS from non MS in the cohort after age and sex adjustment), which justifies the project's design choice to compute per stride features rather than a global distance metric per `SPEC.md` Section 7.1.1. Already in `docs/source/clinical-references.md` under "FLOODLIGHT (Roche / Genentech research program)".

3. **Comber, L., Galvin, R., Coote, S. 2017.** Gait deficits in people with multiple sclerosis: a systematic review and meta analysis. *Gait Posture*. Complementary to Givon et al. 2009 as a meta analytic source for MS gait spatiotemporal parameter standardized mean differences. Cited in Section 8.2 as the comparator for the project's measured cohort effect sizes if MS recruitment succeeds. Already cross referenced from `SPEC.md` Section 7.1.1 and `docs/qa/fixtures.md` Section 6. The Citation Auditor's Phase 0 verification pass already flagged Comber 2017 for a precise audit (exact author list, exact volume, exact pages); per `docs/source/clinical-references.md` "Citations the Citation Auditor should verify in Phase 0" item 1 under the Gait Test sign off, that audit should close before Section 8.2 of this report is populated in Part B.

4. **Madgwick, S.O.H. 2010.** An efficient orientation filter for inertial and inertial / magnetic sensor arrays. The orientation filter used in the project's Phase 3 implementation per `docs/adr/0002-madgwick-from-scratch.md`. Cited in Section 2 of this report as the orientation estimation reference. Already in `docs/source/clinical-references.md` under "Gait analysis on smartphone IMU". The Phase 3 Citation Auditor's PARTIAL P3.1 finding (the "eq 11 to eq 33" specificity in the ADR was not pre audited in the bibliography) is open per `STATUS.md` Resume notes Phase 5 calibration item 7 and is pending Phase 5 Part B Citation Auditor close.

5. **Galati, J. et al. 2024.** User Experience of a Large-Scale Smartphone-Based Observational Study in Multiple Sclerosis: Global, Open-Access, Digital-Only Study. *JMIR Human Factors* 11:e57033. DOI 10.2196/57033. The user experience companion paper to Oh et al. 2024. Cited in Section 9.4 of this report (the 30 second window calibration against the Floodlight UX quit reasons). Already in `docs/source/clinical-references.md` under "FLOODLIGHT (Roche / Genentech research program)".

6. **Shrout, P.E. and Fleiss, J.L. 1979.** Intraclass correlations: uses in assessing rater reliability. *Psychological Bulletin* 86(2):420 to 428. Cited indirectly through `docs/qa/statistical-methods.md` Section 2 (ICC variant taxonomy) and Section 4.1 (analytic F distribution CI for ICC(3,1)). The reference is internal to the methods document; this validation report cites it via the methods document rather than reproducing the citation independently. The Citation Auditor's Phase 5 Part B pass on the methods document closes this reference.

7. **Koo, T.K. and Li, M.Y. 2016.** A guideline of selecting and reporting intraclass correlation coefficients for reliability research. *Journal of Chiropractic Medicine* 15(2):155 to 163. Cited indirectly through `docs/qa/statistical-methods.md` Section 2.2 (variant selection guidance) and Section 2.4 (threshold banding). Already in `docs/source/clinical-references.md` under "Statistical methodology references".

8. **Bland, J.M. and Altman, D.G. 1986.** Statistical methods for assessing agreement between two methods of clinical measurement. *The Lancet* 327(8476):307 to 310. Cited indirectly through `docs/qa/statistical-methods.md` Section 3.1 (per trial signed error reporting) and Section 3.5 (limits of agreement as a supplementary analysis). **New reference introduced by `docs/qa/statistical-methods.md`; not previously listed in `docs/source/clinical-references.md`.** Flagged for Phase 5 Part B Citation Auditor pass per `docs/qa/statistical-methods.md` Section 7.1 and the methods document's pending audit item.

### 10.1 Citation Auditor Phase 5 Part B audit pending

The following citation work is pending Phase 5 Part B's Citation Auditor pass per `docs/plans/phase-5-gait-validation-suite.md` Task B8. The validation report's Part B authors should treat each item as an open audit until the auditor closes it.

1. **Comber et al. 2017 precise audit** (this report's Reference 3 above). Open from the Phase 0 Clinical Validator sign off for the Gait Test; the Citation Auditor verifies the exact author list, the exact volume, the exact page numbers, the exact DOI, and confirms the cited standardized mean difference values appear in the paper as written. Closes before Section 8.2 of this report is populated.

2. **Madgwick 2010 equation specificity audit** (this report's Reference 4 above). Open per `STATUS.md` Resume notes Phase 5 calibration item 7 (Citation Auditor Phase 3 PARTIAL P3.1). The auditor verifies the "eq 11 to eq 33" specificity that the Phase 3 ADR cites against the primary source, or the ADR drops the equation specificity. Closes before Phase 5 close.

3. **Bland and Altman 1986 first time audit** (this report's Reference 8 above). Open per `docs/qa/statistical-methods.md` Section 7.1, the methods document's pending Citation Auditor item. The auditor verifies the citation against the primary source and adds it to `docs/source/clinical-references.md` under a new entry alongside Koo and Li 2016 and Shrout and Fleiss 1979 in the "Statistical methodology references" section. Closes before Phase 5 close.

4. **Shrout and Fleiss 1979 first time audit** (this report's Reference 6 above; cited indirectly through the methods document). Open per `docs/qa/statistical-methods.md` Section 7.1, items 1 and 3 (the ICC(3,1) absolute agreement closed form denominator and the analytic CI closed form). The auditor verifies the formulas in the methods document against the primary source. Closes before Phase 5 close.

5. **Koo and Li 2016 banding audit** (this report's Reference 7 above; cited indirectly through the methods document). Open per `docs/qa/statistical-methods.md` Section 7.1 item 2 (the four band threshold cut points and labels). The auditor verifies the cut points and labels against the primary source. Closes before Phase 5 close.

The validation report's Part B authors should not finalize Section 8 (Discussion) or Section 7 (Test retest reliability results) until items 1, 4, and 5 above are closed; the methodological citations underwrite the report's headline numbers, and a closed audit is the precondition for publishing the headline numbers in the README's validation section per `agents/14-documentation-engineer.md` definition of done ("Validation numbers in the README match the validation report exactly").

Documentation Engineer, Phase 5 Part A Task A3, 2026-05-08.
