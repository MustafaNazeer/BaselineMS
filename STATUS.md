# STATUS, MS Neuro Battery

This file is the single source of truth for which phase is next. Read it at the start of every session.

**Phase 5: Gait Validation Suite.** Not started. Detailed plan to be written by the PM at the start of Phase 5 after the mandatory check-in. Phase 4 closed 2026-05-08 (QA Engineer PASS WITH NOTES; Code Reviewer APPROVED WITH MINOR CHANGES with PE M1 cleanup landed in `3c514f6`; SPE APPROVED WITH MINOR FINDINGS; Patient Advocate APPROVED WITH FINDINGS; Performance Engineer APPROVED WITH MINOR FINDINGS). The user driven multi device sample rate validation per `docs/observability/sensor-runbook.md` is the one outstanding non code Phase 4 close task and feeds directly into Phase 5's real walking course experiments.

## Phase status table

| # | Phase | Status | Completed on | Window cost | Notes |
|---|-------|--------|--------------|-------------|-------|
| 0 | Bootstrap setup | completed | 2026-05-07 | ~65% (within est. 60 to 75% range) | All 12 deliverables produced. Citation Auditor exercised 5 vetoes (deferred to Phase 1 cleanup) and the PM applied 2 session corrections (Oh et al. 2024 attribution, Givon 2009 gait reference values). Compliance Reviewer recommended a finalized README re read which is pending. |
| 1 | Foundation | completed | 2026-05-07 | under 50% (under est. 70 to 85% range) | All 12 plan tasks delivered (Tasks 2 to 11 by Android Engineer, Task 12 README delta by Documentation Engineer). 15 commits on `main` from `7499417` through `690d630`. Test suite: 23 tests, 0 failures, 0 errors, 0 skipped, verified by Code Reviewer and QA Engineer independently. DBA schema audit PASS. Patient Advocate APPROVED with 18 issues (0 high, 11 medium, 7 low) deferred to Phase 2 prep. Compliance Reviewer cleared README. Emulator walkthrough deferred to user (headless agents cannot drive AVD). |
| 2 | Tap Test | completed | 2026-05-07 | within est. 30 to 45% | All 23 plan tasks delivered (Phase 2A 3 specialist verdicts, Phase 2B 7 commits for Patient Advocate carryover, Phase 2C 7 commits for BilateralTapTest, Phase 2D 3 reviews plus 1 fix). 18 commits on `main` from `c33a13c` through `82bbfae`. Test suite: 48 tests, 0 failures, 0 errors, 0 skipped, verified by QA Engineer. Patient Advocate APPROVED with 5 issues (3 medium, 2 low) deferred to Phase 3 prep. Code Reviewer APPROVED WITH MINOR CHANGES (off target capture double counting bug); fix landed in `82bbfae` with regression test. Compliance Reviewer ratified disclaimer copy COMPLIANT. Clinical Validator APPROVED WITH REVISIONS (Option A miss capture, applied in plan). DBA schema audit PASS. Emulator walkthrough deferred to user. |
| 3 | Gait Signal Processing | completed | 2026-05-07 | TBD by user (under est. 80 to 95% range) | All 18 plan tasks delivered (TFE Tasks 1 to 4 plus a strideLength + asymmetryRatio repair, SPE Tasks 6 to 14 plus a Code Reviewer minor cleanup, four reviewer dispatches, two PM convention/citation fixes). 24 commits on `main` from `25050e0` through `4cd0a72`. Test suite: 83 tests, 0 failures, 0 errors, 0 skipped, verified by QA Engineer. Per fixture pipeline accuracy on `GaitPipelineIntegrationTest`: healthyControlNormal 1.01 percent cadence error and 0.26 percent stride length error; msTypicalNormal 2.51 percent / 0.18 percent; mildAsymmetry asymmetry index 0.0881 vs ground truth 0.0953; noisyMsNormal 0.39 percent / 0.39 percent. Performance Engineer APPROVED with 2 MINOR (allocations on once per test batch path, scoped within Phase 3 acceptance) and 3 INFORMATIONAL. Citation Auditor APPROVED WITH MINOR FINDINGS, 1 MISMATCH (Buckley 2020 cite in ADR replaced with Bea 2025 in `b23c287`) and 2 PARTIAL (P3.2 closed by SPE M1 cleanup; P3.1 deferred to Phase 5). Code Reviewer APPROVED WITH MINOR CHANGES, 4 MINOR with M1 / M2 / M4 landed in `78c14e5` and M3 (rotationVector null fallback) deferred to Phase 4 plus 4 LOW deferred to Phase 11. QA Engineer PASS WITH NOTES. Detailed plan at `docs/plans/phase-3-gait-signal-processing.md`. |
| 4 | Gait Test Module Integration | completed | 2026-05-08 | TBD by user (within est. 60 to 75% range) | All 13 plan tasks delivered (SIE Tasks 2 to 5, AE Tasks 1, 6, 7, 8, four reviewer dispatches Tasks 9 to 13) plus 1 PE M1 wording reconciliation cleanup. 15 commits on `main` from `46641b7` through `0582600`. Test suite: 102 tests, 0 failures, 0 errors, 0 skipped, verified by QA Engineer. SPE APPROVED WITH MINOR FINDINGS. Patient Advocate APPROVED WITH FINDINGS (5 findings deferred to Phase 11 polish). Performance Engineer APPROVED WITH MINOR FINDINGS (PE M3 main thread coroutine scheduling deferred as the highest priority Phase 5 prep item). Code Reviewer APPROVED WITH MINOR CHANGES with PE M1 cleanup landed in `3c514f6`. QA Engineer PASS WITH NOTES, all 8 falsifiable conditions verified yes. Multi device sample rate validation deferred to user-driven Phase 4 close task per `docs/observability/sensor-runbook.md`. Detailed plan at `docs/plans/phase-4-gait-test-module-integration.md`. |
| 5 | Gait Validation Suite | not started |  | est. 70 to 85% | Synthetic ground truth tests, real walking course experiments, ICC analysis, validation writeup in README |
| 6 | Vision Test | not started |  | est. 40 to 55% | Sloan low contrast acuity test with ambient brightness check via CameraX |
| 7 | SDMT Test | not started |  | est. 35 to 50% | Symbol Digit Modalities Test, 90 second cognitive battery |
| 8 | Voice Test | not started |  | est. 50 to 65% | Reading passage capture via AudioRecord, acoustic feature extraction (jitter, shimmer, HNR, speaking rate, pause fraction) |
| 9 | Reporting | not started |  | est. 60 to 75% | Longitudinal trend charts using Vico, summary tables, comparison against reference ranges |
| 10 | PDF Export | not started |  | est. 40 to 55% | PdfDocument generation, Share Intent via FileProvider |
| 11 | Accessibility, Beta, Polish | not started |  | est. 60 to 75% | TalkBack audit, Dynamic Type, color contrast, beta cohort recruitment, Play Store internal testing track |

Status values: `not started`, `in progress`, `completed`, `bundled with phase N`, `paused`.

## Resume notes

**Phase 4 closed 2026-05-08** (PASS WITH NOTES from QA Engineer; APPROVED WITH MINOR CHANGES from Code Reviewer with the PE M1 cleanup landed in commit `3c514f6`; APPROVED WITH MINOR FINDINGS from Signal Processing Engineer; APPROVED WITH FINDINGS from Patient Advocate; APPROVED WITH MINOR FINDINGS from Performance Engineer).

**Open items by phase target.** Each entry names the receiving phase, the owner role, and the verification path.

### For Phase 5 prep

1. **PE M3 (HIGHEST PRIORITY).** Main thread coroutine scheduling for the writer plus sensor listener. The Performance Engineer flagged that `GaitTestViewModel`'s writer job and the `SensorManager.registerListener` callback path all run on Main, which on a real device under load could starve the UI thread or coalesce sensor events. Recommended fix: dispatch the writer job and sensor listener on `Dispatchers.Default`. Owner: Sensor Integration Engineer with Android Engineer review. Reference: `docs/perf/latency-budgets.md` Phase 4 review M3.
2. **User driven multi device sample rate validation.** Install the app on each available real Android device, run a gait session with the phone in a front pocket, pull the captured CSV at `sensor_traces/<sessionId>/GAIT.csv.gz` via Android Studio's Device File Explorer or `adb pull`, run the runbook's awk pipeline to derive mean rate, p99 jitter, and the count of dropped 1 second sub windows where cumulative delta drift exceeds 5 percent of nominal, and append a per device entry to `docs/observability/sensor-runbook.md` against the reconciled budget (mean within 5 percent of 100 Hz, p99 jitter under 5 ms, zero dropped sub windows). Owner: user. Code is ready per Performance Engineer Phase 4 review.
3. **SPE I2 first fallback Madgwick step uses gravity removed input.** On the very first emission of the fallback path, `lastAccel` is null because the `TYPE_ACCELEROMETER` callback has not fired yet, so the code temporarily falls back to feeding `lastLinear` (gravity removed) into Madgwick. The drift after one sample is negligible but worth tightening for cleanliness. Owner: Sensor Integration Engineer. Reference: `docs/qa/spe-review-phase-4.md` finding I2.
4. **PE M2, M4, M5 minor allocations.** PE M2 buffer sizing nuance, M4 transient `Quaternion` discarded by `.normalized()` in `quaternionFromAndroidRotationVector`, M5 string allocation pile up in `RawSensorWriter.appendRow` (~42k strings per 30 second capture). Owner: Sensor Integration Engineer. Reference: `docs/perf/latency-budgets.md` Phase 4 review.
5. **CR L1 to L3 Code Reviewer Phase 4 LOW findings.** Cosmetic cleanup. Owner: Sensor Integration Engineer or Android Engineer. Reference: `docs/qa/code-review-phase-4.md` Findings section.
6. **DBA Phase 2 follow up FK pragma forward note (carried from Phase 4 prep notes; still applies).** When `test_result.session_id` migrations run, the FK pragma needs to be set per Room's migration recommendations. Owner: Database Administrator review at any future schema delta.

### For Phase 5 calibration

7. **Citation Auditor Phase 3 PARTIAL P3.1.** The "eq 11 to eq 33" specificity in the Madgwick 2010 reference inside `docs/adr/0002-madgwick-from-scratch.md` was not pre audited in the bibliography. The companion P3.2 was closed by the SPE's M1 cleanup commit `78c14e5`. Recommendation: Citation Auditor re-audits the Madgwick 2010 primary source (or the equation specificity is dropped from the ADR) before Phase 5 close. Reference: `docs/source/citation-audit-log.md` lines 423 to 429.
8. **Synthetic magnitude minima inversion plus three uncovered fixtures (CR L1).** Two paired items: (a) `docs/qa/fixtures.md` Section 2.1 documents that the synthetic forward propulsion peak amplitude exceeds the world Z heel strike Gaussian peak, so synthetic magnitude minima fall AT heel strikes rather than between them; Phase 5 real walking course recordings will tell us whether to recalibrate the forward propulsion amplitude or the pipeline's mid stance detector. (b) Three `PreCannedFixtures` entries (`slowWalk`, `briskWalk`, `severeAsymmetry`) lack `GaitPipelineIntegrationTest` coverage; the natural home is a Phase 5 reviewer task that adds them alongside the real recordings.
9. **Compliance Reviewer Phase 0 carryover (still open).** Re-fetch the 2026-01-06 FDA General Wellness reissue ahead of Phase 5 and Phase 11 sign offs (the 2026-05-07 Phase 2 dispatch triangulated from law firm summaries because the FDA URL returned 404 again).
10. **SPE design choices in `GaitPipeline.kt` and `FeatureExtractor.kt` flagged for real-data calibration (carried from Phase 4 prep notes).** Three documented in code KDoc, none blocking Phase 4 wiring but worth surfacing: (a) lateral sampling offset (a quarter step interval before each detected step) is a synthetic-only quirk because the synthetic generator's lateral sway evaluates to zero exactly at step times; on real signals this offset may not be needed and should be re-evaluated when real recordings arrive in Phase 5. (b) Madgwick pre-warm with 5000 virtual iterations stands in for the user's standing window; in production the user will be asked to stand briefly before walking, which gives the same convergence opportunity from real samples. (c) FeatureExtractor double support time uses a 20 percent of stride time approximation because the synthetic generator does not model true double support phase boundaries; Phase 5 will reconcile against measured ground truth.

### For Phase 11 prep

11. **Patient Advocate Phase 2 review (5 issues, see `docs/qa/patient-advocate-reviews.md` 2026-05-07 Phase 2 entry).** All five deferred to Phase 11 polish per Phase 3 prep PM triage:
    - **Finding 1, medium:** "dominant hand" framing copy fix (BilateralTapTest pre-instructions). Owner: Android Engineer.
    - **Finding 2, medium:** visible 5 second rest timer between rounds. Owner: Android Engineer.
    - **Finding 3, medium:** copy variant for quality 0.0 sessions (fewer than 10 valid taps). Owner: Android Engineer plus Compliance Reviewer ratification.
    - **Finding 4, low:** widen the 16 dp gap between the 120 dp tap circles to 32 dp. Owner: Android Engineer; Clinical Validator confirms normative impact.
    - **Finding 5, low:** add a Continue tap on the Done state. Owner: Android Engineer.
12. **Patient Advocate Phase 4 review (5 findings, see `docs/qa/patient-advocate-reviews.md` 2026-05-07 Phase 4 entry).** All five deferred to Phase 11 polish:
    - **Finding 1, medium:** Cancel button reachability while phone is in front pocket (`GaitCaptureScreen.kt`). Owner: Android Engineer.
    - **Finding 2, medium:** Cancelled state Done screen reuses "Captured but quality is low" copy and says "Your gait test is complete." after a cancel. Owner: Android Engineer.
    - **Finding 3, medium:** No contextual messaging for mobility aid users on the Instructions screen. Owner: Android Engineer.
    - **Finding 4, low:** Quality bands conflate noisy data with genuine bad MS day data. Owner: Android Engineer plus Clinical Outcomes Reviewer.
    - **Finding 5, low:** "Your gait test is complete." line is uninformative; cross battery warmth consistency. Owner: Android Engineer.
13. **Code Reviewer Phase 2 minor finding.** `dobYearText.toInt()` and `heightCmText.toDouble()` in `ProfileSetupScreen.kt` should switch to `toIntOrNull()` plus a defensive sentinel (Phase 11 polish, not a blocker).
14. **Code Reviewer Phase 3 LOW findings (see `docs/qa/code-review-phase-3.md`).** L2 unused `kotlin.math.sin` import; L3 `Math.PI` instead of `kotlin.math.PI` in two files; L4 internally inconsistent docstring on double support time. Owner: Signal Processing Engineer (cosmetic cleanup).
15. **SPE Phase 4 informational findings I1 and I3 (see `docs/qa/spe-review-phase-4.md`).** I1 tail edge sample loss on capture cancel: a sample in flight when the user cancels can be dropped. I3 RawSensorWriter virtuality vs interface based pattern: the writer is `open class`; the SPE prefers an interface plus impl pattern for swap testability. Owner: Sensor Integration Engineer.
16. **Abandoned session lifecycle finding (from 2026-05-07 user walkthrough).** Navigating away from `SessionRunnerScreen` via system back leaves the in-flight `SessionEntity` row at `completedAtEpochMs = null` so History renders it as "In progress" forever. Two acceptable fixes: (a) intercept back during Running state and surface the Cancel confirmation dialog; (b) DisposableEffect cleanup auto-cancels Running sessions. Owner: Android Engineer plus Patient Advocate ratification.
17. **Compliance Reviewer Phase 11 prep.** Decide beta cohort geography restriction or prepare GDPR consent text ahead of Phase 11; schedule a focused US state law review before Phase 11.

### Carryover invariants and operational notes

18. **JDK toolchain note (still applies).** The system JDK at `/usr/lib/jvm/java-21-openjdk-amd64` is JRE only, no `javac`. Local builds run with `JAVA_HOME=/snap/android-studio/209/jbr`. The CI workflow uses Temurin JDK 17 and is unaffected.
19. **Emulator walkthrough deferred to user (Phase 2 carryover; Phase 4 added new gait UI).** The QA Engineer's regression checklists in `docs/qa/regression-checklist.md` Phase 2 and Phase 4 sections list the falsifiable conditions to verify on a Pixel class AVD running Android 12 or later. Phase 4 added the GaitTest instructions, countdown, capture, and done screens; the next emulator walkthrough should exercise the new gait flow alongside the prior tap test flow.
20. **DBA Phase 2 follow up (not a blocker).** CREATE TABLE fallback to trailing `PRIMARY KEY('id')` form if Room's schema validator ever rejects the inline form (no current trigger).

## Validation log

(Empty. The PM writes a one line entry here when a phase that includes validation work completes, with the achieved numbers. Phase 5 will populate this with stride length and cadence error percentages and ICC values.)

## How to update this file

The PM is the only role that writes here. Update at three moments:

1. **Start of phase.** Flip the row from `not started` to `in progress`. Update the "Next phase" line at the top to name this phase.
2. **Pause mid phase.** Flip the row to `paused`. Write a Resume notes entry that names the file and line of the last clean stopping point and the next concrete action when work resumes.
3. **Completion.** Flip the row to `completed`. Fill in the date in `Completed on` and the actual window cost in `Window cost`. Move the "Next phase" line to the next row in the table.

Never start, pause, or complete a phase without updating this file first. Other roles will read the wrong state.
