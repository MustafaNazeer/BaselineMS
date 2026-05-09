# BaselineMS Phase 4: Gait Test Module Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Phase 4 wires the Phase 3 DSP module to real Android sensors and exposes a `GaitTest` Compose flow; TDD discipline applies to every signals layer module and to every screen unit test, but the real-device sample rate sign-off is a user-driven task that defers to Phase 4 close.

**Goal:** Wire the Phase 3 gait DSP module into a `TestModule` with sensor capture, Compose UI, and persistence. End state is a runnable app where the user can launch a gait test from the Home screen, see safety instructions, walk for 30 seconds with the phone in a front pocket, see post test feedback, and find the captured raw sensor trace plus computed gait features in the Room database.

**Architecture:** Native Android, Kotlin and Jetpack Compose. The `signals/` layer is the boundary between Android sensor APIs and the rest of the application; it owns `ImuSource`, the wrapping of `SensorManager` callbacks into a typed `Flow<ImuSample>`. The `signals/` layer imports the value types (`ImuSample`, `Vector3`, `Quaternion`) and the from-scratch Madgwick filter from the `dsp/` package; the dependency direction is `signals/` -> `dsp/`. The `GaitTest` `TestModule` is a self contained Compose flow: instructions screen, countdown, capture, post test feedback. Raw sensor traces are persisted as gzipped CSV in the application's private files dir; the relative path is recorded in `TestResultEntity.rawSensorRelativePath`. The existing `BatteryOrchestrator` and `TestModule` interfaces extend slightly to carry the optional raw path through the persistence boundary; this is a backward compatible change that does not affect the `BilateralTapTest` already shipping in production.

**Tech Stack:** Kotlin 1.9 (already on the project), Jetpack Compose Material 3, `androidx.lifecycle.viewmodel.compose`, Room with KSP (already wired), kotlinx-coroutines (already wired). New runtime dependencies: nothing. New test dependencies: nothing beyond Robolectric (already wired). Android sensor APIs used: `Sensor.TYPE_LINEAR_ACCELERATION`, `Sensor.TYPE_GYROSCOPE`, `Sensor.TYPE_ROTATION_VECTOR`, registered via `SensorManager.registerListener` with a custom 100 Hz sampling period in microseconds.

**Related spec:** `~/src/BaselineMS/SPEC.md` Section 6.2 (gait test summary), Section 7 (gait module deep dive), Section 5 (architecture and signals layer responsibilities), Section 10 (privacy: no `INTERNET` permission, raw sensor trace stored only locally).

**Related agent briefs:**
- `~/src/BaselineMS/agents/18-sensor-integration-engineer.md` (lead on `signals/` layer).
- `~/src/BaselineMS/agents/03-android-engineer.md` (lead on `GaitTest` Compose flow and orchestrator wiring).
- `~/src/BaselineMS/agents/02-signal-processing-engineer.md` (Phase 4 reviewer; verifies sensor stream contract is unmolested).
- `~/src/BaselineMS/agents/19-patient-advocate.md` (reviews gait test from the MS patient perspective; safety language, instructions clarity, contextual skip).
- `~/src/BaselineMS/agents/11-performance-engineer.md` (Phase 4 reviewer; allocation discipline plus the deferred multi-device sign-off coordination).
- `~/src/BaselineMS/agents/10-code-reviewer.md` (PR review at phase close).
- `~/src/BaselineMS/agents/09-qa-engineer.md` (regression checklist and phase sign off).

**Platform note:** Local builds run with `JAVA_HOME=/snap/android-studio/209/jbr ./gradlew :app:testDebugUnitTest`. The CI workflow uses Temurin JDK 17. No Phase 4 task requires a real device for compile or unit-test verification; the only deliverable that requires a real device is the multi-device sample rate sign-off, which is deferred to a user-driven validation task at Phase 4 close.

---

## File Map

Files this plan creates or modifies:

```
~/src/BaselineMS/
├── SPEC.md                                              (no change)
├── STATUS.md                                            (PM updates: Phase 4 in progress -> completed)
├── docs/
│   ├── plans/
│   │   └── phase-4-gait-test-module-integration.md      (this file)
│   ├── adr/
│   │   └── 0003-sensor-type-choice.md                   (NEW: SIE writes the ADR)
│   ├── observability/
│   │   └── sensor-runbook.md                            (NEW: SIE per-device notes)
│   ├── qa/
│   │   ├── code-review-phase-4.md                       (NEW: Code Reviewer's verdict)
│   │   ├── patient-advocate-reviews.md                  (APPEND: Phase 4 review entry)
│   │   └── regression-checklist.md                      (APPEND: Phase 4 entries)
│   └── perf/
│       └── latency-budgets.md                           (APPEND: Phase 4 review)
└── app/src/
    ├── main/
    │   ├── AndroidManifest.xml                          (no permission additions; verified by Code Reviewer)
    │   └── java/com/mustafan4x/baselinems/
    │       ├── battery/
    │       │   ├── TestModule.kt                        (MODIFY: add optional rawSensorRelativePath to TestResultPayload)
    │       │   ├── BatteryOrchestrator.kt               (MODIFY: persist rawSensorRelativePath when payload provides it)
    │       │   └── gait/                                (NEW package)
    │       │       ├── GaitTest.kt                      (TestModule implementation)
    │       │       ├── GaitTestState.kt                 (UI state machine: Instructions, Countdown, Capturing, Done)
    │       │       ├── GaitTestViewModel.kt             (orchestrates capture + DSP call)
    │       │       ├── GaitInstructionsScreen.kt
    │       │       ├── GaitCountdownScreen.kt
    │       │       ├── GaitCaptureScreen.kt
    │       │       └── GaitDoneScreen.kt
    │       ├── signals/                                 (NEW package)
    │       │   ├── ImuSource.kt                         (interface; allows mocking in tests)
    │       │   ├── AndroidImuSource.kt                  (SensorManager-backed implementation)
    │       │   └── RawSensorWriter.kt                   (gzipped CSV writer)
    │       └── BaselineMSApp.kt                          (MODIFY: register GaitTest in TestModule list)
    └── test/java/com/mustafan4x/baselinems/
        ├── battery/
        │   └── TestModuleTest.kt                        (NEW: TestResultPayload backward compat)
        ├── battery/gait/
        │   ├── GaitTestViewModelTest.kt                 (NEW: viewmodel state transitions)
        │   └── GaitTestRenderTest.kt                    (NEW: Compose smoke test for each screen)
        └── signals/
            ├── AndroidImuSourceTest.kt                  (NEW: Robolectric, mocked SensorManager)
            └── RawSensorWriterTest.kt                   (NEW: writes a known sample stream, parses it back)
```

Phase 3's `dsp/` package is consumed unchanged by Phase 4; the `signals/` package imports from it without modifying it. The `BilateralTapTest` from Phase 2 continues to ship unchanged; its behavior is verified by the existing Phase 2 regression checklist at every gradle test run.

---

## Coordination notes for the dispatching PM

- **Sensor Integration Engineer (SIE) owns Tasks 2 to 5.** They land the `signals/` package, the ADR, the runbook, and the rawSensorWriter. SIE does not write Compose UI.
- **Android Engineer (AE) owns Tasks 1, 6, 7, 8.** They land the `TestResultPayload` extension, the `GaitTest` Compose flow, the orchestrator wiring, and the registration in `BaselineMSApp`. AE does not write `signals/` code.
- **Reviewers run after AE Task 8 lands:** SPE Task 9 (sensor stream contract), Patient Advocate Task 10 (gait test patient perspective), Performance Engineer Task 11 (allocation discipline within Phase 4 scope).
- **Code Reviewer Task 12 runs after the three reviewers; QA Engineer Task 13 runs last.** Phase close follows Task 13.
- **Real-device sample rate sign-off** is the SIE's Phase 4 deliverable per `agents/18-sensor-integration-engineer.md` Phase 4 Task 3, but headless agents cannot drive an AVD or measure a real device. The plan delivers the code that is needed for the measurement (a debug-only sample rate counter exposed in the runbook) and defers the multi-device measurement to a user-driven validation task at phase close. This mirrors the Phase 1 / Phase 2 deferred emulator walkthrough convention.

---

## Architectural rails locked in by this plan

These follow from `SPEC.md` Section 7 plus existing project conventions; they are the contract this plan enforces.

1. **No new runtime permissions.** No `INTERNET`, no `ACCESS_FINE_LOCATION`, no `ACTIVITY_RECOGNITION`. The IMU sensors used (`TYPE_LINEAR_ACCELERATION`, `TYPE_GYROSCOPE`, `TYPE_ROTATION_VECTOR`) do not require any runtime permission; the Security Engineer ratifies via the Code Reviewer pass at Task 12.
2. **`ImuSample`, `Vector3`, `Quaternion` stay in `dsp/`.** Phase 3 placed them there; the `signals/` package imports them. This keeps the dependency direction `signals/` -> `dsp/` and avoids moving files Phase 3 already commits.
3. **`signals/` is a thin adaptation layer.** No DSP math runs in `signals/`. The exception is the rotationVector null fallback (Phase 3 Resume notes item 1): if `Sensor.TYPE_ROTATION_VECTOR` is unavailable on the device, `signals/AndroidImuSource` runs the Phase 3 `Madgwick` filter inline to produce a per sample quaternion before emitting `ImuSample`. This preserves the `dsp/` layer's pre-condition (every emitted sample has a non-null rotationVector) without leaking sensor concerns into `dsp/`.
4. **Capture target is 100 Hz nominal.** `SensorManager.registerListener` is called with `samplingPeriodUs = 10_000` (= 100 Hz). The actual rate varies per device per Android documentation; the runbook records the observed rate, and Phase 4 quality-score factors do not assume sample-perfect 100 Hz.
5. **Raw sensor trace persistence is gzipped CSV.** The on-disk format is line-oriented: header line listing column names, then one row per sample with `timestampNanos,accelerometerX,accelerometerY,accelerometerZ,gyroscopeX,gyroscopeY,gyroscopeZ,linearAccelerationX,linearAccelerationY,linearAccelerationZ,rotationVectorW,rotationVectorX,rotationVectorY,rotationVectorZ`. Files live under `Context.filesDir/sensor_traces/<sessionId>/<testType>.csv.gz`. The relative path stored in `TestResultEntity.rawSensorRelativePath` is `sensor_traces/<sessionId>/<testType>.csv.gz`.
6. **`TestResultPayload` extension is backward compatible.** Adding a property with a default value (`val rawSensorRelativePath: String? = null`) does not break the existing `BilateralTapTest`'s `TapSessionFeatures` payload because Kotlin interfaces with default-value abstract members are not language-level (interfaces cannot have default property values). To preserve compatibility the property is declared as `val rawSensorRelativePath: String? get() = null` (default getter implementation). Implementing classes override only when a raw trace is captured.
7. **Allocation discipline carries from Phase 3.** The `signals/AndroidImuSource` per-sample callback path is allocation-free in the steady state (one `ImuSample` per emit is unavoidable, but no transient `Vector3` or `Quaternion` allocations on the hot path beyond what `ImuSample` itself encloses).
8. **No phase-close emulator walkthrough is mandated by Phase 4 itself.** The QA Engineer's regression checklist names falsifiable conditions; the user runs the walkthrough at their convenience. Phase 4 close requires only that the unit test suite be green and the reviewers have signed off.

---

## Task 0: PM prep verification

**Files:**
- Verify: `STATUS.md` row 4 reads `in progress`.
- Verify: clean working tree on `main` before SIE dispatch.
- Verify: 83 tests pass at the start of Phase 4 (Phase 3 close baseline).

- [ ] **Step 1: PM verifies STATUS.md row 4 is in progress**

Run: `grep "^| 4 " /home/mustafa/src/BaselineMS/STATUS.md`
Expected: row contains `| in progress |`.

- [ ] **Step 2: PM verifies clean tree**

Run: `cd ~/src/BaselineMS && git status --short`
Expected: empty (all Phase 3 close commits landed).

- [ ] **Step 3: PM verifies test suite is green at Phase 3 baseline**

Run: `cd ~/src/BaselineMS && JAVA_HOME=/snap/android-studio/209/jbr ./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, 83 tests, 0 failures.

If any check fails, the PM addresses the gap before dispatching the AE.

---

## Task 1: Extend `TestResultPayload` with optional `rawSensorRelativePath`

**Owner:** Android Engineer.

**Files:**
- Modify: `app/src/main/java/com/mustafan4x/baselinems/battery/TestModule.kt` (add the new property to the interface).
- Modify: `app/src/main/java/com/mustafan4x/baselinems/battery/BatteryOrchestrator.kt` (extend `recordResult` signature or read the property directly).
- Create: `app/src/test/java/com/mustafan4x/baselinems/battery/TestModuleTest.kt` (verify the default value is null and that BilateralTapTest's payload has the default).

- [ ] **Step 1: Write the failing test (TDD)**

```kotlin
package com.mustafan4x.baselinems.battery

import com.mustafan4x.baselinems.data.TestType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TestModuleTest {
    @Test
    fun `default TestResultPayload has null rawSensorRelativePath`() {
        val payload = object : TestResultPayload {
            override val qualityScore: Double = 1.0
            override val features: Map<String, Double> = emptyMap()
        }
        assertNull(payload.rawSensorRelativePath)
    }

    @Test
    fun `TestResultPayload can override rawSensorRelativePath`() {
        val payload = object : TestResultPayload {
            override val qualityScore: Double = 0.9
            override val features: Map<String, Double> = emptyMap()
            override val rawSensorRelativePath: String = "sensor_traces/abc/gait.csv.gz"
        }
        assertEquals("sensor_traces/abc/gait.csv.gz", payload.rawSensorRelativePath)
    }
}
```

- [ ] **Step 2: Run to verify it fails (compilation)**

Expected: failure on missing `rawSensorRelativePath` property.

- [ ] **Step 3: Modify `TestModule.kt` to add the new property with a default getter**

Change the interface body of `TestResultPayload`:
```kotlin
interface TestResultPayload {
    val qualityScore: Double
    val features: Map<String, Double>
    val rawSensorRelativePath: String? get() = null
}
```

- [ ] **Step 4: Modify `BatteryOrchestrator.recordResult` to read the path from the payload**

The current `recordResult(testType, qualityScore, features)` signature does not carry the raw path. Two equally valid options for the AE to pick:
- (a) Change the signature to `recordResult(testType, payload: TestResultPayload)`. This ripples to call sites in `BilateralTapTest` and (new in this plan) `GaitTest`.
- (b) Add an overload `recordResult(testType, payload: TestResultPayload)` that delegates to the existing three-arg form internally and additionally writes the raw path. Keeps the existing call sites working with no churn.

The AE picks one; option (a) is cleaner and is the recommended default. The Code Reviewer's Task 12 review confirms the choice is justifiable.

Update the persisted `TestResultEntity` insert to set `rawSensorRelativePath = payload.rawSensorRelativePath` (currently the constructor uses the default null).

- [ ] **Step 5: Run the test to verify it passes**

Run: `cd ~/src/BaselineMS && JAVA_HOME=/snap/android-studio/209/jbr ./gradlew :app:testDebugUnitTest --tests "com.mustafan4x.baselinems.battery.TestModuleTest"`
Expected: 2 tests, 0 failures.

- [ ] **Step 6: Run the full test suite to confirm BilateralTapTest still passes**

Run: `cd ~/src/BaselineMS && JAVA_HOME=/snap/android-studio/209/jbr ./gradlew :app:testDebugUnitTest`
Expected: 85 tests (83 prior + 2 new), 0 failures. If `BilateralTapTest` regressed, the AE rolled the call site change incorrectly; fix before commit.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/mustafan4x/baselinems/battery/TestModule.kt \
        app/src/main/java/com/mustafan4x/baselinems/battery/BatteryOrchestrator.kt \
        app/src/test/java/com/mustafan4x/baselinems/battery/TestModuleTest.kt
git commit -m "battery: extend TestResultPayload with optional rawSensorRelativePath"
```

If the AE chose option (a) above, also include the BilateralTapTest call site update in the commit.

---

## Task 2: `signals/` package with `ImuSource` interface and Android implementation

**Owner:** Sensor Integration Engineer.

**Files:**
- Create: `app/src/main/java/com/mustafan4x/baselinems/signals/ImuSource.kt` (interface).
- Create: `app/src/main/java/com/mustafan4x/baselinems/signals/AndroidImuSource.kt` (SensorManager-backed implementation).
- Create: `app/src/test/java/com/mustafan4x/baselinems/signals/AndroidImuSourceTest.kt` (Robolectric tests with mock SensorManager).

**Spec:** The `ImuSource` interface exposes `fun stream(): Flow<ImuSample>` plus `fun start()` and `fun stop()` lifecycle methods. The Android implementation registers a `SensorEventListener` for `Sensor.TYPE_LINEAR_ACCELERATION`, `Sensor.TYPE_GYROSCOPE`, and `Sensor.TYPE_ROTATION_VECTOR` at `samplingPeriodUs = 10_000` (100 Hz target). The implementation maintains the most-recent sample for each sensor type and emits an `ImuSample` whenever the linear-acceleration sensor fires (which is the primary clock for the gait pipeline). Gyro and rotation samples are interpolated to the linear acceleration timestamp by zero-order hold (use the most recent reading); this is the standard pattern for fused-sensor consumption when the Android sensor framework does not synchronize triggers across types.

If `Sensor.TYPE_ROTATION_VECTOR` is not available on the device (the SIE checks `sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)` returns null), `AndroidImuSource` falls back to running the Phase 3 `Madgwick` filter inline using the linear acceleration plus gyroscope inputs, and stamps the resulting quaternion into the emitted `ImuSample.rotationVector`. The fallback is a single `Madgwick` instance held as a mutable field on the source.

- [ ] **Step 1: Write `ImuSource.kt`**

```kotlin
package com.mustafan4x.baselinems.signals

import com.mustafan4x.baselinems.dsp.ImuSample
import kotlinx.coroutines.flow.Flow

interface ImuSource {
    fun start()
    fun stop()
    fun stream(): Flow<ImuSample>
}
```

- [ ] **Step 2: Write the failing Robolectric test (TDD)**

The Robolectric pattern uses a `ShadowSensorManager` to inject synthetic sensor events. The test verifies:
1. `start()` registers listeners with the system sensor manager.
2. Emitted `ImuSample`s carry the most-recent gyro and rotation values when a linear acceleration event fires.
3. `stop()` unregisters listeners.
4. When `TYPE_ROTATION_VECTOR` is absent, the emitted `ImuSample.rotationVector` is non-null and was produced by the from-scratch Madgwick.

Test code template (the SIE expands the bodies):

```kotlin
package com.mustafan4x.baselinems.signals

import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.test.core.app.ApplicationProvider
import com.mustafan4x.baselinems.dsp.ImuSample
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AndroidImuSourceTest {
    @Test
    fun `start registers listeners for linear acceleration, gyroscope, and rotation vector`() {
        // SIE: build AndroidImuSource(context), call start(), assert ShadowSensorManager has 3 registered listeners
        TODO("write per Robolectric ShadowSensorManager pattern")
    }

    @Test
    fun `stream emits an ImuSample on each linear acceleration event with held gyro and rotation values`() = runTest {
        TODO("inject a gyro event, then a rotation event, then a linear acceleration event; assert emitted sample has those values")
    }

    @Test
    fun `stop unregisters all listeners`() {
        TODO("call start then stop; assert ShadowSensorManager has 0 registered listeners")
    }

    @Test
    fun `when rotation vector sensor is absent, fallback Madgwick fills rotationVector`() {
        TODO("construct ShadowSensorManager without TYPE_ROTATION_VECTOR; emit linear+gyro events; assert ImuSample.rotationVector is non-null")
    }
}
```

The TODOs are placeholders the SIE fills in. The plan deliberately does not pre-write the Robolectric-specific event injection code because the SIE has authority on the right shadow API for the project's Robolectric version.

- [ ] **Step 3: Run to verify the four tests fail (compilation or assertion)**

- [ ] **Step 4: Implement `AndroidImuSource.kt`**

The implementation skeleton:

```kotlin
package com.mustafan4x.baselinems.signals

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.mustafan4x.baselinems.dsp.ImuSample
import com.mustafan4x.baselinems.dsp.Madgwick
import com.mustafan4x.baselinems.dsp.Quaternion
import com.mustafan4x.baselinems.dsp.Vector3
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AndroidImuSource(
    context: Context,
    private val samplingPeriodMicros: Int = 10_000
) : ImuSource {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val linearAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val emissions = MutableSharedFlow<ImuSample>(
        replay = 0,
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val fallbackMadgwick = if (rotation == null) Madgwick(beta = 0.1) else null

    private var lastLinear: Vector3? = null
    private var lastAccel: Vector3? = null
    private var lastGyro: Vector3? = null
    private var lastRotation: Quaternion? = null
    private var lastLinearTimestampNanos: Long = 0L

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_LINEAR_ACCELERATION -> {
                    lastLinear = Vector3(event.values[0].toDouble(), event.values[1].toDouble(), event.values[2].toDouble())
                    lastLinearTimestampNanos = event.timestamp
                    emitIfReady()
                }
                Sensor.TYPE_GYROSCOPE -> {
                    lastGyro = Vector3(event.values[0].toDouble(), event.values[1].toDouble(), event.values[2].toDouble())
                }
                Sensor.TYPE_ROTATION_VECTOR -> {
                    lastRotation = quaternionFromAndroidRotationVector(event.values)
                }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* no op */ }
    }

    override fun start() {
        if (linearAccel != null) sensorManager.registerListener(listener, linearAccel, samplingPeriodMicros)
        if (gyro != null) sensorManager.registerListener(listener, gyro, samplingPeriodMicros)
        if (rotation != null) sensorManager.registerListener(listener, rotation, samplingPeriodMicros)
    }

    override fun stop() {
        sensorManager.unregisterListener(listener)
    }

    override fun stream(): Flow<ImuSample> = emissions.asSharedFlow()

    private fun emitIfReady() {
        val linear = lastLinear ?: return
        val gyroNow = lastGyro ?: Vector3.ZERO
        val rotationNow = lastRotation ?: run {
            if (fallbackMadgwick != null && lastAccel != null) {
                val dt = 1.0 / 100.0  // SIE: derive from timestamp delta when possible
                fallbackMadgwick.update(gyroNow, lastAccel ?: Vector3.ZERO, dt)
                fallbackMadgwick.orientation()
            } else {
                Quaternion.IDENTITY
            }
        }
        emissions.tryEmit(
            ImuSample(
                timestampNanos = lastLinearTimestampNanos,
                accelerometer = lastAccel ?: linear,  // fallback approximation; documented in runbook
                gyroscope = gyroNow,
                linearAcceleration = linear,
                rotationVector = rotationNow
            )
        )
    }

    private fun quaternionFromAndroidRotationVector(values: FloatArray): Quaternion {
        // SIE: convert TYPE_ROTATION_VECTOR's [x, y, z, w?] to Quaternion(w, x, y, z), normalized.
        // Some Android versions provide values[3]; otherwise compute w from |v|^2.
        TODO("SIE implements per the Android docs")
    }
}
```

The skeleton has two TODOs the SIE fills in: the `lastLinearTimestampNanos`-derived dt for the fallback Madgwick, and the `quaternionFromAndroidRotationVector` conversion. The conversion is documented in Android's `SensorManager.getQuaternionFromVector` source; the SIE can either call that helper or reimplement it.

- [ ] **Step 5: Run the four tests to verify they pass**

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/mustafan4x/baselinems/signals/ImuSource.kt \
        app/src/main/java/com/mustafan4x/baselinems/signals/AndroidImuSource.kt \
        app/src/test/java/com/mustafan4x/baselinems/signals/AndroidImuSourceTest.kt
git commit -m "signals: AndroidImuSource for SensorManager-backed Flow<ImuSample> at 100 Hz"
```

---

## Task 3: ADR 0003 sensor type choice

**Owner:** Sensor Integration Engineer.

**Files:**
- Create: `docs/adr/0003-sensor-type-choice.md`.

**Spec:** A short ADR (between 80 and 150 lines is enough) that records the Phase 4 sensor type choice and the rationale.

The decision: use `Sensor.TYPE_LINEAR_ACCELERATION` for the primary acceleration signal (already gravity-removed by the platform), `Sensor.TYPE_GYROSCOPE` for angular velocity, and `Sensor.TYPE_ROTATION_VECTOR` for the platform-fused orientation quaternion, with a from-scratch `Madgwick` fallback when `TYPE_ROTATION_VECTOR` is absent on the device.

Alternatives considered:
- Use `Sensor.TYPE_ACCELEROMETER` with manual gravity removal, no `TYPE_LINEAR_ACCELERATION`. Rejected because the platform's gravity removal benefits from device-specific hardware fusion that an app-level subtraction cannot replicate.
- Use only `TYPE_ACCELEROMETER` and `TYPE_GYROSCOPE`, run the from-scratch `Madgwick` filter for every device. Rejected because the platform fused estimate is more accurate when available, and the parallel Madgwick still runs in the DSP pipeline for the quality score residual.

Trade-offs and consequences:
- The application now depends on the device exposing `TYPE_LINEAR_ACCELERATION`, which is widely available since Android 4 but is not guaranteed. Devices missing it are out of scope; the runbook documents the requirement.
- The fallback Madgwick path adds slight battery overhead on devices without `TYPE_ROTATION_VECTOR` (rare since Android 4.0).

Revisit conditions:
- Phase 5 walking course recordings show sample-rate-instability that traces to the platform fused estimate.
- A future Android version deprecates one of the three sensor types.

- [ ] **Step 1: SIE writes the ADR**

- [ ] **Step 2: Commit**

```bash
git add docs/adr/0003-sensor-type-choice.md
git commit -m "docs(adr): 0003 sensor type choice (TYPE_LINEAR_ACCELERATION + GYROSCOPE + ROTATION_VECTOR)"
```

---

## Task 4: Sensor runbook

**Owner:** Sensor Integration Engineer.

**Files:**
- Create: `docs/observability/sensor-runbook.md`.

**Spec:** The runbook is the per-device record the Phase 4 Performance Engineer (Task 11) and the user (Phase 4 close validation task) populate over time. Phase 4 establishes the structure and the falsifiable conditions that future entries verify against; per-device entries arrive as the user runs the app on real devices.

Sections the runbook MUST have:
1. Purpose and scope.
2. Per-device entry template (device model, Android version, observed sample rate at 100 Hz target, sample-rate stability over a 30 second window, dropped-sample count, notes).
3. Sample-rate measurement procedure (how to derive the observed rate from the captured raw CSV; the SIE writes a short shell snippet or a one-paragraph methodology).
4. Known device gotchas section (initially empty; user appends as they encounter them).
5. The Phase 4 acceptance budgets (from `docs/perf/latency-budgets.md`): actual sample rate within 5 percent of nominal 100 Hz; jitter under 5 ms p99; zero dropped windows over 30 seconds.

- [ ] **Step 1: SIE writes the runbook**

- [ ] **Step 2: Commit**

```bash
git add docs/observability/sensor-runbook.md
git commit -m "docs(observability): sensor runbook with per device entry template and acceptance budgets"
```

---

## Task 5: `RawSensorWriter` for gzipped CSV trace persistence

**Owner:** Sensor Integration Engineer.

**Files:**
- Create: `app/src/main/java/com/mustafan4x/baselinems/signals/RawSensorWriter.kt`.
- Create: `app/src/test/java/com/mustafan4x/baselinems/signals/RawSensorWriterTest.kt`.

**Spec:** A small writer that takes a `Flow<ImuSample>` plus a target `File` and writes a gzipped CSV of the captured samples. Format from the architectural rails: header line plus one row per sample, all 14 columns. The writer is constructed with the target file and exposes `suspend fun write(samples: Flow<ImuSample>): Long` returning the number of samples written. `GZIPOutputStream` wraps a `BufferedOutputStream` wraps a `FileOutputStream`. The writer flushes and closes on completion; if the flow throws, the writer closes the file and rethrows.

Tests:
1. Write a known small stream of `ImuSample`s, read the gzipped file back into memory, decompress, parse the CSV, and assert the round trip preserves every field within floating-point precision.
2. Write a stream that throws after N samples; assert the file is closed and the exception propagates.

- [ ] **Steps 1 to 5:** TDD.

```bash
git add app/src/main/java/com/mustafan4x/baselinems/signals/RawSensorWriter.kt \
        app/src/test/java/com/mustafan4x/baselinems/signals/RawSensorWriterTest.kt
git commit -m "signals: RawSensorWriter gzipped CSV persistence with round trip tests"
```

---

## Task 6: `GaitTest` Compose flow (instructions, countdown, capture, done)

**Owner:** Android Engineer.

**Files:**
- Create: `app/src/main/java/com/mustafan4x/baselinems/battery/gait/GaitTestState.kt` (sealed-class state machine: `Instructions`, `Countdown(secondsRemaining: Int)`, `Capturing(progressMillis: Int)`, `Done(features: GaitFeatures)`, `Cancelled`).
- Create: `app/src/main/java/com/mustafan4x/baselinems/battery/gait/GaitInstructionsScreen.kt`.
- Create: `app/src/main/java/com/mustafan4x/baselinems/battery/gait/GaitCountdownScreen.kt`.
- Create: `app/src/main/java/com/mustafan4x/baselinems/battery/gait/GaitCaptureScreen.kt`.
- Create: `app/src/main/java/com/mustafan4x/baselinems/battery/gait/GaitDoneScreen.kt`.
- Create: `app/src/test/java/com/mustafan4x/baselinems/battery/gait/GaitTestRenderTest.kt` (Compose smoke tests; one per screen, asserting the screen renders without crashing and exposing semantic nodes for the primary CTA).

**Spec:** Each screen is a stateless `@Composable` that takes the relevant state and a callback (e.g., `onStart: () -> Unit` for the instructions screen, `onCancel` for the capture screen). The screens follow the Material 3 baseline already established by the BilateralTapTest's screens; tap targets are at least 48 dp; primary CTAs use `FilledButton`.

Screen content per `agents/19-patient-advocate.md` Phase 4 review (anticipated; the Patient Advocate's actual Phase 4 review at Task 10 may add findings the AE folds in):

- **GaitInstructionsScreen** body: "Walk in a straight line for 30 seconds. Place your phone in a front pocket. Use a flat, unobstructed surface; have a wall or counter within reach if you need it. If you do not feel safe walking right now, you can skip this test." Primary CTA: "I am ready". Secondary CTA: "Skip for now". Skip is the standard test-skip pattern from the BilateralTapTest's prior implementation (per Patient Advocate Phase 0 framing standing objection 6).
- **GaitCountdownScreen** body: a large numeric display counting 3, 2, 1 with one-second cadence. Accessibility: announce each number via `Modifier.semantics`.
- **GaitCaptureScreen** body: a centered "Walking" indicator (circular progress around an icon), the elapsed-time / total-30s readout, a Cancel button. Sensor capture begins when this screen enters composition (`LaunchedEffect`) and stops when it leaves.
- **GaitDoneScreen** body: a thank-you message, a brief quality-score-aware status line ("Captured with high confidence" if quality > 0.8; "Captured with limited confidence" if 0.5 to 0.8; "Captured but quality is low" if below 0.5). Primary CTA: "Continue".

The state machine and the screens compose via a `GaitTest` `TestModule` (Task 7). Task 6 only ships the screens and the state class.

- [ ] **Steps 1 to 5:** Write the state class first, then each screen TDD-style with a smoke test asserting the primary CTA semantic node exists. Run the test suite after every screen to verify no regression.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/mustafan4x/baselinems/battery/gait/GaitTestState.kt \
        app/src/main/java/com/mustafan4x/baselinems/battery/gait/GaitInstructionsScreen.kt \
        app/src/main/java/com/mustafan4x/baselinems/battery/gait/GaitCountdownScreen.kt \
        app/src/main/java/com/mustafan4x/baselinems/battery/gait/GaitCaptureScreen.kt \
        app/src/main/java/com/mustafan4x/baselinems/battery/gait/GaitDoneScreen.kt \
        app/src/test/java/com/mustafan4x/baselinems/battery/gait/GaitTestRenderTest.kt
git commit -m "battery(gait): instructions, countdown, capture, done screens with smoke tests"
```

---

## Task 7: `GaitTestViewModel` and `GaitTest` `TestModule`

**Owner:** Android Engineer.

**Files:**
- Create: `app/src/main/java/com/mustafan4x/baselinems/battery/gait/GaitTestViewModel.kt`.
- Create: `app/src/main/java/com/mustafan4x/baselinems/battery/gait/GaitTest.kt` (the `TestModule` implementation).
- Create: `app/src/test/java/com/mustafan4x/baselinems/battery/gait/GaitTestViewModelTest.kt`.

**Spec:** The `GaitTestViewModel` is constructed with an `ImuSource`, a `GaitPipeline`, a `RawSensorWriter`, and the destination `File`. It exposes a `StateFlow<GaitTestState>`, an `onStart()`, an `onCancel()`, and an `onContinue()` callback that hands a `TestResultPayload` (with `rawSensorRelativePath` populated) back via a callback parameter. The view model owns the state machine: Instructions -> Countdown (3 s) -> Capturing (30 s, with `progressMillis` ticking) -> Done. During Capturing, the `ImuSource` is started, the samples are tee'd to both the `RawSensorWriter` and an in-memory list, and at 30 s the in-memory list is fed into `GaitPipeline.process` to compute `GaitFeatures`. The Done state holds the features; on `onContinue` the view model produces a `TestResultPayload` and invokes the callback.

The `GaitTest` class implements `TestModule`. Its `Content(onComplete)` function builds a `GaitTestViewModel` (via `viewModel()` if available, or a hand-instantiated DI per `BaselineMSApp`'s pattern), observes its state, and renders the appropriate screen for that state. On Done -> Continue, it calls `onComplete(payload)`.

Tests cover:
1. `onStart()` transitions from Instructions to Countdown(3).
2. After 3 ticks of one second each, Countdown transitions to Capturing(0).
3. During Capturing, `progressMillis` increments and the `ImuSource.start()` and `RawSensorWriter.write()` are called.
4. After 30 s, the state transitions to Done with non-null features (use a fake `ImuSource` that emits a 30 s synthetic stream and a fake `GaitPipeline` that returns a fixed `GaitFeatures`).
5. `onCancel()` from Capturing transitions to Cancelled and stops the `ImuSource`.
6. `onContinue` from Done invokes the callback with a `TestResultPayload` whose `rawSensorRelativePath` matches the destination file's relative path.

Use `kotlinx-coroutines-test`'s `TestScope` and `runTest` plus a `TestDispatcher` for time control. The view model's per-second tick uses `delay(1000)`; in tests this is virtual time advanced by `advanceTimeBy`.

- [ ] **Steps 1 to 5:** TDD per the Task 6 pattern.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/mustafan4x/baselinems/battery/gait/GaitTestViewModel.kt \
        app/src/main/java/com/mustafan4x/baselinems/battery/gait/GaitTest.kt \
        app/src/test/java/com/mustafan4x/baselinems/battery/gait/GaitTestViewModelTest.kt
git commit -m "battery(gait): viewmodel state machine and TestModule integration"
```

---

## Task 8: Wire `GaitTest` into `BaselineMSApp`'s `TestModule` list

**Owner:** Android Engineer.

**Files:**
- Modify: `app/src/main/java/com/mustafan4x/baselinems/BaselineMSApp.kt` (add `GaitTest` to the modules list, instantiating with the application's `Context.filesDir`-based output directory and the singleton `AndroidImuSource`).

**Spec:** `BaselineMSApp` already constructs the `BatteryOrchestrator` with a `List<TestModule>`. After Phase 4, this list contains `BilateralTapTest` and `GaitTest`. The order is BilateralTapTest first, GaitTest second; the user can iterate the order in Phase 11 polish.

Tests: existing `BatteryFlowIntegrationTest` continues to pass (it uses a mock module list, so adding GaitTest does not affect it). A new lightweight test asserts `BaselineMSApp` constructs the GaitTest module without crashing in a Robolectric environment.

- [ ] **Steps 1 to 4:** TDD.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mustafan4x/baselinems/BaselineMSApp.kt
git commit -m "battery(gait): register GaitTest in BaselineMSApp module list"
```

---

## Task 9: Signal Processing Engineer review

**Owner:** Signal Processing Engineer.

**Files:**
- Append: `docs/qa/code-review-phase-4.md` (or a separate `docs/qa/spe-review-phase-4.md` if the AE prefers; the Code Reviewer's Task 12 review consolidates).

**Spec:** Per `agents/02-signal-processing-engineer.md` Phase 4 task 7: "Review the Android Engineer's sensor integration to confirm samples reach the DSP module unmolested (no resampling, no dropped samples on the path between SensorManager and the pipeline)." The SPE reads `AndroidImuSource.kt` and `GaitTestViewModel.kt`, traces the sample path, and confirms:
1. `ImuSample`s emitted by `AndroidImuSource` are buffered without resampling (the zero-order hold for gyro / rotation between linear-acceleration ticks is acceptable; the gait pipeline does not need synchronized triggers).
2. `GaitTestViewModel` accumulates samples into a list and passes the full list to `GaitPipeline.process` at end of capture; no decimation, no truncation, no reordering.
3. The fallback Madgwick path in `AndroidImuSource` produces a quaternion in the same convention (device-to-world, matching Android `TYPE_ROTATION_VECTOR`) as the platform-fused estimate.

Verdict: APPROVED, APPROVED WITH MINOR FINDINGS, or REJECTED. SPE files findings; the AE addresses minor findings before Code Reviewer Task 12 runs.

- [ ] **Step 1: PM dispatches the SPE after Task 8 commit lands**

- [ ] **Step 2: SPE produces the review and commits**

```bash
git add docs/qa/code-review-phase-4.md  # or whatever the SPE chose
git commit -m "review(gait): Signal Processing Engineer Phase 4 sensor stream review"
```

---

## Task 10: Patient Advocate review

**Owner:** Patient Advocate.

**Files:**
- Append: `docs/qa/patient-advocate-reviews.md` (Phase 4 entry).

**Spec:** Per `agents/19-patient-advocate.md` Phase 4 dispatch: review the gait test from the lived MS-patient perspective. Specific concerns to address (drawn from the agent brief and the Phase 0 framing notes):
1. Walking safety. Per Galati 2024 the gait test was a top quit reason; the instructions copy must be unambiguous about safety (flat surface, wall within reach, "do not feel safe = skip" framing).
2. Patient-pocket reality. Some MS patients use a walking aid or have limited range of motion; the instructions should accommodate phones held in hand or other pocket positions in a Phase 11 follow up. Phase 4 minimum is the front-pocket case.
3. EDSS 6+ patients. The 800 ms inter-peak ceiling rejects the slowest cadences. The post-test feedback should not communicate a low quality score as the patient's failure.
4. Heat sensitivity. A 30 second walk plus 3 second countdown is brief, but the instructions should not encourage the user to walk fast.
5. Emergency-cancel UX. The Capture screen's Cancel button must be reachable and unambiguous; per the Phase 0 framing standing objection 5, sessions can be cancelled without shame.

Verdict: APPROVED, APPROVED WITH FINDINGS (with each finding given a severity and an owner), or REJECTED.

- [ ] **Step 1: PM dispatches the Patient Advocate after SPE Task 9 lands**

- [ ] **Step 2: Patient Advocate produces the review and commits**

```bash
git add docs/qa/patient-advocate-reviews.md
git commit -m "review(gait): Patient Advocate Phase 4 review"
```

If the Patient Advocate flags blocking findings, the AE addresses them in a fix commit before Task 12.

---

## Task 11: Performance Engineer review (Phase 4 scope)

**Owner:** Performance Engineer.

**Files:**
- Append: `docs/perf/latency-budgets.md` Phase 4 review subsection.

**Spec:** Per `agents/11-performance-engineer.md` Phase 4 tasks 3 to 5 the PE measures actual IMU sample rate and jitter on three real Android devices, profiles the DSP pipeline, and updates `docs/perf/latency-budgets.md`. Phase 4 implementation by headless agents covers the code side: a sample-rate counter on `AndroidImuSource` exposes observed rate and jitter via a debug-only API. The PE's Phase 4 review reads this code and confirms the methodology is sound; the actual multi-device measurement is the user's Phase 4 close task (see "Phase close" section).

The PE Phase 4 review answers:
1. Is the sample rate counter measuring the right thing (delta between successive linear-acceleration timestamps)?
2. Is the jitter metric defined as the p99 of inter-sample delta minus the nominal 10 ms? Yes / no.
3. Is the dropped-windows metric counting any 30 second window in which the cumulative delta drift exceeds 5 percent? Yes / no.
4. Are there any obvious allocation or coroutine-scheduling concerns in `AndroidImuSource` or `RawSensorWriter` that would prevent the device measurement from yielding clean numbers?

Verdict: APPROVED, APPROVED WITH MINOR FINDINGS, or REJECTED.

- [ ] **Step 1: PM dispatches the PE after Task 10 lands**

- [ ] **Step 2: PE produces the review and commits**

```bash
git add docs/perf/latency-budgets.md
git commit -m "perf(gait): Phase 4 Performance Engineer review of sensor capture path"
```

---

## Task 12: Code Reviewer Phase 4 verdict

**Owner:** Code Reviewer.

**Files:**
- Create: `docs/qa/code-review-phase-4.md`.

**Spec:** Phase-level review mirroring the Phase 3 review's format. The Code Reviewer reads every file changed by Phase 4 (run `git log --oneline 4cd0a72..HEAD` to enumerate, the first hash being the Phase 3 close commit on main and `4cd0a72` will be replaced by the actual Phase 3 phase-close commit at the time of this review). Verifies:
1. Conformance to this Phase 4 plan.
2. SPEC.md Section 7 fidelity (capture method matches; raw trace persistence matches).
3. No `INTERNET` permission added to AndroidManifest.xml (the Security Engineer veto applies).
4. No allocation regressions in `AndroidImuSource` per the PE Phase 4 review.
5. Inherited rules (no em / en / hyphen punctuation in prose, no emojis, no AI attribution lines).

Verdict: APPROVED, APPROVED WITH MINOR CHANGES, or REJECTED.

- [ ] **Step 1 to 3:** Same pattern as Phase 3 Task 17.

```bash
git add docs/qa/code-review-phase-4.md
git commit -m "review(gait): Code Reviewer Phase 4 verdict"
```

If APPROVED WITH MINOR CHANGES, the AE or SIE lands a cleanup commit before Task 13.

---

## Task 13: QA Engineer regression checklist and Phase 4 sign off

**Owner:** QA Engineer.

**Files:**
- Append: `docs/qa/regression-checklist.md` Phase 4 section.

**Spec:** The QA Engineer runs the full test suite, records the count, appends the Phase 4 falsifiable conditions to the regression checklist, and issues a sign off (PASS / PASS WITH NOTES / FAIL).

Falsifiable conditions Phase 4 must not regress:
1. `AndroidImuSourceTest` four cases (start registers listeners, stream emits with held values, stop unregisters, fallback Madgwick fills rotationVector).
2. `RawSensorWriterTest` two cases (round trip and exception propagation).
3. `GaitTestViewModelTest` six cases (state transitions, sensor lifecycle, payload production).
4. Compose smoke tests on the four GaitTest screens.
5. `BatteryFlowIntegrationTest` continues to pass with the orchestrator's new payload signature.
6. `BilateralTapTest`'s existing tests continue to pass with the `TestResultPayload` extension.
7. AndroidManifest.xml contains no `INTERNET` permission.
8. Test count strictly grew from 83 (Phase 3 close) to whatever the Phase 4 final number is, with zero failures.

The QA Engineer also names the user-driven Phase 4 close task: real-device sample rate validation per the runbook on at least three real devices (or the user's available device set, with the runbook documenting which devices were exercised).

- [ ] **Step 1 to 4:** Same pattern as Phase 3 Task 18.

```bash
git add docs/qa/regression-checklist.md
git commit -m "qa(gait): Phase 4 regression checklist and sign off"
```

---

## Phase close

After Task 13 lands:

1. PM updates `STATUS.md` row 4 from `in progress` to `completed`, fills in the date and the actual window cost.
2. PM moves the "Next phase" line at the top of `STATUS.md` to Phase 5.
3. PM writes a Resume notes block. Open items expected:
   - Phase 5: real-device sample rate sign-off across the user's devices, runbook entries.
   - Phase 5: any Patient Advocate findings deferred from Task 10.
   - Phase 11: any cosmetic / accessibility polish flagged by the reviewers.
4. PM runs the mandatory check-in protocol before Phase 5 begins.

The user's Phase 4 close validation task is documented in the runbook at `docs/observability/sensor-runbook.md`. The user installs the app on each available real Android device, runs a gait session, opens the captured CSV (or the Settings screen's debug "Show last sample rate" if the AE wired one up), and appends a runbook entry. The PM treats the runbook as the authoritative record of multi-device sample rate stability; once the user has populated entries for at least three devices, the Performance Engineer can sign off the Phase 4 deliverable retroactively or it carries forward to Phase 5 with the validation evidence.

---

## Self review

The PM ran the writing-plans skill self review on this plan:

1. **Spec coverage.** Every Phase 4 deliverable in `docs/plan.md` Phase 4 maps to a task: `signals/` layer (Tasks 2 to 5); `GaitTest` Compose flow (Tasks 6 to 7); raw sensor trace persistence (Tasks 1, 5, 7); orchestrator wiring (Tasks 1, 7, 8); Performance Engineer sign off (Task 11 plus the deferred user task).
2. **Placeholder scan.** Three intentional `TODO` markers exist: the SIE fills them in Task 2 (Robolectric event-injection bodies, the linear-timestamp-derived dt for the fallback Madgwick, and the Android-rotation-vector-to-Quaternion conversion). The plan flags them as SIE-owned authority and the Code Reviewer's Task 12 verdict confirms reasonable choices. No prose placeholder remains.
3. **Type consistency.** `ImuSource`, `AndroidImuSource`, `RawSensorWriter`, `GaitTestViewModel`, `GaitTest`, `GaitTestState`, `GaitFeatures` are named once and used consistently. The `TestResultPayload` extension is documented in Task 1 and consumed in Task 7.

The phase is sized at 60 to 75 percent of a Max plan window per `docs/plan.md`. The reviewer Tasks 9 to 13 are deliberately kept lightweight to leave room for any cleanup commits.
