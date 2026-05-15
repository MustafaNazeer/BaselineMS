package com.mustafanazeer.baselinems.battery.sdmt

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.mustafanazeer.baselinems.battery.TestModule
import com.mustafanazeer.baselinems.battery.TestResultPayload
import com.mustafanazeer.baselinems.data.TestType
import com.mustafanazeer.baselinems.dsp.sdmt.SdmtScore
import java.util.UUID

class SdmtTest : TestModule {

    override val testType: TestType = TestType.SDMT
    override val displayName: String = "Symbol Digit Modalities Test (SDMT) smartphone variant"
    override val instructions: String =
        "Tap the digit that matches each symbol per the key shown. The test runs for 90 seconds."
    override val estimatedDurationSeconds: Int = 110

    @Composable
    override fun Content(onComplete: (TestResultPayload) -> Unit) {
        MaterialTheme(
            colorScheme = HighContrastSdmtScheme,
            typography = MaterialTheme.typography
        ) {
            SdmtTestBody(onComplete = onComplete)
        }
    }
}

@Composable
private fun SdmtTestBody(onComplete: (TestResultPayload) -> Unit) {
    val context = LocalContext.current
    val sessionId = remember { UUID.randomUUID().toString() }
    val showCountdown = remember { SdmtSettings.showCountdown(context) }
    val toneGenerator = remember {
        runCatching {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, TONE_VOLUME_PERCENT)
        }.getOrNull()
    }
    DisposableEffect(toneGenerator) {
        onDispose { toneGenerator?.release() }
    }
    val scope = rememberCoroutineScope()
    val viewModel = remember {
        SdmtViewModel(
            sessionId = sessionId,
            scope = scope,
            showCountdown = showCountdown,
            onTenSecondsRemaining = {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, TONE_DURATION_MS)
            },
            clockMs = { System.currentTimeMillis() }
        )
    }
    val state by viewModel.state.collectAsState()

    when (val s = state) {
        SdmtTestState.Instructions -> SdmtInstructionsScreen(
            onStart = { viewModel.onStart() },
            onCancel = { onComplete(emptyPayload(qualityScore = 0.0)) }
        )
        is SdmtTestState.Running -> SdmtRunnerScreen(
            state = s,
            onDigitTap = { viewModel.onDigitTapped(it) },
            onCancel = { viewModel.onCancel() }
        )
        is SdmtTestState.Done -> SdmtDoneScreen(
            score = s.score,
            onDone = { onComplete(sdmtPayload(s.score)) }
        )
        SdmtTestState.Cancelled -> SdmtCancelledScreen(
            onDone = { onComplete(emptyPayload(qualityScore = 0.0)) }
        )
    }
}

private const val TONE_VOLUME_PERCENT: Int = 80
private const val TONE_DURATION_MS: Int = 250

private val HighContrastSdmtScheme = lightColorScheme(
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color.White,
    onSurfaceVariant = Color.Black,
    primary = Color.Black,
    onPrimary = Color.White,
    secondary = Color.Black,
    onSecondary = Color.White
)

private fun sdmtPayload(score: SdmtScore): TestResultPayload {
    return object : TestResultPayload {
        override val qualityScore: Double = score.qualityScore
        override val features: Map<String, Double> = mapOf(
            "sdmt_total_correct" to score.totalCorrect.toDouble(),
            "sdmt_total_attempted" to score.totalAttempted.toDouble(),
            "sdmt_total_errors" to score.totalErrors.toDouble(),
            "sdmt_error_rate" to score.errorRate,
            "sdmt_response_time_mean_ms" to score.responseTimeMeanMs,
            "sdmt_response_time_sd_ms" to score.responseTimeSdMs
        )
    }
}

private fun emptyPayload(qualityScore: Double): TestResultPayload {
    return object : TestResultPayload {
        override val qualityScore: Double = qualityScore
        override val features: Map<String, Double> = emptyMap()
    }
}

object SdmtSettings {
    private const val PREFS = "baselinems_prefs"
    const val KEY_SHOW_COUNTDOWN = "sdmt_show_countdown"

    fun showCountdown(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHOW_COUNTDOWN, false)
    }

    fun setShowCountdown(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SHOW_COUNTDOWN, value)
            .apply()
    }
}
