# ADR 0001: Android native (Kotlin and Jetpack Compose) for v1

## Status

Accepted, 2026-05-06.

## Context

The MS Neuro Battery requires deep integration with phone sensors (IMU at 100 Hz, microphone, camera) and on device persistence. The development environment available is Linux (no macOS).

Three platform options were evaluated:

1. **iOS native (Swift, SwiftUI, SwiftData, CoreMotion).** Strongest sensor APIs and mature health research app ecosystem. Blocked by the lack of macOS access; iOS builds and signing require Xcode on macOS. Cloud Mac options exist but add cost and friction.
2. **Cross platform (React Native or Flutter).** Allows code to live on Linux. React Native bridges the JS runtime and the native sensors awkwardly at high sampling rates; Flutter is better but still wraps platform sensors through plugins with reduced API surface. Both still require macOS or a cloud Mac for the iOS build path.
3. **Android native (Kotlin, Jetpack Compose, Room, SensorManager).** Builds entirely on Linux. Sensor APIs are equivalent in depth to iOS for the use cases this project needs. Bigger global Android user base in MS patient populations outside the US and Western Europe.

## Decision

Build v1 as a native Android application: Kotlin, Jetpack Compose with Material 3, Room with KSP, native `SensorManager`, `AudioRecord`, and `CameraX`. Distribute via Google Play (internal testing track for the beta cohort, then potentially production).

## Consequences

### Positive
- Full development pipeline runs on Linux (Android Studio is officially supported there).
- Sensor API depth is equivalent to iOS for IMU, audio, and camera use cases. No compromises on the gait pipeline or any other DSP work.
- Kotlin is a more transferable language than Swift for general engineering roles. Jetpack Compose is a modern, well documented declarative UI framework.
- Distribution path through Google Play has lower review friction than the App Store, especially for wellness category apps.
- The application's privacy posture (no `INTERNET` permission, no cloud sync, no account) is straightforward to enforce on Android.

### Negative
- iOS users cannot use the application until and unless an iOS port is funded later. The MS patient population in the US skews iOS, so a meaningful portion of potential users are excluded from v1.
- Apple Watch integration (potentially valuable for the future deferred nocturnal hypoglycemia project, less so for this one) is not in scope.

### Mitigations
- An iOS port is captured in `future-ideas.md` with concrete reentry conditions (after Phase 5 validation numbers are in hand and macOS access is available).
- The architecture in `SPEC.md` Section 5 is platform agnostic below the UI layer, so a future iOS port can reuse the test design, the data model, the DSP pipeline math, and the validation methodology one to one.

## Alternatives rejected

- **Flutter cross platform.** Deferred. The deep sensor work (Madgwick filter, ZUPT integration on a 100 Hz IMU stream) sits awkwardly behind Flutter's plugin abstraction, and would likely require writing native Android and iOS modules anyway, defeating the reuse benefit. The iOS build path still requires macOS.
- **React Native cross platform.** Rejected outright. The JavaScript bridge is poorly suited to high frequency sensor streams.
- **Wait for macOS access and build iOS first.** Rejected. The project value comes from shipping; an indefinite wait for Mac access trades certainty (we can build now) for a hypothetical (Mac comes later) and surrenders the global reach advantage of Android.
