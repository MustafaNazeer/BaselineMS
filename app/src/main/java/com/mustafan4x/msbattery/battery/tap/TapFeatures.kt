package com.mustafan4x.msbattery.battery.tap

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
