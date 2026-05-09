package com.mustafanazeer.baselinems.battery.tap

enum class HandRole { DOMINANT, NON_DOMINANT }

data class TapRound(
    val role: HandRole,
    val durationMs: Long,
    val events: List<TapEvent>,
    val offTargetTaps: Int = 0
)
