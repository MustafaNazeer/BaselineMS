package com.mustafanazeer.baselinems.battery.sdmt

import com.mustafanazeer.baselinems.dsp.sdmt.SDMT_FIXED_KEY
import com.mustafanazeer.baselinems.dsp.sdmt.SdmtResponse
import com.mustafanazeer.baselinems.dsp.sdmt.SdmtScoring
import com.mustafanazeer.baselinems.dsp.sdmt.SdmtSymbol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class SdmtViewModel(
    sessionId: String,
    private val scope: CoroutineScope,
    private val showCountdown: Boolean = false,
    private val onTenSecondsRemaining: () -> Unit = {},
    private val clockMs: () -> Long = { 0L }
) {

    private val random: Random = Random(sessionId.hashCode().toLong())
    private val key: Map<SdmtSymbol, Int> = SDMT_FIXED_KEY
    private val responses: MutableList<SdmtResponse> = mutableListOf()
    private var startWallClockMs: Long = 0L
    private var virtualElapsedMs: Long = 0L

    private val _state: MutableStateFlow<SdmtTestState> =
        MutableStateFlow(SdmtTestState.Instructions)
    val state: StateFlow<SdmtTestState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var lastSymbol: SdmtSymbol? = null

    fun onStart() {
        if (_state.value !is SdmtTestState.Instructions) return
        val first = drawSymbol()
        lastSymbol = first
        startWallClockMs = clockMs()
        virtualElapsedMs = 0L
        _state.value = SdmtTestState.Running(
            currentPrompt = first,
            promptShownAtMs = 0L,
            elapsedMs = 0L,
            totalAttempted = 0,
            totalCorrect = 0,
            consecutiveErrors = 0,
            showCountdown = showCountdown
        )
        startTimer()
    }

    fun onCancel() {
        timerJob?.cancel()
        timerJob = null
        _state.value = SdmtTestState.Cancelled
    }

    fun onDigitTapped(digit: Int) {
        val cur = _state.value as? SdmtTestState.Running ?: return
        val tappedAtMs = currentVirtualElapsedMs()
        val expected = key.getValue(cur.currentPrompt)
        val correct = digit == expected
        responses += SdmtResponse(
            prompted = cur.currentPrompt,
            tappedDigit = digit,
            promptShownAtMs = cur.promptShownAtMs,
            tappedAtMs = tappedAtMs
        )

        if (tappedAtMs >= TEST_DURATION_MS) {
            finishTest()
            return
        }

        val nextSymbol = drawSymbol()
        lastSymbol = nextSymbol
        val newConsecutiveErrors = if (correct) 0 else cur.consecutiveErrors + 1
        _state.value = cur.copy(
            currentPrompt = nextSymbol,
            promptShownAtMs = tappedAtMs,
            elapsedMs = tappedAtMs,
            totalAttempted = cur.totalAttempted + 1,
            totalCorrect = cur.totalCorrect + if (correct) 1 else 0,
            consecutiveErrors = newConsecutiveErrors
        )
    }

    private fun startTimer() {
        timerJob = scope.launch {
            var pingFired = false
            val tickMs = 100L
            while (isActive) {
                delay(tickMs)
                val elapsed = currentVirtualElapsedMs()
                val cur = _state.value as? SdmtTestState.Running ?: return@launch
                if (!pingFired && elapsed >= PING_AT_MS) {
                    pingFired = true
                    onTenSecondsRemaining()
                }
                if (elapsed >= TEST_DURATION_MS) {
                    finishTest()
                    return@launch
                }
                _state.value = cur.copy(elapsedMs = elapsed)
            }
        }
    }

    private fun currentVirtualElapsedMs(): Long {
        val now = clockMs()
        return if (startWallClockMs == 0L && now == 0L) {
            virtualElapsedMs
        } else {
            now - startWallClockMs
        }
    }

    /**
     * Hook used by tests that drive virtual time directly without a real clock source.
     */
    internal fun advanceVirtualTimeTo(elapsedMs: Long) {
        virtualElapsedMs = elapsedMs
    }

    private fun finishTest() {
        timerJob?.cancel()
        timerJob = null
        val score = SdmtScoring.compute(
            responses = responses,
            key = key,
            testDurationMs = TEST_DURATION_MS
        )
        _state.value = SdmtTestState.Done(score)
    }

    private fun drawSymbol(): SdmtSymbol {
        val pool = SdmtSymbol.entries.filter { it != lastSymbol }
        return pool[random.nextInt(pool.size)]
    }

    companion object {
        const val TEST_DURATION_MS: Long = 90_000L
        const val PING_AT_MS: Long = 80_000L
        const val MID_TEST_REASSURANCE_THRESHOLD: Int = 3
    }
}
