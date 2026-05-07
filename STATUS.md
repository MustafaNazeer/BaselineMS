# STATUS, MS Neuro Battery

This file is the single source of truth for which phase is next. Read it at the start of every session.

## Next phase

**Phase 3: Gait Signal Processing.** Not started. Mandatory check in protocol must run before Phase 3 work begins. Phase 3 prep includes triage of the 5 issues the Patient Advocate flagged at the end of Phase 2 (see `docs/qa/patient-advocate-reviews.md` 2026-05-07 Phase 2 entry); some may fold into early Phase 3 dispatches and some may defer to Phase 11 polish.

## Phase status table

| # | Phase | Status | Completed on | Window cost | Notes |
|---|-------|--------|--------------|-------------|-------|
| 0 | Bootstrap setup | completed | 2026-05-07 | ~65% (within est. 60 to 75% range) | All 12 deliverables produced. Citation Auditor exercised 5 vetoes (deferred to Phase 1 cleanup) and the PM applied 2 session corrections (Oh et al. 2024 attribution, Givon 2009 gait reference values). Compliance Reviewer recommended a finalized README re read which is pending. |
| 1 | Foundation | completed | 2026-05-07 | under 50% (under est. 70 to 85% range) | All 12 plan tasks delivered (Tasks 2 to 11 by Android Engineer, Task 12 README delta by Documentation Engineer). 15 commits on `main` from `7499417` through `690d630`. Test suite: 23 tests, 0 failures, 0 errors, 0 skipped, verified by Code Reviewer and QA Engineer independently. DBA schema audit PASS. Patient Advocate APPROVED with 18 issues (0 high, 11 medium, 7 low) deferred to Phase 2 prep. Compliance Reviewer cleared README. Emulator walkthrough deferred to user (headless agents cannot drive AVD). |
| 2 | Tap Test | completed | 2026-05-07 | within est. 30 to 45% | All 23 plan tasks delivered (Phase 2A 3 specialist verdicts, Phase 2B 7 commits for Patient Advocate carryover, Phase 2C 7 commits for BilateralTapTest, Phase 2D 3 reviews plus 1 fix). 18 commits on `main` from `c33a13c` through `82bbfae`. Test suite: 48 tests, 0 failures, 0 errors, 0 skipped, verified by QA Engineer. Patient Advocate APPROVED with 5 issues (3 medium, 2 low) deferred to Phase 3 prep. Code Reviewer APPROVED WITH MINOR CHANGES (off target capture double counting bug); fix landed in `82bbfae` with regression test. Compliance Reviewer ratified disclaimer copy COMPLIANT. Clinical Validator APPROVED WITH REVISIONS (Option A miss capture, applied in plan). DBA schema audit PASS. Emulator walkthrough deferred to user. |
| 3 | Gait Signal Processing | not started |  | est. 80 to 95% | Pure DSP module: Madgwick filter, Butterworth low pass, step detection, ZUPT stride length, feature extraction. Unit tested against synthetic ground truth |
| 4 | Gait Test Module Integration | not started |  | est. 60 to 75% | Sensor capture via SensorManager, Compose UI for the gait test, persistence wiring |
| 5 | Gait Validation Suite | not started |  | est. 70 to 85% | Synthetic ground truth tests, real walking course experiments, ICC analysis, validation writeup in README |
| 6 | Vision Test | not started |  | est. 40 to 55% | Sloan low contrast acuity test with ambient brightness check via CameraX |
| 7 | SDMT Test | not started |  | est. 35 to 50% | Symbol Digit Modalities Test, 90 second cognitive battery |
| 8 | Voice Test | not started |  | est. 50 to 65% | Reading passage capture via AudioRecord, acoustic feature extraction (jitter, shimmer, HNR, speaking rate, pause fraction) |
| 9 | Reporting | not started |  | est. 60 to 75% | Longitudinal trend charts using Vico, summary tables, comparison against reference ranges |
| 10 | PDF Export | not started |  | est. 40 to 55% | PdfDocument generation, Share Intent via FileProvider |
| 11 | Accessibility, Beta, Polish | not started |  | est. 60 to 75% | TalkBack audit, Dynamic Type, color contrast, beta cohort recruitment, Play Store internal testing track |

Status values: `not started`, `in progress`, `completed`, `bundled with phase N`, `paused`.

## Resume notes

**Phase 2 closed 2026-05-07.** Open items for Phase 3 prep:

1. **Patient Advocate Phase 2 review (5 issues, see `docs/qa/patient-advocate-reviews.md` 2026-05-07 Phase 2 entry)** to triage before or during Phase 3:
   - **Finding 1, medium:** "dominant hand" framing is ambiguous for patients whose MS has shifted their effective dominant side. One clarifying sentence in the BilateralTapTest pre instructions copy fixes it. Owner: Android Engineer (small commit, can land at Phase 3 start).
   - **Finding 2, medium:** the 5 second rest period between rounds shows no countdown. Recommended: render a visible rest timer. Owner: Android Engineer.
   - **Finding 3, medium:** quality 0.0 sessions (fewer than 10 valid taps either round) show the same completion text as a full session. Recommended: add a copy variant that acknowledges the short round without shaming. Patient Advocate Phase 0 framing standing objection 6 (quality score copy) applies; Compliance Reviewer should ratify any new clinical adjacent wording. Owner: Android Engineer plus Compliance Reviewer ratification.
   - **Finding 4, low:** the 16 dp gap between the two 120 dp circles may be too narrow for a tremor patient tapping quickly. Recommended: widen to 32 dp. Clinical Validator should confirm normative impact (this is a smartphone adaptation choice, not from MS literature).
   - **Finding 5, low:** the Done state auto advances rather than waiting for a user "Continue" tap. Recommended: add a confirmation tap so fatigued patients have a deliberate breath. Owner: Android Engineer.
2. **Code Reviewer Phase 2 minor finding (see `docs/qa/code-review-phase-2.md`):** the `dobYearText.toInt()` and `heightCmText.toDouble()` calls in `ProfileSetupScreen.kt` are protected by `canSave` but would throw if the guard were ever weakened. Recommended hardening before Phase 11: switch to `toIntOrNull()` plus a defensive sentinel. Owner: Android Engineer (Phase 11 polish, not a Phase 3 blocker).
3. **DBA Phase 2 follow ups (not blockers):** Phase 2 audit verdict noted two minor non blocking items: (a) FK pragma forward note for future migrations that touch a referenced table (relevant in Phase 4 and beyond when `test_result.session_id` migrations may run); (b) CREATE TABLE fallback to trailing `PRIMARY KEY('id')` form if Room's schema validator ever rejects the inline form (no current trigger).
4. **Compliance Reviewer Phase 0 carryover follow ups (still open):** re fetch the 2026-01-06 FDA General Wellness reissue ahead of Phase 5 and Phase 11 sign offs (the 2026-05-07 Phase 2 dispatch triangulated from law firm summaries because the FDA URL returned 404 again); decide beta cohort geography restriction or prepare GDPR consent text ahead of Phase 11; schedule a focused US state law review before Phase 11.
5. **Clinical Validator Phase 2 caveat:** SPEC.md Section 6.1 uses the phrase "miss rate" without an operational definition. The Phase 2 plan and the Phase 2 sign off note both ratified Option A (miss rate combines non alternating taps and off target taps from both rounds), but SPEC.md itself was not amended. Recommendation: the PM amends SPEC.md Section 6.1 in early Phase 3 to include the operational definition, citing the Clinical Validator's 2026-05-07 sign off note.
6. **JDK toolchain note (still applies):** the system JDK at `/usr/lib/jvm/java-21-openjdk-amd64` is JRE only, no `javac`. Phase 2 builds were run with `JAVA_HOME=/snap/android-studio/209/jbr`. The CI workflow uses Temurin JDK 17 and is unaffected.
7. **Emulator walkthrough deferred to user (Task 19 of the Phase 2 plan).** The QA Engineer's Phase 2 regression checklist in `docs/qa/regression-checklist.md` lists the falsifiable conditions to verify on a Pixel class AVD running Android 12 or later. Specifically: full BilateralTapTest flow, the Skip for now path in profile setup, the cancel confirmation dialog, and the Edit profile route from Settings.

## Validation log

(Empty. The PM writes a one line entry here when a phase that includes validation work completes, with the achieved numbers. Phase 5 will populate this with stride length and cadence error percentages and ICC values.)

## How to update this file

The PM is the only role that writes here. Update at three moments:

1. **Start of phase.** Flip the row from `not started` to `in progress`. Update the "Next phase" line at the top to name this phase.
2. **Pause mid phase.** Flip the row to `paused`. Write a Resume notes entry that names the file and line of the last clean stopping point and the next concrete action when work resumes.
3. **Completion.** Flip the row to `completed`. Fill in the date in `Completed on` and the actual window cost in `Window cost`. Move the "Next phase" line to the next row in the table.

Never start, pause, or complete a phase without updating this file first. Other roles will read the wrong state.
