# MS Neuro Battery Phase 5: Gait Validation Suite Implementation Plan

> **For agentic workers:** This plan is split into Part A (agent-doable prep, executed in the current session) and Part B (user-driven validation, deferred until the user has gathered real walking course recordings and runbook entries). Part A tasks are self contained: each lands a commit, the Phase 4 close test count of 102 grows by Task A1's new fixture coverage cases, and the prep can sit on `main` cleanly while the user gathers data. Part B tasks defer until the user returns with data; the plan documents what those tasks need so a future session has a clear picture.

**Goal:** Validate the gait pipeline against measured ground truth and publish the achieved numbers. This is the phase that earns the resume bullet. No phase after this can rely on aspirational numbers.

**Prerequisite for Part B:** the user records the 25 meter walking course experiment per Section 7.2 of `SPEC.md` (20+ trials by 3+ people of varying heights, video recorded, paper marker every 25 cm, three self selected paces of slow, normal, brisk). The user also runs the Phase 4 close multi device sample rate validation per `docs/observability/sensor-runbook.md` and populates per device runbook entries. A future Phase 5 session converts those recordings into the validation report and README numbers.

**Why split the phase:** Phase 5's headline deliverables (stride length error, cadence error, ICC) cannot be produced by headless agents because they depend on real human walking data. Doing the prep work now (expanding the synthetic fixture suite, locking in the statistical methodology, scaffolding the report so real numbers slot in cleanly) means that when the user returns with data, the analysis path is already laid out and the only work left is to slot in numbers and run the audits. This avoids burning a future window on prep work that could land today.

**Architecture:** No code architecture changes in Phase 5. Part A's Task A1 adds integration test cases against the existing `GaitPipeline` and `PreCannedFixtures`. Part A's Tasks A2 and A3 are documentation only.

**Tech Stack:** No new runtime or test dependencies. Part A's Task A1 uses the existing JUnit 4 + `kotlinx-coroutines-test` setup; the new test cases follow the pattern in `app/src/test/java/com/mustafan4x/msbattery/dsp/GaitPipelineIntegrationTest.kt`.

**Related spec:** `~/src/MS-Battery/SPEC.md` Section 7.2 (validation strategy: synthetic ground truth, real walking course, test retest reliability), Section 9 (full validation strategy), Section 16 (success criteria 2: published validation numbers).

**Related agent briefs:**
- `~/src/MS-Battery/agents/20-test-fixture-engineer.md` (Task A1 owner: expands synthetic fixtures and adds integration tests).
- `~/src/MS-Battery/agents/17-biostatistics-reviewer.md` (Task A2 owner: ICC variant justification and error metric definitions).
- `~/src/MS-Battery/agents/14-documentation-engineer.md` (Task A3 owner: validation report scaffolding).
- `~/src/MS-Battery/agents/02-signal-processing-engineer.md` (Part B reviewer; reconciles synthetic fixtures against real recordings).
- `~/src/MS-Battery/agents/09-qa-engineer.md` (Part B owner: designs and runs the experiments; phase close sign off).
- `~/src/MS-Battery/agents/01-clinical-validator.md` (Part B reviewer: ratifies validation methodology against published norms).
- `~/src/MS-Battery/agents/16-citation-auditor.md` (Part B reviewer: final citation audit).
- `~/src/MS-Battery/agents/21-compliance-reviewer.md` (Part B reviewer: regulatory drift on validation report wording).
- `~/src/MS-Battery/agents/15-clinical-outcomes-reviewer.md` (Part B reviewer: final user facing copy).
- `~/src/MS-Battery/agents/10-code-reviewer.md` (Part B reviewer at phase close).

**Platform note:** Local builds run with `JAVA_HOME=/snap/android-studio/209/jbr ./gradlew :app:testDebugUnitTest`. CI uses Temurin JDK 17. Part A is fully agent doable; Part B requires a real device and a real walking course.

---

## File Map

Files this plan creates or modifies:

```
~/src/MS-Battery/
├── SPEC.md                                              (no change)
├── STATUS.md                                            (PM updates Phase 5 row in two passes: Part A close, then Part B close)
├── README.md                                            (Part B updates: validation numbers, retention curves)
├── docs/
│   ├── plans/
│   │   └── phase-5-gait-validation-suite.md             (this file)
│   ├── qa/
│   │   ├── statistical-methods.md                       (NEW: Part A Task A2 by Biostatistics Reviewer)
│   │   ├── code-review-phase-5.md                       (NEW: Part B Code Reviewer's verdict)
│   │   └── regression-checklist.md                      (Part A: append Task A1 falsifiable conditions; Part B: append phase close section)
│   ├── source/
│   │   ├── validation-report.md                         (NEW: Part A Task A3 scaffolding; Part B populates result tables)
│   │   ├── citation-audit-log.md                        (Part B append: final audit on validation report and README)
│   │   └── clinical-references.md                       (Part B append: any new references the validation report cites)
│   ├── observability/
│   │   └── sensor-runbook.md                            (Part B append: per device entries from user)
│   └── perf/
│       └── latency-budgets.md                           (Part B append: Phase 5 PE review if relevant)
└── app/src/test/java/com/mustafan4x/msbattery/dsp/
    └── GaitPipelineIntegrationTest.kt                   (Part A Task A1: append three new fixtures' coverage)
```

The dsp/ source files are consumed unchanged in Part A. Part B does not modify dsp/ either; if synthetic fixture calibration against real signals indicates a pipeline tuning change, that is a separate Phase 5 follow up task tracked in this plan but executed under a Phase 5 cleanup commit owned by the SPE.

---

## Part A: Agent-doable prep (this session)

### Task A1: Expanded synthetic fixture coverage (closes Phase 3 CR L1)

**Owner:** Test Fixture Engineer.

**Files:**
- Modify: `app/src/test/java/com/mustafan4x/msbattery/dsp/GaitPipelineIntegrationTest.kt` (append three new test cases for `slowWalk`, `briskWalk`, and `severeAsymmetry`).
- No production code changes.

**Spec:** Phase 3 Code Reviewer LOW finding L1 noted that `PreCannedFixtures.slowWalk`, `briskWalk`, and `severeAsymmetry` had no `GaitPipelineIntegrationTest` coverage. The Test Fixture Engineer adds one test case per fixture that runs the fixture through the production `GaitPipeline.process` pipeline and asserts the recovered features against the fixture's ground truth metadata, mirroring the structure of the existing four cases for `healthyControlNormal`, `msTypicalNormal`, `mildAsymmetry`, and `noisyMsNormal`.

The existing tolerance bands (cadence within 5 percent, stride length within 5 percent, asymmetry index absolute error under 0.025) are the published Phase 3 acceptance numbers per the Phase 3 close entry in `STATUS.md`. The Test Fixture Engineer holds the new fixtures to the same bands. If a fixture cannot meet the band on the first run, the Test Fixture Engineer flags it with a specific finding (synthetic generator over emphasizes some cue, pipeline parameter that needs Phase 5 calibration, etc.) rather than weakening the assertion.

**Verification:**
- Run `JAVA_HOME=/snap/android-studio/209/jbr ./gradlew :app:testDebugUnitTest --rerun-tasks`.
- Expected: 105 tests (102 prior + 3 new), 0 failures, 0 errors, 0 skipped. If a fixture's recovered features fall outside the tolerance band, the TFE files a finding rather than committing a passing test that hides the gap.

**Commit message:** `fixtures(gait): GaitPipelineIntegrationTest coverage for slowWalk, briskWalk, severeAsymmetry (closes Phase 3 CR L1)`

### Task A2: ICC methodology documentation

**Owner:** Biostatistics Reviewer.

**Files:**
- Create: `docs/qa/statistical-methods.md`.

**Spec:** A canonical, peer reviewable methods document that locks in the statistical analysis path before real data arrives. The Biostatistics Reviewer writes the variant choice for ICC, the error metric definitions, and the confidence interval methodology that Part B will apply to the user's walking course recordings.

Sections the document MUST have:
1. **Purpose and scope.** What this document is, what Phase 5 Part B will reference from it.
2. **Test retest reliability via ICC.** The ICC variant choice (1,1 vs 2,1 vs 3,1; absolute agreement vs consistency) per Shrout and Fleiss 1979 and Koo and Li 2016. The Biostatistics Reviewer justifies the choice for this specific design (5 repeated weekly sessions on the same person within a short window; same rater; the rater is the application).
3. **Error metrics for stride length and cadence.** Mean absolute error (MAE), root mean square error (RMSE), and percent error vs ground truth. The choice of which to report, and why.
4. **Confidence interval methodology.** 95 percent confidence intervals on ICC and on stride length / cadence error percentages. Bootstrap vs analytic; sample size implications.
5. **Sample size and power considerations.** Given the SPEC's "20+ trials by 3+ people" target, the Biostatistics Reviewer comments on the achievable confidence interval width and any ICC threshold caveats.
6. **What Phase 5 Part B will produce.** A short list of the result tables the validation report will fill in and the source data each table comes from.

**References to cite (Biostatistics Reviewer verifies authenticity before citing):**
- Shrout, P.E. and Fleiss, J.L. 1979. Intraclass correlations: uses in assessing rater reliability. *Psychological Bulletin* 86(2):420 to 428.
- Koo, T.K. and Li, M.Y. 2016. A guideline of selecting and reporting intraclass correlation coefficients for reliability research. *Journal of Chiropractic Medicine* 15(2):155 to 163.

The Biostatistics Reviewer is welcome to cite additional references as needed; the Citation Auditor will audit the document in Part B.

**Verification:**
- Run the test suite once after the commit to confirm no compilation regression (the doc is markdown only). Expected 105 tests, 0 failures.

**Commit message:** `docs(qa): Phase 5 ICC methodology and error metric definitions`

### Task A3: Validation report scaffolding

**Owner:** Documentation Engineer.

**Files:**
- Create: `docs/source/validation-report.md`.

**Spec:** The validation report is the canonical writeup of Phase 5's measured numbers. Part A scaffolds the document so that Part B only fills in tables and prose, not structure. The Documentation Engineer drafts every section with the methodology fully written and the result tables present but with placeholder TBD entries that the user will replace.

Sections the document MUST have:
1. **Purpose.** What the report is, who reads it (the user themselves; the user's neurologist if they choose to share; future readers of the resume project).
2. **Pipeline summary.** A short paragraph describing the gait pipeline at a high level, with a pointer to `SPEC.md` Section 7 for details. No new technical content; this is a summary.
3. **Synthetic ground truth results.** Table populated from the `GaitPipelineIntegrationTest` numbers in the Phase 3 close `STATUS.md` row plus the three new fixtures from Task A1 of this plan. This table can be populated in Part A from the existing test output; the Documentation Engineer pulls the numbers from `STATUS.md` row 3 and the Task A1 commit's test report.
4. **Real walking course methodology.** Course design (25 meters straight line, paper marker every 25 cm, video recorded), participant requirements (3+ people of varying heights, three self selected paces), data collection procedure, video processing procedure (frame by frame step count, stopwatch validation). Cite `SPEC.md` Section 7.2.
5. **Real walking course results.** Tables for stride length error and cadence error vs ground truth, by participant and by pace. **Populated in Part B with TBD placeholders in Part A.**
6. **Test retest reliability methodology.** 5 repeated weekly sessions per participant within a short window. ICC computed per the methodology in `docs/qa/statistical-methods.md` (Task A2 output). Cite Task A2 by file and section.
7. **Test retest reliability results.** ICC table with 95 percent confidence intervals, by feature (cadence, stride length, step time variability, etc.). **Populated in Part B with TBD placeholders in Part A.**
8. **Discussion.** Comparison against published norms (Givon et al. 2009 Gait Posture; Comber et al. 2017 Gait Posture). Any caveats about the synthetic fixtures' realism (per the Phase 3 SPE design choices in `STATUS.md` Resume notes Phase 5 calibration item 10). **Populated in Part B; Part A leaves a structured placeholder.**
9. **Limitations.** What the validation does not cover: small sample, healthy controls only, single device, short recording window, etc. **Part A drafts this section in full because it is independent of the actual numbers.**
10. **References.** Bibliography. **Part A populates entries the document already cites; Part B appends any new references introduced by the discussion.**

**References Part A populates:**
- Givon, U., Zeilig, G., Achiron, A. 2009. Gait analysis in multiple sclerosis: characterization of temporal-spatial parameters using GAITRite functional ambulation system. *Gait Posture* 29(1):138 to 142.
- Oh, J. et al. 2024. Smartphone gait test discriminates MS from controls. *Scientific Reports*. (Cited in `SPEC.md` Section 7.1.1 and `docs/source/clinical-references.md`.)
- Comber, L., Galvin, R., Coote, S. 2017. Gait deficits in people with multiple sclerosis: a systematic review and meta analysis. *Gait Posture*.
- Madgwick, S.O.H. 2010. An efficient orientation filter for inertial and inertial / magnetic sensor arrays. (Per ADR 0002 and the Phase 5 calibration carryover P3.1; Citation Auditor verifies ahead of Part B.)
- Galati, J. et al. 2024. Floodlight Open retention analysis. *JMIR Human Factors* 11:e57033.
- Shrout, P.E. and Fleiss, J.L. 1979. (Cited from Task A2.)
- Koo, T.K. and Li, M.Y. 2016. (Cited from Task A2.)

The Documentation Engineer cross checks every reference against `docs/source/clinical-references.md` and the existing citation audit log. New references introduced by the validation report (vs already audited) are flagged for Part B's Citation Auditor pass.

**Verification:**
- Run the test suite once after the commit to confirm no regression. Expected 105 tests, 0 failures.

**Commit message:** `docs(validation): Phase 5 validation report scaffolding with methodology and TBD result tables`

---

## Part B: User-driven validation (deferred until user returns with data)

The following tasks are documented here so a future Phase 5 session has a clear picture. They cannot be executed by headless agents.

### Task B1: User collects data

The user runs:
1. The 25 meter walking course experiment (20+ trials, 3+ people, three paces, video recorded).
2. The 5 repeated weekly session test retest experiment per participant.
3. The multi device sample rate validation per `docs/observability/sensor-runbook.md`.

The user's deliverable for the session that picks up Part B is:
- The captured CSV files from each gait session (under `sensor_traces/<sessionId>/GAIT.csv.gz` on each test device).
- The video recordings of each walking trial.
- A short note describing each participant's height range, sex, MS status (healthy control vs MS, with consent), and any deviations from the protocol.

### Task B2: QA Engineer extracts ground truth from videos

Frame by frame step count and stopwatch validation against the video. Produces a CSV per trial with the ground truth step count and total distance walked.

### Task B3: Signal Processing Engineer runs the pipeline against the captured CSVs

Re run `GaitPipeline.process` on each captured trial offline (via a small JVM script, not the Android app) and emit the recovered features into a per trial CSV.

### Task B4: Biostatistics Reviewer computes errors and ICC per the methodology in `docs/qa/statistical-methods.md`

Stride length error, cadence error, ICC with confidence intervals. Outputs the result tables.

### Task B5: Documentation Engineer populates `docs/source/validation-report.md` result tables

Replaces every TBD placeholder with the Task B4 numbers.

### Task B6: Documentation Engineer updates `README.md`

Validation section reports the achieved numbers verbatim. Retention section reports the day 30 retention from the beta cohort if available; otherwise the README states the retention curve will be updated post Phase 11.

### Task B7: Test Fixture Engineer reconciles synthetic fixtures against real signals

Tunes the synthetic fixture parameters (forward propulsion peak, lateral sway phase, etc.) to match real captured signals. Updates `docs/qa/fixtures.md` with the calibration log.

### Task B8: Citation Auditor final audit

Verifies every reference in `docs/source/validation-report.md` and the README's validation section against the source. Closes the Phase 3 PARTIAL P3.1 (Madgwick 2010 eq 11 to eq 33 specificity).

### Task B9: Compliance Reviewer regulatory drift review

Reviews the validation report wording against FDA wellness guidance, HIPAA scope, and Google Play health app policy. The validation report must not make clinical claims.

### Task B10: Clinical Validator review

Verifies the validation methodology against published norms; ratifies the comparison against Givon 2009 and Comber 2017 reference values.

### Task B11: Clinical Outcomes Reviewer

Reviews the README's user facing validation summary for clarity and absence of misleading claims.

### Task B12: Code Reviewer Phase 5 verdict

Phase level review mirroring Phase 4's Task 12 format. Files at `docs/qa/code-review-phase-5.md`.

### Task B13: QA Engineer regression checklist plus Phase 5 sign off

Final sign off. The acceptance criteria from `SPEC.md` Section 16.2 are the falsifiable conditions: stride length within 5 percent and cadence within 3 percent of ground truth on at least 20 trials across 3+ people. If the achieved numbers miss the targets, the QA Engineer reports the actual numbers honestly per the SPEC's no aspirational claims rule, and the Code Reviewer's verdict is APPROVED WITH NOTES rather than REJECTED.

---

## Part A close (this session)

After Tasks A1, A2, A3 land:

1. PM verifies test count strictly grew from 102 to 105 with zero failures.
2. PM updates `STATUS.md` row 5: status remains `in progress` (not `completed`); window cost note updated with actual Part A cost; the table cell text notes Part B is pending user data.
3. PM updates the "Next phase" line at the top of `STATUS.md` to indicate Part A is complete and Part B is awaiting user data.
4. PM writes a Resume notes block under "For Phase 5 Part B" listing every Task B1 to B13 with the prerequisite from the user.

This plan is the authoritative reference for the future session that picks up Part B. The plan is not modified by Part B execution; the future session writes its task close commits per the same conventions Phase 4 used.

---

## Self review

PM ran the writing-plans-style self review:

1. **Spec coverage.** Every Phase 5 deliverable in `docs/plan.md` Phase 5 maps to a task: 25 meter walking course (Task B1, B2, B3); test retest reliability with ICC (Task B1, B4); synthetic ground truth expansion (Task A1; Task B7 calibration); validation report (Task A3 scaffolds, Task B5 populates); README update (Task B6); ICC variant justification (Task A2 specifies, Task B4 applies); citation audit (Task B8); compliance review (Task B9); biostatistics review (Task A2 ahead of data, Task B4 against data).
2. **Placeholder scan.** Part A tasks have no placeholders; Part B tasks intentionally read as outlines because the user's data shape is not yet known. The validation report scaffolding (Task A3) leaves explicit TBD entries that Task B5 fills in.
3. **Type consistency.** No new types introduced. Existing `GaitPipeline`, `GaitFeatures`, `PreCannedFixtures` referenced by name match the Phase 3 close codebase.

The Part A slice is sized at roughly 25 to 35 percent of a Max plan window: A1 is a small TDD addition, A2 and A3 are documentation tasks. The user's current window has room for the slice with the mandatory check-in already answered.
