package com.mustafanazeer.baselinems.battery.voice

import com.mustafanazeer.baselinems.data.TestType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceTestRegistrationTest {

    @Test
    fun `testType is VOICE`() {
        val module = VoiceTestModule()
        assertEquals(TestType.VOICE, module.testType)
    }

    @Test
    fun `display name is Voice Test`() {
        val module = VoiceTestModule()
        assertEquals("Voice Test", module.displayName)
    }

    @Test
    fun `estimated duration accommodates 30s capture plus permission and quality chrome`() {
        val module = VoiceTestModule()
        assertTrue(
            "estimatedDurationSeconds=${module.estimatedDurationSeconds} should exceed the 30s capture",
            module.estimatedDurationSeconds >= 30
        )
    }

    @Test
    fun `voice prefs key uses per-file idiom mirroring Phase 7 SDMT`() {
        assertEquals("voice_save_audio", VoiceSettings.KEY_VOICE_SAVE_AUDIO)
    }
}
