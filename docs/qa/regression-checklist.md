# Regression checklist

**Status:** placeholder. The QA Engineer writes this file in Phase 0 and updates it after every phase that introduces a screen or a test module.

## Sections

### By phase

For each completed phase, a list of items to verify on a real device or emulator before declaring the phase done.

### By screen

For each screen, the user actions that must succeed (open, populate, submit, navigate back).

### By test module

For each test module, the input fixture, the expected output range, and the quality score behavior under degraded conditions.

## Notes for the QA Engineer

Keep entries small and falsifiable. "The app works" is not a checklist item. "On a Pixel 6 emulator with TalkBack on, opening Settings reads the profile fields aloud in order" is.

## Sign offs

### Phase 1 sign off, 2026-05-07

**Verdict:** SIGNED OFF for Phase 1 close. Automated criteria from the Phase 1 plan's "Done When" section are met. The emulator walkthrough is deferred to the user (see below).

**Test suite run.**

- Command: `JAVA_HOME=/snap/android-studio/209/jbr ./gradlew :app:testDebugUnitTest --rerun-tasks --console=plain`
- Outcome: `BUILD SUCCESSFUL in 10s`, 30 actionable tasks executed.
- Total tests run: 23 across 8 suites, 0 failures, 0 errors, 0 skipped (verified by parsing `app/build/test-results/testDebugUnitTest/TEST-*.xml`). This matches the Code Reviewer's reported counts.
- Per suite breakdown:
  - `com.mustafanazeer.baselinems.data.EnumsTest`: 5 tests, all pass.
  - `com.mustafanazeer.baselinems.data.ConvertersTest`: 5 tests, all pass.
  - `com.mustafanazeer.baselinems.data.UserProfileDaoTest`: 2 tests, all pass.
  - `com.mustafanazeer.baselinems.data.SessionDaoTest`: 3 tests, all pass.
  - `com.mustafanazeer.baselinems.data.TestResultDaoTest`: 1 test, all pass.
  - `com.mustafanazeer.baselinems.battery.MockTestModuleTest`: 2 tests, all pass.
  - `com.mustafanazeer.baselinems.battery.BatteryOrchestratorTest`: 4 tests, all pass.
  - `com.mustafanazeer.baselinems.battery.BatteryFlowIntegrationTest`: 1 test, passes.

**Commit chain on `main` from `60b370e..HEAD`.** 12 commits verified.

1. `7499417` feat(data): add TestType, Sex, Hand, MSType enums (Task 2)
2. `8f4a25a` feat(data): add Room Converters, UserProfileEntity, DAO, and AppDatabase skeleton (Task 3)
3. `94b62e5` feat(data): add Session and TestResult entities with cascade delete (Task 4)
4. `5e0edea` feat(battery): define TestModule interface and TestResultPayload (Task 5)
5. `49dd9fb` feat(battery): add MockTestModule for scaffolding (Task 6)
6. `4a928eb` feat(battery): implement BatteryOrchestrator state machine (Task 7)
7. `d59efa5` feat(ui): add disclaimer and profile setup screens (Task 8)
8. `45df915` feat(ui): add home, session runner, and settings screens (Task 9)
9. `47e5cf4` feat(app): wire RootScreen, BaselineMSApp Application class, and MainActivity (Task 10)
10. `3535ee6` test(battery): add end to end integration test (Task 11)
11. `b2286c5` docs: update README for Phase 1 completion (Task 12, Documentation Engineer)
12. `5adf1bd` Phase 1 reviews: DBA schema audit (PASS) and Patient Advocate Phase 1 review (18 issues for Phase 2 prep)

**Working tree.** `git status` reports `nothing to commit, working tree clean` on branch `main`. Branch is 14 commits ahead of `origin/main` (push deferred to user discretion); the local tree itself is clean.

**Privacy posture spot checks.**

- `app/src/main/AndroidManifest.xml` does not contain `<uses-permission android:name="android.permission.INTERNET" />`. Verified via grep: no match. Security Engineer's hard rail is intact.
- `android:allowBackup="false"` is preserved on line 7 of the manifest, with `android:fullBackupContent="false"` on line 8 and `tools:replace="android:allowBackup"` on line 14.

**CI workflow.** `.github/workflows/ci.yml` runs `./gradlew :app:testDebugUnitTest` followed by `./gradlew :app:assembleDebug` on Ubuntu with Temurin JDK 17. Consistent with `app/build.gradle.kts` which targets `sourceCompatibility = JavaVersion.VERSION_17` and `kotlinOptions { jvmTarget = "17" }`. The workflow file would still run cleanly on the current Phase 1 head; no changes needed.

**Emulator walkthrough deferral.** Task 10 Step 5 of the Phase 1 plan calls for a manual emulator walkthrough (boot the app, accept the disclaimer, complete profile, run a session through five mock test modules, verify history shows one row, exercise Settings). This step requires an interactive Android emulator (AVD running Android 12 or later) and cannot be driven from a headless agent session. **Deferred to the user.** The user should run this walkthrough on a Pixel-class AVD before declaring Phase 1 fully closed in their own records. The integration test (`BatteryFlowIntegrationTest`) exercises the same orchestrator state path programmatically and passes, which gives confidence that the behavioral surface is sound; the emulator pass is the visual confirmation that the Compose UI renders and navigates as intended.

**Follow ups for Phase 2.**

- **Patient Advocate Phase 1 review (18 issues).** Documented in `docs/qa/patient-advocate-reviews.md` (added in commit `5adf1bd`). These are explicitly Phase 2 prep, not Phase 1 blockers. The PM should triage them before dispatching Phase 2 (Tap Test), since several may shape the disclaimer copy, the profile setup affordances, and the home screen layout that the Tap Test will plug into.
- **DBA `exportSchema = true` recommendation.** Documented in `docs/data/schema.md` (updated in commit `5adf1bd`). Currently the Room database is built with `exportSchema = false` to keep the Phase 1 footprint small. The DBA recommends switching to `exportSchema = true` and committing the generated schema JSON files **before Phase 4 to 5**, when migrations between schema versions become a real concern (Phase 4 brings the gait test module which adds the first non placeholder writes against `test_result`, and Phase 5 brings real validation traffic). Tracked here so Phase 2 and Phase 3 do not accidentally bake in additional schema mass without exports in place.
- **Beta cohort and accessibility audits.** Not Phase 2 specific, but the QA Engineer notes that the regression checklist's "by screen" and "by test module" sections will need real entries beginning in Phase 2 once the first concrete TestModule (Tap Test) lands. Phase 2 close should expand this file with screen level checks for the disclaimer, profile setup, home, session runner, and settings screens.

**Uncertainties and notes.**

- The `--rerun-tasks` invocation and the previous cached run produced identical pass counts and identical 0 failure or error totals, so the green result is reproducible, not just a stale cache.
- One innocuous Gradle warning was emitted on the rerun: `application@android:allowBackup was tagged at AndroidManifest.xml:5 to replace other declarations but no other declaration present`. This is the Android Gradle Plugin observing that no library manifest is supplying a competing `allowBackup` value for `tools:replace` to override. It is harmless and does not affect the application's backup posture; the explicit `android:allowBackup="false"` still governs. Flagging it so the Android Engineer can decide in a later phase whether to drop the `tools:replace` once the manifest stabilises.
- I did not modify any application code or configuration during sign off; only this document was touched.

---

## Phase 2 close, 2026-05-07

**Verdict:** SIGNED OFF for Phase 2 close. All automated criteria met. The emulator walkthrough is deferred to the user (see manual walkthrough note below).

**Test suite run.**

- Command: `JAVA_HOME=/snap/android-studio/209/jbr ./gradlew :app:testDebugUnitTest --rerun-tasks --console=plain`
- Outcome: `BUILD SUCCESSFUL in 13s`, 30 actionable tasks executed.
- Total tests run: **46 tests, 0 failures, 0 errors, 0 skipped** (verified by parsing `app/build/test-results/testDebugUnitTest/TEST-*.xml`).
- Per suite breakdown:
  - `com.mustafanazeer.baselinems.battery.BatteryFlowIntegrationTest`: 1 test, passes (carried from Phase 1).
  - `com.mustafanazeer.baselinems.battery.BatteryOrchestratorTest`: 4 tests, all pass (carried from Phase 1).
  - `com.mustafanazeer.baselinems.battery.MockTestModuleTest`: 2 tests, all pass (carried from Phase 1).
  - `com.mustafanazeer.baselinems.battery.tap.BilateralTapTestMetadataTest`: 1 test, passes (new in Phase 2).
  - `com.mustafanazeer.baselinems.battery.tap.BilateralTapTestRenderTest`: 1 test, passes (new in Phase 2).
  - `com.mustafanazeer.baselinems.battery.tap.TapFeaturesTest`: 12 tests, all pass (new in Phase 2).
  - `com.mustafanazeer.baselinems.data.ConvertersTest`: 5 tests, all pass (carried from Phase 1).
  - `com.mustafanazeer.baselinems.data.EnumsTest`: 5 tests, all pass (carried from Phase 1).
  - `com.mustafanazeer.baselinems.data.Migration1To2Test`: 2 tests, all pass (new in Phase 2: Room v1 to v2 migration for nullable `heightCm`).
  - `com.mustafanazeer.baselinems.data.SessionDaoTest`: 3 tests, all pass (carried from Phase 1).
  - `com.mustafanazeer.baselinems.data.TestResultDaoTest`: 1 test, passes (carried from Phase 1).
  - `com.mustafanazeer.baselinems.data.UserProfileDaoTest`: 2 tests, all pass (carried from Phase 1).
  - `com.mustafanazeer.baselinems.ui.common.EnumLabelsTest`: 4 tests, all pass (new in Phase 2: human readable enum label resolver).
  - `com.mustafanazeer.baselinems.ui.home.HomeScreenFormattersTest`: 3 tests, all pass (new in Phase 2: relative date formatters).
- Phase 1 baseline was 23 tests across 8 suites. Phase 2 adds 23 tests across 6 new suites, for a Phase 2 close total of 46 tests across 14 suites.

**Build clean status.**

- Command: `JAVA_HOME=/snap/android-studio/209/jbr ./gradlew :app:assembleDebug --console=plain`
- Outcome: `BUILD SUCCESSFUL in 930ms`, 37 actionable tasks executed.
- Zero new warnings introduced beyond the Phase 1 baseline. The one known innocuous Gradle warning (`application@android:allowBackup was tagged at AndroidManifest.xml:5 to replace other declarations but no other declaration present`) was present in Phase 1 and remains present; it is not a Phase 2 regression.

**Regression checklist: Phase 2 surface area.**

All items below are manual verification targets. The QA Engineer cannot drive an emulator; the items are written as falsifiable conditions for the user to confirm on a Pixel-class AVD running Android 12 or later.

### Disclaimer screen

- [ ] Three sentences render left aligned: "This app is not a medical device. It does not diagnose or treat any condition." / "Please do not change your treatment based on what you see here." / "When you visit your neurologist, you can share these results to help the conversation."
- [ ] Button reads "Got it, continue" (not "I understand").
- [ ] Tapping the button advances to the profile setup screen.
- [ ] First launch shows the disclaimer screen before any other screen.
- [ ] Returning launch (profile already saved) goes directly to Home without showing the disclaimer.

### Profile setup screen

- [ ] TopAppBar reads "Set up profile (1 of 1)".
- [ ] Both fields (year of birth and height) start empty on first visit; no pre filled default values.
- [ ] Entering a year outside a plausible range (for example 1800 or 2026) triggers an inline validation error.
- [ ] Entering a height outside a plausible range triggers an inline validation error.
- [ ] The dominant hand field uses an `ExposedDropdownMenuBox`; tapping it opens a dropdown with human readable labels ("Left", "Right", "Either hand") not raw enum names.
- [ ] The sex field uses an `ExposedDropdownMenuBox` with labels ("Female", "Male", "Other", "Prefer not to say").
- [ ] The MS type field uses an `ExposedDropdownMenuBox` with labels such as "Relapsing remitting (RRMS)".
- [ ] "Save and continue" is disabled (greyed out) until both the year of birth and height fields are filled with plausible values.
- [ ] "Skip for now" button is visible and tapping it creates a profile with a null height, then advances to Home.
- [ ] After "Skip for now", the Home screen is reached and no crash occurs.

### Home screen

- [ ] The primary call to action reads "Start this week's check in".
- [ ] The empty state copy mentions "about ten minutes" (no double space or raw `  ` artefact; rendered via a `Spacer` not a space hack).
- [ ] History rows show relative dates: "Today, 14:30" for a session from today, "Yesterday, 09:15" for a session from yesterday, "Tuesday, 14:30" for a session three to six days ago, "May 5" for a session seven or more days ago.

### Session runner screen

- [ ] A persistent header at the top shows the current test name and "Test 1 of 1" (with the correct count when multiple tests are present in a future phase).
- [ ] A `LinearProgressIndicator` beneath the header updates as the test progresses.
- [ ] Tapping Cancel opens an `AlertDialog` with the title "Stop this check in?" and two buttons: "Keep going" (default) and "Stop".
- [ ] Choosing "Keep going" dismisses the dialog and resumes the test.
- [ ] Choosing "Stop" exits the session and returns to Home without persisting a partial result.
- [ ] The idle or loading state shows a `CircularProgressIndicator` with copy similar to "Getting your check in ready".
- [ ] After both tap test rounds complete, the completion screen shows "All done. Your check in has been saved." and a "Done" button.
- [ ] Tapping "Done" returns to Home where a new history row is visible.

### Bilateral Tap test module

- [ ] The pre instructions screen shows copy describing the alternating tap task and a "Start dominant hand round" button.
- [ ] After tapping "Start dominant hand round", a countdown from 5 to 1 is visible on screen before the targets appear.
- [ ] Two circular targets render side by side in a single row, each at least 96 dp in visible diameter.
- [ ] Tapping inside the left target (when it is the expected side) increments the live tap counter.
- [ ] Tapping inside the right target (when it is the expected side) increments the live tap counter.
- [ ] Tapping the same target twice in a row does not increment the valid tap counter; the tap is recorded as `NON_ALTERNATING`.
- [ ] Tapping in the row area outside both target circles increments the off target counter, not the valid tap counter.
- [ ] After 30 seconds the dominant round ends and a 5 second rest is shown.
- [ ] After the rest, the non dominant round begins automatically and runs for 30 seconds.
- [ ] After the non dominant round ends, the session runner shows the completion screen.
- [ ] A `TestResult` row for the tap test is present in the database after the session (verify via the history row appearing on Home).

### Settings screen

- [ ] The dominant hand field label reads "Dominant hand" (not the raw enum name).
- [ ] The year of birth label reads "Year of birth" (not "Date of birth" or "DOB").
- [ ] The About section text is rendered at `bodyLarge` (not `bodySmall` or `bodyMedium`).
- [ ] The visual spacing between the About text and the Edit profile button uses a `Spacer` composable, not a `Text(" ")` hack.
- [ ] Tapping "Edit profile" routes to the profile setup screen with the current year, height, dominant hand, sex, and MS type values pre filled.
- [ ] Saving from that pre filled form updates the profile and returns to Settings with the updated values visible.

### Root screen and navigation

- [ ] On first launch the disclaimer renders immediately; there is no brief flash of the Home screen before the disclaimer appears.
- [ ] On second launch (profile present, disclaimer already accepted) the Home screen renders directly with no flash of the disclaimer.
- [ ] A `CircularProgressIndicator` (splash indicator) is shown briefly while the suspending profile read resolves; it disappears and is replaced by the correct destination screen.
- [ ] The navigation back stack on Home does not contain the disclaimer or profile setup screens (pressing Back exits the app, it does not navigate to onboarding).

**Manual walkthrough note (Task 19 of the Phase 2 plan).**

The manual emulator walkthrough is the user's responsibility. The QA Engineer is a headless agent and cannot drive an Android AVD. The user should run the above checklist on a Pixel-class AVD (API 31 or later, Android 12 plus) before considering Phase 2 fully closed in their own records. The automated test suite (46 tests) exercises the pure Kotlin math and state machine paths; the checklist above is the visual and interaction confirmation that the Compose UI renders, navigates, and handles edge cases as intended.

**Uncertainties and notes.**

- The `--rerun-tasks` run produced `BUILD SUCCESSFUL in 13s` with 30 actionable tasks executed and the full 46 test count. The cached run produced `BUILD SUCCESSFUL in 789ms` with 1 executed task (the test runner was the only non cached task). Both runs yield 0 failures, 0 errors, 0 skipped. The green result is reproducible.
- The one known Gradle warning (`allowBackup tools:replace with no competing declaration`) is unchanged from Phase 1. It does not affect the application's backup posture.
- The `assembleDebug` run was `BUILD SUCCESSFUL in 930ms` with 37 actionable tasks (4 executed, 33 up to date). No new Kotlin compiler warnings or AGP warnings appeared in the output beyond the `allowBackup` warning noted above.
- I did not modify any application code, tests, or configuration during sign off; only this document was touched.

---

## Phase 3 close, 2026-05-07

**Verdict:** PASS WITH NOTES. Phase 3 is signed off cleanly on the test bar (all 83 unit tests pass, zero failures, zero errors, zero skipped). The notes below are carryover items the PM should track in `STATUS.md` Resume notes for Phase 4 and Phase 11; none of them block Phase 3 close.

**Test suite run.**

- Command: `JAVA_HOME=/snap/android-studio/209/jbr ./gradlew :app:testDebugUnitTest --rerun-tasks --console=plain`
- Outcome: `BUILD SUCCESSFUL in 13s`, 30 actionable tasks executed.
- Total tests run: **83 tests across 25 suites, 0 failures, 0 errors, 0 skipped** (verified by parsing `app/build/test-results/testDebugUnitTest/TEST-*.xml`).
- Phase 2 close baseline was 48 tests; Phase 3 grew the suite by 35 tests across 11 new suites for a Phase 3 close total of 83 tests across 25 suites. Strict growth confirmed.
- Per suite breakdown of the 11 Phase 3 additions:
  - `com.mustafanazeer.baselinems.dsp.ButterworthLowPassTest`: 3 tests, all pass.
  - `com.mustafanazeer.baselinems.dsp.MadgwickTest`: 2 tests, all pass.
  - `com.mustafanazeer.baselinems.dsp.WorldFrameTest`: 2 tests, all pass.
  - `com.mustafanazeer.baselinems.dsp.StepDetectorTest`: 2 tests, all pass.
  - `com.mustafanazeer.baselinems.dsp.StridePairingTest`: 4 tests, all pass.
  - `com.mustafanazeer.baselinems.dsp.ZuptTest`: 3 tests, all pass.
  - `com.mustafanazeer.baselinems.dsp.FeatureExtractorTest`: 7 tests, all pass.
  - `com.mustafanazeer.baselinems.dsp.QuaternionTest`: 2 tests, all pass (added by the M2 fix in commit `78c14e5` after the Code Reviewer verdict).
  - `com.mustafanazeer.baselinems.dsp.GaitPipelineIntegrationTest`: 4 tests, all pass.
  - `com.mustafanazeer.baselinems.fixtures.SyntheticImuTest`: 6 tests, all pass.
- All 14 Phase 1 and Phase 2 suites carry forward with their counts unchanged.

**Privacy and architectural rail spot checks.**

- `grep -rn "import android" app/src/main/java/com/mustafanazeer/baselinems/dsp/` returns nothing. The DSP package is pure Kotlin with zero `android.*` imports, as required by `SPEC.md` Section 5.2 and the Phase 3 plan architectural rail.
- `grep -n "INTERNET" app/src/main/AndroidManifest.xml` returns nothing. The carryover Phase 1 and Phase 2 invariant (no `android.permission.INTERNET`) holds. Security Engineer's hard rail is intact.

### Falsifiable conditions for Phase 3 (DSP package)

The conditions below are the regression tripwires for any future change that touches `app/src/main/java/com/mustafanazeer/baselinems/dsp/` or `app/src/test/java/com/mustafanazeer/baselinems/fixtures/`. Each item names the verification command or grep that confirms the condition holds.

#### Per fixture pipeline accuracy budgets (`GaitPipelineIntegrationTest`)

The integration test exercises four of the seven `PreCannedFixtures` entries. The other three (`slowWalk`, `briskWalk`, `severeAsymmetry`) are declared but not yet exercised; see L1 in the carryover notes below.

- [ ] `healthyControlNormal` recovers cadence within 3 percent of 115.2 steps per minute. Verify with `JAVA_HOME=/snap/android-studio/209/jbr ./gradlew :app:testDebugUnitTest --tests "com.mustafanazeer.baselinems.dsp.GaitPipelineIntegrationTest.healthy control normal recovers cadence within 3 percent and stride length within 2 percent"`. The assertion lives at `app/src/test/java/com/mustafanazeer/baselinems/dsp/GaitPipelineIntegrationTest.kt` line 15.
- [ ] `healthyControlNormal` recovers stride length within 2 percent of 1.442 m. Same test method as above; the assertion is at line 16.
- [ ] `msTypicalNormal` recovers cadence within 3 percent of 94.4 steps per minute. Verify with the test method `MS typical normal recovers within the same envelope`; assertion at line 24.
- [ ] `msTypicalNormal` recovers stride length within 2 percent of 0.906 m. Same test method; assertion at line 25.
- [ ] `noisyMsNormal` recovers cadence within 5 percent of 94.4 steps per minute under 0.5 m per second squared additive Gaussian noise. Verify with the test method `noisy MS normal still recovers cadence within 5 percent and stride length within 4 percent`; assertion at line 41.
- [ ] `noisyMsNormal` recovers stride length within 4 percent of 0.906 m under noise. Same test method; assertion at line 42.

The 2 percent stride length target on the clean fixtures matches `SPEC.md` Section 7.2 layer 1. The 3 percent cadence target tracks `SPEC.md` Section 7.2 layer 2 ahead of the real walking course measurement scheduled for Phase 5.

#### Asymmetry index recovery

- [ ] `mildAsymmetry` recovers a stride asymmetry index within 0.04 of the expected value 0.10. Verify with the test method `mild asymmetry recovers a positive asymmetry index of about 0_1`; assertion at `GaitPipelineIntegrationTest.kt` line 34. The `mildAsymmetry` fixture is configured with `asymmetryRatio = 1.10` per `PreCannedFixtures.kt` line 52.

#### Quality score floors

- [ ] `healthyControlNormal` produces a quality score strictly above 0.8. Assertion at `GaitPipelineIntegrationTest.kt` line 17.
- [ ] `msTypicalNormal` produces a quality score strictly above 0.7. Assertion at `GaitPipelineIntegrationTest.kt` line 26.

The two floor values reflect the orientation residual quality model documented in `GaitPipeline.kt` lines 226 to 271; the higher floor on the healthy fixture reflects the higher cadence (more strides per trial) and the lower stride time variability.

#### Per module DSP unit test bars

Each falsifiable condition below is "the named test class passes with the recorded test count." The verification command is `JAVA_HOME=/snap/android-studio/209/jbr ./gradlew :app:testDebugUnitTest --tests "com.mustafanazeer.baselinems.dsp.<ClassName>"`.

- [ ] `ButterworthLowPassTest` passes with 3 tests. Covers DC pass, high frequency suppression, and zero phase response.
- [ ] `MadgwickTest` passes with 2 tests. Covers static gravity convergence and gyroscope only integration.
- [ ] `WorldFrameTest` passes with 2 tests. Covers gravity removal and device to world rotation.
- [ ] `StepDetectorTest` passes with 2 tests. Covers peak prominence rejection and minimum inter peak distance.
- [ ] `StridePairingTest` passes with 4 tests. Covers lateral sign assignment, alternation discipline, edge cases.
- [ ] `ZuptTest` passes with 3 tests. Covers velocity reset at mid stance, integration over a single segment, and per stride displacement.
- [ ] `FeatureExtractorTest` passes with 7 tests. Covers cadence, stride length mean, step time CV, asymmetry, double support time, and the `toMap()` projection.
- [ ] `QuaternionTest` passes with 2 tests. Covers `inverse()` round trip and unit norm preservation; added in commit `78c14e5` to support the M2 fix.

#### Synthetic generator round trip tests

- [ ] `SyntheticImuTest` passes with 6 tests. Verify with `JAVA_HOME=/snap/android-studio/209/jbr ./gradlew :app:testDebugUnitTest --tests "com.mustafanazeer.baselinems.fixtures.SyntheticImuTest"`. The six round trip checks ensure the fixture generator's stated ground truth (number of heel strikes, mean stride length, asymmetry ratio, step time CV, gravity addition, and rotation vector convention) is recoverable by inversion of the generator's parameters. If a future change to `SyntheticImu.kt` regresses any of these round trips, every downstream pipeline test is invalidated.

#### Suite growth and green bar

- [ ] Total test count remains strictly greater than 48 (the Phase 2 close baseline). Verify with `python3 -c "import xml.etree.ElementTree as ET; import glob; print(sum(int(ET.parse(f).getroot().attrib.get('tests', 0)) for f in glob.glob('app/build/test-results/testDebugUnitTest/TEST-*.xml')))"`. Phase 3 close measured 83.
- [ ] Total failures, errors, and skipped each equal 0. Verify with the same XML aggregation, summing the `failures`, `errors`, and `skipped` attributes on each `<testsuite>` root.

#### Architectural rails (carryover from Phase 1 and Phase 2)

- [ ] No `android.*` import appears anywhere under `app/src/main/java/com/mustafanazeer/baselinems/dsp/`. Verify with `grep -rn "import android" app/src/main/java/com/mustafanazeer/baselinems/dsp/`. The DSP package is pure Kotlin per `SPEC.md` Section 5.2 ("Signal Processing. Pure Kotlin. No sensor or UI imports.").
- [ ] No `android.permission.INTERNET` appears in `app/src/main/AndroidManifest.xml`. Verify with `grep -n "INTERNET" app/src/main/AndroidManifest.xml`. This is the Security Engineer's hard rail per `SPEC.md` Section 10 and the carryover Phase 1 plus Phase 2 invariant.

### Sign off and carryover items

**Verdict: PASS WITH NOTES.** All falsifiable conditions above hold at Phase 3 close. The notes below are documented carryover for the PM to capture in `STATUS.md` Resume notes for Phase 4 and Phase 11. None of them blocked Phase 3 close.

**Carryover items for Phase 4 (Sensor Integration and Android Engineer dispatch):**

1. **Code Reviewer M3 (rotationVector null fallback).** `GaitPipeline.rotateToWorld` falls back to `Quaternion.IDENTITY` when `ImuSample.rotationVector` is null, which would silently skip the world frame transform on a device that does not expose `TYPE_ROTATION_VECTOR`. The Phase 4 Sensor Integration Engineer must either populate `rotationVector` from the from scratch Madgwick filter in the signals layer, or update the DSP `rotateToWorld` to compute its own Madgwick estimate as a fallback. No Phase 3 unit test exercises this null path because every fixture populates `rotationVector`. Documented in `docs/qa/code-review-phase-3.md` MINOR M3 (lines 113 to 146).

**Carryover items for Phase 5 (real walking course validation and reviewer cleanup):**

2. **Citation Auditor PARTIAL P3.1 (Madgwick 2010 equation specificity).** The ADR `docs/adr/0002-madgwick-from-scratch.md` cites "eq 11 to eq 33" of the Madgwick 2010 University of Bristol technical report; the bibliography does not record equation numbers and the Phase 3 audit did not re fetch the primary source. Recommended Phase 5 home: a Citation Auditor follow up pass that either re fetches the primary source to verify the equation range or rewrites the ADR clause to drop the equation specificity. Documented in `docs/source/citation-audit-log.md` lines 423 to 429. The companion P3.2 was closed by the Signal Processing Engineer's M1 cleanup commit `78c14e5`.
3. **Code Reviewer L1 (three pre canned fixtures uncovered by integration tests).** `slowWalk`, `briskWalk`, and `severeAsymmetry` are declared in `PreCannedFixtures.kt` lines 27 to 69 but are not exercised by `GaitPipelineIntegrationTest.kt`. Recommended home: a Phase 5 reviewer task or Phase 11 polish task that adds three integration test cases (or the equivalent regression checklist conditions). The CR's recommended tolerances are 3 percent cadence and 5 percent stride length on the slow and brisk envelopes, and asymmetry index within 0.05 of 0.2609 on `severeAsymmetry`. The QA Engineer is not adding the three cases now because doing so would either require modifying production test code (out of role for this sign off) or the looser 5 percent stride length envelope would need an SPE dispatch first to confirm the implementation actually meets it; both paths are better fitted into Phase 5 calibration work alongside the real walking course recordings.
4. **Synthetic generator magnitude minima inversion caveat.** `docs/qa/fixtures.md` Section 2.1 documents that the synthetic forward propulsion peak amplitude (about 16.7 m per second squared on `healthyControlNormal`) exceeds the world Z heel strike Gaussian peak (5.0 m per second squared), so the local minima of the synthetic three axis acceleration magnitude fall AT the heel strike instants rather than between them. The pipeline accommodates this on the synthetic side, but the real walking course recordings in Phase 5 will need to confirm whether real signals exhibit the same inversion. If real recordings show the published convention (minima between heel strikes, at mid stance), the Test Fixture Engineer will need to recalibrate the forward propulsion amplitude or the pipeline's mid stance detector will need a band passed magnitude input.

**Informational items recorded for completeness, no carryover action required:**

5. **Performance Engineer INFORMATIONAL findings 1, 2, 3.** `docs/perf/latency-budgets.md` lines 66 to 88 flag (1) `medianStepInterval` recomputed inside the lateral at step initializer, (2) Kotlin `DoubleArray(size, init)` autoboxing of the init lambda, and (3) `ArrayList<Double>` autoboxing in `computeAsymmetry`. None are blocking and none require Phase 4 action; the Phase 4 real device measurement on `IMU sampling: actual sample rate within 5 percent of nominal 100 Hz, jitter under 5 ms p99` will confirm whether any of them surface as measured regressions. INFORMATIONAL 1 was promoted to MINOR M4 by the Code Reviewer and was closed by the SPE's commit `78c14e5` (median hoisted out of the loop).

**Manual walkthrough note.**

There is no Phase 3 manual emulator walkthrough. Phase 3 is a pure Kotlin DSP module with no UI surface, no Compose screens added, and no `app/src/main/AndroidManifest.xml` changes. The DSP module is exercised entirely through the JVM unit test suite. The first user facing manifestation of the gait pipeline lands in Phase 4, when the Sensor Integration Engineer wires `SensorManager` to `Flow<ImuSample>` and the Android Engineer adds the gait test screen; the manual walkthrough for that surface is the QA Engineer's deliverable at Phase 4 close.

**Uncertainties and notes.**

- The `--rerun-tasks` invocation produced `BUILD SUCCESSFUL in 13s` with 30 actionable tasks executed and the full 83 test count. A subsequent cached run produced `BUILD SUCCESSFUL in 827ms` with 1 executed task. Both runs yield 0 failures, 0 errors, 0 skipped. The green result is reproducible.
- The Code Reviewer's verdict in `docs/qa/code-review-phase-3.md` was generated against commit `b23c287` and recorded 81 tests. The current `HEAD` (`cf81970`) is two commits past that point; the M2 fix in commit `78c14e5` added two `QuaternionTest` cases (`inverse round trip` and `inverse preserves unit norm`) to support the new public `Quaternion.inverse()` method, lifting the count from 81 to 83. The `cf81970` follow up dropped a now redundant private `Quaternion.inverse` extension from the fixtures package without changing the test count. The Phase 3 sign off bar is the current `HEAD` count of 83.
- I did not modify any application code, tests, or configuration during sign off; only this document was touched.

---

## Phase 4 close, 2026-05-08

**Verdict:** PASS WITH NOTES. Phase 4 is signed off cleanly on the test bar (all 102 unit tests pass, zero failures, zero errors, zero skipped). The notes below are carryover items the PM should track in `STATUS.md` Resume notes for Phase 5 prep and Phase 11 polish, and the user driven multi device sample rate validation task per `docs/observability/sensor-runbook.md` is the one outstanding non code item Phase 4 defers to the user. None of the carryover items block Phase 4 close; they were ruled deferrable by the Code Reviewer's consolidation in `docs/qa/code-review-phase-4.md`.

**Test suite run.**

- Command: `JAVA_HOME=/snap/android-studio/209/jbr ./gradlew :app:testDebugUnitTest --rerun-tasks --console=plain`
- Outcome: `BUILD SUCCESSFUL in 14s`, 30 actionable tasks executed.
- Total tests run: **102 tests across 31 suites, 0 failures, 0 errors, 0 skipped** (verified by parsing `app/build/test-results/testDebugUnitTest/TEST-*.xml`).
- Phase 3 close baseline was 83 tests across 25 suites. Phase 4 grew the suite by 19 tests across 6 new suites for a Phase 4 close total of 102 tests across 31 suites. Strict growth confirmed.

**Commit chain on `main` from `4cd0a72..HEAD`.** 15 commits verified via `git log --oneline 4cd0a72..HEAD`. The chain spans Phase 3 close cleanup through Phase 4 plan prep, the eight implementation tasks (Tasks 1 to 8), the four review dispatches (Tasks 9 to 12), and the single Phase 4 cleanup commit (PE M1) the Code Reviewer required before this sign off:

1. `874182a` Phase 3 close: flip STATUS to completed, restructure Resume notes by phase target.
2. `46641b7` Phase 4 prep: STATUS in progress, detailed Phase 4 plan.
3. `b8187c8` battery: extend TestResultPayload with optional rawSensorRelativePath (Task 1).
4. `0cbaafa` signals: AndroidImuSource for SensorManager backed Flow<ImuSample> at 100 Hz (Task 2).
5. `ea27060` docs(adr): 0003 sensor type choice (Task 3).
6. `aac52f2` docs(observability): sensor runbook with per device entry template and acceptance budgets (Task 4).
7. `2f47991` signals: RawSensorWriter gzipped CSV persistence with round trip tests (Task 5).
8. `c33988f` battery(gait): instructions, countdown, capture, done screens with smoke tests (Task 6).
9. `34a3174` battery(gait): viewmodel state machine and TestModule integration (Task 7).
10. `d9b4795` battery(gait): register GaitTest in BaselineMSApp module list (Task 8).
11. `bf99d79` review(gait): Signal Processing Engineer Phase 4 sensor stream review (Task 9).
12. `4bc1bec` review(gait): Patient Advocate Phase 4 review (Task 10).
13. `a8ca3e2` perf(gait): Phase 4 Performance Engineer review of sensor capture path (Task 11).
14. `377dc00` review(gait): Code Reviewer Phase 4 verdict (Task 12).
15. `3c514f6` perf(gait): reconcile dropped windows budget wording with sensor runbook (PE M1 cleanup).

The PM handoff note for this sign off named 11 commits from `4cd0a72` exclusive; the actual count on the branch is 15 because the Phase 3 close `STATUS.md` flip (`874182a`) and the Phase 4 plan prep (`46641b7`) both land in the same range alongside the 13 Phase 4 implementation, review, and cleanup commits. The 15 count is the verified count and the one this sign off treats as authoritative.

**File count delta from Phase 3 close.** `git diff --stat 4cd0a72..HEAD` reports 32 files changed, 3,649 insertions, 43 deletions.

**Privacy and architectural rail spot checks.**

- `grep -in 'INTERNET' /home/mustafa/src/BaselineMS/app/src/main/AndroidManifest.xml` returns nothing (exit 1). The carryover invariant from Phase 1, Phase 2, and Phase 3 (no `android.permission.INTERNET`) holds. Security Engineer's hard rail is intact. The Code Reviewer's Task 12 verdict independently verified `git diff 4cd0a72..HEAD -- app/src/main/AndroidManifest.xml` returns nothing; the manifest is byte identical to the Phase 3 close commit `4cd0a72`.
- `grep -rn "import android" app/src/main/java/com/mustafanazeer/baselinems/dsp/` continues to return nothing; the DSP package remains pure Kotlin with zero `android.*` imports per `SPEC.md` Section 5.2 and Phase 3 architectural rail. Phase 4 added `signals/` as a thin adaptation layer above `dsp/`; the dependency direction is `signals/` to `dsp/` only, verified by `grep -rn "import com.mustafanazeer.baselinems.signals" app/src/main/java/com/mustafanazeer/baselinems/dsp/` returning nothing (recorded in `docs/qa/code-review-phase-4.md` PASS section).

### Falsifiable conditions for Phase 4 (gait test module integration)

Each of the eight conditions below is the regression tripwire taken verbatim from `docs/plans/phase-4-gait-test-module-integration.md` Task 13 spec lines 728 to 736. Each condition is recorded with explicit "verified yes / no" wording plus the failure mode that would invalidate it.

#### Condition 1, AndroidImuSourceTest four cases

- **Verified yes.** `JAVA_HOME=/snap/android-studio/209/jbr ./gradlew :app:testDebugUnitTest --tests "com.mustafanazeer.baselinems.signals.AndroidImuSourceTest"` reports `tests=4 failures=0 errors=0 skipped=0` (parsed from `app/build/test-results/testDebugUnitTest/TEST-com.mustafanazeer.baselinems.signals.AndroidImuSourceTest.xml`). The four cases cover (a) `start` registers listeners on the system `SensorManager`, (b) `stream` emits an `ImuSample` on each linear acceleration event with held gyro and rotation values via zero order hold, (c) `stop` unregisters all listeners, and (d) when `Sensor.TYPE_ROTATION_VECTOR` is absent the fallback Madgwick fills `ImuSample.rotationVector` with a non null quaternion.
- **Failure mode that would invalidate this condition.** Any of the four cases regressing (a missing listener registration, a dropped held value, a leaked listener after `stop`, or a null `rotationVector` on the fallback path), or the test class disappearing from the suite. SPE M1 flags a fixture realism caveat on case (d) (the test does not exercise the fallback Madgwick with realistic gravity included input) and is deferred to Phase 5 prep; the case still passes as written and is not invalidated by the SPE finding.

#### Condition 2, RawSensorWriterTest two cases

- **Verified yes.** `tests=2 failures=0 errors=0 skipped=0` on `com.mustafanazeer.baselinems.signals.RawSensorWriterTest`. The two cases cover (a) round trip: a known stream of `ImuSample` rows is written to a gzipped CSV, read back, decompressed, parsed, and every field is recovered within floating point precision; (b) exception propagation: a flow that throws after N samples results in the writer closing the file and rethrowing.
- **Failure mode that would invalidate this condition.** A round trip drift on any of the 14 columns (timestamp, three acceleration channels, three gyro channels, three linear acceleration channels, four rotation vector components), or the writer leaking the file handle after an exception, or the exception being swallowed.

#### Condition 3, GaitTestViewModelTest six cases

- **Verified yes.** `tests=6 failures=0 errors=0 skipped=0` on `com.mustafanazeer.baselinems.battery.gait.GaitTestViewModelTest`. The six cases cover (1) Instructions to Countdown(3) on `onStart()`, (2) Countdown(3) to Capturing(0) after three one second ticks, (3) `imuSource.start()` and `RawSensorWriter.write()` invoked once on capture entry with samples accumulated in order, (4) Capturing to Done after 30 seconds with non null `GaitFeatures`, (5) `onCancel()` from Capturing transitions to Cancelled and stops the source, (6) `onContinue` from Done invokes the completion callback with a `TestResultPayload` whose `rawSensorRelativePath` matches the destination file's relative path under `Context.filesDir`.
- **Failure mode that would invalidate this condition.** Any case regressing, or the state machine accepting a transition the spec forbids (Instructions to Capturing without a Countdown, Capturing to Done before 30 seconds without a cancel, etc.). SPE I1 flags a tail edge sample loss on cancel that is acceptable for Phase 4 and deferred to Phase 5 prep; case (5) still passes as written because the assertion is on state transition and source lifecycle, not on the trailing sample count.

#### Condition 4, Compose smoke tests on the four GaitTest screens

- **Verified yes.** `tests=4 failures=0 errors=0 skipped=0` on `com.mustafanazeer.baselinems.battery.gait.GaitTestRenderTest`. The four smoke tests cover Instructions, Countdown, Capture, and Done. Each test asserts the screen renders without crashing in a Robolectric Compose host and that the primary CTA's semantic node exists (Instructions has `I am ready` and `Skip for now`; Countdown exposes the current second via `Modifier.semantics { contentDescription = secondsLabel }` plus the `Get ready to walk` headline endorsed by the Patient Advocate; Capture has a Cancel button; Done has Continue).
- **Failure mode that would invalidate this condition.** A screen failing to render in the test host (e.g. a Compose runtime exception), the loss of a primary CTA semantic node, or the Cancelled state path failing to reach `GaitDoneScreen` via `GaitTest.kt` lines 61 to 64.

#### Condition 5, BatteryFlowIntegrationTest with the new orchestrator signature

- **Verified yes.** `tests=1 failures=0 errors=0 skipped=0` on `com.mustafanazeer.baselinems.battery.BatteryFlowIntegrationTest`. The orchestrator's `recordResult` was migrated in commit `b8187c8` from the three argument `recordResult(testType, qualityScore, features)` form to the two argument `recordResult(testType: TestType, payload: TestResultPayload)` form so the optional `rawSensorRelativePath` flows through to the persisted `TestResultEntity`. `BatteryFlowIntegrationTest.kt` lines 61 to 72 call the new signature with a `MockTestModule.MockResult` payload across all three test types (TAP, GAIT, VISION); the test asserts the orchestrator reaches the `Completed` state, exactly one `SessionEntity` row is written with a non null `completedAtEpochMs`, and three `TestResultEntity` rows are written.
- **Failure mode that would invalidate this condition.** A regression in the orchestrator's state machine after the signature change, a payload that fails to flow `rawSensorRelativePath` to the persisted entity, or a null `completedAtEpochMs` on a session that should be marked complete.

#### Condition 6, BilateralTapTest tests with the TestResultPayload extension

- **Verified yes.** Across the four BilateralTapTest suites the parsed XML reports `BilateralTapTestMetadataTest` 1 of 1, `BilateralTapTestRenderTest` 1 of 1, `TapFeaturesTest` 12 of 12, `TwoTargetsOffTargetTest` 2 of 2, all with zero failures, errors, and skipped. The `TestResultPayload` interface added the new property as `val rawSensorRelativePath: String? get() = null`, a default getter implementation, so the existing `TapSessionFeatures` payload picks up the default automatically without source level changes; the Code Reviewer's Task 12 verdict and the Phase 4 plan rail 6 both confirm this is a backward compatible extension.
- **Failure mode that would invalidate this condition.** Any of the 16 tap related tests regressing, or the `TestResultPayload` default getter being removed (which would force every implementation to declare `rawSensorRelativePath` explicitly and break the Phase 2 contract).

#### Condition 7, AndroidManifest.xml contains no INTERNET permission

- **Verified yes.** Direct grep evidence:

```
$ grep -in 'INTERNET' /home/mustafa/src/BaselineMS/app/src/main/AndroidManifest.xml
$ echo $?
1
```

The grep returns nothing on stdout and exits 1 (no match). The Code Reviewer's Task 12 verdict additionally records `git diff 4cd0a72..HEAD -- app/src/main/AndroidManifest.xml` returns nothing, meaning the manifest is byte identical to the Phase 3 close commit `4cd0a72`. The four sensor types Phase 4 registers (`TYPE_LINEAR_ACCELERATION`, `TYPE_GYROSCOPE`, `TYPE_ROTATION_VECTOR`, `TYPE_ACCELEROMETER` on the fallback path only) do not require any runtime permission per ADR 0003.
- **Failure mode that would invalidate this condition.** Any future commit that adds `<uses-permission android:name="android.permission.INTERNET" />` (or any other runtime permission) to `app/src/main/AndroidManifest.xml`. The Security Engineer holds veto power on any such change.

#### Condition 8, test count strictly grew from 83 to 102 with zero failures

- **Verified yes.** Phase 3 close baseline was 83 tests; Phase 4 close measured 102 tests (`102 - 83 = 19` net new tests). All 102 pass with zero failures, zero errors, zero skipped, verified by aggregating the `tests`, `failures`, `errors`, and `skipped` attributes across every `<testsuite>` root in `app/build/test-results/testDebugUnitTest/TEST-*.xml`. Strict growth holds.
- **Failure mode that would invalidate this condition.** A test count below 102, a non zero failures, errors, or skipped aggregate, or a Phase 4 commit landing without a co committed test case for any new production file (which would violate the TDD discipline the Phase 4 plan required and which the Code Reviewer's Task 12 verdict verified across all eight implementation commits).

### Test count breakdown

83 baseline + 19 new = 102 final. The 19 new tests are attributed to six tasks per the Phase 4 plan:

- **Task 1 (TestModuleTest):** 2 cases. Default `rawSensorRelativePath` is null on a payload that does not override; payload can override with a non null relative path.
- **Task 2 (AndroidImuSourceTest):** 4 cases. `start` registers listeners; `stream` emits with held values; `stop` unregisters; fallback Madgwick fills `rotationVector` when `TYPE_ROTATION_VECTOR` is absent.
- **Task 5 (RawSensorWriterTest):** 2 cases. Round trip preserves every field within floating point precision; exception propagation closes the file and rethrows.
- **Task 6 (GaitTestRenderTest):** 4 cases. Compose smoke tests for Instructions, Countdown, Capture, and Done screens.
- **Task 7 (GaitTestViewModelTest):** 6 cases. State transitions, sensor lifecycle, payload production end to end.
- **Task 8 (GaitTestRegistrationTest):** 1 case. `BaselineMSApp` constructs the GaitTest module without crashing in a Robolectric environment.

Task by task sum: 2 + 4 + 2 + 4 + 6 + 1 = 19. Per suite XML aggregation independently confirms: `TestModuleTest` 2, `AndroidImuSourceTest` 4, `RawSensorWriterTest` 2, `GaitTestRenderTest` 4, `GaitTestViewModelTest` 6, `GaitTestRegistrationTest` 1.

The two existing test files modified for the orchestrator signature change (`BatteryFlowIntegrationTest.kt`, `BatteryOrchestratorTest.kt`) do not contribute new cases; they are migrations of existing cases to the new `recordResult(testType, payload)` form. Phase 4's net new test contribution is therefore the 19 cases above.

### User driven Phase 4 close task

**Multi device sample rate validation per `docs/observability/sensor-runbook.md`.** This is the one outstanding non code item Phase 4 defers to the user. Headless agents cannot drive a real Android device or an emulator, so the Performance Engineer's Phase 4 deliverable per `agents/18-sensor-integration-engineer.md` Phase 4 Task 3 (actual sample rate stability across at least three real Android devices) is split: the code side methodology landed in this phase (the captured CSV's `timestampNanos` column produced by `AndroidImuSource.emitIfReady` is the source of truth for the measurement, and the runbook's awk pipeline derives mean rate, p99 jitter, and dropped one second sub windows from the CSV) and the multi device measurement itself is the user's task.

The user's procedure per the runbook:

1. Install the app on each available real Android device (Pixel class, Samsung class, and a budget device per `SPEC.md` Section 13 risk mitigation row 2 are the recommended set; the user's actually available device set is acceptable).
2. Run a gait session on each device with the phone in a front pocket per the in app instructions.
3. Pull the captured CSV from the device's app private files directory at `sensor_traces/<sessionId>/GAIT.csv.gz` via Android Studio's Device File Explorer or `adb pull` (the application is in debuggable build, which the runbook confirms is sufficient).
4. Run the runbook's awk pipeline on each captured CSV to produce mean inter sample delta, p50 delta, p99 jitter (`|delta minus 10 ms|`), and the count of dropped one second sub windows where cumulative delta drift exceeds 5 percent of nominal.
5. Append a per device entry to the runbook's per device entry template recording device model, Android version, observed sample rate, jitter, dropped sub window count, and any observed gotchas.

Acceptance budget per `docs/perf/latency-budgets.md` "Initial budgets" line 7 (reconciled in commit `3c514f6` to match the runbook's stricter sub window framing): mean sample rate within 5 percent of nominal 100 Hz; p99 jitter under 5 ms; zero one second sub windows over the 30 second capture in which the cumulative delta drift exceeds 5 percent of nominal.

Once the runbook holds entries for at least three devices, the Performance Engineer can sign off the Phase 4 multi device deliverable retroactively, or it carries forward into Phase 5 alongside the real walking course recordings. This mirrors the Phase 1 and Phase 2 deferred emulator walkthrough convention; the user runs the walkthrough at their convenience.

### Deferred items

The Code Reviewer's consolidation in `docs/qa/code-review-phase-4.md` is the authoritative list. Each finding is reproduced below with its severity, owner, and target phase. None blocked Phase 4 close.

**MEDIUM, Phase 5 prep:**

1. **PE M3, main thread coroutine scheduling on the capture path.** `GaitTestViewModel.beginCapture` runs `writerJob` (gzip plus IO) on `rememberCoroutineScope()` which dispatches on `Dispatchers.Main.immediate`; `SensorManager.registerListener` is also called without a `Handler` so callbacks fire on Main. Risks UI 60 fps budget and producer side schedule drift. The CSV based `event.timestamp` measurement is robust in the typical case (256 element SharedFlow buffer, 2.56 second headroom), but the design should not rely on it. Fix: pass `Dispatchers.IO` to `writerJob.launch` (or wrap `rawSensorWriter.write` in `withContext(Dispatchers.IO)`) and pass a dedicated `Handler` or `HandlerThread` to `SensorManager.registerListener`. Owner: Android Engineer (view model dispatcher); Sensor Integration Engineer (listener handler). The PM's Phase 5 dispatch brief should highlight this as the single highest priority Phase 5 prep item.

**MINOR, Phase 5 prep:**

2. **SPE M1, fallback Madgwick test fixture realism.** `AndroidImuSourceTest` case (d) feeds `9.81` on `TYPE_LINEAR_ACCELERATION` instead of including `TYPE_ACCELEROMETER` with realistic gravity. The test passes on the current assertion (non null `rotationVector`) but does not exercise production fallback semantics. Fix: add a `TYPE_ACCELEROMETER` shadow sensor and feed gravity included `(0, 0, 9.81)` events on that channel. Owner: Sensor Integration Engineer.
3. **PE M4, transient Quaternion allocation in `quaternionFromAndroidRotationVector`.** The `Quaternion(w, x, y, z)` constructor result is immediately discarded by the `.normalized()` call, producing about 3000 redundant allocations per 30 second capture on the platform fused path. Fix: compute the normalized components in primitive locals and call the constructor exactly once. Architectural rail 7 is substantially preserved; this is the only literal violation. Owner: Sensor Integration Engineer.
4. **PE M5 (conditional), per row String allocations in `RawSensorWriter.appendRow`.** `.toString()` on 13 doubles plus the long timestamp per row produces about 42,000 short lived `String` allocations per 30 second capture. Not bound by rail 7 (the writer is not the per sample callback path), but on a thermally throttled budget device this could coincide with a 10 ms hiccup that the producer attributes to the sensor framework. Fix conditional on Phase 4 close measurements showing jitter spikes correlated with the writer thread; if not, drops to Phase 11 polish. Owner: Sensor Integration Engineer.

**MINOR, Phase 11 polish:**

5. **PE M2, no debug only sample rate counter on `AndroidImuSource`.** A `lastDeltaNanos` accessor or windowed mean delta would let the user see the rate live in app without pulling the CSV via adb. Phase 4 close is fully achievable via the captured CSV per the runbook's awk pipeline, so this is a UX win, not a blocker. Owner: Sensor Integration Engineer.

**INFORMATIONAL, Phase 5 prep:**

6. **SPE I1 / PE I1r, tail edge sample loss on capture cancellation.** `MutableSharedFlow` buffered samples not yet pulled by the collector are lost when `writerJob` is cancelled in `finishCapture`. Practical loss is at most a handful of samples at the trailing edge; gait features are robust. Fix: switch `finishCapture` to `cancelAndJoin` after `stop()` so the collector flushes pending elements first. Owner: Android Engineer.
7. **SPE I2 / PE I2r, first fallback Madgwick step uses gravity removed input.** On the first linear acceleration event after `start()`, `lastAccel` may be null because no `TYPE_ACCELEROMETER` event has fired yet; the fallback substitutes `linear` for `lastAccel` for one sample. Single sample anomalous input is below quality score sensitivity; ADR 0002's `beta = 0.1` makes the filter slow to commit to any one observation. Fix: document in the runbook if Phase 5 fallback recordings show first samples are visibly off. Owner: Sensor Integration Engineer.

**LOW, Phase 11 polish:**

8. **Patient Advocate Finding 1 (medium per PA severity, ruled LOW for Phase 4 by the Code Reviewer's consolidation), Cancel reachability while phone is in front pocket.** Mitigation is a one sentence Instructions screen add ("If you need to stop, you can take the phone out of your pocket and tap Cancel, or just stop walking and the test will end on its own with reduced confidence."). Owner: Android Engineer; UI/UX Designer for tone.
9. **Patient Advocate Finding 2, Cancelled state Done screen says "Captured but quality is low" rather than acknowledging the cancel.** Conflates a chosen stop with a noisy completion. Fix: a `Cancelled` arm with distinct copy ("Test stopped. No data was saved for this round.") plus a Continue CTA. Owner: Android Engineer.
10. **Patient Advocate Finding 3, no contextual messaging for mobility aid users on the Instructions screen.** Skip path is present, but a wheelchair, walker, or cane user has no signal that skipping is the correct action for them. Fix: a one sentence add. Owner: Android Engineer; UI/UX Designer for tone.
11. **Patient Advocate Finding 4, low quality copy on a bad MS day session conflated with first time confused users.** Quality bands applied uniformly. Owner: Android Engineer in coordination with Clinical Outcomes Reviewer; better fitted into Phase 9 reporting work where the per session view can surface a richer narrative.
12. **Patient Advocate Finding 5, "Your gait test is complete." line is technically accurate but uninformative.** Cross battery warmth consistency with the BilateralTapTest's Done copy. Owner: Android Engineer; UI/UX Designer for tone.
13. **CR L1, no `GaitFeatures.empty()` companion factory.** `GaitTest.kt` lines 75 to 83 inlines a zero `GaitFeatures` constructor. A future phase synthesizing a zero `GaitFeatures` would either duplicate this or refactor it. Fix: extract `GaitFeatures.empty()` companion factory. Owner: Android Engineer.
14. **CR L2, anonymous `TestResultPayload` for skipped payload.** `GaitTest.kt` lines 69 to 73 constructs an anonymous payload; the BilateralTapTest's skipped payload follows a similar pattern but uses a named factory. Cross battery shape consistency. Owner: Android Engineer.
15. **CR L3, swallowed throwable in `GaitTestViewModel` writer job.** Line 77 `catch (other: Throwable)` block swallows non `CancellationException` failures with the comment "process whatever samples we captured." The semantic is intentional but a future Observability Engineer pass should at least log the swallowed throwable for diagnostics. Owner: Observability Engineer (Phase 13 prep) or Phase 11 polish.

**INFORMATIONAL, Phase 11 polish (optional):**

16. **SPE I3 / PE I3r, `RawSensorWriter` virtuality vs interface based pattern.** The class is `open` and `write` is `open` to enable test doubling; the project's pattern for swappable components is a small interface (`ImuSource` is the example). A `RawSensorSink` interface plus a final production class is the more idiomatic shape. Phase 8 voice test can choose the interface pattern if preferred. Owner: Code Reviewer or whoever lands the voice writer.

The complete deferred item list mirrors the Code Reviewer's Task 12 findings count summary: 0 BLOCKING, 0 HIGH, 1 MEDIUM (PE M3, Phase 5 prep), 5 MINOR (1 closed in this phase as PE M1 cleanup commit `3c514f6`, 4 deferred per the table above), 5 LOW (all Phase 11), 3 INFORMATIONAL (2 Phase 5, 1 Phase 11), 3 PA medium severity (all Phase 11). The PM should carry these forward into `STATUS.md` Resume notes when flipping the phase row to `completed`.

### Manual walkthrough note

There is a Phase 4 user surface (the gait test flow: Instructions, Countdown, Capture, Done) but the manual emulator walkthrough remains the user's responsibility because headless QA agents cannot drive an AVD. The four Compose smoke tests (Condition 4 above) verify each screen renders without crashing in a Robolectric host and exposes the primary CTA semantic node, which is the automated coverage available; the visual and interaction confirmation on a Pixel class AVD running Android 12 or later is the user's task. The user driven multi device sample rate validation task above is the more consequential Phase 4 close validation deliverable; the visual walkthrough is a smaller follow up the user can run alongside the device measurements.

### Sign off

**Verdict: PASS WITH NOTES.** All eight falsifiable conditions hold at Phase 4 close. The Phase 4 cleanup commit the Code Reviewer required (PE M1, the dropped windows budget wording reconciliation in `docs/perf/latency-budgets.md`) landed at `3c514f6`. The remaining findings from SPE, Patient Advocate, Performance Engineer, and the Code Reviewer's per file pass are all deferrable to Phase 5 prep or Phase 11 polish per the consolidated table in `docs/qa/code-review-phase-4.md`. The user driven multi device sample rate validation per `docs/observability/sensor-runbook.md` is the one outstanding non code item Phase 4 defers to the user; the runbook documents the procedure and the acceptance budgets.

**Uncertainties and notes.**

- The `--rerun-tasks` invocation produced `BUILD SUCCESSFUL in 14s` with 30 actionable tasks executed and the full 102 test count. The result is reproducible: the same command run twice in a row from the same `HEAD` produces the same 102 tests, 0 failures, 0 errors, 0 skipped totals.
- The PM handoff note for this sign off named 11 commits from `4cd0a72` exclusive to HEAD. Direct verification with `git log --oneline 4cd0a72..HEAD | wc -l` returns 15. The two extra commits beyond the 13 Phase 4 implementation, review, and cleanup commits are the Phase 3 close STATUS flip (`874182a`) and the Phase 4 plan prep (`46641b7`). The 15 count is the authoritative one for this entry.
- The one known Gradle warning (`application@android:allowBackup was tagged at AndroidManifest.xml:5 to replace other declarations but no other declaration present`) is unchanged from Phase 1, Phase 2, and Phase 3. It does not affect the application's backup posture (`android:allowBackup="false"` and `android:fullBackupContent="false"` remain in place).
- I did not modify any application code, tests, or configuration during sign off; only this document was touched.
