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
