# Signal Processing Engineer Review: Phase 4 (Gait Test Module Integration)

Reviewer: Signal Processing Engineer agent (session 2026-05-07)
Commits reviewed: `b8187c8..d9b4795` on `main` (Phase 4 Tasks 1 to 8: TestResultPayload extension, AndroidImuSource, ADR 0003, sensor runbook, RawSensorWriter, four GaitTest screens, GaitTestViewModel and TestModule, BaselineMSApp wiring).
Verdict: **APPROVED WITH MINOR FINDINGS**

---

## Summary

Phase 4 preserves the Phase 3 DSP module's input contract. Samples emitted by
`AndroidImuSource` reach `GaitPipeline.process` in order, in full, and without
resampling; the view model accumulates samples and passes the full list at end of
capture; the fallback Madgwick path produces a quaternion in the device to world
convention matching Android `TYPE_ROTATION_VECTOR`. The two `open` markings
(`GaitPipeline`, `RawSensorWriter`) are acceptable. The fallback's deviation from
plan rail 3 wording (it uses `TYPE_ACCELEROMETER`, not `TYPE_LINEAR_ACCELERATION`)
is correctness driven and is ratified by ADR 0003. Test suite: `tests=102
failures=0 errors=0 skipped=0` after `./gradlew :app:testDebugUnitTest --rerun-tasks`
(up from 83 at Phase 3 close).

---

## Rail by rail verification

### Rail 1: ImuSamples are buffered without resampling

**Verified yes.** `AndroidImuSource.kt` lines 67 to 101 stores the most recent
reading for each sensor type. Only the linear acceleration branch calls
`emitIfReady()` (line 78), so each linear acceleration event produces exactly one
`ImuSample` carrying held gyro and rotation values. No decimation, no resampling,
no batching. The zero order hold is acceptable: the pipeline consumes
`linearAcceleration` and `rotationVector` per sample and never assumes sub
millisecond cross channel synchronization, and the 20 Hz Butterworth low pass
(`GaitPipeline.kt` line 50) suppresses any high frequency artifact the held lag
could introduce.

### Rail 2: The view model accumulates samples and passes the full list to the pipeline

**Verified yes.** `GaitTestViewModel.kt` line 63 clears at capture entry; line 69
(`imuSource.stream().onEach { capturedSamples += it }`) appends every emitted
sample; line 94 (`gaitPipeline.process(capturedSamples.toList())`) passes the full
snapshot. No decimation (`onEach` runs for every element pulled from the upstream
`MutableSharedFlow`), no truncation (the list is unbounded), no reordering
(`MutableList.+=` preserves order and `MutableSharedFlow` emits FIFO). A small
tail edge concern is captured under finding I1.

### Rail 3: The fallback Madgwick path produces a quaternion in the device to world convention

**Verified yes.** `dsp/Madgwick.kt` lines 11 to 14 document
`orientation().rotate(deviceVec) = worldVec`, which matches Android
`TYPE_ROTATION_VECTOR`. `Madgwick.orientation()` returns `Quaternion(q0, q1, q2, q3)`
in the same `(w, x, y, z)` ordering as `dsp/Quaternion.kt`.
`AndroidImuSource.kt` line 132 stamps `fallbackMadgwick.orientation()` into
`ImuSample.rotationVector` with no axis swap, no conjugation, no sign flip; the
platform fused path at line 88 calls `quaternionFromAndroidRotationVector`, which
builds the same normalized `(w, x, y, z)` Quaternion. `GaitPipeline.rotateToWorld`
calls `q.rotate(s.linearAcceleration)` consistently for both paths.

---

## Plan deviation ratifications

### (a) `GaitPipeline` and `process` made `open`

**Ratified.** `git show 34a3174 -- app/src/main/java/com/mustafan4x/baselinems/dsp/GaitPipeline.kt`
shows the only diff is `class` to `open class` and `fun process` to `open fun process`.
No body change, no signature change, no behavior shift. The test suite uses the
override via `FakeGaitPipeline` in `GaitTestViewModelTest.kt` lines 46 to 52. Phase 3
invariants (allocation discipline, the seven stage composition) are preserved.

### (b) `RawSensorWriter` and `write` made `open`

**Ratified with a minor preference noted in finding I3.** The class was created `open`
from scratch in commit `2f47991`. The writer runs on the IO coroutine, not the per
sample DSP path, so virtual dispatch overhead is irrelevant. The AE made a defensible
choice for testability via `CountingRawSensorWriter` in `GaitTestViewModelTest.kt`
lines 54 to 62.

### (c) Fallback Madgwick uses `TYPE_ACCELEROMETER` (gravity included), not `TYPE_LINEAR_ACCELERATION`

**Ratified.** From a DSP correctness perspective this is the only viable choice.
Madgwick (2010) eq 25 to eq 33 (the accelerometer correction term) measures the
residual between the gyro integrated orientation's predicted gravity direction and
the observed acceleration direction. Feeding gravity removed input strips the
observed gravity reference vector; the correction term collapses to noise around
zero and the filter degenerates to pure gyro integration. ADR 0003 documents this;
the implementation correctly registers `TYPE_ACCELEROMETER` as a fourth listener
only on the fallback path. The plan rail 3 wording is the artifact that needs
reconciliation, not the implementation. A small first sample transient is captured
under finding I2.

---

## Findings

### MINOR M1: Fallback Madgwick test feeds gravity included data through the linearAcceleration channel

**File:** `app/src/test/java/com/mustafan4x/baselinems/signals/AndroidImuSourceTest.kt`,
lines 147 to 183.

The fallback test omits `Sensor.TYPE_ACCELEROMETER` from the shadow sensor manager
(only linear and gyro are added on lines 150 to 151), so `rawAccel` resolves to
null and `emitIfReady` line 130 substitutes `linear` for the absent `lastAccel`.
The test then feeds `9.81f` on the z component of `TYPE_LINEAR_ACCELERATION`
(line 171), which is unrealistic because that channel is gravity removed by
definition. The test asserts non null `rotationVector`, which passes regardless of
input quality; it does not exercise the real production fallback semantics.

**Severity:** MINOR. Test fixture realism issue, not a runtime bug.

**Recommended action:** Add a `TYPE_ACCELEROMETER` shadow sensor and feed gravity
included `(0, 0, 9.81)` events on that channel while feeding gravity removed
`(0, 0, 0)` on `TYPE_LINEAR_ACCELERATION`. Optionally assert the converged
`rotationVector` is approximately identity after warmup. Owner: Sensor Integration
Engineer.

---

### INFORMATIONAL I1: Tail edge sample loss on capture cancellation

**File:** `app/src/main/java/com/mustafan4x/baselinems/battery/gait/GaitTestViewModel.kt`,
lines 90 to 96 (`finishCapture`) and 98 to 106 (`onCancel`).

`AndroidImuSource.emissions` is a `MutableSharedFlow` with `extraBufferCapacity = 256`
and `onBufferOverflow = DROP_OLDEST`. `finishCapture` calls `imuSource.stop()` then
`writerJob?.cancel()`. Buffered samples the collector has not yet pulled are lost
when the writer job is cancelled, because `onEach { capturedSamples += it }` only
runs as the collector pulls each element. Practical loss is at most a handful of
samples at the trailing edge of the 30 second window.

**Severity:** INFORMATIONAL. Gait features are robust to small tail edge truncation.

**Recommended action:** Phase 5 prep can switch `finishCapture` to `cancelAndJoin`
after `stop()` so the collector flushes pending elements first. Owner: Android
Engineer (Phase 5 prep).

---

### INFORMATIONAL I2: First fallback Madgwick step uses gravity removed input

**File:** `app/src/main/java/com/mustafan4x/baselinems/signals/AndroidImuSource.kt`,
lines 124 to 137.

On the first linear acceleration event after `start()`, `lastAccel` may be null
because no `TYPE_ACCELEROMETER` event has fired yet. The fallback branch substitutes
`linear` for `lastAccel` (line 130). For one sample the Madgwick filter sees gravity
removed input; subsequent samples use `lastAccel` correctly.

**Severity:** INFORMATIONAL. A single anomalous input is below the quality score's
sensitivity; ADR 0002's beta = 0.1 makes the filter slow to commit to any one
observation, and the in pipeline Madgwick downstream at `GaitPipeline.kt` lines
230 to 246 pre warms with the first 50 static gravity samples.

**Recommended action:** Document in the runbook if Phase 5 fallback recordings
show first samples are visibly off. Owner: Sensor Integration Engineer (Phase 5).

---

### INFORMATIONAL I3: `RawSensorWriter` virtuality is inverted from the project's interface based pattern

**File:** `app/src/main/java/com/mustafan4x/baselinems/signals/RawSensorWriter.kt`,
lines 26 and 28.

The class is `open` and `write` is `open` to enable test doubling. The project's
pattern for swappable components is a small interface (`ImuSource` is the example
in this same Phase 4 dispatch). A `RawSensorSink` interface would let the
production class stay `final`. Both shapes work; the chosen shape is concise.

**Severity:** INFORMATIONAL. **Recommended action:** None for Phase 4. Phase 8
voice test can choose the interface pattern if preferred. Owner: Code Reviewer.

---

## Recommendations for the Code Reviewer's Task 12 verdict

1. **Reconcile plan rail 3 wording with ADR 0003.** The plan rail says "linear
   acceleration plus gyroscope inputs"; the implementation correctly uses gravity
   included `TYPE_ACCELEROMETER`. Edit the plan rail to match the ADR.
2. **Treat M1 as a Phase 4 cleanup item.** Tighten the fallback test before Phase
   5 real device measurements rely on the fallback path's quaternion accuracy.
3. **No DSP module changes required.** Phase 3 DSP was not touched except for two
   purely additive `open` keywords. Invariants (no `android.*` imports, allocation
   discipline, the seven stage composition) all remain intact.
4. **The 256 element shared flow buffer is sized appropriately.** Per I1 the tail
   edge truncation on cancel is below the pipeline's feature sensitivity.

---

## Sign off

Verdict: **APPROVED WITH MINOR FINDINGS.**

Findings: 0 high, 0 medium, 1 minor (M1), 3 informational (I1, I2, I3).

None block Code Reviewer Task 12. M1 is a recommended Phase 4 cleanup the SIE can
land before Task 12 sign off; the other three are notes for Phase 5 prep.
