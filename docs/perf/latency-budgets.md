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
