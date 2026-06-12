package com.mustafanazeer.baselinems.battery.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceRetentionDecisionTest {

    @Test
    fun `toggle off discards regardless of device security`() {
        assertEquals(
            VoiceRetentionDecision.DISCARD,
            resolveVoiceRetentionDecision(saveAudioEnabled = false, deviceSecure = true)
        )
        assertEquals(
            VoiceRetentionDecision.DISCARD,
            resolveVoiceRetentionDecision(saveAudioEnabled = false, deviceSecure = false)
        )
    }

    @Test
    fun `toggle on and device secure persists`() {
        assertEquals(
            VoiceRetentionDecision.PERSIST,
            resolveVoiceRetentionDecision(saveAudioEnabled = true, deviceSecure = true)
        )
    }

    @Test
    fun `toggle on but device no longer secure discards and flags the lapse`() {
        assertEquals(
            VoiceRetentionDecision.DISCARD_LAPSED,
            resolveVoiceRetentionDecision(saveAudioEnabled = true, deviceSecure = false)
        )
    }
}
