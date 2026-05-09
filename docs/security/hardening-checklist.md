# Hardening checklist, BaselineMS

**Phase:** 0 (Bootstrap setup)
**Author role:** Security Engineer (agent 07)
**Date:** 2026-05-07
**Companion document:** `docs/security/threat-model.md`.

This checklist enumerates the concrete, enforceable rules every PR is checked against. Where the rule can be enforced mechanically (CI grep, dependency report, manifest scan), the enforcement mechanism is named so DevOps can wire it. Where the rule requires human review, the reviewer role is named.

The checklist is written so that "PR violates rule N" is a yes or no determination, not a judgment call. Where a judgment call is unavoidable, the rule says so explicitly.

## A. Manifest rules

### A.1. No `android.permission.INTERNET` in any manifest variant.

**Enforceable in CI.** Recommended check (the PM can dispatch DevOps to wire this; the Security Engineer does not modify CI directly):

```yaml
- name: No INTERNET permission
  run: |
    if grep -r "android.permission.INTERNET" app/src/; then
      echo "FAIL: android.permission.INTERNET declared in app/src/. Security Engineer veto."
      exit 1
    fi
```

The grep covers `app/src/main`, `app/src/debug`, and any future build variants. The merged manifest at build time will also be inspected by the Security Engineer at every release; the merged manifest currently contains only the AndroidX auto injected `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, which is a custom, app scoped, non network permission and is acceptable.

**Current state, verified 2026-05-07:** `/home/mustafa/src/BaselineMS/app/src/main/AndroidManifest.xml` declares no `uses-permission` element. Pass.

### A.2. Every new permission is reviewed by the Security Engineer before merge.

Planned additions through the existing phase plan:

| Phase | Permission | Why |
|-------|-----------|-----|
| 6 | `android.permission.CAMERA` (runtime) | Vision test ambient brightness check via CameraX. |
| 8 | `android.permission.RECORD_AUDIO` (runtime) | Voice reading test. |
| 10 | `FileProvider` declaration in the manifest (no permission, but a manifest change) | Share Intent target for PDF and CSV exports. |

All three are dangerous runtime permissions or sensitive manifest declarations and require a Security Engineer review on the PR that adds them. Any other permission requires explicit user override.

### A.3. No exported components beyond the launcher activity.

The current `MainActivity` declares the standard `MAIN`/`LAUNCHER` intent filter and `android:exported="true"`. This is acceptable. No content provider, service, broadcast receiver, or additional activity is exported in v1 except the `FileProvider` added in Phase 10, which is documented in agent 07's "every subsequent phase" tasks and requires Security Engineer sign off.

### A.4. Backup and data extraction posture.

The current scaffold has `android:allowBackup="true"`, `android:fullBackupContent="@xml/backup_rules"`, and `android:dataExtractionRules="@xml/data_extraction_rules"`, with both XML rule files using the Android Studio defaults (effectively "include everything"). This is inconsistent with the spec's "no cloud" privacy posture: the user's session data could be silently uploaded to their Google account through Android Auto Backup.

**Security Engineer's recommendation:** set `android:allowBackup="false"`, set `android:fullBackupContent="false"`, and remove the data extraction rules reference (or set it to a rules file that excludes everything). This change is **flagged for PM decision**, not implemented unilaterally, because the agent brief specifies it as a decision item.

If the PM accepts the recommendation, the change is made by the Android Engineer in a single PR that updates the manifest and removes the now unused XML rule files. If the PM defers, the threat model's Section 5 residual risk on backup remains in force and must be acknowledged in the README's privacy section.

## B. Dependency rules

### B.1. No third party analytics, telemetry, behavioral tracking, crash reporting, A B testing, feature flag, or networking SDKs.

**Enforceable in CI by deny list.** Recommended addition (PM dispatches DevOps to wire):

```yaml
- name: Forbidden dependencies deny list
  run: |
    PATTERN='okhttp|retrofit|ktor|volley|apollo|firebase|crashlytics|google-analytics|mixpanel|amplitude|segment|sentry|datadog|newrelic|appsflyer|adjust|braze|onesignal|posthog|launchdarkly|optimizely|split-io|statsig'
    if grep -i -E "\"($PATTERN)" app/build.gradle.kts; then
      echo "FAIL: forbidden dependency declared. Security Engineer veto."
      exit 1
    fi
```

The deny list is illustrative, not exhaustive. The Security Engineer audits every new dependency on its merits before merge. The deny list is a tripwire for the most common slips, not a substitute for review.

### B.2. Every new dependency is audited before it lands on `main`.

The audit names: dependency, version, license, transitive dependencies, network behavior at runtime, privacy implications, and a Security Engineer sign off line. Audit entries live in `docs/security/dependency-audits.md` (created when the first new dependency lands).

**Current state, verified 2026-05-07:** `app/build.gradle.kts` declares the following runtime dependencies. None perform network I/O in normal usage. Test only dependencies are excluded from this list because they do not ship in the release APK.

| Dependency | Version | Network behavior |
|-----------|---------|------------------|
| `androidx.compose:compose-bom` | 2024.06.00 | None |
| `androidx.compose.material3:material3` | (BOM) | None |
| `androidx.compose.ui:ui` | (BOM) | None |
| `androidx.compose.ui:ui-tooling-preview` | (BOM) | None |
| `androidx.compose.ui:ui-tooling` (debug only) | (BOM) | None |
| `androidx.activity:activity-compose` | 1.9.0 | None |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.8.0 | None |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.8.0 | None |
| `androidx.navigation:navigation-compose` | 2.7.7 | None |
| `androidx.room:room-runtime` | 2.6.1 | None |
| `androidx.room:room-ktx` | 2.6.1 | None |
| `androidx.room:room-compiler` (KSP) | 2.6.1 | None (build time only) |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.8.1 | None |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.6.3 | None |

Test only dependencies (JUnit 4 4.13.2, kotlinx coroutines test 1.8.1, Room testing 2.6.1, Robolectric 4.12.2, AndroidX test core 1.5.0, AndroidX test ext junit 1.1.5, MockK 1.13.11) are confined to `testImplementation` and do not appear in the release APK.

The dependency set is clean for Phase 0. Pass.

### B.3. Anticipated future dependencies require pre merge audit.

Per `SPEC.md` Section 14 open questions and per the planned Phase roster:

- **Vico** (Compose chart library) in Phase 9. Renders charts on device; no network. Audit on PR.
- **TarsosDSP** (audio analysis) is being considered in Phase 8. Java audio DSP library; no network in normal use, but the Security Engineer must verify against the published source before sign off. Audit on PR.
- **ML Kit face detection** in Phase 6 if the optional viewing distance estimate via face size is implemented. ML Kit on device models do not require network at runtime once the model is bundled, but ML Kit has a path that downloads models on demand from Google Play services; the on device only API must be used and verified. Audit on PR.

No backend SDK, no Firebase Remote Config, no Firebase analytics, no Cloud Firestore, no Firebase Authentication is permitted.

### B.4. Dependency versions are pinned, not ranges.

The current `app/build.gradle.kts` pins exact versions on every direct dependency (no `+`, no `[)` ranges). Maintain this.

## C. Build and release configuration

### C.1. R8 (or ProGuard) is enabled in the release build.

**Current state:** `isMinifyEnabled = false` in the release build type, per `app/build.gradle.kts` and per the Phase 1 detailed plan in `docs/plans/phase-1-foundation.md`. This is acceptable for Phase 0 and Phase 1 (debug only builds, no release builds are produced). Before any release build is produced (Phase 11 at the latest), R8 must be enabled.

**Phase 11 entry criterion:** `isMinifyEnabled = true` in the release build type, and the project compiles, tests pass, and the resulting APK runs on a real device with all five tests functional. The Security Engineer signs off on the ProGuard or R8 rules at that time.

### C.2. Signing keystore is stored outside the repository.

Phase 11 deliverable. The keystore must live outside the git repository, referenced from a gitignored `keystore.properties` (or similar mechanism). The Security Engineer co reviews this with the DevOps Engineer at release time per agent 07's Phase 11 task list. Until Phase 11, debug builds use the standard Android debug keystore, which is acceptable.

### C.3. Release builds disable debug logging and tooling.

Phase 11 deliverable. `BuildConfig.DEBUG` gates verbose logging. Compose UI tooling (`androidx.compose.ui:ui-tooling`) is already declared `debugImplementation` only and does not ship in release.

## D. Storage and data handling rules

### D.1. Room database lives in app private storage.

Default behavior. The application must not pass an absolute path to `Room.databaseBuilder` that points outside `Context.getDatabasePath(...)`. The Phase 1 detailed plan and the Data Engineer's brief enforce this; the Security Engineer reviews the Phase 1 PR that creates the `AppDatabase`.

### D.2. File Based Encryption (FBE) is the default at rest protection.

Android 10 plus encrypts app private storage with keys derived from the user's lock screen credential. The application's database, files dir, and cache dir are all encrypted at rest by the platform. No additional code is required.

This is the documented assumption. If it ceases to be true (e.g., the user disables their lock screen, in which case the device is not encrypted), the application's protections degrade to per app UID isolation only on a non rooted device. This is a property of the user's device posture, not of the application.

### D.3. Optional SQLCipher path is a v1.1 enhancement.

`SPEC.md` Section 8.2 and `docs/plan.md` "What lives outside this plan" both mark SQLCipher as outside v1. The hardening checklist re affirms this. SQLCipher is not required for v1; it remains available as a v1.1 enhancement for users who want stronger protection beyond FBE. Adding it in v1 would require a new dependency audit and a re review of the threat model.

### D.4. No PII (UserProfile fields, raw sensor traces, audio samples) in any log statement.

Reviewer enforced. The Code Reviewer flags any `Log.d`, `Log.i`, `Log.w`, `Log.e`, `println`, or other logging call that includes a `UserProfileEntity` field, a raw IMU sample, or any audio buffer content. The Observability Engineer's logging runbook (written in a later phase) defines the precise redaction rules and the diagnostic dump format. Until that runbook lands, the conservative default is: no logging of any field defined in `SPEC.md` Section 8 except an opaque session UUID.

### D.5. Files written outside `Context.filesDir` and `Context.cacheDir` require Security Engineer sign off.

The application writes only to `filesDir` (raw IMU traces) and `cacheDir` (PDF and CSV exports prior to share). No external storage, no `Environment.getExternalStorageDirectory`, no `MediaStore`, no `DocumentsContract` writes from background code paths. Any PR that introduces such a write requires explicit Security Engineer sign off.

### D.6. Voice audio is processed in memory only by default.

`SPEC.md` Section 10 specifies that raw audio is discarded after feature extraction unless the user opts in. The Phase 8 PR for the voice test must demonstrate this in code: `AudioRecord` buffers are released; no buffer escapes to disk on the default code path. The Security Engineer reviews the Phase 8 PR.

## E. Secrets

### E.1. No secrets are expected in v1.

There is no API key, no signing key embedded in the APK, no third party SDK key. The signing key for the release APK is held outside the repository per C.2.

### E.2. Any future secret reads from the Android Keystore.

If any later phase introduces a secret (for example, the optional SQLCipher v1.1 path needs a database key), the secret is generated and stored in the Android Keystore (`AndroidKeyStore` provider) using a `KeyGenParameterSpec` with `setUserAuthenticationRequired` set per the user's chosen posture. The secret never appears in `SharedPreferences`, never appears in a config file, and is never logged.

### E.3. No secrets in the repository.

The repository contains no API keys, no tokens, no credentials. The `.gitignore` covers the standard set (`local.properties`, `*.jks`, `keystore.properties`, build outputs). DevOps maintains the gitignore; the Security Engineer flags additions if necessary.

## F. Export and share posture

### F.1. Export is user initiated only.

The application has no automatic, scheduled, or background export. There is no `WorkManager` job, no `JobScheduler` job, no `ForegroundService`, no `BroadcastReceiver` that can trigger an export without an explicit user action in the app's UI.

### F.2. Export uses `ACTION_SEND` via `FileProvider`.

Phase 10 deliverable. The `FileProvider` paths are scoped to `cacheDir` and grant per URI temporary read access only. The Security Engineer reviews the Phase 10 PR.

### F.3. No automatic cloud upload.

The application has no `INTERNET` permission and no networking dependency. There is therefore no path for an automatic cloud upload at runtime. This rule is restated explicitly because it is the strongest privacy claim the application makes and because future PRs may try to relax it for what feels like a benign reason.

### F.4. No system wide background services that could leak data.

The application does not declare any `Service`, `IntentService`, `JobIntentService`, or `ForegroundService` in v1. If a later phase needs one (for example, a long running gait capture that survives an Activity restart), the Security Engineer reviews the PR that adds it and verifies that no data flow leaves the app sandbox from the service.

## G. CI hardening recommendations (for DevOps to implement)

These recommendations are for the PM to dispatch to DevOps if approved. The Security Engineer does not modify the CI workflow directly per the role coordination rules.

1. **Manifest grep for `android.permission.INTERNET`** (rule A.1 above).
2. **Forbidden dependencies deny list grep** (rule B.1 above).
3. **AGP `lint` task in CI** with `lintOptions { abortOnError = true }` for at least the security relevant categories. The Phase 1 detailed plan introduces lint as part of the foundational build setup; the Security Engineer signs off on the lint configuration at that time.
4. **Dependency report artifact.** A CI step that produces `./gradlew app:dependencies` output as a build artifact, so dependency drift between PRs is visible without running the build locally.
5. **No commit hooks bypass.** The CI must run on every PR to `main`; merge protection rules on the GitHub repository should require the CI green status before merge. (DevOps already wired the workflow; the branch protection rule is the remaining item.)

## H. Re review triggers

This checklist is re reviewed by the Security Engineer at any of the following events.

1. A new dependency is added to `app/build.gradle.kts`.
2. A new permission is added to `AndroidManifest.xml`.
3. The release build configuration changes.
4. A new file write path is introduced.
5. The backup posture changes.
6. A new component (activity, service, content provider, broadcast receiver, file provider) is exported.
7. A new release is being prepared (Phase 11).
