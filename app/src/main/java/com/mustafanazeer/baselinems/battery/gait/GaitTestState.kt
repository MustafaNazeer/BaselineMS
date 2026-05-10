package com.mustafanazeer.baselinems.battery.gait

import com.mustafanazeer.baselinems.dsp.GaitFeatures

/**
 * Gait test state machine. The view model in `GaitTestViewModel` transitions through these
 * states; the `GaitTest` `TestModule` renders one screen per state.
 *
 * Sequence on the happy path: `Instructions` (start, or skip) -> `Countdown(3)` -> `Countdown(2)` ->
 * `Countdown(1)` -> `Capturing(0)` -> ... -> `Capturing(30000)` -> `Done(features)`. From
 * `Capturing` the user may also tap Cancel, transitioning to `Cancelled`.
 */
sealed class GaitTestState {
    data object Instructions : GaitTestState()
    data class Countdown(val secondsRemaining: Int) : GaitTestState()
    data class Capturing(val progressMillis: Int) : GaitTestState()
    data class Done(val features: GaitFeatures) : GaitTestState()
    data object Cancelled : GaitTestState()
}
