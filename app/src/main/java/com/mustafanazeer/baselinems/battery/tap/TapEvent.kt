package com.mustafanazeer.baselinems.battery.tap

enum class TapSide { LEFT, RIGHT }

enum class TapKind { VALID, NON_ALTERNATING }

data class TapEvent(
    val timestampMs: Long,
    val side: TapSide,
    val kind: TapKind
)
