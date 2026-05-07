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
  - `com.mustafan4x.msbattery.data.EnumsTest`: 5 tests, all pass.
  - `com.mustafan4x.msbattery.data.ConvertersTest`: 5 tests, all pass.
  - `com.mustafan4x.msbattery.data.UserProfileDaoTest`: 2 tests, all pass.
  - `com.mustafan4x.msbattery.data.SessionDaoTest`: 3 tests, all pass.
  - `com.mustafan4x.msbattery.data.TestResultDaoTest`: 1 test, all pass.
  - `com.mustafan4x.msbattery.battery.MockTestModuleTest`: 2 tests, all pass.
  - `com.mustafan4x.msbattery.battery.BatteryOrchestratorTest`: 4 tests, all pass.
  - `com.mustafan4x.msbattery.battery.BatteryFlowIntegrationTest`: 1 test, passes.

**Commit chain on `main` from `60b370e..HEAD`.** 12 commits verified.

1. `7499417` feat(data): add TestType, Sex, Hand, MSType enums (Task 2)
2. `8f4a25a` feat(data): add Room Converters, UserProfileEntity, DAO, and AppDatabase skeleton (Task 3)
3. `94b62e5` feat(data): add Session and TestResult entities with cascade delete (Task 4)
4. `5e0edea` feat(battery): define TestModule interface and TestResultPayload (Task 5)
5. `49dd9fb` feat(battery): add MockTestModule for scaffolding (Task 6)
6. `4a928eb` feat(battery): implement BatteryOrchestrator state machine (Task 7)
7. `d59efa5` feat(ui): add disclaimer and profile setup screens (Task 8)
8. `45df915` feat(ui): add home, session runner, and settings screens (Task 9)
9. `47e5cf4` feat(app): wire RootScreen, MSBatteryApp Application class, and MainActivity (Task 10)
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
  - `com.mustafan4x.msbattery.battery.BatteryFlowIntegrationTest`: 1 test, passes (carried from Phase 1).
  - `com.mustafan4x.msbattery.battery.BatteryOrchestratorTest`: 4 tests, all pass (carried from Phase 1).
  - `com.mustafan4x.msbattery.battery.MockTestModuleTest`: 2 tests, all pass (carried from Phase 1).
  - `com.mustafan4x.msbattery.battery.tap.BilateralTapTestMetadataTest`: 1 test, passes (new in Phase 2).
  - `com.mustafan4x.msbattery.battery.tap.BilateralTapTestRenderTest`: 1 test, passes (new in Phase 2).
  - `com.mustafan4x.msbattery.battery.tap.TapFeaturesTest`: 12 tests, all pass (new in Phase 2).
  - `com.mustafan4x.msbattery.data.ConvertersTest`: 5 tests, all pass (carried from Phase 1).
  - `com.mustafan4x.msbattery.data.EnumsTest`: 5 tests, all pass (carried from Phase 1).
  - `com.mustafan4x.msbattery.data.Migration1To2Test`: 2 tests, all pass (new in Phase 2: Room v1 to v2 migration for nullable `heightCm`).
  - `com.mustafan4x.msbattery.data.SessionDaoTest`: 3 tests, all pass (carried from Phase 1).
  - `com.mustafan4x.msbattery.data.TestResultDaoTest`: 1 test, passes (carried from Phase 1).
  - `com.mustafan4x.msbattery.data.UserProfileDaoTest`: 2 tests, all pass (carried from Phase 1).
  - `com.mustafan4x.msbattery.ui.common.EnumLabelsTest`: 4 tests, all pass (new in Phase 2: human readable enum label resolver).
  - `com.mustafan4x.msbattery.ui.home.HomeScreenFormattersTest`: 3 tests, all pass (new in Phase 2: relative date formatters).
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
