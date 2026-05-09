# BaselineMS Phase 3: Gait Signal Processing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Phase 3 is heavily TDD'd against synthetic ground truth; the test fixture for a step **must** be written and committed before the implementation step.

**Goal:** Build the gait digital signal processing pipeline as a pure Kotlin module that takes IMU samples in and returns gait features (cadence, stride length, step time CV, stride asymmetry, double support time) out, with no Android sensor or UI dependencies. Heavily TDD'd against a parameterized synthetic IMU generator.

**Architecture:** The pipeline is a layered pure Kotlin module under `app/src/main/java/com/mustafan4x/baselinems/dsp/`. It consumes a `Flow<ImuSample>` produced upstream (the Sensor Integration Engineer wires that up in Phase 4) and emits a `GaitFeatures` value at the end of a 30 second window. Internally it runs: Butterworth low pass, Madgwick orientation, gravity removal into world frame, step detection (peak finder on vertical linear acceleration), left or right stride pairing, ZUPT stride length integration, and feature extraction. Test fixtures live under `app/src/test/java/com/mustafan4x/baselinems/fixtures/`, owned by the Test Fixture Engineer. The DSP package and the fixtures package together define the contract that Phase 4 will integrate against.

**Tech Stack:** Kotlin 1.9 (already on the project), pure JVM library code (no Android SDK calls in DSP code, no Robolectric requirement), JUnit 4 for the tests (already on the project), kotlin.math for trigonometry. No new third party dependencies are added in Phase 3 unless the Madgwick ADR concludes otherwise (see Task 14). The fixtures package depends on `kotlin.math` only.

**Related spec:** `~/src/BaselineMS/SPEC.md` Section 7 (Gait Module Deep Dive) and Section 6.2 (Gait Test summary).

**Related agent briefs:**
- `~/src/BaselineMS/agents/02-signal-processing-engineer.md` (lead implementer of every DSP module).
- `~/src/BaselineMS/agents/20-test-fixture-engineer.md` (lead designer of the synthetic IMU generator and pre canned fixtures).
- `~/src/BaselineMS/agents/11-performance-engineer.md` (Phase 3 reviewer for allocation free hot paths).
- `~/src/BaselineMS/agents/16-citation-auditor.md` (Phase 3 review of DSP method citations).
- `~/src/BaselineMS/agents/10-code-reviewer.md` (PR review at phase close).
- `~/src/BaselineMS/agents/09-qa-engineer.md` (regression checklist and phase sign off).

**Platform note:** Every command in this plan runs on Linux. Phase 3 introduces no Android sensor dependencies. The CI workflow (`.github/workflows/test.yml`, established Phase 0) already runs `./gradlew :app:testDebugUnitTest` on every PR; that is the command this plan exercises throughout.

---

## File Map

Files this plan creates or modifies:

```
~/src/BaselineMS/
├── SPEC.md                                        (already amended Phase 3 prep: Section 6.1 miss rate definition)
├── STATUS.md                                      (PM updates: Phase 3 in progress -> completed)
├── docs/
│   ├── plans/
│   │   └── phase-3-gait-signal-processing.md      (this file)
│   ├── qa/
│   │   ├── fixtures.md                            (NEW: TFE documents every fixture)
│   │   └── regression-checklist.md                (APPEND: Phase 3 regression entries)
│   ├── perf/
│   │   └── latency-budgets.md                     (APPEND: Phase 3 actual measurements)
│   ├── adr/
│   │   └── 0002-madgwick-from-scratch.md          (NEW: SPE writes the ADR)
│   └── source/
│       └── citation-audit-log.md                  (APPEND: CA Phase 3 audits)
└── app/src/
    ├── main/java/com/mustafan4x/baselinems/
    │   └── dsp/
    │       ├── ImuSample.kt                       (data class shared with fixtures)
    │       ├── Vector3.kt                         (3D vector value type)
    │       ├── Quaternion.kt                      (orientation value type)
    │       ├── ButterworthLowPass.kt              (4th order, zero phase, 20 Hz)
    │       ├── Madgwick.kt                        (orientation filter)
    │       ├── WorldFrame.kt                      (gravity removal + frame transform)
    │       ├── StepDetector.kt                    (peak finder on vertical accel)
    │       ├── StridePairing.kt                   (left/right assignment)
    │       ├── Zupt.kt                            (zero velocity update integrator)
    │       ├── FeatureExtractor.kt                (cadence, stride length, CV, asymmetry, DST)
    │       ├── GaitFeatures.kt                    (output value class)
    │       └── GaitPipeline.kt                    (composes everything; public entry point)
    └── test/java/com/mustafan4x/baselinems/
        ├── fixtures/
        │   ├── SyntheticImu.kt                    (parameterized generator)
        │   ├── PreCannedFixtures.kt               (slowWalk, normalWalk, briskWalk, etc.)
        │   └── SyntheticImuTest.kt                (round trip tests on the generator itself)
        └── dsp/
            ├── ButterworthLowPassTest.kt
            ├── MadgwickTest.kt
            ├── WorldFrameTest.kt
            ├── StepDetectorTest.kt
            ├── StridePairingTest.kt
            ├── ZuptTest.kt
            ├── FeatureExtractorTest.kt
            └── GaitPipelineIntegrationTest.kt
```

The DSP package and the fixtures package depend only on Kotlin standard library and `kotlin.math`. Nothing in `data/`, `battery/`, `ui/`, or `util/` changes in Phase 3.

---

## Coordination notes for the dispatching PM

- **Test Fixture Engineer (TFE) runs first.** The Signal Processing Engineer (SPE) cannot TDD against fixtures that do not exist yet. Tasks 1 to 4 are TFE; tasks 5 to 14 are SPE; tasks 15 to 18 are reviewers.
- **Specialists do not recursively dispatch.** If TFE or SPE surfaces a need outside their role (for example, SPE needs a new fixture variant), they report it back to the PM, who dispatches.
- **Every Kotlin file the SPE produces must have a co committed test file.** No DSP commit lands without a passing test. The Code Reviewer will reject any commit that adds DSP code without an accompanying test.
- **The Madgwick ADR (Task 14) is part of the Phase 3 deliverable.** It documents why the project rolled its own filter rather than depending on a library. This is in `SPEC.md` Section 7.1 step 2 as an explicit teaching choice.
- **The DSP package compiles as plain JVM code.** Do not import `android.*` anywhere under `dsp/`. The Sensor Integration Engineer will adapt `android.hardware.SensorEvent` to `ImuSample` in Phase 4.

---

## Architectural decisions locked in by this plan

These are not up for SPE or TFE rediscovery. They follow from `SPEC.md` Section 7 and are the contract this plan enforces:

1. **Sample rate.** The pipeline assumes 100 Hz nominal but is tolerant of small jitter (within 5 percent per `docs/perf/latency-budgets.md` Phase 0). Per sample timestamps are honored in seconds derived from `timestampNanos`; the pipeline never assumes a fixed delta.
2. **Coordinate frame.** Device frame for raw inputs (accelerometer, gyroscope). World frame for the step detection and stride length stages, after Madgwick rotates and the gravity vector is removed. The world frame is right handed, Z up.
3. **Filtering order.** Low pass first (4th order Butterworth, 20 Hz cutoff, zero phase via forward then reverse pass), then orientation, then gravity removal, then step detection.
4. **Step detection signal.** Vertical (world frame Z) component of linear acceleration after low pass. Peak finder with prominence and inter peak distance constraints from `SPEC.md` Section 7.1 step 5.
5. **Stride length method.** Zero velocity update (ZUPT) with mid stance instants detected from local minima of acceleration magnitude, integration from one mid stance to the next, velocity reset to zero at each mid stance.
6. **Feature outputs.** Cadence (steps per minute), mean stride length (meters), step time CV (dimensionless), stride asymmetry (signed, symmetric mean denominator), double support time (seconds), quality score (0 to 1). All five plus quality score are mandatory outputs.
7. **Madgwick implementation.** Rolled from scratch per `SPEC.md` Section 7.1 step 2 ("teaching exercise"), with the platform `TYPE_ROTATION_VECTOR` quaternion (when present in the input sample) used as a parallel reference for the quality score residual. The from scratch decision is recorded as ADR 0002.
8. **No allocations on the hot path.** The per sample callbacks must not allocate. State (filter taps, Madgwick state, peak finder ring buffer) lives in mutable fields. This is enforced by the Performance Engineer's review at Task 15.

---

## Task 0: PM prep before any specialist runs

**Files:**
- Modify: `~/src/BaselineMS/SPEC.md` (already done in Phase 3 prep, this task is a verification step)
- Modify: `~/src/BaselineMS/STATUS.md` (already done in Phase 3 prep, verification step)
- Verify: clean working tree on `main` before TFE dispatch

- [ ] **Step 1: Verify SPEC.md Section 6.1 amendment is committed**

Run: `cd ~/src/BaselineMS && git log --oneline -3`
Expected: a recent commit message naming the SPEC.md Section 6.1 miss rate operational definition amendment.

- [ ] **Step 2: Verify STATUS.md row 3 reads "in progress"**

Run: `grep "^| 3 " ~/src/BaselineMS/STATUS.md`
Expected: the row contains `| in progress |`.

- [ ] **Step 3: Verify the working tree is clean**

Run: `cd ~/src/BaselineMS && git status --short`
Expected: empty output (no uncommitted changes).

If any of the three checks fails, the PM addresses the gap before dispatching TFE. Specialists are not dispatched into a dirty tree.

---

## Task 1: Establish the DSP and fixtures package skeleton plus the shared `ImuSample` contract

**Owner:** Test Fixture Engineer (lead, because the fixtures package owns the synthetic generator that consumes `ImuSample`; the DSP package will import `ImuSample` from the shared location).

**Files:**
- Create: `app/src/main/java/com/mustafan4x/baselinems/dsp/Vector3.kt`
- Create: `app/src/main/java/com/mustafan4x/baselinems/dsp/Quaternion.kt`
- Create: `app/src/main/java/com/mustafan4x/baselinems/dsp/ImuSample.kt`
- Create: `app/src/test/java/com/mustafan4x/baselinems/fixtures/.gitkeep` (placeholder so the package directory exists before SyntheticImu.kt lands)
- Create: `app/src/test/java/com/mustafan4x/baselinems/dsp/.gitkeep` (same reason for the DSP test package)

- [ ] **Step 1: Write `Vector3.kt`**

```kotlin
package com.mustafan4x.baselinems.dsp

import kotlin.math.sqrt

data class Vector3(val x: Double, val y: Double, val z: Double) {
    fun magnitude(): Double = sqrt(x * x + y * y + z * z)

    operator fun plus(other: Vector3): Vector3 = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3): Vector3 = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Double): Vector3 = Vector3(x * scalar, y * scalar, z * scalar)

    companion object {
        val ZERO = Vector3(0.0, 0.0, 0.0)
    }
}
```

- [ ] **Step 2: Write `Quaternion.kt`**

```kotlin
package com.mustafan4x.baselinems.dsp

import kotlin.math.sqrt

data class Quaternion(val w: Double, val x: Double, val y: Double, val z: Double) {
    fun normalized(): Quaternion {
        val n = sqrt(w * w + x * x + y * y + z * z)
        if (n == 0.0) return IDENTITY
        return Quaternion(w / n, x / n, y / n, z / n)
    }

    fun rotate(v: Vector3): Vector3 {
        val ww = w * w; val xx = x * x; val yy = y * y; val zz = z * z
        val wx = w * x; val wy = w * y; val wz = w * z
        val xy = x * y; val xz = x * z; val yz = y * z
        return Vector3(
            x = (ww + xx - yy - zz) * v.x + 2.0 * (xy - wz) * v.y + 2.0 * (xz + wy) * v.z,
            y = 2.0 * (xy + wz) * v.x + (ww - xx + yy - zz) * v.y + 2.0 * (yz - wx) * v.z,
            z = 2.0 * (xz - wy) * v.x + 2.0 * (yz + wx) * v.y + (ww - xx - yy + zz) * v.z
        )
    }

    companion object {
        val IDENTITY = Quaternion(1.0, 0.0, 0.0, 0.0)
    }
}
```

- [ ] **Step 3: Write `ImuSample.kt`**

```kotlin
package com.mustafan4x.baselinems.dsp

/**
 * One IMU sample produced by the Sensor Integration Engineer's signals layer (Phase 4).
 *
 * Coordinate frames are device frame: x to the right edge of the phone, y to the top edge,
 * z out of the screen. Phase 4 will document orientation when the phone is in the front pocket;
 * the DSP module operates on whatever frame it is fed and uses the orientation filter to
 * reconstruct the world frame.
 */
data class ImuSample(
    val timestampNanos: Long,
    val accelerometer: Vector3,
    val gyroscope: Vector3,
    val linearAcceleration: Vector3,
    val rotationVector: Quaternion?
)
```

The fields map directly to Android sensor types: `accelerometer` to `Sensor.TYPE_ACCELEROMETER` (raw, includes gravity); `gyroscope` to `Sensor.TYPE_GYROSCOPE` (rad per second); `linearAcceleration` to `Sensor.TYPE_LINEAR_ACCELERATION` (gravity removed by the platform); `rotationVector` to `Sensor.TYPE_ROTATION_VECTOR` (platform fused quaternion, may be null on devices that do not expose it). The DSP module uses `linearAcceleration` for the main pipeline and uses `accelerometer` plus `gyroscope` for its parallel from scratch Madgwick orientation; the platform `rotationVector` is consumed by the quality score as a residual reference.

- [ ] **Step 4: Add empty `.gitkeep` files so the test package directories are tracked**

```bash
mkdir -p app/src/test/java/com/mustafan4x/baselinems/fixtures
mkdir -p app/src/test/java/com/mustafan4x/baselinems/dsp
touch app/src/test/java/com/mustafan4x/baselinems/fixtures/.gitkeep
touch app/src/test/java/com/mustafan4x/baselinems/dsp/.gitkeep
```

- [ ] **Step 5: Verify the build compiles**

Run: `cd ~/src/BaselineMS && JAVA_HOME=/snap/android-studio/209/jbr ./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/mustafan4x/baselinems/dsp/Vector3.kt \
        app/src/main/java/com/mustafan4x/baselinems/dsp/Quaternion.kt \
        app/src/main/java/com/mustafan4x/baselinems/dsp/ImuSample.kt \
        app/src/test/java/com/mustafan4x/baselinems/fixtures/.gitkeep \
        app/src/test/java/com/mustafan4x/baselinems/dsp/.gitkeep
git commit -m "dsp: introduce Vector3, Quaternion, ImuSample value types"
```

---

## Task 2: Build the parameterized synthetic IMU generator

**Owner:** Test Fixture Engineer.

**Files:**
- Create: `app/src/test/java/com/mustafan4x/baselinems/fixtures/SyntheticImu.kt`
- Create: `app/src/test/java/com/mustafan4x/baselinems/fixtures/SyntheticImuTest.kt`

**Spec:** A parameterized generator that, given gait parameters (cadence, stride length, asymmetry, step time CV, trial duration, noise level), produces a deterministic stream of `ImuSample` records whose ground truth (number of steps, total distance, mean stride length, mean step time, asymmetry index) is derivable directly from the input parameters. The phone is modeled as fixed in the front pocket (constant world-to-device orientation in the absence of leg swing), with three superposed signals: vertical heel-strike spikes at the cadence, lateral sway alternating at half the cadence (one full cycle per stride pair), and forward propulsion as a near sinusoidal acceleration. Gravity is added back into the `accelerometer` field; the `linearAcceleration` field carries the gravity removed signal. Gyroscope is the time derivative of the orientation quaternion. Noise is additive white Gaussian on each axis.

- [ ] **Step 1: Write the generator's spec test first (TDD)**

Create `SyntheticImuTest.kt`:

```kotlin
package com.mustafan4x.baselinems.fixtures

import com.mustafan4x.baselinems.dsp.Vector3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SyntheticImuTest {
    @Test
    fun `generator produces the expected sample count for 30 seconds at 100 Hz`() {
        val samples = SyntheticImu(
            cadenceStepsPerMinute = 100.0,
            strideLengthMeters = 1.40,
            asymmetryRatio = 1.0,
            stepTimeCv = 0.0,
            durationSeconds = 30.0,
            noiseLevelMps2 = 0.0,
            sampleRateHz = 100.0,
            seed = 42L
        ).generate().toList()
        assertEquals(3000, samples.size)
    }

    @Test
    fun `generator produces evenly spaced timestamps`() {
        val samples = SyntheticImu(
            cadenceStepsPerMinute = 100.0,
            strideLengthMeters = 1.40,
            asymmetryRatio = 1.0,
            stepTimeCv = 0.0,
            durationSeconds = 1.0,
            noiseLevelMps2 = 0.0,
            sampleRateHz = 100.0,
            seed = 42L
        ).generate().toList()
        val expectedDeltaNanos = 10_000_000L
        for (i in 1 until samples.size) {
            assertEquals(expectedDeltaNanos, samples[i].timestampNanos - samples[i - 1].timestampNanos)
        }
    }

    @Test
    fun `linearAcceleration plus gravity equals raw accelerometer in the device frame`() {
        val samples = SyntheticImu(
            cadenceStepsPerMinute = 100.0,
            strideLengthMeters = 1.40,
            asymmetryRatio = 1.0,
            stepTimeCv = 0.0,
            durationSeconds = 1.0,
            noiseLevelMps2 = 0.0,
            sampleRateHz = 100.0,
            seed = 42L
        ).generate().toList()
        for (s in samples) {
            val gravity = s.rotationVector!!.rotate(Vector3(0.0, 0.0, 9.80665))
            val reconstructed = s.linearAcceleration + gravity
            assertTrue(abs(reconstructed.x - s.accelerometer.x) < 1e-9)
            assertTrue(abs(reconstructed.y - s.accelerometer.y) < 1e-9)
            assertTrue(abs(reconstructed.z - s.accelerometer.z) < 1e-9)
        }
    }

    @Test
    fun `peak count of vertical linear acceleration matches expected step count`() {
        val cadence = 100.0
        val durationSeconds = 30.0
        val expectedSteps = (cadence * durationSeconds / 60.0).toInt()
        val samples = SyntheticImu(
            cadenceStepsPerMinute = cadence,
            strideLengthMeters = 1.40,
            asymmetryRatio = 1.0,
            stepTimeCv = 0.0,
            durationSeconds = durationSeconds,
            noiseLevelMps2 = 0.0,
            sampleRateHz = 100.0,
            seed = 42L
        ).generate().toList()
        val peaks = countLocalMaxima(samples.map { it.linearAcceleration.z })
        val tolerance = 1
        assertTrue(
            "expected approximately $expectedSteps peaks, got $peaks",
            abs(peaks - expectedSteps) <= tolerance
        )
    }

    private fun countLocalMaxima(series: List<Double>): Int {
        var count = 0
        for (i in 1 until series.size - 1) {
            if (series[i] > series[i - 1] && series[i] > series[i + 1] && series[i] > 0.5) {
                count++
            }
        }
        return count
    }
}
```

- [ ] **Step 2: Run the test to verify it fails because `SyntheticImu` does not exist yet**

Run: `cd ~/src/BaselineMS && JAVA_HOME=/snap/android-studio/209/jbr ./gradlew :app:testDebugUnitTest --tests com.mustafan4x.baselinems.fixtures.SyntheticImuTest`
Expected: compilation failure on the missing `SyntheticImu` class.

- [ ] **Step 3: Implement `SyntheticImu.kt`**

```kotlin
package com.mustafan4x.baselinems.fixtures

import com.mustafan4x.baselinems.dsp.ImuSample
import com.mustafan4x.baselinems.dsp.Quaternion
import com.mustafan4x.baselinems.dsp.Vector3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Parameterized synthetic IMU trace generator.
 *
 * The phone is modeled as fixed in the front pocket with the screen facing the body. The world
 * frame Z axis is up. The device frame at rest is x to the right edge of the phone, y to the
 * top of the phone, z out of the screen. With the phone in the front pocket, the device z
 * axis points forward in the world frame, the device y axis points down, and the device x
 * axis points to the body's right.
 *
 * Three signals are superposed in the world frame:
 *  1. Heel strike spikes on the world Z axis (vertical), one per step, modeled as a narrow
 *     Gaussian centered at the step time with amplitude proportional to gravity.
 *  2. Lateral sway on the world X axis, alternating sign per step (left foot to right foot).
 *  3. Forward propulsion on the world Y axis, modeled as a sinusoid at the step frequency.
 *
 * Asymmetry is implemented by scaling the time between consecutive same foot strides; the
 * asymmetryRatio is the ratio of mean dominant step time to mean non dominant step time.
 *
 * Step time CV is implemented by jittering each step time by a Gaussian with the requested
 * coefficient of variation around the mean step interval.
 *
 * Noise is additive Gaussian on each accelerometer and gyroscope axis at the requested level.
 */
class SyntheticImu(
    private val cadenceStepsPerMinute: Double,
    private val strideLengthMeters: Double,
    private val asymmetryRatio: Double,
    private val stepTimeCv: Double,
    private val durationSeconds: Double,
    private val noiseLevelMps2: Double,
    private val sampleRateHz: Double,
    private val seed: Long
) {
    private val rng = Random(seed)

    fun generate(): Sequence<ImuSample> = sequence {
        val totalSamples = (durationSeconds * sampleRateHz).toInt()
        val sampleDeltaNanos = (1_000_000_000.0 / sampleRateHz).toLong()

        val meanStepIntervalSeconds = 60.0 / cadenceStepsPerMinute
        val stepTimes = computeStepTimes(meanStepIntervalSeconds)

        val orientation = pocketOrientationQuaternion()

        for (i in 0 until totalSamples) {
            val t = i / sampleRateHz
            val world = worldFrameAccelerationAt(t, stepTimes)
            val gravityWorld = Vector3(0.0, 0.0, 9.80665)
            val deviceLinear = orientation.inverse().rotate(world)
            val deviceGravity = orientation.inverse().rotate(gravityWorld)
            val deviceRaw = deviceLinear + deviceGravity
            val gyro = Vector3.ZERO
            val noisyAccel = deviceRaw.addNoise()
            val noisyGyro = gyro.addNoise()
            val noisyLinear = deviceLinear.addNoise()
            yield(
                ImuSample(
                    timestampNanos = i.toLong() * sampleDeltaNanos,
                    accelerometer = noisyAccel,
                    gyroscope = noisyGyro,
                    linearAcceleration = noisyLinear,
                    rotationVector = orientation
                )
            )
        }
    }

    private fun computeStepTimes(meanStepIntervalSeconds: Double): DoubleArray {
        val nSteps = (cadenceStepsPerMinute * durationSeconds / 60.0).toInt()
        val out = DoubleArray(nSteps)
        var t = 0.0
        for (i in 0 until nSteps) {
            val isDominant = i % 2 == 0
            val mean = if (isDominant) meanStepIntervalSeconds * asymmetryRatio
                       else meanStepIntervalSeconds * (2.0 - asymmetryRatio)
            val jitter = if (stepTimeCv > 0.0) gaussian() * stepTimeCv * mean else 0.0
            t += mean + jitter
            out[i] = t
        }
        return out
    }

    private fun worldFrameAccelerationAt(t: Double, stepTimes: DoubleArray): Vector3 {
        val sigma = 0.04
        var az = 0.0
        for (st in stepTimes) {
            val dt = t - st
            az += 5.0 * exp(-(dt * dt) / (2.0 * sigma * sigma))
        }
        val cadenceHz = cadenceStepsPerMinute / 60.0
        val ax = 1.0 * sin(2.0 * PI * cadenceHz * t / 2.0)
        val ay = 0.5 * cos(2.0 * PI * cadenceHz * t)
        return Vector3(ax, ay, az)
    }

    private fun pocketOrientationQuaternion(): Quaternion =
        Quaternion(w = cos(PI / 4.0), x = -sin(PI / 4.0), y = 0.0, z = 0.0)

    private fun gaussian(): Double {
        val u1 = rng.nextDouble().coerceAtLeast(1e-12)
        val u2 = rng.nextDouble()
        return kotlin.math.sqrt(-2.0 * kotlin.math.ln(u1)) * cos(2.0 * PI * u2)
    }

    private fun Vector3.addNoise(): Vector3 {
        if (noiseLevelMps2 <= 0.0) return this
        return Vector3(
            x + gaussian() * noiseLevelMps2,
            y + gaussian() * noiseLevelMps2,
            z + gaussian() * noiseLevelMps2
        )
    }

    private fun Quaternion.inverse(): Quaternion = Quaternion(w, -x, -y, -z).normalized()
}
```

- [ ] **Step 4: Run the tests to verify all four pass**

Run: `cd ~/src/BaselineMS && JAVA_HOME=/snap/android-studio/209/jbr ./gradlew :app:testDebugUnitTest --tests com.mustafan4x.baselinems.fixtures.SyntheticImuTest`
Expected: 4 tests, 0 failures.

If the peak count test fails by more than the tolerance, the issue is either the heel strike Gaussian width (`sigma = 0.04`) producing peaks that merge at high cadence, or the world frame to device frame rotation losing the vertical signal. Investigate before proceeding; do not loosen the tolerance.

- [ ] **Step 5: Commit**

```bash
git add app/src/test/java/com/mustafan4x/baselinems/fixtures/SyntheticImu.kt \
        app/src/test/java/com/mustafan4x/baselinems/fixtures/SyntheticImuTest.kt
git rm app/src/test/java/com/mustafan4x/baselinems/fixtures/.gitkeep
git commit -m "fixtures(gait): parameterized synthetic IMU generator with round trip tests"
```

---

## Task 3: Pre canned fixture set

**Owner:** Test Fixture Engineer.

**Files:**
- Create: `app/src/test/java/com/mustafan4x/baselinems/fixtures/PreCannedFixtures.kt`

**Spec:** A library of named fixture configurations covering the published MS gait reference values and adjacent ranges. Each fixture is a frozen `SyntheticImu` configuration. Per `docs/source/clinical-references.md` and the Clinical Validator's Phase 0 sign off (`docs/source/clinical-references.md` Gait Test sign off): MS reference cadence is 94.4 steps per minute and step length 45.3 cm (Givon et al. 2009); healthy control cadence is 115.2 steps per minute and step length 72.1 cm. Fixtures map MS literature reference values to synthetic generator parameters.

- [ ] **Step 1: Write `PreCannedFixtures.kt`**

```kotlin
package com.mustafan4x.baselinems.fixtures

object PreCannedFixtures {

    fun healthyControlNormal(seed: Long = 1L): SyntheticImu = SyntheticImu(
        cadenceStepsPerMinute = 115.2,
        strideLengthMeters = 1.442,
        asymmetryRatio = 1.0,
        stepTimeCv = 0.03,
        durationSeconds = 30.0,
        noiseLevelMps2 = 0.0,
        sampleRateHz = 100.0,
        seed = seed
    )

    fun msTypicalNormal(seed: Long = 2L): SyntheticImu = SyntheticImu(
        cadenceStepsPerMinute = 94.4,
        strideLengthMeters = 0.906,
        asymmetryRatio = 1.0,
        stepTimeCv = 0.05,
        durationSeconds = 30.0,
        noiseLevelMps2 = 0.0,
        sampleRateHz = 100.0,
        seed = seed
    )

    fun slowWalk(seed: Long = 3L): SyntheticImu = SyntheticImu(
        cadenceStepsPerMinute = 80.0,
        strideLengthMeters = 0.85,
        asymmetryRatio = 1.0,
        stepTimeCv = 0.06,
        durationSeconds = 30.0,
        noiseLevelMps2 = 0.0,
        sampleRateHz = 100.0,
        seed = seed
    )

    fun briskWalk(seed: Long = 4L): SyntheticImu = SyntheticImu(
        cadenceStepsPerMinute = 130.0,
        strideLengthMeters = 1.55,
        asymmetryRatio = 1.0,
        stepTimeCv = 0.025,
        durationSeconds = 30.0,
        noiseLevelMps2 = 0.0,
        sampleRateHz = 100.0,
        seed = seed
    )

    fun mildAsymmetry(seed: Long = 5L): SyntheticImu = SyntheticImu(
        cadenceStepsPerMinute = 100.0,
        strideLengthMeters = 1.30,
        asymmetryRatio = 1.10,
        stepTimeCv = 0.04,
        durationSeconds = 30.0,
        noiseLevelMps2 = 0.0,
        sampleRateHz = 100.0,
        seed = seed
    )

    fun severeAsymmetry(seed: Long = 6L): SyntheticImu = SyntheticImu(
        cadenceStepsPerMinute = 90.0,
        strideLengthMeters = 1.05,
        asymmetryRatio = 1.30,
        stepTimeCv = 0.08,
        durationSeconds = 30.0,
        noiseLevelMps2 = 0.0,
        sampleRateHz = 100.0,
        seed = seed
    )

    fun noisyMsNormal(seed: Long = 7L): SyntheticImu = SyntheticImu(
        cadenceStepsPerMinute = 94.4,
        strideLengthMeters = 0.906,
        asymmetryRatio = 1.0,
        stepTimeCv = 0.05,
        durationSeconds = 30.0,
        noiseLevelMps2 = 0.5,
        sampleRateHz = 100.0,
        seed = seed
    )
}
```

The cadence numbers are the Givon et al. 2009 values (cited in `docs/source/clinical-references.md` and `SPEC.md` Section 7.1.1). Step length to stride length conversion uses `stride = 2 * step` since a stride is two steps in the gait literature; Givon 2009 reports step length 45.3 cm for MS and 72.1 cm for healthy controls, so MS stride length is 0.906 m and healthy control stride length is 1.442 m. The slow and brisk walks bracket the published MS to healthy control range. The mild and severe asymmetries are smartphone adaptation choices not from a single MS paper; the asymmetry ratio of 1.10 means the dominant foot's mean step time is 10 percent longer than the non dominant foot's, and 1.30 is the high end the project plausibly sees.

- [ ] **Step 2: Verify the fixtures compile and the existing `SyntheticImuTest` still passes**

Run: `cd ~/src/BaselineMS && JAVA_HOME=/snap/android-studio/209/jbr ./gradlew :app:testDebugUnitTest --tests "com.mustafan4x.baselinems.fixtures.*"`
Expected: 4 tests, 0 failures.

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/mustafan4x/baselinems/fixtures/PreCannedFixtures.kt
git commit -m "fixtures(gait): pre canned configurations from Givon 2009 MS reference values"
```

---

## Task 4: Document the fixtures in `docs/qa/fixtures.md`

**Owner:** Test Fixture Engineer.

**Files:**
- Create: `docs/qa/fixtures.md`

**Spec:** A document that names every pre canned fixture, lists its parameters, names the ground truth derivable from the parameters (number of steps, mean stride length, asymmetry index, mean step time, expected variability), and cites the published reference range that justifies each parameter choice.

- [ ] **Step 1: Write `docs/qa/fixtures.md`**

Include at minimum: a header explaining the fixture library's purpose, a per fixture entry with parameters and ground truth, and a section linking parameter choices to clinical references. The Test Fixture Engineer is the appropriate author for the prose; the PM does not pre write the body.

The minimum sections the document must have:
1. Purpose and scope.
2. The parameterized generator API summary (cadence, stride length, asymmetry ratio, step time CV, duration, noise, sample rate, seed).
3. One table or one section per pre canned fixture, with: name, parameters, ground truth values, citation for parameter choice, realism caveats.
4. A "How to add a new fixture" subsection so future phases can extend the library.
5. A "Known limitations" section listing what the synthetic generator does not model (turn dynamics, surface variation, device specific noise spectra) so that the Phase 5 reviewer can reconcile against real recordings without re reading this plan.

- [ ] **Step 2: Commit**

```bash
git add docs/qa/fixtures.md
git commit -m "docs(qa): document the synthetic IMU fixture library"
```

---

## Task 5: TFE hand off and SPE kickoff

**Owner:** PM (administrative). The TFE returns to the PM with the fixture library and `docs/qa/fixtures.md`. The PM dispatches the SPE. The SPE reads the fixture library, the SPEC.md Section 7, and `docs/source/clinical-references.md` smartphone gait section before writing any code.

- [ ] **Step 1: PM verifies all four TFE commits landed**

Run: `cd ~/src/BaselineMS && git log --oneline -5`
Expected: the most recent four commits are TFE deliverables (`fixtures(gait): ...` and `docs(qa): ...`).

- [ ] **Step 2: PM verifies the test suite is green**

Run: `cd ~/src/BaselineMS && JAVA_HOME=/snap/android-studio/209/jbr ./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL with 0 failures.

- [ ] **Step 3: PM dispatches the SPE with the standard dispatch template (CLAUDE.md). The SPE works through Tasks 6 to 14 below.**

---

## Task 6: 4th order zero phase Butterworth low pass filter

**Owner:** Signal Processing Engineer.

**Files:**
- Create: `app/src/main/java/com/mustafan4x/baselinems/dsp/ButterworthLowPass.kt`
- Create: `app/src/test/java/com/mustafan4x/baselinems/dsp/ButterworthLowPassTest.kt`

**Spec:** A 4th order Butterworth low pass filter with a 20 Hz cutoff at 100 Hz sample rate, applied as a forward then reverse pass to achieve zero phase distortion (filtfilt). The forward pass uses a stateful biquad in direct form II transposed; the reverse pass mirrors the operation on the buffered output. The implementation is allocation free in the per sample callback path (the user supplies a `DoubleArray` and the filter mutates it in place during the reverse pass; the forward pass uses fixed-size mutable state).

- [ ] **Step 1: Write the failing test**

```kotlin
package com.mustafan4x.baselinems.dsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class ButterworthLowPassTest {
    @Test
    fun `passes a 1 Hz tone with near unity amplitude`() {
        val fs = 100.0
        val n = 1000
        val signal = DoubleArray(n) { i -> sin(2.0 * PI * 1.0 * i / fs) }
        val filtered = ButterworthLowPass(cutoffHz = 20.0, sampleRateHz = fs).filtfilt(signal)
        val rmsIn = rms(signal)
        val rmsOut = rms(filtered)
        assertTrue(abs(rmsOut / rmsIn - 1.0) < 0.05)
    }

    @Test
    fun `attenuates a 40 Hz tone by at least 12 dB`() {
        val fs = 100.0
        val n = 1000
        val signal = DoubleArray(n) { i -> sin(2.0 * PI * 40.0 * i / fs) }
        val filtered = ButterworthLowPass(cutoffHz = 20.0, sampleRateHz = fs).filtfilt(signal)
        val rmsIn = rms(signal)
        val rmsOut = rms(filtered)
        val ratio = rmsOut / rmsIn
        assertTrue("ratio = $ratio", ratio < 0.25)
    }

    @Test
    fun `is approximately zero phase on a delta input`() {
        val n = 200
        val signal = DoubleArray(n).also { it[100] = 1.0 }
        val filtered = ButterworthLowPass(cutoffHz = 20.0, sampleRateHz = 100.0).filtfilt(signal)
        val argMax = filtered.indices.maxByOrNull { filtered[it] } ?: -1
        assertEquals(100, argMax)
    }

    private fun rms(x: DoubleArray): Double {
        var s = 0.0
        for (v in x) s += v * v
        return kotlin.math.sqrt(s / x.size)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `cd ~/src/BaselineMS && JAVA_HOME=/snap/android-studio/209/jbr ./gradlew :app:testDebugUnitTest --tests com.mustafan4x.baselinems.dsp.ButterworthLowPassTest`
Expected: compilation failure on missing `ButterworthLowPass`.

- [ ] **Step 3: Implement `ButterworthLowPass.kt`**

The 4th order is implemented as two cascaded biquads. The biquad coefficients for a Butterworth low pass at a given normalized cutoff are derived from the bilinear transform of the analog prototype. The SPE may either compute the coefficients in the constructor from the cutoff and sample rate, or hard code coefficients for the 20 Hz at 100 Hz case and reject any other configuration. Because Phase 4 capture runs at 100 Hz (with possible jitter), and the cutoff is fixed at 20 Hz per `SPEC.md` Section 7.1 step 4, hard coded coefficients are acceptable and simplify the implementation; the constructor still accepts cutoff and sample rate to keep the test parameterizable.

The SPE writes the implementation. The plan does not pre commit the SPE to a specific coefficient derivation strategy. The Code Reviewer's Task 17 review confirms the choice is justifiable.

- [ ] **Step 4: Run the test to verify it passes**

Run: `cd ~/src/BaselineMS && JAVA_HOME=/snap/android-studio/209/jbr ./gradlew :app:testDebugUnitTest --tests com.mustafan4x.baselinems.dsp.ButterworthLowPassTest`
Expected: 3 tests, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mustafan4x/baselinems/dsp/ButterworthLowPass.kt \
        app/src/test/java/com/mustafan4x/baselinems/dsp/ButterworthLowPassTest.kt
git commit -m "dsp(gait): 4th order zero phase Butterworth low pass at 20 Hz"
```

---

## Task 7: Madgwick orientation filter

**Owner:** Signal Processing Engineer.

**Files:**
- Create: `app/src/main/java/com/mustafan4x/baselinems/dsp/Madgwick.kt`
- Create: `app/src/test/java/com/mustafan4x/baselinems/dsp/MadgwickTest.kt`

**Spec:** A Madgwick (2010) orientation filter that consumes per sample gyroscope (rad per second) and accelerometer (m per second squared) and outputs an estimated world to device quaternion. The filter is stateful per instance: the quaternion field is initialized to identity and updated on each `update(gyro, accel, dtSeconds)` call. The default beta gain is 0.1, configurable per instance. The filter is allocation free per update.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.mustafan4x.baselinems.dsp

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class MadgwickTest {
    @Test
    fun `static gravity sample converges to upright orientation`() {
        val m = Madgwick(beta = 0.5)
        repeat(2000) {
            m.update(
                gyro = Vector3.ZERO,
                accel = Vector3(0.0, 0.0, 9.80665),
                dtSeconds = 0.01
            )
        }
        val q = m.orientation()
        val zUp = q.rotate(Vector3(0.0, 0.0, 1.0))
        assertTrue(abs(zUp.x) < 0.05)
        assertTrue(abs(zUp.y) < 0.05)
        assertTrue(abs(zUp.z - 1.0) < 0.05)
    }

    @Test
    fun `pure rotation about world Z is reflected in the orientation`() {
        val m = Madgwick(beta = 0.0)
        val omega = 1.0
        val dt = 0.01
        repeat(((2.0 * kotlin.math.PI / omega) / dt).toInt()) {
            m.update(
                gyro = Vector3(0.0, 0.0, omega),
                accel = Vector3(0.0, 0.0, 9.80665),
                dtSeconds = dt
            )
        }
        val q = m.orientation()
        val toleranceDegrees = 5.0
        val angleDegrees = 2.0 * kotlin.math.acos(abs(q.w)) * 180.0 / kotlin.math.PI
        assertTrue("residual angle $angleDegrees", angleDegrees < toleranceDegrees)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

- [ ] **Step 3: Implement `Madgwick.kt`**

The standard Madgwick (2010) IMU update rule is in the SPE's role brief and in the published paper. The implementation is approximately 60 lines of Kotlin: a quaternion field, a beta scalar, and an `update` method that computes the gyro-derived rate of change, the gradient correction from the accelerometer, and integrates with the supplied dt.

- [ ] **Step 4: Run the tests to verify they pass**

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mustafan4x/baselinems/dsp/Madgwick.kt \
        app/src/test/java/com/mustafan4x/baselinems/dsp/MadgwickTest.kt
git commit -m "dsp(gait): Madgwick orientation filter from scratch"
```

---

## Task 8: World frame transform and gravity removal sanity check

**Owner:** Signal Processing Engineer.

**Files:**
- Create: `app/src/main/java/com/mustafan4x/baselinems/dsp/WorldFrame.kt`
- Create: `app/src/test/java/com/mustafan4x/baselinems/dsp/WorldFrameTest.kt`

**Spec:** A pure function that, given a per sample orientation quaternion (from Task 7's Madgwick or from `ImuSample.rotationVector` if present) and the per sample linear acceleration in the device frame, returns the linear acceleration in the world frame (Z up). When the project consumes `ImuSample.linearAcceleration` directly (which is already gravity removed by the platform), the world frame transform is `q.rotate(linearAcceleration)`. The module additionally exposes a sanity check helper that recovers an estimate of the gravity vector from a static window of raw accelerometer samples; this is used in Task 13 quality scoring to confirm orientation tracking did not drift.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.mustafan4x.baselinems.dsp

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class WorldFrameTest {
    @Test
    fun `device frame vertical of plus z transforms to world frame z when device is upright`() {
        val deviceVerticalLinear = Vector3(0.0, 0.0, 1.0)
        val upright = Quaternion.IDENTITY
        val world = WorldFrame.toWorld(upright, deviceVerticalLinear)
        assertTrue(abs(world.z - 1.0) < 1e-9)
        assertTrue(abs(world.x) < 1e-9)
        assertTrue(abs(world.y) < 1e-9)
    }

    @Test
    fun `gravity estimate from a static window approximates 9_8 mps2 magnitude`() {
        val staticWindow = (0 until 100).map { Vector3(0.0, 0.0, 9.80665) }
        val g = WorldFrame.estimateGravity(staticWindow)
        assertTrue(abs(g.magnitude() - 9.80665) < 0.05)
    }
}
```

- [ ] **Step 2 to 5:** TDD per the Task 6 pattern. Implement, verify, commit.

```bash
git add app/src/main/java/com/mustafan4x/baselinems/dsp/WorldFrame.kt \
        app/src/test/java/com/mustafan4x/baselinems/dsp/WorldFrameTest.kt
git commit -m "dsp(gait): world frame transform and gravity sanity check"
```

---

## Task 9: Step detection (peak finder)

**Owner:** Signal Processing Engineer.

**Files:**
- Create: `app/src/main/java/com/mustafan4x/baselinems/dsp/StepDetector.kt`
- Create: `app/src/test/java/com/mustafan4x/baselinems/dsp/StepDetectorTest.kt`

**Spec:** A peak finder over the world frame vertical (Z) component of linear acceleration after Butterworth low pass. Returns a list of step events with timestamp (in seconds) and peak amplitude. Constraints from `SPEC.md` Section 7.1 step 5: minimum peak prominence (a positive threshold the SPE chooses, defaults to 1.0 m per second squared), minimum inter peak distance 250 ms, maximum inter peak distance 800 ms (peaks separated by more than 800 ms are still emitted but the inter peak gap is recorded so the quality score can downgrade the trial).

- [ ] **Step 1: Write the failing test using a `PreCannedFixtures.healthyControlNormal()` trace**

```kotlin
package com.mustafan4x.baselinems.dsp

import com.mustafan4x.baselinems.fixtures.PreCannedFixtures
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class StepDetectorTest {
    @Test
    fun `detects approximately the expected number of steps in a healthy control normal walk`() {
        val samples = PreCannedFixtures.healthyControlNormal().generate().toList()
        val verticalWorld = samples.map { it.linearAcceleration.z }
        val filtered = ButterworthLowPass(cutoffHz = 20.0, sampleRateHz = 100.0)
            .filtfilt(verticalWorld.toDoubleArray())
        val timestamps = samples.map { it.timestampNanos / 1_000_000_000.0 }
        val steps = StepDetector(
            minPeakProminence = 1.0,
            minIntervalSeconds = 0.25,
            maxIntervalSeconds = 0.80
        ).detect(filtered, timestamps.toDoubleArray())
        val expected = (115.2 * 30.0 / 60.0).toInt()
        assertTrue("expected approximately $expected steps, got ${steps.size}", abs(steps.size - expected) <= 2)
    }

    @Test
    fun `respects the minimum inter peak distance`() {
        val signal = DoubleArray(1000)
        for (i in signal.indices) {
            if (i % 5 == 0) signal[i] = 5.0
        }
        val timestamps = DoubleArray(1000) { it * 0.01 }
        val steps = StepDetector(
            minPeakProminence = 1.0,
            minIntervalSeconds = 0.25,
            maxIntervalSeconds = 0.80
        ).detect(signal, timestamps)
        for (i in 1 until steps.size) {
            assertTrue(steps[i].timeSeconds - steps[i - 1].timeSeconds >= 0.25 - 1e-9)
        }
    }
}
```

- [ ] **Step 2 to 5:** TDD. Implement, verify, commit.

```bash
git add app/src/main/java/com/mustafan4x/baselinems/dsp/StepDetector.kt \
        app/src/test/java/com/mustafan4x/baselinems/dsp/StepDetectorTest.kt
git commit -m "dsp(gait): peak finder step detector with prominence and interval constraints"
```

---

## Task 10: Stride pairing (left versus right)

**Owner:** Signal Processing Engineer.

**Files:**
- Create: `app/src/main/java/com/mustafan4x/baselinems/dsp/StridePairing.kt`
- Create: `app/src/test/java/com/mustafan4x/baselinems/dsp/StridePairingTest.kt`

**Spec:** Given a list of step events plus the world frame lateral (X) acceleration sampled at the step times, assign each step to "left" or "right" based on the sign of the lateral acceleration at the step. Group successive same foot steps into strides. Returns a list of strides with foot label, start time, end time, and the lateral acceleration sign at start. The left or right convention is arbitrary at the API level; the integration test in Task 13 confirms alternating assignment on the synthetic generator.

- [ ] **Steps 1 to 5:** TDD per the Task 6 pattern.

```bash
git add app/src/main/java/com/mustafan4x/baselinems/dsp/StridePairing.kt \
        app/src/test/java/com/mustafan4x/baselinems/dsp/StridePairingTest.kt
git commit -m "dsp(gait): left right stride pairing from lateral acceleration sign"
```

---

## Task 11: ZUPT stride length integration

**Owner:** Signal Processing Engineer.

**Files:**
- Create: `app/src/main/java/com/mustafan4x/baselinems/dsp/Zupt.kt`
- Create: `app/src/test/java/com/mustafan4x/baselinems/dsp/ZuptTest.kt`

**Spec:** Zero velocity update integrator. Given world frame linear acceleration time series and a list of mid stance instants (local minima of acceleration magnitude near each step time), compute stride length as the integral of forward (Y) acceleration between successive mid stance instants of the same foot, with velocity reset to zero at each mid stance to bound integration drift. Returns one stride length per stride. Per `SPEC.md` Section 7.2 target 1: stride length recovered within 2 percent on clean synthetic signals.

- [ ] **Step 1: Write the failing test using `PreCannedFixtures.healthyControlNormal()`**

The expected stride length is the input parameter (1.442 m). The test asserts the recovered mean stride length is within 2 percent of 1.442 m.

```kotlin
package com.mustafan4x.baselinems.dsp

import com.mustafan4x.baselinems.fixtures.PreCannedFixtures
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ZuptTest {
    @Test
    fun `recovers stride length within 2 percent on a healthy control normal walk`() {
        val expected = 1.442
        val recovered = pipelineRecoveredMeanStrideLength(PreCannedFixtures.healthyControlNormal())
        val percentError = 100.0 * abs(recovered - expected) / expected
        assertTrue("percent error $percentError exceeded 2", percentError <= 2.0)
    }

    private fun pipelineRecoveredMeanStrideLength(fixture: com.mustafan4x.baselinems.fixtures.SyntheticImu): Double {
        TODO("Wire through StepDetector + StridePairing + Zupt as in GaitPipeline once that exists; see Task 13")
    }
}
```

The `pipelineRecoveredMeanStrideLength` helper is a placeholder until Task 13 lands. The SPE writes the integration test against `GaitPipeline` directly in Task 13; this Task 11 test writes a more local Zupt test that uses a hand constructed mid stance index list rather than going through the full pipeline. The SPE picks the form that exercises Zupt's integration logic without depending on `StepDetector` or `StridePairing`.

- [ ] **Step 2 to 5:** TDD. Implement, verify, commit.

```bash
git add app/src/main/java/com/mustafan4x/baselinems/dsp/Zupt.kt \
        app/src/test/java/com/mustafan4x/baselinems/dsp/ZuptTest.kt
git commit -m "dsp(gait): ZUPT stride length integrator with mid stance velocity reset"
```

---

## Task 12: Feature extractor

**Owner:** Signal Processing Engineer.

**Files:**
- Create: `app/src/main/java/com/mustafan4x/baselinems/dsp/FeatureExtractor.kt`
- Create: `app/src/main/java/com/mustafan4x/baselinems/dsp/GaitFeatures.kt`
- Create: `app/src/test/java/com/mustafan4x/baselinems/dsp/FeatureExtractorTest.kt`

**Spec:** Given the outputs of Step Detector, Stride Pairing, and Zupt, compute the five gait features named in `SPEC.md` Section 7.1 step 8 plus the quality score named in step 9.

`GaitFeatures.kt`:

```kotlin
package com.mustafan4x.baselinems.dsp

data class GaitFeatures(
    val cadenceStepsPerMinute: Double,
    val meanStrideLengthMeters: Double,
    val stepTimeCv: Double,
    val strideAsymmetryIndex: Double,
    val doubleSupportTimeSeconds: Double,
    val qualityScore: Double,
    val detectedStepCount: Int
) {
    fun toMap(): Map<String, Double> = mapOf(
        "cadence_steps_per_minute" to cadenceStepsPerMinute,
        "mean_stride_length_meters" to meanStrideLengthMeters,
        "step_time_cv" to stepTimeCv,
        "stride_asymmetry_index" to strideAsymmetryIndex,
        "double_support_time_seconds" to doubleSupportTimeSeconds,
        "quality_score" to qualityScore,
        "detected_step_count" to detectedStepCount.toDouble()
    )
}
```

The asymmetry index follows the same symmetric mean denominator convention the Tap Test uses (`(left - right) / mean(left, right)`), keeping the convention consistent across modules. The quality score combines four factors per `SPEC.md` Section 7.1 step 9: at least 20 steps detected, orientation residual stable, trajectory approximately straight (low cumulative yaw drift), and no inter peak gap exceeded 800 ms. The SPE chooses the exact weighting; the Code Reviewer's Task 17 review confirms reasonableness.

Tests cover: cadence math, stride length math, step time CV math, asymmetry math (including the symmetric mean denominator convention), double support time math (using a hand constructed phase relationship), quality score floor (zero when fewer than 20 steps detected), and quality score ceiling (one on a clean synthetic walk).

- [ ] **Steps 1 to 5:** TDD.

```bash
git add app/src/main/java/com/mustafan4x/baselinems/dsp/FeatureExtractor.kt \
        app/src/main/java/com/mustafan4x/baselinems/dsp/GaitFeatures.kt \
        app/src/test/java/com/mustafan4x/baselinems/dsp/FeatureExtractorTest.kt
git commit -m "dsp(gait): feature extractor for cadence, stride length, CV, asymmetry, DST"
```

---

## Task 13: `GaitPipeline` end to end integration

**Owner:** Signal Processing Engineer.

**Files:**
- Create: `app/src/main/java/com/mustafan4x/baselinems/dsp/GaitPipeline.kt`
- Create: `app/src/test/java/com/mustafan4x/baselinems/dsp/GaitPipelineIntegrationTest.kt`

**Spec:** The public entry point of the DSP module. Composes Butterworth low pass, Madgwick (with the platform `rotationVector` consumed as a parallel reference for the quality score residual), world frame transform, step detection, stride pairing, ZUPT, and feature extraction. Accepts a `List<ImuSample>` input (collected over a 30 second window by Phase 4) and returns `GaitFeatures`. The pipeline is a class with a single `process(samples: List<ImuSample>): GaitFeatures` method; per stage state is allocated once per pipeline instance, not per sample.

The integration test covers every pre canned fixture from Task 3:

```kotlin
package com.mustafan4x.baselinems.dsp

import com.mustafan4x.baselinems.fixtures.PreCannedFixtures
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class GaitPipelineIntegrationTest {
    private val pipeline = GaitPipeline()

    @Test
    fun `healthy control normal recovers cadence within 3 percent and stride length within 2 percent`() {
        val samples = PreCannedFixtures.healthyControlNormal().generate().toList()
        val f = pipeline.process(samples)
        assertWithin(115.2, f.cadenceStepsPerMinute, 0.03)
        assertWithin(1.442, f.meanStrideLengthMeters, 0.02)
        assertTrue(f.qualityScore > 0.8)
    }

    @Test
    fun `MS typical normal recovers within the same envelope`() {
        val samples = PreCannedFixtures.msTypicalNormal().generate().toList()
        val f = pipeline.process(samples)
        assertWithin(94.4, f.cadenceStepsPerMinute, 0.03)
        assertWithin(0.906, f.meanStrideLengthMeters, 0.02)
        assertTrue(f.qualityScore > 0.7)
    }

    @Test
    fun `mild asymmetry recovers a positive asymmetry index of about 0_1`() {
        val samples = PreCannedFixtures.mildAsymmetry().generate().toList()
        val f = pipeline.process(samples)
        val expected = 0.10
        assertTrue("asymmetry $f.strideAsymmetryIndex", abs(f.strideAsymmetryIndex - expected) < 0.04)
    }

    @Test
    fun `noisy MS normal still recovers cadence within 5 percent and stride length within 4 percent`() {
        val samples = PreCannedFixtures.noisyMsNormal().generate().toList()
        val f = pipeline.process(samples)
        assertWithin(94.4, f.cadenceStepsPerMinute, 0.05)
        assertWithin(0.906, f.meanStrideLengthMeters, 0.04)
    }

    private fun assertWithin(expected: Double, actual: Double, fraction: Double) {
        val tolerance = expected * fraction
        assertTrue(
            "expected $expected within ${fraction * 100}%, got $actual (delta ${abs(actual - expected)}, tolerance $tolerance)",
            abs(actual - expected) <= tolerance
        )
    }
}
```

The 2 percent stride length target is the `SPEC.md` Section 7.2 layer 1 target. The 3 percent cadence target tracks `SPEC.md` Section 7.2 layer 2 (the real walking course target is 3 percent against measured ground truth, and the synthetic ground truth should be at least as good). The noisy fixture's looser tolerances reflect the realism penalty noise introduces; the SPE may tighten these as the implementation matures.

- [ ] **Step 1 to 5:** TDD. The failing form of this test is the SPE's red bar; the implementation is then iterated until every fixture's recovery passes.

```bash
git add app/src/main/java/com/mustafan4x/baselinems/dsp/GaitPipeline.kt \
        app/src/test/java/com/mustafan4x/baselinems/dsp/GaitPipelineIntegrationTest.kt
git commit -m "dsp(gait): end to end GaitPipeline with integration tests across pre canned fixtures"
```

---

## Task 14: ADR for the from scratch Madgwick decision

**Owner:** Signal Processing Engineer.

**Files:**
- Create: `docs/adr/0002-madgwick-from-scratch.md`

**Spec:** A short architecture decision record (between 100 and 200 lines is plenty) that names the choice (roll our own Madgwick filter rather than depending on a third party library), the alternatives considered (the most defensible alternatives are: depend on the open source Java port at github.com/xioTechnologies/Open-Source-AHRS-With-x-IMU; depend on Android's `TYPE_ROTATION_VECTOR` exclusively without writing the parallel filter), the rationale (`SPEC.md` Section 7.1 step 2 explicitly motivates a teaching exercise; the parallel implementation also feeds the quality score residual against the platform fused estimate; the from scratch implementation has a small dependency footprint), the trade offs, and the success criteria for revisiting the decision. The ADR follows `docs/adr/0001-android-native-platform.md`'s format if that file exists; otherwise the SPE writes a short ADR with the standard fields (Status, Context, Decision, Consequences, Alternatives Considered).

- [ ] **Step 1: SPE writes the ADR**

- [ ] **Step 2: Commit**

```bash
git add docs/adr/0002-madgwick-from-scratch.md
git commit -m "docs(adr): 0002 Madgwick from scratch over library dependency"
```

---

## Task 15: Performance Engineer review

**Owner:** Performance Engineer.

**Files:**
- Modify: `docs/perf/latency-budgets.md` (append Phase 3 entry)

**Spec:** The Performance Engineer reviews the DSP package for allocation free hot paths, confirms the per sample state is mutable and reused, and notes any allocation that escapes the hot path. The review is a Phase 3 reviewer task per `agents/11-performance-engineer.md` Phase 3 section. The performance budget from `docs/perf/latency-budgets.md` (sample rate within 5 percent of nominal 100 Hz, jitter under 5 ms p99) is for Phase 4 measurement against a real device; in Phase 3 the Performance Engineer's job is to confirm the pure Kotlin DSP code has no obvious garbage producers.

- [ ] **Step 1: Performance Engineer dispatched by PM after Task 13 commit lands**

- [ ] **Step 2: Performance Engineer reads the seven new DSP files plus `GaitPipeline.kt`**

- [ ] **Step 3: Performance Engineer files findings as a markdown comment block appended to `docs/perf/latency-budgets.md` under a new "Phase 3 review" subsection**

- [ ] **Step 4: If findings are non blocking, the PM moves to Task 16. If a finding blocks (allocation in the per sample callback), the PM dispatches the SPE for a fix commit.**

- [ ] **Step 5: Commit**

```bash
git add docs/perf/latency-budgets.md
git commit -m "perf(gait): Phase 3 allocation review of dsp package"
```

---

## Task 16: Citation Auditor pass on cited DSP references

**Owner:** Citation Auditor.

**Files:**
- Modify: `docs/source/citation-audit-log.md` (append Phase 3 entries)

**Spec:** The Citation Auditor verifies every DSP method citation that the Phase 3 deliverables introduce or reference: Madgwick 2010 in the ADR (Task 14) and the SPEC.md Section 7.1 step 2 reference; Skog et al. 2010 in any Zupt implementation comments or in the ADR; Bea T et al. 2025 if the SPE cites the SEM range in the ADR or in any user facing copy added in Phase 3 (Phase 3 should not yet have user facing copy, but the auditor confirms). The auditor does not introduce new claims; the auditor's job is to verify the SPE has not drifted from `docs/source/clinical-references.md` in any new prose. Verdict per audit, recorded in `docs/source/citation-audit-log.md`.

- [ ] **Step 1: Citation Auditor dispatched by PM after Task 14 commit lands**

- [ ] **Step 2: Citation Auditor reviews the new ADR plus any Phase 3 prose added under `docs/`**

- [ ] **Step 3: Citation Auditor appends Phase 3 entries to `docs/source/citation-audit-log.md`**

- [ ] **Step 4: Commit**

```bash
git add docs/source/citation-audit-log.md
git commit -m "docs(citations): Phase 3 Citation Auditor pass on DSP references"
```

---

## Task 17: Code Reviewer pass

**Owner:** Code Reviewer.

**Files:**
- Create: `docs/qa/code-review-phase-3.md`

**Spec:** A phase level review that uses the `code-review:code-review` skill and follows the precedent set by `docs/qa/code-review-phase-2.md`. The reviewer reads every file changed by Phase 3, runs `./gradlew :app:testDebugUnitTest`, and produces a verdict (APPROVED, APPROVED WITH MINOR CHANGES, or REJECTED) with concrete findings. Verdict landed in `docs/qa/code-review-phase-3.md`.

- [ ] **Step 1: Dispatched by PM after Task 16**

- [ ] **Step 2: Reviewer produces verdict file**

- [ ] **Step 3: If APPROVED WITH MINOR CHANGES, the PM dispatches the SPE for any fix commits. The fix commits land before Task 18.**

- [ ] **Step 4: Commit**

```bash
git add docs/qa/code-review-phase-3.md
git commit -m "review(gait): Code Reviewer Phase 3 verdict"
```

---

## Task 18: QA Engineer regression checklist and phase sign off

**Owner:** QA Engineer.

**Files:**
- Modify: `docs/qa/regression-checklist.md` (append Phase 3 section)

**Spec:** The QA Engineer adds a Phase 3 regression checklist entry and runs the full test suite a final time. Per `agents/09-qa-engineer.md` and the precedent of `docs/qa/regression-checklist.md` Phase 2 section, the checklist names the falsifiable test conditions a future change to the DSP package must not regress: every fixture in `PreCannedFixtures` recovers cadence within its phase tolerance and stride length within its phase tolerance; `ButterworthLowPassTest`, `MadgwickTest`, `WorldFrameTest`, `StepDetectorTest`, `StridePairingTest`, `ZuptTest`, `FeatureExtractorTest`, and `GaitPipelineIntegrationTest` all pass; the test suite total grows from 48 (Phase 2 close) to roughly 60 plus tests (Phase 3 close) with zero failures.

- [ ] **Step 1: QA Engineer dispatched by PM after Task 17**

- [ ] **Step 2: QA Engineer runs `./gradlew :app:testDebugUnitTest` and records the count plus any skipped or ignored tests**

- [ ] **Step 3: QA Engineer appends the Phase 3 checklist to `docs/qa/regression-checklist.md`**

- [ ] **Step 4: QA Engineer issues a sign off (PASS, FAIL, or PASS WITH NOTES) at the bottom of the appended section**

- [ ] **Step 5: Commit**

```bash
git add docs/qa/regression-checklist.md
git commit -m "qa(gait): Phase 3 regression checklist and sign off"
```

---

## Phase close

After Task 18 lands the PM:

1. Updates `STATUS.md` row 3 from `in progress` to `completed`, fills in the date and the actual window cost.
2. Moves the "Next phase" line at the top of `STATUS.md` to Phase 4.
3. Writes a Resume notes block summarizing Phase 3 deliverables, the test count (expected ~60 plus), and any items deferred to Phase 4 or Phase 11.
4. Runs the mandatory check-in protocol before Phase 4 begins. No Phase 4 dispatch happens without the user answering the three check-in questions.

The DSP module from Phase 3 is consumed by Phase 4: the Sensor Integration Engineer wires `SensorManager` to a `Flow<ImuSample>`, the Android Engineer builds the gait test screen and orchestrator wiring, and the Signal Processing Engineer reviews the integration to confirm samples reach the DSP module unmolested.

---

## Self review

The PM ran the writing-plans skill self review on this plan before dispatching:

1. **Spec coverage.** Every bullet in `SPEC.md` Section 7.1 maps to a task: capture (Phase 4, out of scope here); orientation estimation (Task 7); gravity removal (Task 8); low pass (Task 6); step detection (Task 9); stride pairing (Task 10); ZUPT stride length (Task 11); feature extraction (Task 12); quality score (Task 12). Section 7.2 layer 1 (synthetic ground truth, stride length within 2 percent) is the Task 13 integration test target. Section 7.2 layers 2 and 3 are Phase 5 work, called out explicitly here. The Madgwick from scratch teaching choice is Task 14's ADR.
2. **Placeholder scan.** Tasks 8 to 12 collapse the inner TDD steps into a "TDD per the Task 6 pattern" reference. This is intentional: the SPE is a senior specialist who follows the same pattern. The first task (Task 6) is fully expanded; subsequent tasks reference it. The commit messages, file paths, and test specs are all explicit. Task 11's `pipelineRecoveredMeanStrideLength` helper is flagged as a placeholder and the SPE is told to either replace it with a local Zupt only test or leave the integration test for Task 13; either is acceptable.
3. **Type consistency.** `ImuSample`, `Vector3`, `Quaternion`, `GaitFeatures`, `StepEvent`, `Stride` are named once and used consistently. The `GaitFeatures` data class fields are listed in Task 12 and reused in Task 13's tests.
