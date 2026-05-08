# ADR 0003: IMU sensor type choice for the gait test

## Status

Accepted, 2026-05-07.

## Context

`SPEC.md` Section 7.1 step 1 prescribes that the gait pipeline capture three signals per sample: 3 axis user (gravity removed) acceleration, 3 axis angular velocity, and a device attitude quaternion. Android exposes several sensor types that could feed each of these channels, and the choices interact: a poor pick on one channel may force a poor pick on another. The Sensor Integration Engineer's Phase 4 task 5 (`agents/18-sensor-integration-engineer.md`) requires this ADR to record the chosen mapping and the rationale.

The relevant Android sensor types are:

1. `Sensor.TYPE_LINEAR_ACCELERATION`. Reports user (gravity removed) acceleration in the device frame. The platform performs the gravity removal using a fused estimate that combines the raw accelerometer with the gyroscope (and on most devices, hardware sensor fusion).
2. `Sensor.TYPE_ACCELEROMETER`. Reports raw acceleration in the device frame, gravity included.
3. `Sensor.TYPE_GYROSCOPE`. Reports angular velocity in the device frame.
4. `Sensor.TYPE_ROTATION_VECTOR`. Reports the platform fused orientation quaternion (device frame to world frame) as a 3 or 4 element vector per `SensorManager.getQuaternionFromVector`.

The gait pipeline (`dsp/GaitPipeline.kt`) consumes `linearAcceleration` (per `ImuSample`) and `rotationVector` to produce world frame channels via `rotateToWorld`. The from scratch Madgwick filter (`dsp/Madgwick.kt`, ADR 0002) consumes raw accelerometer plus gyroscope as a parallel orientation reference for the trial quality score residual; on a device that exposes `TYPE_ROTATION_VECTOR` the platform fused estimate is the primary orientation source and the Madgwick filter runs alongside for the residual.

Phase 3 close left one open contract gap (Resume notes item 1): when a device does not expose `TYPE_ROTATION_VECTOR`, `ImuSample.rotationVector` is null and `GaitPipeline.rotateToWorld` falls back to `Quaternion.IDENTITY`, which silently produces wrong features. Phase 4 closes that gap inside the `signals/` layer rather than inside `dsp/`.

## Decision

Register four sensor listeners in `signals/AndroidImuSource`:

1. `Sensor.TYPE_LINEAR_ACCELERATION` as the primary acceleration channel and the source clock for emissions.
2. `Sensor.TYPE_GYROSCOPE` as the angular velocity channel.
3. `Sensor.TYPE_ROTATION_VECTOR` as the primary orientation channel (when available on the device).
4. `Sensor.TYPE_ACCELEROMETER` as a fourth listener registered **only** when `Sensor.TYPE_ROTATION_VECTOR` is absent. On those rare devices the fallback path runs the from scratch Madgwick filter (`dsp/Madgwick.kt`) inline using the raw accelerometer plus gyroscope inputs, and stamps the resulting quaternion into the emitted `ImuSample.rotationVector`. On the common case (a device that exposes `TYPE_ROTATION_VECTOR`), the fourth listener is not registered and the source operates with three sensors.

All four listeners are registered with `samplingPeriodUs = 10_000` (100 Hz nominal). The linear acceleration sensor drives emissions; gyroscope, rotation vector, and (on the fallback path) accelerometer values are zero order held to the linear acceleration timestamp.

### Why the fallback uses TYPE_ACCELEROMETER, not TYPE_LINEAR_ACCELERATION

The Phase 4 plan's "Architectural rails locked in by this plan" rail 3 wording reads "fallback Madgwick path uses linear acceleration plus gyroscope inputs." The implementation in `signals/AndroidImuSource.kt` deliberately deviates from that wording: the fallback feeds the Madgwick filter the gravity included `TYPE_ACCELEROMETER` reading rather than the gravity removed `TYPE_LINEAR_ACCELERATION` reading. The reason is correctness, not optimization.

The Madgwick filter locks orientation against the gravity reference vector. The accelerometer correction term (Madgwick 2010, eq 25 to eq 33) measures the residual between the gyro integrated orientation's predicted gravity direction and the observed acceleration direction, and uses that residual to nudge the integrated orientation back into agreement with gravity. If the filter is fed gravity removed input it has no anchor: the predicted gravity direction has no observed counterpart in the input, and the correction term reduces to noise around zero. The filter then degenerates to pure gyro integration, drifts, and stops being useful as a reference for the platform fused estimate. Feeding `TYPE_ACCELEROMETER` (gravity included) to the filter is therefore mandatory for the filter to function at all.

This deviation only applies on the fallback path. On the common platform fused path (a device that exposes `TYPE_ROTATION_VECTOR`), the platform fused estimate is the primary orientation source, the gait pipeline reads `linearAcceleration` from `TYPE_LINEAR_ACCELERATION`, and `TYPE_ACCELEROMETER` is not registered.

The Code Reviewer's Phase 4 Task 12 review will reconcile the plan rail wording against this ADR; the implementation lands ahead of the plan rail wording fix because the implementation cannot produce a correct fallback orientation any other way.

## Alternatives considered

1. **Use `Sensor.TYPE_ACCELEROMETER` for the primary acceleration channel and remove gravity inside the application.** Rejected because the platform's gravity removal benefits from device specific hardware fusion that an application level subtraction (rotate the gyro integrated gravity vector and subtract) cannot match. The platform fused estimate also handles the cold start convergence period at boot, which an application level filter would have to replicate. Using `TYPE_LINEAR_ACCELERATION` is the simpler and more accurate choice on the common path.
2. **Skip `TYPE_ROTATION_VECTOR` entirely and run the from scratch Madgwick filter on every device.** Rejected. The platform fused estimate is more accurate when available because device manufacturers integrate magnetometer and barometer corrections that the IMU only Madgwick variant ignores. The parallel Madgwick filter still runs in the gait pipeline (`GaitPipeline.orientationQualitySignals`) for the trial quality score residual; the platform fused estimate is the primary, the from scratch filter is the second opinion.
3. **Skip the fallback path and reject sessions on devices without `TYPE_ROTATION_VECTOR`.** Considered but rejected. `TYPE_ROTATION_VECTOR` has been part of Android since API level 9 (2010), so the absence case is rare. Rejecting those sessions outright would silently exclude users on cheaper or older hardware from the gait test. Running the from scratch Madgwick inline costs a small amount of battery on those devices and produces a usable orientation estimate; the trade is worth it. The runbook (`docs/observability/sensor-runbook.md`) records observed devices in the fallback case.
4. **Run the from scratch Madgwick filter on every device, regardless of `TYPE_ROTATION_VECTOR` availability, and emit both quaternions.** This is the long term plan for the quality score residual described in `SPEC.md` Section 7.1 step 9 and ADR 0002. The Phase 4 implementation runs the from scratch filter inside `dsp/GaitPipeline.orientationQualitySignals` for every sample (per Phase 3 close), so the residual is already produced. The `signals/` layer does not need to run a second copy on the platform fused path; one is enough.

## Consequences

### Positive

- The application now depends on the device exposing `TYPE_LINEAR_ACCELERATION`, which has been part of Android since API level 9 and is universally available on the project's minimum SDK target (API 31, Android 12). The runtime risk of this dependency is effectively zero.
- The platform fused orientation is the primary on the common path, which gives the gait pipeline the best available orientation estimate and lets the from scratch Madgwick filter focus on its teaching exercise role (residual against the platform fused estimate).
- The fallback path closes the Phase 3 contract gap (Resume notes item 1): every emitted `ImuSample.rotationVector` is non null on every device, so `GaitPipeline.rotateToWorld` no longer needs an `IDENTITY` fallback.
- No new runtime permissions. The four sensor types used (`TYPE_LINEAR_ACCELERATION`, `TYPE_GYROSCOPE`, `TYPE_ROTATION_VECTOR`, `TYPE_ACCELEROMETER`) do not require any runtime permission. The Security Engineer veto on adding `INTERNET` or any other permission is preserved.

### Negative

- Devices missing `TYPE_ROTATION_VECTOR` pay a small battery overhead for the inline Madgwick filter and a small CPU overhead for the four sensor listener case. On the common case (the three sensor case) the cost is the same as a baseline IMU capture.
- The implementation has two code paths for orientation: the platform fused path and the inline Madgwick fallback path. Two paths means two surface areas to test. The Robolectric test in `signals/AndroidImuSourceTest.kt` covers both, including the case where `TYPE_ROTATION_VECTOR` is absent.

### Mitigations

- The runbook records every device where the fallback path triggered, so Phase 5 real walking course validation can confirm the fallback path produces accuracy comparable to the platform fused path. If a device shows poor accuracy on the fallback path, the runbook triggers a Phase 5 follow up to either tune the Madgwick beta for that device or add the device to a reject list.
- The four sensor case is rare. The runbook tracks it explicitly, so the project has data on how often the fallback path runs in practice.

## Revisit conditions

This decision should be revisited if any of the following hold:

1. Phase 5 walking course recordings show sample rate instability on the platform fused path that traces to the device's `TYPE_LINEAR_ACCELERATION` implementation. If a device produces unstable linear acceleration but stable raw accelerometer, the choice flips on that device.
2. Phase 5 walking course recordings show that the fallback path's accuracy is materially worse than the platform fused path. The fix in that case is to either reject sessions on those devices or to tune the Madgwick beta per device family.
3. A future Android version deprecates one of the four sensor types. None are currently deprecated; `TYPE_LINEAR_ACCELERATION`, `TYPE_GYROSCOPE`, `TYPE_ROTATION_VECTOR`, and `TYPE_ACCELEROMETER` are all stable APIs.
4. A future phase adds a magnetometer fused (marg) Madgwick variant. That variant would change the input requirements and the choice would need to be re evaluated.

## References

- `SPEC.md` Section 7.1 step 1 (capture requirements) and step 2 (orientation estimation).
- ADR 0002 (Madgwick from scratch). The fallback path described here uses the filter ADR 0002 specifies.
- `agents/18-sensor-integration-engineer.md` Phase 4 tasks 1 to 5.
- `docs/plans/phase-4-gait-test-module-integration.md` "Architectural rails locked in by this plan" rails 1, 3, and 4. Rail 3 wording will be reconciled with this ADR by the Code Reviewer's Task 12 review.
- Android `Sensor.TYPE_LINEAR_ACCELERATION`, `Sensor.TYPE_ACCELEROMETER`, `Sensor.TYPE_GYROSCOPE`, `Sensor.TYPE_ROTATION_VECTOR` reference documentation (developer.android.com).
- Madgwick S O H. 2010. An efficient orientation filter for inertial and inertial / magnetic sensor arrays. University of Bristol technical report. The accelerometer correction term that requires gravity included input is described in eq 25 to eq 33.
