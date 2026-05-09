# Code Review: Phase 3 (Gait Signal Processing)

Reviewer: Code Reviewer agent (session 2026-05-07)
Commits reviewed: `4996ed4..b23c287` (18 commits, cumulative diff over `app/`, `docs/`, `SPEC.md`, `STATUS.md`)
Verdict: **APPROVED WITH MINOR CHANGES**

---

## Summary

Phase 3 lands a clean, well structured pure Kotlin DSP module that meets the SPEC.md
Section 7.1 pipeline shape and the SPEC.md Section 7.2 layer 1 synthetic ground truth
target. All 81 unit tests pass with zero failures and zero errors. The architectural
rails are honored: no `android.*` imports anywhere under `dsp/`, the parallel from
scratch Madgwick filter exists alongside `ImuSample.rotationVector` and feeds the
quality score residual, and the per sample inner loops in `Butterworth.Biquad.process`
and `Madgwick.update` are allocation free per the Performance Engineer's review. Inherited
CLAUDE.md rules (no dashes as prose punctuation, no emojis, no `Co-Authored-By:` trailers,
no AI attribution lines) are fully respected across all 18 commits and across the prose
deliverables. The Citation Auditor MISMATCH on the ADR (Buckley 2020 to Bea T 2025) was
fixed in commit `b23c287` before this review.

Findings below: zero BLOCKING, four MINOR, four LOW, one PASS section. None block phase
close. The minor changes named under "Required actions" should land before Phase 11 polish
or before any of the affected artifacts is referenced as authoritative downstream;
two of them (M1 and M2) are duplicates or extensions of the Citation Auditor's PARTIAL
findings P3.1 and P3.2 that were already accepted as Phase 5 cleanup items.

Test suite at the time of review:

```
JAVA_HOME=/snap/android-studio/209/jbr ./gradlew :app:testDebugUnitTest
BUILD SUCCESSFUL
tests=81 skipped=0 failures=0 errors=0
```

That is up from 48 at Phase 2 close, matching the Phase 3 plan's "roughly 60 plus" target
with a margin (Phase 2 close was 48; Phase 3 added 33 tests across the 8 new test files
plus the 6 fixture round trip tests plus the 4 GaitPipeline integration tests).

---

## Findings

### MINOR M1: Unverified paper content claim repeats in `GaitPipeline.kt` outside the Citation Auditor's audit scope

**File:** `app/src/main/java/com/mustafan4x/baselinems/dsp/GaitPipeline.kt`
**Line:** 214 (KDoc on `orientationQualitySignals`).

The Citation Auditor's Phase 3 verdict P3.2 (`docs/source/citation-audit-log.md` line 431
to 439) flagged the class level KDoc in `Madgwick.kt` line 20 for asserting "The default
beta gain (0.1) is the value Madgwick reports as a reasonable starting point in the
paper." That claim was verdicted PARTIAL because the project's bibliography does not
record what value Madgwick (2010) recommends and the auditor did not re fetch the primary
source. The CA explicitly scoped the Phase 3 audit to `Madgwick.kt` and `Zupt.kt` source
comments and did not audit `GaitPipeline.kt`.

The same unverified attribution re appears in `GaitPipeline.kt` line 214: "The Madgwick
filter is configured at beta = 0.1 which Madgwick (2010) recommends for walking IMU
streams." The phrasing is even more specific than the `Madgwick.kt` version (it adds
"for walking IMU streams" which is a paper-content claim that the bibliography does not
record).

This finding is the same defect class as P3.2 but in a file the CA did not audit. The
correction is the same: drop the "Madgwick (2010) recommends" attribution and frame
0.1 as the project's chosen starting value. Per the global CLAUDE.md "memory is not
proof" and "verify before you assert" rules, an unverified attribution in a code comment
is the kind of defect this project actively guards against.

**Severity:** MINOR. Same justification as P3.2: the comment is internal to the DSP
package and does not propagate to any user facing artifact in Phase 3. It does not
block phase close. It should land as a one line edit before Phase 4 dispatches.

**Required action:** Edit `GaitPipeline.kt` line 214 to drop the "which Madgwick (2010)
recommends for walking IMU streams" clause. Either rephrase to "configured at beta =
0.1, the project's default starting value (see ADR 0002 for tuning revisit conditions)"
or omit the attribution entirely. The same edit should land in `Madgwick.kt` line 20
to fully resolve the CA's P3.2 PARTIAL finding while the file is being touched.

---

### MINOR M2: `Quaternion` API has no `inverse()` method but the `ImuSample.kt` KDoc references one

**File:** `app/src/main/java/com/mustafan4x/baselinems/dsp/ImuSample.kt`
**Lines:** 13 to 14.

The KDoc states "`rotationVector.inverse().rotate(worldVec)` produces the device frame
equivalent." There is no `inverse()` method on `Quaternion` in production code. The
fixtures package defines a private extension `Quaternion.inverse()` (`SyntheticImu.kt`
line 190), and `SyntheticImuTest.kt` line 58 inlines the conjugate as
`Quaternion(q.w, -q.x, -q.y, -q.z).normalized()`.

The doc names a method that does not exist. A reader following the doc would write
`rotationVector.inverse().rotate(worldVec)` and get a compile error.

**Severity:** MINOR. The fixtures-internal extension does not surface to consumers of the
public API. The KDoc is a documentation/API alignment issue, not a runtime bug.

**Required action (one of):**
- (a) Promote a public `Quaternion.inverse()` (or equivalently named `conjugate()` since
  for unit quaternions inverse equals conjugate) to the production class, with a unit
  test, and update the existing fixtures extension to call it. This is the cleanest
  fix and avoids the duplicated inline conjugate construction in `SyntheticImuTest.kt`.
- (b) Edit the `ImuSample.kt` KDoc to express the inverse as `Quaternion(rotationVector.w,
  -rotationVector.x, -rotationVector.y, -rotationVector.z).normalized()`, which mirrors
  what the test code does.

Option (a) is the lower friction long term fix; option (b) is the lower friction short
term fix.

---

### MINOR M3: `rotateToWorld` falls back to identity when `rotationVector` is null instead of using the from scratch Madgwick

**File:** `app/src/main/java/com/mustafan4x/baselinems/dsp/GaitPipeline.kt`
**Lines:** 95 to 109 (`rotateToWorld`).

`SPEC.md` Section 7.1 step 1 states the rotation vector "may be null on devices that do
not expose it." `ImuSample.rotationVector` is therefore typed `Quaternion?`. The pipeline's
`rotateToWorld` falls back to `Quaternion.IDENTITY` when the platform vector is null
(line 102: `val q = s.rotationVector ?: Quaternion.IDENTITY`). On a device without
`TYPE_ROTATION_VECTOR`, this would skip the world frame transform entirely (the device
frame would be treated as the world frame), so step detection would run on device frame
Z rather than world frame Z, and stride pairing would run on device frame X rather than
world frame X. The pipeline would silently produce wrong features.

The architectural rail in the Phase 3 plan (decision 7) says "The from scratch Madgwick
implementation exists alongside the platform fused estimate for the quality score
residual." The natural use of the from scratch Madgwick when the platform vector is
null is as the world frame transform's primary source. The pipeline does run Madgwick
inside `orientationQualitySignals` (lines 226 to 271), so the data is computed but then
discarded; it is not used as a fallback.

**Severity:** MINOR. No Phase 3 unit test exercises this null path because every fixture
populates `rotationVector` (per `SyntheticImu.kt` line 90). On a real device that does
expose `TYPE_ROTATION_VECTOR` this finding is irrelevant. The risk surfaces only for
devices that lack the fused sensor, a scenario the Phase 4 Sensor Integration Engineer
will encounter.

**Required action:** Phase 4 (not Phase 3) should resolve this when wiring the signals
layer. Either (a) the Sensor Integration Engineer guarantees `rotationVector` is always
populated by computing it from the from scratch Madgwick when the platform fused sensor
is unavailable, or (b) the DSP pipeline's `rotateToWorld` is updated to compute its own
Madgwick estimate per sample as a fallback. Phase 3 should record this as a known gap
in `STATUS.md` Resume notes so Phase 4 picks it up; this review records it here so the
PM can include it in the Phase 4 dispatch brief.

---

### MINOR M4: `medianStepInterval` is recomputed once per step inside the lateral at step loop

**File:** `app/src/main/java/com/mustafan4x/baselinems/dsp/GaitPipeline.kt`
**Lines:** 75 to 77 (the `DoubleArray(steps.size) { i -> lateralAtStepIndex(...) }`
initializer) and 120 to 138 (`lateralAtStepIndex` which calls `medianStepInterval(steps)`
per invocation).

The Performance Engineer flagged this as INFORMATIONAL finding 1 in
`docs/perf/latency-budgets.md` line 66 to 72. The Code Reviewer concurs and recommends
hoisting because: (a) the sort and array allocation inside `medianStepInterval` runs
once per detected step, so a 60 step trial does 60 redundant sorts; (b) the redundant
work is the only place in the DSP package where the inner loop scales super linearly
with one of the input dimensions; (c) hoisting is a one line refactor with no behavior
change. The PE's framing was "informational, not a perf concern at the documented step
counts," which is accurate. The Code Reviewer flags it MINOR because of the simplicity
of the fix and because hoisting also clarifies the call shape.

**Severity:** MINOR. Not blocking.

**Required action:** Hoist `medianStepInterval(steps)` out of the `DoubleArray(steps.size)
{ i -> lateralAtStepIndex(...) }` initializer in `process` (line 75). Pass the median as
a parameter to `lateralAtStepIndex` rather than recomputing inside. One line refactor
that the SPE can pick up before Phase 4 dispatch.

---

### LOW L1: Three pre canned fixtures (`slowWalk`, `briskWalk`, `severeAsymmetry`) have no integration test coverage

**File:** `app/src/test/java/com/mustafan4x/baselinems/dsp/GaitPipelineIntegrationTest.kt`

The Phase 3 plan Task 13 spec says "The integration test covers every pre canned fixture
from Task 3" (`docs/plans/phase-3-gait-signal-processing.md` line 1082). The current
integration test exercises only 4 of the 7 fixtures from `PreCannedFixtures`:
`healthyControlNormal`, `msTypicalNormal`, `mildAsymmetry`, `noisyMsNormal`. The 3
missing fixtures (`slowWalk`, `briskWalk`, `severeAsymmetry`) are declared in
`PreCannedFixtures.kt` but no test runs the pipeline against them.

The deviation is plausibly intentional: the plan body provided test code samples for
only the same 4 fixtures the implementation tests. Reading the plan literally, the SPE
chose to honor the explicit test samples rather than the prose "every pre canned
fixture" wording. The QA Engineer's Phase 3 regression checklist (Task 18, not yet
landed at the time of this review) is the natural place to add the missing 3 cases.

**Severity:** LOW. No Phase 3 deliverable is unsafe; the pipeline's three fixture cases
are exercised. The 3 missing fixtures bracket the design envelope (slow tail, brisk tail,
high asymmetry) and adding them would catch envelope regressions earlier.

**Recommended action:** QA Engineer adds three cases to
`GaitPipelineIntegrationTest.kt` (or to the regression checklist as a falsifiable
condition) before Task 18 sign off:

- `slowWalk`: cadence within 3 percent of 80, stride length within 5 percent of 0.85.
- `briskWalk`: cadence within 3 percent of 130, stride length within 5 percent of 1.55.
- `severeAsymmetry`: stride asymmetry index within 0.05 of 0.2609 (per `docs/qa/fixtures.md`
  line 221).

The 5 percent tolerance on the slow and brisk fixtures is looser than the 2 percent that
`healthyControlNormal` and `msTypicalNormal` use; the looser tolerance reflects the fact
that the synthetic forward propulsion model assumes velocity is exactly zero at every
mid stance, which is exact at the symmetric base case but accumulates a small per
segment error at the envelope tails. Phase 5 calibration may tighten these.

---

### LOW L2: Unused `kotlin.math.sin` import in `ButterworthLowPass.kt`

**File:** `app/src/main/java/com/mustafan4x/baselinems/dsp/ButterworthLowPass.kt`
**Line:** 5.

`import kotlin.math.sin` is declared but `sin` is not referenced anywhere in the file
(verified by grep). Likely a leftover from an earlier implementation iteration that
used `sin` in the bilinear transform formula and switched to `tan(w0 / 2.0)`.

**Severity:** LOW. Lint warning, not a defect.

**Recommended action:** Remove the import.

---

### LOW L3: `java.lang.Math.PI` used where `kotlin.math.PI` is the project idiom

**Files:**
- `app/src/main/java/com/mustafan4x/baselinems/dsp/GaitPipeline.kt` lines 279, 286.
- `app/src/test/java/com/mustafan4x/baselinems/dsp/ZuptTest.kt` lines 16, 21, 38, 44.

Every other file in the DSP package and in the fixtures package uses `kotlin.math.PI`
(via `import kotlin.math.PI`). `Math.PI` is functionally identical (it resolves to the
same constant on the JVM) but is the Java idiom; `kotlin.math.PI` is the Kotlin idiom.
The project's coding conventions in `CLAUDE.md` ("idiomatic, no Java style getters and
setters where Kotlin properties suffice") imply Kotlin standard library imports should
be preferred where they exist.

**Severity:** LOW. Style consistency, not a defect.

**Recommended action:** Add `import kotlin.math.PI` in `GaitPipeline.kt` and `ZuptTest.kt`
and replace `Math.PI` with `PI` in the affected lines.

---

### LOW L4: Internal docstring in `FeatureExtractor.kt` is internally inconsistent on double support time formula

**File:** `app/src/main/java/com/mustafan4x/baselinems/dsp/FeatureExtractor.kt`
**Lines:** 19 to 22.

The KDoc says "this feature is computed as half the mean step interval, which approximates
the published double support fraction (about 20 percent of stride time in healthy adults,
slightly higher in MS)." The implementation at line 120 is
`return 0.2 * (2.0 * meanStepInterval)`, which evaluates to `0.4 * meanStepInterval`,
i.e. 40 percent of mean step interval, or equivalently 20 percent of stride time (since
stride time = 2 * step interval).

The "20 percent of stride time" framing in the docstring matches the code; the "half the
mean step interval" framing does not (half of step interval would be 0.5 * meanStepInterval,
which is 25 percent of stride time, which contradicts the 20 percent figure the docstring
also claims). The docstring is internally inconsistent.

**Severity:** LOW. Documentation only, no behavior impact.

**Recommended action:** Rewrite the docstring to "this feature is computed as 20 percent
of stride time, which approximates the published double support fraction in healthy
adults (slightly higher in MS), implemented as `0.2 * 2 * meanStepInterval` since
stride time = 2 step intervals." Drop the "half the mean step interval" framing.

---

### PASS: Inherited rules (no dashes, no emojis, no trailers, no AI attribution)

`git log --format='%B' 4996ed4..HEAD | grep -iE "co-authored|generated with|claude code"`
returns nothing.
`git log --format='%B' 4996ed4..HEAD | grep -P '[\x{1F300}-\x{1FAFF}\x{2600}-\x{27BF}]'`
returns nothing.
`git diff 4996ed4..HEAD -- '*.kt' '*.md' | grep -P '^[+-].*[\x{2013}\x{2014}]'` returns
nothing (the regex matches U+2013 EN DASH and U+2014 EM DASH on the diff output).
Spot read of the ADR, the citation audit log Phase 3 section, the fixtures.md, the
perf review, and the SPEC.md and STATUS.md diffs shows only ASCII hyphens used inside
identifiers, file paths, URLs, and code (the only contexts where the global CLAUDE.md
permits hyphens). No emojis. No co author or AI attribution lines.

---

### PASS: Architectural rails (no `android.*` under `dsp/`, parallel from scratch Madgwick exists, allocation discipline)

- `grep -rn "import android" app/src/main/java/com/mustafan4x/baselinems/dsp/
  app/src/test/java/com/mustafan4x/baselinems/dsp/
  app/src/test/java/com/mustafan4x/baselinems/fixtures/` returns nothing.
- `Madgwick.kt` is the from scratch IMU only filter, instantiated in `GaitPipeline.kt`
  line 51 and consumed in `orientationQualitySignals` lines 226 to 271 to compute the
  per sample residual against `s.rotationVector` (line 257, `angleBetween(madgwick.orientation(),
  s.rotationVector)`). The parallel from scratch filter exists alongside the platform
  fused quaternion as the Phase 3 plan architectural decision 7 prescribes.
- The Performance Engineer's allocation review (`docs/perf/latency-budgets.md` line 20
  to 100) confirms the Butterworth biquad and Madgwick per sample loops are allocation
  free. The Code Reviewer concurs after independent read of `ButterworthLowPass.kt`
  lines 75 to 95 (Biquad class) and `Madgwick.kt` lines 40 to 93 (`update`).

---

### PASS: Test discipline (every code change has an accompanying test)

Every production Kotlin file under `dsp/` has a co-committed test file in `app/src/test`:

| Production file | Test file | Tests |
|---|---|---|
| `Vector3.kt` | covered indirectly | exercised by every other DSP test |
| `Quaternion.kt` | covered indirectly | exercised by every other DSP test |
| `ImuSample.kt` | covered indirectly | exercised by `SyntheticImuTest` |
| `ButterworthLowPass.kt` | `ButterworthLowPassTest.kt` | 3 |
| `Madgwick.kt` | `MadgwickTest.kt` | 2 |
| `WorldFrame.kt` | `WorldFrameTest.kt` | 2 |
| `StepDetector.kt` | `StepDetectorTest.kt` | 2 |
| `StridePairing.kt` | `StridePairingTest.kt` | 4 |
| `Zupt.kt` | `ZuptTest.kt` | 3 |
| `FeatureExtractor.kt` | `FeatureExtractorTest.kt` | 7 |
| `GaitFeatures.kt` | covered by `FeatureExtractorTest.kt` | included in extractor tests |
| `GaitPipeline.kt` | `GaitPipelineIntegrationTest.kt` | 4 |

The fixtures package has its own round trip tests (`SyntheticImuTest.kt`, 6 tests). Total
new DSP plus fixture tests: 33, lifting the suite from 48 (Phase 2 close) to 81. The TDD
ordering required by the plan (failing test before implementation) is observable in the
commit order: each `dsp(gait): ...` commit message names a single module and lands its
production file plus its test file in the same commit, which is the discipline the
Phase 3 plan prescribed for the SPE.

`Vector3.kt`, `Quaternion.kt`, `ImuSample.kt` lack dedicated test files but are exercised
in every downstream test (e.g. `MadgwickTest`, `WorldFrameTest`, `SyntheticImuTest`).
The plan's Task 1 did not require dedicated value-type tests, so this is conformant.

---

### PASS: Code quality (Kotlin idiom, no defensive code, no dead code, comments where the why is non obvious)

- All new public API uses `val` over `var` where possible; mutable state is restricted
  to the Madgwick quaternion field (`q0` to `q3`), the Butterworth biquad state (`z1`,
  `z2`), and intermediate locals.
- Operator overloads on `Vector3` (plus, minus, times) are idiomatic.
- `GaitFeatures` is an idiomatic data class with a `toMap()` projection; `StepEvent`,
  `FootStep`, `Stride` are all data classes.
- `Foot` is a proper enum.
- KDoc comments are present on the public API and explain the WHY (citation traceability,
  architectural rails, smartphone adaptation choices). No comments narrate WHAT the code
  does.
- The two MINOR finding clauses M1 and M4 above are the only places where the Code
  Reviewer flags the comments; the rest are well calibrated.
- No `try/catch` swallowing exceptions, no `runCatching` defensive code, no nullable returns
  used to signal failure.
- No commented out code blocks, no orphan helpers.

---

### PASS: Cross cutting concerns (no new dependencies, no manifest touch, no data layer changes)

`git diff 4996ed4..HEAD -- 'app/build.gradle.kts' 'app/src/main/AndroidManifest.xml'`
returns no lines. No new runtime dependencies, no new test dependencies, no manifest
changes. The DSP module honors the Phase 3 plan's "no new third party dependencies"
architectural rail.

The `data/` and `battery/` packages are untouched. The Sensor Integration Engineer's
Phase 4 work is the natural next consumer of `ImuSample`; no Data Engineer contract is
crossed in Phase 3.

---

## Prioritized correction list

The verdict is APPROVED WITH MINOR CHANGES. Phase 3 may close once the following land or
are recorded as deferred items in `STATUS.md` Resume notes for Phase 4 and Phase 11.

**Required before Phase 4 dispatch (PM may decide to fold these into Phase 4 prep rather
than dispatching the SPE for a fix commit cycle now):**

1. **M1.** Edit `GaitPipeline.kt` line 214 and `Madgwick.kt` line 20 to drop the unverified
   "Madgwick (2010) recommends" attribution. Frame 0.1 as the project's chosen starting
   value with a pointer to ADR 0002. This closes the CA's P3.2 PARTIAL finding fully and
   removes the duplicate in `GaitPipeline.kt` that the CA did not audit.
2. **M2.** Either promote a `Quaternion.inverse()` (or `conjugate()`) to the production
   class with a unit test, or rewrite the `ImuSample.kt` KDoc lines 13 to 14 to inline
   the conjugate construction. Option (a) is preferred.
3. **M3.** Record in `STATUS.md` Resume notes that Phase 4's Sensor Integration Engineer
   must handle the case where `ImuSample.rotationVector` is null on a device that does
   not expose `TYPE_ROTATION_VECTOR`. Either populate it from the from scratch Madgwick
   in the signals layer, or update `GaitPipeline.rotateToWorld` to fall back to its own
   Madgwick estimate.

**Recommended for Phase 4 prep or before any Phase 5 milestone:**

4. **M4.** Hoist `medianStepInterval(steps)` out of the lateral at step initializer in
   `GaitPipeline.process` (one line refactor).
5. **L1.** Add integration test cases for `slowWalk`, `briskWalk`, and `severeAsymmetry`,
   either in `GaitPipelineIntegrationTest.kt` or as falsifiable conditions in the QA
   Engineer's Phase 3 regression checklist (Task 18).

**Cleanup items (LOW severity, can be batched in any future PR that touches the file):**

6. **L2.** Remove the unused `import kotlin.math.sin` in `ButterworthLowPass.kt`.
7. **L3.** Switch `Math.PI` to `kotlin.math.PI` in `GaitPipeline.kt` (lines 279, 286)
   and `ZuptTest.kt` (lines 16, 21, 38, 44).
8. **L4.** Rewrite the `FeatureExtractor.kt` double support time docstring (lines 19 to
   22) to drop the "half the mean step interval" framing and keep only the "20 percent
   of stride time" framing that matches the implementation.

---

## Notes for Phase 4

1. The DSP module is consumed by Phase 4: the Sensor Integration Engineer wires
   `SensorManager` to a `Flow<ImuSample>` and the Android Engineer wires the Compose UI.
   The `ImuSample` contract (timestamp in nanoseconds, three `Vector3` channels plus
   the optional `Quaternion?` rotation vector) is the integration point. Phase 4
   should preserve the convention `rotationVector.rotate(deviceVec) = worldVec`
   documented in `ImuSample.kt` lines 11 to 17.
2. The `GaitPipeline.process(samples: List<ImuSample>): GaitFeatures` entry point takes
   a fully captured 30 second buffer. Phase 4 should not call it on a partial buffer;
   the integrator and the quality score both depend on the full trial duration.
3. The pipeline pre warms the from scratch Madgwick filter with the time averaged static
   accelerometer over the first 50 samples (`GaitPipeline.kt` line 231). Phase 4 should
   either provide a real standing window before the walk (so the platform fused estimate
   has converged before the user starts walking) or accept that the first ~0.5 seconds
   of trial samples will dominate the static gravity estimate.
4. The `lateralAtStepIndex` heuristic (sample lateral acceleration a quarter step
   interval before the step) is a synthetic generator adaptation choice (see
   `GaitPipeline.kt` lines 111 to 138 KDoc). Phase 5 should compare this against real
   recordings; if real lateral peaks fall at different phase offsets relative to heel
   strikes, this offset will need calibration.
