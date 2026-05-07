# STATUS, MS Neuro Battery

This file is the single source of truth for which phase is next. Read it at the start of every session.

## Next phase

**Phase 2: Tap Test.** Not started. Mandatory check in protocol must run before Phase 2 work begins. Phase 2 prep includes triage of the 18 issues the Patient Advocate flagged at the end of Phase 1 (see `docs/qa/patient-advocate-reviews.md`); some of these will be folded into Phase 2's Android Engineer dispatch and some require Compliance Reviewer or Data Engineer ratification first.

## Phase status table

| # | Phase | Status | Completed on | Window cost | Notes |
|---|-------|--------|--------------|-------------|-------|
| 0 | Bootstrap setup | completed | 2026-05-07 | ~65% (within est. 60 to 75% range) | All 12 deliverables produced. Citation Auditor exercised 5 vetoes (deferred to Phase 1 cleanup) and the PM applied 2 session corrections (Oh et al. 2024 attribution, Givon 2009 gait reference values). Compliance Reviewer recommended a finalized README re read which is pending. |
| 1 | Foundation | completed | 2026-05-07 | under 50% (under est. 70 to 85% range) | All 12 plan tasks delivered (Tasks 2 to 11 by Android Engineer, Task 12 README delta by Documentation Engineer). 15 commits on `main` from `7499417` through `690d630`. Test suite: 23 tests, 0 failures, 0 errors, 0 skipped, verified by Code Reviewer and QA Engineer independently. DBA schema audit PASS. Patient Advocate APPROVED with 18 issues (0 high, 11 medium, 7 low) deferred to Phase 2 prep. Compliance Reviewer cleared README. Emulator walkthrough deferred to user (headless agents cannot drive AVD). |
| 2 | Tap Test | not started |  | est. 30 to 45% | Bilateral tap test as first concrete TestModule |
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

**Phase 1 closed 2026-05-07.** Open items for Phase 2 prep:

1. **Patient Advocate Phase 1 review (18 issues, see `docs/qa/patient-advocate-reviews.md` Phase 1 entry)** to triage before or during Phase 2:
   - 11 medium severity copy and behavior issues (enum jargon in profile and settings, custom dropdown widget, default pre filled values, no skip path on onboarding, disclaimer chunking, no progress indicator on session runner, cancel mid session has no confirmation, DOB display when only year was collected, About disclaimer at small body text, no profile edit path).
   - 7 low severity issues (button copy, double space hack on home screen, raw timestamps in history, debug strings, complete screen polish, mock pattern awareness, root screen race condition).
   - Issue 5 (disclaimer rewording) requires Compliance Reviewer ratification before any change ships.
   - Issues 4 and 17 (nullable `heightCm`, profile edit path) are schema or SPEC.md changes; require Data Engineer and Database Administrator decisions.
2. **DBA recommendation:** flip `exportSchema = true` in `AppDatabase.kt` and add `room { schemaDirectory(...) }` to `app/build.gradle.kts` before Phase 5 (Gait Validation Suite), so version 1 schema is captured before any future migration. Phase 4 close is the last clean opportunity. Owner: Data Engineer.
3. **Compliance Reviewer Phase 0 carryover follow ups (not Phase 2 blockers):** re fetch the 2026-01-06 FDA General Wellness reissue ahead of Phase 5 and Phase 11 sign offs; decide beta cohort geography restriction or prepare GDPR consent text ahead of Phase 11; schedule a focused US state law review before Phase 11.
4. **JDK toolchain note for the user's local environment:** the system JDK at `/usr/lib/jvm/java-21-openjdk-amd64` is JRE only, no `javac`. Phase 1 builds were run with `JAVA_HOME=/snap/android-studio/209/jbr`. The CI workflow uses Temurin JDK 17 and is unaffected. If you want a frictionless `./gradlew` from the system shell, install OpenJDK 17 (`apt install openjdk-17-jdk`) or set `org.gradle.java.home` in `gradle.properties`.
5. **Emulator walkthrough deferred to user.** Task 10 Step 5 of `docs/plans/phase-1-foundation.md` (manual emulator walk through of disclaimer, profile setup, home, session runner, settings) was not run because the agents are headless. The user should walk the application on a real Android 12+ AVD or device before merging this to a public review channel.

Phase 0 carryover items 1, 2, 3, and 4 were all closed during Phase 1 (commit c49847e for items 1, 2, 4; commit 60b370e for item 3, the Compliance Reviewer README re read).

## Validation log

(Empty. The PM writes a one line entry here when a phase that includes validation work completes, with the achieved numbers. Phase 5 will populate this with stride length and cadence error percentages and ICC values.)

## How to update this file

The PM is the only role that writes here. Update at three moments:

1. **Start of phase.** Flip the row from `not started` to `in progress`. Update the "Next phase" line at the top to name this phase.
2. **Pause mid phase.** Flip the row to `paused`. Write a Resume notes entry that names the file and line of the last clean stopping point and the next concrete action when work resumes.
3. **Completion.** Flip the row to `completed`. Fill in the date in `Completed on` and the actual window cost in `Window cost`. Move the "Next phase" line to the next row in the table.

Never start, pause, or complete a phase without updating this file first. Other roles will read the wrong state.
