# Code Review: Phase 2 (Bilateral Tap Test)

Reviewer: Code Reviewer agent (session 2026-05-07)
Commits reviewed: `08e38b3..HEAD` (14 commits, cumulative diff over `app/`)
Verdict: **APPROVED WITH MINOR CHANGES**

---

## Summary

Phase 2 is well constructed. The Kotlin and Compose idioms are mostly clean, the math is correct,
migration integrity is solid, and the inherited CLAUDE.md rules are fully respected across all
commits. One medium severity concern about untested pointer event semantics requires either a
targeted Compose UI test or an acknowledged risk entry before Phase 3 ships the feature to users.
No blockers.

---

## Findings

### MEDIUM: Off-target capture semantics are asserted in the plan but not tested

**File:** `app/src/main/java/com/mustafan4x/msbattery/battery/tap/BilateralTapTest.kt`
**Lines:** `TwoTargets` composable, parent `Row` with `Modifier.pointerInput(Unit) { detectTapGestures(...) }` and child `Target` `Box` with `Modifier.clickable`.

The plan (`docs/plans/phase-2-tap-test.md`, line 2025) claims that when a user taps inside a
`Target` box, the child's `clickable` consumes the event and the parent's `detectTapGestures` does
not fire. In practice, `detectTapGestures` runs in `PointerEventPass.Main` and does not check the
`isConsumed` flag on incoming events. It is therefore possible (depending on Compose runtime version
and traversal order) that both `onInTargetTap` and `onOffTargetTap` fire for the same tap, inflating
the off-target counter and the miss rate.

There is no Compose UI test that exercises this path. The only UI test (`BilateralTapTestRenderTest`)
checks pre-instruction rendering only.

**Required action:** Add a Compose UI test that performs a simulated tap on a `Target` node and
asserts that `onOffTargetTap` is not called (or that the off-target counter stays at zero after an
in-target tap). If the test reveals that both fire, use `Modifier.pointerInput(Unit)` with a raw
`awaitPointerEventScope` loop on the parent that checks `event.changes.any { it.isConsumed }`
before counting the tap as off-target.

---

### LOW: `canSave` gate does not allow the Save button when fields are empty

**File:** `app/src/main/java/com/mustafan4x/msbattery/ui/onboarding/ProfileSetupScreen.kt`

`isPlausibleYear("")` returns `false`, so `canSave` is `false` on first render. This is intentional
and correct behavior: the user must fill in both fields to enable Save. The Skip path correctly
bypasses the gate. No change needed. Noted here for documentation completeness.

---

### PASS: Inherited rules (no dashes, no emojis, no trailers)

`git log --format='%B' 08e38b3..HEAD | grep -i "co-authored"` returns nothing.
`git log --format='%B' 08e38b3..HEAD | grep -i "generated with\|claude code"` returns nothing.
Spot check of commit messages and doc diffs shows no em dashes, en dashes, or hyphens used as
prose punctuation. No emojis found.

---

### PASS: Kotlin and Compose idiom

- All new state in `BilateralTapTestContent` is correctly declared with `by remember { mutableStateOf(...) }`.
- `LaunchedEffect(phase)` is keyed on `phase`, which is the single value driving all coroutine
  lifecycle transitions. Each phase change cancels the previous coroutine and starts a new one.
  Keying is correct.
- `elapsedSec` is reset to `0` inside the `Countdown` branch before transitioning to `Active`,
  so the `while (elapsedSec < totalSec)` loop in the `Active` branch always starts from zero.
- `TapPhase` is a proper sealed class with `data class` and `data object` variants.
- `TapEvent`, `TapRound`, `RoundFeatures`, `SessionFeatures` are all idiomatic data classes with
  `val` fields.
- `EnumMenuBox` is generic over `T` (not restricted to `Enum<T>`), which is more flexible and still
  correct.
- No unnecessary comments in any new file.

---

### PASS: TapFeatures math

- **Miss rate:** `totalMisses = d.nonAlternatingTaps + n.nonAlternatingTaps + d.offTargetTaps + n.offTargetTaps`.
  Combines non-alternating and off-target taps from both rounds. Correct.
- **Asymmetry index:** `(d.tapRateHz - n.tapRateHz) / mean` where `mean = (d.tapRateHz + n.tapRateHz) / 2.0`.
  This is the signed symmetric mean denominator. Correct.
- **Test count:** 12 `@Test` annotations confirmed in `TapFeaturesTest.kt`.

---

### PASS: Migration test integrity

- `Migration1To2Test` asserts all 7 post-migration columns (indices 0 through 6) in the first test.
- Empty table case is present as a separate `@Test`.
- `MigrationTestHelper` constructor uses `AppDatabase::class.java` (correct for Room 2.6.1).
- All assertions use `assertEquals` / `assertTrue`, not `check`.
- Null height insert and read-back verified via `c.isNull(0)`.

---

### PASS: Dependencies

New entries in `app/build.gradle.kts` are exactly:
- `testImplementation(composeBom)`
- `testImplementation("androidx.compose.ui:ui-test-junit4")`
- `debugImplementation("androidx.compose.ui:ui-test-manifest")`

No other new runtime or test dependencies. No Security Engineer review required.

---

## Notes for Phase 3

1. The off-target Compose UI test identified above should be added before Phase 3 if Phase 3 touches
   the tap test UI or if the tap session runner goes to real users.
2. `RootScreen` now wires `BilateralTapTest` as the sole module. When Phase 3 adds a second test
   module, the `modules = listOf(...)` line in `RootScreen.kt` will need updating. Phase 3 should
   verify `BatteryOrchestrator` handles multi-module lists correctly in its existing tests.
3. `ProfileSetupScreen` uses `dobYearText.toInt()` and `heightCmText.toDouble()` (not `toIntOrNull`
   / `toDoubleOrNull`) inside the Save coroutine, protected by `canSave`. If the guard logic is ever
   changed, these will throw. Consider defensively changing them to `toIntOrNull() ?: return@launch`
   in a follow-up.
