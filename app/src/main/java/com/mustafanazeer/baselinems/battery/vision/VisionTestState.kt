package com.mustafanazeer.baselinems.battery.vision

import com.mustafanazeer.baselinems.dsp.vision.SloanScore
import com.mustafanazeer.baselinems.dsp.vision.SloanScoring.ContrastLevel

sealed class VisionTestState {
    data object Instructions : VisionTestState()
    data class QualityCheck(
        val lux: Double?,
        val distanceCm: Double?,
        val luxOk: Boolean,
        val distanceOk: Boolean
    ) : VisionTestState()
    data class Running(
        val contrast: ContrastLevel,
        val lineIndex: Int,
        val letterIndex: Int,
        val currentLetter: Char,
        val multipleChoiceLetters: List<Char>,
        val consecutiveSkipCount: Int = 0
    ) : VisionTestState()
    data class Done(val score: SloanScore) : VisionTestState()
    data object Cancelled : VisionTestState()
}
