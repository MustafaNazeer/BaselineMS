# Retention design specification, MS Neuro Battery

**Status:** Phase 0 deliverable, written by the UI/UX Designer.

**Scope:** the reminder copy and timing rules, the default reminder schedule, the onboarding flow targeting 75 percent registration to activation conversion, the contextual skip rules for the gait test, and the no streaks no shaming progress display. Together these define how the application earns the user's continued weekly engagement without nagging, shaming, or alarming a chronic disease patient.

**Audience:** the Android Engineer who will implement onboarding, notifications, and the home screen, plus the Patient Advocate who reviews every flow from the perspective of a person living with MS, plus the Compliance Reviewer who verifies the reminder copy against Google Play health app policy.

## 1. Why retention is a Phase 0 first class concern

Galati et al. 2024 (*JMIR Human Factors* 11:e57033, see `docs/source/clinical-references.md`) is the user experience companion paper to the Floodlight Open program. In the US MS cohort, day 30 retention with reminders enabled was 30.8 percent. Without reminders, it fell to 9.7 percent. Reminders roughly tripled retention. The same paper documents that an onboarding redesign moved registration to activation conversion from 53.9 percent to 74.6 percent, a 20 point retention floor before any test runs.

The broader Floodlight cohort analysis (Oh et al. 2024) identified clinical supervision as the single largest persistence driver. MS Battery does not have clinical supervision in the trial sense, but it operationalizes the same lever differently: the patient owned PDF for the neurologist gives the user a tangible, recurring clinical reason to keep weekly sessions, an artifact Floodlight Open did not provide.

These findings are why retention design is a Phase 0 deliverable, alongside design tokens, rather than a Phase 11 polish item. Getting reminders, onboarding, and the PDF reward right at the start is what determines whether the application has a beta cohort to validate against. The empirical floor: meet or exceed 30.8 percent day 30 retention with reminders enabled. The design goal, given the PDF reward mechanic Floodlight lacked: 50 percent or higher.

## 2. Reminder design

### 2.1 Hard rules

These are the constraints. Every implementation choice that follows must respect them.

1. **At most two reminders per day.** Even on missed weeks, the application never sends a third notification in any 24 hour window.
2. **Never after 7 PM local time.** A late evening notification reads as anxiety inducing for a chronic disease patient, can interfere with sleep, and tends to be dismissed without action. The 7 PM cap is one hour earlier than the Galati 2024 study's permitted window because the application targets a population that often experiences worse fatigue in the evening.
3. **Fire only on missed sessions.** If the user has completed their weekly session within the current week (Monday 00:00 to Sunday 23:59 local), no reminder fires that week. The application is not trying to maximize app opens; it is trying to ensure the weekly capture happens.
4. **Friendly tone.** No alarmist language. No deficit framing ("you have missed your last 2 sessions"). No deficit colored UI on the notification (no red, no warning icons).
5. **Periodic acknowledgment.** Roughly once a month, a reminder includes a short acknowledgment line ("Your sessions are helping your neurologist track your progress"). Not every reminder, since the line loses meaning by repetition.
6. **The PDF reward is surfaced from day one.** The first onboarding screen, the first reminder, and the home screen all reference the patient owned PDF. The reward is the point.
7. **No streaks. No shame copy.** "You broke your streak" is forbidden text. See Section 5.

### 2.2 Sample reminder copy strings

Six sample strings, designed to vary tone over a quarter so the user does not feel they are reading the same notification every week. The application rotates through these (in order or random with no immediate repeats; either is acceptable, the Android Engineer chooses the simpler implementation).

Each string is short enough to render fully in the Android notification preview (under roughly 65 characters in the body line; titles under roughly 40 characters). Each is written for a person living with MS, not a generic wellness app user.

1. **Title:** "Time for this week's check in"
   **Body:** "Five minutes to capture how you are doing. Your PDF report builds with each session."

2. **Title:** "This week's session"
   **Body:** "Five tests, five minutes. Your neurologist will see this in your next PDF."

3. **Title:** "Your weekly check in is ready"
   **Body:** "Whenever you have a quiet five minutes."

4. **Title:** "This week's session"
   **Body:** "Your sessions are helping your neurologist track your progress." (acknowledgment variant, used roughly monthly)

5. **Title:** "Ready when you are"
   **Body:** "Five minutes for your weekly battery."

6. **Title:** "This week's check in"
   **Body:** "A quiet moment is enough. Your report is waiting to update."

What is deliberately not present in any of the above:

- The word "streak" or any reference to consecutive sessions.
- The word "missed" or any deficit framing.
- The word "must," "should," or "need to."
- Disease forward language ("your MS data," "your symptoms"). The user knows why they are using the application; the notifications do not need to remind them they have a disease.
- Any urgency cue ("now," "today," "before midnight").

### 2.3 Default reminder schedule

**One weekly reminder, anchored to a user chosen day and time slot captured during onboarding.** Not a generic 10 AM ping.

Behavioral research on habit formation (BJ Fogg's tiny habits work, Wendy Wood on context dependent cue learning) consistently finds that anchoring a new behavior to an existing routine produces higher long term adherence than fixed clock time pings. Onboarding asks the user "When does your week tend to be quietest?" and offers a small set of slots: weekday morning, weekday lunch, weekday evening (capped at 6 PM), Saturday morning, Sunday morning, Sunday evening (capped at 6 PM). The chosen slot becomes the anchor.

If the user does not complete their session at the anchor time, a single second reminder fires 24 hours later in the same time band, never after 7 PM. If still not completed, the application waits until the next week's anchor. The application does not chase the user across the week.

The user can change their anchor slot at any time in Settings. The application does not nudge the user to change it.

### 2.4 Onboarding capture of the anchor slot

During the onboarding flow (Section 3), one screen asks: "When does your week tend to be quietest?" with the six slot options as large tap targets (64 dp height each). A "Choose later" option is available; if chosen, the application defaults to Saturday morning at 10 AM and tells the user they can change it any time in Settings. The default is Saturday morning because it correlates least with weekday work obligations across most user demographics, and because Saturday morning is the slot Galati 2024 identifies as least quit prone in the Floodlight US cohort.

### 2.5 Notification implementation notes for the Android Engineer

These are constraints, not implementation. The Android Engineer reports back if any conflict with Android 12+ notification semantics:

- **Channel.** A single notification channel `weekly_session_reminder`, default importance, no sound. Sound and vibration are off by default; the user can opt in via Settings if they want a more salient cue.
- **Permission.** Android 13+ requires `POST_NOTIFICATIONS` runtime permission. Onboarding asks for this at the slot selection screen, with copy explaining that the application will use it only for weekly session reminders. Refusal does not block onboarding; the user proceeds without reminders, and the home screen surfaces a quiet "Enable reminders" affordance the user can tap later.
- **Tap behavior.** Tapping the notification opens the application directly into the session runner, not into the home screen. Friction reduction (Section 3) extends to reminders.

## 3. Onboarding flow

### 3.1 Goal

Move the user from "first install" to "first completed test of the first session" in under three minutes, and from "first completed test" to "first completed full session with PDF preview" in under ten minutes. Galati 2024's redesigned onboarding moved registration to activation conversion from 53.9 percent to 74.6 percent. MS Battery's design target is at least 75 percent.

The single largest difference between MS Battery's onboarding and Floodlight Open's: MS Battery has no account, no email, no server side identifier (`SPEC.md` Section 10). Every step Floodlight needed for account creation is removed. This is a structural advantage; the design must not waste it.

### 3.2 Flow steps

The onboarding flow has at most six screens before the user starts their first test. Each screen is a single composable; the user can go back at any step. No screen blocks on a network call (the application has no `INTERNET` permission).

1. **Welcome and disclaimer (1 screen, mandatory).** The disclaimer text from `SPEC.md` Section 10 ("This is not a medical device. It does not diagnose or treat any condition. Do not change your treatment based on these results. Share with your neurologist for clinical decisions."). One acknowledgment tap. The screen also previews the PDF reward in plain copy: "Each session adds to a PDF you can share with your neurologist." This is the first time the user sees the reward; surfacing it here, before any data is requested, is deliberate.

2. **The five tests at a glance (1 screen, optional, "Skip" tap available).** A scrollable list of the five tests with one sentence each ("A 30 second walk in your pocket measures how you are walking this week"). Sets the expectation that the session is finite and quick. "Skip" is available because some users will want to dive in.

3. **Profile basics (1 screen, mandatory).** Date of birth, biological sex, dominant hand, height. Four fields. The application uses these for normative comparisons in the report; explained in one line on the screen. No name, no email, no phone number. MS type disclosure is offered with an "I would rather not say" option per `SPEC.md` Section 8 (`MSType.UNDISCLOSED` is a valid value).

4. **Reminder anchor slot (1 screen, mandatory if notifications are accepted).** Per Section 2.4 above. Asks the runtime notification permission first, then captures the slot, with a "Choose later" path that does not block.

5. **Permissions preview (1 screen, mandatory but lightweight).** Lists the permissions the application will ask for during the first session: motion sensors (always granted, no prompt), microphone (asked at the first voice test), camera (asked at the first vision test). Explains each in one line. No prompts fire on this screen; permissions are requested in context at the test that needs them.

6. **First session entry point (1 screen, mandatory).** "Ready when you are" with a single 64 dp action button: "Start first session." Tap proceeds directly into the first test (the Tap Test, since it is the lowest friction and requires no permissions at runtime).

### 3.3 Friction reduction principles

- **No account.** No email, no password, no phone verification.
- **No network.** Every screen renders instantly; no loading spinners.
- **One question per screen.** Profile basics is the exception (four fields), justified because they are uniformly short and form a single conceptual block.
- **Back is always available.** Material 3 default top app bar with back button.
- **Skip the optional screen.** Step 2 (test overview) is skippable with a clearly labeled "Skip" affordance, not buried.
- **Defer permissions to the moment of use.** Microphone and camera prompts come at the voice test and vision test, with single sentence in context explanations, not at onboarding.

### 3.4 Deep linking

Notification tap (Section 2.5) opens the session runner directly. The system back button from the session runner returns to the home screen. The notification does not deep link into a marketing screen, an upsell, an account screen (there is no account), or any other surface.

### 3.5 Free preview report after first session

This is the activation hook. After the user completes their first weekly session, the application generates a one page "preview report" PDF with the single session's results and a placeholder for the trends the next session will start to fill in. Surfaced as a "View your first report" button on the post session screen.

The mechanic addresses the gap Galati 2024 identifies: Floodlight users had to wait for cumulative weeks of data before seeing personal value. MS Battery shows the user a tangible payoff (a real PDF, in their hands, with their name and their results) on day one. The trends are sparse on day one by definition; the report says so honestly ("Trends will fill in as you complete more sessions") and shows a single point on each chart. The user has the receipt of the work they just did.

The PDF is generated on device (Phase 10's `PdfDocument` infrastructure is reused) and offered through the Share Intent so the user can save it, email it to themselves, or save it to Files immediately. Saving to Files in particular is the implicit suggestion: "this is yours, hold onto it."

## 4. Contextual skip rules for the gait test

### 4.1 Why the gait test gets a skip rule the others do not

The gait test requires the user to walk in a straight line for 30 seconds with the phone in their pocket. Galati 2024's qualitative analysis identifies "physical or contextual barriers" including unsafe locations as a top quit reason for the 2 minute walk test in the Floodlight cohort. The 2 minute walk test had the lowest day 1 retention of any test in that study (32.34 percent without reminders, 40.7 percent with). MS Battery shortens the walk to 30 seconds, but the contextual barrier remains: the user may not have a clear walking space at the moment of their session.

A user who hits the gait test in a cramped office, a moving vehicle, a cluttered apartment with a child or pet underfoot, or any other unsafe location must be able to defer the gait test without abandoning the entire session. Forcing them to walk would corrupt the measurement and create a fall risk. Forcing them to abandon the session loses four other tests' worth of data and lowers retention.

### 4.2 The defer affordance

When the gait test screen first loads (the pre test instructions screen, before the countdown), a secondary action labeled "Skip the walk for now" sits below the primary "Start walk" button. Both are 64 dp action buttons; the primary is filled, the secondary is text style. Both are equally reachable.

Tapping "Skip the walk for now" advances the session to the next test (the vision test, if the standard order is being followed). The session continues. The skipped gait test is recorded in the session as `skipped_contextual` (a small data model addition the Data Engineer will need to confirm in Phase 1; see open questions Section 7). The PDF report and trend charts handle a skipped test gracefully: the trend line skips that week's data point, no error, no shaming text.

The user is offered, on the post session screen, a one tap "Capture your walk now if you have time" affordance. This is opt in and does not block PDF generation. If they take it, the gait result is recorded with the same session ID. If they ignore it, the session closes and the application returns to the home screen.

### 4.3 What the rule explicitly does not do

- **Does not allow skipping any other test.** The other four tests have no contextual barrier comparable to the gait test. The user can pause the session and resume later, but they cannot skip individual tests. This preserves data integrity.
- **Does not prompt the user repeatedly.** If the user skips the walk this week, the application does not pop a "Walk now?" notification later in the day. The post session opt in is the only nudge.
- **Does not penalize the skip in the home screen progress display.** Skipped weeks count as completed for the "completed sessions this quarter" counter. The skip appears in the trend chart as a gap, with the legend explaining gaps are skipped tests, not bad results.

## 5. No streaks, no shaming

### 5.1 The progress metric

The home screen displays "Completed sessions this quarter" as the primary progress affordance. Examples:

- Week 1 of a quarter, no session yet: "0 of 13 sessions this quarter. Start anytime."
- Week 4, three completed: "3 of 13 sessions this quarter."
- Week 8, four completed (one missed week): "4 of 13 sessions this quarter."

The metric is non punitive. The denominator (13, the number of weeks in a quarter) is fixed and does not adjust to the user's start date; the user fills it in over time and a partial quarter is fine. There is no "streak," no "you missed one," no "X weeks in a row," no fire emoji and no leaderboard.

Quarterly framing is chosen because it matches the user's clinical context: most MS patients see their neurologist every three to six months, so "this quarter" maps to "since my last appointment, roughly." The PDF report uses the same window.

### 5.2 What is forbidden

- The word "streak" anywhere in user facing copy.
- Any "do not break your streak" or "X days in a row" copy.
- Any deficit framing ("you missed last week," "you have not completed any sessions this month").
- Any urgency cue on the home screen ("Last chance this week").
- Any guilt language ("Your data is incomplete without your weekly sessions").
- Any social proof framing ("X percent of users completed their session this week"). The application has no telemetry; this would be fabricated, which is forbidden by the user's global rules and by basic honesty.
- Any negative emoji (frowny face) on the home screen, in notifications, or in any user facing surface.

### 5.3 What is allowed

- The neutral count described in Section 5.1.
- The acknowledgment line in Section 2.2 string 4 ("Your sessions are helping your neurologist track your progress"), used roughly monthly, never as a guilt cue.
- A "View your report so far" button on the home screen any time at least one session has been completed. The button is the implicit reinforcement: "what you have done has produced something real."

## 6. Empirical retention floor and design target

| Metric | Floodlight floor (Galati 2024) | MS Battery design target |
|--------|-------------------------------|--------------------------|
| Day 30 retention with reminders, US MS cohort | 30.8 percent | 30.8 percent or higher (must clear) |
| Day 30 retention without reminders, US MS cohort | 9.7 percent | not directly measured (reminders are default on) |
| Registration to activation conversion (redesigned onboarding) | 74.6 percent | 75 percent or higher |
| Registration to activation conversion (pre redesign) | 53.9 percent | floor reference, not target |

MS Battery's headline retention target: meet or exceed 30.8 percent day 30 retention with reminders enabled. Stretch target, given the patient owned PDF reward Floodlight lacked: 50 percent or higher day 30 retention. Phase 5 does not measure retention; Phase 11 does, against the beta cohort.

The retention curve is reported in the README's `Retention` subsection per `SPEC.md` Section 9. Numbers are reported as measured, no rounding up, no aspirational claims. If the application does not clear 30.8 percent in beta, the README says so and the next iteration addresses why.

## 7. Open questions for PM and adjacent agents

Items where the UI/UX Designer made a defensible call but the relevant agent should weigh in:

1. **`skipped_contextual` as a test result state.** Section 4.2 introduces a skipped state for the gait test that does not count as a quality failure or a session abandon. The Data Engineer (Phase 1) needs to extend the data model to support this without introducing a NULL semantic on `TestResultEntity`. The PM should add this to the Phase 1 Data Engineer scope.
2. **Default reminder anchor slot.** Section 2.4 picks Saturday morning at 10 AM as the fallback when the user defers the slot choice. The Patient Advocate should confirm Saturday morning is appropriate for an MS audience, since morning fatigue is variable. Sunday afternoon is a defensible alternative.
3. **Acknowledgment line frequency.** Section 2.2 uses "roughly monthly" for the acknowledgment variant. Patient Advocate should weigh in on whether monthly is right or whether quarterly is less repetitive.
4. **Notification permission timing.** Section 3.2 step 4 asks for `POST_NOTIFICATIONS` at the slot selection screen, before the user has done any tests. The Android Engineer and Patient Advocate may prefer asking after the first session, when the user has already received value. The Designer's choice was upfront because the slot selection is meaningless without the permission, but the alternative is reasonable.
5. **Quarterly progress metric denominator.** Section 5.1 fixes the denominator at 13 (weeks per quarter). Some users will start mid quarter; the denominator does not adjust. Patient Advocate may prefer a denominator that adjusts to the user's start date, at the cost of a less stable mental model. Designer's call is to keep it stable.
6. **First session free preview report (Section 3.5).** The mechanic depends on Phase 10's `PdfDocument` infrastructure existing before the first session ever runs in production. In Phase 1 (the foundation phase) this will not exist; the application will need a placeholder text "Your first PDF will be available after Phase 10 ships" or equivalent in early builds. PM needs to sequence this; ideally Phase 10's PDF generation is brought forward enough that the activation hook is real by the time the beta cohort installs.
7. **Onboarding screen 2 ("five tests at a glance") skip behavior.** The "Skip" tap proceeds to profile basics. The Patient Advocate may prefer that skipping also surfaces a "you can read about the tests later in Settings" affordance, since some users dismiss without reading and then ask later. Designer's call defers that to the home screen "About" entry.

## 8. Coordination

- **Patient Advocate (agent 19)** reviews the reminder copy strings in Section 2.2 from the perspective of a person living with MS, plus the open questions in Section 7 (items 2, 3, 4, 5, 7).
- **Compliance Reviewer (agent 21)** reviews the reminder copy strings against Google Play health app policy, particularly the constraint that copy not make clinical claims or imply diagnosis.
- **Clinical Outcomes Reviewer (agent 15)** reviews the acknowledgment line ("Your sessions are helping your neurologist track your progress") for clinical accuracy. The line is true (the PDF is shared with the neurologist) and not a clinical claim, but the Reviewer has veto on user facing clinical content.
- **Citation Auditor (agent 16)** verifies that the Galati 2024 retention numbers cited throughout (30.8 percent, 9.7 percent, 53.9 percent, 74.6 percent) match the source paper. These numbers also appear in `SPEC.md` Section 9 and `docs/source/clinical-references.md`; consistency matters.
- **Data Engineer (agent 05)** confirms the `skipped_contextual` state addition to the data model (Section 7 item 1).
- **Android Engineer (agent 03)** consumes Section 2.5 (notification implementation notes), Section 3 (onboarding flow), and Section 4 (gait skip behavior) in Phase 1 and Phase 4.
- **Accessibility Specialist (agent 12)** verifies that the onboarding flow (Section 3) and the home screen progress display (Section 5) are TalkBack and Dynamic Type compliant.
