# BaselineMS Phase 2: Tap Test Implementation Plan (Android)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace `MockTestModule` with `BilateralTapTest`, the first real `TestModule`, while landing the subset of the Phase 1 Patient Advocate review that affects screens Phase 2 touches. End state is a runnable app where the weekly battery executes a real bilateral tap test (30 second alternating tap on two on screen targets, dominant hand then non dominant hand), persists tap rate, inter tap interval coefficient of variation, asymmetry index, and miss rate to Room, and surfaces an honest quality score that does not punish a fatigued user.

**Architecture:** The `TestModule` interface from Phase 1 is the seam, so the orchestrator does not change. `BilateralTapTest` lives in `app/src/main/java/com/mustafanazeer/baselinems/battery/tap/` and splits into a pure Kotlin feature computation module (`TapEvent.kt`, `TapRound.kt`, `TapFeatures.kt`) and a Compose UI module (`BilateralTapTest.kt`). The pure module is JVM testable without Robolectric and is the home for all numeric math. The UI module is tested with Compose UI tests where they pay rent and otherwise verified through manual emulator walkthrough.

**Tech Stack:** Kotlin 1.9, Jetpack Compose (Material 3), Navigation Compose, Room (already wired), Kotlin Coroutines and Flow, JUnit 4, Robolectric, kotlinx-coroutines-test, `androidx.compose.ui.test`. Same stack as Phase 1.

**Related spec:** `~/src/BaselineMS/SPEC.md` Section 6.1 (Tap Test) and Section 11 (Testing Strategy).

**Related references:** `docs/source/clinical-references.md` "Bilateral Tap Test (proxy for 9 Hole Peg Test)" entry. Anchors are Tanigawa et al. 2017 (the MS specific construct anchor for tap rate as an upper limb dexterity measure), Mathiowetz et al. 1985 (the original 9HPT paper), and Feys et al. 2017 (the modern MS specific 9HPT clinical trial endpoint consensus). All three were verified by the Citation Auditor in Phase 0 and the Phase 0 to Phase 1 cleanup re audit; no new citations are introduced in this plan.

**Patient Advocate Phase 1 carryover:** the Patient Advocate flagged 18 issues at the end of Phase 1 (`docs/qa/patient-advocate-reviews.md` 2026-05-07 entry). 13 of those are folded into this plan because they affect screens Phase 2 touches (onboarding, session runner, settings) or because they make the Tap Test landable safely. 5 are gated on Compliance Reviewer or Data Engineer plus Database Administrator decisions and are addressed in Phase 2A below.

**Platform note:** All commands assume Linux. JDK toolchain note from `STATUS.md` Resume notes still applies: if `./gradlew` from the system shell errors with "javac not found", run with `JAVA_HOME=/snap/android-studio/209/jbr ./gradlew ...` or install OpenJDK 17.

---

## File Map

Files this plan creates or modifies:

```
~/src/BaselineMS/
├── STATUS.md                                     (modify: phase status flips)
├── docs/
│   ├── plans/
│   │   └── phase-2-tap-test.md                   (this file)
│   ├── qa/
│   │   ├── patient-advocate-reviews.md           (modify: append Phase 2 entry)
│   │   └── regression-checklist.md               (modify: add Phase 2 entries)
│   ├── source/
│   │   └── clinical-references.md                (no change; entries already audited)
│   └── security/
│       └── compliance-review.md                  (modify: append Phase 2 disclaimer entry)
└── app/src/main/java/com/mustafanazeer/baselinems/
    ├── battery/
    │   ├── tap/
    │   │   ├── TapEvent.kt                       (create)
    │   │   ├── TapRound.kt                       (create)
    │   │   ├── TapFeatures.kt                    (create)
    │   │   └── BilateralTapTest.kt               (create)
    │   └── MockTestModule.kt                     (no change; kept for unit tests)
    └── ui/
        ├── RootScreen.kt                         (modify: swap MockTestModule for BilateralTapTest)
        ├── common/
        │   └── EnumLabels.kt                     (create: human readable labels)
        ├── onboarding/
        │   ├── DisclaimerScreen.kt               (modify: chunk to three sentences, TextAlign.Start)
        │   └── ProfileSetupScreen.kt             (modify: ExposedDropdownMenuBox, no defaults, plausibility, TopAppBar copy)
        ├── home/
        │   ├── HomeScreen.kt                     (modify: warmer copy, spacer not double space, relative dates)
        │   └── SessionRunnerScreen.kt            (modify: persistent header, cancel confirmation, idle copy, completion copy)
        └── settings/
            └── SettingsScreen.kt                 (modify: year of birth label, About bodyLarge, Spacer not Text(" "))

app/src/test/java/com/mustafanazeer/baselinems/
├── battery/
│   └── tap/
│       ├── TapFeaturesTest.kt                    (create)
│       └── BilateralTapTestMetadataTest.kt       (create)
└── ui/
    └── common/
        └── EnumLabelsTest.kt                     (create)
```

After this phase ships, the data layer (`data/`), the orchestrator (`battery/BatteryOrchestrator.kt`), and the `TestModule` interface (`battery/TestModule.kt`) are unchanged. The Phase 2 surface is concentrated in `battery/tap/` (new) and seven existing UI files (modified for the Patient Advocate carryover).

---

## Phase 2A: Pre implementation ratification (PM dispatched, no code yet)

This phase block contains decisions that block code work. Until each is resolved, the code tasks that depend on them stay deferred. The PM dispatches the relevant specialists. None of these tasks produce a commit; they produce a written verdict in the relevant log.

### Task 1: Compliance Reviewer ratifies disclaimer copy changes

**Why this is needed:** Patient Advocate Issue 5 proposes splitting the SPEC.md Section 10 disclaimer ("Share with your neurologist for clinical decisions") into three left aligned sentences and softening the third sentence to "When you visit your neurologist, you can share these results to help the conversation." Issue 6 proposes changing the button copy "I understand" to "Got it, continue." Both changes touch the regulatory positioning the Compliance Reviewer signed off on in Phase 0 (`docs/security/compliance-review.md`). The Compliance Reviewer has veto power on this surface.

**Files reviewed:**
- `app/src/main/java/com/mustafanazeer/baselinems/ui/onboarding/DisclaimerScreen.kt:30-42`
- `SPEC.md` Section 10
- `docs/security/compliance-review.md` (Phase 0 sign off entry)

- [ ] **Step 1: PM dispatches Compliance Reviewer**

Dispatch the Compliance Reviewer per the `CLAUDE.md` Task tool dispatch template. The dispatch prompt names this plan, points at the three proposed sentences and the proposed button copy, and asks for a COMPLIANT, NON COMPLIANT (with remediation), or AMBIGUOUS verdict against FDA wellness device guidance and Google Play health apps policy. Per the agent brief, the Compliance Reviewer is required to fetch the relevant FDA and Play policy text in this session before ruling.

Proposed sentences (Patient Advocate Issue 5):

> This app is not a medical device. It does not diagnose or treat any condition.
>
> Please do not change your treatment based on what you see here.
>
> When you visit your neurologist, you can share these results to help the conversation.

Proposed button copy (Patient Advocate Issue 6):

> Got it, continue

- [ ] **Step 2: Receive verdict and update `docs/security/compliance-review.md`**

The Compliance Reviewer appends a new entry to `docs/security/compliance-review.md` with date, regime, artifact, wording reviewed, regulatory text consulted (with citation), and verdict. If COMPLIANT, the wording lands in Task 7 below. If NON COMPLIANT, the Compliance Reviewer's recommended remediation supersedes the Patient Advocate proposal. If AMBIGUOUS, the PM falls back to the existing Phase 0 disclaimer wording for sentence 3 and the existing "I understand" button, lands only the chunking and `TextAlign.Start` (which are structural, not wording, changes), and re flags this for Phase 11.

- [ ] **Step 3: Commit the compliance review log entry**

```bash
git add docs/security/compliance-review.md
git commit -m "compliance: phase 2 disclaimer copy ratification"
```

### Task 2: Data Engineer plus Database Administrator decision on nullable heightCm and profile edit path

**Why this is needed:** Patient Advocate Issue 4 proposes a "Skip for now" path on profile setup, which (in the Patient Advocate's framing) sets `heightCm` to null. `UserProfileEntity.heightCm` is currently `val heightCm: Double` (non null) per `app/src/main/java/com/mustafanazeer/baselinems/data/UserProfileEntity.kt:14`. Patient Advocate Issue 17 proposes an "Edit profile" path from Settings, which conflicts with `SPEC.md` Section 8's "append only at the Session level" framing: SPEC is silent on whether `UserProfileEntity` is mutable.

**Files reviewed:**
- `app/src/main/java/com/mustafanazeer/baselinems/data/UserProfileEntity.kt`
- `SPEC.md` Section 8
- `docs/data/schema.md` (if present; written by Data Engineer in Phase 1)

- [ ] **Step 1: PM dispatches Data Engineer with explicit two question scope**

Per the dispatch template. The two questions:

1. Is `heightCm` nullable for v1? If yes, write the migration (Room schema version bump or in place change before Phase 5 captures the schema export). If no, the Patient Advocate's "Skip for now" path stores a sentinel value the Data Engineer specifies.
2. Is `UserProfileEntity` mutable for v1? If yes, "Edit profile" updates the same row. If no, every profile edit produces a new row and the application reads the most recent row.

Hand the Data Engineer the relevant SPEC sections and the Patient Advocate Issues 4 and 17 rationale verbatim. Ask the Data Engineer to also answer whether `exportSchema` should flip in this phase or stay deferred to Phase 4 close per the existing `STATUS.md` Resume notes item 2.

- [ ] **Step 2: PM dispatches Database Administrator to review the Data Engineer proposal**

The DBA reviews the proposal, confirms migration safety, and signs off on the `exportSchema` question independently. The Phase 1 DBA audit already passed on the version 1 schema; this dispatch is specifically about the proposed change.

- [ ] **Step 3: Update `docs/data/schema.md` with the decision**

Either file the decision (with the chosen design and rationale) or document the deferral (heightCm stays non null, profile remains effectively immutable for v1, the Patient Advocate's Issues 4 and 17 are deferred to Phase 11 polish or earlier if a beta tester surfaces the friction).

- [ ] **Step 4: Commit the schema decision**

```bash
git add docs/data/schema.md
git commit -m "data: phase 2 decision on nullable heightCm and profile edit path"
```

### Task 3: Clinical Validator design sign off on BilateralTapTest

**Why this is needed:** the Clinical Validator has veto power on test designs. Phase 0 sign off was conditional on the Bays 2015 reference being verified, which was resolved in the Phase 0 to Phase 1 transition note in `docs/source/clinical-references.md` (the reference was replaced with Tanigawa et al. 2017). The Clinical Validator's Phase 0 sign off note explicitly defers the Phase 2 sign off to "before it ships" per `agents/01-clinical-validator.md` Phase 2 task 5. This is that sign off.

**Files reviewed:**
- `SPEC.md` Section 6.1
- `docs/source/clinical-references.md` Bilateral Tap Test entry
- The BilateralTapTest design summary below (this same task)

**BilateralTapTest design summary (for the Clinical Validator's review):**

- Two on screen circular targets, left and right, each at least 96 dp visible diameter (more than the 48 dp minimum WCAG floor; sized for tremor and weakness tolerance per Patient Advocate Phase 0 framing concern 4). Hit test radius is the visible radius; no expanded forgiveness zone in v1 (a forgiveness zone would inflate tap counts beyond what 9HPT scoring supports, per Patient Advocate framing).
- One round per hand. Round order is dominant first then non dominant, matching 9HPT clinical convention per Clinical Validator Phase 0 sign off item 3.
- Each round is 30 seconds of alternating taps. Counter starts at zero; instructions and a countdown timer are persistently visible (Patient Advocate Issue 14).
- A tap is "valid" when (a) it lands inside one of the two target hit regions, AND (b) it is on the opposite side from the previous valid tap (alternation constraint). The very first valid tap of a round is the first in target tap that lands inside any target.
- A tap is a "non alternating in target tap" when it lands inside the same target as the previous valid tap. Recorded as a `TapEvent` with `kind = NON_ALTERNATING`, but does not advance the alternation pointer.
- A tap is "off target" when it lands outside both target hit regions. Recorded only as an integer counter on the `TapRound`, not as an individual `TapEvent`, because the side of an off target tap is not meaningful for v1.
- Features computed at the end of each round and rolled up at the end of both rounds:
  - `dominant_tap_rate_hz`: count of valid taps in dominant round divided by 30. Spec name: "tap rate (taps per second)" per `SPEC.md` Section 6.1.
  - `non_dominant_tap_rate_hz`: same for non dominant round.
  - `dominant_iti_cv`: coefficient of variation (stddev divided by mean) of inter tap intervals in milliseconds among valid taps in the dominant round.
  - `non_dominant_iti_cv`: same for non dominant round.
  - `asymmetry_index`: `(dominant_tap_rate_hz - non_dominant_tap_rate_hz) / mean(dominant_tap_rate_hz, non_dominant_tap_rate_hz)`. Signed: positive when dominant is faster than non dominant, which is the typical case. Stored raw; the Phase 9 Reporting layer decides display.
  - `miss_rate`: combined miss count across both rounds divided by total tap count across both rounds. Combined miss count is `non_alternating_taps + off_target_taps` summed across both rounds. Total tap count is `valid_taps + non_alternating_taps + off_target_taps` summed across both rounds. This combined definition was ratified by the Clinical Validator's Phase 2 sign off note in `docs/source/clinical-references.md` (Option A).
  - `dominant_in_target_taps`: integer count of valid plus non alternating taps in the dominant round (i.e. all in target taps).
  - `non_dominant_in_target_taps`: same for non dominant round.
  - `dominant_off_target_taps`: integer count of off target taps in the dominant round.
  - `non_dominant_off_target_taps`: same for non dominant round.
- Quality score, value in 0.0 to 1.0:
  - 0.0 if either round produced fewer than 10 valid taps (insufficient stride count analog; signals the patient could not engage with the test).
  - Otherwise, multiply 1.0 by:
    - factor 1: `min(1.0, total_valid_taps / 60.0)` (60 is a soft target across both rounds; a healthy adult typically produces 100 plus taps per round, so 60 across both rounds is very forgiving).
    - factor 2: `1.0 - miss_rate` (a session where most taps missed scores low even with sufficient counts).
    - factor 3: `1.0 - clamp((dominant_iti_cv + non_dominant_iti_cv) / 2, 0.0, 1.0)` (extremely irregular tapping reduces confidence; a CV near 0.0 means metronomic; a CV at or above 1.0 means the standard deviation exceeds the mean and the user was likely not engaged).
  - The final quality score is the product of the three factors, clamped to 0.0 to 1.0.
- Quality score copy never uses the words "invalid", "failed", or "please retry" per Patient Advocate Phase 0 framing standing objection 6. The user sees the score on the post test screen as descriptive text only (e.g. "Recorded with high confidence" for 0.75 plus, "Recorded with moderate confidence" for 0.4 to 0.75, "Recorded with lower confidence" below 0.4).

**Citations the Clinical Validator should reference (already audited; no new audit needed):**
- Tanigawa et al. 2017, *Multiple Sclerosis Journal: Experimental, Translational and Clinical*. The MS specific construct anchor: rapid tap rate as an MS upper limb dexterity measure, with greater statistical power than the 9HPT and the T25FW for detecting one and two year MS progression. Caveat from `docs/source/clinical-references.md`: Tanigawa used a desktop or web tap interface, not a smartphone touchscreen with two alternating bilateral targets, so the project's design is a smartphone adaptation of the same construct, not a literal protocol replication.
- Mathiowetz et al. 1985, *Occupational Therapy Journal of Research*. The original 9HPT methodology paper.
- Feys et al. 2017, *Multiple Sclerosis Journal*. The MSOAC consensus on the 9HPT as a modern MS specific clinical trial endpoint.

- [ ] **Step 1: PM dispatches Clinical Validator**

Per the dispatch template. The dispatch attaches this plan and asks the Clinical Validator for a SIGN OFF or REQUIRES DESIGN CHANGE verdict on the BilateralTapTest design summary above, with specific citations to the three references already in `docs/source/clinical-references.md`.

- [ ] **Step 2: Receive verdict and append the sign off note to `docs/source/clinical-references.md`**

The Clinical Validator appends a new note under "Notes from the Clinical Validator" titled "Phase 2 sign off, Bilateral Tap Test, 2026-05-07" or with the appropriate date. If REQUIRES DESIGN CHANGE, the design summary above is updated and the Clinical Validator re reviews. If SIGN OFF, code work in Phase 2C may begin.

- [ ] **Step 3: Commit the sign off**

```bash
git add docs/source/clinical-references.md
git commit -m "clinical: phase 2 bilateral tap test sign off"
```

---

## Phase 2B: Patient Advocate Phase 1 carryover (Android Engineer, before the Tap test ships)

The 13 issues folded into Phase 2 from the Phase 1 review. Each task is bite sized and TDD where math is involved; UI cleanups are committed in small slices so the Code Reviewer can read the diff. Issues that depend on Phase 2A verdicts are clearly marked.

### Task 4: Human readable enum labels (Patient Advocate Issue 1)

**Files:**
- Create: `app/src/main/java/com/mustafanazeer/baselinems/ui/common/EnumLabels.kt`
- Create: `app/src/test/java/com/mustafanazeer/baselinems/ui/common/EnumLabelsTest.kt`

**Why:** the user sees raw enum names (`RRMS`, `UNDISCLOSED`, `AMBIDEXTROUS`, etc.) rendered in screaming caps. Patient Advocate Issue 1 names this as a dignity concern and a cog fog concern. The fix introduces a UI layer label resolver that does not change the Room enums.

- [ ] **Step 1: Write the failing test**

File: `app/src/test/java/com/mustafanazeer/baselinems/ui/common/EnumLabelsTest.kt`

```kotlin
package com.mustafanazeer.baselinems.ui.common

import com.mustafanazeer.baselinems.data.Hand
import com.mustafanazeer.baselinems.data.MSType
import com.mustafanazeer.baselinems.data.Sex
import com.mustafanazeer.baselinems.data.TestType
import org.junit.Assert.assertEquals
import org.junit.Test

class EnumLabelsTest {

    @Test fun sexLabels() {
        assertEquals("Female", Sex.FEMALE.displayLabel())
        assertEquals("Male", Sex.MALE.displayLabel())
        assertEquals("Other", Sex.OTHER.displayLabel())
        assertEquals("Prefer not to say", Sex.UNDISCLOSED.displayLabel())
    }

    @Test fun handLabels() {
        assertEquals("Left", Hand.LEFT.displayLabel())
        assertEquals("Right", Hand.RIGHT.displayLabel())
        assertEquals("Either hand", Hand.AMBIDEXTROUS.displayLabel())
    }

    @Test fun msTypeLabels() {
        assertEquals("Relapsing remitting (RRMS)", MSType.RRMS.displayLabel())
        assertEquals("Primary progressive (PPMS)", MSType.PPMS.displayLabel())
        assertEquals("Secondary progressive (SPMS)", MSType.SPMS.displayLabel())
        assertEquals("Clinically isolated syndrome (CIS)", MSType.CIS.displayLabel())
        assertEquals("Prefer not to say", MSType.UNDISCLOSED.displayLabel())
    }

    @Test fun testTypeLabels() {
        assertEquals("Bilateral Tap", TestType.TAP.displayLabel())
        assertEquals("Gait", TestType.GAIT.displayLabel())
        assertEquals("Low Contrast Vision", TestType.VISION.displayLabel())
        assertEquals("Symbol Digit", TestType.SDMT.displayLabel())
        assertEquals("Voice Reading", TestType.VOICE.displayLabel())
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.mustafanazeer.baselinems.ui.common.EnumLabelsTest"`

Expected: FAIL with `Unresolved reference: displayLabel`.

- [ ] **Step 3: Write the implementation**

File: `app/src/main/java/com/mustafanazeer/baselinems/ui/common/EnumLabels.kt`

```kotlin
package com.mustafanazeer.baselinems.ui.common

import com.mustafanazeer.baselinems.data.Hand
import com.mustafanazeer.baselinems.data.MSType
import com.mustafanazeer.baselinems.data.Sex
import com.mustafanazeer.baselinems.data.TestType

fun Sex.displayLabel(): String = when (this) {
    Sex.FEMALE -> "Female"
    Sex.MALE -> "Male"
    Sex.OTHER -> "Other"
    Sex.UNDISCLOSED -> "Prefer not to say"
}

fun Hand.displayLabel(): String = when (this) {
    Hand.LEFT -> "Left"
    Hand.RIGHT -> "Right"
    Hand.AMBIDEXTROUS -> "Either hand"
}

fun MSType.displayLabel(): String = when (this) {
    MSType.RRMS -> "Relapsing remitting (RRMS)"
    MSType.PPMS -> "Primary progressive (PPMS)"
    MSType.SPMS -> "Secondary progressive (SPMS)"
    MSType.CIS -> "Clinically isolated syndrome (CIS)"
    MSType.UNDISCLOSED -> "Prefer not to say"
}

fun TestType.displayLabel(): String = when (this) {
    TestType.TAP -> "Bilateral Tap"
    TestType.GAIT -> "Gait"
    TestType.VISION -> "Low Contrast Vision"
    TestType.SDMT -> "Symbol Digit"
    TestType.VOICE -> "Voice Reading"
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.mustafanazeer.baselinems.ui.common.EnumLabelsTest"`

Expected: PASS, four tests, zero failures.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mustafanazeer/baselinems/ui/common/EnumLabels.kt \
         app/src/test/java/com/mustafanazeer/baselinems/ui/common/EnumLabelsTest.kt
git commit -m "ui: human readable enum labels for profile and test type display"
```

### Task 5: Replace EnumDropdown with Material 3 ExposedDropdownMenuBox (Patient Advocate Issue 2)

**Files:**
- Modify: `app/src/main/java/com/mustafanazeer/baselinems/ui/onboarding/ProfileSetupScreen.kt`

**Why:** Patient Advocate Issue 2 names the existing two step "Change" button plus dropdown as a cog fog burden. `ExposedDropdownMenuBox` is one tap to open, one tap to choose, label visible at all times.

- [ ] **Step 1: Replace `EnumDropdown` with `EnumMenuBox` and switch the field rendering to label resolved**

File: `app/src/main/java/com/mustafanazeer/baselinems/ui/onboarding/ProfileSetupScreen.kt`

Delete the existing `private fun <T : Enum<T>> EnumDropdown(...)` at lines 83 to 102 and replace its three call sites at lines 58, 59, 60 plus the new helper. The full file after the change:

```kotlin
package com.mustafanazeer.baselinems.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mustafanazeer.baselinems.data.Hand
import com.mustafanazeer.baselinems.data.MSType
import com.mustafanazeer.baselinems.data.Sex
import com.mustafanazeer.baselinems.data.UserProfileDao
import com.mustafanazeer.baselinems.data.UserProfileEntity
import com.mustafanazeer.baselinems.ui.common.displayLabel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    userProfileDao: UserProfileDao,
    onComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var sex by remember { mutableStateOf(Sex.UNDISCLOSED) }
    var hand by remember { mutableStateOf(Hand.RIGHT) }
    var msType by remember { mutableStateOf(MSType.UNDISCLOSED) }
    var heightCmText by remember { mutableStateOf("") }
    var dobYearText by remember { mutableStateOf("") }

    val yearError = !isPlausibleYear(dobYearText)
    val heightError = !isPlausibleHeightCm(heightCmText)
    val canSave = !yearError && !heightError

    Scaffold(topBar = { TopAppBar(title = { Text("Set up profile (1 of 1)") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = dobYearText,
                onValueChange = { dobYearText = it.filter { ch -> ch.isDigit() }.take(4) },
                label = { Text("Year of birth (for example 1985)") },
                isError = dobYearText.isNotEmpty() && yearError,
                supportingText = {
                    if (dobYearText.isNotEmpty() && yearError) {
                        Text("Please enter a year between 1925 and 2026")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = heightCmText,
                onValueChange = { heightCmText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(5) },
                label = { Text("Height in cm (for example 168)") },
                isError = heightCmText.isNotEmpty() && heightError,
                supportingText = {
                    if (heightCmText.isNotEmpty() && heightError) {
                        Text("Please enter a height between 100 and 230 cm")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            EnumMenuBox(
                label = "Biological sex",
                options = Sex.values().toList(),
                selected = sex,
                labelOf = { it.displayLabel() },
                onSelected = { sex = it }
            )
            EnumMenuBox(
                label = "Dominant hand",
                options = Hand.values().toList(),
                selected = hand,
                labelOf = { it.displayLabel() },
                onSelected = { hand = it }
            )
            EnumMenuBox(
                label = "MS type (optional)",
                options = MSType.values().toList(),
                selected = msType,
                labelOf = { it.displayLabel() },
                onSelected = { msType = it }
            )
            Text(
                "We use these to personalize your trends. The MS type field is optional.",
                style = MaterialTheme.typography.bodySmall
            )
            Button(
                onClick = {
                    scope.launch {
                        val year = dobYearText.toInt()
                        val cal = java.util.Calendar.getInstance().apply { set(year, 0, 1) }
                        userProfileDao.insert(
                            UserProfileEntity(
                                dateOfBirthEpochMs = cal.timeInMillis,
                                biologicalSex = sex,
                                dominantHand = hand,
                                msTypeDisclosed = msType,
                                heightCm = heightCmText.toDouble()
                            )
                        )
                        onComplete()
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save and continue")
            }
        }
    }
}

private fun isPlausibleYear(text: String): Boolean {
    val n = text.toIntOrNull() ?: return false
    return n in 1925..2026
}

private fun isPlausibleHeightCm(text: String): Boolean {
    val n = text.toDoubleOrNull() ?: return false
    return n in 100.0..230.0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumMenuBox(
    label: String,
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = labelOf(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(labelOf(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
```

This single file edit closes Patient Advocate Issues 1 (raw enum names; via `displayLabel` calls), 2 (custom dropdown; via `ExposedDropdownMenuBox`), 3 (default values; both fields now start empty with placeholder copy), and the TopAppBar half of Issue 4 (the title now reads "Set up profile (1 of 1)" so the user knows this is the only setup screen). The "Skip for now" button half of Issue 4 is held for Step 2 below, which depends on Phase 2A Task 2.

- [ ] **Step 2: Apply the schema migration approved by the Data Engineer and DBA**

The Data Engineer's `docs/data/schema.md` "Phase 2 schema decisions, 2026-05-07" section ratified `heightCm: Double?` with a full `Migration(1, 2)` code block (Path A). The DBA's `docs/data/schema.md` "Phase 2 schema audit, DBA verdict, 2026-05-07" section reviewed the migration and recommended the KSP arg form for `exportSchema`. The Android Engineer applies all of the following from those two sections, in this commit ordering:

1. Add the KSP `room.schemaLocation` arg to `app/build.gradle.kts` and flip `exportSchema = true` in `AppDatabase.kt`. Build once to capture `schemas/com.mustafanazeer.baselinems.data.AppDatabase/1.json`. Commit this v1 snapshot capture as a separate commit before any version bump (per DBA Item 5 commit ordering).
2. Modify `app/src/main/java/com/mustafanazeer/baselinems/data/UserProfileEntity.kt` to make `heightCm: Double?` (nullable).
3. Bump `AppDatabase` `version` from 1 to 2.
4. Add the `MIGRATION_1_2` `Migration` object exactly as written in `docs/data/schema.md` Path A code, and wire it into the `Room.databaseBuilder(...)` call in `BaselineMSApp.kt` via `.addMigrations(MIGRATION_1_2)`.
5. Build again to capture the v2 snapshot, then commit the schema bump and migration.
6. Add the Robolectric migration test from `docs/data/schema.md` with the five DBA modifications (assert all seven post migration columns; add the empty table case method; verify `MigrationTestHelper` constructor signature against the actual `room-testing` version on the classpath; switch `check` to `assertEquals`; add the post migration null insert read back assertion).

The skip path Skip for now button below is then added to `ProfileSetupScreen.kt` after Save and continue:

```kotlin
TextButton(
    onClick = {
        scope.launch {
            userProfileDao.insert(
                UserProfileEntity(
                    dateOfBirthEpochMs = 0L,
                    biologicalSex = Sex.UNDISCLOSED,
                    dominantHand = Hand.RIGHT,
                    msTypeDisclosed = MSType.UNDISCLOSED,
                    heightCm = null
                )
            )
            onComplete()
        }
    },
    modifier = Modifier.fillMaxWidth()
) {
    Text("Skip for now (you can fill these in any time from Settings)")
}
```

`SettingsScreen.kt` already renders `p.heightCm?.toInt() ?: "Not provided"` in Task 9 below, so the nullable read site is covered.

- [ ] **Step 3: Run the build and existing tests**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`

Expected: BUILD SUCCESSFUL. All 23 Phase 1 tests still pass plus the four new `EnumLabelsTest` tests from Task 4 plus the new migration test from this task. Total expected: roughly 28 to 30 tests.

- [ ] **Step 4: Commit (in two commits, per DBA commit ordering)**

```bash
git add app/build.gradle.kts \
         app/src/main/java/com/mustafanazeer/baselinems/data/AppDatabase.kt \
         app/schemas/com.mustafanazeer.baselinems.data.AppDatabase/1.json
git commit -m "data: capture v1 Room schema snapshot via KSP exportSchema"
```

Then a second commit:

```bash
git add app/src/main/java/com/mustafanazeer/baselinems/ui/onboarding/ProfileSetupScreen.kt \
         app/src/main/java/com/mustafanazeer/baselinems/data/UserProfileEntity.kt \
         app/src/main/java/com/mustafanazeer/baselinems/data/AppDatabase.kt \
         app/src/main/java/com/mustafanazeer/baselinems/BaselineMSApp.kt \
         app/src/test/java/com/mustafanazeer/baselinems/data/Migration1To2Test.kt \
         app/schemas/com.mustafanazeer.baselinems.data.AppDatabase/2.json
git commit -m "ui(onboarding): exposed dropdown menu, no default values, plausibility validation, skip path; data: nullable heightCm via Migration(1, 2)"
```

### Task 6: Disclaimer chunking and (conditional) wording softening (Patient Advocate Issues 5 and 6)

**Files:**
- Modify: `app/src/main/java/com/mustafanazeer/baselinems/ui/onboarding/DisclaimerScreen.kt`

**Why:** Patient Advocate Issue 5 names dense centered body text as a low contrast sensitivity and cog fog burden. The structural fix (chunking into three left aligned sentences) is independent of regulatory wording. The wording softening of sentence 3 is gated on Phase 2A Task 1.

- [ ] **Step 1: Apply the structural fix and the ratified wording**

The Compliance Reviewer's `docs/security/compliance-review.md` Entry 3 ratified the bare Patient Advocate proposal for sentence 3 ("When you visit your neurologist, you can share these results to help the conversation.") and the "Got it, continue" button copy as both COMPLIANT. Both ship verbatim.

File: `app/src/main/java/com/mustafanazeer/baselinems/ui/onboarding/DisclaimerScreen.kt`

Replace the entire file with:

```kotlin
package com.mustafanazeer.baselinems.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun DisclaimerScreen(onAcknowledge: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(PaddingValues(24.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "BaselineMS",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = "This app is not a medical device. It does not diagnose or treat any condition.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Start
        )
        Text(
            text = "Please do not change your treatment based on what you see here.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Start
        )
        Text(
            text = "When you visit your neurologist, you can share these results to help the conversation.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Start
        )
        Button(
            onClick = onAcknowledge,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Got it, continue")
        }
    }
}
```

- [ ] **Step 2: Run the build and existing tests**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`

Expected: BUILD SUCCESSFUL, tests pass.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/mustafanazeer/baselinems/ui/onboarding/DisclaimerScreen.kt
git commit -m "ui(onboarding): chunk disclaimer to three left aligned sentences per patient advocate review"
```

### Task 7: HomeScreen warmer copy, spacer fix, relative dates (Patient Advocate Issues 7, 8, 9)

**Files:**
- Modify: `app/src/main/java/com/mustafanazeer/baselinems/ui/home/HomeScreen.kt`

**Why:** Issue 7 changes "Start weekly battery" to "Start this week's check in" and warms the empty state. Issue 8 replaces the double space hack with a `Spacer`. Issue 9 introduces relative dates ("Today, 14:30", "Yesterday, 09:15", "May 5") for session history.

- [ ] **Step 1: Add a `formatRelative(epochMs)` helper with a unit test**

File: `app/src/test/java/com/mustafanazeer/baselinems/ui/home/HomeScreenFormattersTest.kt`

```kotlin
package com.mustafanazeer.baselinems.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class HomeScreenFormattersTest {

    private fun atLocalNoon(year: Int, month: Int, day: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeZone = TimeZone.getDefault()
        cal.set(year, month, day, 12, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    @Test fun todayShowsTodayPrefix() {
        val now = System.currentTimeMillis()
        val label = formatRelative(now, locale = Locale.US)
        assertEquals(true, label.startsWith("Today, "))
    }

    @Test fun yesterdayShowsYesterdayPrefix() {
        val now = System.currentTimeMillis()
        val yesterday = now - 24L * 60 * 60 * 1000
        val label = formatRelative(yesterday, locale = Locale.US)
        assertEquals(true, label.startsWith("Yesterday, "))
    }

    @Test fun weekAgoShowsAbsoluteMonthDay() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -10)
        val label = formatRelative(cal.timeInMillis, locale = Locale.US)
        assertEquals(true, label.matches(Regex("[A-Z][a-z]+ \\d{1,2}")))
    }
}
```

Run: `./gradlew :app:testDebugUnitTest --tests "com.mustafanazeer.baselinems.ui.home.HomeScreenFormattersTest"`

Expected: FAIL with `Unresolved reference: formatRelative`.

- [ ] **Step 2: Implement `formatRelative` and rewrite `HomeScreen.kt`**

File: `app/src/main/java/com/mustafanazeer/baselinems/ui/home/HomeScreen.kt`

```kotlin
package com.mustafanazeer.baselinems.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mustafanazeer.baselinems.data.SessionDao
import com.mustafanazeer.baselinems.data.SessionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    sessionDao: SessionDao,
    onStartSession: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val sessions by sessionDao.observeAll().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BaselineMS") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onStartSession) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start this week's check in", style = MaterialTheme.typography.titleMedium)
            }
            Text("History", style = MaterialTheme.typography.titleMedium)
            if (sessions.isEmpty()) {
                Text(
                    "You have not run a check in yet. The first one takes about ten minutes " +
                        "and produces a record you can share with your neurologist next visit.",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sessions) { session ->
                        SessionRow(session)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: SessionEntity) {
    Column {
        Text(
            formatRelative(session.startedAtEpochMs, Locale.getDefault()),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            if (session.completedAtEpochMs != null) "Completed" else "In progress",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

internal fun formatRelative(epochMs: Long, locale: Locale): String {
    val now = Calendar.getInstance()
    val that = Calendar.getInstance().apply { timeInMillis = epochMs }
    val timeFmt = SimpleDateFormat("HH:mm", locale)
    val daysApart = run {
        val a = now.clone() as Calendar
        a.set(Calendar.HOUR_OF_DAY, 0); a.set(Calendar.MINUTE, 0)
        a.set(Calendar.SECOND, 0); a.set(Calendar.MILLISECOND, 0)
        val b = that.clone() as Calendar
        b.set(Calendar.HOUR_OF_DAY, 0); b.set(Calendar.MINUTE, 0)
        b.set(Calendar.SECOND, 0); b.set(Calendar.MILLISECOND, 0)
        ((a.timeInMillis - b.timeInMillis) / (24L * 60 * 60 * 1000)).toInt()
    }
    return when {
        daysApart == 0 -> "Today, ${timeFmt.format(Date(epochMs))}"
        daysApart == 1 -> "Yesterday, ${timeFmt.format(Date(epochMs))}"
        daysApart in 2..6 -> {
            val dow = SimpleDateFormat("EEEE", locale).format(Date(epochMs))
            "$dow, ${timeFmt.format(Date(epochMs))}"
        }
        else -> SimpleDateFormat("MMM d", locale).format(Date(epochMs))
    }
}
```

- [ ] **Step 3: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.mustafanazeer.baselinems.ui.home.HomeScreenFormattersTest"`

Expected: PASS, three tests.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/mustafanazeer/baselinems/ui/home/HomeScreen.kt \
         app/src/test/java/com/mustafanazeer/baselinems/ui/home/HomeScreenFormattersTest.kt
git commit -m "ui(home): warmer copy, spacer instead of double space, relative date formatting"
```

### Task 8: SessionRunnerScreen persistent header, cancel confirmation, idle and complete copy (Patient Advocate Issues 10, 11, 12, 13)

**Files:**
- Modify: `app/src/main/java/com/mustafanazeer/baselinems/ui/home/SessionRunnerScreen.kt`

**Why:** Issue 10 (persistent test progress header) is the load bearing precondition for Phase 2 — once `BilateralTapTest` hides its instructions during the 30 second tap window, the user has nothing reminding them what test they are on without this header. Issue 11 (cancel confirmation) prevents tremor patients from losing collected results. Issues 12 (idle copy) and 13 (complete copy) close the cog fog and tone gaps.

- [ ] **Step 1: Rewrite `SessionRunnerScreen.kt`**

File: `app/src/main/java/com/mustafanazeer/baselinems/ui/home/SessionRunnerScreen.kt`

```kotlin
package com.mustafanazeer.baselinems.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mustafanazeer.baselinems.battery.BatteryOrchestrator
import com.mustafanazeer.baselinems.ui.common.displayLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionRunnerScreen(
    orchestrator: BatteryOrchestrator,
    onFinished: () -> Unit
) {
    val state by orchestrator.state.collectAsState()
    var confirmingCancel by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("This week's check in") },
                actions = {
                    if (state !is BatteryOrchestrator.State.Completed) {
                        TextButton(onClick = { confirmingCancel = true }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val s = state) {
                BatteryOrchestrator.State.Idle -> {
                    CircularProgressIndicator()
                    Text(
                        "Getting your check in ready",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                is BatteryOrchestrator.State.Running -> {
                    val module = orchestrator.modules[s.index]
                    val total = orchestrator.modules.size
                    val current = s.index + 1
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "${module.testType.displayLabel()} (test $current of $total)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Estimated ${module.estimatedDurationSeconds} seconds",
                            style = MaterialTheme.typography.bodySmall
                        )
                        LinearProgressIndicator(
                            progress = { (s.index).toFloat() / total.toFloat() },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                    }
                    module.Content { result ->
                        orchestrator.recordResult(
                            testType = module.testType,
                            qualityScore = result.qualityScore,
                            features = result.features
                        )
                    }
                }
                BatteryOrchestrator.State.Completed -> {
                    Text(
                        "All done. Your check in has been saved.",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "You can view your results from the home screen, " +
                            "or share a report later from Settings.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = onFinished) { Text("Done") }
                }
            }
        }
    }

    if (confirmingCancel) {
        AlertDialog(
            onDismissRequest = { confirmingCancel = false },
            title = { Text("Stop this check in?") },
            text = {
                Text("Your results so far will not be saved. You can come back to it later.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmingCancel = false
                    orchestrator.cancel()
                    onFinished()
                }) { Text("Stop and discard") }
            },
            dismissButton = {
                TextButton(onClick = { confirmingCancel = false }) {
                    Text("Keep going")
                }
            }
        )
    }
}
```

- [ ] **Step 2: Run the build and existing tests**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`

Expected: BUILD SUCCESSFUL, all tests still pass (this change does not change orchestrator semantics).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/mustafanazeer/baselinems/ui/home/SessionRunnerScreen.kt
git commit -m "ui(session): persistent test header, cancel confirmation dialog, warmer idle and completion copy"
```

### Task 9: Settings DOB label, About bodyLarge, Spacer fix, conditional Edit profile (Patient Advocate Issues 15, 16, 17)

**Files:**
- Modify: `app/src/main/java/com/mustafanazeer/baselinems/ui/settings/SettingsScreen.kt`

**Why:** Issue 15 changes the "Date of birth: 1995-01-01" line to "Year of birth: 1995" because only the year was collected. Issue 16 raises the About disclaimer from `bodySmall` to `bodyLarge` and replaces the `Text(" ")` spacer hack with a `Spacer`. Issue 17 (Edit profile path) is gated on Phase 2A Task 2.

- [ ] **Step 1: Rewrite `SettingsScreen.kt` for the unconditional changes**

File: `app/src/main/java/com/mustafanazeer/baselinems/ui/settings/SettingsScreen.kt`

```kotlin
package com.mustafanazeer.baselinems.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mustafanazeer.baselinems.data.UserProfileDao
import com.mustafanazeer.baselinems.data.UserProfileEntity
import com.mustafanazeer.baselinems.ui.common.displayLabel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userProfileDao: UserProfileDao,
    onEditProfile: (() -> Unit)? = null
) {
    var profile by remember { mutableStateOf<UserProfileEntity?>(null) }
    LaunchedEffect(Unit) { profile = userProfileDao.getFirst() }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Profile", style = MaterialTheme.typography.titleMedium)
            val p = profile
            if (p == null) {
                Text("No profile yet.")
            } else {
                Text("Year of birth: ${yearOf(p.dateOfBirthEpochMs)}")
                Text("Sex: ${p.biologicalSex.displayLabel()}")
                Text("Dominant hand: ${p.dominantHand.displayLabel()}")
                Text("Height: ${p.heightCm?.toInt() ?: "Not provided"} cm")
                Text("MS type: ${p.msTypeDisclosed.displayLabel()}")
                if (onEditProfile != null) {
                    TextButton(
                        onClick = onEditProfile,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Edit profile") }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("About", style = MaterialTheme.typography.titleMedium)
            Text(
                "BaselineMS is not a medical device. " +
                    "It does not diagnose or treat any condition.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

private fun yearOf(epochMs: Long): Int {
    val cal = Calendar.getInstance()
    cal.timeInMillis = epochMs
    return cal.get(Calendar.YEAR)
}
```

Note: this file already references `p.heightCm?.toInt() ?: "Not provided"`, which assumes `heightCm` may be nullable. If Phase 2A Task 2 verdict was NO on nullable `heightCm`, change the line to `Text("Height: ${p.heightCm.toInt()} cm")`. The `onEditProfile` parameter is added now even if Issue 17 is deferred; it defaults to `null` and the button is hidden. Wiring `onEditProfile` to a nav route is the responsibility of Task 14 below if Phase 2A Task 2 verdict was YES on profile edit; otherwise leave it `null` at the call site.

- [ ] **Step 2: Run the build and existing tests**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/mustafanazeer/baselinems/ui/settings/SettingsScreen.kt
git commit -m "ui(settings): year of birth label, bodyLarge about, spacer fix, optional edit profile entry"
```

### Task 10: RootScreen race fix and BilateralTapTest wire in deferred to Phase 2C (Patient Advocate Issue 18)

**Files:**
- Modify: `app/src/main/java/com/mustafanazeer/baselinems/ui/RootScreen.kt`

**Why:** Issue 18 names a transient race where `startDestination` is computed before the suspending `hasProfile` resolves, briefly landing returning users on the profile setup screen. The fix: gate the `NavHost` behind a small splash that resolves both checks before any destination is picked. This is structural; the Tap test wire in lives in Phase 2C Task 17 below.

- [ ] **Step 1: Rewrite `RootScreen.kt` for the race fix only (still wiring `MockTestModule` in the session route)**

File: `app/src/main/java/com/mustafanazeer/baselinems/ui/RootScreen.kt`

```kotlin
package com.mustafanazeer.baselinems.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mustafanazeer.baselinems.BaselineMSApp
import com.mustafanazeer.baselinems.battery.BatteryOrchestrator
import com.mustafanazeer.baselinems.battery.MockTestModule
import com.mustafanazeer.baselinems.ui.home.HomeScreen
import com.mustafanazeer.baselinems.ui.home.SessionRunnerScreen
import com.mustafanazeer.baselinems.ui.onboarding.DisclaimerScreen
import com.mustafanazeer.baselinems.ui.onboarding.ProfileSetupScreen
import com.mustafanazeer.baselinems.ui.settings.SettingsScreen
import com.mustafanazeer.baselinems.util.DeviceInfo

private const val PREFS = "baselinems_prefs"
private const val KEY_DISCLAIMER = "disclaimer_acknowledged"

@Composable
fun RootScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as BaselineMSApp
    val nav = rememberNavController()

    var resolved by remember { mutableStateOf(false) }
    var disclaimerAcknowledged by remember { mutableStateOf(false) }
    var hasProfile by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        disclaimerAcknowledged = context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_DISCLAIMER, false)
        hasProfile = app.database.userProfileDao().getFirst() != null
        resolved = true
    }

    if (!resolved) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = when {
        !disclaimerAcknowledged -> "disclaimer"
        !hasProfile -> "profile"
        else -> "home"
    }

    NavHost(navController = nav, startDestination = startDestination) {
        composable("disclaimer") {
            DisclaimerScreen(onAcknowledge = {
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putBoolean(KEY_DISCLAIMER, true).apply()
                disclaimerAcknowledged = true
                nav.navigate(if (hasProfile) "home" else "profile") {
                    popUpTo("disclaimer") { inclusive = true }
                }
            })
        }
        composable("profile") {
            ProfileSetupScreen(
                userProfileDao = app.database.userProfileDao(),
                onComplete = {
                    hasProfile = true
                    nav.navigate("home") { popUpTo("profile") { inclusive = true } }
                }
            )
        }
        composable("home") {
            HomeScreen(
                sessionDao = app.database.sessionDao(),
                onStartSession = { nav.navigate("session") },
                onOpenSettings = { nav.navigate("settings") }
            )
        }
        composable("session") {
            val orchestrator = remember {
                BatteryOrchestrator(
                    modules = listOf(MockTestModule()),
                    sessionDao = app.database.sessionDao(),
                    testResultDao = app.database.testResultDao(),
                    deviceInfo = DeviceInfo.summary
                ).also { it.start() }
            }
            SessionRunnerScreen(orchestrator = orchestrator, onFinished = {
                nav.popBackStack("home", inclusive = false)
            })
        }
        composable("settings") {
            SettingsScreen(
                userProfileDao = app.database.userProfileDao(),
                onEditProfile = { nav.navigate("profile") }
            )
        }
    }
}
```

The Data Engineer's `docs/data/schema.md` "Phase 2 schema decisions, 2026-05-07" section ratified `UserProfileEntity` as MUTABLE with no schema change, and the DBA's audit section confirmed PASS on the mutability path with one constraint: the Edit profile path must reuse the existing row's `id` (load via `getFirst()`, mutate via `.copy(...)`, insert with `OnConflictStrategy.REPLACE`) so the singleton invariant holds. `ProfileSetupScreen.kt` in Task 5 currently always inserts a fresh `UserProfileEntity` (which mints a new `UUID`), which would break the invariant during edit. The Android Engineer must therefore extend `ProfileSetupScreen` to accept an optional pre populated `UserProfileEntity` parameter; when present, the Save and continue button reuses that entity's `id` rather than minting a new one. The Edit profile nav route in `RootScreen` passes the existing profile from `userProfileDao.getFirst()` to the screen.

- [ ] **Step 2: Run the build and existing tests**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/mustafanazeer/baselinems/ui/RootScreen.kt
git commit -m "ui(root): resolve disclaimer and profile checks before nav host first composition"
```

---

## Phase 2C: BilateralTapTest implementation (Android Engineer, TDD on the math)

This is the load bearing phase block. Pure Kotlin math files first, with TDD; Compose UI after; wire in last.

### Task 11: TapEvent and TapRound data models

**Files:**
- Create: `app/src/main/java/com/mustafanazeer/baselinems/battery/tap/TapEvent.kt`
- Create: `app/src/main/java/com/mustafanazeer/baselinems/battery/tap/TapRound.kt`

**Why:** the data models are the contract every other Tap module file consumes. Created without tests because there is no behavior; the behavior tests live in Task 12 where features are computed.

- [ ] **Step 1: Create `TapEvent.kt`**

File: `app/src/main/java/com/mustafanazeer/baselinems/battery/tap/TapEvent.kt`

```kotlin
package com.mustafanazeer.baselinems.battery.tap

enum class TapSide { LEFT, RIGHT }

enum class TapKind { VALID, NON_ALTERNATING }

data class TapEvent(
    val timestampMs: Long,
    val side: TapSide,
    val kind: TapKind
)
```

`TapEvent` represents only in target taps; the `side` and `kind` are meaningful only when the tap landed inside a target. Off target taps are recorded as a per round counter on `TapRound` (see Step 2) rather than as individual events, because the side of an off target tap is not meaningful for v1 per the Clinical Validator Phase 2 sign off note.

- [ ] **Step 2: Create `TapRound.kt`**

File: `app/src/main/java/com/mustafanazeer/baselinems/battery/tap/TapRound.kt`

```kotlin
package com.mustafanazeer.baselinems.battery.tap

enum class HandRole { DOMINANT, NON_DOMINANT }

data class TapRound(
    val role: HandRole,
    val durationMs: Long,
    val events: List<TapEvent>,
    val offTargetTaps: Int = 0
)
```

- [ ] **Step 3: Build to verify the new files compile**

Run: `./gradlew :app:assembleDebug`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/mustafanazeer/baselinems/battery/tap/TapEvent.kt \
         app/src/main/java/com/mustafanazeer/baselinems/battery/tap/TapRound.kt
git commit -m "battery(tap): tap event and tap round data models"
```

### Task 12: TapFeatures.computeRound (tap rate, ITI CV)

**Files:**
- Create: `app/src/main/java/com/mustafanazeer/baselinems/battery/tap/TapFeatures.kt`
- Create: `app/src/test/java/com/mustafanazeer/baselinems/battery/tap/TapFeaturesTest.kt`

**Why:** the per round features are the building blocks for everything else. TDD because the math is the load bearing piece of this phase.

- [ ] **Step 1: Write the failing tests for tap rate and ITI CV**

File: `app/src/test/java/com/mustafanazeer/baselinems/battery/tap/TapFeaturesTest.kt`

```kotlin
package com.mustafanazeer.baselinems.battery.tap

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.abs

class TapFeaturesTest {

    private fun valid(t: Long, side: TapSide) = TapEvent(t, side, TapKind.VALID)
    private fun nonAlt(t: Long, side: TapSide) = TapEvent(t, side, TapKind.NON_ALTERNATING)

    @Test
    fun tapRateMetronomic200msIs5Hz() {
        val events = (0..150 step 1).map { i ->
            valid(i * 200L, if (i % 2 == 0) TapSide.LEFT else TapSide.RIGHT)
        }
        val round = TapRound(HandRole.DOMINANT, durationMs = 30_000L, events = events)
        val r = TapFeatures.computeRound(round)
        assertEquals(5.0, r.tapRateHz, 0.01)
    }

    @Test
    fun tapRateIgnoresNonAlternatingAndOffTarget() {
        val events = listOf(
            valid(0L, TapSide.LEFT),
            valid(200L, TapSide.RIGHT),
            nonAlt(300L, TapSide.RIGHT)
        )
        val round = TapRound(HandRole.DOMINANT, durationMs = 30_000L, events = events, offTargetTaps = 5)
        val r = TapFeatures.computeRound(round)
        assertEquals(2.0 / 30.0, r.tapRateHz, 0.0001)
    }

    @Test
    fun itiCvMetronomicIsZero() {
        val events = (0..10).map { i -> valid(i * 200L, if (i % 2 == 0) TapSide.LEFT else TapSide.RIGHT) }
        val round = TapRound(HandRole.DOMINANT, durationMs = 30_000L, events = events)
        val r = TapFeatures.computeRound(round)
        assertEquals(0.0, r.itiCv, 0.0001)
    }

    @Test
    fun itiCvVariableIsPositiveAndCorrect() {
        val timestamps = listOf(0L, 100L, 300L, 600L, 1000L)
        val events = timestamps.mapIndexed { i, t -> valid(t, if (i % 2 == 0) TapSide.LEFT else TapSide.RIGHT) }
        val round = TapRound(HandRole.DOMINANT, durationMs = 30_000L, events = events)
        val r = TapFeatures.computeRound(round)
        val itis = listOf(100.0, 200.0, 300.0, 400.0)
        val mean = itis.average()
        val variance = itis.map { (it - mean) * (it - mean) }.average()
        val expected = Math.sqrt(variance) / mean
        assertEquals(true, abs(r.itiCv - expected) < 0.0001)
    }

    @Test
    fun roundWithFewerThanTwoValidTapsHasZeroItiCv() {
        val round = TapRound(HandRole.DOMINANT, durationMs = 30_000L, events = listOf(valid(0L, TapSide.LEFT)))
        val r = TapFeatures.computeRound(round)
        assertEquals(0.0, r.itiCv, 0.0001)
        assertEquals(1.0 / 30.0, r.tapRateHz, 0.0001)
    }

    @Test
    fun missCountsAreSurfaced() {
        val events = listOf(
            valid(0L, TapSide.LEFT),
            valid(100L, TapSide.RIGHT),
            nonAlt(150L, TapSide.RIGHT),
            valid(200L, TapSide.LEFT)
        )
        val round = TapRound(HandRole.DOMINANT, durationMs = 30_000L, events = events, offTargetTaps = 2)
        val r = TapFeatures.computeRound(round)
        assertEquals(3, r.validTaps)
        assertEquals(1, r.nonAlternatingTaps)
        assertEquals(2, r.offTargetTaps)
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.mustafanazeer.baselinems.battery.tap.TapFeaturesTest"`

Expected: FAIL with `Unresolved reference: TapFeatures` and `Unresolved reference: computeRound`.

- [ ] **Step 3: Write the minimal implementation**

File: `app/src/main/java/com/mustafanazeer/baselinems/battery/tap/TapFeatures.kt`

```kotlin
package com.mustafanazeer.baselinems.battery.tap

import kotlin.math.sqrt

data class RoundFeatures(
    val tapRateHz: Double,
    val itiCv: Double,
    val validTaps: Int,
    val nonAlternatingTaps: Int,
    val offTargetTaps: Int
)

object TapFeatures {

    fun computeRound(round: TapRound): RoundFeatures {
        val valid = round.events.filter { it.kind == TapKind.VALID }
        val nonAlt = round.events.count { it.kind == TapKind.NON_ALTERNATING }
        val durationSec = round.durationMs / 1000.0
        val tapRateHz = if (durationSec > 0.0) valid.size / durationSec else 0.0

        val itiCv = if (valid.size < 2) 0.0 else {
            val itis = valid.zipWithNext { a, b -> (b.timestampMs - a.timestampMs).toDouble() }
            val mean = itis.average()
            if (mean <= 0.0) 0.0 else {
                val variance = itis.map { (it - mean) * (it - mean) }.average()
                sqrt(variance) / mean
            }
        }

        return RoundFeatures(
            tapRateHz = tapRateHz,
            itiCv = itiCv,
            validTaps = valid.size,
            nonAlternatingTaps = nonAlt,
            offTargetTaps = round.offTargetTaps
        )
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.mustafanazeer.baselinems.battery.tap.TapFeaturesTest"`

Expected: PASS, six tests, zero failures.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mustafanazeer/baselinems/battery/tap/TapFeatures.kt \
         app/src/test/java/com/mustafanazeer/baselinems/battery/tap/TapFeaturesTest.kt
git commit -m "battery(tap): per round tap rate and inter tap interval coefficient of variation with tests"
```

### Task 13: Cross round features: asymmetry, miss rate, quality score

**Files:**
- Modify: `app/src/main/java/com/mustafanazeer/baselinems/battery/tap/TapFeatures.kt`
- Modify: `app/src/test/java/com/mustafanazeer/baselinems/battery/tap/TapFeaturesTest.kt`

**Why:** asymmetry, miss rate, and quality score live across both rounds and produce the final feature payload the orchestrator persists.

- [ ] **Step 1: Append failing tests for cross round features**

Append to `TapFeaturesTest.kt` (still inside the same class):

```kotlin
    @Test
    fun asymmetryIndexZeroWhenSymmetric() {
        val even = (0..10).map { i -> valid(i * 200L, if (i % 2 == 0) TapSide.LEFT else TapSide.RIGHT) }
        val dom = TapRound(HandRole.DOMINANT, 30_000L, even)
        val nondom = TapRound(HandRole.NON_DOMINANT, 30_000L, even)
        val s = TapFeatures.computeSession(dom, nondom)
        assertEquals(0.0, s.asymmetryIndex, 0.0001)
    }

    @Test
    fun asymmetryIndexPositiveWhenDominantFaster() {
        val fast = (0..149).map { i -> valid(i * 200L, if (i % 2 == 0) TapSide.LEFT else TapSide.RIGHT) }
        val slow = (0..49).map { i -> valid(i * 600L, if (i % 2 == 0) TapSide.LEFT else TapSide.RIGHT) }
        val dom = TapRound(HandRole.DOMINANT, 30_000L, fast)
        val nondom = TapRound(HandRole.NON_DOMINANT, 30_000L, slow)
        val s = TapFeatures.computeSession(dom, nondom)
        assertEquals(true, s.asymmetryIndex > 0.0)
    }

    @Test
    fun missRateCombinesNonAlternatingAndOffTarget() {
        val dom = TapRound(
            HandRole.DOMINANT, 30_000L,
            events = listOf(
                valid(0L, TapSide.LEFT),
                valid(200L, TapSide.RIGHT),
                nonAlt(300L, TapSide.RIGHT)
            ),
            offTargetTaps = 1
        )
        val nondom = TapRound(
            HandRole.NON_DOMINANT, 30_000L,
            events = listOf(
                valid(0L, TapSide.LEFT),
                nonAlt(100L, TapSide.LEFT)
            ),
            offTargetTaps = 0
        )
        val s = TapFeatures.computeSession(dom, nondom)
        assertEquals(3.0 / 6.0, s.missRate, 0.0001)
    }

    @Test
    fun qualityZeroWhenEitherRoundUnderTenValidTaps() {
        val full = (0..40).map { i -> valid(i * 200L, if (i % 2 == 0) TapSide.LEFT else TapSide.RIGHT) }
        val sparse = (0..5).map { i -> valid(i * 200L, if (i % 2 == 0) TapSide.LEFT else TapSide.RIGHT) }
        val s = TapFeatures.computeSession(
            TapRound(HandRole.DOMINANT, 30_000L, full),
            TapRound(HandRole.NON_DOMINANT, 30_000L, sparse)
        )
        assertEquals(0.0, s.qualityScore, 0.0001)
    }

    @Test
    fun qualityHighOnHealthySession() {
        val full = (0..149).map { i -> valid(i * 200L, if (i % 2 == 0) TapSide.LEFT else TapSide.RIGHT) }
        val s = TapFeatures.computeSession(
            TapRound(HandRole.DOMINANT, 30_000L, full),
            TapRound(HandRole.NON_DOMINANT, 30_000L, full)
        )
        assertEquals(true, s.qualityScore > 0.9)
    }

    @Test
    fun featureMapHasAllExpectedKeys() {
        val full = (0..149).map { i -> valid(i * 200L, if (i % 2 == 0) TapSide.LEFT else TapSide.RIGHT) }
        val s = TapFeatures.computeSession(
            TapRound(HandRole.DOMINANT, 30_000L, full),
            TapRound(HandRole.NON_DOMINANT, 30_000L, full)
        )
        val map = s.toFeatureMap()
        val expected = setOf(
            "dominant_tap_rate_hz", "non_dominant_tap_rate_hz",
            "dominant_iti_cv", "non_dominant_iti_cv",
            "asymmetry_index", "miss_rate",
            "dominant_in_target_taps", "non_dominant_in_target_taps",
            "dominant_off_target_taps", "non_dominant_off_target_taps"
        )
        assertEquals(expected, map.keys)
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.mustafanazeer.baselinems.battery.tap.TapFeaturesTest"`

Expected: FAIL with `Unresolved reference: computeSession` and `Unresolved reference: toFeatureMap`.

- [ ] **Step 3: Extend `TapFeatures.kt`**

Append to `app/src/main/java/com/mustafanazeer/baselinems/battery/tap/TapFeatures.kt` (extend the file, do not rewrite the existing object):

```kotlin
data class SessionFeatures(
    val dominant: RoundFeatures,
    val nonDominant: RoundFeatures,
    val asymmetryIndex: Double,
    val missRate: Double,
    val qualityScore: Double
) {
    fun toFeatureMap(): Map<String, Double> = mapOf(
        "dominant_tap_rate_hz" to dominant.tapRateHz,
        "non_dominant_tap_rate_hz" to nonDominant.tapRateHz,
        "dominant_iti_cv" to dominant.itiCv,
        "non_dominant_iti_cv" to nonDominant.itiCv,
        "asymmetry_index" to asymmetryIndex,
        "miss_rate" to missRate,
        "dominant_in_target_taps" to (dominant.validTaps + dominant.nonAlternatingTaps).toDouble(),
        "non_dominant_in_target_taps" to (nonDominant.validTaps + nonDominant.nonAlternatingTaps).toDouble(),
        "dominant_off_target_taps" to dominant.offTargetTaps.toDouble(),
        "non_dominant_off_target_taps" to nonDominant.offTargetTaps.toDouble()
    )
}

fun TapFeatures.computeSession(dominant: TapRound, nonDominant: TapRound): SessionFeatures {
    val d = computeRound(dominant)
    val n = computeRound(nonDominant)
    val mean = (d.tapRateHz + n.tapRateHz) / 2.0
    val asymmetry = if (mean <= 0.0) 0.0 else (d.tapRateHz - n.tapRateHz) / mean
    val totalValid = d.validTaps + n.validTaps
    val totalMisses = d.nonAlternatingTaps + n.nonAlternatingTaps + d.offTargetTaps + n.offTargetTaps
    val total = totalValid + totalMisses
    val missRate = if (total == 0) 0.0 else totalMisses.toDouble() / total.toDouble()

    val quality = if (d.validTaps < 10 || n.validTaps < 10) 0.0 else {
        val factorCount = minOf(1.0, totalValid / 60.0)
        val factorMiss = 1.0 - missRate
        val cvAvg = (d.itiCv + n.itiCv) / 2.0
        val factorCv = (1.0 - cvAvg.coerceIn(0.0, 1.0))
        (factorCount * factorMiss * factorCv).coerceIn(0.0, 1.0)
    }

    return SessionFeatures(
        dominant = d,
        nonDominant = n,
        asymmetryIndex = asymmetry,
        missRate = missRate,
        qualityScore = quality
    )
}
```

Note: `computeSession` is declared as an extension on the `TapFeatures` object so the test calls `TapFeatures.computeSession(...)` matching the call site signature.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.mustafanazeer.baselinems.battery.tap.TapFeaturesTest"`

Expected: PASS, twelve tests total in `TapFeaturesTest`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mustafanazeer/baselinems/battery/tap/TapFeatures.kt \
         app/src/test/java/com/mustafanazeer/baselinems/battery/tap/TapFeaturesTest.kt
git commit -m "battery(tap): cross round asymmetry, miss rate, quality score, feature map"
```

### Task 14: BilateralTapTest module skeleton conforming to TestModule

**Files:**
- Create: `app/src/main/java/com/mustafanazeer/baselinems/battery/tap/BilateralTapTest.kt`
- Create: `app/src/test/java/com/mustafanazeer/baselinems/battery/tap/BilateralTapTestMetadataTest.kt`

**Why:** the metadata fields (`testType`, `displayName`, `instructions`, `estimatedDurationSeconds`) are part of the `TestModule` contract and are exercised by the orchestrator and the SessionRunnerScreen. Test the metadata first; the Compose UI follows in the next task.

- [ ] **Step 1: Write the failing test**

File: `app/src/test/java/com/mustafanazeer/baselinems/battery/tap/BilateralTapTestMetadataTest.kt`

```kotlin
package com.mustafanazeer.baselinems.battery.tap

import com.mustafanazeer.baselinems.data.TestType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BilateralTapTestMetadataTest {

    @Test
    fun moduleMetadata() {
        val module = BilateralTapTest()
        assertEquals(TestType.TAP, module.testType)
        assertEquals("Bilateral Tap", module.displayName)
        assertTrue(module.instructions.isNotEmpty())
        assertTrue(module.instructions.length > 30)
        assertEquals(70, module.estimatedDurationSeconds)
    }
}
```

The estimated duration is 70 seconds: 5 second pre test instructions, 30 seconds dominant round, 30 seconds non dominant round, plus a 5 second rest screen between rounds. The user facing copy in the SessionRunner header shows 70 to set expectations honestly.

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.mustafanazeer.baselinems.battery.tap.BilateralTapTestMetadataTest"`

Expected: FAIL with `Unresolved reference: BilateralTapTest`.

- [ ] **Step 3: Create the module skeleton**

File: `app/src/main/java/com/mustafanazeer/baselinems/battery/tap/BilateralTapTest.kt`

```kotlin
package com.mustafanazeer.baselinems.battery.tap

import androidx.compose.runtime.Composable
import com.mustafanazeer.baselinems.battery.TestModule
import com.mustafanazeer.baselinems.battery.TestResultPayload
import com.mustafanazeer.baselinems.data.TestType

class BilateralTapTest : TestModule {

    data class TapTestResult(
        override val qualityScore: Double,
        override val features: Map<String, Double>
    ) : TestResultPayload

    override val testType: TestType = TestType.TAP
    override val displayName: String = "Bilateral Tap"
    override val instructions: String =
        "Hold your phone with both hands. Tap the left and right circles, alternating, " +
            "as fast as you can manage. You will do one round with your dominant hand " +
            "and one round with your non dominant hand. Each round lasts 30 seconds."
    override val estimatedDurationSeconds: Int = 70

    @Composable
    override fun Content(onComplete: (TestResultPayload) -> Unit) {
        BilateralTapTestContent(onComplete = onComplete)
    }
}
```

`BilateralTapTestContent` is the composable defined in Task 15. The skeleton compiles because the function name is referenced; the body of `BilateralTapTestContent` is added in Task 15.

- [ ] **Step 4: Add the placeholder composable so the file compiles**

Append to `app/src/main/java/com/mustafanazeer/baselinems/battery/tap/BilateralTapTest.kt`:

```kotlin
@Composable
internal fun BilateralTapTestContent(onComplete: (TestResultPayload) -> Unit) {
    androidx.compose.material3.Text(
        "BilateralTapTest UI is implemented in the next task."
    )
}
```

This placeholder is replaced in Task 15. It exists now only so the metadata test runs without a UI compile error.

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.mustafanazeer.baselinems.battery.tap.BilateralTapTestMetadataTest"`

Expected: PASS, one test.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/mustafanazeer/baselinems/battery/tap/BilateralTapTest.kt \
         app/src/test/java/com/mustafanazeer/baselinems/battery/tap/BilateralTapTestMetadataTest.kt
git commit -m "battery(tap): bilateral tap test module skeleton with metadata test"
```

### Task 15: BilateralTapTest Compose UI: countdown, targets, rounds, completion

**Files:**
- Modify: `app/src/main/java/com/mustafanazeer/baselinems/battery/tap/BilateralTapTest.kt` (replace the placeholder `BilateralTapTestContent`)

**Why:** the UI is the seam where touchscreen events become `TapEvent`s. The Compose layer drives the round state machine: pre instructions → countdown → dominant round → rest → countdown → non dominant round → complete.

The UI is intentionally not unit tested at this layer because the math is fully covered by `TapFeaturesTest` and the wiring is straightforward; the Phase 2D Patient Advocate review and the manual emulator walkthrough verify the UI feel. A single Compose smoke test in Task 16 verifies the screen renders without crashing.

- [ ] **Step 1: Add the new imports at the top of `BilateralTapTest.kt`**

Append to the existing import block at the top of `app/src/main/java/com/mustafanazeer/baselinems/battery/tap/BilateralTapTest.kt`:

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
```

(`androidx.compose.runtime.Composable` is already imported from the skeleton task.)

- [ ] **Step 2: Replace the placeholder body in `BilateralTapTest.kt`**

Below the imports (and after the `class BilateralTapTest` declaration), the file should end with the following two top level constants, the sealed class, and the two composables. Replace everything from the `@Composable internal fun BilateralTapTestContent` placeholder at the bottom of the file down to the file's last line with this block:

```kotlin
private const val ROUND_DURATION_MS = 30_000L
private const val COUNTDOWN_SECONDS = 5
private const val REST_SECONDS = 5

private sealed class TapPhase {
    data object PreInstructions : TapPhase()
    data class Countdown(val role: HandRole, val secondsRemaining: Int) : TapPhase()
    data class Active(val role: HandRole, val startedAtMs: Long) : TapPhase()
    data object Rest : TapPhase()
    data object Done : TapPhase()
}

@Composable
internal fun BilateralTapTestContent(onComplete: (TestResultPayload) -> Unit) {
    var phase by remember { mutableStateOf<TapPhase>(TapPhase.PreInstructions) }
    var dominantEvents by remember { mutableStateOf<List<TapEvent>>(emptyList()) }
    var nonDominantEvents by remember { mutableStateOf<List<TapEvent>>(emptyList()) }
    var dominantOffTarget by remember { mutableStateOf(0) }
    var nonDominantOffTarget by remember { mutableStateOf(0) }
    var liveTapCount by remember { mutableStateOf(0) }
    var elapsedSec by remember { mutableStateOf(0) }

    LaunchedEffect(phase) {
        when (val p = phase) {
            is TapPhase.Countdown -> {
                delay(1000)
                phase = if (p.secondsRemaining > 1) {
                    p.copy(secondsRemaining = p.secondsRemaining - 1)
                } else {
                    elapsedSec = 0
                    TapPhase.Active(role = p.role, startedAtMs = System.currentTimeMillis())
                }
            }
            is TapPhase.Active -> {
                val totalSec = (ROUND_DURATION_MS / 1000).toInt()
                while (elapsedSec < totalSec) {
                    delay(1000)
                    elapsedSec += 1
                }
                if (p.role == HandRole.DOMINANT) {
                    phase = TapPhase.Rest
                } else {
                    val dominantRound = TapRound(
                        role = HandRole.DOMINANT,
                        durationMs = ROUND_DURATION_MS,
                        events = dominantEvents,
                        offTargetTaps = dominantOffTarget
                    )
                    val nonDominantRound = TapRound(
                        role = HandRole.NON_DOMINANT,
                        durationMs = ROUND_DURATION_MS,
                        events = nonDominantEvents,
                        offTargetTaps = nonDominantOffTarget
                    )
                    val features = TapFeatures.computeSession(dominantRound, nonDominantRound)
                    onComplete(
                        BilateralTapTest.TapTestResult(
                            qualityScore = features.qualityScore,
                            features = features.toFeatureMap()
                        )
                    )
                    phase = TapPhase.Done
                }
            }
            is TapPhase.Rest -> {
                var seconds = REST_SECONDS
                while (seconds > 0) {
                    delay(1000)
                    seconds -= 1
                }
                liveTapCount = 0
                phase = TapPhase.Countdown(role = HandRole.NON_DOMINANT, secondsRemaining = COUNTDOWN_SECONDS)
            }
            else -> Unit
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Tap the left and right circles, alternating, as fast as you can manage.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        when (val p = phase) {
            is TapPhase.PreInstructions -> {
                Text(
                    "You will do two 30 second rounds. Dominant hand first, then non dominant.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Button(onClick = {
                    liveTapCount = 0
                    elapsedSec = 0
                    dominantEvents = emptyList()
                    nonDominantEvents = emptyList()
                    dominantOffTarget = 0
                    nonDominantOffTarget = 0
                    phase = TapPhase.Countdown(role = HandRole.DOMINANT, secondsRemaining = COUNTDOWN_SECONDS)
                }) { Text("Start dominant hand round") }
            }
            is TapPhase.Countdown -> {
                val roleLabel = if (p.role == HandRole.DOMINANT) "Dominant hand" else "Non dominant hand"
                Text(roleLabel, style = MaterialTheme.typography.titleLarge)
                Text(
                    "Starting in ${p.secondsRemaining}",
                    style = MaterialTheme.typography.displayMedium
                )
            }
            is TapPhase.Active -> {
                val roleLabel = if (p.role == HandRole.DOMINANT) "Dominant hand" else "Non dominant hand"
                val totalSec = (ROUND_DURATION_MS / 1000).toInt()
                val remaining = (totalSec - elapsedSec).coerceAtLeast(0)
                Text("$roleLabel, $remaining seconds left", style = MaterialTheme.typography.titleMedium)
                Text("Taps so far: $liveTapCount", style = MaterialTheme.typography.bodyMedium)
                TwoTargets(
                    onInTargetTap = { side ->
                        val events = if (p.role == HandRole.DOMINANT) dominantEvents else nonDominantEvents
                        val previous = events.lastOrNull { it.kind == TapKind.VALID }
                        val kind = when {
                            previous == null -> TapKind.VALID
                            previous.side != side -> TapKind.VALID
                            else -> TapKind.NON_ALTERNATING
                        }
                        val event = TapEvent(
                            timestampMs = System.currentTimeMillis() - p.startedAtMs,
                            side = side,
                            kind = kind
                        )
                        if (p.role == HandRole.DOMINANT) {
                            dominantEvents = dominantEvents + event
                        } else {
                            nonDominantEvents = nonDominantEvents + event
                        }
                        if (kind == TapKind.VALID) liveTapCount += 1
                    },
                    onOffTargetTap = {
                        if (p.role == HandRole.DOMINANT) {
                            dominantOffTarget += 1
                        } else {
                            nonDominantOffTarget += 1
                        }
                    }
                )
            }
            is TapPhase.Rest -> {
                Text(
                    "Take a short break. The next round starts soon.",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
            is TapPhase.Done -> {
                Text(
                    "Tap test recorded. Returning to the session.",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TwoTargets(onInTargetTap: (TapSide) -> Unit, onOffTargetTap: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onOffTargetTap() })
            },
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Target(side = TapSide.LEFT, onTap = onInTargetTap)
        Spacer(Modifier.size(16.dp))
        Target(side = TapSide.RIGHT, onTap = onInTargetTap)
    }
}

@Composable
private fun Target(side: TapSide, onTap: (TapSide) -> Unit) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable { onTap(side) }
    ) {
        Text(
            text = if (side == TapSide.LEFT) "L" else "R",
            color = Color.White,
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.displaySmall
        )
    }
}
```

Notes on the design decisions in this code:

- The 120 dp diameter target sits well above the 48 dp WCAG floor and the 96 dp design summary minimum, sized for tremor and weakness tolerance per Patient Advocate Phase 0 framing.
- `Modifier.clickable { onTap(side) }` fires once per click (the indication ripple is reset at each finger lift), which correctly counts each finger press as one tap. An earlier version of this plan used a raw `pointerInput` event loop, which would have fired on every motion event during a press and over counted; that bug was caught in the writing-plans self review.
- A separate `elapsedSec` state drives the per second countdown display and the round end transition, so the displayed seconds remaining update every second. The single `delay(ROUND_DURATION_MS)` pattern would have left the displayed countdown frozen.
- Off target taps are captured by a parent `pointerInput { detectTapGestures }` on the Row that holds the two `Target` Boxes. When the user taps inside a target, the target's `Modifier.clickable` consumes the event and the parent's `detectTapGestures` does not fire (Compose pointer event consumption semantics). When the user taps the gap between targets or outside both target circles but inside the Row's bounds, only the parent's `detectTapGestures` fires and increments the off target counter. This is the resolution the Clinical Validator's Phase 2 sign off note ratified (Option A: capture both off target and non alternating taps as separate counts; persist both in the features map). The `miss_rate` feature therefore combines alternation discipline failures with aim failures, with each surfaced separately in `dominant_off_target_taps`, `non_dominant_off_target_taps`, and the in target tap counts so the Phase 9 Reporting layer can render them honestly.
- Off target taps that land outside the Row's bounds (for example, on the persistent test header at the top of the screen) are not captured. That is acceptable for v1: the user is positioned to tap near the targets, and a tap that lands on the header is most likely a deliberate user interaction with the header rather than a missed tap. The Patient Advocate Phase 0 framing concern 4 (do not punish tremor patients for off target taps beyond what the literature scores) is honored by the 120 dp generous target size; a tap that lands inside the Row but outside both 120 dp targets is a meaningful aim signal.

- [ ] **Step 2: Run the build and existing tests**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`

Expected: BUILD SUCCESSFUL, all tests still pass.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/mustafanazeer/baselinems/battery/tap/BilateralTapTest.kt
git commit -m "battery(tap): bilateral tap test compose ui with countdown, two rounds, and rest phase"
```

### Task 16: Compose smoke test for BilateralTapTest

**Files:**
- Create: `app/src/test/java/com/mustafanazeer/baselinems/battery/tap/BilateralTapTestRenderTest.kt`

**Why:** verifies the screen renders without crashing in the pre instructions phase. Deeper UI testing (countdown ticking, target touches counting taps) is expensive to test and is verified by the Patient Advocate review in Phase 2D and the manual emulator walkthrough. This single smoke test is enough to catch a Compose composable that fails on first composition.

- [ ] **Step 1: Write the test**

File: `app/src/test/java/com/mustafanazeer/baselinems/battery/tap/BilateralTapTestRenderTest.kt`

```kotlin
package com.mustafanazeer.baselinems.battery.tap

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class BilateralTapTestRenderTest {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun preInstructionsRender() {
        composeRule.setContent {
            BilateralTapTestContent(onComplete = {})
        }
        composeRule.onNodeWithText("Start dominant hand round").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run the test**

Run: `./gradlew :app:testDebugUnitTest --tests "com.mustafanazeer.baselinems.battery.tap.BilateralTapTestRenderTest"`

If `androidx.compose.ui.test.junit4` is not on the test classpath, the test fails to compile. In that case, add to `app/build.gradle.kts` `dependencies` block:

```kotlin
testImplementation("androidx.compose.ui:ui-test-junit4:1.6.7")
debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.7")
```

(Confirm exact version against the existing Compose BOM in `app/build.gradle.kts`; do not bump versions unilaterally.)

Expected after dependency add: PASS, one test.

If adding the dependency turns out to be non trivial in this Phase 2 window (Compose UI test on Robolectric has a history of friction), the Code Reviewer may approve deferring this single render test to Phase 11 polish provided the manual emulator walkthrough in Phase 2D Task 19 is documented. Document the deferral in the commit message.

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/mustafanazeer/baselinems/battery/tap/BilateralTapTestRenderTest.kt \
         app/build.gradle.kts
git commit -m "battery(tap): compose smoke test verifying pre instructions render"
```

### Task 17: Wire BilateralTapTest into RootScreen, replacing MockTestModule

**Files:**
- Modify: `app/src/main/java/com/mustafanazeer/baselinems/ui/RootScreen.kt`

**Why:** the user facing flow now runs the real Tap test. `MockTestModule` and its tests stay in the codebase because the `MockTestModuleTest` and `BatteryOrchestratorTest` and `BatteryFlowIntegrationTest` use it as a test fixture; only the `RootScreen` `session` route changes its module list.

- [ ] **Step 1: Edit `RootScreen.kt`**

In `app/src/main/java/com/mustafanazeer/baselinems/ui/RootScreen.kt`, replace the import:

```kotlin
import com.mustafanazeer.baselinems.battery.MockTestModule
```

with:

```kotlin
import com.mustafanazeer.baselinems.battery.tap.BilateralTapTest
```

and replace the `modules = listOf(MockTestModule())` line in the `composable("session")` block with:

```kotlin
modules = listOf(BilateralTapTest()),
```

- [ ] **Step 2: Run the build and existing tests**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`

Expected: BUILD SUCCESSFUL, all tests pass. Note: `BatteryFlowIntegrationTest` and `BatteryOrchestratorTest` continue to use `MockTestModule` because they exercise the orchestrator independently of any specific test module; they do not need to switch to `BilateralTapTest`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/mustafanazeer/baselinems/ui/RootScreen.kt
git commit -m "ui(root): wire bilateral tap test as the first concrete test module"
```

---

## Phase 2D: Phase close (PM dispatched, no new code)

### Task 18: Patient Advocate review of the Tap test

**Why:** per `agents/19-patient-advocate.md` Phase 2 task 5: "Review the tap test. Specific concerns: is the 30 second duration sustainable for a fatigued patient? Is the alternating bilateral structure clear? Does the dominant vs non dominant hand framing assume motor function the patient may not have?"

- [ ] **Step 1: PM dispatches Patient Advocate**

Per the dispatch template. The dispatch attaches the merged Phase 2 implementation, the design summary in Task 3 above, and the merged Phase 1 carryover commits. The Patient Advocate appends a new entry to `docs/qa/patient-advocate-reviews.md` in the same format as the Phase 1 entry.

- [ ] **Step 2: Receive verdict and address findings before phase close**

If APPROVED with recommended copy changes, the PM either folds them into the Phase 2 close commit (small) or files them as Phase 3 prep (anything substantive). If NEEDS REWORK, the PM dispatches the Android Engineer to apply the changes and re dispatches the Patient Advocate.

- [ ] **Step 3: Commit the Phase 2 review entry**

```bash
git add docs/qa/patient-advocate-reviews.md
git commit -m "qa: phase 2 patient advocate review of bilateral tap test"
```

### Task 19: Manual emulator walkthrough

**Why:** agents are headless. The user, or another human reviewer, runs the app on a real Android 12 plus emulator or device and walks the full flow:

1. Fresh install. The disclaimer renders as three left aligned sentences (per Phase 2A Task 1 verdict). The button copy is correct.
2. Profile setup. Both fields start empty. Plausibility validation rejects 2030 and 50. The dropdowns are one tap each. Save is disabled until both fields validate.
3. Home screen. The CTA reads "Start this week's check in". The empty state mentions "about ten minutes". History rows render relative dates.
4. Session runner. The persistent header shows the test name, the test count, and a progress bar. Cancel opens a dialog that defaults to Keep going.
5. Bilateral Tap test. The pre instructions screen reads correctly. Countdown ticks visibly. The two targets render at a comfortable size. Taps count. The round transitions through dominant → rest → non dominant → complete. The completion screen returns to the session runner.
6. Settings. Year of birth is shown without a fake day. About is at body large size. (If Edit profile shipped, the button is present and routes to a pre populated profile setup.)

The walkthrough is documented in a brief note appended to `docs/qa/regression-checklist.md`.

- [ ] **Step 1: PM dispatches QA Engineer to draft the regression checklist entries**

Per the dispatch template. The QA Engineer writes the new Phase 2 entries in `docs/qa/regression-checklist.md` based on this plan, then runs `./gradlew :app:testDebugUnitTest` and reports the results.

- [ ] **Step 2: User runs the manual walkthrough on a Pixel emulator running Android 12 plus**

This is the only step in the entire plan that an agent cannot drive. The user should expect to spend 5 to 10 minutes walking the flow, including running through both Tap rounds end to end. The QA Engineer documents the user's findings as the regression checklist entry.

- [ ] **Step 3: Commit the regression checklist update**

```bash
git add docs/qa/regression-checklist.md
git commit -m "qa: phase 2 regression checklist and manual walkthrough notes"
```

### Task 20: Code Reviewer sign off

**Why:** every PR before merge per `agents/10-code-reviewer.md`.

- [ ] **Step 1: PM dispatches Code Reviewer**

The Code Reviewer reads the entire Phase 2 diff (all commits from Phase 2A through Phase 2D so far), checks the inherited rules from `CLAUDE.md` (no dashes as prose punctuation, no emojis, no `Co-Authored-By` trailers, default to no comments), checks Kotlin and Compose idiom, and verifies the TDD discipline was followed in Tasks 4, 12, 13, and 14.

- [ ] **Step 2: Address review comments**

If the Code Reviewer requests changes, the PM dispatches the Android Engineer to apply them and re dispatches the Code Reviewer. If APPROVED, proceed to Task 21.

### Task 21: QA Engineer sign off

- [ ] **Step 1: PM dispatches QA Engineer**

Per `agents/09-qa-engineer.md`. The QA Engineer runs `./gradlew :app:testDebugUnitTest` one more time, confirms zero failures and zero new warnings, and signs off in `docs/qa/regression-checklist.md`.

- [ ] **Step 2: Commit the QA sign off**

```bash
git add docs/qa/regression-checklist.md
git commit -m "qa: phase 2 sign off"
```

### Task 22: Update STATUS.md to mark Phase 2 completed

**Files:**
- Modify: `STATUS.md`

- [ ] **Step 1: Update STATUS.md**

Flip the Phase 2 row to `completed`, fill in the date and the actual window cost, and move the Next phase line to Phase 3 Gait Signal Processing.

- [ ] **Step 2: Commit**

```bash
git add STATUS.md
git commit -m "phase 2 close: flip STATUS to completed, capture phase 3 prep items"
```

### Task 23: Mandatory check in protocol with the user before Phase 3

Per `CLAUDE.md`. The PM asks the three check in questions and waits for the user's answers before starting Phase 3.

---

## Self review checklist for the PM (after writing this plan)

This is the checklist the writing-plans skill mandates. Run before handing off.

1. **Spec coverage.** SPEC.md Section 6.1 specifies: 30 seconds alternating taps on two targets, dominant then non dominant; tap rate, ITI CV, dominant vs non dominant asymmetry, miss rate; quality score factors of sufficient taps, sustained engagement, and in target landings. Tasks 11 to 15 cover all six. SPEC.md Section 11 testing strategy: unit tests on signal processing math (Tasks 12 to 14), Compose UI smoke test (Task 16), real device walkthrough (Task 19). Patient Advocate Phase 1 carryover: Issues 1, 2, 3, 4 partial, 5 partial, 7, 8, 9, 10, 11, 12, 13, 15, 16, 18 all addressed. Issues 4 schema part, 5 wording part, 6 button copy, 17 profile edit gated on Phase 2A verdicts.
2. **Placeholder scan.** No "TBD", "implement later", or "similar to Task N" references; every code step has actual code; every test step has expected output. The Phase 2A verdicts (Compliance Reviewer, Data Engineer, DBA, Clinical Validator) are all in and the plan was revised on 2026-05-07 to remove the conditional flag constants and apply the ratified outcomes inline (bare Patient Advocate sentence 3, "Got it, continue" button, nullable `heightCm` with `Migration(1, 2)`, profile edit path).
3. **Type consistency.** `TestModule`, `TestResultPayload`, `TestType.TAP`, `BatteryOrchestrator`, `SessionEntity`, `TestResultEntity`, `UserProfileEntity`, `Hand`, `Sex`, `MSType` all match Phase 1 source. `TapEvent`, `TapSide`, `TapKind` (two values, `VALID` and `NON_ALTERNATING`), `TapRound` (with `events` and `offTargetTaps`), `HandRole`, `RoundFeatures` (with `validTaps`, `nonAlternatingTaps`, `offTargetTaps`), `SessionFeatures`, `BilateralTapTest`, `BilateralTapTestContent` are introduced consistently across Tasks 11 to 15. The Clinical Validator's Option A resolution (capture both off target and non alternating taps as separate counts; persist in features map) is implemented across Tasks 11 (data model), 12 (per round), 13 (per session and feature map), and 15 (Compose UI parent `pointerInput` capturing off target taps). The `displayLabel()` extension is referenced consistently across `EnumLabels.kt`, `ProfileSetupScreen.kt`, `SettingsScreen.kt`, and `SessionRunnerScreen.kt`. `TapFeatures.computeRound` and `TapFeatures.computeSession` are both attached to the `TapFeatures` object and called as `TapFeatures.computeRound(...)` and `TapFeatures.computeSession(...)` everywhere.

If the self review finds issues, the PM fixes them in this file before dispatching any specialist.
