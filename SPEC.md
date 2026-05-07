# MS Neuro Battery: Design Specification

**Date:** 2026-05-06
**Status:** Draft, approved by user. Updated 2026-05-06 to switch platform from iOS native to Android native.
**Author:** Mustafan4x (with Claude as design partner)
**Related documents:** ~/src/project_ideas.md

## 1. Vision

A native Android application that lets people living with Multiple Sclerosis (MS) self administer a short, clinically grounded battery of five tests once a week, track their results longitudinally on device, and bring a clear summary report to their next neurology appointment. The application turns the kind of assessment that today happens only in clinic, every three to six months, into a five minute weekly ritual at home.

This document specifies what is being built, why, and how. Implementation planning happens in a separate document produced from this spec.

## 2. Problem Statement

Multiple Sclerosis is a chronic, progressive neurological disease that affects roughly 2.8 million people globally. Disease activity and disability are tracked using the Expanded Disability Status Scale (EDSS), which is administered by a neurologist during clinic visits scheduled every three to six months. Between those visits patients deteriorate silently, with no objective record of how their walking, hand dexterity, vision, cognition, or speech is changing day by day or week by week.

The clinical consequence is that subtle relapses or gradual progression are often detected late. The patient experience consequence is that people feel powerless to track their own disease and often arrive at appointments unable to articulate what has changed since the last visit.

There are research apps in this space, most notably Roche's FLOODLIGHT, but they are study only, locked to specific protocols, and not available to the broader MS community as a self administered clinical tool. There is a real and unfilled gap. Globally, the MS patient population skews toward Android devices, particularly outside the US and Western Europe, which strengthens the case for an Android first build.

## 3. Target Users

### Primary user: a person living with MS

- Age range roughly 20 to 60, though MS occurs across a wide range.
- Owns an Android phone running Android 12 (API 31) or later.
- Comfortable using a smartphone for at least basic tasks.
- May have mild visual impairment, mild cognitive fatigue, mild hand tremor or weakness, and mild gait instability. The application must remain usable across all of these.
- Wants a simple, weekly check in that produces a record they can share with their neurologist.

### Secondary user: the patient's neurologist

- Receives the patient's exported PDF report at appointments.
- Wants a clear, longitudinal view of objective measures, not raw sensor data.
- Treats the application output as supplementary, not diagnostic.

## 4. Goals and Non Goals

### Goals

1. Administer five clinically grounded tests, each runnable in under two minutes, in a single weekly session that takes under ten minutes total.
2. Compute objective, quantitative outputs for each test and store them longitudinally.
3. Produce a clear, readable PDF report on demand summarizing trends across all five tests over time.
4. Validate the most technically rich module, gait analysis, against measured ground truth and publish the accuracy numbers.
5. Operate entirely on device. No cloud, no account, no telemetry.
6. Be accessible by default (TalkBack, large text scaling, sufficient contrast).
7. Be a defensible, demoable resume project that demonstrates full stack Android, signal processing, machine learning, and clinically literate product thinking.

### Non goals

1. Diagnose or treat MS or any other condition.
2. Replace clinical assessment.
3. Earn FDA clearance in this initial scope (the application is positioned as wellness or research, not Software as a Medical Device).
4. Sync data to a backend or share data with anyone other than the user.
5. Support iOS in this initial scope. A future phase may port the application once the validation data is in hand.
6. Include questionnaires or subjective fatigue or mood scales in the v1 battery.
7. Integrate with electronic health records.

## 5. Architecture

The application is a native Android application built with Kotlin and Jetpack Compose, organized into clearly separated layers.

### 5.1 Layer overview

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

### 5.2 Layer responsibilities and invariants

- **Compose UI.** Composable screens and view state. Owns navigation (Navigation Compose), onboarding, the weekly session entry point, history visualizations, settings, and PDF export UI. Has no direct access to sensors.
- **Battery Orchestrator.** A `ViewModel` that owns the lifecycle of a `Session`. Runs each `TestModule` in sequence, persists the result of each test as it completes, and handles pause and resume gracefully if the user is interrupted.
- **Test Modules.** Each conforms to the `TestModule` interface described in Section 5.3. Each module encapsulates its own composable, its own sensor needs, and its own result type. The orchestrator does not know how a test works internally.
- **Signals.** The only code in the application that touches `SensorManager`, `AudioRecord`, or `CameraX`. Exposes sensor streams as Kotlin `Flow` values that can be replaced with mock flows in tests.
- **Signal Processing.** Pure Kotlin. No sensor or UI imports. Takes arrays or flows of samples in, returns features out. Heavily unit tested.
- **Data Store.** Room. Append only at the `Session` level: completed sessions are immutable.
- **Reporting.** Reads from the Data Store, renders trend charts with a Compose charts library (Vico), generates PDF reports via Android's `PdfDocument`.

### 5.3 The TestModule interface

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

Adding a sixth test in the future is a matter of conforming a new type to `TestModule` and registering it with the orchestrator. No changes are required elsewhere.

## 6. The Five Test Modules

Each module is grounded in a clinically validated MS assessment. Selection criteria: published norms must exist, each test must run in under two minutes, and each test must produce sensor backed objective outputs.

### 6.1 Bilateral Tap Test

- **Clinical basis.** Proxy for the 9 Hole Peg Test (9HPT), the EDSS upper limb gold standard.
- **User experience.** 30 seconds of alternating taps on two on screen targets, performed once with the dominant hand and once with the non dominant hand.
- **Sensors.** Touchscreen only.
- **Outputs.** Tap rate (taps per second), inter tap interval coefficient of variation, dominant versus non dominant asymmetry, miss rate.
- **Quality score factors.** Sufficient number of taps recorded, sustained engagement across the full window, taps actually landing within target bounds.

### 6.2 Gait Test (deep module)

See Section 7 for the detailed pipeline. Summary: 30 second walk in a straight line with the phone in the front pocket, 100Hz IMU recording via `SensorManager`, full signal processing pipeline producing cadence, stride length, step time variability, stride asymmetry, and double support time.

### 6.3 Low Contrast Vision Test

- **Clinical basis.** Sloan low contrast acuity, validated for tracking optic neuritis recovery and progression in MS.
- **User experience.** Reads letters from a Sloan style chart at four contrast levels: 100, 25, 5, and 1.25 percent. The user taps each letter from a multiple choice grid to avoid voice recognition errors.
- **Sensors.** Touchscreen for input. Front facing camera (CameraX preview frames) for an ambient brightness check at the start of the test (the test is invalidated if ambient lighting is too dim or too bright).
- **Outputs.** Letters correct at each contrast level, computed Sloan score.
- **Quality score factors.** Ambient brightness within acceptable bounds, viewing distance (estimated from front camera face size via ML Kit face detection) within acceptable bounds, response times consistent with reading rather than guessing.

### 6.4 SDMT (Symbol Digit Modalities Test)

- **Clinical basis.** The most validated cognitive measure in MS. Strong test retest reliability and sensitivity to MS related cognitive change.
- **User experience.** A key showing nine symbol to digit mappings is displayed at the top of the screen. The user is shown a sequence of symbols and taps the matching digit on a numeric keypad. Standard duration is 90 seconds.
- **Sensors.** Touchscreen only.
- **Outputs.** Number of correct responses in 90 seconds, response time mean and standard deviation, error rate.
- **Quality score factors.** No long pauses suggesting interruption, sustained engagement.

### 6.5 Voice Reading Test

- **Clinical basis.** Acoustic biomarkers correlate with bulbar function and global fatigue in MS.
- **User experience.** Reads a fixed 30 second passage aloud. The application displays the passage on screen and records audio via `AudioRecord` while the user reads.
- **Sensors.** Microphone, 44.1kHz mono PCM.
- **Outputs.** Jitter (cycle to cycle frequency variation), shimmer (cycle to cycle amplitude variation), harmonics to noise ratio, speaking rate (words per minute), pause fraction (silence as proportion of total recording).
- **Quality score factors.** Adequate signal to noise ratio, no clipping, voice activity detected throughout.

### 6.6 Tests considered and rejected

- **Pinch test on the touchscreen.** Overlaps with the Tap Test; redundant.
- **Spiral drawing for tremor.** Tremor is more characteristic of Parkinson's than MS. Weak MS specific evidence.
- **PHQ-9 or fatigue questionnaire.** Subjective, not sensor backed. Out of scope for v1.
- **Standing balance via IMU.** Has clinical merit but introduces a fall risk for the kind of patient the application targets and complicates the user experience without strong incremental signal.

## 7. Gait Module Deep Dive

This module is the technical centerpiece. It is the module that earns the headline resume bullet, and it is implemented with conventional, well documented signal processing rather than as an opaque machine learning model so that every step of the pipeline is inspectable and testable.

### 7.1 Pipeline

1. **Capture.** Android's `SensorManager` configured to deliver `Sensor.TYPE_LINEAR_ACCELERATION`, `Sensor.TYPE_GYROSCOPE`, and `Sensor.TYPE_ROTATION_VECTOR` at `SENSOR_DELAY_GAME` (~50Hz) or, where supported, a custom 100Hz interval via `registerListener` with a sampling period in microseconds. Records 3 axis user acceleration, 3 axis rotation rate, and the device attitude quaternion into a ring buffer for 30 seconds.
2. **Orientation estimation.** Android's `TYPE_ROTATION_VECTOR` provides a fused orientation quaternion out of the box; we use it directly. As a teaching exercise (and to make this module more impressive) we also implement a Madgwick filter on raw gyro plus accelerometer and compare its output to the platform fused estimate. Both estimates describe phone orientation in the world frame and let the pipeline recover vertical, forward, and lateral acceleration components regardless of how the phone is sitting in the pocket.
3. **Gravity removal.** Linear acceleration from `TYPE_LINEAR_ACCELERATION` is already gravity compensated. Where the custom pipeline starts from `TYPE_ACCELEROMETER`, the rotated gravity vector is subtracted to produce linear acceleration expressed in the world frame.
4. **Low pass filter.** A 4th order zero phase Butterworth filter at 20Hz suppresses high frequency noise without smearing step events.
5. **Step detection.** A peak finder operating on vertical linear acceleration. Constraints: minimum peak prominence to reject small wobbles, minimum inter peak distance of 250ms to prevent double counting, maximum inter peak distance of 800ms to reject pauses or stops.
6. **Left versus right pairing.** The sign of lateral acceleration at each step assigns left or right. This works because a phone in the pocket records the lateral sway of the body during walking.
7. **Stride segmentation.** Successive steps of the same foot are paired into strides.
8. **Feature extraction.**
   - **Cadence.** Steps per minute over the trial window.
   - **Stride length.** Zero velocity update method. Mid stance instants are detected from local minima of acceleration magnitude. Forward acceleration is integrated between mid stance instants, with velocity reset to zero at each mid stance to bound integration drift.
   - **Step time variability.** Coefficient of variation of step times.
   - **Stride asymmetry.** Normalized ratio of mean step time on the left versus the right.
   - **Double support time.** Estimated from the phase relationship between successive steps.
9. **Quality score.** A `0..1` confidence value computed from heuristics: at least 20 steps detected, orientation estimate stable (low residual error in the Madgwick filter), trajectory approximately straight (low cumulative yaw drift).

### 7.1.1 Why per stride features rather than a global distance metric

Floodlight Open's Two Minute Walk Test, which reports only total distance covered, did not significantly distinguish MS from non MS participants in the analysis of 1,350 MS plus 1,133 non MS users after age and sex adjustment (Oh et al. 2024, *Scientific Reports*, DOI 10.1038/s41598-023-49299-4). Per stride features (step length, cadence, asymmetry, step time variability, double support time) are highly discriminating in the MS literature; the reference values cited in `docs/source/clinical-references.md` (MS vs control step length 45.3 vs 72.1 cm; cadence 94.4 vs 115.2 steps per minute) are from Givon et al. 2009 (*Gait Posture* 29(1):138 to 142). Comber et al. 2017 (*Gait Posture*) provides a complementary meta analysis reporting standardized mean differences. The 30 second window is calibrated to capture enough strides for robust feature estimation while staying short enough to be tolerable for fatigued MS patients, who reported the 2 minute walk as the single most quit prone test in the Floodlight UX analysis (Galati et al. 2024).

### 7.2 Validation

Three layers, each with concrete, defensible numbers:

1. **Synthetic ground truth.** Generated IMU traces with known stride parameters drive the pipeline. Target: stride length recovered within 2 percent on clean synthetic signals.
2. **Real walking course ground truth.** A 25 meter course marked at 25cm intervals with paper or tape, video recorded, with the participant walking at three self selected paces (slow, normal, brisk). Step count from video and total distance walked provide the ground truth. Target: stride length within 5 percent and cadence within 3 percent of measured ground truth, across at least 20 trials by 3 different people of varying heights.
3. **Test retest reliability.** 5 repeated runs by the same participant within 48 hours under similar conditions. Target: intraclass correlation coefficient above 0.75 for cadence, stride length, and step time variability.

These results are written into the project README and into the in app About screen.

## 8. Data Model

Room entities. Append only at the `Session` level: a completed session is immutable; corrections produce a new session. UUIDs are stored as strings via a Room `TypeConverter`.

```kotlin
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dateOfBirthEpochMs: Long,
    val biologicalSex: Sex,
    val dominantHand: Hand,
    val msTypeDisclosed: MSType = MSType.UNDISCLOSED,
    val heightCm: Double,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "session")
data class SessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val startedAtEpochMs: Long = System.currentTimeMillis(),
    val completedAtEpochMs: Long? = null,
    val deviceInfo: String  // model + Android version, frozen at session start
)

@Entity(
    tableName = "test_result",
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["session_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("session_id")]
)
data class TestResultEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "session_id") val sessionId: String,
    val testType: TestType,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long,
    val qualityScore: Double,
    val featuresJson: String,
    val rawSensorRelativePath: String? = null  // path within app private files dir
)

enum class TestType { TAP, GAIT, VISION, SDMT, VOICE }
enum class Sex { FEMALE, MALE, OTHER, UNDISCLOSED }
enum class Hand { LEFT, RIGHT, AMBIDEXTROUS }
enum class MSType { RRMS, PPMS, SPMS, CIS, UNDISCLOSED }
```

DAOs expose suspending functions and `Flow` queries for reactive UI:

```kotlin
@Dao interface SessionDao {
    @Insert suspend fun insert(session: SessionEntity)
    @Update suspend fun update(session: SessionEntity)
    @Delete suspend fun delete(session: SessionEntity)
    @Query("SELECT * FROM session ORDER BY startedAtEpochMs DESC")
    fun observeAll(): Flow<List<SessionEntity>>
    @Query("SELECT * FROM session WHERE id = :id")
    suspend fun getById(id: String): SessionEntity?
}

@Dao interface TestResultDao {
    @Insert suspend fun insert(result: TestResultEntity)
    @Query("SELECT * FROM test_result WHERE session_id = :sessionId ORDER BY startedAtEpochMs ASC")
    suspend fun getForSession(sessionId: String): List<TestResultEntity>
}
```

### 8.1 Storage size estimates

A single weekly session produces roughly:

- Tap, vision, SDMT: under 5KB combined (just events and timestamps).
- Voice features: under 1KB. Raw audio is not retained by default.
- Gait raw IMU: 60KB compressed for a 30 second trace at 100Hz, stored in the app's private files directory and referenced by `rawSensorRelativePath`.

Annual storage per user: under 4MB. Negligible for an on device store.

### 8.2 Encryption

The Room database lives in the application's internal storage (`/data/data/<package>/databases/`), which is inaccessible to other apps and protected by Android's File Based Encryption (FBE). On Android 10+ this means the database is encrypted at rest with keys tied to user credentials and only accessible after the user has unlocked the device after boot.

For users who want stronger protection, an optional setting will switch the Room database to `SQLCipher` via the `androidx.sqlite` integration with a key stored in the Android Keystore. This is a v1.1 enhancement, not a v1 requirement.

### 8.3 Export

A user initiated action produces:

1. A PDF report with a cover page (date range, session count), a per test trend chart, and a summary table of latest values with reference ranges.
2. A structured CSV containing all features and timestamps for power users and researchers.

Both are produced in memory, written to the app's cache directory, and offered through Android's `ACTION_SEND` Share Intent via a `FileProvider`. Nothing is written to shared storage or external storage automatically.

## 9. Validation Strategy

Captured here as a single section because validation is what gives the application credibility, both clinically and on a resume.

1. **Unit validation on synthetic signals.** Every signal processing function has unit tests that exercise it on synthetic inputs with known correct outputs. Target: 90 percent line coverage on the signal processing module.
2. **Real ground truth on gait.** As described in Section 7.2.
3. **Test retest reliability across the full battery.** 5 repeated weekly sessions on healthy controls within a short time window, ICC above 0.75 on the primary feature of each test.
4. **Beta cohort feedback.** Internal test track in Google Play Console with a cohort of 10 to 20 people, with a target of at least 2 to 3 self identified MS patients recruited through an MS support community with informed consent. Two week duration. Survey based feedback on usability, perceived value, and clarity of the report.
5. **Retention measurement during the beta cohort.** Day 1, day 7, day 14, day 30 retention curves measured on the beta cohort, with reminders enabled by default. The empirical floor from the published Floodlight Open analysis (Galati et al. 2024, *JMIR Human Factors* 11:e57033, US MS cohort) is day 30 retention of 30.8 percent with reminders versus 9.7 percent without. MS Battery's target is to meet or exceed 30.8 percent day 30 retention with reminders enabled. The patient owned PDF for the neurologist (a clinical artifact Floodlight Open did not provide) is the project's intended retention lever, leveraging the Oh et al. 2024 finding that clinical supervision was the single largest persistence driver in the Floodlight cohort.

The numbers from layers 1, 2, and 3 are documented in a `Validation` section of the project README. The retention curves from layer 5 are reported in a `Retention` subsection of the same README.

## 10. Privacy and Safety

- **All computation on device.** No backend, no cloud, no analytics SDK.
- **No account.** No login, no email, no server side identifier.
- **Health Connect access is read only and optional.** The application requests sleep and step count for context. Refusing does not break anything.
- **Microphone audio.** Processed in memory; only acoustic features are persisted. Raw audio is discarded after feature extraction unless the user opts in to retention for personal review.
- **Onboarding disclaimer.** First launch presents a clear screen: "This is not a medical device. It does not diagnose or treat any condition. Do not change your treatment based on these results. Share with your neurologist for clinical decisions." The user must acknowledge to proceed.
- **Network permissions.** The application does not declare the `INTERNET` permission. This is a hard guarantee enforceable by Google Play review: a user can verify it by inspecting the app manifest.
- **Export is the only outward facing action,** and it is fully user initiated through the Android Share Intent.

This shape avoids HIPAA scope (the application is not a covered entity or business associate, and never receives data on a server) and avoids FDA Software as a Medical Device classification (the application explicitly does not diagnose or treat). If the project ever pursues clinical claims, that becomes a separate regulatory project with its own design.

## 11. Testing Strategy

- **Unit tests** on the signal processing layer (DSP filters, gait pipeline, audio feature extraction, cognitive scoring, quality score heuristics) using synthetic inputs with known outputs. Run on the JVM with JUnit 4 (Android's default test framework). Target: 90 percent line coverage on the signal processing module.
- **Repository tests** using Room's in memory database (`Room.inMemoryDatabaseBuilder`) to verify DAO behavior and cascade deletes.
- **ViewModel tests** for `BatteryOrchestrator` using fake DAOs and a `TestDispatcher` from `kotlinx-coroutines-test`.
- **Compose UI tests** for screens, using `androidx.compose.ui.test`. Smoke level coverage: each screen renders, primary buttons fire their handlers.
- **Snapshot tests** for key screens using Paparazzi (renders Compose UI on the JVM, screenshot diffs).
- **Accessibility audit** with TalkBack, Font Scale 200%, and Color Contrast Analyzer. This is functional, not optional, given the patient population.
- **Real device validation** for the gait pipeline against the measured course described in Section 7.2.
- **Beta cohort** through Google Play internal testing as described in Section 9.

## 12. Tech Stack

- Language: Kotlin 1.9 or later
- UI: Jetpack Compose with Material 3
- Navigation: Navigation Compose
- Concurrency: Kotlin Coroutines and Flow
- Persistence: Room with KSP code generation
- Sensors: `SensorManager`, `AudioRecord`, CameraX, optional Health Connect
- Charts: Vico (Compose chart library)
- PDF: Android `android.graphics.pdf.PdfDocument`
- Dependency injection: Hilt (light usage; only what genuinely benefits)
- Build: Gradle with Kotlin DSL, `gradle-wrapper`
- Testing: JUnit 4 for unit tests, `kotlinx-coroutines-test` for coroutine and `Flow` testing, Robolectric where Android framework dependence is unavoidable on the JVM (e.g., DAO tests with in memory Room), Paparazzi for snapshot tests, `androidx.compose.ui.test` for instrumented UI tests
- IDE: Android Studio Iguana or later (runs on Linux)
- Min SDK: 31 (Android 12), Target SDK: 34 (Android 14)

No third party analytics, networking, or telemetry libraries. The application does not declare the `INTERNET` permission.

## 13. Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| Gait pipeline accuracy is worse than published norms | Iterate on filter parameters, compare against published implementations, fall back to validated open source reference (e.g., `Madgwick` implementations on GitHub) for cross checking. Be honest in the README if numbers fall short. |
| Sensor sampling rate variance across Android devices | Test on at least three different devices (a Pixel, a Samsung, and a budget phone). Document required minimum capabilities. Reject sessions where actual sample rate falls below threshold. |
| Recruiting real MS patients for the beta is hard | Start with healthy controls. Recruit MS patients through a support community (with consent), or partner with a local MS clinic informally. Spec is honest that this is a stretch goal. |
| The application looks like a generic CRUD app on the surface | The depth lives in the gait module and in the validation README. The interview pitch leads with those, not with the Compose scaffolding. |
| Scope creep into more tests, login, sync, web app, etc. | This spec lists explicit non goals. New scope requires a new spec. |
| Google Play review friction for a health app | Application is positioned as wellness or research. No clinical claims. Disclaimers present. Standard wellness apps clear Play Review without difficulty. |

## 14. Open Questions

These are deferred to implementation planning, not blocking spec approval:

1. Madgwick filter implementation: pull in an existing well tested Kotlin or Java library, or implement from scratch for the resume story?
2. Voice acoustic feature extraction: roll a small DSP module in Kotlin, or pull TarsosDSP (a mature Java audio analysis library)?
3. Compose chart library choice: Vico (recommended), versus alternatives such as MPAndroidChart wrapped in Compose.
4. Beta recruitment: which MS support community to approach, and what informed consent text is appropriate for a personal beta?

## 15. Agent Roster

The project is delivered through a roster of specialist agents dispatched by the Project Manager. Full briefs live in `agents/` at the project root. One line missions:

| # | Agent | Mission |
|---|-------|---------|
| 00 | Project Manager (`agents/00-project-manager.md`) | Plans, sequences phases, dispatches every other agent, runs the mandatory check-in. Default session role. |
| 01 | Clinical Validator (`agents/01-clinical-validator.md`) | Verifies test designs match published clinical literature. Veto on misrepresented test designs. |
| 02 | Signal Processing Engineer (`agents/02-signal-processing-engineer.md`) | Implements gait DSP, audio acoustic features, validation math. |
| 03 | Android Engineer (`agents/03-android-engineer.md`) | Compose UI, ViewModels, navigation, integration of DSP and Signals layers into the application. |
| 04 | UI/UX Designer (`agents/04-ui-ux-designer.md`) | Material 3 tokens and accessibility focused interaction patterns. |
| 05 | Data Engineer (`agents/05-data-engineer.md`) | Room schema, DAOs, JSON contract for feature persistence. |
| 06 | Database Administrator (`agents/06-database-administrator.md`) | Migrations, indices, query performance. |
| 07 | Security Engineer (`agents/07-security-engineer.md`) | Threat model, hardening, dependency audits, no INTERNET veto. |
| 08 | DevOps Engineer (`agents/08-devops-engineer.md`) | Gradle build, signing, GitHub Actions CI, Play Store distribution. |
| 09 | QA Engineer (`agents/09-qa-engineer.md`) | Test plan, regression suite, real world validation experiments, beta cohort. |
| 10 | Code Reviewer (`agents/10-code-reviewer.md`) | Reviews every PR for idiom, structure, inherited rules. |
| 11 | Performance Engineer (`agents/11-performance-engineer.md`) | Sensor sample rate stability, frame rate, battery drain. |
| 12 | Accessibility Specialist (`agents/12-accessibility-specialist.md`) | WCAG 2.1 AA, TalkBack, Dynamic Type, contrast. Veto on accessibility regressions. |
| 13 | Observability Engineer (`agents/13-observability-engineer.md`) | On device structured logging, redaction, diagnostic dump. |
| 14 | Documentation Engineer (`agents/14-documentation-engineer.md`) | README, architecture, ADRs, validation report prose. |
| 15 | Clinical Outcomes Reviewer (`agents/15-clinical-outcomes-reviewer.md`) | "Would a neurologist interpret this number, label, or chart correctly." Veto on misleading clinical content. |
| 16 | Citation Auditor (`agents/16-citation-auditor.md`) | Verifies every cited reference in project artifacts against the source paper. Veto on misattributed or hallucinated citations. |
| 17 | Biostatistics Reviewer (`agents/17-biostatistics-reviewer.md`) | Reviews statistical methods (ICC choice, error metrics, confidence intervals) for correctness. Veto on incorrect methods. |
| 18 | Sensor Integration Engineer (`agents/18-sensor-integration-engineer.md`) | Owns the `signals/` layer (`SensorManager`, `AudioRecord`, `CameraX`) including version specific quirks. |
| 19 | Patient Advocate (`agents/19-patient-advocate.md`) | Stand in for the actual MS patient; reviews every flow for usability under MS related limitations. |
| 20 | Test Fixture Engineer (`agents/20-test-fixture-engineer.md`) | Designs and maintains synthetic test fixtures (IMU, audio) with documented ground truth. |
| 21 | Compliance Reviewer (`agents/21-compliance-reviewer.md`) | Reviews wording and behavior against FDA SaMD guidance, HIPAA, GDPR (where applicable), and Google Play health app policy. Veto on regulatory drift. |

**Coordination rules:**

- Specialists do not recursively dispatch. If a specialist needs work outside its role, it reports the need back to the PM, who dispatches the next agent.
- Veto powers held by Clinical Validator, Accessibility Specialist, Clinical Outcomes Reviewer, Citation Auditor, Biostatistics Reviewer, Security Engineer, and Compliance Reviewer can only be overridden by the user.
- Every phase boundary requires the **mandatory check-in protocol** (see `CLAUDE.md`): the PM asks the user three questions (current Max plan usage, time to reset, continue or pause or stop) before starting the next phase.
- No phase closes with red tests, missing security review where applicable, missing clinical sign off where applicable, or missing documentation deliverables for that phase.

## 16. Success Criteria

The project is considered successful when:

1. All five tests run end to end on a real device, producing structured results stored on device.
2. The gait module hits the published validation numbers (within 5 percent on stride length, within 3 percent on cadence) on at least 20 walking trials across at least 3 people of varying heights.
3. The full battery achieves test retest ICC above 0.75 on the primary feature of each test.
4. A PDF report can be generated from a real session history and is readable to a non technical reader.
5. A README documents the validation methodology and reports the achieved numbers.
6. The application clears Google Play review and ships to internal testing for at least one external beta cohort.
7. The resume bullet is defensible: every claim in it can be traced to a specific number in the README.
8. Day 30 retention in the beta cohort with reminders enabled meets or exceeds 30.8 percent (the Floodlight Open empirical floor per Galati et al. 2024). Retention curves are reported in the README's `Retention` subsection.
