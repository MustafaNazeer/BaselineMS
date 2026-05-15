package com.mustafanazeer.baselinems.dsp.sdmt

import kotlin.math.sqrt

data class SdmtResponse(
    val prompted: SdmtSymbol,
    val tappedDigit: Int,
    val promptShownAtMs: Long,
    val tappedAtMs: Long
) {
    val responseTimeMs: Long get() = tappedAtMs - promptShownAtMs
}

data class SdmtScore(
    val totalCorrect: Int,
    val totalAttempted: Int,
    val totalErrors: Int,
    val errorRate: Double,
    val responseTimeMeanMs: Double,
    val responseTimeSdMs: Double,
    val responseTimes: List<Long>,
    val qualityScore: Double
)

object SdmtScoring {

    const val ENGAGEMENT_FLOOR_CORRECT: Int = 20
    const val ABSOLUTE_FLOOR_CORRECT: Int = 6
    const val MAX_INTER_RESPONSE_GAP_MS: Long = 15_000L
    const val SUSTAINED_ENGAGEMENT_FRACTION: Double = 75.0 / 90.0
    const val QUALITY_ACCEPTABLE_FLOOR: Double = 0.8

    fun compute(
        responses: List<SdmtResponse>,
        key: Map<SdmtSymbol, Int>,
        testDurationMs: Long
    ): SdmtScore {
        val totalAttempted = responses.size
        val correctList = responses.filter { it.tappedDigit == key[it.prompted] }
        val totalCorrect = correctList.size
        val totalErrors = totalAttempted - totalCorrect
        val errorRate = if (totalAttempted == 0) 0.0 else totalErrors.toDouble() / totalAttempted

        val rtList = correctList.map { it.responseTimeMs }
        val rtMean = if (rtList.isEmpty()) 0.0 else rtList.sumOf { it.toDouble() } / rtList.size
        val rtSd = if (rtList.size < 2) {
            0.0
        } else {
            val variance = rtList.sumOf { (it.toDouble() - rtMean).let { d -> d * d } } / rtList.size
            sqrt(variance)
        }

        val quality = computeQualityScore(
            responses = responses,
            totalCorrect = totalCorrect,
            testDurationMs = testDurationMs
        )

        return SdmtScore(
            totalCorrect = totalCorrect,
            totalAttempted = totalAttempted,
            totalErrors = totalErrors,
            errorRate = errorRate,
            responseTimeMeanMs = rtMean,
            responseTimeSdMs = rtSd,
            responseTimes = rtList,
            qualityScore = quality
        )
    }

    private fun computeQualityScore(
        responses: List<SdmtResponse>,
        totalCorrect: Int,
        testDurationMs: Long
    ): Double {
        if (totalCorrect <= ABSOLUTE_FLOOR_CORRECT) return 0.0
        if (responses.isEmpty()) return 0.0

        val countFactor = if (totalCorrect >= ENGAGEMENT_FLOOR_CORRECT) 1.0 else 0.0

        val sortedTaps = responses.map { it.tappedAtMs }.sorted()
        val maxGap = (1 until sortedTaps.size)
            .maxOfOrNull { sortedTaps[it] - sortedTaps[it - 1] }
            ?: 0L
        val firstTapAt = sortedTaps.first()
        val firstGap = firstTapAt - (responses.minOf { it.promptShownAtMs })
        val effectiveMaxGap = maxOf(maxGap, firstGap)
        val gapFactor = if (effectiveMaxGap <= MAX_INTER_RESPONSE_GAP_MS) 1.0 else 0.0

        val lastTapAt = sortedTaps.last()
        val sustainedThresholdMs = (testDurationMs * SUSTAINED_ENGAGEMENT_FRACTION).toLong()
        val spanFactor = if (lastTapAt >= sustainedThresholdMs) 1.0 else 0.0

        return (countFactor + gapFactor + spanFactor) / 3.0
    }
}
