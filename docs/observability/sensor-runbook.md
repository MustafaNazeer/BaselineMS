# Sensor runbook

**Status:** structure established in Phase 4. Per device entries arrive as the user runs the application on real Android devices.

## Purpose and scope

This runbook records the observed behavior of `signals/AndroidImuSource` on every Android device the project has exercised. Phase 4 closes the code side of the IMU capture path: the `signals/` layer registers `Sensor.TYPE_LINEAR_ACCELERATION`, `Sensor.TYPE_GYROSCOPE`, `Sensor.TYPE_ROTATION_VECTOR`, and on the fallback path `Sensor.TYPE_ACCELEROMETER` per ADR 0003 (`docs/adr/0003-sensor-type-choice.md`). The runtime question that Phase 4 cannot answer in unit tests is whether a real device actually delivers the requested 100 Hz sampling rate with low jitter and zero dropped windows over a 30 second capture. Real devices vary; the runbook is the source of truth for what each one does.

The runbook is read by:

1. The Performance Engineer at every gait pipeline review (Phase 4 Task 11, Phase 5, Phase 11).
2. The Sensor Integration Engineer when investigating a device specific bug.
3. The QA Engineer when triaging a quality score regression that traces back to capture rate.
4. Future Code Reviewers and contributors who need to understand why a device family is on a known gotcha list.

The runbook is appended to, never rewritten. Each entry is dated and identifies the device unambiguously (model string from `Build.MODEL`, Android version from `Build.VERSION.RELEASE`, API level from `Build.VERSION.SDK_INT`).

## Phase 4 acceptance budgets

These are the targets every device entry is judged against. They come from `docs/perf/latency-budgets.md` "Initial budgets" section (IMU sampling row) and are restated here for convenience.

1. **Observed sample rate within 5 percent of nominal 100 Hz.** That is, the mean inter sample delta over a 30 second capture must lie in the range 9.5 ms to 10.5 ms.
2. **Jitter under 5 ms p99.** The 99th percentile of `|delta_i minus 10 ms|` over the 30 second capture must be under 5 ms.
3. **Zero dropped windows over 30 seconds.** No 1 second sub window of the 30 second capture has a cumulative delta drift exceeding 5 percent of nominal (that is, no second of capture loses more than 50 ms of total elapsed time relative to the wall clock).

A device that meets all three budgets is recorded as PASS. A device that fails any one budget is recorded as FAIL with the specific budget called out, and the user decides whether to add the device to a reject list, fall back to a lower nominal sampling rate (50 Hz `SENSOR_DELAY_GAME`), or accept the degraded quality and let the per session quality score reflect it.

## Per device entry template

Use this template verbatim for each new device entry. Preserve the field order so future readers can scan entries quickly.

```
### <device model> on Android <version>

**Date observed:** YYYY-MM-DD
**Build model:** <Build.MODEL>
**Build manufacturer:** <Build.MANUFACTURER>
**Android version:** <Build.VERSION.RELEASE> (API <Build.VERSION.SDK_INT>)
**Sensor types present:** TYPE_LINEAR_ACCELERATION yes/no, TYPE_GYROSCOPE yes/no, TYPE_ROTATION_VECTOR yes/no, TYPE_ACCELEROMETER yes/no
**Capture path:** platform fused (3 listeners) | fallback Madgwick (4 listeners, see ADR 0003)
**Sampling period requested:** 10000 us (100 Hz nominal)

**Observed at 100 Hz target:**
- Mean inter sample delta (ms): <value>
- Standard deviation of inter sample delta (ms): <value>
- p50 inter sample delta (ms): <value>
- p99 inter sample delta (ms): <value>
- Min and max inter sample delta (ms): <min>, <max>
- Total samples collected over 30 seconds: <count> (nominal 3000)
- Jitter p99 against 10 ms nominal: <value> ms
- Dropped windows count over 30 seconds: <integer count of 1 second sub windows whose cumulative drift exceeds 5 percent>

**Verdict against Phase 4 acceptance budgets:** PASS | FAIL (with the failing budget called out)

**Notes:** any device specific quirks observed (warm up period, thermal throttling, sensor batching), pointer to a captured CSV trace if the user retained one, and the user's chosen action (accept, reject, fall back to 50 Hz).
```

## Sample rate measurement procedure

The user produces a sample rate measurement by running a real gait test on the target device. The captured raw sensor trace lives at `Context.filesDir/sensor_traces/<sessionId>/GAIT.csv.gz` per the Phase 4 plan's architectural rail 5. The trace's header line lists 14 columns; the relevant column for sample rate measurement is the first, `timestampNanos`. Inter sample delta is derived from successive timestamps.

A short shell pipeline produces the headline numbers from a captured trace. Replace `<path>` with the path to the gzipped CSV pulled off the device via Android Studio's Device File Explorer or `adb pull`.

```
gunzip -c <path> | awk -F, 'NR>1 { if (prev) print ($1-prev)/1000000; prev=$1 }' > deltas_ms.txt
sort -n deltas_ms.txt > deltas_sorted.txt
# Mean and standard deviation
awk '{ s+=$1; n++ } END { print "mean ms:", s/n }' deltas_ms.txt
awk '{ s+=$1; ss+=$1*$1; n++ } END { m=s/n; print "stddev ms:", sqrt(ss/n - m*m) }' deltas_ms.txt
# p50 and p99
n=$(wc -l < deltas_sorted.txt); awk -v n=$n 'NR==int(n*0.50) { print "p50 ms:", $1 } NR==int(n*0.99) { print "p99 ms:", $1 }' deltas_sorted.txt
# Total samples over the trace
echo "samples: $(($(wc -l < deltas_ms.txt) + 1))"
# Jitter p99 against 10 ms nominal
awk '{ d = $1 - 10; if (d < 0) d = -d; print d }' deltas_ms.txt | sort -n > jitter_sorted.txt
n=$(wc -l < jitter_sorted.txt); awk -v n=$n 'NR==int(n*0.99) { print "jitter p99 ms:", $1 }' jitter_sorted.txt
```

The dropped windows count is computed by walking the trace in 1 second buckets (group successive timestamps until the cumulative delta exceeds 1000 ms) and asserting that each bucket's actual elapsed time lies within 5 percent of 1000 ms. A reference implementation in pure shell is verbose; a 20 line Python or Kotlin script is cleaner. The user can write either one once their first device is in hand. The Phase 5 reviewer task that runs against real recordings should formalize the script and add it to this runbook.

The numbers above feed directly into the per device entry template's "Observed at 100 Hz target" block.

## Capture path identification

The runbook entry's "Capture path" line distinguishes the platform fused path (the common case) from the fallback Madgwick path (the rare case where `Sensor.TYPE_ROTATION_VECTOR` is absent on the device, per ADR 0003). The user can determine the path on the target device without inspecting code by running:

1. Open the application's debug or about screen (the Android Engineer wires a sensor list display in Phase 4 Task 7 if the project decides one is helpful; otherwise the user inspects the captured CSV's `rotationVectorW/X/Y/Z` columns and notes whether the values look like a continuous unit quaternion or like the from scratch Madgwick output).
2. Cross reference the device's published sensor list at developer.android.com or the manufacturer's product page; very few Android phones since 2012 lack `TYPE_ROTATION_VECTOR`.

If the capture path is the fallback path, the entry's "Sensor types present" line shows `TYPE_ROTATION_VECTOR no` and the entry's "Capture path" line shows `fallback Madgwick (4 listeners)`.

## Per device entries

(Empty as of Phase 4 close. The Performance Engineer and the user populate this section as devices are exercised. Phase 4's deliverable is the runbook structure plus the acceptance budgets; the entries are a Phase 5 user task per `docs/plans/phase-4-gait-test-module-integration.md` "Phase close" section.)

## Known device gotchas

(Empty as of Phase 4 close. Future entries describe device specific behaviors that affect IMU capture, with a one line summary, the affected device family, the workaround if any, and a pointer to the per device entry that first surfaced the gotcha.)

## Notes for the Sensor Integration Engineer

1. The runbook is the multi device sign off deliverable that Phase 4's `agents/18-sensor-integration-engineer.md` Phase 4 task 3 calls for. Headless agents cannot drive a real device; the user populates entries as the application reaches their hardware. The Performance Engineer's Phase 4 review (Task 11) verifies the measurement methodology in this runbook is sound; the actual measurements are a user driven validation task that defers to Phase 4 close, mirroring the Phase 1 and Phase 2 deferred emulator walkthrough convention.
2. If a device fails the Phase 4 acceptance budgets, the SIE's options are to (a) accept the degraded quality and let the per session quality score reflect it, (b) fall back to a lower nominal sampling rate (50 Hz `SENSOR_DELAY_GAME`) on that device family, or (c) add the device to a reject list. Option (c) is a last resort; the project's privacy posture and global Android user base argue against rejecting users by hardware.
3. The Phase 4 plan's architectural rail 4 ("Capture target is 100 Hz nominal") notes that actual rate varies per device per Android documentation. The runbook is where that variance is recorded and reasoned about.
