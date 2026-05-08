# Latency budgets

**Status:** placeholder. The Performance Engineer writes this file in Phase 0 and updates it as measurements come in during Phases 4, 8, and 11.

## Initial budgets

- **IMU sampling:** actual sample rate within 5 percent of nominal 100 Hz. Jitter under 5 ms p99. Zero dropped 1 second sub windows over a 30 second capture, where a sub window is dropped when its cumulative delta drift exceeds 5 percent of nominal (the bucket's actual elapsed time falls outside 950 to 1050 ms). See `docs/observability/sensor-runbook.md` for the methodology.
- **Audio capture:** zero glitches over a 30 second capture at 44.1 kHz mono.
- **UI:** 60 fps target on a Pixel 6 class device. 30 fps minimum on a budget Android 12 phone.
- **Battery:** a full weekly battery session (all five tests) consumes under 1 percent of phone battery.

## Measurement log

(Populated by the Performance Engineer.)

## Notes for the Performance Engineer

Always measure release builds with R8 enabled. Debug builds are not representative. Document the device used for each measurement.

## Phase 3 review (allocation discipline)

**Reviewer:** Performance Engineer (Phase 3 reviewer task per `agents/11-performance-engineer.md` Phase 3 section).
**Date:** 2026-05-07.
**Scope:** the ten production Kotlin files under `app/src/main/java/com/mustafan4x/msbattery/dsp/` produced by Phase 3 Tasks 1 through 13. No production code modified by this review.

**Pass criteria.** Phase 3 is a pure JVM module with no real device measurements possible at this stage. The reviewer confirms two things: (1) the per sample state of stateful filters (Butterworth biquad taps, Madgwick quaternion) lives in mutable fields rather than being reallocated per sample; (2) the inner loops over samples in `GaitPipeline.process` do not introduce avoidable per sample allocations beyond what is documented in each file's KDoc. The Phase 4 measurement against real device sample rate and jitter will validate the production hot path; the production hot path itself (the sensor callback that pushes one `ImuSample` into a ring buffer) is not part of the Phase 3 deliverable.

**Headline verdict:** APPROVED. No BLOCKING findings. The DSP package meets the Phase 3 allocation discipline criterion. Two MINOR findings and three INFORMATIONAL findings are noted for awareness; none block Task 16 or Task 17.

### Filter state lives in mutable fields (PASS)

**File:** `app/src/main/java/com/mustafan4x/msbattery/dsp/ButterworthLowPass.kt:75 to 95` (`Biquad` class).

The biquad state (`z1`, `z2`) is held in mutable `var` fields and reset via the `reset()` method at the start of each forward and reverse pass. The `process(x: Double): Double` method uses primitive `double` arithmetic only: no `Vector3`, no boxed `Double`, no intermediate object construction. The forward and reverse loops in `filtfilt` (lines 60, 62, 66, 68) iterate over `DoubleArray` indices and call `Biquad.process` with primitive doubles. Confirmed allocation free in the per sample inner loop.

**File:** `app/src/main/java/com/mustafan4x/msbattery/dsp/Madgwick.kt:24 to 93`.

The quaternion state (`q0`, `q1`, `q2`, `q3`) is held in four mutable `var Double` fields. `update(gyro, accel, dtSeconds)` computes the integration using only primitive `double` locals and reassigns the fields by primitive assignment at the end. No `Quaternion` or `Vector3` is allocated inside `update`. The `gyro.x`, `accel.x` etc. accesses read primitive properties of the parameter objects without boxing. Confirmed allocation free in the per sample inner loop.

### Per call (not per sample) batch buffers (PASS, justified)

**File:** `app/src/main/java/com/mustafan4x/msbattery/dsp/ButterworthLowPass.kt:45 to 73`.

Each `filtfilt` call allocates four `DoubleArray` buffers: `padded` (size `n + 2 * padLen`), `forward` (size `padded.size`), `reverse` (size `padded.size`), and `out` (size `n`). The class KDoc at lines 20 to 26 explicitly documents this and justifies it: the Phase 4 capture path collects samples for 30 seconds before processing, so the filter is invoked once per gait test on a fixed size buffer. Allocation is per process call, not per sample, and is bounded by `n` (about 3000 samples for a 30 second 100 Hz trace). PASS.

**File:** `app/src/main/java/com/mustafan4x/msbattery/dsp/GaitPipeline.kt:62 to 93` (`process` method).

The `process(samples: List<ImuSample>)` entry point allocates several `DoubleArray` and `IntArray` buffers (`timestampSeconds`, three world frame channel arrays via `rotateToWorld`, three filtered arrays via three `filtfilt` calls, `lateralAtStep`, `midStanceIndices`, `evenIndices`, `oddIndices`, `strideLengthsMeters`). Each buffer is sized `O(n)` in samples or `O(n_steps)` in detected steps. The total allocation is `O(n)` in samples per `process` call. Confirmed `O(n)` not `O(n^2)`. PASS.

### MINOR finding 1: per sample `Vector3` allocation in `rotateToWorld`

**File:** `app/src/main/java/com/mustafan4x/msbattery/dsp/GaitPipeline.kt:95 to 109`.

The loop body at line 103 calls `q.rotate(s.linearAcceleration)`, which returns a new `Vector3` per sample (`Quaternion.rotate` at `Quaternion.kt:12 to 21` constructs a fresh `Vector3` from primitives). For a 3000 sample trace this allocates 3000 short lived `Vector3` objects per `process` call. The loop only reads `.x`, `.y`, `.z` from the returned vector immediately, so the allocation is purely for the return value shape.

**Severity:** MINOR. The DSP pipeline runs once per gait test (not in a real time sensor callback), and 3000 short lived allocations are well within the JVM's nursery generation pressure tolerance. The Phase 4 measurement against real device will confirm the actual cost. The fix, if Phase 4 measurement shows it matters, is to inline the quaternion to vector rotation against three primitive locals (or to add a primitive `rotate(qw, qx, qy, qz, vx, vy, vz)` overload) and write directly into the three `DoubleArray` channels. Not blocking.

### MINOR finding 2: per sample `Quaternion` allocations in `orientationQualitySignals`

**File:** `app/src/main/java/com/mustafan4x/msbattery/dsp/GaitPipeline.kt:226 to 271`.

Inside the per sample loop (lines 252 to 267), when the sample carries a non null `rotationVector`, the code calls `madgwick.orientation()` (one new `Quaternion` per call, `Madgwick.kt:31`), then `angleBetween(...)` which calls `.normalized()` on both arguments (two new `Quaternion`s per call, `Quaternion.kt:6 to 10`), then `yawDegrees(...)` which again calls `.normalized()` on its argument (one new `Quaternion`). That is roughly four `Quaternion` allocations per sample on the orientation residual path.

**Severity:** MINOR. Same justification as finding 1: this is per `process` call after the trial has been captured, not a real time per sample callback. For a 3000 sample trace this is about 12,000 short lived `Quaternion` allocations, which the JVM nursery handles cheaply. If the Phase 4 measurement flags this, the fix is to expose `Madgwick`'s internal `q0..q3` as primitive accessors and to compute `angleBetween` and `yawDegrees` from primitive arguments. Not blocking.

### INFORMATIONAL finding 1: `medianStepInterval` recomputed inside the lateral at step loop

**File:** `app/src/main/java/com/mustafan4x/msbattery/dsp/GaitPipeline.kt:75 to 77` and `120 to 138`.

`lateralAtStepIndex` is invoked once per detected step from the `DoubleArray(steps.size) { i -> lateralAtStepIndex(...) }` initializer. Inside `lateralAtStepIndex` the helper calls `medianStepInterval(steps)`, which allocates a `DoubleArray` of size `steps.size - 1` and sorts it. This computation is identical for every `i` and could be hoisted out of the loop. The asymptotic cost is `O(n_steps^2 log n_steps)` in detected steps, not in samples, and `n_steps` is bounded at roughly 60 for a 30 second walk. Negligible at the production scale, but it is the only place in the package where the inner loop body does redundant work that scales super linearly with one of the input dimensions.

**Severity:** INFORMATIONAL. Hoisting the median out of the loop would simplify the code and avoid the small allocation pile up on the step axis. Not a perf concern at the documented step counts. If the SPE chooses to revisit this, a one line hoist into `process` solves it; otherwise it is left as is.

### INFORMATIONAL finding 2: Kotlin `DoubleArray(size, init)` autoboxing of the init lambda

**File:** Used in `GaitPipeline.kt` at lines 65, 75, 135 and in `FeatureExtractor.kt` at line 60.

The Kotlin stdlib `DoubleArray(size: Int, init: (Int) -> Double)` constructor takes a generic `Function1<Int, Double>`. On the JVM, `Function1<Integer, Double>` boxes the `Int` parameter and the `Double` return on each invocation, so each element in the array's initialization conceptually goes through one `Integer.valueOf` and one `Double.valueOf`. Modern JIT compilers (HotSpot C2) routinely scalarize and elide these boxing operations via escape analysis when the lambda is inlined and never stored, but the bytecode level reading is that boxing is requested.

**Severity:** INFORMATIONAL. This is a standard Kotlin idiom that the JIT handles. If a Phase 4 microbenchmark shows it matters in practice, the fix is to use an explicit `for` loop writing into a pre allocated `DoubleArray`. The reviewer is not 100 percent sure the JIT elides every one of these on a real Android device (the Android Runtime ART is not HotSpot), so this is flagged as INFORMATIONAL rather than asserted as a real allocation. Phase 4 measurement settles it.

### INFORMATIONAL finding 3: `ArrayList<Double>` autoboxing in `computeAsymmetry`

**File:** `app/src/main/java/com/mustafan4x/msbattery/dsp/FeatureExtractor.kt:98 to 115`.

`leftIntervals` and `rightIntervals` are declared as `ArrayList<Double>` and populated with `add(gap)` once per step. Each `add` autoboxes a primitive `Double` into a `java.lang.Double`. This is per step, not per sample, and the maximum step count for a 30 second trial at typical cadence is about 60. So the autoboxing pile up is at most 60 boxed `Double`s per `process` call, which is negligible.

**Severity:** INFORMATIONAL. Replacing the `ArrayList<Double>` with two `DoubleArray`s plus an integer cursor would avoid the autoboxing entirely. Not worth the readability hit at this scale, but flagged for the SPE's awareness if a future audit revisits it.

### Summary

| Severity | Count |
|----------|-------|
| BLOCKING | 0 |
| MINOR | 2 |
| INFORMATIONAL | 3 |

**Verdict: APPROVED.** The Phase 3 DSP package meets the allocation discipline criterion: filter state is held in mutable fields, the inner per sample loops in `Butterworth.Biquad.process` and `Madgwick.update` are allocation free, and `GaitPipeline.process` is `O(n)` in samples. The two MINOR per sample object allocation sites in `GaitPipeline` (rotateToWorld, orientationQualitySignals) are batch operations, not real time sensor callbacks, and are within the documented allocation budget for a once per gait test pipeline. They are noted for the Phase 4 real device measurement to confirm. No code changes required for Phase 3 to close.

The PM may proceed to Task 16 (Citation Auditor) and Task 17 (Code Reviewer).

## Phase 4 review (sensor capture path methodology and allocation discipline)

**Verdict: APPROVED WITH MINOR FINDINGS.**

**Reviewer:** Performance Engineer (Phase 4 reviewer task per `agents/11-performance-engineer.md` Phase 4 section, Task 11 of `docs/plans/phase-4-gait-test-module-integration.md`).
**Date:** 2026-05-08.
**Scope:** the Phase 4 sensor capture path code that enables the user's multi device sample rate validation task. The reviewer did not run measurements on a real device (that work is the user's Phase 4 close validation deliverable per the runbook). The reviewer evaluated (1) whether the sample rate counter is measuring the right thing, (2) whether the jitter metric definition is correct, (3) whether the dropped windows metric definition is correct, and (4) whether the production capture path has allocation or scheduling concerns that would prevent clean device measurements.

**Files read in full:** `app/src/main/java/com/mustafan4x/msbattery/signals/ImuSource.kt`, `app/src/main/java/com/mustafan4x/msbattery/signals/AndroidImuSource.kt`, `app/src/main/java/com/mustafan4x/msbattery/signals/RawSensorWriter.kt`, `app/src/main/java/com/mustafan4x/msbattery/battery/gait/GaitTestViewModel.kt`, `app/src/main/java/com/mustafan4x/msbattery/battery/gait/GaitTest.kt`, `app/src/main/java/com/mustafan4x/msbattery/dsp/GaitPipeline.kt`. Cross referenced `docs/observability/sensor-runbook.md`, `docs/adr/0003-sensor-type-choice.md`, and `docs/qa/spe-review-phase-4.md`.

### Question 1: Is the sample rate counter measuring the right thing (delta between successive linear acceleration timestamps)?

**Yes.** A standalone in code "counter" is not exposed as a debug API surface in this Phase, but the methodology in `docs/observability/sensor-runbook.md` lines 60 to 81 derives inter sample delta from the captured CSV's `timestampNanos` column, which is `event.timestamp` from the `TYPE_LINEAR_ACCELERATION` branch (`AndroidImuSource.kt` line 77). `AndroidImuSource.emitIfReady` (line 124) emits exactly one `ImuSample` per linear acceleration event, so successive `timestampNanos` values in the CSV are successive linear acceleration timestamps. The delta the runbook awk pipeline computes is therefore the exact quantity the budget is defined against. The two fields `lastLinearTimestampNanos` (line 64) and `prevLinearTimestampNanos` (line 65) are sufficient to compute one running delta inside the source, and they already gate the fallback Madgwick `dt` derivation in `computeDtSeconds` (lines 163 to 169). They are not exposed publicly. Adding a debug only API (for example a `lastDeltaNanos: Long` accessor or a small ring buffer of the last N deltas) is a nice to have for an in app diagnostic screen but not required for the Phase 4 close validation, because the captured CSV is already the source of truth for the measurement procedure documented in the runbook. Recommendation downgraded from "needed" to "deferrable Phase 11 polish" (see finding M2).

### Question 2: Is the jitter metric defined as the p99 of inter sample delta minus the nominal 10 ms? Yes / no.

**Yes.** `docs/observability/sensor-runbook.md` line 23 reads "Jitter under 5 ms p99. The 99th percentile of `|delta_i minus 10 ms|` over the 30 second capture must be under 5 ms," and the awk snippet at lines 74 to 76 computes `d = $1 - 10; if (d < 0) d = -d` then sorts and indexes at `int(n*0.99)`. The absolute value matches the specification's intent that jitter is two sided around the nominal period, and the indexing is the standard linear interpolation free p99. Note that the sort is performed on the absolute delta minus nominal, which is the right quantity to take the 99th percentile of (sorting raw deltas and subtracting 10 at the p99 boundary would give the wrong answer when the distribution is skewed). The methodology is sound.

### Question 3: Is the dropped windows metric counting any 30 second window in which the cumulative delta drift exceeds 5 percent? Yes / no.

**No, the runbook uses a stricter 1 second sub window definition, not a 30 second window definition.** `docs/observability/sensor-runbook.md` line 24 reads "Zero dropped windows over 30 seconds. No 1 second sub window of the 30 second capture has a cumulative delta drift exceeding 5 percent of nominal." The Phase 4 plan question wording (Task 11 question 3) and the original `docs/perf/latency-budgets.md` "Initial budgets" wording ("Zero dropped windows over a 30 second capture") are both ambiguous and could be read either way. The runbook's operational interpretation, dividing the 30 second capture into thirty 1 second sub windows and asserting each one's elapsed time falls within 5 percent of 1000 ms, is strictly more demanding than asserting the cumulative 30 second drift stays under 5 percent: a single 1 second sub window with a 100 ms drift would fail the 1 second test but be invisible to the 30 second test if the rest of the capture compensated. The reviewer ratifies the runbook's 1 second sub window definition because it catches localized stalls that the 30 second cumulative metric would average away, and a localized stall is exactly the failure mode worth detecting (a thermal throttle event, a brief sensor batching pause, a GC pause on the listener thread). The Phase 4 plan rail wording and the Phase 0 budget wording should be reconciled to match the runbook; that is finding M1 below.

### Question 4: Are there any obvious allocation or coroutine scheduling concerns in AndroidImuSource or RawSensorWriter that would prevent the device measurement from yielding clean numbers?

**Yes, two MINOR concerns and one MEDIUM concern (M3 below) on the coroutine scheduling axis.** The allocation review of `AndroidImuSource.onSensorChanged` plus `emitIfReady` (architectural rail 7 verification) follows; the coroutine scheduling review of `GaitTestViewModel` follows after that.

### Allocation review: AndroidImuSource onSensorChanged plus emitIfReady (rail 7)

`AndroidImuSource.kt` lines 67 to 154 are the per sensor event hot path. Per architectural rail 7 the path must be allocation free in the steady state beyond the unavoidable `ImuSample` per emit. The reviewer's findings:

1. **Each sensor event allocates one `Vector3` or `Quaternion` and stores the reference.** Lines 71, 81, 91 each construct a fresh `Vector3` from the event's three float values; line 88 calls `quaternionFromAndroidRotationVector` which constructs a `Quaternion` and then a normalized `Quaternion`. The constructed value is assigned to `lastLinear`, `lastGyro`, `lastAccel`, or `lastRotation` and held until the next event of the same type. These allocations are not "transient" in the sense that the rail 7 wording proscribes, because each one is held as state until the next event of the same type overwrites it; they are part of what `ImuSample` ultimately encloses. PASS on rail 7 spirit.
2. **`quaternionFromAndroidRotationVector` allocates two `Quaternion` instances per `TYPE_ROTATION_VECTOR` event.** Lines 181 to 192. The constructor `Quaternion(w, x, y, z)` allocates the first; `.normalized()` (`Quaternion.kt` line 6 to 10) allocates a second by returning a new `Quaternion`. The first allocation is purely transient; only the normalized result is stored. At a 100 Hz target this is one extra allocation per rotation event, or about 3000 short lived `Quaternion` allocations over a 30 second capture. The JVM nursery handles this comfortably, but on Android Runtime ART under thermal pressure the per allocation cost is not free. Severity: MINOR (finding M4).
3. **One `ImuSample` allocation per linear acceleration event.** Line 145 to 153. This is the unavoidable allocation rail 7 explicitly permits. PASS.
4. **`Vector3.ZERO` fallback at line 126 is a singleton reference, no allocation.** PASS.
5. **`accelForSample = lastAccel ?: linear` at line 143 is a reference selection, no allocation.** PASS.
6. **`emissions.tryEmit` does not allocate.** `MutableSharedFlow.tryEmit` writes into the pre allocated 256 element ring buffer (`extraBufferCapacity = 256` on line 54). PASS.

Verdict on rail 7: substantially PASS. The one transient allocation that arguably violates the wording is the discarded `Quaternion` constructor result inside `quaternionFromAndroidRotationVector`. Easy fix is to compute the normalized quaternion components in primitive locals and call the `Quaternion` constructor exactly once.

### Allocation review: RawSensorWriter.write loop

`RawSensorWriter.kt` lines 28 to 63 is invoked once per capture from the view model's `writerJob` and then collects the `Flow<ImuSample>` for the duration of the capture. Per row, `appendRow` (lines 65 to 97) calls `.toString()` on 13 `Double` fields and the `Long` timestamp. Each `Double.toString()` allocates a fresh `String` (Kotlin's primitive `Double.toString()` ultimately routes to `java.lang.Double.toString(double)` which always allocates). Each `,` and `\n` `.append` writes a single character. At 14 string allocations per row times 3000 rows that is roughly 42,000 short lived `String` allocations per capture, plus the `StringBuilder` like growth inside `OutputStreamWriter`'s charset encoder buffer.

This is not bound by architectural rail 7 (the rail names `signals/AndroidImuSource per sample callback path`, not the raw writer). It is, however, the consumer of the `MutableSharedFlow` buffer; if the writer cannot keep up with the producer the buffer will fill and `BufferOverflow.DROP_OLDEST` (`AndroidImuSource.kt` line 55) will silently drop samples, which would corrupt the user's sample rate measurement.

The reviewer's check: 100 Hz means one row every 10 ms. Writing 14 `Double.toString()` plus 14 `Writer.append` calls plus deflate on a per row basis on a modern Android device is well under 10 ms; on a budget Android 12 phone it is plausibly 1 to 3 ms per row in the worst case. Headroom is large enough that the `extraBufferCapacity = 256` buffer should never fill in the steady state. Verdict: acceptable, but flagged as MINOR (finding M5) because the per row `String` allocations stress the GC and could on a thermally throttled device coincide with a 10 ms hiccup that the producer interprets as jitter. The fix, if Phase 4 close measurements show jitter spikes localized to the writer thread, is to switch `appendRow` to a pre allocated `char[]` buffer with a primitive `Double.toString` to char buffer routine, or to preformat each row in a reusable `StringBuilder`.

### Coroutine scheduling review: GaitTestViewModel collect plus simultaneous write path

`GaitTestViewModel.kt` lines 49 to 88 wires the capture lifecycle. The constructor takes a `CoroutineScope` (line 39); the production binding is `rememberCoroutineScope()` inside `GaitTest.Content` (`GaitTest.kt` line 40), which produces a scope that dispatches on `Dispatchers.Main.immediate` per the Compose convention. That scope is then used for both `writerJob` (line 66) and `captureJob` (line 79).

Concrete consequence: `imuSource.stream().onEach { capturedSamples += it }` and the entire `RawSensorWriter.write` body, including the gzip deflate and the `FileOutputStream.write` calls, run on the **main thread**. So does the `delay(TICK_INTERVAL_MILLIS)` loop that drives the progress UI. So does Compose's recomposition triggered by `_state.value = GaitTestState.Capturing(...)` every 100 ms.

This is a MEDIUM severity coroutine scheduling concern (finding M3). The risks at production time:

1. **UI jank during capture.** Gzip deflate on the main thread for 100 rows per second will visibly degrade the 60 fps target documented in the "Initial budgets" UI row of this same document. The capture screen has a small UI surface (a circular progress indicator and an elapsed time readout, per `GaitCaptureScreen`), so the jank may be tolerable, but the budget says 60 fps target on a Pixel 6 class device and 30 fps minimum on a budget phone; main thread gzip is a credible threat to the budget on the budget device.
2. **Listener thread starvation.** The `SensorEventListener` callbacks run on the Android main looper by default (no `Handler` is passed to `registerListener` at `AndroidImuSource.kt` lines 105 to 115). If the writer's gzip work occupies the main thread for more than 10 ms in a row, the next `onSensorChanged` callback is delayed and the producer side `lastLinearTimestampNanos` value advances behind the wall clock. The user's CSV based sample rate measurement will then attribute that jitter to the sensor framework when the actual cause is the writer.
3. **Buffer pressure.** Same root cause: if the main thread is occupied, the collector cannot pull from the `MutableSharedFlow` buffer, which fills toward the 256 element capacity. At 100 Hz that is 2.56 seconds of headroom.

The fix is small: pass a dedicated `Handler` to `SensorManager.registerListener` so the listener thread is not the main thread, and run `writerJob` on `Dispatchers.IO` rather than the composition's scope. A two argument override of `RawSensorWriter.write` taking a `CoroutineDispatcher`, or a `withContext(Dispatchers.IO)` wrapper at the call site in `GaitTestViewModel.beginCapture`, suffices. The 1 second tick `captureJob` can stay on Main because it is for UI updates.

This is the most consequential finding of the review. It is MEDIUM rather than HIGH because (a) the Phase 4 close validation task's measurement is derived from `event.timestamp` values, which the Android sensor framework stamps from a hardware clock at the time of the sensor event, not at the time of the listener callback. The CSV's measured sample rate is therefore a property of the hardware, not the listener thread schedule, and is robust to writer jank, **provided no samples are dropped at the SharedFlow boundary**. (b) The 256 element buffer has 2.56 seconds of headroom which exceeds any plausible main thread stall that does not also kill the test entirely. So in practice the user's measurement will likely be valid even with the current scheduling. But the MEDIUM severity captures that this is luck rather than design, and the fix is small enough that landing it before Phase 4 close is worthwhile.

### Ratification of SPE I1, I2, I3 from a perf perspective

**I1 (tail edge sample loss on capture cancel).** Ratified as INFORMATIONAL. The SPE's framing is correct: a handful of tail edge samples lost at `finishCapture` time fall well below the gait pipeline's feature sensitivity. From a perf perspective this is also a non issue: the `cancelAndJoin` Phase 5 prep fix the SPE recommended is correct and will not introduce any allocation or scheduling regression.

**I2 (first fallback Madgwick step uses gravity removed input).** Ratified as INFORMATIONAL. Single sample anomalous input is below the quality score's sensitivity; the SPE's reasoning is sound. From a perf perspective there is no allocation impact; the substitution at `AndroidImuSource.kt` line 130 is a reference selection.

**I3 (RawSensorWriter virtuality vs interface based pattern).** Ratified as INFORMATIONAL. The SPE's note is a code shape preference and not a perf concern. Virtual dispatch on the writer's `write` method occurs once per capture and is dwarfed by the gzip and IO work; even the `appendRow` private method is not on a virtual dispatch site that matters at 100 Hz. No perf objection.

### Multi device measurement readiness

**The code is ready for the user's Phase 4 close validation task.** The `event.timestamp` based measurement methodology in `docs/observability/sensor-runbook.md` is robust to the M3 main thread scheduling concern in the typical case (the 256 element SharedFlow buffer has 2.56 seconds of headroom), and the CSV based awk pipeline will produce valid mean, p50, p99, jitter, and dropped sub window numbers from any captured trace. The user can begin populating runbook entries today. M3 should be addressed before Phase 5 close at the latest, so that the production capture path matches the design intent and so that real walking course recordings in Phase 5 are not subtly biased by main thread jank.

### Findings

| Severity | ID | Description | File and lines | Owner |
|----------|----|-----|----|----|
| MEDIUM | M3 | `GaitTestViewModel.beginCapture` runs `writerJob` (gzip plus IO) on `rememberCoroutineScope()` which dispatches on Main; `SensorManager.registerListener` is called without a `Handler` so callbacks also fire on Main. This creates main thread contention that risks both UI jank (60 fps budget) and producer side schedule drift. The CSV based `event.timestamp` measurement is robust to this in the typical case because of the 2.56 second SharedFlow headroom, but the design should not rely on luck. Fix: pass `Dispatchers.IO` to `writerJob.launch` (or `withContext(Dispatchers.IO)` around the `rawSensorWriter.write` call), and pass a dedicated `Handler` (or `HandlerThread`) to `SensorManager.registerListener` so callbacks do not run on Main. | `app/src/main/java/com/mustafan4x/msbattery/battery/gait/GaitTestViewModel.kt:62 to 88`; `app/src/main/java/com/mustafan4x/msbattery/signals/AndroidImuSource.kt:103 to 116` | Android Engineer (view model dispatcher); Sensor Integration Engineer (listener handler) |
| MINOR | M1 | Dropped windows budget wording is inconsistent across documents. The Phase 0 "Initial budgets" line ("Zero dropped windows over a 30 second capture") and the Phase 4 plan Task 11 question 3 wording ("any 30 second window") are both 30 second framings; the sensor runbook's operational definition is 1 second sub windows. The runbook's stricter definition is the right one (it catches localized stalls), but the budget wording should be reconciled. Fix: update the "Initial budgets" line in this same file to read "Zero 1 second sub windows over a 30 second capture in which the cumulative delta drift exceeds 5 percent of nominal" and update the Phase 4 plan rail wording in the same edit. | `docs/perf/latency-budgets.md:7`; `docs/plans/phase-4-gait-test-module-integration.md:676` (the question 3 wording) | Performance Engineer (this file); PM (plan rail wording) |
| MINOR | M2 | No debug only sample rate counter is exposed on `AndroidImuSource`. The Phase 4 close validation task is fully achievable via the captured CSV, so this is not blocking. A small `lastDeltaNanos: Long` accessor or a windowed mean delta would let the user see the rate live in app without pulling the file via adb, which would be a UX win for the eventual debug or about screen. Defer to Phase 11 polish. | `app/src/main/java/com/mustafan4x/msbattery/signals/AndroidImuSource.kt:64 to 65, 124 to 154` | Sensor Integration Engineer (Phase 11 polish) |
| MINOR | M4 | `quaternionFromAndroidRotationVector` allocates a transient `Quaternion` that is immediately discarded by the `.normalized()` call. About 3000 redundant allocations per 30 second capture on the platform fused path. Fix: compute the normalized components in primitive locals and call `Quaternion(w, x, y, z)` exactly once. Architectural rail 7 wording reads "no transient `Vector3` or `Quaternion` allocations on the hot path beyond what `ImuSample` itself encloses"; the discarded constructor result is the only literal violation in the file. | `app/src/main/java/com/mustafan4x/msbattery/signals/AndroidImuSource.kt:181 to 192` | Sensor Integration Engineer |
| MINOR | M5 | `RawSensorWriter.appendRow` calls `.toString()` on 13 `Double` fields and the `Long` timestamp per row. About 42,000 short lived `String` allocations per 30 second capture. Not bound by rail 7 (the writer is not the sensor callback path), but on a thermally throttled budget device this could coincide with a 10 ms hiccup that the producer attributes to the sensor framework. If Phase 4 close measurements show jitter spikes that correlate with the writer thread, switch to a reusable `StringBuilder` or a `char[]` buffer per row. | `app/src/main/java/com/mustafan4x/msbattery/signals/RawSensorWriter.kt:65 to 97` | Sensor Integration Engineer (Phase 5 prep if device measurement flags it) |
| INFORMATIONAL | I1r | Tail edge sample loss on cancel. Ratified from `docs/qa/spe-review-phase-4.md` I1; no new perf objection. | `app/src/main/java/com/mustafan4x/msbattery/battery/gait/GaitTestViewModel.kt:90 to 106` | Android Engineer (Phase 5 prep) |
| INFORMATIONAL | I2r | First fallback Madgwick step uses gravity removed input. Ratified from SPE I2; no perf impact. | `app/src/main/java/com/mustafan4x/msbattery/signals/AndroidImuSource.kt:124 to 137` | Sensor Integration Engineer (Phase 5) |
| INFORMATIONAL | I3r | `RawSensorWriter` virtuality preference. Ratified from SPE I3; no perf objection. | `app/src/main/java/com/mustafan4x/msbattery/signals/RawSensorWriter.kt:26, 28` | Code Reviewer (Phase 8 voice test, optional) |

### Summary

| Severity | Count |
|----------|-------|
| BLOCKING | 0 |
| HIGH | 0 |
| MEDIUM | 1 |
| MINOR | 4 |
| INFORMATIONAL | 3 |

**Verdict: APPROVED WITH MINOR FINDINGS.** The methodology questions in the Phase 4 plan Task 11 spec are answered correctly by the runbook (yes / yes / no by the stricter sub window interpretation, ratified). The allocation discipline on the `signals/AndroidImuSource` per sample callback path substantially meets architectural rail 7, with one literal violation (M4) that is a one line fix. The MEDIUM coroutine scheduling concern (M3) is the most consequential finding: the production capture path runs gzip and IO on the main thread, which is a latent risk to both the UI 60 fps budget and the producer side schedule. M3 is DEFERRABLE rather than BLOCKING because the user's CSV based measurement methodology is robust to it in the typical case (the SharedFlow has 2.56 seconds of headroom and `event.timestamp` is hardware stamped), but it should be fixed before Phase 5 close so the production code matches the design intent. M1 (the dropped windows wording reconciliation) is a documentation fix in this same file plus the Phase 4 plan; it is also DEFERRABLE.

The code is ready for the user's multi device sample rate validation task. The PM may proceed to Code Reviewer Task 12.
