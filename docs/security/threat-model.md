# Threat model, MS Neuro Battery

**Phase:** 0 (Bootstrap setup)
**Author role:** Security Engineer (agent 07)
**Date:** 2026-05-07
**Scope:** v1 Android application, native Kotlin and Jetpack Compose, on device only.

This document enumerates the trust boundaries, assets, threats (organized by STRIDE), mitigations already present in the design, and the residual risks the user accepts. It is the technical companion to `docs/security/hardening-checklist.md` (the enforceable rules) and to the Compliance Reviewer's `docs/security/compliance-review.md` (the regulatory positioning).

The privacy posture this document defends is described in `SPEC.md` Section 10 and Section 4 (non goals 4 and 5: no backend sync, no iOS in v1). The clinical motivation for that posture is the Floodlight Open analyses recorded in `docs/source/clinical-references.md` (Oh et al. 2024, *Scientific Reports*; Galati et al. 2024, *JMIR Human Factors*). MS Battery is positioned as the patient owned, on device, no INTERNET, no account alternative to a cloud based pilot.

## 1. Trust boundaries

The application has an unusually small attack surface because it has no server, no account system, and no network presence. The boundaries that exist are:

1. **Inside the app sandbox.** Code, the Room database, the app's `filesDir` and `cacheDir`, `SharedPreferences`. Protected by Android's per app UID isolation and by File Based Encryption (FBE) at rest on Android 10 plus.
2. **Between the app and the rest of the device.** The user's Android OS, other installed apps, system services, the keyboard, the launcher, the share sheet. The app crosses this boundary only when the user explicitly initiates an export through `ACTION_SEND` via a `FileProvider`, or grants a runtime permission (microphone, camera).
3. **Between the app's exported PDF or CSV and the receiving party.** Once the user shares the file (typically with their neurologist by email, messaging app, cloud drive, or USB transfer), the receiving party becomes the new custodian. The application has no further control.
4. **The absence of a server boundary.** The application has no backend, no cloud sync, no third party SDK that performs network I/O, and no `android.permission.INTERNET` declaration. There is therefore no remote attacker model: there is no remote endpoint to attack and no data in transit to intercept. This is a deliberate design choice and is enforced by the manifest and dependency checks in the hardening checklist.

There is no account boundary. There is no credential to phish, leak, replay, or compromise.

## 2. Assets

The following assets live inside the app sandbox. Sensitivity ratings reflect the harm to the user (an MS patient) if the asset were disclosed, modified, or destroyed, not the harm to a covered entity.

| Asset | Where it lives | Sensitivity | Notes |
|-------|---------------|-------------|-------|
| User profile (`UserProfileEntity`: date of birth, biological sex, dominant hand, MS type, height) | Room database in `/data/data/com.mustafan4x.msbattery/databases/` | High. Health condition disclosure. | MS type field can be `UNDISCLOSED`; the app does not require it. |
| Session metadata (`SessionEntity`) | Same Room database | Low to medium | Timestamps and device info; not personally identifying on its own, but linkable to the profile. |
| Test results (`TestResultEntity`: feature JSON, quality scores, foreign keys to sessions) | Same Room database | High | The longitudinal record of motor, cognitive, vision, and voice metrics is the most sensitive asset because it is the clinical signal itself. |
| Raw IMU traces (gait test, gzipped CSV) | App private files dir, referenced from `TestResultEntity.rawSensorRelativePath` | Medium to high | Raw 100 Hz accelerometer and gyroscope traces over a 30 second walk. Could in principle be re identifying through gait biometrics if exfiltrated alongside the profile. |
| Voice audio (transient) | In memory only during recording and feature extraction. Discarded after features are computed unless the user opts in to retention. | High while in memory, otherwise not retained | See `SPEC.md` Section 10. Retention is opt in, off by default. |
| Cached PDF and CSV exports | App `cacheDir`, served through `FileProvider` | High | Created on user demand, shared through `ACTION_SEND`. The OS may evict cache files at any time. |
| Onboarding disclaimer acknowledgment, settings, schema version | `SharedPreferences` and Room | Low | Operational state. |

There are no secrets in v1 (no API keys, no signing material at runtime). If any future change introduces a secret, it must be stored in the Android Keystore per the hardening checklist.

## 3. Threats by STRIDE

The STRIDE categories are evaluated against the threat actors named in the agent brief: a curious roommate or family member with intermittent access to the unlocked phone, a thief or finder of a lost phone (with or without the screen unlocked), a malicious app on the same device without root, a forensic adversary on a rooted device, and a malicious or careless party who receives an exported PDF.

### 3.1 Spoofing

| Threat | Likelihood | Impact | Mitigation |
|--------|-----------|--------|-----------|
| An attacker spoofs the user's identity to the application. | Not applicable. The application has no account, no login, no remote identity. There is nothing to spoof at the application layer. | None | Out of scope by design. |
| Another app spoofs the MS Battery package to receive the user's exported PDF (intent redirection). | Low | Medium | The user picks the recipient app from the share sheet on every export. The application does not auto target any single recipient. |
| An attacker spoofs the device's neurology user (i.e., uses the unlocked phone and pretends to be the patient when running a session, polluting the longitudinal record). | Medium for shared family devices | Medium (data integrity, not confidentiality) | Mitigation is procedural: the app is single user, the user is told this in onboarding, and the user is responsible for the device they install on. The application does not authenticate per session. This is documented as a residual risk in Section 5. |

### 3.2 Tampering

| Threat | Likelihood | Impact | Mitigation |
|--------|-----------|--------|-----------|
| A roommate with the unlocked phone deletes or edits sessions through the app's UI. | Medium for shared devices | Medium (loss of longitudinal record) | The data model is append only at the `Session` level (`SPEC.md` Section 8). Corrections produce a new session. Bulk deletion through the app UI is the user's prerogative as the data owner. Out of band tampering with the Room database file is blocked by app sandboxing on a non rooted device. |
| A malicious app on the same non rooted device modifies MS Battery's database. | Very low | High | Android per app UID isolation prevents it. App private storage at `/data/data/com.mustafan4x.msbattery/` is not readable or writable by other apps without root. |
| A forensic adversary on a rooted device modifies the database or raw sensor traces. | Low (requires physical possession plus root) | Medium | Out of scope as a primary attack model. Documented as a residual risk in Section 5. The user is the data owner; if their device is rooted by an adversary they have already lost the integrity battle for the device. |
| An attacker tampers with the exported PDF after it leaves the device. | Medium (file in transit through email, messaging, cloud drive) | Medium (could mislead a neurologist) | Out of the application's control once the user shares the file. The PDF includes a generated date, app version, and the disclaimer text. Cryptographic signing of exports is not in scope for v1; flagged for `future-ideas.md` if demand emerges. |

### 3.3 Repudiation

| Threat | Likelihood | Impact | Mitigation |
|--------|-----------|--------|-----------|
| A user disputes that a session was theirs. | Low | Low | The application is not used as a legal record. There is no notion of non repudiation in v1. The user is the sole owner of their own data. |
| A user disputes the contents of an exported PDF they shared with their neurologist. | Low | Low | Same as above. The PDF carries an in app generated date and a disclaimer that the application is not a medical device and is not for diagnosis. |

Repudiation is not a meaningful threat in this design because the application has no remote consumer of the data and no liability boundary that turns on attribution.

### 3.4 Information disclosure

This is the most important STRIDE category for this application. The assets in Section 2 are all health related.

| Threat | Likelihood | Impact | Mitigation |
|--------|-----------|--------|-----------|
| A roommate or family member opens the app on the unlocked phone and views the history. | Medium for shared devices | High | The application is sandboxed behind the device lock screen. Beyond that, the app does not implement an in app PIN in v1. App level lock is flagged as a candidate for `future-ideas.md` if user demand emerges. |
| A thief recovers the unlocked phone before the screen times out and reads the history. | Low to medium | High | Same as above. The user's device lock screen, biometric authentication, and screen timeout are the primary defense. The application surfaces the disclaimer that the device's lock posture is the user's responsibility. |
| A thief recovers the locked phone and attempts offline extraction of the database file. | Low | Medium to high | Android File Based Encryption (FBE) on Android 10 plus encrypts app private storage with keys tied to user credentials. The device must be unlocked at least once after boot to access Credential Encrypted (CE) storage; the database lives in CE storage by default. An attacker with the locked device cannot read the database without the user's lock screen credential. |
| A malicious app on the same device reads MS Battery's database or files. | Very low | High | Per app UID isolation. The application does not export any content provider that exposes data. The `FileProvider` used for share intents grants per URI temporary read access only, only when the user explicitly shares. |
| A forensic adversary with a rooted device extracts the database. | Low (requires physical possession plus root) | High | Out of scope as a primary defense target. SQLCipher is documented as a v1.1 enhancement (`SPEC.md` Section 8.2 and the hardening checklist) for users who want stronger protection beyond FBE. Documented as a residual risk in Section 5. |
| The user's session data is silently uploaded by Android Auto Backup to their Google account, and from there to a Google Drive subject to the same set of attackers as any cloud account. | Medium without an explicit choice (default `android:allowBackup` is true on Android 12 plus per the current manifest) | High | This is the most significant residual concern in the current scaffold. See Section 4 (mitigation in design) and Section 6 (decision flagged for the PM). |
| A third party SDK exfiltrates data over the network. | Not applicable in v1 | High | The application does not declare `android.permission.INTERNET`. No third party analytics, crash reporting, telemetry, or networking SDK is in the dependency set. The hardening checklist forbids adding one without explicit Security Engineer sign off, and the absence of `INTERNET` would block any such SDK at runtime even if it were added. |
| Microphone audio is retained beyond what the user expects. | Low | High | Voice audio is processed in memory and discarded after feature extraction unless the user opts in to retention (`SPEC.md` Section 10). Opt in is the default off setting. |
| Camera frames from the low contrast vision test ambient brightness check are retained. | Low | Medium | The vision test reads a single ambient brightness value from a CameraX preview frame and discards the frame. No video, no still image is persisted. This will be re reviewed when the vision test lands in Phase 6. |
| The clipboard or screenshots leak data. | Low to medium | Medium | The app does not write sensitive data to the clipboard. `FLAG_SECURE` to suppress screenshots and recents previews is flagged as a v1.1 enhancement, not a v1 requirement, because it interacts with accessibility tooling (TalkBack reading aloud is unaffected, but some screen reader workflows users rely on can be impacted). Decision deferred. |
| Logs (Logcat or in app diagnostic dump) leak PII. | Low | Medium | The Observability Engineer's logging runbook (`docs/observability/logging-runbook.md`, written in a later phase) defines the redaction rules. The hardening checklist forbids plaintext logging of `UserProfileEntity` fields, raw sensor traces, and audio samples. |

### 3.5 Denial of service

| Threat | Likelihood | Impact | Mitigation |
|--------|-----------|--------|-----------|
| A malicious app fills the device's storage so the Room database cannot be written. | Low | Low (no data lost; new sessions cannot be saved) | Out of scope. This is a generic device condition. The application surfaces a clear error message if it cannot write. |
| A pathological raw sensor trace (very long, very large) fills the app's files dir and prevents future captures. | Low | Low | The gait raw IMU file is bounded by the 30 second test window. Estimated under 60 KB per session compressed (`SPEC.md` Section 8.1). Annual storage per user is under 4 MB. There is no realistic path to denial of service from the app's own data. |
| A user retention attack: a malicious app pushes notifications mimicking MS Battery to disrupt the weekly cadence. | Low | Low | Out of scope as a security concern. Notification spoofing protection is a platform concern. |

Denial of service is not a meaningful primary threat for an offline single user application. The user is also the operator; if they decide to stop using the app there is no service availability obligation.

### 3.6 Elevation of privilege

| Threat | Likelihood | Impact | Mitigation |
|--------|-----------|--------|-----------|
| A malicious app exploits an exported component of MS Battery to gain access to the database. | Low | High | The application exports only the `MainActivity` (with the standard `MAIN`/`LAUNCHER` intent filter) per the current `AndroidManifest.xml`. No content provider, no service, no broadcast receiver is exported. The `FileProvider` that will be added in Phase 10 for share intents grants per URI temporary read access only, scoped to `cacheDir`. |
| A native code vulnerability in a transitive dependency is exploited to escape the sandbox. | Very low | High | The dependency set is conservative, well maintained AndroidX libraries plus Room, Compose, kotlinx serialization, kotlinx coroutines, and test only libraries (JUnit 4, Robolectric, MockK, kotlinx coroutines test, Room testing). The Security Engineer audits every new dependency before it lands on `main`. |
| The application requests dangerous runtime permissions and the user grants more than is needed. | Low | Low | Microphone (voice test) and camera (vision test ambient brightness) are the only dangerous runtime permissions planned. Both are requested only when the user is about to run the relevant test, and the application functions without them (the affected test is skipped). |

## 4. Mitigations already in design

The following controls are baked into the design and the Phase 0 scaffold. They are restated here so the threat model can be read as a self contained document.

1. **No `android.permission.INTERNET`.** The current `AndroidManifest.xml` at `/home/mustafa/src/MS-Battery/app/src/main/AndroidManifest.xml` declares no `uses-permission` element. The merged manifest contains only the auto injected `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (a custom, app scoped permission generated by AndroidX, not a network or otherwise dangerous permission).
2. **No third party analytics, crash reporting, telemetry, or networking dependencies.** The dependency set in `app/build.gradle.kts` is: Compose BOM 2024.06.00, Material 3, Compose UI tooling, AndroidX Activity Compose 1.9.0, Lifecycle 2.8.0, Navigation Compose 2.7.7, Room 2.6.1 (runtime, ktx, compiler), kotlinx coroutines android 1.8.1, kotlinx serialization json 1.6.3, plus test only dependencies (JUnit 4, kotlinx coroutines test, Room testing, Robolectric 4.12.2, AndroidX test core and ext junit, MockK 1.13.11). None of these libraries perform network I/O at runtime in normal usage. Test libraries do not ship in the release APK.
3. **Room database lives in app private storage.** Inaccessible to other apps on a non rooted device. Encrypted at rest by Android File Based Encryption on Android 10 plus, with keys tied to user credentials.
4. **Append only data model at the session level.** Limits the blast radius of accidental tampering through the app UI.
5. **No account, no login, no server side identifier.** There is no credential to compromise.
6. **Microphone audio is processed in memory only by default.** Retention is opt in, off by default.
7. **Camera frames are read once for ambient brightness and discarded.** No video or still image is persisted by the vision test.
8. **Export is user initiated only.** Through Android's `ACTION_SEND` Share Intent and a `FileProvider` scoped to `cacheDir`. The application has no automatic, scheduled, or background export path. No `WorkManager`, `JobScheduler`, `ForegroundService`, `BroadcastReceiver`, or `ContentProvider` is wired for outbound data flow in v1.
9. **Onboarding disclaimer.** First launch presents the "this is not a medical device" notice, which the user must acknowledge. This is recorded so that the onboarding flow is not skipped silently.
10. **Conservative target SDK and runtime permission posture.** Min SDK 31 (Android 12), target SDK 34 (Android 14). Dangerous runtime permissions (microphone, camera) are requested at point of use only.

## 5. Residual risks

The following risks remain after the controls in Section 4 and are explicitly accepted for v1.

1. **Device theft with the screen unlocked.** If an adversary has physical possession of the user's device while it is unlocked, they can open the app and view the history. The defense is the user's lock screen and screen timeout, both of which are outside the application's control. An in app PIN or biometric prompt is flagged for `future-ideas.md`.
2. **Forensic adversary on a rooted device.** An adversary with physical possession plus root can read the Room database file and the raw IMU traces. Optional SQLCipher is documented as a v1.1 path for users who want stronger protection beyond FBE.
3. **Malicious or careless sharing partner.** Once the user has shared an exported PDF with their neurologist (or anyone else), the receiving party becomes the new custodian. The application has no control over what the recipient does with the file. This is true of any patient generated artifact.
4. **Lost or cloned device backup, if Android Auto Backup is enabled.** The current manifest sets `android:allowBackup="true"` and `android:fullBackupContent="@xml/backup_rules"` (with `<full-backup-content>` empty, which under Android Auto Backup defaults to including most of the app's data) and `android:dataExtractionRules="@xml/data_extraction_rules"` (with `<cloud-backup>` empty, same default). Under these defaults the user's session data, profile, and raw sensor traces could be uploaded to the user's Google account as part of system backup. From there, they are subject to the same set of attackers as any cloud account: phished credentials, account takeover, lawful access to Google. This is the most significant residual gap relative to the spec's "all computation on device, no cloud" claim. The recommended mitigation is to set `android:allowBackup="false"` and remove the data extraction rules reference. See Section 6 for the decision flagged to the PM.
5. **No code signing of exports.** A PDF or CSV exported from the app and forwarded to a neurologist could be modified by anyone in the chain. The application does not cryptographically sign exports in v1. This is acceptable because the document is supplementary, not diagnostic, per `SPEC.md` Section 4 non goals.
6. **Shared device pollution.** If multiple people use the same device profile (a couple sharing a phone), session results from one user could be entered against the other's profile. The application is single user and does not authenticate per session. Documented in onboarding.
7. **Microphone retention if the user opts in.** The opt in retention path is honest about itself in the UI. The user is responsible for understanding what they have enabled.

## 6. Decisions for the PM

These items require user level decisions before the Security Engineer can sign them off as either mitigations in Section 4 or accepted residual risks in Section 5.

1. **`android:allowBackup` and `android:fullBackupContent` for v1.** The current scaffold defaults are inconsistent with the spec's "no cloud" privacy posture. The Security Engineer's recommendation is to set `android:allowBackup="false"` and `android:fullBackupContent="false"` (or remove the rules file references), so that the user's session data cannot be silently uploaded to Google's cloud as part of system backup. This is flagged for the PM rather than implemented unilaterally because it is a behavior change visible to users (system backup of this app stops working) and the agent brief states it is a decision for the PM. The Compliance Reviewer should also weigh in: a privacy claim like "no data leaves the device unless the user shares it" must be both technically true (Security Engineer sign off) and consistent with regulatory positioning (Compliance Reviewer sign off) per agent 07's coordination notes.
2. **In app lock (PIN or biometric) on app launch.** Defers to `future-ideas.md` unless the PM elects to bring it into v1.
3. **`FLAG_SECURE` on screens displaying history or PDF previews.** Defers to `future-ideas.md`. Trade off is screenshots from the user's own workflow.
4. **SQLCipher path for users who want stronger protection beyond FBE.** Recorded as a v1.1 enhancement in `SPEC.md` Section 8.2 and in the hardening checklist. Not a v1 requirement.

## 7. Veto rules (Security Engineer)

Per agent 07's brief, the Security Engineer holds veto power on the following kinds of change. Override is only available to the user.

1. **No `android.permission.INTERNET`.** Any PR that adds it is rejected by default.
2. **No new networking dependency.** Any PR that adds a library that performs network I/O at runtime (OkHttp, Retrofit, Ktor, Volley, Apollo, Firebase remote services, any analytics SDK, any crash reporting SDK other than the platform default Play Console crash reporting which does not require code changes, any A B testing SDK, any feature flag SDK that fetches remotely) is rejected by default.
3. **No third party analytics, telemetry, or behavioral tracking SDK.** Same as above.
4. **No backend service of any kind.** The application is on device. Any backend implies the absence of `INTERNET` is a lie, which would invalidate the privacy claim in `SPEC.md` Section 10.
5. **No automatic upload, automatic sync, or background data flow leaving the device.** Export is user initiated only.
6. **No silent change to the backup posture.** Any change to `android:allowBackup`, `android:fullBackupContent`, or the data extraction rules requires explicit Security Engineer sign off and a corresponding update to this document and the hardening checklist.

The veto exists because the project's clinical and resume value depends on the privacy posture being defensible in plain language. A single regression on any of the above invalidates the posture and would require rewriting the spec, the README, and the user facing onboarding text.

## 8. Re review triggers

This document is re reviewed by the Security Engineer at any of the following events, per agent 07's "every subsequent phase" tasks.

1. A new dependency is added to `app/build.gradle.kts` (any phase).
2. A new permission is added to `AndroidManifest.xml` (Phase 6 for camera, Phase 8 for microphone, Phase 10 for the FileProvider, plus any later additions).
3. A new path is added that writes outside `Context.filesDir` or `Context.cacheDir`.
4. The release build configuration changes (Phase 11 for R8 and signing).
5. Any change to the backup posture, the export flow, or the recipient apps the share sheet is allowed to surface.
