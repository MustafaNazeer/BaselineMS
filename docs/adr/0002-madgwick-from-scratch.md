# ADR 0002: Madgwick orientation filter implemented from scratch rather than via library

## Status

Accepted, 2026-05-07.

## Context

SPEC.md Section 7.1 step 2 prescribes that the gait pipeline use Android's `Sensor.TYPE_ROTATION_VECTOR` (the platform fused orientation quaternion) as the primary orientation source. The same section explicitly motivates implementing a Madgwick (2010) filter from raw accelerometer plus gyroscope as a parallel teaching exercise, with the parallel filter's residual against the platform fused estimate feeding the trial quality score (`SPEC.md` Section 7.1 step 9, "orientation residual stable"). The filter is required; the question is how to acquire it.

Three sourcing options were considered:

1. **Roll our own implementation in Kotlin.** Approximately 80 lines of code: a quaternion field, a beta gain, an `update(gyro, accel, dt)` method that computes the gyro derived rate of change, the gradient correction from the accelerometer, and integrates with the supplied delta time. The math is the IMU only variant of Madgwick (2010), eq 11 to eq 33 of the original technical report.
2. **Depend on the open source Java AHRS port.** The reference Java implementation lives at github.com/xioTechnologies/Open-Source-AHRS-With-x-IMU. It implements both the IMU only and the marg (with magnetometer) variants. Pulling it as a Maven dependency (or as a vendored source file) is straightforward.
3. **Skip the parallel filter entirely; use only `TYPE_ROTATION_VECTOR`.** Drop the SPEC.md teaching exercise. The pipeline still produces orientation, just without the parallel reference for the quality score residual.

## Decision

Implement the Madgwick filter from scratch in Kotlin under `app/src/main/java/com/mustafan4x/baselinems/dsp/Madgwick.kt`.

The implementation is small (under 100 lines), parallels the published paper one to one, and preserves the project's principle of keeping the gait pipeline pure JVM Kotlin with no third party dependencies beyond the Kotlin standard library and `kotlin.math`. Two unit tests in `app/src/test/java/com/mustafan4x/baselinems/dsp/MadgwickTest.kt` exercise the static gravity convergence case and the pure gyro integration case at zero beta.

## Consequences

### Positive

- No new third party dependency in the DSP module. The `dsp/` package compiles as plain JVM code with only Kotlin standard library and `kotlin.math` imports, which is the architectural rail set in `docs/plans/phase-3-gait-signal-processing.md` ("The DSP package and the fixtures package depend only on Kotlin standard library and `kotlin.math`").
- The implementation matches the SPEC.md Section 7.1 step 2 teaching exercise framing: every line of the filter is inspectable, testable, and traceable to the published paper. This is the resume project's claim of clinical depth and the project's defense against the "this is a generic CRUD app" risk in `SPEC.md` Section 13.
- The parallel filter feeds the quality score residual against the platform fused estimate. When the two estimates disagree, that disagreement is observable and shapes the quality score per `SPEC.md` Section 7.1 step 9. A library only solution would produce one or the other estimate but not the residual.
- The filter is allocation free on the per sample hot path (the per call branch only allocates one new Quaternion to replace the field; primitive Doubles carry the rest). The Phase 3 Performance Engineer review (Task 15 in the Phase 3 plan) verifies this.

### Negative

- The from scratch implementation is one more module the project owns and maintains. Bugs in the math have to be found by us, not surfaced by upstream.
- A beta value tuning pass on real device recordings (Phase 5) may surface that the default beta of 0.1 is not appropriate. A library default would have the same property; we just have to tune our own copy.

### Mitigations

- The unit tests in `MadgwickTest.kt` exercise the canonical convergence cases (static gravity converges to upright; pure gyro integration about world Z returns the residual angle to near zero). Any future change to the filter code is gated by these tests.
- The Madgwick paper reference is captured in the source comment header so any future contributor can re derive the math from the exact reference.
- If Phase 5 surfaces accuracy gaps that reflect implementation subtleties rather than parameter tuning, the open source Java port (option 2 below) is a drop in cross check; the project can adopt it as a reference for differential testing without committing to a long term dependency.

## Alternatives rejected

- **Open source Java AHRS port at github.com/xioTechnologies/Open-Source-AHRS-With-x-IMU.** Mature, broadly used, and implements both the IMU and marg variants. Rejected for v1 because it adds a dependency that the project does not need (the IMU only variant is approximately 80 lines of Kotlin) and because the SPEC.md teaching exercise framing prefers a from scratch implementation. The port remains a viable cross check reference for Phase 5 differential testing if accuracy gaps surface; adopting it later is a one commit change.
- **Use only `Sensor.TYPE_ROTATION_VECTOR`.** Operationally simpler. Rejected because it eliminates the parallel reference that feeds the quality score residual per `SPEC.md` Section 7.1 step 9. Without the parallel filter, an orientation estimate that drifts cannot be detected at the per trial level (the platform fused estimate is treated as ground truth, so the project has no second opinion). The teaching exercise framing in `SPEC.md` Section 7.1 step 2 also explicitly prefers two estimates over one.

## Revisit conditions

This decision should be revisited if any of the following hold:

1. Phase 5 real walking course validation surfaces orientation tracking accuracy gaps that trace to the from scratch implementation rather than to parameter tuning. In that case, swap to the Java port and re run the validation suite.
2. A future phase adds a magnetometer fused (marg) variant to the project. The marg variant adds complexity that may justify a library dependency; the from scratch IMU only filter does not.
3. A maintenance burden surfaces (for example, repeated bugs in the quaternion math that the unit tests do not catch). At that point a library dependency trades the maintenance burden for a transitive dependency.

## References

- Madgwick S O H. 2010. An efficient orientation filter for inertial and inertial / magnetic sensor arrays. University of Bristol technical report. The IMU only variant in eq 11 to eq 33 is the basis for this implementation.
- `SPEC.md` Section 7.1 step 2 (the teaching exercise framing) and step 9 (the quality score residual factor).
- `docs/plans/phase-3-gait-signal-processing.md` Architectural decisions section ("Madgwick implementation: rolled from scratch per `SPEC.md` Section 7.1 step 2 teaching exercise").
- `docs/source/clinical-references.md` smartphone gait analysis section, which lists Madgwick 2010 alongside Skog et al. 2010 ZUPT and Bea T et al. 2025 (the open access systematic review of psychometric characteristics of smartphone based gait analyses, DOI 10.3390/jfmk10020133) as the project's primary DSP method and validation references. The Buckley et al. 2020 entry referenced in earlier project drafts was vetoed in the Phase 0 Citation Auditor audit (verdict A.8, paper unlocatable) and replaced with Bea T et al. 2025 during the Phase 0 to Phase 1 cleanup.
