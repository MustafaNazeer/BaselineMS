package com.mustafanazeer.baselinems.battery.vision

import androidx.lifecycle.ViewModel
import com.mustafanazeer.baselinems.dsp.vision.LetterResponse
import com.mustafanazeer.baselinems.dsp.vision.SLOAN_LETTERS
import com.mustafanazeer.baselinems.dsp.vision.SloanScoring
import com.mustafanazeer.baselinems.dsp.vision.SloanScoring.ContrastLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

class VisionTestViewModel(sessionId: String) : ViewModel() {

    private val random: Random = Random(sessionId.hashCode().toLong())
    private val chart: Map<ContrastLevel, List<List<Char>>> = generateChart(random)
    private val responses: MutableList<LetterResponse> = mutableListOf()

    private val _state: MutableStateFlow<VisionTestState> = MutableStateFlow(VisionTestState.Instructions)
    val state: StateFlow<VisionTestState> = _state

    fun onStart() {
        _state.value = VisionTestState.QualityCheck(lux = null, distanceCm = null, luxOk = false, distanceOk = false)
    }

    fun onLuxEstimated(lux: Double?) {
        val cur = _state.value as? VisionTestState.QualityCheck ?: return
        _state.value = cur.copy(lux = lux, luxOk = lux != null && lux in LUX_ACCEPT_MIN..LUX_ACCEPT_MAX)
    }

    fun onDistanceEstimated(distanceCm: Double?) {
        val cur = _state.value as? VisionTestState.QualityCheck ?: return
        _state.value = cur.copy(distanceCm = distanceCm, distanceOk = distanceCm != null && distanceCm in DIST_ACCEPT_MIN..DIST_ACCEPT_MAX)
    }

    fun onQualityCheckPassed() {
        val cur = _state.value as? VisionTestState.QualityCheck ?: return
        if (!cur.luxOk || !cur.distanceOk) return
        _state.value = newRunningStateAt(ContrastLevel.C100, lineIndex = 0, letterIndex = 0, consecutiveSkipCount = 0)
    }

    fun onLetterTapped(tapped: Char?) {
        val cur = _state.value as? VisionTestState.Running ?: return
        responses += LetterResponse(shown = cur.currentLetter, tapped = tapped, contrast = cur.contrast)
        val newSkipCount = if (tapped == null) cur.consecutiveSkipCount + 1 else 0
        advance(cur, newSkipCount)
    }

    fun onCancel() {
        _state.value = VisionTestState.Cancelled
    }

    private fun advance(cur: VisionTestState.Running, newSkipCount: Int) {
        val nextLetterIndex = cur.letterIndex + 1
        if (nextLetterIndex < LETTERS_PER_LINE) {
            _state.value = newRunningStateAt(cur.contrast, cur.lineIndex, nextLetterIndex, newSkipCount)
            return
        }
        val nextLineIndex = cur.lineIndex + 1
        if (nextLineIndex < LINES_PER_CHART) {
            _state.value = newRunningStateAt(cur.contrast, nextLineIndex, 0, newSkipCount)
            return
        }
        val nextContrast = nextContrastOrNull(cur.contrast)
        if (nextContrast != null) {
            _state.value = newRunningStateAt(nextContrast, 0, 0, consecutiveSkipCount = 0)
            return
        }
        _state.value = VisionTestState.Done(SloanScoring.compute(responses))
    }

    private fun newRunningStateAt(contrast: ContrastLevel, lineIndex: Int, letterIndex: Int, consecutiveSkipCount: Int): VisionTestState.Running {
        val letter = chart.getValue(contrast)[lineIndex][letterIndex]
        val distractors = SLOAN_LETTERS.filter { it != letter }.shuffled(random).take(3)
        val choices = (distractors + letter).shuffled(random)
        return VisionTestState.Running(contrast, lineIndex, letterIndex, letter, choices, consecutiveSkipCount)
    }

    private fun nextContrastOrNull(c: ContrastLevel): ContrastLevel? = when (c) {
        ContrastLevel.C100 -> ContrastLevel.C2Pt5
        ContrastLevel.C2Pt5 -> ContrastLevel.C1Pt25
        ContrastLevel.C1Pt25 -> null
    }

    private fun generateChart(rng: Random): Map<ContrastLevel, List<List<Char>>> {
        return ContrastLevel.values().associateWith { generateOneChart(rng) }
    }

    private fun generateOneChart(rng: Random): List<List<Char>> {
        val lines = mutableListOf<List<Char>>()
        var prevStart: Char? = null
        repeat(LINES_PER_CHART) {
            val available = SLOAN_LETTERS.toMutableList()
            val line = mutableListOf<Char>()
            repeat(LETTERS_PER_LINE) { idx ->
                val choices = if (idx == 0 && prevStart != null) {
                    available.filter { it != prevStart }
                } else {
                    available.toList()
                }
                val pick = choices.random(rng)
                line += pick
                available.remove(pick)
                if (idx == 0) prevStart = pick
            }
            lines += line
        }
        return lines
    }

    companion object {
        const val LINES_PER_CHART = 8
        const val LETTERS_PER_LINE = 5
        const val LUX_ACCEPT_MIN = 30.0
        const val LUX_ACCEPT_MAX = 1000.0
        const val DIST_ACCEPT_MIN = 30.0
        const val DIST_ACCEPT_MAX = 50.0
    }
}
