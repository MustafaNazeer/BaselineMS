package com.mustafanazeer.baselinems.battery.sdmt

import com.mustafanazeer.baselinems.dsp.sdmt.SdmtScore
import com.mustafanazeer.baselinems.dsp.sdmt.SdmtSymbol

sealed class SdmtTestState {
    data object Instructions : SdmtTestState()
    data class Running(
        val currentPrompt: SdmtSymbol,
        val promptShownAtMs: Long,
        val elapsedMs: Long,
        val totalAttempted: Int,
        val totalCorrect: Int,
        val consecutiveErrors: Int,
        val showCountdown: Boolean
    ) : SdmtTestState()
    data class Done(val score: SdmtScore) : SdmtTestState()
    data object Cancelled : SdmtTestState()
}
