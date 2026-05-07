# MS Neuro Battery

A native Android application that lets people living with Multiple Sclerosis self administer a five test neurological battery once a week, track results longitudinally on device, and share a clinician ready PDF report.

The technical centerpiece is a validated gait analysis pipeline that turns 30 seconds of phone IMU data into stride length, cadence, step time variability, and stride asymmetry, validated against a measured walking course.

## Status

Bootstrap complete. Phase 0 (project scaffold) is the next phase. See `STATUS.md` for the current state.

## Documents

- `SPEC.md`, the full specification.
- `STATUS.md`, the single source of truth for which phase is next.
- `GETTING-STARTED.md`, walkthrough for a fresh session.
- `CLAUDE.md`, project conventions (auto loaded by Claude Code).
- `docs/plan.md`, multi phase implementation plan overview.
- `docs/plans/phase-1-foundation.md`, detailed Phase 1 plan.

## Privacy

All data is stored on device. The application does not declare the `INTERNET` permission. There is no cloud sync, no account, no analytics, and no third party telemetry.

## Disclaimer

This application is not a medical device. It does not diagnose or treat any condition. Do not change your treatment based on its output. Share results with your neurologist for clinical decisions.

## Repository

https://github.com/Mustafan4x/MS-Battery
