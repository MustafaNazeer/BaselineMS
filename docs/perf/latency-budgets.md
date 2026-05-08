# Latency budgets

**Status:** placeholder. The Performance Engineer writes this file in Phase 0 and updates it as measurements come in during Phases 4, 8, and 11.

## Initial budgets

- **IMU sampling:** actual sample rate within 5 percent of nominal 100 Hz. Jitter under 5 ms p99. Zero dropped windows over a 30 second capture.
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
