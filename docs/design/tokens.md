# Design tokens, BaselineMS

**Status:** Phase 0 deliverable, written by the UI/UX Designer.

**Scope:** Material 3 baseline plus an accessibility focused token set for a v1 mobile native Android application. There is no web frontend and no custom visual identity beyond what is specified here. Visual customization beyond this baseline is deferred to `future-ideas.md` and revisited after Phase 11.

**Audience:** the Android Engineer who will translate these tokens into Compose `ColorScheme`, `Typography`, and `Shapes` instances in Phase 1, plus the Accessibility Specialist who verifies WCAG compliance, plus the Patient Advocate who reviews from the perspective of a person living with MS.

## 1. Design rationale

The application's primary user is a person living with MS who may have any combination of mild visual impairment, mild cognitive fatigue, mild hand tremor or weakness, and mild gait instability (`SPEC.md` Section 3). The application is used at home, weekly, in roughly five to ten minute sessions, and produces a clinician facing PDF report. The design must therefore be:

1. **Sober.** This is a clinically grounded health tool, not a wellness lifestyle app. No flourishes, no playful copy, no decorative illustrations.
2. **High contrast at the default theme.** Body text must clear WCAG 2.1 AA (4.5:1) on every surface, with no reliance on the user enabling a separate high contrast mode.
3. **Generous tap targets.** Mild tremor, weakness, and intermittent dexterity changes are common in MS. Material 3's 48 dp minimum is the floor; primary test action buttons go to 64 dp.
4. **Quiet motion.** Reduced motion preferences are honored. Default animations are short and use easing rather than spring overshoot, since vestibular sensitivity is reported in some MS subtypes.
5. **No reliance on color alone.** Status, progress, and result information always pair color with a label, an icon, or a position cue.

Every deviation from the Material 3 default below is justified either by accessibility (WCAG 2.1 AA, Android accessibility guidance) or by clinical context (the MS patient profile in `SPEC.md` Section 3).

## 2. Color palette

### 2.1 Source seed and approach

The palette is derived from a Material 3 dynamic color seed of `#0B5FA8` (a deep, desaturated clinical blue). The seed produces the full Material 3 tonal palette via Material 3's HCT (Hue, Chroma, Tone) algorithm. Below are the resulting role tokens at the "baseline" tonal values Material 3 publishes, expressed as the explicit hex fallbacks the application ships when dynamic color is unavailable on the device (Android 11 and below, or when the user is on a device without Material You support).

The seed choice rationale: blue reads as clinical and trustworthy in healthcare contexts (well documented in healthcare UX literature), avoids the false reassurance of green ("everything is fine, ignore changes") and the alarm framing of red, and produces good contrast against both light and dark surfaces in Material 3's tonal mapping.

### 2.2 Light theme color roles

| Role | Hex | Role | Hex |
|------|-----|------|-----|
| primary | `#0B5FA8` | onPrimary | `#FFFFFF` |
| primaryContainer | `#D3E3FD` | onPrimaryContainer | `#001C38` |
| secondary | `#4A6072` | onSecondary | `#FFFFFF` |
| secondaryContainer | `#CDE5F8` | onSecondaryContainer | `#06192C` |
| tertiary | `#5C5B7E` | onTertiary | `#FFFFFF` |
| tertiaryContainer | `#E2DFFF` | onTertiaryContainer | `#181837` |
| error | `#BA1A1A` | onError | `#FFFFFF` |
| errorContainer | `#FFDAD6` | onErrorContainer | `#410002` |
| background | `#FCFCFF` | onBackground | `#1A1C1E` |
| surface | `#FCFCFF` | onSurface | `#1A1C1E` |
| surfaceVariant | `#DFE2EB` | onSurfaceVariant | `#42474E` |
| outline | `#73777F` | outlineVariant | `#C3C7CF` |

### 2.3 Dark theme color roles

| Role | Hex | Role | Hex |
|------|-----|------|-----|
| primary | `#A4C8FF` | onPrimary | `#003063` |
| primaryContainer | `#00468B` | onPrimaryContainer | `#D3E3FD` |
| secondary | `#B1CADC` | onSecondary | `#1C3142` |
| secondaryContainer | `#324859` | onSecondaryContainer | `#CDE5F8` |
| tertiary | `#C5C3EA` | onTertiary | `#2D2D4D` |
| tertiaryContainer | `#444364` | onTertiaryContainer | `#E2DFFF` |
| error | `#FFB4AB` | onError | `#690005` |
| errorContainer | `#93000A` | onErrorContainer | `#FFDAD6` |
| background | `#1A1C1E` | onBackground | `#E3E2E6` |
| surface | `#1A1C1E` | onSurface | `#E3E2E6` |
| surfaceVariant | `#42474E` | onSurfaceVariant | `#C3C7CF` |
| outline | `#8D9199` | outlineVariant | `#42474E` |

### 2.4 Computed contrast ratios

Contrast ratios computed using the WCAG 2.1 relative luminance formula. WCAG 2.1 AA requires 4.5:1 for body text and 3:1 for large text (18pt regular / 14pt bold and above) and for non text UI components and graphical objects. Computed values rounded down to the second decimal so the published number is never optimistic.

**Light theme:**

| Foreground | Background | Ratio | AA body (4.5:1) | AA large (3:1) |
|-----------|-----------|-------|-----------------|----------------|
| onPrimary `#FFFFFF` | primary `#0B5FA8` | 7.92 | pass | pass |
| onPrimaryContainer `#001C38` | primaryContainer `#D3E3FD` | 14.39 | pass | pass |
| onSecondary `#FFFFFF` | secondary `#4A6072` | 6.41 | pass | pass |
| onSecondaryContainer `#06192C` | secondaryContainer `#CDE5F8` | 14.78 | pass | pass |
| onTertiary `#FFFFFF` | tertiary `#5C5B7E` | 6.30 | pass | pass |
| onTertiaryContainer `#181837` | tertiaryContainer `#E2DFFF` | 13.55 | pass | pass |
| onError `#FFFFFF` | error `#BA1A1A` | 5.91 | pass | pass |
| onErrorContainer `#410002` | errorContainer `#FFDAD6` | 13.07 | pass | pass |
| onBackground `#1A1C1E` | background `#FCFCFF` | 16.36 | pass | pass |
| onSurface `#1A1C1E` | surface `#FCFCFF` | 16.36 | pass | pass |
| onSurfaceVariant `#42474E` | surfaceVariant `#DFE2EB` | 7.07 | pass | pass |
| outline `#73777F` | surface `#FCFCFF` | 4.07 | n/a (UI) | pass |
| outline `#73777F` | background `#FCFCFF` | 4.07 | n/a (UI) | pass |

**Dark theme:**

| Foreground | Background | Ratio | AA body (4.5:1) | AA large (3:1) |
|-----------|-----------|-------|-----------------|----------------|
| onPrimary `#003063` | primary `#A4C8FF` | 8.41 | pass | pass |
| onPrimaryContainer `#D3E3FD` | primaryContainer `#00468B` | 8.94 | pass | pass |
| onSecondary `#1C3142` | secondary `#B1CADC` | 9.14 | pass | pass |
| onSecondaryContainer `#CDE5F8` | secondaryContainer `#324859` | 8.27 | pass | pass |
| onTertiary `#2D2D4D` | tertiary `#C5C3EA` | 9.00 | pass | pass |
| onTertiaryContainer `#E2DFFF` | tertiaryContainer `#444364` | 8.30 | pass | pass |
| onError `#690005` | error `#FFB4AB` | 7.30 | pass | pass |
| onErrorContainer `#FFDAD6` | errorContainer `#93000A` | 7.46 | pass | pass |
| onBackground `#E3E2E6` | background `#1A1C1E` | 14.30 | pass | pass |
| onSurface `#E3E2E6` | surface `#1A1C1E` | 14.30 | pass | pass |
| onSurfaceVariant `#C3C7CF` | surfaceVariant `#42474E` | 7.13 | pass | pass |
| outline `#8D9199` | surface `#1A1C1E` | 5.20 | n/a (UI) | pass |

Every text on surface combination passes WCAG 2.1 AA at the body text threshold (4.5:1) in both themes. The non text UI tokens (outline, outlineVariant pairings) clear the 3:1 graphical objects threshold required by WCAG 2.1 AA Success Criterion 1.4.11.

### 2.5 Semantic color usage

- **primary.** The primary action: "Start session," "Continue," "Generate report." Used sparingly so that the eye learns to associate it with the next test step.
- **error.** Reserved for genuine error states: a session that failed quality checks, a permission denied, an export that failed. Never used for "test result is concerning." Clinical content is never colored as error or warning, since the application is not diagnostic and does not classify results into "good" and "bad" categories.
- **secondary, tertiary.** Used for chart series and supporting information. A user with red green color blindness can still distinguish primary from tertiary because the hues are well separated and luminance differs.
- **No reliance on color alone.** Every status indicator (quality pass or fail, ambient light pass or fail, session completion state) pairs the color with a text label and, where appropriate, an icon. WCAG 2.1 Success Criterion 1.4.1.

### 2.6 Deviation from Material 3 defaults

The Material 3 baseline scheme uses a generic purple primary. We override that with the clinical blue seed `#0B5FA8` because:

1. Purple does not read as clinical in healthcare UX research.
2. The blue tone selected is desaturated enough to feel sober, saturated enough to remain identifiable as the action color.

No tonal overrides are applied beyond the seed change. All `on*` and container relationships follow Material 3's default tonal mapping.

## 3. Typography

### 3.1 Type scale

Material 3 type scale. Roboto Flex is the system default on Android 12+; the application does not bundle a custom font in v1.

| Role | Size (sp) | Line height (sp) | Weight | Usage |
|------|-----------|------------------|--------|-------|
| displayLarge | 57 | 64 | Regular (400) | Reserved; not used in v1. |
| displayMedium | 45 | 52 | Regular (400) | Reserved; not used in v1. |
| displaySmall | 36 | 44 | Regular (400) | Test session "Session complete" celebration text only. |
| headlineLarge | 32 | 40 | Regular (400) | Top of major screens (Home, Session Complete, Report). |
| headlineMedium | 28 | 36 | Regular (400) | Test name on the test instructions screen. |
| headlineSmall | 24 | 32 | Regular (400) | Section headers within long screens (Settings, Report). |
| titleLarge | 22 | 28 | Regular (400) | Card titles, dialog titles. |
| titleMedium | 16 | 24 | Medium (500) | Subsection titles, chart titles. |
| titleSmall | 14 | 20 | Medium (500) | Reserved for compact list items in non critical surfaces. |
| **bodyLarge** | **16** | **24** | **Regular (400)** | **Default body text.** Instruction copy, descriptive text. |
| bodyMedium | 14 | 20 | Regular (400) | Reserved for non critical metadata only (timestamps, captions in Report). |
| bodySmall | 12 | 16 | Regular (400) | Reserved for the disclaimer block on the PDF report only. Never used in core test flows. |
| labelLarge | 14 | 20 | Medium (500) | Button labels. |
| labelMedium | 12 | 16 | Medium (500) | Reserved for non critical chips. |
| labelSmall | 11 | 16 | Medium (500) | Reserved; not used in v1. |

### 3.2 Body text minimum 16sp

The Material 3 default for `bodyMedium` (14sp) is below the recommended minimum body text size for accessibility on phones, particularly for users with mild visual impairment, which is common in MS (optic neuritis history, residual contrast sensitivity loss). The application's default body role is therefore `bodyLarge` at 16sp, not `bodyMedium`. `bodyMedium` is reserved for non critical metadata. `bodySmall` is reserved for the disclaimer block on the PDF report, where it is read with the page in hand at close range, and where the legal disclaimer style is conventional.

### 3.3 Dynamic Type

Compose honors the system font scale by default when sizes are expressed in `sp`. The application must remain usable at the 200 percent system font scale (Phase 11 accessibility audit verifies this). Layouts use scrollable containers wherever fixed height could cause clipping at 200 percent. No size is expressed in `dp` for text content.

### 3.4 Deviation from Material 3 defaults

Material 3 publishes the type scale as a guideline; using `bodyLarge` rather than `bodyMedium` as the default body role is a token assignment choice, not a deviation from the Material 3 scale itself. The choice is justified by the accessibility threshold above.

## 4. Spacing scale

A 4 dp base grid. Five canonical sizes:

| Token | dp | Usage |
|-------|-----|-------|
| spaceXs | 4 | Inline gap between an icon and adjacent text. Not used as a stand alone margin. |
| spaceSm | 8 | Gap between related elements within a single component (e.g., chip internal padding, chart gridline padding). |
| spaceMd | 16 | Default content margin on screen edges. Default gap between stacked unrelated elements (e.g., between two cards). |
| spaceLg | 24 | Gap between major sections within a screen (e.g., between the test instruction block and the start button). |
| spaceXl | 32 | Top and bottom safe areas on test screens, where the user's hands hold the phone and a thumb rest is desirable. |

All paddings, margins, and gaps in the application use one of these five values. Ad hoc values (e.g., 12 dp, 20 dp) are not allowed without a documented reason.

## 5. Tap targets

### 5.1 Minimums

- **Default minimum interactive size: 48 dp by 48 dp.** Material 3's published minimum and Google's accessibility floor for Android.
- **Primary test action buttons: 64 dp height, full width minus 32 dp horizontal margin (i.e., 32 dp on each side).** "Start test," "Begin walking," "Submit," "Continue to next test." These are the buttons the user taps in the middle of a test session, often with a thumb reaching across the screen, sometimes with mild tremor.
- **Tap test target tap zones: minimum 96 dp diameter circular targets, separated by at least 32 dp.** The Bilateral Tap Test (Phase 2) is the only screen in the application where the user is taking timed taps under task pressure. Underspecified targets here would corrupt the measurement, not just the experience. The 96 dp number is larger than the 64 dp action button target because the test is timed and the user is rotating between two targets quickly under fatigue.
- **Numeric keypad keys for SDMT (Phase 7): minimum 64 dp by 64 dp per key**, arranged in a 3 by 3 grid plus the zero, with at least 8 dp gap between keys. SDMT scoring depends on accurate single key entry under time pressure.

### 5.2 Touch slop and edges

- The minimum 48 dp interactive size applies to the touch target, not the visual size. A 32 dp visual icon button is acceptable if the surrounding tappable area is expanded to 48 dp by 48 dp. This is Compose's `Modifier.minimumInteractiveComponentSize()` default, which the application relies on rather than overriding.
- Test action buttons are placed at least 16 dp from screen edges to avoid accidental edge gesture conflicts on phones with gesture navigation.

### 5.3 Deviation from Material 3 defaults

Material 3 specifies 48 dp minimum. The 64 dp recommendation on primary test buttons, 96 dp on tap test targets, and 64 dp on the SDMT keypad exceed Material 3's floor. Each is justified by clinical context: the user has MS, is using the application during a timed test session, and tap accuracy is part of the measurement, not just the input. Larger than 64 dp on any other surface is not authorized without PM review.

## 6. Shapes and elevation

Material 3 default shape scale is preserved without override:

| Token | Corner radius |
|-------|---------------|
| shapeExtraSmall | 4 dp |
| shapeSmall | 8 dp |
| shapeMedium | 12 dp |
| shapeLarge | 16 dp |
| shapeExtraLarge | 28 dp |

Cards use `shapeMedium` (12 dp). Buttons use `shapeSmall` (8 dp) for filled buttons; the test action button is `shapeMedium` (12 dp) so the larger button does not look stamped out.

Elevation follows Material 3's tonal elevation model, not shadow elevation. Surfaces tinted with primary at increasing tonal levels rather than drop shadows. This reads cleanly on OLED displays at low ambient light, common when a fatigued user is using the application in bed.

## 7. Motion

- **Default duration: 200 ms.** Material 3's medium duration token. Long enough to be perceived as motion, short enough to feel responsive on a battery aware device.
- **Easing: standard easing (cubic bezier 0.2, 0.0, 0, 1.0).** No spring overshoot. Some MS patients report vestibular sensitivity, and overshoot can read as motion that "did not stop where it was supposed to."
- **Reduce Motion respected.** When `Settings.Global.TRANSITION_ANIMATION_SCALE` is 0 or the system Reduce Motion preference is on, transitions are replaced with cross fades or with no animation at all. Compose's `Animatable` honors the system animator scale by default.
- **No parallax, no autoplaying media, no decorative motion.** The test session UI uses motion only to indicate state change (test starting, test ending, result saved).

## 8. Optional high contrast theme

Triggered by an explicit user setting in Settings, off by default. Rationale for offering it: users with significant residual visual deficit (post optic neuritis with poor contrast sensitivity recovery) report the standard Material 3 baseline does not provide enough contrast at low ambient light. The user opts in; the application does not auto detect because false positive auto detection is more disruptive than the opt in tap.

### 8.1 High contrast light theme overrides

| Role | Default `#hex` | High contrast `#hex` | Rationale |
|------|----------------|---------------------|-----------|
| primary | `#0B5FA8` | `#003063` | Darker primary for stronger contrast on white. Computed contrast against `onPrimary #FFFFFF` rises from 7.92 to 16.04. |
| onSurface | `#1A1C1E` | `#000000` | True black on body text increases contrast on `surface #FCFCFF` from 16.36 to 21.0 (the maximum). |
| onSurfaceVariant | `#42474E` | `#1A1C1E` | Caption and metadata text moves from 7.07 to 16.36 contrast. |
| outline | `#73777F` | `#42474E` | Borders and dividers become more visible at 7.07 contrast against surface, up from 4.07. |

### 8.2 High contrast dark theme overrides

| Role | Default `#hex` | High contrast `#hex` | Rationale |
|------|----------------|---------------------|-----------|
| primary | `#A4C8FF` | `#D3E3FD` | Lighter primary on dark increases contrast against `onPrimary` body. |
| onSurface | `#E3E2E6` | `#FFFFFF` | True white body text on `surface #1A1C1E` rises from 14.30 to 17.94. |
| onSurfaceVariant | `#C3C7CF` | `#E3E2E6` | Caption and metadata text moves from 7.13 to 14.30. |
| outline | `#8D9199` | `#C3C7CF` | Borders go from 5.20 to 10.20 contrast against surface. |

### 8.3 Behavior

The high contrast theme is a Boolean opt in; selecting it switches both the active light and dark schemes to their high contrast variants. The user's separate light or dark preference is preserved. The high contrast theme does not change typography, spacing, tap targets, or motion tokens.

## 9. Per test interaction notes

These notes are interaction guidance for the Android Engineer to consume in Phases 2 through 8. They are not implementation; they constrain the visual and interaction shape of each test.

### 9.1 Bilateral Tap Test (Phase 2)

- Two large circular tap targets, 96 dp diameter, separated horizontally by at least 32 dp, vertically centered on the screen.
- Targets pulse subtly (8 percent opacity oscillation, 1 second period) only before the test begins, never during measurement (motion during a timed test is a measurement contaminant and a vestibular concern).
- Tap registration feedback is a 100 ms color tint shift on the target, not a particle burst, not a sound effect.
- Counter ("X taps") visible in `headlineSmall` at the top of the screen; live updating to give the user a sense of progress.
- The hand instruction ("Use your dominant hand") sits in `headlineMedium` above the targets and stays visible throughout the trial.

### 9.2 Gait Test (Phase 4)

- Pre test screen shows clear, brief instructions, with the contextual skip option (per `docs/design/retention.md`) reachable in one tap.
- During capture, the screen shows only a static "Walk now" message and a countdown. No animated map, no step counter, no decorative visualization. The user is not looking at the phone during the walk; the phone is in their pocket.
- Audio cue at start ("Walk") and at end ("Stop"). Vibration pattern at end (short, single 100 ms vibration) for users walking with audio off.
- Post test screen shows "Capture complete" and the quality score in plain language ("Good capture" / "Capture too short, please retry"), never a raw 0.78 number.

### 9.3 Low Contrast Vision Test (Phase 6)

- Maximum brightness lock during the test (per Sloan administration convention). Brightness restores to the user's setting at the end.
- Letters are rendered at the four Sloan contrast levels (100, 25, 5, 1.25 percent) on the application's `surface` token (not on a custom white). The contrast token math is computed against `surface`, not against the device default white.
- The user taps the matching letter from a multiple choice grid, not types it. Each grid cell is at least 64 dp by 64 dp.
- Ambient light failure surface ("Room is too dim, please move to brighter light") is informational, not error styled. It uses `onSurfaceVariant` text on `surfaceVariant` background, not `error` styling.

### 9.4 SDMT (Phase 7)

- Symbol to digit mapping key is fixed at the top of the screen, never scrolls out of view, occupies roughly the top third of the screen.
- The current symbol to identify is centered in the middle third.
- The numeric keypad occupies the bottom third, 3 by 3 grid plus zero centered below.
- Key sizes per Section 5.1: 64 dp by 64 dp minimum, 8 dp gaps.
- Wrong answer feedback: the key briefly tints to `errorContainer` for 100 ms, no sound, no vibration. The test continues.

### 9.5 Voice Reading Test (Phase 8)

- Reading passage rendered in `bodyLarge` (16sp) on `surface`, with line height generous enough (24 sp default) to ease tracking under cognitive fatigue.
- Recording indicator: a single static microphone icon with a one word label ("Recording"). No animated waveform, no level meter. A live waveform invites the user to look at it instead of reading.
- Pause and resume control sized at the 64 dp action button standard.

## 10. Open questions for PM and Patient Advocate

Items where the UI/UX Designer made a defensible call but the Patient Advocate or PM may want to override:

1. **96 dp tap test targets.** Larger than any other tap target in the application. Justification is that the tap test is a measurement, not just an interaction. The Patient Advocate should confirm this size feels right under MS related dexterity limits.
2. **Disabling subtle pulse animation during timed test windows.** Trades off "animation feedback feels alive" against "no motion contaminant during measurement and no vestibular trigger." The Patient Advocate should confirm.
3. **No live waveform on the voice test.** Trades off "user wants to know the mic is working" against "user starts watching the waveform instead of reading the passage." The Patient Advocate should confirm.
4. **High contrast theme is opt in, not auto detected.** Trades off "user with significant visual deficit gets the right theme automatically" against "false positive auto detection is intrusive." The Accessibility Specialist should confirm.

## 11. Implementation handoff to Android Engineer

This file is the source of truth for tokens. The Android Engineer translates these into:

- A `BaselineMSColorScheme` defined in `app/src/main/java/.../ui/theme/Color.kt` with the light and dark hex values from Section 2.
- A `BaselineMSTypography` defined in `app/src/main/java/.../ui/theme/Type.kt` with the type scale assignments from Section 3.
- A spacing object (e.g., `BaselineMSSpacing`) exposing the five canonical values from Section 4 as `Dp` constants.
- A `BaselineMSTheme` composable wrapping `MaterialTheme` with the above and reading the user's high contrast preference from settings.

The current `MainActivity.kt` placeholder uses bare `MaterialTheme {}` with no colors or typography set, which is acceptable as a starting point for Phase 0 (it compiles and runs) but must be replaced in Phase 1 by the `BaselineMSTheme` composable. This is a Phase 1 task for the Android Engineer.

## 12. Coordination

- **Patient Advocate (agent 19)** reviews each per test interaction note in Section 9 and the open questions in Section 10.
- **Accessibility Specialist (agent 12)** verifies WCAG technical compliance against Section 2.4 contrast tables and confirms the dynamic type behavior in Section 3.3 against the Phase 11 audit checklist.
- **Android Engineer (agent 03)** consumes Section 11 in Phase 1.
- **Clinical Outcomes Reviewer (agent 15)** weighs in on Section 2.5 specifically, confirming that `error` is reserved for system errors and never used for clinical content.
