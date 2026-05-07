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
