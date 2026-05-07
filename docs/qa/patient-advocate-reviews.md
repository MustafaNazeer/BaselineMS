# Patient Advocate reviews, MS Neuro Battery

## Purpose

This file is the append only review log produced by the Patient Advocate role (agent 19). It exists so that every screen, every test module, and every adherence mechanism in the application is evaluated, before it ships, against the lived reality of a person with Multiple Sclerosis using the application at home, on a phone, possibly fatigued, possibly with cognitive fog, possibly without a caregiver beside them.

The Patient Advocate is distinct from the Accessibility Specialist (who measures WCAG 2.1 AA conformance, TalkBack behavior, and contrast tools), the UI/UX Designer (who chooses Material 3 tokens and interaction patterns), and the Clinical Outcomes Reviewer (who confirms a neurologist would interpret the output correctly). The Patient Advocate fills a different gap: a screen can be technically accessible, visually polished, and clinically accurate, while still being unusable by a tired person with mild cog fog on a Tuesday afternoon. This log catches that.

## Append only convention

Entries in this file are append only. Old entries are never edited or deleted, even when the Patient Advocate's verdict is later overruled or revised. If a verdict changes, a new entry is appended that names the prior entry by date and explains the revision. This preserves the audit trail of patient perspective decisions across the life of the project.

The Phase 0 framing notes section below is the lens through which all subsequent per screen entries are written. It can be amended only by appending a dated revision block at the end of this file, never by editing the original framing.

## Phase 0 framing notes

These notes were written on 2026-05-07 as the first Patient Advocate deliverable. They establish the persona, the MS related concerns, the evidence base, and the standing objections that all later reviews will reference.

### Target user persona, expanded from SPEC.md Section 3

Per SPEC.md Section 3, the primary user is a person living with MS, broadly between 20 and 60 years of age, who owns an Android phone running Android 12 or later and is comfortable with basic smartphone tasks. The spec further notes the user "may have mild visual impairment, mild cognitive fatigue, mild hand tremor or weakness, and mild gait instability" and that the application "must remain usable across all of these."

The Patient Advocate expands that minimal sketch into a working persona for review purposes. The expansions below are framing by the Patient Advocate, not direct quotes from the spec, and are flagged as such.

1. **Age and life stage (Patient Advocate framing).** MS is most commonly diagnosed between roughly 20 and 50 years of age, with a population skew toward women. The persona is therefore most often working age, often holding paid employment, frequently a parent or caregiver, frequently the person responsible for household logistics. The application is competing with that life load for five to ten minutes a week, not slotting into idle time. Per Galati 2024 (clinical-references.md), "competing life demands" was one of the quantified quit clusters in the Floodlight Open cohort.

2. **Sensory profile (grounded in SPEC.md plus standard MS symptomatology).** Mild visual impairment per the spec. The MS specific subtypes that matter for this application are reduced low contrast sensitivity (often a residue of optic neuritis, the very symptom the Sloan vision module is designed to track), occasional diplopia, and visual fatigue under sustained reading. Bright screens at full luminance can also exacerbate symptoms in heat sensitive patients (see point 6 below).

3. **Motor profile (grounded in SPEC.md plus standard MS symptomatology).** Mild hand tremor or weakness per the spec. The Patient Advocate flags two specific subtypes for the persona: action tremor that worsens during a precision task such as tapping a small target, and proprioceptive degradation that makes the user genuinely uncertain whether their finger landed where they intended. Both interact badly with small tap targets, narrow on screen affordances, and tests scored on a millisecond budget.

4. **Cognitive profile (grounded in SPEC.md plus standard MS symptomatology).** Mild cognitive fatigue per the spec. The Patient Advocate names two specific cognitive subtypes that any UX must tolerate: information processing speed reduction (the symptom the SDMT directly measures), and working memory load (the symptom that makes multi step instructions on a single screen fail). Cog fog is also worse later in the day and worse on hot days, which couples to point 6.

5. **Gait and mobility profile (grounded in SPEC.md plus standard MS symptomatology).** Mild gait instability per the spec. The Patient Advocate flags that "mild" understates the population variance the application will see in production: real MS users of an Android application will include people who use a cane, a walker, a rollator, forearm crutches, or a wheelchair some or all of the time. The application's gait test design assumes the user can stand and walk thirty seconds in a straight line with the phone in a front pocket. Per Galati 2024, this exact assumption was the most common physical or contextual barrier reported by Floodlight Open users.

6. **Heat sensitivity (Patient Advocate framing, grounded in standard MS symptomatology, Uhthoff phenomenon).** A meaningful fraction of people with MS experience symptom worsening with even modest core temperature elevation. This matters for the application in two non obvious ways: a held phone with the screen at full brightness for ten minutes is a heat source against the hand; and the gait test asks the user to walk for thirty seconds, which is enough exertion to produce symptom worsening in some users on a warm day.

7. **Environmental and contextual reality (Patient Advocate framing).** The user is not in a clinic. They are in a kitchen, a hallway, a parked car, a waiting room, a bedroom. Background noise is variable. Lighting is variable. They may be standing, sitting, lying down, or in bed during a flare. The application must treat these as the default operating environment, not as edge cases.

8. **Energy budget and the "bad MS day" (Patient Advocate framing).** Patients with MS describe days where everything takes more effort, where speech slurs, where vision blurs, where balance is off, where the body refuses to be a reliable instrument. On such days the patient may still want to record a session, precisely because that day is clinically relevant data the neurologist should see. The application must accept a session run on a bad day, flag the quality score honestly, and never punish the user for the recording being noisier than usual.

### MS related concerns to evaluate UI and test design choices against

These are the standing concerns the Patient Advocate will apply in every later review entry. Each one is named here, with the evidence base where available, and with a flag distinguishing evidence from Patient Advocate framing.

1. **Fatigue tolerance.** Concrete tests per Galati 2024: total session duration, individual test duration, number of repetitive sub trials per test. The Floodlight 2MWT was the single most quit prone test in the cohort per Galati 2024 (lowest day 1 retention of any test, 32.34 percent without reminders versus 40.7 percent with). The 30 second walk in this project's design is a deliberate response to that finding (per SPEC.md Section 7.1.1) and the Patient Advocate concurs with it. The Patient Advocate will, however, push back if any other test silently grows past two minutes of active engagement, or if the total session creeps past ten minutes of active engagement.

2. **Cognitive fog tolerance.** Concrete tests: any screen that requires the user to remember a rule for more than one beat of attention; any test where the instructions are not visible while the test is running; any error message that asks the user to interpret rather than act; any quality score explanation that uses statistical terminology without translation. The Patient Advocate will push back on instructions that load working memory unnecessarily.

3. **Low contrast vision tolerance.** Concrete tests: any text or affordance below the WCAG AA contrast ratio applied to small text; any onboarding or quality message displayed on a colored or photographic background; any control whose only differentiator from its surroundings is hue. (Note: the vision test itself is a deliberate exception by clinical design, and the test specific concern is handled in concern 6 below.)

4. **Dexterity tolerance.** Concrete tests: tap targets smaller than 48 dp; controls placed within 8 dp of a screen edge such that hand tremor causes accidental edge gestures; any input that demands sub 100 millisecond timing precision from a user who may have action tremor; the bilateral tap test target sizing and spacing specifically. The Patient Advocate is on record now that the tap test must not punish a tremor patient with tap-on-empty-space penalties beyond what the Bays et al. 2015 literature actually scores.

5. **Mobility and balance tolerance, including mobility aid users.** Concrete tests: any instruction that assumes the user can stand; any instruction that assumes the user has a hallway long enough to walk thirty seconds in a straight line; any instruction that assumes the user can place the phone in a front trouser pocket (women's clothing routinely lacks usable front pockets, and wheelchair users often cannot reach a front pocket safely). The Patient Advocate will push back if the gait test cannot be skipped gracefully on a session by session basis with a clear, non guilt-tripping skip path.

6. **Heat sensitivity and sustained luminance.** Concrete tests: any test that runs the screen at full brightness for more than thirty seconds; any test that requires the user to grip the phone tightly for the duration. (Note: the vision test legitimately requires brightness control for clinical fidelity, but the surrounding screens should not.)

7. **The "bad MS day" mode and quality score honesty.** Concrete test: the user must be able to complete a session on a day when their tap rate is half of normal, their gait is shuffling, their voice is slurred, and their vision is foggy, and the session must be saved with an honest quality score rather than refused or minimized. The clinical value of MS Battery is precisely the longitudinal record across good and bad days, so the application cannot reject bad day data.

8. **Mobility aid users specifically.** Concrete test: a wheelchair user, a walker user, a cane user. The application must offer a per session "skip the gait test" path that does not break the weekly streak, does not silently degrade the PDF report value, and does not require the user to disclose their mobility aid status as a profile setting. (Disclosure can be offered, but it must not be required.) Per Galati 2024 a verbatim quit reason was "I can only proceed so far because I'm in the car. I can't stand and balance," which the Patient Advocate reads as direct evidence that contextual skip is required infrastructure, not a feature backlog item.

### Galati 2024 qualitative quit themes as anchored evidence

The following are the seven quit reason clusters identified in the Galati et al. 2024 user experience analysis of Floodlight Open (JMIR Human Factors 11:e57033, DOI 10.2196/57033), per the summary in `docs/source/clinical-references.md` and the Galati 2024 Table 4 reference supplied to the Patient Advocate at Phase 0 dispatch:

1. Activation and onboarding confusion.
2. Forgetting without reminders.
3. Competing life demands.
4. Physical or contextual barriers (including unsafe locations for tests).
5. Test burden and repetitiveness.
6. Comprehension of instructions.
7. 2MWT scheduling friction specifically.

Two verbatim quotes from the Floodlight Open cohort are reproduced here per Galati 2024 Table 4, as supplied to the Patient Advocate at Phase 0 dispatch, to anchor the framing of physical and contextual barriers and of test burden and repetitiveness respectively:

> "I can only proceed so far because I'm in the car. I can't stand and balance."
> Per Galati 2024 Table 4, Floodlight Open MS user, on physical and contextual barriers.

> "How many more tomatoes can I pinch?"
> Per Galati 2024 Table 4, Floodlight Open MS user, on test burden and repetitiveness (the Pinching Test in Floodlight Open used a tomato pinching metaphor).

The Patient Advocate will not invent additional patient quotes. Where a future review entry needs to cite patient experience, it will either cite Galati 2024 by cluster, cite an additional entry in `docs/source/clinical-references.md` after the Clinical Validator has added it, or be flagged as Patient Advocate framing and not as evidence.

A third commonly referenced theme from the Galati 2024 paper, the substantial onboarding conversion gap (registration to activation rose from 53.9 percent to 74.6 percent after the Floodlight Open onboarding redesign, per `docs/source/clinical-references.md`), is the empirical basis for treating onboarding as a Phase 0 first class concern rather than a Phase 11 polish item. The Patient Advocate concurs with that prioritization.

### Recommendations the Patient Advocate will push back on by default

These are standing objections. If a later phase brings any of the following to a review entry, the Patient Advocate will object on the record. They are listed here so that the UI/UX Designer, Android Engineer, and Project Manager have advance notice of where friction will arise.

1. **Reminders that fire after 7 PM.** Evening MS fatigue is well documented in patient communities and in the MS PRO literature. A reminder ping at 8 PM is an instruction to a tired person to do a cognitively taxing task. The retention design specification in `docs/design/retention.md` (per `docs/plan.md` Phase 0) already names this rule per the Project Manager's framing, and the Patient Advocate is on record endorsing it.

2. **Gait test instructions that assume the user can stand and walk in any setting.** The instruction "place the phone in your front pocket and walk thirty seconds in a straight line" is correct for the clinical design, but it must be paired with: a per session skip path that does not break the streak, an honest acknowledgment in the report that a skipped gait test produces a partial record, and a non shaming UI tone for users who skip frequently. The verbatim Galati 2024 quote on cars and standing is the load bearing evidence here.

3. **Microphone tests that assume a quiet environment.** The voice test in Phase 8 captures a 30 second reading sample. Realistic operating environments include kitchens during meal preparation, living rooms with a television on, cars with road noise, and homes with children. The voice test must compute a usable acoustic features result on noisy input, mark the quality score honestly, and not refuse the session because background noise exceeded a threshold. If the signal to noise ratio is too low for valid acoustic feature extraction, the user must be told concretely (for example, "background was loud, the recording was kept but voice quality features were not computed for this session"), not abstractly.

4. **Vision tests that require unimpaired contrast sensitivity to operate the test UI.** The vision test legitimately probes low contrast acuity as the clinical signal. The surrounding UI (instructions, progress, quality flags, retry path) must therefore meet a stricter contrast bar than the rest of the application, because the very symptom the test measures will be present in the user when they are reading the surrounding chrome. The Patient Advocate will push back if any vision test instruction or quality message is rendered at the same contrast as the test stimulus.

5. **Onboarding flows that gate activation behind multi screen disclosures or profile fields.** Per Galati 2024 the registration to activation gap was the single largest pre test retention floor, recoverable from 53.9 percent to 74.6 percent by an onboarding redesign. Any onboarding that asks for date of birth, biological sex, dominant hand, MS type, and height (per SPEC.md Section 8 user profile) must (a) make the medically optional fields actually skippable without nagging, (b) defer the disclaimer acknowledgment screen to a dismissible, glanceable form, and (c) get the user to their first test in the smallest practicable number of taps.

6. **Quality score language that reads as failure.** A session with a low quality score on a bad MS day is clinically valuable, not a user error. The Patient Advocate will push back on any quality score copy that uses words like "invalid," "failed," "too poor to use," or "please retry." The acceptable register is descriptive, for example "this session was recorded with lower confidence; the result is included in your trends and labeled accordingly."

7. **Streak mechanics or gamification that punish a missed week.** A patient in a flare, in the hospital, or simply having a bad MS week may miss a session. A streak counter that resets to zero, or worse, a notification framed around a lost streak, is the kind of design that drives the "competing life demands" quit cluster per Galati 2024. The Patient Advocate will push back on any retention mechanic that frames absence as failure.

8. **Defaulting reminder copy to clinical or medicalized language.** A user with MS does not need to be reminded that they have MS. Reminder copy should be warm, brief, and human, and should never imply that missing a session has clinical consequences. The retention design specification in `docs/design/retention.md` is named in `docs/plan.md` Phase 0 as covering "friendly tone with periodic acknowledgment messages," and the Patient Advocate concurs.

## Coordination notes for the Clinical Validator (parallel Phase 0 work)

The Clinical Validator (agent 01) owns `docs/source/clinical-references.md` per `agents/19-patient-advocate.md`. The Patient Advocate contributes proposed entries; the Clinical Validator decides what is added and verifies citations. The following PRO instruments are recommended for inclusion in `docs/source/clinical-references.md`. They are recommendations, not assertions of citation accuracy. The Clinical Validator and the Citation Auditor own verification of authors, years, and DOIs.

The Patient Advocate's selection criteria for this list:
- The instrument is a patient reported outcome measure validated in MS populations.
- The instrument's domains overlap with at least one of the standing concerns above (fatigue, cognition, mobility, vision, hand function, quality of life, day to day disability impact).
- The instrument is widely cited in MS clinical literature and is recognizable to a neurologist receiving the MS Battery PDF.

Recommended additions, with the Patient Advocate's rationale for each:

1. **MSIS-29 (Multiple Sclerosis Impact Scale, 29 item).** Patient reported overall physical and psychological impact of MS. Anchors the Patient Advocate's "bad MS day" concern with a validated measurement frame.

2. **MSQOL-54 (Multiple Sclerosis Quality of Life, 54 item).** MS specific quality of life instrument extending the SF-36. Useful for grounding the case that subjective experience and objective measurement are complementary, not redundant.

3. **FAMS (Functional Assessment of Multiple Sclerosis).** MS specific quality of life instrument with subscales including mobility, symptoms, and thinking and fatigue. Particularly relevant to the cognitive fog concern.

4. **Neuro-QoL (Neurology Quality of Life).** NIH developed PRO bank with MS validated short forms covering fatigue, cognitive function, upper extremity function, lower extremity function, sleep, and stigma. Several short forms map cleanly onto specific test modules in the battery (upper extremity to Tap, lower extremity to Gait, cognitive to SDMT).

5. **MFIS (Modified Fatigue Impact Scale).** MS specific fatigue PRO. Directly informs the fatigue tolerance concern and the case for capping session duration.

6. **PDDS (Patient Determined Disease Steps).** Patient reported analog of EDSS, with strong correlation to neurologist administered EDSS. Useful for the persona work because it gives the team a crisp shorthand for "what stage of MS is this user living with" without requiring a clinician.

7. **SymptoMScreen.** Patient reported symptom burden screen across multiple MS domains. Useful as a structured map of which symptoms might be present concurrently in the persona.

8. **Cognitive function PRO instruments validated in MS** (the Clinical Validator will pick the right one or two; candidates include MSNQ, the MS Neuropsychological Screening Questionnaire, and PROMIS Cognitive Function short forms with MS validation). These ground the Patient Advocate's cog fog tolerance concern in a measurable instrument the team and a clinician would both recognize.

The Patient Advocate explicitly defers to the Clinical Validator on author, year, journal, DOI, and which version of each instrument to cite. The Patient Advocate's role is to flag relevance, not to assert bibliographic detail.

## Review log

The table below is the per screen review log. Entries are appended in Phase 1 and onward, in chronological order, never reordered, never edited in place. A revision to a prior verdict is appended as a new row that names the prior entry by date.

| Date | Phase | Screen or module | MS related concerns considered | Recommended changes | Verdict | Notes |
|------|-------|------------------|--------------------------------|---------------------|---------|-------|
|      |       |                  |                                |                     |         |       |

(No entries yet. Phase 0 framing notes above are the lens for all future entries.)
