# Architecture, BaselineMS

**Status:** Phase 0 deliverable, written by the Documentation Engineer.

**Audience:** a new contributor cloning the repository, a recruiter reading the project, or a returning agent re orienting at the start of a session. This document is a navigation aid. The source of truth for what is being built is `SPEC.md`. The source of truth for which phase is in progress is `STATUS.md`.

**Scope:** the layered architecture of the v1 native Android application, the seam between the orchestrator and the test modules, where the gait module sits, the privacy posture that constrains every layer, the build and CI shape, and a pointer index into the rest of the documentation tree.

This document is intentionally short. It does not duplicate the specification. It explains how the pieces fit together and where to read more.

## 1. What the application is, in one paragraph

BaselineMS is a native Android application built with Kotlin and Jetpack Compose. It lets a person living with Multiple Sclerosis self administer a short, sensor backed set of five tests once a week, persists the results on device, and exports a clinician facing PDF report on demand. The application has no backend, no account, no cloud sync, and no `android.permission.INTERNET` declaration. The technical centerpiece is the gait analysis pipeline (Section 4 below). The full design rationale is in `SPEC.md` Section 1 and Section 2.

## 2. Layered architecture

The application is organized into clearly separated layers. The diagram below is reproduced from `SPEC.md` Section 5.1 so this document can stand on its own.

```
┌────────────────────────────────────────────────────────────────┐
│                    Compose UI (Activity + Screens)              │
│  Onboarding, weekly session prompt, history, settings, PDF      │
└──────────────────────────┬─────────────────────────────────────┘
                           │
                ┌──────────▼───────────┐
                │  Battery Orchestrator │
                │  (ViewModel) runs the │
                │  5 tests in sequence, │
                │  handles pause/resume │
                └──────────┬───────────┘
                           │
   ┌─────────┬─────────────┼─────────────┬─────────┐
   │         │             │             │         │
┌──▼──┐  ┌───▼───┐  ┌──────▼──────┐  ┌───▼───┐ ┌───▼───┐
│ Tap │  │ Gait  │  │ Low Contrast │  │ SDMT  │ │ Voice │
│Test │  │ Test  │  │   Vision     │  │ Test  │ │ Test  │
└──┬──┘  └───┬───┘  └──────┬───────┘  └───┬───┘ └───┬───┘
   │         │             │              │         │
   └─────────┴─────┬───────┴──────┬───────┴─────────┘
                   │              │
          ┌────────▼────┐  ┌──────▼─────┐
          │  Signals     │  │   Signal    │
          │  SensorMgr   │  │ Processing  │
          │  AudioRecord │  │  (DSP, gait │
          │  CameraX     │  │  pipeline)  │
          └────────┬─────┘  └──────┬─────┘
                   │              │
                   └──────┬───────┘
                          │
                ┌─────────▼─────────┐
                │   Data Store       │
                │  (Room, encrypted, │
                │   on device)       │
                └─────────┬─────────┘
                          │
                ┌─────────▼─────────┐
                │ Reporting + Trends │
                │ (Compose charts,   │
                │  PdfDocument)      │
                └────────────────────┘
```

## 3. Layer responsibilities and invariants

Each layer has one job and one set of inputs. These invariants are reproduced from `SPEC.md` Section 5.2; if this document and the spec ever disagree, the spec is the source of truth.

- **Compose UI.** Composable screens and view state. Owns navigation (Navigation Compose), onboarding, the weekly session entry point, history visualizations, settings, and PDF export UI. Has no direct access to sensors. Visual tokens (color, typography, spacing, tap targets, motion) come from `docs/design/tokens.md`, which is the source of truth for the design layer.
- **Battery Orchestrator.** A `ViewModel` that owns the lifecycle of a `Session`. Runs each `TestModule` in sequence, persists the result of each test as it completes, and handles pause and resume gracefully if the user is interrupted.
- **Test Modules.** Each conforms to the `TestModule` interface described in Section 5 below. Each module encapsulates its own composable, its own sensor needs, and its own result type. The orchestrator does not know how a test works internally.
- **Signals.** The only code in the application that touches `SensorManager`, `AudioRecord`, or `CameraX`. Exposes sensor streams as Kotlin `Flow` values that can be replaced with mock flows in tests. Lives in the layer that the Sensor Integration Engineer (agent 18) owns.
- **Signal Processing.** Pure Kotlin. No sensor or UI imports. Takes arrays or flows of samples in, returns features out. Heavily unit tested. The gait DSP module (Phase 3) and the voice acoustic feature module (Phase 8) live here.
- **Data Store.** Room. Append only at the `Session` level: completed sessions are immutable. Lives in app private storage; encryption posture is described in Section 6 below.
- **Reporting.** Reads from the Data Store, renders trend charts with a Compose charts library (Vico), generates PDF reports via Android's `PdfDocument`.

The "no sensor or UI imports in signal processing" invariant is what lets the gait pipeline be unit tested on the JVM against synthetic IMU traces without needing an instrumented test runner. It is also what lets the project hit its test coverage target on the signal processing module.

## 4. The gait module is the technical centerpiece

The gait test is the most technically rich module in the application. It is implemented as a conventional, well documented signal processing pipeline rather than as an opaque machine learning model so that every step is inspectable and testable.

A summary of what the pipeline does:

1. Captures 100 Hz IMU data (linear acceleration, gyroscope, rotation vector) over a 30 second walk via `SensorManager`.
2. Estimates phone orientation in the world frame using the platform's `TYPE_ROTATION_VECTOR` and, in parallel, a Madgwick filter implemented from raw gyro plus accelerometer for cross checking.
3. Removes gravity from acceleration.
4. Low pass filters at 20 Hz with a 4th order zero phase Butterworth.
5. Detects step events via a peak finder on vertical linear acceleration, with prominence and inter peak distance constraints to reject wobbles and pauses.
6. Pairs successive same foot steps into strides and assigns left or right from the sign of lateral acceleration.
7. Extracts cadence, stride length (zero velocity update integration between mid stance instants), step time variability, stride asymmetry, and double support time.
8. Reports a quality score on `[0, 1]` derived from step count, orientation residual error, and trajectory straightness.

The full pipeline, including filter parameters, the Madgwick formulation, the zero velocity update method, and the rationale for choosing per stride features over a global distance metric, is in `SPEC.md` Section 7. The validation strategy (synthetic ground truth, a measured 25 meter walking course, and test retest reliability) is in `SPEC.md` Section 7.2 and Section 9. Phase 5 of the implementation plan delivers the validation experiments and the published numbers.

The choice between rolling our own Madgwick implementation and depending on an existing library is deferred to an ADR written in Phase 3 by the Signal Processing Engineer with help from this role. Until that ADR is written, both paths remain open.

## 5. The TestModule seam

Adding a sixth test in the future should be a matter of conforming a new type to `TestModule` and registering it with the orchestrator. No changes are required elsewhere in the architecture. The interface is reproduced here from `SPEC.md` Section 5.3 because it is the central seam of the application.

```kotlin
interface TestResultPayload {
    val qualityScore: Double  // 0..1, was the test administered cleanly
    val features: Map<String, Double>  // flat feature map for storage and reporting
}

interface TestModule {
    val testType: TestType
    val displayName: String
    val instructions: String
    val estimatedDurationSeconds: Int

    @Composable
    fun Content(onComplete: (TestResultPayload) -> Unit)
}
```

The orchestrator hands each `TestModule` a callback. The module renders itself, captures its own sensor data, runs its own signal processing, computes its features, and calls the callback exactly once with a `TestResultPayload`. The orchestrator persists the payload and advances. If a test fails its quality check, the orchestrator records the failure and offers a retry without rolling back the rest of the session.

Everything the application does at the test level passes through this interface. The orchestrator is generic; the test modules carry the specifics.

## 6. Privacy posture

The application is on device only. The privacy posture is what gives the project its credibility on a resume and what keeps it outside HIPAA scope and outside FDA Software as a Medical Device classification. The constraints below are not aspirational; they are baked into the architecture.

- **No `android.permission.INTERNET` declaration.** The merged Android manifest contains no networking permission. A user can verify this by inspecting the APK manifest. The Security Engineer (agent 07) and the Compliance Reviewer (agent 21) hold a joint veto on any change that would add this permission.
- **No account, no login, no remote identifier.** There is nothing to phish, leak, replay, or compromise.
- **No third party analytics, telemetry, crash reporting, or networking SDK.** The dependency set is conservative AndroidX libraries plus Room, Compose, kotlinx serialization, kotlinx coroutines, and test only libraries.
- **All data is stored in the application's private storage.** The Room database lives in `/data/data/<package>/databases/`, inaccessible to other apps on a non rooted device, and encrypted at rest by Android's File Based Encryption on Android 10 plus, with keys tied to the user's lock screen credential.
- **Microphone audio is processed in memory only by default.** Acoustic features are persisted; raw audio is discarded after feature extraction unless the user explicitly opts in to retention.
- **Camera frames from the vision test ambient brightness check are read once and discarded.** No video or still image is persisted.
- **Export is the only outward facing action and it is fully user initiated.** Through Android's `ACTION_SEND` Share Intent with a `FileProvider` scoped to the application's `cacheDir`. There is no automatic, scheduled, or background export path.

The full trust boundary analysis, threat enumeration by STRIDE, mitigations, and the residual risks the user accepts live in `docs/security/threat-model.md`. The enforceable rules that go with the threat model are in `docs/security/hardening-checklist.md`. The regulatory positioning (FDA wellness framing, HIPAA scope, GDPR scope, Google Play health app policy) is in `docs/security/compliance-review.md`.

The two settings called out in the threat model that need a PM decision before Phase 1 closes are the `android:allowBackup` posture (currently inconsistent with the on device claim under the scaffold defaults) and the optional in app PIN. Both are flagged in `docs/security/threat-model.md` Section 6.

## 7. Build, CI, and dependency posture

The application is built with Gradle and the Kotlin DSL. Room generates its DAO implementations via KSP rather than KAPT, for faster incremental builds and explicit Kotlin compatibility. The minimum SDK is 31 (Android 12); the target SDK is 34 (Android 14).

Continuous integration runs on GitHub Actions on every pull request. The workflow file is `/.github/workflows/ci.yml` at the repository root. The current CI invocation runs `./gradlew :app:testDebugUnitTest`, which exercises the unit test suite on the JVM. Compose UI tests, Robolectric repository tests, and Paparazzi snapshot tests are added in Phase 1 onward; they continue to run on Linux runners since none of the project's test surfaces require an Android emulator in CI today. The full test pyramid is documented in `SPEC.md` Section 11.

The dependency set, restated for orientation:

- **UI:** Compose BOM 2024.06.00, Material 3, Compose UI tooling, AndroidX Activity Compose, Lifecycle, Navigation Compose.
- **Persistence:** Room 2.6.1 (runtime, ktx, compiler) with KSP code generation.
- **Concurrency:** kotlinx coroutines and Flow.
- **Serialization:** kotlinx serialization JSON for the flat feature map persisted on each `TestResultEntity`.
- **Charts (Phase 9):** Vico.
- **PDF (Phase 10):** Android's built in `android.graphics.pdf.PdfDocument`. No third party PDF library.
- **Test only:** JUnit 4, kotlinx coroutines test, Room testing, Robolectric, AndroidX test core and ext junit, MockK, Paparazzi (added in Phase 1).

No third party analytics, networking, telemetry, or crash reporting library is in this set. The Security Engineer audits every new dependency before it lands on `main`.

## 8. Where to find what

A small index for newcomers and for returning sessions.

- **What is being built.** `SPEC.md` at the repository root, especially Section 1 (vision), Section 2 (problem statement), Section 4 (goals and non goals), and Section 5 (architecture).
- **What phase is in progress.** `STATUS.md` at the repository root. Read the "Next phase" line first.
- **The multi phase plan.** `docs/plan.md`. Each phase has its own scope, agent roster, deliverables, and rough window cost.
- **The detailed Phase 1 plan.** `docs/plans/phase-1-foundation.md`. Subsequent phases get their own plans written by the PM at phase start.
- **Project conventions, dispatch templates, and inherited rules.** `CLAUDE.md` at the repository root. Auto loaded by Claude Code in every session in this directory.
- **The agent roster and one line missions per role.** `SPEC.md` Section 15. Full briefs in `agents/00-project-manager.md` through `agents/21-compliance-reviewer.md`.
- **Design tokens (color, typography, spacing, tap targets, motion).** `docs/design/tokens.md`.
- **Retention design and onboarding conversion targets.** `docs/design/retention.md`.
- **Threat model and trust boundaries.** `docs/security/threat-model.md`.
- **Hardening rules and the no INTERNET veto.** `docs/security/hardening-checklist.md`.
- **Regulatory positioning and the Compliance Reviewer's veto register.** `docs/security/compliance-review.md`.
- **Clinical references and validated clinical norms.** `docs/source/clinical-references.md`.
- **Patient perspective notes.** `docs/qa/patient-advocate-reviews.md`.
- **Continuous integration workflow.** `.github/workflows/ci.yml` at the repository root.
- **Architectural decisions.** `docs/adr/` (the platform choice ADR is `docs/adr/0001-android-native-platform.md`; future ADRs are added when a cross cutting decision is made).
- **Deferred features and explicit non goals tracked separately.** `future-ideas.md` at the repository root.

## 9. What this document is not

This document does not describe how the application is implemented at the source code level. The Phase 1 plan in `docs/plans/phase-1-foundation.md` is the document that describes the package layout, the file structure, and the order in which the foundation tasks land. This document also does not describe the validation numbers achieved against measured ground truth; those are written into the `Validation` section of `README.md` after Phase 5 completes, sourced from `docs/source/validation-report.md`.

When the application ships, this document gets a short "as shipped" closing section in Phase 11 reflecting any architectural change made during implementation.
