package com.mustafanazeer.baselinems.battery.voice

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class VoiceRetentionLapseSettingsTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `lapsed notice is not pending on fresh preferences`() {
        assertFalse(VoiceSettings.retentionLapsedNoticePending(context))
    }

    @Test
    fun `marking a lapse arms the pending notice`() {
        VoiceSettings.markRetentionLapsed(context)

        assertTrue(VoiceSettings.retentionLapsedNoticePending(context))
    }

    @Test
    fun `marking a lapse reconciles the save audio toggle to off`() {
        VoiceSettings.setSaveAudio(context, true)

        VoiceSettings.markRetentionLapsed(context)

        assertFalse(VoiceSettings.saveAudio(context))
    }

    @Test
    fun `clearing the notice flips pending off without re enabling save audio`() {
        VoiceSettings.markRetentionLapsed(context)

        VoiceSettings.clearRetentionLapsedNotice(context)

        assertFalse(VoiceSettings.retentionLapsedNoticePending(context))
        assertFalse(VoiceSettings.saveAudio(context))
    }
}
