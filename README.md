# MS Neuro Battery

A native Android application that lets people living with Multiple Sclerosis self administer a short, sensor backed set of five tests once a week, track results longitudinally on device, and share a clinician facing PDF report.

The technical centerpiece is a gait analysis pipeline that turns 30 seconds of phone IMU data into stride length, cadence, step time variability, and stride asymmetry. Pipeline accuracy is validated against a measured walking course; methodology and error numbers documented in the project's Validation section.

## Problem

MS clinic visits typically happen every three to six months. Between visits, people living with MS often want a simple, self directed way to keep an objective record of how their walking, hand dexterity, vision, cognition, and speech are changing, so that they can bring something concrete to their next neurology appointment. Existing research apps in this space are study only and not available as a self administered self tracking application for the broader community. MS Neuro Battery aims to fill that gap as a personal record keeping tool, not as a diagnostic instrument. The full problem statement is in `SPEC.md` Section 2.

## Solution

A weekly five to ten minute session at home runs five short tests on the user's phone: a bilateral tap test, a 30 second walk, a low contrast vision test, a Symbol Digit Modalities Test, and a 30 second voice reading. Each test design mirrors a published clinical instrument; the application is not a substitute for any of those instruments and does not produce clinical interpretations. Results are stored on device. The user can generate a PDF report on demand and share it with their neurologist. Nothing leaves the device unless the user explicitly chooses to share an export.

## What this is not

This application is not a medical device. It does not diagnose or treat any condition. Results are not validated for any clinical use. Do not start, stop, or change any treatment based on these results. Share results with your neurologist for clinical decisions.

## Status

Phase 0 (bootstrap setup) is complete; Phase 1 (Foundation) is complete as of 2026-05-07. The application runs end to end against a mock test module on the Compose UI shell, with the Room data layer, the `TestModule` protocol, and the `BatteryOrchestrator` wired together. Real test modules are added in subsequent phases (Phase 2 is the bilateral tap test). See `STATUS.md` for the current state and `docs/plan.md` for the full multi phase plan.

## How it is built

The application is a native Android codebase in Kotlin and Jetpack Compose, organized into separated layers (Compose UI, Battery Orchestrator, Test Modules, Signals, Signal Processing, Data Store, Reporting). Source of truth documents:

- `SPEC.md` for the full design specification.
- `docs/architecture.md` for a layered overview and a pointer index into the rest of the documentation tree.
- `docs/plan.md` for the multi phase implementation plan.
- `STATUS.md` for the single source of truth on which phase is next.
- `docs/plans/phase-1-foundation.md` for the detailed Phase 1 plan.

### Running

Open `~/src/MS-Battery` in Android Studio Iguana or later, select an Android 12 or later emulator (or attach a physical device with USB debugging enabled), and press Run.

From the command line:

```
./gradlew :app:installDebug
adb shell am start -n com.mustafan4x.msbattery/.MainActivity
```

### Testing

```
./gradlew :app:testDebugUnitTest
```

This runs every JVM unit test, including the Robolectric Room repository tests and the orchestrator tests.

## Privacy

All data is stored on device. The application does not declare the `android.permission.INTERNET` permission. There is no cloud sync, no account, no analytics SDK, and no third party telemetry. Microphone audio is processed in memory and discarded after feature extraction unless the user explicitly opts in to retention. Camera frames from the vision test ambient brightness check are read once and discarded. Export through Android's Share Intent is the only outward facing action and it is fully user initiated. The full trust boundary analysis is in `docs/security/threat-model.md`; the regulatory positioning is in `docs/security/compliance-review.md`.

## Validation

Reserved for the validation numbers produced in Phase 5. Stride length error percent against a measured 25 meter walking course, cadence error percent, and test retest reliability (intraclass correlation coefficient) for the gait pipeline's primary features will be reported here once the experiments are complete. The methodology lives in `SPEC.md` Section 7.2 and the full report will live in `docs/source/validation-report.md`.

## Retention

Reserved for the beta cohort retention numbers produced in Phase 11. Day 1, day 7, day 14, and day 30 retention curves with reminders enabled will be reported here. The empirical floor used to size the design goal is the Galati et al. 2024 (JMIR Human Factors 11:e57033) day 30 figure of 30.8 percent with reminders versus 9.7 percent without; the design goal is to meet or exceed that floor.

## Acknowledgments

The retention design and the gait pipeline rationale draw on two published analyses of the Floodlight Open dataset:

- Oh et al. 2024, *Scientific Reports*, "Floodlight Open, an open access global longitudinal smartphone based study of multiple sclerosis: Cohort characteristics and study design" (DOI 10.1038/s41598-023-49299-4).
- Galati et al. 2024, *JMIR Human Factors* 11:e57033, on retention and engagement in the Floodlight Open US cohort.

Full citations and the specific findings the project relies on are in `docs/source/clinical-references.md`.

## Repository

https://github.com/Mustafan4x/MS-Battery
