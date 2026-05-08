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
| 2026-05-07 | 1 | DisclaimerScreen, ProfileSetupScreen, HomeScreen, SessionRunnerScreen, SettingsScreen, MockTestModule | Cognitive fog tolerance, low contrast vision tolerance, dexterity tolerance, dignity of clinical labels, "bad MS day" tone, onboarding gating per Galati 2024 | See detailed entry below | APPROVED WITH RECOMMENDED COPY CHANGES | 18 issues found, 0 high, 11 medium, 7 low. None block Phase 1 close because the application is still on a mock test module and no real patient will use it before Phase 2 introduces the Tap test. Copy and labeling fixes should land before Phase 2. |

## 2026-05-07, Phase 1 review, onboarding and home shell on the mock test module

### Files reviewed (all read in full at the commit on the branch as of 2026-05-07)

- `/home/mustafa/src/MS-Battery/app/src/main/java/com/mustafan4x/msbattery/ui/onboarding/DisclaimerScreen.kt`
- `/home/mustafa/src/MS-Battery/app/src/main/java/com/mustafan4x/msbattery/ui/onboarding/ProfileSetupScreen.kt`
- `/home/mustafa/src/MS-Battery/app/src/main/java/com/mustafan4x/msbattery/ui/home/HomeScreen.kt`
- `/home/mustafa/src/MS-Battery/app/src/main/java/com/mustafan4x/msbattery/ui/home/SessionRunnerScreen.kt`
- `/home/mustafa/src/MS-Battery/app/src/main/java/com/mustafan4x/msbattery/ui/settings/SettingsScreen.kt`
- `/home/mustafa/src/MS-Battery/app/src/main/java/com/mustafan4x/msbattery/battery/MockTestModule.kt`
- `/home/mustafa/src/MS-Battery/app/src/main/java/com/mustafan4x/msbattery/ui/RootScreen.kt`

### Verdict

**APPROVED WITH RECOMMENDED COPY CHANGES** for Phase 1 close. The Phase 1 deliverable is a scaffold around a mock test module; no real patient will be exposed to this build because Phase 2 introduces the first real test. The framing layer is therefore allowed to ship in its current shape so long as the recommended copy and labeling changes below are tracked and applied before Phase 2 ships a real Tap test to a real device. None of the issues found rise to NEEDS REWORK because none of them silently corrupt clinical data, stigmatize the user permanently, or hide a safety claim.

### What is already good

These are concrete things the Phase 1 implementation got right and which the Patient Advocate wants captured here so they are not regressed in Phase 2 polish:

1. The disclaimer copy lands the wellness vs medical device distinction in three short clauses, in the order a person reading on a small screen actually needs them: not a medical device, do not change treatment, share with neurologist. This is closer to the SPEC.md Section 10 wording than alternative drafts, and it does not catastrophize.
2. The MS type field in `ProfileSetupScreen.kt` line 60 is labeled "MS type (optional)" and defaults to `MSType.UNDISCLOSED`. A user uncomfortable disclosing can tap Save and continue without ever opening that dropdown. This is the right behavior per the standing objection 5 in the Phase 0 framing.
3. The `Sex` enum default is also `UNDISCLOSED` (line 37 of `ProfileSetupScreen.kt`), preserving the same out for users uncomfortable disclosing biological sex.
4. The session list empty state in `HomeScreen.kt` line 62 reads "No sessions yet. Run the weekly battery to get started." and does not shame the user or imply harm from not having a history yet. The tone is acceptable, with the wording caveats noted in issue 7 below.
5. There are no flashing animations and no rapid auto advancing transitions in any of the reviewed screens. This matters for both photosensitive responses and for cognitive fog. Phase 2 polish must preserve this.
6. There is no streak counter, no badge, no gamification element anywhere in the reviewed surface. This complies with standing objection 7 in the Phase 0 framing.
7. The disclaimer is a one tap acknowledgment ("I understand"), not a multi screen carousel. This is on the right side of standing objection 5 (onboarding gating) and aligns with the Galati 2024 recovery of registration to activation rates after Floodlight Open simplified onboarding.

### Issues found

Each issue is listed with: file, line range, severity (low or medium or high), the problem stated in patient terms, and the recommended copy or behavior change. Severity is the Patient Advocate's framing, not WCAG severity (which the Accessibility Specialist owns).

#### Issue 1, raw enum names visible to the user, dignity and jargon

- **File:** `app/src/main/java/com/mustafan4x/msbattery/ui/onboarding/ProfileSetupScreen.kt`
- **Lines:** 92, 96 (the `EnumDropdown` composable renders `selected.name` and each option's `option.name` directly)
- **Also affects:** `app/src/main/java/com/mustafan4x/msbattery/ui/settings/SettingsScreen.kt` lines 42, 43, 45 (same raw enum `.name` reads)
- **Severity:** medium
- **Problem:** the user sees strings like `RRMS`, `PPMS`, `SPMS`, `CIS`, `UNDISCLOSED`, `AMBIDEXTROUS`, `OTHER`, and `FEMALE` rendered in screaming caps because they are raw Kotlin enum names. To a person living with MS, especially one newly diagnosed, seeing your own diagnosis subtype rendered as a four letter all caps acronym next to other four letter all caps acronyms reads like a chart in a clinic, not a personal app. "UNDISCLOSED" reads as a database null sentinel, not as a respectful "prefer not to share." This is the core dignity concern that the standing objection on clinical jargon was written for.
- **Recommended change:** introduce a per enum display label resolver in the UI layer (no schema change to the Room enums, which can stay as they are). Suggested labels (final wording is the PM's call):
  - `Sex.FEMALE` -> "Female"
  - `Sex.MALE` -> "Male"
  - `Sex.OTHER` -> "Another option" (or "Other") (PM picks)
  - `Sex.UNDISCLOSED` -> "Prefer not to say"
  - `Hand.LEFT` -> "Left"
  - `Hand.RIGHT` -> "Right"
  - `Hand.AMBIDEXTROUS` -> "Either hand" (more honest for the bilateral tap test framing than the medical Latinate)
  - `MSType.RRMS` -> "Relapsing remitting (RRMS)"
  - `MSType.PPMS` -> "Primary progressive (PPMS)"
  - `MSType.SPMS` -> "Secondary progressive (SPMS)"
  - `MSType.CIS` -> "Clinically isolated syndrome (CIS)"
  - `MSType.UNDISCLOSED` -> "Prefer not to say"
  - `TestType.TAP` -> "Bilateral Tap" (when ever surfaced in UI; not yet surfaced in Phase 1)
  - `TestType.GAIT` -> "Gait"
  - `TestType.VISION` -> "Low Contrast Vision"
  - `TestType.SDMT` -> "Symbol Digit"
  - `TestType.VOICE` -> "Voice Reading"

#### Issue 2, "EnumDropdown" interaction is a two step gesture and reads as a debug control

- **File:** `app/src/main/java/com/mustafan4x/msbattery/ui/onboarding/ProfileSetupScreen.kt`
- **Lines:** 83 to 102
- **Severity:** medium
- **Problem:** the current control renders as a static line of text (`label: SELECTED_VALUE`) followed by a separate "Change" button, which then opens a `DropdownMenu`. For a user with cognitive fog this is two cognitive moves to discover and confirm a selection, not one. The user has to read the current state, parse what "Change" applies to, tap Change, find the selection in the dropdown, tap the option, then visually verify the line above updated. A standard Material 3 `ExposedDropdownMenuBox` is one cognitive move (tap the field, pick the option) and is the pattern the user has already been trained on by every other Android app. The current control also has zero affordance that the value can be changed without scrolling to the Change button, which is below the value text.
- **Recommended change:** replace the custom `EnumDropdown` with a Material 3 `ExposedDropdownMenuBox` that wraps a read only `OutlinedTextField` with a trailing dropdown indicator. The user taps anywhere on the field to open the menu. The Patient Advocate is not specifying the Compose API; the UI/UX Designer or Android Engineer can choose between `ExposedDropdownMenuBox`, a Compose `RadioButton` group rendered as a list (better for accessibility on small lists), or another standard Material 3 selector. The behavior requirement is: one tap to open, one tap to choose, label visible at all times.

#### Issue 3, default profile values are guesses pre filled into the user's record

- **File:** `app/src/main/java/com/mustafan4x/msbattery/ui/onboarding/ProfileSetupScreen.kt`
- **Lines:** 40 (height defaults to "170"), 41 (year of birth defaults to "1995")
- **Severity:** medium
- **Problem:** the form pre fills 170 cm and 1995 in the year of birth and height fields. A user with cognitive fog who reads the prompt, sees a filled value, and assumes the app has read it from somewhere may tap Save without correcting it. The result is a profile that records a fabricated date of birth and height for a real person, which then propagates into any normative comparison the application later performs (the gait literature norms in `docs/source/clinical-references.md` are height stratified per Givon 2009). The standing concern on the "bad MS day" mode is that the application must accept genuine user input on bad days; the corollary is that it must not invent input on the user's behalf on any day.
- **Recommended change:** remove the default values. Both fields should start empty with a placeholder such as "Year (for example 1985)" and "Height in cm (for example 168)". The Save button should be disabled until both fields parse to plausible ranges. The plausibility check is a small UX add: year between roughly 1925 and the current year, height between roughly 100 cm and 230 cm. If a user types something outside the range, the field shows an inline message ("Please enter a year between 1925 and 2026") rather than silently coercing or silently accepting. Note: SPEC.md Section 8 stores `dateOfBirthEpochMs` as a `Long`, so the conversion from year of birth to epoch ms can keep its current January 1 of that year convention, but the UI must not invent the year itself.

#### Issue 4, profile setup has no skip path and no progress indication

- **File:** `app/src/main/java/com/mustafan4x/msbattery/ui/onboarding/ProfileSetupScreen.kt`
- **Lines:** 43 (TopAppBar title) to 78 (Save button)
- **Severity:** medium
- **Problem:** per standing objection 5 in the Phase 0 framing, onboarding flows that gate activation behind multi screen profile fields have a documented retention cost (Galati 2024, registration to activation gap). The Phase 1 implementation makes year of birth, height, biological sex, dominant hand, and MS type all visible on a single screen, which is correct (one screen is better than five), but it does not let the user defer the profile to Settings. A cognitively fatigued user who opens the app for the first time and sees five fields may close the app before tapping the first test. The screen also has no progress indicator (Step 2 of 2) so the user does not know how close they are to the first test.
- **Recommended change:** add a "Skip for now" text button to the right of "Save and continue" with a body small footnote underneath the form: "You can fill these in any time from Settings. We need them later for the most accurate gait comparisons, but you can run all tests without them." The skip path navigates straight to home with a profile where the medically optional fields are `UNDISCLOSED` and `heightCm` is null (this last is a SPEC.md change, not a unilateral Patient Advocate change; flagging it for PM and Data Engineer to ratify before implementation). Independently, even without the skip path, the TopAppBar should read "Set up profile (1 of 1)" or the screen should otherwise communicate that this is the only setup screen. The current "Set up profile" implies more screens are coming.
- **PM ratification needed:** the proposal to make `heightCm` nullable touches the Room schema. Patient Advocate flags it as a recommendation, defers to Data Engineer and Database Administrator on whether the schema change is in or out of scope for Phase 1.

#### Issue 5, disclaimer body uses centered text alignment and a single dense paragraph

- **File:** `app/src/main/java/com/mustafan4x/msbattery/ui/onboarding/DisclaimerScreen.kt`
- **Lines:** 30 to 35
- **Severity:** medium
- **Problem:** the disclaimer is rendered as one long centered sentence stitched out of three clauses with `+` concatenation in source. Centered body text is significantly harder to read for users with low contrast sensitivity, mild visual impairment, or reading fatigue, because the eye loses the left margin anchor between lines. This is well established in typographic accessibility practice and is also relevant per Phase 0 standing concern 3 (low contrast vision tolerance). The dense single paragraph also asks the user to hold three clinical concepts in working memory at once: "this is not a medical device," "do not change treatment based on results," and "share with neurologist." Cognitive fog tolerance per Phase 0 framing concern 2 prefers chunked, scannable copy.
- **Recommended change:** render the disclaimer as three separate `Text` lines with `TextAlign.Start`, each one a single short sentence:
  > This app is not a medical device. It does not diagnose or treat any condition.
  >
  > Please do not change your treatment based on what you see here.
  >
  > When you visit your neurologist, you can share these results to help the conversation.
  
  The third sentence is intentionally warmer than the SPEC.md Section 10 language. SPEC.md says "Share with your neurologist for clinical decisions," which is functionally correct but reads as a directive. The recommended copy preserves the safety claim (the user does not act on results, the neurologist does) while reframing the role of the report from "input to a clinical decision" to "input to a conversation," which is closer to how patients describe what they want from this kind of tool. PM and Compliance Reviewer ratification is required before this copy ships, because the Compliance Reviewer specifically reviewed the original wording in Phase 0.

#### Issue 6, "I understand" button is acceptable but not warm; minimum height not asserted

- **File:** `app/src/main/java/com/mustafan4x/msbattery/ui/onboarding/DisclaimerScreen.kt`
- **Lines:** 36 to 42
- **Severity:** low
- **Problem:** the button copy "I understand" is acceptable. It is also slightly transactional. A user with mild cognitive fatigue who reads the disclaimer carefully may pause on whether they actually do understand. Material 3's default `Button` is around 40 dp tall, which is below the 48 dp tap target the Phase 0 framing names as a standing dexterity concern. The Material 3 default is what the Accessibility Specialist will measure against; the Patient Advocate flags it here as cross cutting because a user with hand tremor reading the disclaimer is exactly the user this rule is for.
- **Recommended change (copy):** change "I understand" to "Got it, continue" or "OK, take me in." The Patient Advocate's preference is "Got it, continue" because it acknowledges the user has read it and confirms forward motion in one phrase.
- **Recommended change (size):** the Patient Advocate flags the 48 dp minimum as a project wide concern; the Accessibility Specialist owns the technical implementation. No specific code change is requested here; this is a forwarded observation.

#### Issue 7, "Start weekly battery" reads as performative and clinical

- **File:** `app/src/main/java/com/mustafan4x/msbattery/ui/home/HomeScreen.kt`
- **Lines:** 56 to 59 (Button copy)
- **Severity:** low
- **Problem:** the home call to action is "Start weekly battery." The word "battery" is technically correct (a battery of tests is the clinical term, and the project name uses it), but in a home context it reads as a noun the patient does not normally use about themselves. Patients tend to talk about a "check in," a "session," a "this week's tests," or "my MS tracking." The Phase 0 framing concern on cognitive fog tolerance and on warmth in retention copy applies. There is no welcoming framing for a first time user beyond the empty history message below the button.
- **Recommended change:** change the button copy to "Start this week's check in" or "Begin this week's session." The screen title in the TopAppBar can stay "MS Battery" because that is the application's name. Independently, when the session list is empty, augment the empty state from the current "No sessions yet. Run the weekly battery to get started." to something warmer and informative:
  > You have not run a check in yet. The first one takes about ten minutes and produces a record you can share with your neurologist next visit.
  
  The mention of duration up front ("about ten minutes") is per standing concern 1 (fatigue tolerance) so the user can decide if today is a good day before starting.

#### Issue 8, button uses two leading spaces as a visual hack to space the icon

- **File:** `app/src/main/java/com/mustafan4x/msbattery/ui/home/HomeScreen.kt`
- **Line:** 58 (`Text("  Start weekly battery", ...)`)
- **Severity:** low
- **Problem:** the leading double space inside the `Text` call is the Android Engineer's way of nudging the icon and label apart. This is fragile (TalkBack reads it as a leading pause; future Compose updates may collapse the whitespace; the spacing is invisible to the user but the label reads as " Start weekly battery" to a screen reader, with a leading whitespace pause). This is a structural concern, not a copy concern, but it lives at the framing layer because it is the patient facing string.
- **Recommended change:** replace the double space hack with a `Spacer(Modifier.width(8.dp))` between the `Icon` and the `Text`, or the equivalent Material 3 `Button` content slot pattern. The label string itself becomes "Start this week's check in" per issue 7.

#### Issue 9, session row history shows raw timestamp rather than relative date

- **File:** `app/src/main/java/com/mustafan4x/msbattery/ui/home/HomeScreen.kt`
- **Lines:** 74 to 84 (`SessionRow`)
- **Severity:** low
- **Problem:** the row prints `2026-05-07 14:30` and a status of "Completed" or "In progress." For a user with cognitive fog scanning their history, "Today" or "This Tuesday" or "Last week" carries more meaning than a yyyy MM dd HH:mm string. The current format is also internationally generic (good) but locale insensitive (the SimpleDateFormat is initialized with `Locale.getDefault()` but the literal pattern is fixed). For the v1 scope the Patient Advocate is not asking for full i18n; just a friendlier surface for the most recent few rows.
- **Recommended change:** show "Today, 14:30," "Yesterday, 09:15," "Tuesday, 14:30 (3 days ago)," or "May 5" for older entries. A small Kotlin helper that maps an epoch ms to one of these forms is a self contained ask. The status label "Completed" should stay; the alternative ("In progress") should only appear if the user has an actually resumable session, which is itself a Phase 5 or Phase 9 question (resume across app launches).

#### Issue 10, session runner has no progress indication and instructions disappear during the test

- **File:** `app/src/main/java/com/mustafan4x/msbattery/ui/home/SessionRunnerScreen.kt`
- **Lines:** 46 to 64 (the `when (val s = state)` block)
- **Severity:** medium (high once real tests replace the mock; flagged now to land before Phase 2)
- **Problem:** while a test is running, the user sees only what the `TestModule.Content` composable renders. There is no top level progress indicator like "Test 1 of 5" and the test instructions are not persistently visible. Phase 0 framing concern 2 (cognitive fog tolerance) explicitly names this case: "any test where the instructions are not visible while the test is running." The mock test in Phase 1 does keep its short instruction visible (line 36 of `MockTestModule.kt`), so this is not a Phase 1 close blocker, but the moment the Tap test in Phase 2 hides its instructions during the 30 second tap window, this becomes a real regression.
- **Recommended change:** the `SessionRunnerScreen` should surface, at the top of the body (below the TopAppBar), a persistent header that shows the current test display name, the test number out of total ("Test 1 of 5"), and (optionally) the estimated duration of this test from `module.estimatedDurationSeconds`. The individual `TestModule.Content` composable owns its instructions, but the orchestrator owns the "where are we in the session" framing. The Phase 1 close can ship with this missing because the only test is the mock, but it must be present before Phase 2 ships the Tap test. Patient Advocate is flagging now so the work is not deferred to Phase 11 polish.

#### Issue 11, Cancel button mid session has no confirmation and lives in the TopAppBar action slot

- **File:** `app/src/main/java/com/mustafan4x/msbattery/ui/home/SessionRunnerScreen.kt`
- **Lines:** 32 to 38
- **Severity:** medium
- **Problem:** while a session is running, the only escape is a "Cancel" button in the TopAppBar actions slot. A tap on Cancel calls `orchestrator.cancel()` and `onFinished()` immediately, with no confirmation dialog. A user with hand tremor or cog fog who taps the wrong area of the TopAppBar can lose all collected results from the session in progress. This is the precise interaction Phase 0 framing concern 4 (dexterity tolerance) was written for. The TopAppBar action area is also closer to the screen edge, which the Phase 0 framing flags as a tremor problem area.
- **Recommended change:** replace the immediate cancel with a two step flow: tap Cancel opens a small Material 3 `AlertDialog` titled "Stop this check in?" with body text "Your results so far will not be saved. You can come back to it later." and two actions: "Keep going" (default, primary) and "Stop and discard." The default focus is on Keep going so a stray Enter or accidental tap returns to the test. As a separate ergonomic consideration, the Cancel button should not be the only path; a "Skip this test" path will be needed for the Gait test per Phase 0 standing objection 2 (mobility aid users), but that work belongs in Phase 4, not Phase 1.

#### Issue 12, "Session not started" placeholder reads as a debug string

- **File:** `app/src/main/java/com/mustafan4x/msbattery/ui/home/SessionRunnerScreen.kt`
- **Line:** 48
- **Severity:** low
- **Problem:** the `BatteryOrchestrator.State.Idle` arm renders the literal string "Session not started." In current navigation flow this state is unreachable because `RootScreen.kt` line 80 starts the orchestrator in the same `remember` block that creates it. So the user almost never sees this string. However, if the navigation ever races (configuration change before `start()` is called), the user sees a flat technical string with no recovery action. For the "bad MS day" tone the Patient Advocate prefers a copy that does not feel like the app crashed.
- **Recommended change:** replace with "Getting your check in ready..." or, ideally, render a small `CircularProgressIndicator` with no text at all. The state is transient and the user does not need to read it. If the state persists more than a few seconds, that is a bug to surface to the Performance Engineer or Android Engineer, not a string for the user to read.

#### Issue 13, "Session complete" closing screen is terse and does not summarize what was recorded

- **File:** `app/src/main/java/com/mustafan4x/msbattery/ui/home/SessionRunnerScreen.kt`
- **Lines:** 59 to 62
- **Severity:** low
- **Problem:** when the session completes, the user sees the literal string "Session complete" and a "Done" button. There is no summary of which tests were run, no indication of where the results live, no path into the report. For a user who just spent ten minutes on a battery this is anti climactic. The Phase 0 framing concern 6 (quality score language) is relevant in adjacent: a session that completed at low quality should not feel like a failure.
- **Recommended change:** for Phase 1, change the literal to "All done. Your check in has been saved." and below it a body small line "You can view your results from the home screen, or share a report later from Settings." For Phase 9 (Reporting) and Phase 10 (PDF Export) the completion screen should also include a primary action "See your results" that navigates into the per session view once that view exists. For Phase 1, the navigation back to Home is acceptable because there is no per session view yet.

#### Issue 14, MockTestModule placeholder copy will silently set Phase 2 expectations if not changed

- **File:** `app/src/main/java/com/mustafan4x/msbattery/battery/MockTestModule.kt`
- **Lines:** 25, 26
- **Severity:** low
- **Problem:** the mock instructs the user to "Tap Continue" with "This is a mock test used during scaffolding." This is fine for Phase 1 because no real user runs it. However, the patterns are ones the real test modules will inherit by example unless explicitly stopped. Specifically: the instruction is presented above the action and disappears if the user scrolls; the action button label is generic ("Continue"); there is no countdown; there is no indication of what the test is for.
- **Recommended change:** none for Phase 1 itself. This is a flagged note for the Phase 2 Tap test review and onward. The Patient Advocate is recording here that the `TestModule.Content` contract should preserve four properties the mock does not yet model: instructions visible during the test, a countdown or progress visible during the test, an action label that is task specific (not "Continue"), and a task purpose visible at the top ("This test measures finger speed and rhythm in each hand").

#### Issue 15, Settings shows "Date of birth" with day precision when only year was collected

- **File:** `app/src/main/java/com/mustafan4x/msbattery/ui/settings/SettingsScreen.kt`
- **Line:** 41
- **Severity:** medium
- **Problem:** profile setup in `ProfileSetupScreen.kt` line 49 collects only the year of birth, then in line 64 constructs an epoch ms from January 1 of that year. Settings then renders "Date of birth: 1995-01-01." A user reading this in Settings will reasonably believe the application thinks their birthday is January 1. For a user with cognitive fog this can be confusing; for any user this is mildly disrespectful (the application has invented two thirds of a date it never asked for).
- **Recommended change:** label the row "Year of birth: 1995" and store the underlying epoch ms unchanged. The display logic does the year extraction (`Calendar.YEAR` from the stored epoch ms in the user's locale's calendar). The Room schema does not change. PM ratification needed only if the team wants to capture the day separately later; for v1, year of birth is sufficient per the gait normative literature.

#### Issue 16, Settings About text is rendered at bodySmall and uses spacer hack

- **File:** `app/src/main/java/com/mustafan4x/msbattery/ui/settings/SettingsScreen.kt`
- **Lines:** 47 (`Text(" ")` as a spacer), 49 to 52 (`bodySmall` typography on the about text)
- **Severity:** medium
- **Problem:** the About text in Settings is the only place the medical device disclaimer appears after onboarding. It is rendered at `MaterialTheme.typography.bodySmall`, the smallest body size in Material 3. For a user with mild visual impairment or low contrast sensitivity, this is the wrong scale for the most regulatorily and clinically important sentence in the application. The Phase 0 framing concern 3 (low contrast vision tolerance) applies. Separately, the `Text(" ")` on line 47 is being used as a vertical spacer; it works visually but reads as an empty announcement to TalkBack and adds nothing for sighted users either.
- **Recommended change (size):** render the About disclaimer at `MaterialTheme.typography.bodyMedium` or `bodyLarge`. The Patient Advocate's preference is `bodyLarge` because this is the sentence the user is most likely to read carefully on a bad MS day, and it should be set in a size the user can read without reaching for reading glasses they are not wearing on the couch.
- **Recommended change (spacer):** replace `Text(" ")` with `Spacer(Modifier.height(16.dp))`. This is structural cleanup, not a copy change, but it goes in the same patch.

#### Issue 17, Settings has no path to edit the profile

- **File:** `app/src/main/java/com/mustafan4x/msbattery/ui/settings/SettingsScreen.kt`
- **Lines:** 38 to 46 (the read only profile block)
- **Severity:** medium
- **Problem:** the profile fields are display only. A user who entered a value incorrectly during onboarding (because of cog fog, because of a typo, because the height conversion was wrong) cannot fix it from Settings. They have to clear app data or reinstall, both of which destroy their session history. This is a "bad MS day" landmine: the user enters a wrong value one day and is stuck with it forever, and any height stratified gait comparison the application later does is wrong for them. The Phase 0 framing concern 4 (dexterity tolerance) is relevant: a user who fat fingers the height field on day one with mild hand tremor cannot recover.
- **Recommended change:** add an "Edit profile" button at the bottom of the profile block in Settings that navigates to the profile setup screen pre populated with current values. The same screen handles edit and create, distinguished by whether `userProfileDao.getFirst()` returns a row. The PM and Android Engineer can decide whether to fold this into the Phase 1 close or to defer to early Phase 2; the Patient Advocate flags it as needing to land before any external user (beta cohort) sees the application.

#### Issue 18, RootScreen race between disclaimer and profile checks

- **File:** `app/src/main/java/com/mustafan4x/msbattery/ui/RootScreen.kt`
- **Lines:** 32 to 45
- **Severity:** low
- **Problem:** this is a structural framing observation, not a copy issue. The `startDestination` is computed from `disclaimerAcknowledged` (a synchronous SharedPreferences read) and `hasProfile` (a suspending DAO call wrapped in `LaunchedEffect`). On a slow device, the navigation graph picks `disclaimer` or `profile` before `hasProfile` resolves, which means a returning user with a profile may briefly land on the profile setup screen before the suspending check completes. For a user with cognitive fog who opens the app expecting to see their history, briefly landing on a profile setup screen is disorienting. The actual navigation is brief and self correcting because of the `popUpTo` calls, but the flicker is real.
- **Recommended change (framing only):** flag this to the Android Engineer as a Phase 1 polish note. The fix is to compute `startDestination` only after both checks have resolved (either gate the `NavHost` behind a small splash or load both values in a single suspending block before the first composition picks a destination). The Patient Advocate is not specifying the implementation. For Phase 1 close, this is a low severity flicker that does not affect correctness.

### Cross cutting observations (not numbered as issues)

1. **Tap target sizing across all screens.** The Patient Advocate flags this as a cross cutting concern but defers technical implementation to the Accessibility Specialist (agent 12), who owns the WCAG 2.1 AA tap target rule. The Patient Advocate's standing position is that 48 dp minimum applies to every interactive element, no exceptions, and that the `IconButton` for Settings in `HomeScreen.kt` line 45 specifically should be measured. No concrete change is requested here; this is a forwarded observation to the Accessibility Specialist for the Phase 11 audit.

2. **Material 3 type scale defaults.** All screens use `MaterialTheme.typography.titleMedium`, `bodyLarge`, `bodySmall`, etc., which means Dynamic Type will work later when the user changes the system font scale. This is the right pattern. Issue 16 is the one place where the typography choice is wrong for the content; everything else is using the scale correctly and should be preserved.

3. **No flashing animations, no rapid auto advances.** Confirmed by code reading. Phase 2 polish must preserve this.

4. **No streaks, no badges, no gamification.** Confirmed. Phase 11 retention work must not introduce them silently.

5. **No clinical claim leakage.** No copy in the reviewed surface implies the application diagnoses, treats, or interprets results clinically. The disclaimer in Issue 5 and the about text in Issue 16 are the two places where the wellness vs medical device boundary is restated; both are present and correct in substance even where the rendering needs work.

### Uncertainties (Patient Advocate did not assert these as findings)

1. The proposal in Issue 4 to make `heightCm` nullable is a Room schema change and a SPEC.md change. The Patient Advocate flags it as a recommendation only. The PM and the Data Engineer (agent 05) and the Database Administrator (agent 06) decide whether nullable height is in scope for Phase 1.
2. The proposal in Issue 5 to soften the disclaimer wording from SPEC.md Section 10 ("Share with your neurologist for clinical decisions") to a warmer ("you can share these results to help the conversation") needs the Compliance Reviewer (agent 21) to ratify before it ships. The Patient Advocate is not asserting that the warmer version is regulatorily safe; it is a copy proposal that has to be vetted by the role that owns FDA SaMD framing.
3. The "edit profile" path in Issue 17 may interact with how the SPEC.md "append only at the Session level" rule applies to `UserProfileEntity`. SPEC.md Section 8 is silent on whether the user profile is mutable or whether profile edits should produce a new row. The Patient Advocate flags this as a Data Engineer decision.
4. The Patient Advocate did not run the application on a physical device. All findings are from reading the source. The Patient Advocate has high confidence in the copy and labeling findings (Issues 1, 5, 6, 7, 9, 12, 13, 15, 16) and in the structural findings that follow from clear code patterns (Issues 2, 3, 4, 11, 14, 17, 18). Issue 10 is the one finding where running the application on a device would sharpen the recommendation; the source level conclusion is sufficient to flag it now.

### Handoff

The Patient Advocate hands back to the Project Manager. The PM ratifies the recommended copy and labeling changes (and the schema and compliance items flagged in the uncertainties), then dispatches the Android Engineer (agent 03) to apply the changes, the Compliance Reviewer (agent 21) to ratify the disclaimer rewording, and the Data Engineer (agent 05) to weigh in on the nullable height question. The Patient Advocate's next scheduled review is Phase 2 (Tap test).

(End of 2026-05-07 Phase 1 entry. Append only convention: this entry is not edited in place. Subsequent reviews append new dated sections.)

## 2026-05-07, Phase 2 review, Bilateral Tap Test and Phase 1 carryover fixes

### Files reviewed

- `app/src/main/java/com/mustafan4x/msbattery/battery/tap/BilateralTapTest.kt` (read in full)
- `app/src/main/java/com/mustafan4x/msbattery/battery/tap/TapFeatures.kt` (skimmed for quality score, miss rate, asymmetry)
- `app/src/main/java/com/mustafan4x/msbattery/ui/onboarding/DisclaimerScreen.kt` (carryover verification)
- `app/src/main/java/com/mustafan4x/msbattery/ui/onboarding/ProfileSetupScreen.kt` (carryover verification)
- `app/src/main/java/com/mustafan4x/msbattery/ui/home/HomeScreen.kt` (carryover verification)
- `app/src/main/java/com/mustafan4x/msbattery/ui/home/SessionRunnerScreen.kt` (carryover verification)
- `app/src/main/java/com/mustafan4x/msbattery/ui/settings/SettingsScreen.kt` (carryover verification)
- `app/src/main/java/com/mustafan4x/msbattery/ui/RootScreen.kt` (carryover verification)

### Verdict

**APPROVED WITH RECOMMENDED COPY CHANGES.** The bilateral tap module is clinically structured, the targets are large (120 dp), the overall duration is within the fatigue tolerance budget, and the quality score logic is honest. Three findings below are medium severity and should land before Phase 3 begins; none of them corrupt data or silently harm the user.

### Phase 1 carryover status

- Issue 1 (raw enum names): closed. `displayLabel()` resolver present in `ui/common/`, used throughout.
- Issue 2 (EnumDropdown): closed. `ExposedDropdownMenuBox` with `OutlinedTextField` and trailing icon in place.
- Issue 3 (prefilled defaults): closed. Both fields start empty; plausibility guards active.
- Issue 4 (skip path): closed. "Skip for now" `TextButton` present and saves a placeholder profile with `heightCm = null`.
- Issue 5 (centered dense disclaimer): closed. Three `TextAlign.Start` sentences, each a single statement.
- Issue 6 ("I understand" button): closed. Button reads "Got it, continue."
- Issue 7 (clinical button copy): closed. Button reads "Start this week's check in."
- Issue 8 (leading space hack): closed. `Spacer(Modifier.width(8.dp))` between icon and label.
- Issue 9 (raw timestamp): closed. `formatRelative()` produces "Today, 14:30," "Yesterday," etc.
- Issue 10 (no persistent header): closed. `SessionRunnerScreen` now shows test name, count, estimated seconds, and a `LinearProgressIndicator` above the module content.
- Issue 11 (immediate cancel): closed. Cancel opens an `AlertDialog` with "Keep going" as the dismiss action and "Stop and discard" as confirm.
- Issue 12 ("Session not started" placeholder): closed. `Idle` state now shows `CircularProgressIndicator` and "Getting your check in ready."
- Issue 13 (terse completion): closed. Completion reads "All done. Your check in has been saved." plus a path to settings.
- Issue 15 (date of birth label): closed. Settings renders "Year of birth: YYYY" via `yearOf()`.
- Issue 16 (bodySmall About text): closed. About text now at `bodyLarge`; `Spacer(Modifier.height(16.dp))` replaces the `Text(" ")` spacer.
- Issue 17 (no edit profile path): closed. `SettingsScreen` accepts `onEditProfile` and shows "Edit profile" `TextButton`; `RootScreen` wires the settings route to `nav.navigate("profile")`.
- Issue 18 (disclaimer and profile race): closed. `RootScreen` gates `NavHost` behind a `resolved` boolean; a `CircularProgressIndicator` shows until both async checks complete.

All 17 tracked carryover issues are closed.

### Phase 2 findings on BilateralTapTest

#### Finding 1, "dominant hand" and "non dominant hand" labels assume intact hand function, severity: medium

The instructions on `PreInstructions` read "Dominant hand first, then non dominant." The `instructions` string on the class uses the same framing. A patient whose dominant hand is their more severely affected hand (common in unilateral MS relapse patterns) may not know how to answer. Some patients in MS communities report that their "dominant" hand is no longer the hand they prefer for daily tasks because their original dominant hand has degraded. The labels are clinically standard but may be disorienting to a patient mid test. Recommended change: append a clarifying sentence to the instruction string and to the `PreInstructions` text: "If you are unsure, start with the hand you write or type with most of the time." This one sentence costs no screen space and covers the most common confusion case.

#### Finding 2, rest period has no countdown and no duration disclosure, severity: medium

The `Rest` phase shows "Take a short break. The next round starts soon." with no countdown. `REST_SECONDS` is 5 seconds, which is very short for a fatigued patient who may not be holding the phone or may have set it down. When the countdown to the non dominant round begins immediately after, the user may miss the first second or two of the `Countdown` display. Recommended change: display the rest timer ("Rest: 5... 4... 3...") in the same large `displayMedium` style used for the active countdown, so the user knows exactly when to pick the phone up again. The logic to drive a rest countdown already exists as a pattern in the `Active` phase; it is a small addition to the `Rest` arm of `LaunchedEffect`.

#### Finding 3, quality score threshold of fewer than 10 valid taps produces a score of 0.0 with no user feedback, severity: medium

`TapFeatures.computeSession` returns `qualityScore = 0.0` when either round has fewer than 10 valid taps. The session is still recorded via `onComplete`, which is correct per the "bad MS day" standing concern: the data should not be discarded. However, the current `Done` phase shows only "Tap test recorded. Returning to the session." with no indication that the quality score was low. A patient who produced fewer than 10 taps per round (plausible on a severe fatigue day or for a patient with significant hand weakness) will see the same completion message as a patient who produced 60. The application will save a `qualityScore` of 0.0 with no contextual note. Recommended change: when `qualityScore == 0.0`, change the `Done` text to "Tap test recorded. This round was short but your data has been kept." This is the register the Phase 0 framing specifies for low quality sessions: descriptive, not judgmental, confirms the data was kept.

#### Finding 4, the off target tap capture area is the full `Row` behind both targets, severity: low

The `TwoTargets` composable applies `detectTapGestures` to the entire `Row`, then each `Target` uses `clickable` which intercepts before the row gesture. This is the correct layering in Compose. The patient concern is that the gap between the two 120 dp circles is 16 dp wide (the `Spacer`). A patient with action tremor who is alternating quickly across the gap will accumulate off target taps counted against their miss rate. The `missRate` contributes to the quality score and to `asymmetryIndex` comparisons. This is not a show stopper (the Bays et al. 2015 scoring the Phase 0 framing mentioned does account for off target events), but the 16 dp gap is narrow for a tremor population. Recommended change (low priority): widen the spacer to 32 dp to reduce incidental gap misses during fast alternation. Flag for the Clinical Validator to confirm this gap width does not alter the normative interpretation of off target count.

#### Finding 5, the `Done` state transitions automatically rather than with user confirmation, severity: low

After the non dominant round completes, `onComplete` is called and `phase = TapPhase.Done` in the same `LaunchedEffect` block. The `Done` state text appears briefly and then the orchestrator calls `recordResult` which moves the session forward. The user has no moment to orient. For a fatigued patient who was tapping hard for 30 seconds, the transition to the next test (or to the session complete screen) may feel abrupt. Recommended change: gate the `Done` state on a brief `Button` labeled "Continue" rather than auto advancing. This aligns with the session runner's own `LinearProgressIndicator` pacing and gives the user a deliberate breath between tests.

### Three Phase 2 brief questions

**Duration sustainability:** 30 seconds of active tapping per round is within the tolerance budget. The total active tapping time is 60 seconds, not 70: `estimatedDurationSeconds = 70` correctly accounts for the two 5 second countdowns and one 5 second rest on top of the 60 active seconds. For almost all patients in the MS Battery target population this duration is achievable even on a moderate fatigue day. The concern is the rest period being too short to recover (5 seconds) rather than the tapping duration itself. This is captured in Finding 2.

**Bilateral structure clarity:** the alternating left and right structure is clearly conveyed. The "L" and "R" labels on the 120 dp circles are large and legible at `displaySmall`. The persistent instruction line at the top of the `Column` ("Tap the left and right circles, alternating, as fast as you can manage") remains visible throughout the `Active` phase, satisfying the Phase 0 framing concern that instructions must be visible while the test runs. The `NON_ALTERNATING` tap kind is silently recorded but never shown to the user during the test, which is correct: showing a real time error indicator would disrupt the task and is not standard in the 9HPT or 25FW administration.

**Dominant vs non dominant hand framing:** the framing is standard clinical terminology and is acceptable as a default. It becomes ambiguous for patients whose MS has shifted their effective dominant hand over time, which is a real phenotype in relapsing remitting disease. Finding 1 above addresses this with a one sentence clarification that costs nothing. The Patient Advocate does not recommend removing the dominant and non dominant distinction, because the asymmetry index between rounds is a clinically meaningful feature (the signal the Clinical Outcomes Reviewer will want for lateralized deficit tracking). The fix is better framing, not removal.

### Uncertainties

1. The Patient Advocate did not run the application on a physical device. The 120 dp target size and 16 dp gap assessment is from reading the source. On a small phone (for example a 5.0 inch screen at 420 dp wide), the two circles plus 16 dp spacer consume the full width, which may leave no room for the tremor margin the Phase 0 framing names. Flag for the Accessibility Specialist to measure on a real device.
2. The quality score formula uses `minOf(1.0, totalValid / 60.0)` as the count factor, implying 60 valid taps across both rounds is the target. The Patient Advocate does not know whether 60 is grounded in the normative Bays et al. 2015 literature or is an engineering estimate. Flag for the Clinical Validator to confirm.

### Handoff

Patient Advocate hands back to the Project Manager. Findings 1, 2, and 3 are recommended for the Android Engineer before Phase 3 begins. Findings 4 and 5 can be deferred to Phase 11 polish if the PM prefers. Finding 4 gap width question should be routed to the Clinical Validator for normative guidance.

(End of 2026-05-07 Phase 2 entry. Append only convention: this entry is not edited in place.)

## 2026-05-07, Phase 4 review, Gait Test Module Integration

### Files reviewed (read in full at HEAD on `main` as of 2026-05-07)

- `/home/mustafa/src/MS-Battery/app/src/main/java/com/mustafan4x/msbattery/battery/gait/GaitInstructionsScreen.kt`
- `/home/mustafa/src/MS-Battery/app/src/main/java/com/mustafan4x/msbattery/battery/gait/GaitCountdownScreen.kt`
- `/home/mustafa/src/MS-Battery/app/src/main/java/com/mustafan4x/msbattery/battery/gait/GaitCaptureScreen.kt`
- `/home/mustafa/src/MS-Battery/app/src/main/java/com/mustafan4x/msbattery/battery/gait/GaitDoneScreen.kt`
- `/home/mustafa/src/MS-Battery/app/src/main/java/com/mustafan4x/msbattery/battery/gait/GaitTestState.kt`
- `/home/mustafa/src/MS-Battery/app/src/main/java/com/mustafan4x/msbattery/battery/gait/GaitTestViewModel.kt`
- `/home/mustafa/src/MS-Battery/app/src/main/java/com/mustafan4x/msbattery/battery/gait/GaitTest.kt`

### Verdict

**APPROVED WITH FINDINGS.** The gait test as integrated lands the safety framing the Phase 0 framing standing objection 2 demands, the screens are uncluttered, the Cancel button is present and is not adversarial, and no copy in the Done screen punishes a low quality session as user failure. Three medium severity findings and two low severity findings are listed below; one of the medium findings (Finding 1, Cancel reachability while phone is in pocket) is a direct consequence of the front pocket capture posture and warrants attention before Phase 4 close, but the Patient Advocate's read is that the present implementation is acceptable to ship as Phase 4 minimum because it does not silently corrupt clinical data, does not stigmatize the user, and does not gate progress through the battery on the gait result. The remaining findings are properly Phase 11 polish or future feature work.

### Concern by concern evaluation

#### Concern 1, walking safety per Galati 2024 quit cluster 4

**Verified yes.** The instructions copy in `GaitInstructionsScreen.kt` lines 47 to 49 is verbatim: "Walk in a straight line for 30 seconds. Place your phone in a front pocket. Use a flat, unobstructed surface; have a wall or counter within reach if you need it. If you do not feel safe walking right now, you can skip this test." The four safety hooks the Patient Advocate looked for are all present: explicit straight line direction, explicit flat surface direction, explicit "wall or counter within reach" affordance, and the explicit "do not feel safe = skip" framing. The Skip control is a `TextButton` directly below the primary `I am ready` button (lines 67 to 77), reachable in one tap, not buried behind a confirmation dialog. This satisfies the Galati 2024 cluster 4 framing directly, and the verbatim Floodlight quote ("I can only proceed so far because I'm in the car. I can't stand and balance.") is the lens this passes under.

#### Concern 2, patient pocket reality and walking aid users

**Verified partially.** Phase 4 minimum (front pocket only) is what the implementation honors, and the plan explicitly named accommodation of held in hand or alternate pocket placements as Phase 11 follow up work. The current `GaitInstructionsScreen` body is silent on what a wheelchair user, a walker user, or a forearm crutch user should do; it offers the Skip path, which is the right Phase 4 minimum, but it does not surface to the user that skipping is the correct action for them rather than a personal failure. This is a Finding (Finding 4 below) that the Patient Advocate is deferring to Phase 11 because the Skip path is functional, the data is not corrupted, and the current copy does not shame the skipping user.

#### Concern 3, EDSS 6+ patients and the 800 ms inter peak ceiling

**Verified yes for the post test feedback layer; the underlying clinical limitation is documented in `docs/source/clinical-references.md`.** The Done screen at `GaitDoneScreen.kt` lines 34 to 38 maps the quality score to three plain language confidence bands ("Captured with high confidence" above 0.8; "Captured with limited confidence" between 0.5 and 0.8 inclusive; "Captured but quality is low" below 0.5). None of the bands use the words "invalid," "failed," "too poor to use," or "please retry" that standing objection 6 in the Phase 0 framing prohibits. A patient whose cadence falls below the 75 steps per minute floor (800 ms ceiling) may see "Captured but quality is low," which is descriptive rather than judgmental and is acceptable. The Clinical Validator's Phase 0 caveat 4 on `docs/source/clinical-references.md` already records that EDSS 6 to 6.5 patients are out of scope for the gait module as currently specified and that the limitation should be made explicit in the README and the in app About screen; that is a Phase 9 or Phase 11 documentation task, not a Phase 4 blocker.

#### Concern 4, heat sensitivity

**Verified yes.** The instructions copy nowhere encourages walking fast. The phrase used in `BilateralTapTest`'s instructions ("as fast as you can manage") does not appear here; the gait copy says "Walk in a straight line," with no rate qualifier. The 30 second active capture plus 3 second countdown is brief enough that Uhthoff phenomenon induction is unlikely on most days, and the absence of a "walk briskly" or "walk at your fastest comfortable pace" instruction protects the heat sensitive subset of the persona. No change requested under this concern.

#### Concern 5, emergency cancel UX and the standing objection 5 (cancel without shame)

**Verified yes for the surface, partially for the ergonomics.** The Cancel control on `GaitCaptureScreen.kt` lines 62 to 73 is a full width `OutlinedButton` with a 64 dp minimum height, sitting directly below the elapsed seconds readout. It is unambiguous in label ("Cancel") and is not styled as a destructive action with red affordance, which keeps the register acceptable per standing objection 5. The Cancelled state in `GaitTest.kt` lines 61 to 64 routes back through a zero quality `GaitDoneScreen` with a Continue button, so the user can advance to the next test in the battery without losing their position; this is the right behavior for a non shaming cancel. The ergonomic concern is that during the actual capture the phone is in a front pocket, which means the physical reachability of the button is not the issue this concern was originally framed around; Finding 1 below records the practical consequence.

### Evaluation of the three AE flagged items

#### Item a, the "Get ready to walk" headline above the numeric countdown

**Helpful, keep.** The plan literal copy spec for the Countdown screen specified only the numeric display. The AE added a `headlineMedium` "Get ready to walk" line above the `displayLarge` number on `GaitCountdownScreen.kt` lines 36 to 40. This addition is on the right side of the Phase 0 framing concern 2 (cognitive fog tolerance) for two reasons. First, a bare giant number with no accompanying label loads working memory (the user has to remember from the prior screen what is being counted down). Second, on a TalkBack pass the headline gives a screen reader a one phrase semantic anchor before the number is announced, which improves the cognitive scaffold for low vision users. The Patient Advocate endorses the addition. The addition does not change the test pacing, does not push the user to do anything they did not opt into on the previous screen, and does not introduce a new tap target. This is exactly the kind of small humanizing copy add the Phase 0 framing concern 2 calls for.

#### Item b, rendering the Cancelled state as a zero quality Done screen

**Acceptable for Phase 4. A dedicated cancel confirmation screen is not needed, but a small copy improvement is recommended in Finding 2 below.** The decision the AE made (route Cancelled through `GaitDoneScreen` with a synthesized zero quality `GaitFeatures` so the user can tap Continue and advance) is the right behavior under standing objection 5: a cancelled session is not a failure, it is data the user chose not to record, and the user must not be stranded. The implementation also correctly does not persist a real `TestResultEntity` for the cancelled session because `GaitTest.kt` line 63 calls `onComplete(skippedPayload())` which carries `qualityScore = 0.0` and `rawSensorRelativePath = null`; the orchestrator can then either skip persistence or persist a quality 0.0 placeholder. The Patient Advocate prefers the orchestrator to skip persistence on cancel rather than fill the History row with phantom 0.0 entries, but that is a Phase 11 polish question for the Android Engineer to weigh against the Data Engineer. The cancel confirmation dialog the Phase 1 review's Issue 11 added for the Session Runner Cancel is a separate flow (the Session Runner level "Stop this check in?" dialog) and remains in place above this gait test level Cancel. The user is therefore protected against a stray tap by the Session Runner's own confirmation if they hit the top level Cancel; the gait Capture Cancel below it is an intentional opt out that the user has had the safety framing for since the Instructions screen. One dialog at the higher level is enough; a second dialog at the gait level would be friction the Phase 0 framing concern 2 (cognitive fog) does not want.

#### Item c, "do not feel safe = skip" framing on the Instructions screen

**Verified yes.** The exact framing is in the body copy on `GaitInstructionsScreen.kt` line 49: "If you do not feel safe walking right now, you can skip this test." The Skip CTA at line 73 ("Skip for now") is the matching action. The "right now" qualifier is the load bearing word here: it tells the user the skip is per session, not a permanent waiver, and it removes any implication that the user is opting out of their own data collection forever. This is the framing the Patient Advocate would have written if the Patient Advocate were drafting the copy. No change requested.

### Findings

#### Finding 1, Cancel button reachability while the phone is in a front pocket, severity: medium

- **File:** `app/src/main/java/com/mustafan4x/msbattery/battery/gait/GaitCaptureScreen.kt` lines 62 to 73.
- **Problem:** the Cancel button is rendered on the screen during a capture in which the phone is, by design, in a front pocket. A user who feels unsafe mid walk and wants to abort has to (a) remove the phone from the pocket, (b) wake the screen, (c) locate the Cancel control, and (d) tap it, while still possibly off balance. This is not an ergonomic failure unique to this implementation (it is inherent to the front pocket capture posture), but it is worth surfacing because the Phase 0 framing standing objection 5 named "cancellable without shame" and the practical reachability of the cancel during the test is the load bearing detail. The current screen is also silent on what happens if the user simply stops walking; the capture continues to the 30 second mark and the pipeline produces a low quality score from the missing samples, which is acceptable per the Phase 0 framing concern 7 ("bad MS day" honesty), but the user has no in test signal that simply stopping is also a way out.
- **File affected:** `GaitCaptureScreen.kt`.
- **Recommended owner:** Android Engineer; UI/UX Designer for tone.
- **Recommended target phase:** Phase 11 polish. The minimum viable mitigation is a single sentence on the Instructions screen ("If you need to stop, you can take the phone out of your pocket and tap Cancel, or just stop walking and the test will end on its own with reduced confidence.") that primes the user before the capture begins. A more invasive Phase 11 ergonomic improvement would be a hardware volume key Cancel chord per Android accessibility guidance, but that is a future feature, not a Phase 4 blocker.

#### Finding 2, Cancelled state Done screen says "Captured but quality is low" rather than acknowledging the cancel, severity: medium

- **File:** `app/src/main/java/com/mustafan4x/msbattery/battery/gait/GaitTest.kt` lines 61 to 64; `GaitDoneScreen.kt` lines 34 to 38.
- **Problem:** when the user taps Cancel mid capture, the AE renders a zero quality `GaitDoneScreen`. Because the qualityScore is 0.0, the band the screen picks is "Captured but quality is low," which is the same band a heat fatigued user who finished the walk under bad conditions would see. The two cases are not the same: one is a user who chose to stop, the other is a user who completed but produced noisy data. Conflating them in the post test copy is a small dignity miss. The verbatim "Your gait test is complete." line on `GaitDoneScreen.kt` line 53 is also factually misleading after a cancel, because the test is not complete, it was cancelled.
- **File affected:** `GaitTest.kt` (route the Cancelled state to a different copy variant) or `GaitDoneScreen.kt` (accept a `wasCancelled` flag).
- **Recommended owner:** Android Engineer.
- **Recommended target phase:** Phase 4 fix is small (a `Cancelled` arm that renders distinct copy: "Test stopped. No data was saved for this round." plus a Continue CTA), but the Patient Advocate is comfortable with this landing in Phase 11 polish if Phase 4 close is tight on window. Marking it medium because it is the kind of detail the user notices and the kind that erodes trust over time, even though it does not corrupt data or block the battery.

#### Finding 3, no contextual messaging for mobility aid users on the Instructions screen, severity: medium

- **File:** `app/src/main/java/com/mustafan4x/msbattery/battery/gait/GaitInstructionsScreen.kt` lines 41 to 52.
- **Problem:** the Instructions copy assumes a user who can walk 30 seconds in a straight line with the phone in a front pocket. The Skip path is present, which is the right Phase 4 minimum (verified under Concern 2), but a wheelchair user, a walker user, or a cane user has no signal that the Skip path is the correct action for them rather than a personal opt out. The Phase 0 framing standing objection 2 explicitly named this concern, and the verbatim Galati 2024 quote ("I can only proceed so far because I'm in the car. I can't stand and balance.") is the load bearing evidence.
- **File affected:** `GaitInstructionsScreen.kt`.
- **Recommended owner:** Android Engineer for the copy change; UI/UX Designer for tone.
- **Recommended target phase:** Phase 11 polish, alongside the Phase 11 follow up the plan already named (alternate pocket positions, held in hand, etc.). A one sentence add to the body copy ("If you use a cane, walker, or wheelchair, the gait test may not produce useful results for you. Skipping is the right choice.") would close the gap. The Patient Advocate is not asking for a per profile setting that suppresses the gait test for declared mobility aid users (that touches the Room schema and the SPEC.md user profile) but is flagging the path for Phase 11.

#### Finding 4, low quality copy on a bad MS day session conflated with first time confused users, severity: low

- **File:** `app/src/main/java/com/mustafan4x/msbattery/battery/gait/GaitDoneScreen.kt` lines 34 to 38.
- **Problem:** the three quality bands are applied uniformly. A patient who walks well but produces noisy data because the phone shifted in their pocket gets the same "Captured but quality is low" message as a patient on a flare day. The Phase 0 framing concern 7 ("bad MS day" mode and quality score honesty) calls for the application to not punish bad day data; the current copy does not punish, but it also does not contextualize. This is properly Phase 9 or Phase 10 work where the per session view can surface a richer narrative; for Phase 4 the simple band copy is acceptable.
- **File affected:** `GaitDoneScreen.kt`.
- **Recommended owner:** Android Engineer in coordination with Clinical Outcomes Reviewer.
- **Recommended target phase:** Phase 11 polish or Phase 9 reporting work. Not a Phase 4 blocker.

#### Finding 5, "Your gait test is complete." line is technically accurate on Done but uninformative, severity: low

- **File:** `app/src/main/java/com/mustafan4x/msbattery/battery/gait/GaitDoneScreen.kt` line 53.
- **Problem:** the body line is purely transitional. A patient who finished a 30 second walk has earned a slightly warmer acknowledgment than "Your gait test is complete." compared with the BilateralTapTest's Phase 2 review Finding 5 which the Patient Advocate already flagged at the same low severity. For consistency across the battery, both modules' post test screens should land on similar warmth. The Phase 0 framing concern on warmth in retention copy applies here too.
- **File affected:** `GaitDoneScreen.kt`.
- **Recommended owner:** Android Engineer; UI/UX Designer for tone.
- **Recommended target phase:** Phase 11 polish.

### Recommendations for the Code Reviewer Task 12 verdict to consider

1. **No finding above is BLOCKING.** Findings 1 through 5 are deferrable. Finding 1 (Cancel reachability) is the closest to a real Phase 4 concern but the mitigation the Patient Advocate recommends is a one sentence Instructions screen add, which the Code Reviewer can either fold into the Phase 4 close as a small follow up or carry forward to Phase 11 with a note in `STATUS.md` Resume notes. Findings 2 through 5 are Phase 11 polish.
2. **AE flagged item a (the "Get ready to walk" headline) is endorsed.** The Code Reviewer can treat this as a design improvement over the plan's literal copy spec rather than a deviation from the plan; the Patient Advocate's endorsement is on the record above.
3. **AE flagged item b (Cancelled rendered as zero quality Done) is acceptable for Phase 4 in its current shape.** The Code Reviewer should not require a dedicated cancel confirmation screen. The Patient Advocate would prefer the orchestrator to skip persistence on cancel rather than insert a phantom 0.0 row in History (per Finding 2 deferred consideration), but the current shape does not corrupt data and is appropriate for Phase 4 close.
4. **AE flagged item c ("do not feel safe = skip" framing) is verified verbatim.** No copy change needed at this level.
5. **Cross cutting note for the Code Reviewer.** No copy in the reviewed surface uses em dashes, en dashes, or hyphens as prose punctuation, no emojis are present, and no AI attribution is in any docstring. The KDoc on `GaitTest.kt` line 25 ("Hilt free") and `GaitTestViewModel.kt` line 32 ("`composable("session")`") are honest engineering KDoc, not user facing copy, and are out of scope for the Patient Advocate review.

### Uncertainties

1. The Patient Advocate did not run the application on a physical device. All findings are from reading the source. The Cancel reachability assessment in Finding 1 is the one finding where running the app with the phone actually in a pocket would sharpen the recommendation; the source level conclusion is sufficient to flag it now.
2. The Patient Advocate did not verify whether the orchestrator persists a `TestResultEntity` row when `GaitTest.skippedPayload()` is delivered with `qualityScore = 0.0` and `rawSensorRelativePath = null`. The recommended Phase 11 behavior (skip persistence on cancel) depends on the orchestrator's current handling, which the Patient Advocate did not read in this pass; flagging for the Android Engineer and Data Engineer to confirm.
3. The Patient Advocate did not verify the `BilateralTapTest`'s instructions copy for cross battery consistency in Finding 5. The Phase 2 review at this same date's prior entry covered that test in detail; the Patient Advocate is recommending the cross battery polish as a Phase 11 task without re reading `BilateralTapTest.kt` in this pass.

### Handoff

Patient Advocate hands back to the Project Manager. None of the five findings is a Phase 4 blocker; the Code Reviewer's Task 12 review can proceed without waiting on a fix commit. Findings 2 through 5 belong in the Phase 11 polish queue alongside the Phase 1 and Phase 2 carryover items already there. Finding 1 (Cancel reachability mitigation copy on Instructions) is small enough that the PM may elect to fold it into Phase 4 close as a one line copy add; the Patient Advocate is comfortable either way.

(End of 2026-05-07 Phase 4 entry. Append only convention: this entry is not edited in place.)
