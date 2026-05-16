package com.mustafanazeer.baselinems.battery.voice

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mustafanazeer.baselinems.dsp.voice.FeatureQualityFlag
import com.mustafanazeer.baselinems.dsp.voice.VoiceFeatureSet
import com.mustafanazeer.baselinems.dsp.voice.VoiceQualityScore
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VoiceTestScreenRenderTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun featuresWithVoicedSeconds(voicedSeconds: Double): VoiceFeatureSet {
        return VoiceFeatureSet(
            jitterLocal = 0.008,
            shimmerLocal = 0.030,
            hnrDb = 20.0,
            f0MeanHz = 130.0,
            f0SdHz = 1.5,
            speakingRateWpm = 120.0,
            pauseFraction = 0.1,
            voicedSeconds = voicedSeconds,
            totalSeconds = 30.0,
            periodCount = 3000
        )
    }

    private fun featuresForBand(
        voicedSeconds: Double,
        jitter: Double?,
        shimmer: Double?
    ): VoiceFeatureSet {
        return VoiceFeatureSet(
            jitterLocal = jitter,
            shimmerLocal = shimmer,
            hnrDb = 20.0,
            f0MeanHz = 130.0,
            f0SdHz = 1.5,
            speakingRateWpm = 120.0,
            pauseFraction = 0.1,
            voicedSeconds = voicedSeconds,
            totalSeconds = 30.0,
            periodCount = 3000
        )
    }

    private fun qualityScoreOk(): VoiceQualityScore {
        return VoiceQualityScore(
            qualityScore = 0.9,
            perFeatureFlags = mapOf("jitter_local" to FeatureQualityFlag.OK),
            engagementOk = true,
            clippingDetected = false,
            snrAdequate = true
        )
    }

    @Test
    fun instructions_renders_start_and_cancel_and_privacy_reassurance() {
        composeRule.setContent {
            VoiceInstructionsScreen(onStart = {}, onCancel = {})
        }
        composeRule.onNodeWithText("Start").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
        composeRule.onNodeWithText(
            "The microphone records only while you read. The recording is processed on your phone and never leaves the device."
        ).assertIsDisplayed()
    }

    @Test
    fun instructions_does_not_render_technical_vocabulary() {
        composeRule.setContent {
            VoiceInstructionsScreen(onStart = {}, onCancel = {})
        }
        composeRule.onNodeWithText("jitter").assertDoesNotExist()
        composeRule.onNodeWithText("shimmer").assertDoesNotExist()
        composeRule.onNodeWithText("HNR").assertDoesNotExist()
    }

    @Test
    fun instructions_start_button_fires_callback() {
        var started = false
        composeRule.setContent {
            VoiceInstructionsScreen(onStart = { started = true }, onCancel = {})
        }
        composeRule.onNodeWithText("Start").performClick()
        assertTrue(started)
    }

    @Test
    fun record_audio_denied_renders_unavailable_copy_and_back_to_home() {
        composeRule.setContent {
            VoiceRecordAudioDeniedScreen(onDone = {})
        }
        composeRule.onNodeWithText("Voice test unavailable on this device").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Microphone access is required for this test. Voice test is off for now."
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Back to home").assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun audio_quality_check_red_disables_start_reading() {
        val state = VoiceTestState.AudioQualityCheck(
            noiseFloorRmsDbFs = -15.0,
            band = NoiseBand.Red
        )
        composeRule.setContent {
            VoiceAudioQualityCheckScreen(state = state, onStartReading = {}, onCancel = {})
        }
        composeRule.onNodeWithText("Too noisy. Move somewhere quieter.").assertIsDisplayed()
        composeRule.onNodeWithText("Start reading").assertIsNotEnabled()
    }

    @Test
    fun audio_quality_check_green_enables_start_reading_and_does_not_show_passage() {
        val state = VoiceTestState.AudioQualityCheck(
            noiseFloorRmsDbFs = -60.0,
            band = NoiseBand.Green
        )
        composeRule.setContent {
            VoiceAudioQualityCheckScreen(state = state, onStartReading = {}, onCancel = {})
        }
        composeRule.onNodeWithText("Quiet enough to record").assertIsDisplayed()
        composeRule.onNodeWithText("Start reading").assertIsEnabled()
        composeRule.onNodeWithText("You wish to know all about my grandfather", substring = true)
            .assertDoesNotExist()
    }

    @Test
    fun audio_quality_check_yellow_enables_start_reading() {
        val state = VoiceTestState.AudioQualityCheck(
            noiseFloorRmsDbFs = -40.0,
            band = NoiseBand.Yellow
        )
        composeRule.setContent {
            VoiceAudioQualityCheckScreen(state = state, onStartReading = {}, onCancel = {})
        }
        composeRule.onNodeWithText(
            "Some background noise. Recording will still work."
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Start reading").assertIsEnabled()
    }

    @Test
    fun runner_renders_passage_and_recording_affordance_and_countdown() {
        val state = VoiceTestState.Running(elapsedMs = 5_000L)
        composeRule.setContent {
            VoiceRunnerScreen(state = state, onCancel = {})
        }
        composeRule.onNodeWithText("You wish to know all about my grandfather", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("Recording").assertIsDisplayed()
        composeRule.onNodeWithText("25 s remaining").assertIsDisplayed()
    }

    @Test
    fun done_renders_warm_headline_and_per_feature_line_and_trend_line() {
        composeRule.setContent {
            VoiceDoneScreen(
                features = featuresWithVoicedSeconds(28.0),
                quality = qualityScoreOk(),
                onDone = {}
            )
        }
        composeRule.onNodeWithText("Voice test recorded").assertIsDisplayed()
        composeRule.onNodeWithText(
            "You read the passage for 28 seconds. Your voice was steady most of the time."
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            "Voice patterns change over time", substring = true
        ).assertIsDisplayed()
    }

    @Test
    fun done_renders_steady_copy_when_jitter_and_shimmer_below_steady_cutoffs() {
        composeRule.setContent {
            VoiceDoneScreen(
                features = featuresForBand(voicedSeconds = 28.0, jitter = 0.005, shimmer = 0.020),
                quality = qualityScoreOk(),
                onDone = {}
            )
        }
        composeRule.onNodeWithText(
            "You read the passage for 28 seconds. Your voice was steady most of the time."
        ).assertIsDisplayed()
        composeRule.onNodeWithText("had some variation", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("varied more than usual", substring = true).assertDoesNotExist()
    }

    @Test
    fun done_renders_mostly_steady_copy_when_jitter_and_shimmer_in_mid_band() {
        composeRule.setContent {
            VoiceDoneScreen(
                features = featuresForBand(voicedSeconds = 26.0, jitter = 0.015, shimmer = 0.055),
                quality = qualityScoreOk(),
                onDone = {}
            )
        }
        composeRule.onNodeWithText(
            "You read the passage for 26 seconds. Your voice had some variation."
        ).assertIsDisplayed()
        composeRule.onNodeWithText("steady most of the time", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("varied more than usual", substring = true).assertDoesNotExist()
    }

    @Test
    fun done_renders_varied_copy_when_jitter_or_shimmer_above_mostly_steady_cutoffs() {
        composeRule.setContent {
            VoiceDoneScreen(
                features = featuresForBand(voicedSeconds = 24.0, jitter = 0.030, shimmer = 0.090),
                quality = qualityScoreOk(),
                onDone = {}
            )
        }
        composeRule.onNodeWithText(
            "You read the passage for 24 seconds. Your voice varied more than usual today."
        ).assertIsDisplayed()
        composeRule.onNodeWithText("steady most of the time", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("had some variation", substring = true).assertDoesNotExist()
    }

    @Test
    fun done_renders_unmeasurable_copy_when_jitter_is_null() {
        composeRule.setContent {
            VoiceDoneScreen(
                features = featuresForBand(voicedSeconds = 22.0, jitter = null, shimmer = 0.040),
                quality = qualityScoreOk(),
                onDone = {}
            )
        }
        composeRule.onNodeWithText("You read the passage for 22 seconds.").assertIsDisplayed()
        composeRule.onNodeWithText("steady most of the time", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("had some variation", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("varied more than usual", substring = true).assertDoesNotExist()
    }

    @Test
    fun done_does_not_render_jitter_shimmer_or_hnr_text() {
        composeRule.setContent {
            VoiceDoneScreen(
                features = featuresWithVoicedSeconds(28.0),
                quality = qualityScoreOk(),
                onDone = {}
            )
        }
        composeRule.onNodeWithText("jitter", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("shimmer", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("HNR", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Romero-Arias", substring = true).assertDoesNotExist()
    }

    @Test
    fun cancelled_renders_framing_without_per_feature_line() {
        composeRule.setContent {
            VoiceCancelledScreen(onDone = {})
        }
        composeRule.onNodeWithText("Test cancelled. Nothing was saved.").assertIsDisplayed()
        composeRule.onNodeWithText("Back to home").assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithText("you answered", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("of 30 seconds", substring = true).assertDoesNotExist()
    }

    @Test
    fun cancelled_back_to_home_fires_callback() {
        var done = false
        composeRule.setContent {
            VoiceCancelledScreen(onDone = { done = true })
        }
        composeRule.onNodeWithText("Back to home").performClick()
        assertTrue(done)
    }
}
