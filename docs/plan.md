# Implementation plan, BaselineMS

This document is the multi phase plan overview. Each phase has its own scope, agent roster, and rough window cost. The detailed plan for Phase 1 lives at `docs/plans/phase-1-foundation.md`. Subsequent phases get their own detailed plans at `docs/plans/phase-N-<name>.md` written by the PM at the start of each phase, after the phase boundary check-in.

The cardinal rule: every phase ends with the mandatory check-in protocol described in `CLAUDE.md`. No phase begins without the user answering the three check-in questions.

## Phase 0: Bootstrap setup

**Window cost:** rough estimate 50 to 60 percent.

**Goal:** Move from documentation only state to "Android Studio project exists, threat model exists, accessibility tokens drafted, GitHub remote wired, README finalized, architecture stub written."

**Agents:** PM (this session), DevOps Engineer, Security Engineer, UI/UX Designer, Documentation Engineer, Clinical Validator, Citation Auditor, Compliance Reviewer, Patient Advocate (initial review).

**Deliverables:**
- `app/` Android Studio project scaffolded per Task 1 of `docs/plans/phase-1-foundation.md`.
- Git repo initialized at the project root, GitHub remote `https://github.com/MustafaNazeer/BaselineMS` wired, initial commit pushed.
- GitHub Actions CI workflow that runs `./gradlew :app:testDebugUnitTest` on every PR (Linux runners are sufficient for unit tests).
- `docs/security/threat-model.md` with attack surface enumeration and `docs/security/hardening-checklist.md` with concrete enforceable rules (no INTERNET permission, ProGuard or R8 enabled in release, Android Keystore for any future secrets).
- `docs/design/tokens.md` with Material 3 baseline plus accessibility tokens (tap targets, contrast, type scale).
- `docs/architecture.md` written as a high level overview of the layered architecture from `SPEC.md` Section 5.
- `README.md` finalized.
- Clinical Validator's first read of `SPEC.md` Section 6 against current literature, with notes appended to `docs/source/clinical-references.md`.
- **Citation Auditor's** first audit of every entry in `docs/source/clinical-references.md` and every cited number in `SPEC.md`; results in `docs/source/citation-audit-log.md`.
- **Compliance Reviewer's** initial review of `SPEC.md` Section 4 (non goals), Section 10 (privacy), and `README.md` positioning against FDA wellness guidance, HIPAA scope, and Google Play health app policy; results in `docs/security/compliance-review.md`.
- **Patient Advocate's** initial framing notes on the patient population in `docs/qa/patient-advocate-reviews.md`.
- **UI/UX Designer's retention design specification** in `docs/design/retention.md`. Covers reminder copy and timing rules (cap 2 per day, never after 7 PM, only fire on missed sessions, friendly tone with periodic acknowledgment messages), default schedule (one weekly reminder anchored to a user chosen slot during onboarding), the onboarding flow targeting at least 75 percent registration to activation conversion, and gait test contextual skip rules (defer when not in a walkable space). Empirical retention floor is the Galati et al. 2024 (JMIR Human Factors 11:e57033) day 30 number of 30.8 percent with reminders versus 9.7 percent without; BaselineMS's design goal is to meet or exceed 30.8 percent day 30 retention. Adherence design is treated as a Phase 0 first class concern, not a Phase 11 polish item, because the patient owned PDF for the neurologist (a clinical artifact Floodlight Open did not provide) is the strongest available retention lever in this population per Oh et al. 2024 finding that clinical supervision was the single largest persistence driver.

**End of phase:** run the mandatory check-in protocol before starting Phase 1.

## Phase 1: Foundation

**Window cost:** rough estimate 70 to 85 percent.

**Goal:** Run the weekly battery flow end to end against a mock test, with results persisted to Room and visible in history.

**Agents:** PM, Android Engineer (lead), Data Engineer, Database Administrator (review), QA Engineer, Code Reviewer, Documentation Engineer, Patient Advocate (reviews onboarding and home screens).

**Detailed plan:** `docs/plans/phase-1-foundation.md`. Tasks 2 through 12 of that plan execute in this phase (Task 1 was completed in Phase 0 as the project scaffold).

**End of phase:** check-in.

## Phase 2: Tap Test

**Window cost:** rough estimate 30 to 45 percent.

**Goal:** Replace the mock test with the bilateral tap test as the first concrete `TestModule`. Validates the architecture end to end against a real test.

**Agents:** PM, Android Engineer, Clinical Validator (reviews tap test design vs 9HPT literature), Citation Auditor (verifies any added 9HPT citations), Patient Advocate (reviews tap test from MS patient perspective), Code Reviewer, QA Engineer.

**Deliverables:**
- `BilateralTapTest` module conforming to `TestModule`.
- 30 second alternating tap UI on two on screen targets, dominant then non dominant hand.
- Outputs: tap rate, inter tap CV, dominant vs non dominant asymmetry, miss rate, quality score.
- Unit tests on tap detection and feature math.
- Compose UI smoke test for the tap test screen.

**End of phase:** check-in.

## Phase 3: Gait Signal Processing

**Window cost:** rough estimate 80 to 95 percent.

**Goal:** Implement the full gait DSP pipeline as a pure Kotlin module that takes IMU samples in and returns gait features out, with no Android sensor dependencies. Heavily TDD'd against synthetic ground truth.

**Agents:** PM, Signal Processing Engineer (lead), Test Fixture Engineer (lead on synthetic IMU fixtures), Citation Auditor (verifies cited DSP method references), Performance Engineer, Code Reviewer, QA Engineer.

**Deliverables:**
- `core/dsp/` module (or equivalent) with: Butterworth low pass filter, Madgwick orientation filter, gravity removal, peak step detection, stride pairing, ZUPT stride length integrator, feature extractor.
- **Test Fixture Engineer's parameterized synthetic IMU generator** plus a documented set of pre canned fixtures (slow, normal, brisk walks; symmetric, asymmetric; clean, noisy) in `app/src/test/.../fixtures/` and `docs/qa/fixtures.md`.
- Unit tests on synthetic IMU traces with known stride parameters; target stride length within 2 percent on clean synthetic signals.
- Performance characterization: pipeline runs in real time on a Pixel 6 class device.
- ADR documenting the choice between rolling our own Madgwick vs depending on an existing library.

**End of phase:** check-in.

## Phase 4: Gait Test Module Integration

**Window cost:** rough estimate 60 to 75 percent.

**Goal:** Wire the gait DSP module into a `TestModule` with sensor capture, Compose UI, and persistence.

**Agents:** PM, Sensor Integration Engineer (lead on the `signals/` layer), Android Engineer (lead on the test module UI and orchestrator wiring), Signal Processing Engineer (reviewer), Patient Advocate (reviews the gait test from MS patient perspective), Performance Engineer, Code Reviewer, QA Engineer.

**Deliverables:**
- **Sensor Integration Engineer's `signals/` layer** that captures `Sensor.TYPE_LINEAR_ACCELERATION`, `Sensor.TYPE_GYROSCOPE`, `Sensor.TYPE_ROTATION_VECTOR` at 100Hz via `SensorManager`, exposed as a typed `Flow<ImuSample>`. Includes per device sample rate verification and runbook entries in `docs/observability/sensor-runbook.md`.
- `GaitTest` module: pre test instructions, countdown, 30 second walk capture, post test feedback.
- Result persistence including raw sensor trace as gzipped CSV in app private files dir, referenced from `TestResultEntity.rawSensorRelativePath`.
- Performance Engineer signs off on actual sample rate stability across at least three real Android devices.

**End of phase:** check-in.

## Phase 5: Gait Validation Suite

**Window cost:** rough estimate 70 to 85 percent.

**Goal:** Validate the gait pipeline against measured ground truth. This is the phase that earns the resume bullet. No phase after this can rely on aspirational numbers.

**Agents:** PM, Signal Processing Engineer, **Biostatistics Reviewer (lead reviewer on every statistical method, ICC choice, error metric, and confidence interval)**, Test Fixture Engineer (reviewer; calibrates synthetic fixtures against real captured signals), QA Engineer (designs and runs the experiments), Clinical Validator, Citation Auditor (audits every cited reference in `docs/source/validation-report.md` and the `README.md` validation section), Compliance Reviewer (reviews validation report wording for regulatory drift), Clinical Outcomes Reviewer, Documentation Engineer.

**Deliverables:**
- 25 meter walking course experiment: 20 plus trials by 3 plus people of varying heights, video recorded for ground truth, paper marker every 25 cm. Stride length and cadence error vs ground truth computed and reported.
- Synthetic ground truth test suite expanded; **Test Fixture Engineer reconciles fixture realism against real captured signals.**
- Test retest reliability: 5 sessions on same person within 48 hours, ICC computed for primary features. **ICC variant selection (1,1 vs 2,1 vs 3,1; absolute agreement vs consistency) is justified in `docs/qa/statistical-methods.md` by the Biostatistics Reviewer.**
- `docs/source/validation-report.md` with methodology and full numbers.
- `README.md` updated with the achieved numbers, no rounding up, no aspirational claims. If a target is missed, the README reports the actual number.

**End of phase:** check-in.

## Phase 6: Vision Test

**Window cost:** rough estimate 40 to 55 percent.

**Goal:** Implement the Sloan style low contrast acuity test with ambient brightness check via CameraX.

**Agents:** PM, Android Engineer, Sensor Integration Engineer (reviewer on the CameraX integration), Clinical Validator (reviews letter rendering and contrast levels against Sloan standard), Citation Auditor (verifies any newly cited Sloan references), Patient Advocate (reviews vision test from a low contrast sensitive patient perspective), Clinical Outcomes Reviewer, Code Reviewer, QA Engineer.

**Deliverables:**
- Low contrast letter renderer at 100, 25, 5, and 1.25 percent contrast.
- CameraX preview frame analyzer for ambient brightness; reject test below or above thresholds.
- Optional: face size based viewing distance estimate via ML Kit face detection.
- Unit tests on scoring and quality score factors.

**End of phase:** check-in.

## Phase 7: SDMT Test

**Window cost:** rough estimate 35 to 50 percent.

**Goal:** Implement the Symbol Digit Modalities Test as a 90 second cognitive battery.

**Agents:** PM, Android Engineer, Clinical Validator (reviews symbol set choice against published SDMT digital adaptations), Citation Auditor (verifies SDMT references), Patient Advocate (reviews SDMT from a cognitively fatigued patient perspective), Clinical Outcomes Reviewer, Code Reviewer, QA Engineer.

**Deliverables:**
- Symbol to digit mapping key persistent on screen.
- Sequence presenter and numeric keypad input.
- Outputs: correct count, response time mean and SD, error rate.
- Quality score that rejects sessions with long pauses suggesting interruption.

**End of phase:** check-in.

## Phase 8: Voice Test

**Window cost:** rough estimate 50 to 65 percent.

**Goal:** Capture a 30 second reading sample and extract acoustic features (jitter, shimmer, harmonics to noise ratio, speaking rate, pause fraction).

**Agents:** PM, Signal Processing Engineer (lead for acoustic features), Sensor Integration Engineer (lead on the `AudioRecord` integration in the `signals/` layer), Test Fixture Engineer (lead on synthetic audio fixtures), Biostatistics Reviewer (reviews acoustic feature distribution analyses), Android Engineer, Clinical Validator, Citation Auditor (verifies cited voice biomarker references), Patient Advocate (reviews voice test from a bulbar function impaired patient perspective and verifies the reading passage is appropriate), Code Reviewer, QA Engineer.

**Deliverables:**
- `AudioRecord` capture at 44.1 kHz mono via the Sensor Integration Engineer's `signals/` layer.
- DSP module for acoustic features in pure Kotlin (or via TarsosDSP if the ADR concludes that is the right choice).
- **Test Fixture Engineer's synthetic audio fixture set** (clean voice, mild dysarthria, severe dysarthria, fatigued speech) with documented ground truth and realism rationale in `docs/qa/fixtures.md`.
- Reading passage on screen.
- Voice activity detection and quality score.
- ADR for the TarsosDSP vs roll our own decision.

**End of phase:** check-in.

## Phase 9: Reporting

**Window cost:** rough estimate 60 to 75 percent.

**Goal:** Longitudinal trend visualization. Charts per test feature with reference ranges where they exist.

**Agents:** PM, Android Engineer, Data Engineer, UI/UX Designer, Documentation Engineer.

**Deliverables:**
- Compose charts using Vico for cadence, stride length, tap rate, SDMT correct count, low contrast acuity, voice features.
- Reference range overlays where published norms exist (informational, not diagnostic).
- Empty state and sparse data handling.

**End of phase:** check-in.

## Phase 10: PDF Export

**Window cost:** rough estimate 40 to 55 percent.

**Goal:** Generate a clinician ready PDF summarizing trends and latest values, sharable via Android Share Intent.

**Agents:** PM, Android Engineer, UI/UX Designer (reviews the PDF layout), Documentation Engineer (drafts standard cover language and disclaimer text on the PDF).

**Deliverables:**
- `PdfDocument` based generation.
- FileProvider configuration for Share Intent.
- CSV export option alongside PDF.
- Disclaimer block on every PDF.

**End of phase:** check-in.

## Phase 11: Accessibility, Beta, Polish

**Window cost:** rough estimate 60 to 75 percent.

**Goal:** Ship the application to Google Play internal testing track for the first external beta cohort.

**Agents:** PM, Accessibility Specialist, Patient Advocate (final patient perspective sweep and beta cohort briefing review), QA Engineer, DevOps Engineer (signing, Play Store internal track), Security Engineer (final manifest and dependency audit), **Compliance Reviewer (final regulatory review of every user facing artifact, beta consent text, and Play Console listing)**, Citation Auditor (final audit of every citation in user facing artifacts), Clinical Outcomes Reviewer (final user facing copy review).

**Deliverables:**
- Full TalkBack pass on every screen.
- Dynamic Type at 200 percent works.
- Color contrast meets WCAG AA on every text element.
- Keystore generated and stored securely; signing config wired.
- Play Console listing drafted.
- Internal testing track populated with at least one healthy control beta tester.

**End of phase:** check-in. After this phase, the application is shipped in the sense that meaningful real users have it.

## Coordination rules

- The PM owns the plan and updates `STATUS.md` at every transition.
- Specialists do not recursively dispatch. If they need work outside their role, they report it back to the PM, who dispatches the next agent.
- No phase starts without the mandatory check-in protocol.
- No phase closes with red tests, missing security review where required, or missing documentation deliverables for that phase.
- The Code Reviewer reviews every PR before merge. The QA Engineer signs off before phase close.
- **Veto power.** The following agents can block phase progress, with override only by the user:
  - Clinical Validator: misrepresented test designs.
  - Citation Auditor: misattributed or hallucinated citations.
  - Biostatistics Reviewer: incorrect statistical methods.
  - Security Engineer: privacy or attack surface regressions, including any change adding the `INTERNET` permission.
  - Accessibility Specialist: WCAG 2.1 AA regressions.
  - Clinical Outcomes Reviewer: misleading user facing clinical content.
  - Compliance Reviewer: regulatory drift (FDA, HIPAA, GDPR, Google Play).

## What lives outside this plan

The following are explicitly deferred and tracked in `future-ideas.md`, not here:

- iOS port.
- EHR integration.
- FDA pathway.
- Custom UI beyond Material 3 plus accessibility tokens.
- Multi language support.
- Additional clinical tests beyond the v1 five.
- SQLCipher database encryption.
