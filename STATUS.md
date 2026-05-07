# STATUS, MS Neuro Battery

This file is the single source of truth for which phase is next. Read it at the start of every session.

## Next phase

**Phase 1: Foundation.** In progress (started 2026-05-07). Mandatory check in protocol completed; user answered fresh window, more than 4 hours to reset, continue full phase. Phase 0 Resume notes 1, 2, and 4 (Citation Auditor vetoes, PM decisions, plan path corrections) were closed by commit c49847e. Resume note 3 (Compliance Reviewer README re read) is the only Phase 0 carryover and is dispatched as the first Phase 1 prep task.

## Phase status table

| # | Phase | Status | Completed on | Window cost | Notes |
|---|-------|--------|--------------|-------------|-------|
| 0 | Bootstrap setup | completed | 2026-05-07 | ~65% (within est. 60 to 75% range) | All 12 deliverables produced. Citation Auditor exercised 5 vetoes (deferred to Phase 1 cleanup) and the PM applied 2 session corrections (Oh et al. 2024 attribution, Givon 2009 gait reference values). Compliance Reviewer recommended a finalized README re read which is pending. |
| 1 | Foundation | in progress |  | est. 70 to 85% | Data layer, TestModule protocol, BatteryOrchestrator, UI shell, mock test integration. Detailed plan in `docs/plans/phase-1-foundation.md`. Started 2026-05-07. |
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

**Phase 1 in progress, started 2026-05-07.** Dispatch sequence:

1. Compliance Reviewer re read of the finalized `README.md` (Phase 0 carryover).
2. Android Engineer (lead) executes Tasks 2 to 11 of `docs/plans/phase-1-foundation.md` with TDD discipline: data layer, TestModule, MockTestModule, BatteryOrchestrator, UI shell, integration test.
3. Database Administrator reviews the Room schema and DAOs.
4. Documentation Engineer updates `README.md` for Phase 1 (delta only; Phase 0 sections are preserved).
5. Patient Advocate reviews onboarding and home screens.
6. Code Reviewer reviews the full Phase 1 PR.
7. QA Engineer signs off.

**Phase 0 closed 2026-05-07.** Carryover items 1 (Citation Auditor vetoes), 2 (PM decisions on backup posture and SPEC Section 3), and 4 (Phase 1 plan path corrections) were all closed by commit c49847e. Carryover item 3 (Compliance Reviewer README re read) is in flight as the first Phase 1 dispatch.

## Validation log

(Empty. The PM writes a one line entry here when a phase that includes validation work completes, with the achieved numbers. Phase 5 will populate this with stride length and cadence error percentages and ICC values.)

## How to update this file

The PM is the only role that writes here. Update at three moments:

1. **Start of phase.** Flip the row from `not started` to `in progress`. Update the "Next phase" line at the top to name this phase.
2. **Pause mid phase.** Flip the row to `paused`. Write a Resume notes entry that names the file and line of the last clean stopping point and the next concrete action when work resumes.
3. **Completion.** Flip the row to `completed`. Fill in the date in `Completed on` and the actual window cost in `Window cost`. Move the "Next phase" line to the next row in the table.

Never start, pause, or complete a phase without updating this file first. Other roles will read the wrong state.
