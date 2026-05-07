# STATUS, MS Neuro Battery

This file is the single source of truth for which phase is next. Read it at the start of every session.

## Next phase

**Phase 1: Foundation.** Not started. Begin with the mandatory check in protocol before dispatching agents. Five Citation Auditor vetoes from Phase 0 should be resolved during Phase 1 prep (see `docs/source/citation-audit-log.md`); two PM decisions are also pending user ratification (see Resume notes below).

## Phase status table

| # | Phase | Status | Completed on | Window cost | Notes |
|---|-------|--------|--------------|-------------|-------|
| 0 | Bootstrap setup | completed | 2026-05-07 | ~65% (within est. 60 to 75% range) | All 12 deliverables produced. Citation Auditor exercised 5 vetoes (deferred to Phase 1 cleanup) and the PM applied 2 session corrections (Oh et al. 2024 attribution, Givon 2009 gait reference values). Compliance Reviewer recommended a finalized README re read which is pending. |
| 1 | Foundation | not started |  | est. 70 to 85% | Data layer, TestModule protocol, BatteryOrchestrator, UI shell, mock test integration. Detailed plan in `docs/plans/phase-1-foundation.md` |
| 2 | Tap Test | not started |  | est. 30 to 45% | Bilateral tap test as first concrete TestModule |
| 3 | Gait Signal Processing | not started |  | est. 80 to 95% | Pure DSP module: Madgwick filter, Butterworth low pass, step detection, ZUPT stride length, feature extraction. Unit tested against synthetic ground truth |
| 4 | Gait Test Module Integration | not started |  | est. 60 to 75% | Sensor capture via SensorManager, Compose UI for the gait test, persistence wiring |
| 5 | Gait Validation Suite | not started |  | est. 70 to 85% | Synthetic ground truth tests, real walking course experiments, ICC analysis, validation writeup in README |
| 6 | Vision Test | not started |  | est. 40 to 55% | Sloan low contrast acuity test with ambient brightness check via CameraX |
| 7 | SDMT Test | not started |  | est. 35 to 50% | Symbol Digit Modalities Test, 90 second cognitive battery |
| 8 | Voice Test | not started |  | est. 50 to 65% | Reading passage capture via AudioRecord, acoustic feature extraction (jitter, shimmer, HNR, speaking rate, pause fraction) |
| 9 | Reporting | not started |  | est. 60 to 75% | Longitudinal trend charts using Vico, summary tables, comparison against reference ranges |
| 10 | PDF Export | not started |  | est. 40 to 55% | PdfDocument generation, Share Intent via FileProvider |
| 11 | Accessibility, Beta, Polish | not started |  | est. 60 to 75% | TalkBack audit, Dynamic Type, color contrast, beta cohort recruitment, Play Store internal testing track |

Status values: `not started`, `in progress`, `completed`, `bundled with phase N`, `paused`.

## Resume notes

**Phase 0 closed 2026-05-07. Open items for Phase 1 prep:**

1. **Citation Auditor vetoes (5 items, see `docs/source/citation-audit-log.md`)** to resolve before the relevant phase consumes them:
   - Bays 2015 tap test reference not located (blocks Phase 2 tap test design rationale).
   - Buckley 2020 *Sensors* gait review not located (blocks Phase 5 validation target rationale).
   - Mathiowetz 1985 journal name needs correction (the original 9HPT paper).
   - Sosnoff et al. 2014, PMID 25117855: first author should be Socie, not Sosnoff.
   - Rusz 2018 *Sleep Medicine* is Parkinson's specific, not MS specific (blocks Phase 8 voice biomarker rationale; replace with the 2025 *J Voice* MS systematic review).
2. **PM decisions pending user ratification:**
   - Backup posture: Security Engineer recommends `android:allowBackup="false"` and removing the data extraction rules reference, to prevent the user's session data from being included in cloud based Android Auto Backup. The current scaffold has `allowBackup="true"`.
   - SPEC.md Section 3 expansion: Patient Advocate recommends adding one sentence noting that the user population includes mobility aid users (cane, walker, wheelchair), since the current "mild gait instability" understates the variance the application will see.
3. **Compliance Reviewer re read of finalized README** is recommended before the wellness positioning is locked in. The Documentation Engineer applied all flagged drift fixes; a quick verification pass is the remaining step.
4. **Phase 1 plan reference correction:** during Phase 0 bootstrap the PM corrected 38 path references in `docs/plans/phase-1-foundation.md` from `~/src/MSBattery/` to `~/src/MS-Battery/`. The Phase 1 plan otherwise remains as authored before the iOS to Android pivot decision.

## Validation log

(Empty. The PM writes a one line entry here when a phase that includes validation work completes, with the achieved numbers. Phase 5 will populate this with stride length and cadence error percentages and ICC values.)

## How to update this file

The PM is the only role that writes here. Update at three moments:

1. **Start of phase.** Flip the row from `not started` to `in progress`. Update the "Next phase" line at the top to name this phase.
2. **Pause mid phase.** Flip the row to `paused`. Write a Resume notes entry that names the file and line of the last clean stopping point and the next concrete action when work resumes.
3. **Completion.** Flip the row to `completed`. Fill in the date in `Completed on` and the actual window cost in `Window cost`. Move the "Next phase" line to the next row in the table.

Never start, pause, or complete a phase without updating this file first. Other roles will read the wrong state.
