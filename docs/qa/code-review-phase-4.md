# Code Review: Phase 4 (Gait Test Module Integration)

Reviewer: Code Reviewer agent (session 2026-05-08)
Commits reviewed: `4cd0a72..a8ca3e2` (11 commits, cumulative diff over `app/`, `docs/adr/`, `docs/observability/`, `docs/qa/`, `docs/perf/`)
Verdict: **APPROVED WITH MINOR CHANGES**

---

## Summary

Phase 4 wires the Phase 3 gait DSP module to a `SensorManager` backed `Flow<ImuSample>`,
adds a Compose `GaitTest` `TestModule` with four screens (Instructions, Countdown,
Capture, Done), persists raw sensor traces as gzipped CSV to the application's private
files directory, and threads the optional `rawSensorRelativePath` through the
`TestResultPayload` contract into the existing Room schema. All 102 unit tests pass with
zero failures and zero errors. The architectural rails locked in by the Phase 4 plan are
honored except for one deliberate, ratified deviation on rail 3 (the fallback Madgwick
path uses `TYPE_ACCELEROMETER` rather than `TYPE_LINEAR_ACCELERATION`; ADR 0003 documents
the correctness reason). The `BilateralTapTest` shipped in Phase 2 continues to pass
unchanged because the `TestResultPayload` extension is a default property getter
(`val rawSensorRelativePath: String? get() = null`), which the existing payload picks up
automatically. Inherited CLAUDE.md rules (no dashes as prose punctuation, no emojis, no
`Co-Authored-By:` trailers, no AI attribution lines) are fully respected across all 11
commits and across the prose deliverables. The Manifest is verified untouched in Phase
4; the Security Engineer's veto on adding `INTERNET` is preserved.

The three prior Phase 4 reviews resolved as follows:

- **SPE Phase 4** (`docs/qa/spe-review-phase-4.md`, 2026-05-07): APPROVED WITH MINOR
  FINDINGS (1 MINOR M1, 3 INFORMATIONAL I1, I2, I3).
- **Patient Advocate Phase 4** (`docs/qa/patient-advocate-reviews.md`, 2026-05-07
  entry): APPROVED WITH FINDINGS (3 medium, 2 low; none blocking).
- **Performance Engineer Phase 4** (`docs/perf/latency-budgets.md` Phase 4 review,
  2026-05-08): APPROVED WITH MINOR FINDINGS (0 BLOCKING, 0 HIGH, 1 MEDIUM M3, 4 MINOR
  M1, M2, M4, M5, 3 INFORMATIONAL I1r, I2r, I3r).

This review consolidates those verdicts, rules each finding to a target phase (Phase 4
cleanup, Phase 5 prep, Phase 11 polish), ratifies the seven plan deviations, and adds
the Code Reviewer's own per file pass.

Findings consolidated below: 0 BLOCKING, 0 HIGH, 1 MEDIUM (PE M3, Phase 5 prep), 5
MINOR (1 required Phase 4 cleanup, 4 Phase 5 prep), 5 LOW (all Phase 11), 3
INFORMATIONAL (2 Phase 5, 1 Phase 11). The single Phase 4 cleanup commit required
before QA Engineer Task 13 is PE M1 (a one line wording reconciliation in the latency
budgets file).

Test suite at the time of review:

```
JAVA_HOME=/snap/android-studio/209/jbr ./gradlew :app:testDebugUnitTest
BUILD SUCCESSFUL
tests=102 skipped=0 failures=0 errors=0
```

That is up from 83 at Phase 3 close, matching the Phase 4 plan's QA Engineer Task 13
"test count strictly grew" falsifiable condition. Phase 4 added 19 tests across 5 new
test files (`TestModuleTest.kt` 2, `AndroidImuSourceTest.kt` 4, `RawSensorWriterTest.kt`
2, `GaitTestRenderTest.kt` 4, `GaitTestViewModelTest.kt` 6, `GaitTestRegistrationTest.kt`
1) and modified two existing test files (`BatteryFlowIntegrationTest.kt`,
`BatteryOrchestratorTest.kt`) for the `recordResult` signature change.

---

## Conformance to Phase 4 plan

The Phase 4 plan defines 13 tasks (Task 0 PM prep, Tasks 1 to 8 implementation, Tasks 9
to 12 reviews, Task 13 QA close). Tasks 1 to 12 land in this review window; Task 13
follows Code Reviewer sign off.

| Task | Owner | Deliverable | Commit | Status |
|------|-------|-------------|--------|--------|
| 1 | Android Engineer | `TestResultPayload.rawSensorRelativePath`, `BatteryOrchestrator.recordResult(testType, payload)`, `TestModuleTest` | `b8187c8` | LANDED |
| 2 | Sensor Integration Engineer | `signals/ImuSource.kt`, `signals/AndroidImuSource.kt`, `AndroidImuSourceTest.kt` | `0cbaafa` | LANDED |
| 3 | Sensor Integration Engineer | `docs/adr/0003-sensor-type-choice.md` | `ea27060` | LANDED |
| 4 | Sensor Integration Engineer | `docs/observability/sensor-runbook.md` | `aac52f2` | LANDED |
| 5 | Sensor Integration Engineer | `signals/RawSensorWriter.kt`, `RawSensorWriterTest.kt` | `2f47991` | LANDED |
| 6 | Android Engineer | Four GaitTest screens, `GaitTestState`, `GaitTestRenderTest` | `c33988f` | LANDED |
| 7 | Android Engineer | `GaitTestViewModel`, `GaitTest`, `GaitTestViewModelTest` (plus `open` markings on `GaitPipeline` and `RawSensorWriter`) | `34a3174` | LANDED |
| 8 | Android Engineer | `MSBatteryApp` singleton `imuSource` plus `RootScreen` session route factory plus `GaitTestRegistrationTest` | `d9b4795` | LANDED |
| 9 | Signal Processing Engineer | `docs/qa/spe-review-phase-4.md` | `bf99d79` | LANDED |
| 10 | Patient Advocate | Phase 4 entry in `docs/qa/patient-advocate-reviews.md` | `4bc1bec` | LANDED |
| 11 | Performance Engineer | Phase 4 review subsection in `docs/perf/latency-budgets.md` | `a8ca3e2` | LANDED |
| 12 | Code Reviewer | This file (`docs/qa/code-review-phase-4.md`) | (this commit) | IN PROGRESS |

Commit messages match the plan's prescribed text shape (`battery: ...`, `signals: ...`,
`docs(adr): ...`, `review(gait): ...`, `perf(gait): ...`). The TDD discipline the plan
required is observable in the diffs: each implementation commit lands a production file
plus a matching test file in the same commit, and the test count strictly grows on every
commit. The plan's coordination rules (SIE owns Tasks 2 to 5; AE owns Tasks 1, 6, 7, 8;
reviewers run after AE Task 8; Code Reviewer runs after the three reviewers) are
honored by the commit timestamps. Three intentional plan deviations land in Phase 4 and
are itemized in the "Plan deviations ratified" section below.

---

## SPEC.md Section 7 fidelity

`SPEC.md` Section 7.1 step 1 prescribes "Android's `SensorManager` configured to deliver
`Sensor.TYPE_LINEAR_ACCELERATION`, `Sensor.TYPE_GYROSCOPE`, and `Sensor.TYPE_ROTATION_VECTOR`
... a custom 100Hz interval via `registerListener` with a sampling period in
microseconds." Verified in `AndroidImuSource.kt` lines 43 to 50 (three sensor types
registered; `TYPE_ACCELEROMETER` only on the fallback path per ADR 0003) and lines 103
to 116 (`registerListener(listener, sensor, samplingPeriodMicros)` with default
`samplingPeriodMicros = 10_000`). The wording matches verbatim.

`SPEC.md` Section 8 (Data Model) prescribes "Gait raw IMU: 60KB compressed for a 30
second trace at 100Hz, stored in the app's private files directory and referenced by
`rawSensorRelativePath`." Verified in `RawSensorWriter.kt` lines 26 to 63 (gzipped CSV
via `GZIPOutputStream` over `BufferedOutputStream` over `FileOutputStream`),
`RawSensorWriter.HEADER` lines 99 to 105 (the 14 column header), and `RootScreen.kt`
lines 107 to 110 (destination path
`File(app.filesDir, "sensor_traces/$sessionId/${TestType.GAIT.name}.csv.gz")`). The
relative path passed to `TestResultPayload.rawSensorRelativePath` is
`destinationFile.relativeTo(filesDir).path` (`GaitTestViewModel.kt` line 111), which
yields `"sensor_traces/<sessionId>/GAIT.csv.gz"`, matching Phase 4 plan rail 5 exactly.
`BatteryOrchestrator.recordResult` lines 46 to 77 set
`rawSensorRelativePath = payload.rawSensorRelativePath` on the inserted
`TestResultEntity`, writing to the column already present in the Phase 1 schema. No
schema change is required.

`SPEC.md` Section 7.1 step 2 (orientation estimation) reads "Android's
`TYPE_ROTATION_VECTOR` provides a fused orientation quaternion out of the box; we use it
directly. As a teaching exercise (and to make this module more impressive) we also
implement a Madgwick filter on raw gyro plus accelerometer and compare its output to the
platform fused estimate." The Phase 3 close already wired the parallel Madgwick into
`GaitPipeline.orientationQualitySignals` for the trial quality score residual. Phase 4
adds a second use: when `TYPE_ROTATION_VECTOR` is absent, `AndroidImuSource` runs an
inline Madgwick instance to fill `ImuSample.rotationVector` (ADR 0003 ratifies this).
The from scratch filter therefore serves both roles: the teaching exercise residual on
the common path, and the orientation source on the fallback path.

`SPEC.md` Section 10 (Privacy and Safety) prescribes no `INTERNET` permission. Verified
in the "Manifest audit" section.

---

## Manifest audit

Per the Phase 4 plan Task 12 spec and the Security Engineer's veto on any change that
would add `INTERNET` or any other runtime permission to the application.

Direct verification on `app/src/main/AndroidManifest.xml`:

```
$ grep -in 'INTERNET\|permission' app/src/main/AndroidManifest.xml
$ git diff 4cd0a72..HEAD -- app/src/main/AndroidManifest.xml
```

The first grep returns nothing (the manifest contains no `<uses-permission>` element of
any kind, and no occurrence of the string `INTERNET` or `permission`). The git diff
returns nothing (the manifest is byte identical to the Phase 3 close commit `4cd0a72`).
ADR 0003 records that the four sensor types used (`TYPE_LINEAR_ACCELERATION`,
`TYPE_GYROSCOPE`, `TYPE_ROTATION_VECTOR`, `TYPE_ACCELEROMETER`) do not require any
runtime permission. PASS.

---

## Plan deviations ratified

The Phase 4 plan locked in eight architectural rails plus a set of explicit task level
deliverables. Implementation lands seven plan deviations. Each one is itemized below
with the Code Reviewer's ratification.

### Rail 3 wording vs ADR 0003 (fallback Madgwick uses `TYPE_ACCELEROMETER`)

**Rail 3 says:** the fallback Madgwick path runs "using the linear acceleration plus
gyroscope inputs." **Implementation:** uses raw `TYPE_ACCELEROMETER` (gravity included),
not `TYPE_LINEAR_ACCELERATION` (gravity removed). `AndroidImuSource.kt` lines 46 to 50
register `TYPE_ACCELEROMETER` as a fourth listener only when `TYPE_ROTATION_VECTOR` is
absent. The `emitIfReady` fallback branch (lines 128 to 137) feeds `lastAccel` to the
Madgwick filter.

**Ratification:** ACCEPTABLE. ADR 0003 lines 33 to 41 document the correctness reason
exhaustively: the Madgwick accelerometer correction term measures the residual between
the gyro integrated orientation's predicted gravity direction and the observed
acceleration direction; feeding gravity removed input strips the gravity reference
vector and the filter degenerates to pure gyro integration. The implementation cannot
produce a correct fallback orientation any other way. The plan rail is the historical
artifact of how the phase was scoped before the SIE encountered the correctness
consequence; the ADR is the authoritative current contract. The Phase 4 plan file is
not retroactively edited because the plan is the historical record of how the phase was
scoped, not the current contract.

### Task 8 file location (`MSBatteryApp.kt` vs `RootScreen.kt`)

**Plan says:** "`MSBatteryApp` already constructs the `BatteryOrchestrator` with a
`List<TestModule>`." **Implementation:** `MSBatteryApp.kt` exposes a singleton
`imuSource` property (lines 14 to 20), but `BatteryOrchestrator` and `GaitTest` factory
construction lives in `RootScreen.kt` lines 102 to 129 inside the `composable("session")`
block. Orchestrator is per session, sensor source is per process.

**Ratification:** ACCEPTABLE. The Phase 1 to Phase 3 wiring already located orchestrator
construction in `RootScreen.kt`'s session route. Phase 4 honored that pattern rather
than refactoring orchestrator construction up to the application. The split is the
right cut: sensor source is process scoped (it owns `SensorManager` listener
registrations across activity recreations); orchestrator is session scoped.
`GaitTestRegistrationTest` verifies the production wiring constructs without crashing.

### `GaitPipeline` and `process` made `open`

`git show 34a3174 -- app/src/main/java/com/mustafan4x/msbattery/dsp/GaitPipeline.kt`
shows the only diff is `class GaitPipeline` to `open class GaitPipeline` and
`fun process` to `open fun process`. No body change, no signature change. The test
suite uses the override via `FakeGaitPipeline` in `GaitTestViewModelTest.kt` lines 46 to
52.

**Ratification:** ACCEPTABLE. SPE Phase 4 review ratified this in its "Plan deviation
ratifications" item (a). Phase 3 invariants (allocation discipline, the seven stage
composition) are preserved because `process` body is untouched. An interface based
pattern would also work and is the more Kotlin idiomatic shape, but the cost of the
refactor exceeds the benefit at Phase 4 scope. Optional Phase 11 cleanup if the
pattern surfaces in Phase 8 voice test.

### `RawSensorWriter` and `write` made `open`

`RawSensorWriter.kt` line 26 declares `open class` and line 28 declares `open suspend
fun write`. Test doubling lives in `CountingRawSensorWriter` in
`GaitTestViewModelTest.kt` lines 54 to 62.

**Ratification:** ACCEPTABLE. SPE I3 (informational) preferred a `RawSensorSink`
interface plus a final production class. The Code Reviewer concurs the interface is the
more idiomatic shape, but the writer runs on the IO coroutine, not the per sample DSP
path, so virtual dispatch overhead is irrelevant. Phase 8 voice test can choose the
interface pattern if preferred.

### `BatteryOrchestrator.activeSessionId` visibility (private to public)

`git show d9b47959 -- ...BatteryOrchestrator.kt` shows the private backing field renamed
to `_activeSessionId` and a public read only `val activeSessionId: String? get() =
_activeSessionId` (lines 33 to 34) added. `RootScreen.kt`'s session route reads it (lines
106 to 110) to compute the gait test's destination file path before the gait test runs.

**Ratification:** ACCEPTABLE. The orchestrator's session id is the right anchor for the
raw sensor trace path (the path must be deterministic and recoverable post hoc by the
History view). Read only public projection is the minimum viable surface. The
orchestrator's `_activeSessionId` is still mutated only internally. Style preference
would have skipped the underscore prefix on the backing field (Kotlin's automatic
backing field suffices for a `val`), but that is style, not a defect.

### Cancelled state rendered as zero quality `GaitDoneScreen`

`GaitTest.kt` lines 61 to 64 routes the `Cancelled` state through `GaitDoneScreen` with
a synthesized zero quality `GaitFeatures` and an `onContinue` callback that calls
`onComplete(skippedPayload())`. The synthesized `qualityScore = 0.0` means the Done
screen's quality band picks "Captured but quality is low."

**Ratification:** ACCEPTABLE. The Patient Advocate Phase 4 review explicitly evaluated
this AE flagged item (item b) and ruled it APPROVED for Phase 4
(`docs/qa/patient-advocate-reviews.md` 2026-05-07 Phase 4 entry, lines 498 to 500). The
Patient Advocate's Finding 2 (medium severity, deferred to Phase 11 polish) flags the
copy mismatch but is not a Phase 4 blocker because (a) no clinical data is corrupted,
(b) the user is not stranded mid battery, (c) the `skippedPayload` carries a null
`rawSensorRelativePath` so History will not point at a non existent CSV.

### Countdown headline addition ("Get ready to walk")

`GaitCountdownScreen.kt` lines 36 to 40 add a `headlineMedium` "Get ready to walk" line
above the `displayLarge` numeric display. The numeric display is preserved verbatim and
the `Modifier.semantics { contentDescription = secondsLabel }` on line 44 satisfies the
plan's accessibility hook.

**Ratification:** ACCEPTABLE. The Patient Advocate Phase 4 review explicitly endorsed
this AE flagged item (item a) (line 496). The headline gives a TalkBack pass a one
phrase semantic anchor before the number is announced and reduces working memory load
for users with cognitive fog. The addition does not change the test pacing, does not
introduce a new tap target, and does not violate the plan's literal spec.

---

## Consolidated findings

The findings from all three Phase 4 reviewers plus the Code Reviewer's per file pass.
Each finding is ruled to a target phase: Phase 4 cleanup (lands before QA Engineer Task
13), Phase 5 prep (carryover to the Phase 5 dispatch), or Phase 11 polish.

### From the SPE Phase 4 review

| ID | Severity | Finding | Code Reviewer ruling |
|----|----------|---------|----------------------|
| SPE M1 | MINOR | Fallback Madgwick test feeds gravity included data through the linearAcceleration channel; should add `TYPE_ACCELEROMETER` shadow sensor and feed `(0, 0, 9.81)` on it. | **Phase 5 prep.** Test fixture realism issue, not a runtime bug. The SIE's Phase 5 prep dispatch picks this up alongside the Phase 5 real device measurements. |
| SPE I1 | INFO | Tail edge sample loss on capture cancellation (`writerJob.cancel()` does not flush pending elements). | **Phase 5 prep.** Switch `finishCapture` to `cancelAndJoin` after `stop()` so the collector flushes pending elements. Owner: Android Engineer. |
| SPE I2 | INFO | First fallback Madgwick step uses gravity removed input because `lastAccel` may be null on the very first event. | **Phase 5 prep.** Document in the runbook if Phase 5 fallback recordings show first samples are visibly off. Owner: SIE. |
| SPE I3 | INFO | `RawSensorWriter` virtuality is inverted from the project's interface based pattern; preference would be a `RawSensorSink` interface and a final production class. | **Phase 11 polish (optional).** Phase 8 voice test can choose the interface pattern if preferred. |

### From the Patient Advocate Phase 4 review

| ID | Severity | Finding | Code Reviewer ruling |
|----|----------|---------|----------------------|
| PA Finding 1 | medium | Cancel button reachability while the phone is in a front pocket; mitigation is a one sentence Instructions screen add priming the user before capture begins. | **Phase 11 polish.** Patient Advocate explicitly named this acceptable to defer; the Code Reviewer agrees. |
| PA Finding 2 | medium | Cancelled state Done screen says "Captured but quality is low" rather than acknowledging the cancel; recommended a `Cancelled` arm with distinct copy. | **Phase 11 polish.** No clinical data is corrupted; cosmetic. |
| PA Finding 3 | medium | No contextual messaging for mobility aid users on the Instructions screen. | **Phase 11 polish.** Aligns with the Phase 11 follow up the plan already named. |
| PA Finding 4 | low | Quality bands applied uniformly conflate flare days with phone shift noise. | **Phase 11 polish or Phase 9 reporting.** |
| PA Finding 5 | low | "Your gait test is complete." line is uninformative; cross battery warmth consistency. | **Phase 11 polish.** Cross battery copy polish. |

### From the Performance Engineer Phase 4 review

| ID | Severity | Finding | Code Reviewer ruling |
|----|----------|---------|----------------------|
| PE M1 | MINOR | Dropped windows budget wording is inconsistent across documents (Phase 0 budgets and Phase 4 plan use 30 second framing; runbook uses stricter 1 second sub window framing). | **Phase 4 cleanup commit before QA Engineer Task 13.** The only finding the Code Reviewer requires before phase close. One line edit in `docs/perf/latency-budgets.md` "Initial budgets" line 7 to read "Zero 1 second sub windows over a 30 second capture in which the cumulative delta drift exceeds 5 percent of nominal." The Phase 4 plan rail wording is left as the historical artifact (per the rail 3 ratification convention). Owner: PE or PM. |
| PE M2 | MINOR | No debug only sample rate counter exposed on `AndroidImuSource`; would be a UX win for an in app debug screen. | **Phase 11 polish.** Phase 4 close validation is fully achievable via the captured CSV (the runbook documents the awk pipeline). |
| PE M3 | MEDIUM | `GaitTestViewModel.beginCapture` runs `writerJob` (gzip plus IO) on `rememberCoroutineScope()` which dispatches on Main; `SensorManager.registerListener` is also called without a `Handler` so callbacks fire on Main. Risks the 60 fps UI budget and producer side schedule drift. | **Phase 5 prep, with a noted caveat.** This is the only MEDIUM finding in the entire Phase 4 review chain. The PE explicitly framed it as deferrable, "should be fixed before Phase 5 close." The Code Reviewer accepts the Phase 5 prep deferral because (a) the user's CSV based measurement methodology is robust to the concern in the typical case (the SharedFlow has 2.56 seconds of headroom and `event.timestamp` is hardware stamped), (b) the fix surface area is small (move `writerJob` to `Dispatchers.IO`; pass a `Handler` to `registerListener`) but requires its own test pass that bundles naturally with the Phase 5 real device measurements. The PM's Phase 5 dispatch brief should call out PE M3 as the single highest priority Phase 5 prep item. Owner: AE (view model dispatcher); SIE (listener handler). |
| PE M4 | MINOR | `quaternionFromAndroidRotationVector` allocates a transient `Quaternion` that is immediately discarded by the `.normalized()` call (~3000 redundant allocations per 30 second capture). The discarded constructor result is the only literal violation of architectural rail 7. | **Phase 5 prep.** One line refactor (compute the normalized components in primitive locals and call `Quaternion(w, x, y, z)` exactly once). Acceptable as landed for Phase 4 because the JVM nursery handles 3000 short lived allocations cheaply, and the Phase 4 close validation is on `event.timestamp` (hardware stamped), not on allocation pressure. |
| PE M5 | MINOR | `RawSensorWriter.appendRow` calls `.toString()` on 13 doubles plus the long timestamp per row (~42,000 short lived `String` allocations per capture); could coincide with thermal hiccups on budget devices. | **Phase 5 prep, conditional.** Conditional on the user's multi device measurements showing a writer thread correlation. If they do not, this drops to Phase 11 polish. |
| PE I1r | INFO | Tail edge sample loss on cancel (ratified from SPE I1). | **Phase 5 prep.** Same ruling as SPE I1. |
| PE I2r | INFO | First fallback Madgwick step uses gravity removed input (ratified from SPE I2). | **Phase 5 prep.** Same ruling as SPE I2. |
| PE I3r | INFO | `RawSensorWriter` virtuality preference (ratified from SPE I3). | **Phase 11 polish (optional).** Same ruling as SPE I3. |

### From the Code Reviewer's per file pass (new findings)

| ID | Severity | Finding | Code Reviewer ruling |
|----|----------|---------|----------------------|
| CR L1 | LOW | `GaitTest.kt` lines 75 to 83 `emptyFeatures()` inlines a zero `GaitFeatures` constructor; no `GaitFeatures.empty()` companion factory exists. A future Phase that synthesizes zero `GaitFeatures` would either duplicate this construction or refactor it. | **Phase 11 polish.** Style and DRY, not a defect. Extract `GaitFeatures.empty()` companion factory. |
| CR L2 | LOW | `GaitTest.kt` lines 69 to 73 `skippedPayload()` constructs an anonymous `TestResultPayload`; the `BilateralTapTest`'s skipped payload follows a similar pattern but uses a named factory. Cross battery consistency would prefer one shape. | **Phase 11 polish.** Style consistency. |
| CR L3 | LOW | `GaitTestViewModel.kt` line 77 `writerJob` `catch (other: Throwable)` block silently swallows non `CancellationException` failures with the comment "process whatever samples we captured." The semantic is intentional and documented, but a future Observability Engineer pass should at least log the swallowed throwable for diagnostics. | **Phase 11 polish or Phase 13 prep.** Observability Engineer is the right owner. |

### Findings count summary

| Severity | Count | Phase 4 cleanup | Phase 5 prep | Phase 11 polish |
|----------|-------|-----------------|--------------|-----------------|
| BLOCKING | 0 | 0 | 0 | 0 |
| HIGH | 0 | 0 | 0 | 0 |
| MEDIUM | 1 (PE M3) | 0 | 1 | 0 |
| MINOR | 5 (SPE M1; PE M1, M2, M4, M5) | 1 (PE M1) | 3 (SPE M1, PE M4, PE M5) | 1 (PE M2) |
| LOW | 5 (PA F4, F5; CR L1, L2, L3) | 0 | 0 | 5 |
| INFORMATIONAL | 3 (SPE I1, I2, I3 ratified by PE as I1r, I2r, I3r) | 0 | 2 (I1, I2) | 1 (I3) |
| medium (Patient Advocate severity, lower case to distinguish from Code Reviewer MEDIUM) | 3 (PA F1, F2, F3) | 0 | 0 | 3 |

**Required Phase 4 cleanup commit before QA Engineer Task 13:** PE M1 only. The fix is
a one line edit in `docs/perf/latency-budgets.md` "Initial budgets" line 7 to read
"Zero 1 second sub windows over a 30 second capture in which the cumulative delta drift
exceeds 5 percent of nominal." The PE listed this finding as deferrable, but the Code
Reviewer elects to require it as a Phase 4 cleanup because (a) the fix is one line, (b)
the inconsistency is a documentation defect that risks future readers reaching
inconsistent conclusions, (c) the QA Engineer's Phase 4 regression checklist is the
right artifact to record the resolved budget wording so it ratifies into the Phase 5
acceptance contract.

---

## PASS sections

### PASS: Inherited rules (no dashes, no emojis, no trailers, no AI attribution)

`git log --format='%B' 4cd0a72..HEAD | grep -iE "co-authored|generated with|claude code"`
returns nothing.
`git log --format='%B' 4cd0a72..HEAD | grep -P '[\x{1F300}-\x{1FAFF}\x{2600}-\x{27BF}]'`
returns nothing.
`git diff 4cd0a72..HEAD -- '*.kt' '*.md' '*.xml' | grep -P '^[+].*[\x{2013}\x{2014}]'`
returns nothing (the regex matches U+2013 EN DASH and U+2014 EM DASH).
Spot read of the new ADR 0003, the sensor runbook, the SPE review, the PA Phase 4 entry,
the PE Phase 4 review, and every Phase 4 source file shows only ASCII hyphens used
inside identifiers, file paths, URLs, and code (the only contexts the global CLAUDE.md
permits).

### PASS: Architectural rails

- **Rail 1 (no new permissions):** verified directly under "Manifest audit." PASS.
- **Rail 2 (`ImuSample`, `Vector3`, `Quaternion` stay in `dsp/`):**
  `grep -rn "import com.mustafan4x.msbattery.signals" app/src/main/java/com/mustafan4x/msbattery/dsp/`
  returns nothing; the dependency direction is `signals/` to `dsp/` only.
- **Rail 4 (100 Hz nominal):** `AndroidImuSource.kt` line 37 `samplingPeriodMicros: Int = 10_000`
  matches the rail wording exactly.
- **Rail 5 (gzipped CSV, 14 columns, `sensor_traces/<sessionId>/<testType>.csv.gz`):**
  verified in `RawSensorWriter.kt` lines 99 to 105 (header constant) and `RootScreen.kt`
  lines 107 to 110 (destination path).
- **Rail 6 (backward compatible `TestResultPayload`):** `TestModule.kt` line 11
  (`val rawSensorRelativePath: String? get() = null`) plus `TestModuleTest.kt` lines 9
  to 26 (default null and override paths). The `BilateralTapTest`'s payload picks up the
  default automatically; no diff to the tap module's production code in Phase 4.
- **Rail 7 (allocation discipline):** PE M4 flags one literal violation (the discarded
  `Quaternion` constructor result inside `quaternionFromAndroidRotationVector`),
  classified MINOR and deferred to Phase 5 prep. The remainder of the per sample
  callback path is allocation free in the steady state.
- **Rail 8 (no emulator walkthrough mandate):** Phase 4 close requires only that the
  unit test suite be green and the reviewers have signed off. Both met.

### PASS: Test discipline

19 net new tests across 5 new test files plus 2 modified test files. Every new
production file has a co-committed test file in the same commit; the TDD ordering is
observable in the commit log. `Vector3`, `Quaternion`, `ImuSample`, `ImuSource`,
`GaitTestState` are exercised indirectly by every downstream test.

### PASS: Code quality (Kotlin idiom, no defensive code, no dead code)

- All new public API uses `val` over `var` where possible; mutable state is private.
- `GaitTestState` is an idiomatic sealed class with `data object` and `data class` arms.
- `GaitTestResult` (`GaitTestViewModel.kt` lines 121 to 125) is an idiomatic data class
  implementing `TestResultPayload` directly.
- KDoc comments are present on every new public API and explain the WHY (citation
  traceability, architectural rails, plan deviations). No comments narrate WHAT the code
  does.
- `try/catch` blocks in two places (`RawSensorWriter` lines 33 to 39 and 48 to 60;
  `GaitTestViewModel` lines 67 to 78). Both are intentional and documented; CR L3 above
  flags the view model's swallow for Phase 11 observability work but rules it
  acceptable as landed.
- No commented out code, no orphan helpers, no `runCatching` defensive code.

### PASS: Cross cutting concerns (no new dependencies, no manifest touch, no data layer changes)

`git diff 4cd0a72..HEAD -- 'app/build.gradle.kts' 'app/src/main/AndroidManifest.xml'`
returns no lines. No new runtime dependencies, no new test dependencies, no manifest
changes. The Phase 4 plan's rail 1 and the plan's "New runtime dependencies: nothing"
are both honored. The `data/` package is touched only through the existing
`TestResultEntity.rawSensorRelativePath` column (already part of the Phase 1 schema; no
new migration). The Data Engineer's contract is honored.

---

## Recommendations for QA Engineer Task 13

1. **Run the full test suite** with
   `JAVA_HOME=/snap/android-studio/209/jbr ./gradlew :app:testDebugUnitTest --rerun-tasks`
   and confirm `tests=102 failures=0 errors=0 skipped=0`.
2. **Confirm the falsifiable conditions** from the Phase 4 plan Task 13 spec: the four
   `AndroidImuSourceTest` cases, the two `RawSensorWriterTest` cases, the six
   `GaitTestViewModelTest` cases, the four Compose smoke tests on the GaitTest screens,
   `BatteryFlowIntegrationTest` continues to pass, `BilateralTapTest`'s existing tests
   continue to pass, AndroidManifest.xml contains no `INTERNET` permission, and the test
   count strictly grew from 83 to 102.
3. **Land the Phase 4 cleanup commit (PE M1 only).** A one line edit in
   `docs/perf/latency-budgets.md` "Initial budgets" line 7 to read "Zero 1 second sub
   windows over a 30 second capture in which the cumulative delta drift exceeds 5
   percent of nominal." Owner: PM or PE. Phase 4 close is gated on this commit landing
   before QA Engineer Task 13.
4. **Append the Phase 4 falsifiable conditions to `docs/qa/regression-checklist.md`.**
   Mirror the Phase 2 and Phase 3 entry shape. Include the manifest grep guard so future
   reviewers can verify the absence of `INTERNET` at every phase boundary.
5. **Name the user driven Phase 4 close validation task.** The runbook
   (`docs/observability/sensor-runbook.md`) is the multi device sample rate sign off
   deliverable. The QA Engineer's regression checklist should record this as the user's
   Phase 4 close validation task: install on each available real device, run a gait
   session, pull the captured CSV via Device File Explorer or `adb pull`, run the
   runbook's awk pipeline, and append a per device entry. This mirrors the Phase 1, 2
   deferred emulator walkthrough convention.
6. **Carry deferred findings forward to STATUS.md Resume notes.** Phase 5 prep gets PE
   M3 (MEDIUM, single highest priority), SPE M1, SPE I1, SPE I2, PE M4, PE M5
   conditional. Phase 11 polish gets PA Findings 1 to 5, PE M2, SPE I3, CR L1, L2, L3.
   The PM's Phase 5 dispatch brief should highlight PE M3 first.

---

## Notes for Phase 5

1. **PE M3 is the single highest priority Phase 5 prep item.** The fix touches two
   files (`GaitTestViewModel.kt` `beginCapture` to wrap the `rawSensorWriter.write` call
   in `withContext(Dispatchers.IO)` or to launch `writerJob` on `Dispatchers.IO`;
   `AndroidImuSource.kt` `start` to pass a `Handler` or `HandlerThread` to
   `registerListener`). The Phase 5 reviewer task that runs against real walking course
   recordings is the natural acceptance gate.
2. **SPE I1 (tail edge sample loss) and SPE I2 (first fallback Madgwick step) are
   companion items.** Natural additions to the Phase 5 prep dispatch alongside PE M3.
3. **The runbook is empty as of Phase 4 close.** The user populates per device entries
   as devices are exercised. The Performance Engineer's Phase 5 dispatch should plan to
   read the runbook before the Phase 5 walking course recordings begin; if specific
   devices show fallback Madgwick behavior, the Phase 5 fixtures should include those
   devices to validate the fallback path's accuracy.
4. **The `lateralAtStepIndex` heuristic (Phase 3 Notes for Phase 4 item 4) is still a
   synthetic generator adaptation.** Phase 5 real walking course recordings will tell us
   whether to recalibrate the forward propulsion amplitude or the pipeline's mid stance
   detector. Unchanged from the Phase 3 close note; no Phase 4 work alters it.

---

## Sign off

Verdict: **APPROVED WITH MINOR CHANGES.**

The single required Phase 4 cleanup commit before QA Engineer Task 13 is the PE M1
budget wording reconciliation in `docs/perf/latency-budgets.md`. Every other finding is
deferrable to Phase 5 prep or Phase 11 polish. None of the findings is BLOCKING phase
close. The MEDIUM finding (PE M3) is documented as Phase 5 prep per the PE's framing
and the Code Reviewer's reading of the robustness of the user's CSV based measurement
methodology to the concern in the typical case.

The Code Reviewer hands back to the PM. The PM's next dispatch is the Phase 4 cleanup
commit (PE M1) followed by the QA Engineer Task 13 dispatch.
