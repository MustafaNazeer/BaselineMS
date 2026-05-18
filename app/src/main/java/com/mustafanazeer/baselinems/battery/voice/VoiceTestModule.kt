package com.mustafanazeer.baselinems.battery.voice

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import com.mustafanazeer.baselinems.battery.TestModule
import com.mustafanazeer.baselinems.battery.TestResultPayload
import com.mustafanazeer.baselinems.data.TestType
import com.mustafanazeer.baselinems.dsp.voice.VoiceFeatureSet
import com.mustafanazeer.baselinems.dsp.voice.VoiceQuality
import com.mustafanazeer.baselinems.dsp.voice.VoiceQualityScore
import com.mustafanazeer.baselinems.signals.AndroidAudioCapture
import com.mustafanazeer.baselinems.signals.AudioCapture
import kotlinx.coroutines.launch
import java.io.File
import java.util.Arrays
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

class VoiceTestModule(
    private val audioCaptureFactory: () -> AudioCapture = { AndroidAudioCapture() }
) : TestModule {

    override val testType: TestType = TestType.VOICE
    override val displayName: String = "Voice Test"
    override val instructions: String =
        "Read the passage aloud for 30 seconds."
    override val estimatedDurationSeconds: Int = 60

    @Composable
    override fun Content(onComplete: (TestResultPayload) -> Unit) {
        MaterialTheme(
            colorScheme = HighContrastVoiceScheme,
            typography = MaterialTheme.typography
        ) {
            VoiceTestBody(audioCaptureFactory = audioCaptureFactory, onComplete = onComplete)
        }
    }
}

@Composable
private fun VoiceTestBody(
    audioCaptureFactory: () -> AudioCapture,
    onComplete: (TestResultPayload) -> Unit
) {
    val scope = rememberCoroutineScope()
    val audioCapture = remember { audioCaptureFactory() }
    val viewModel = remember {
        VoiceTestViewModel(audioCapture = audioCapture, scope = scope)
    }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        val current = state
        if (current is VoiceTestState.AudioQualityCheck && current.band == NoiseBand.Pending) {
            scope.launch {
                val dbFs = measureNoiseFloorDbFs(audioCapture)
                if (dbFs != null) viewModel.onNoiseFloorMeasured(dbFs)
            }
        }
    }

    when (val s = state) {
        VoiceTestState.Instructions -> VoiceInstructionsScreen(
            onStart = { viewModel.onStart() },
            onCancel = { onComplete(emptyPayload()) }
        )
        VoiceTestState.RecordAudioRequested -> VoiceRecordAudioRequestedScreen(
            onPermissionResult = { viewModel.onPermissionResult(it) }
        )
        VoiceTestState.RecordAudioDenied -> VoiceRecordAudioDeniedScreen(
            onDone = { onComplete(emptyPayload()) }
        )
        is VoiceTestState.AudioQualityCheck -> VoiceAudioQualityCheckScreen(
            state = s,
            onStartReading = { viewModel.onQualityCheckPassed() },
            onCancel = { viewModel.onCancel() }
        )
        is VoiceTestState.Running -> VoiceRunnerScreen(
            state = s,
            onCancel = { viewModel.onCancel() }
        )
        is VoiceTestState.Done -> VoiceDoneScreen(
            features = s.features,
            quality = s.quality,
            onDone = { onComplete(voicePayload(s.features, s.quality)) }
        )
        VoiceTestState.Cancelled -> VoiceCancelledScreen(
            onDone = { onComplete(emptyPayload()) }
        )
    }
}

private suspend fun measureNoiseFloorDbFs(audioCapture: AudioCapture): Double? {
    var buffer: ShortArray? = null
    return try {
        val captured = audioCapture.record(durationSec = NOISE_FLOOR_DURATION_SEC)
        buffer = captured
        rmsDbFs(captured)
    } catch (e: SecurityException) {
        null
    } catch (e: IllegalStateException) {
        null
    } finally {
        buffer?.let { Arrays.fill(it, 0.toShort()) }
    }
}

private fun rmsDbFs(samples: ShortArray): Double {
    if (samples.isEmpty()) return Double.NEGATIVE_INFINITY
    var sumSq = 0.0
    for (s in samples) {
        val v = s.toDouble()
        sumSq += v * v
    }
    val rms = sqrt(sumSq / samples.size)
    val fullScale = Short.MAX_VALUE.toDouble()
    val ratio = max(rms / fullScale, 1e-9)
    return 20.0 * log10(ratio)
}

private const val NOISE_FLOOR_DURATION_SEC: Int = 1

private val HighContrastVoiceScheme = lightColorScheme(
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

private fun voicePayload(
    features: VoiceFeatureSet,
    quality: VoiceQualityScore
): TestResultPayload {
    val flatFeatures: Map<String, Double> = buildMap {
        features.jitterLocal?.let { put(VoiceQuality.FEATURE_KEY_JITTER, it) }
        features.shimmerLocal?.let { put(VoiceQuality.FEATURE_KEY_SHIMMER, it) }
        features.hnrDb?.let { put(VoiceQuality.FEATURE_KEY_HNR, it) }
        features.f0MeanHz?.let { put(VoiceQuality.FEATURE_KEY_F0_MEAN, it) }
        features.f0SdHz?.let { put(VoiceQuality.FEATURE_KEY_F0_SD, it) }
        features.speakingRateWpm?.let { put(VoiceQuality.FEATURE_KEY_SPEAKING_RATE, it) }
        put(VoiceQuality.FEATURE_KEY_PAUSE_FRACTION, features.pauseFraction)
        put(KEY_VOICED_SECONDS, features.voicedSeconds)
    }
    val nestedJson = buildVoiceFeaturesJson(features = features, quality = quality)
    return object : TestResultPayload {
        override val qualityScore: Double = quality.qualityScore
        override val features: Map<String, Double> = flatFeatures
        override val featuresJsonOverride: String = nestedJson
    }
}

private fun emptyPayload(): TestResultPayload {
    return object : TestResultPayload {
        override val qualityScore: Double = 0.0
        override val features: Map<String, Double> = emptyMap()
    }
}

object VoiceSettings {
    private const val PREFS = "baselinems_prefs"
    const val KEY_VOICE_SAVE_AUDIO = "voice_save_audio"
    const val AUDIO_TRACES_DIR = "audio_traces"

    fun saveAudio(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_VOICE_SAVE_AUDIO, false)
    }

    fun setSaveAudio(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_VOICE_SAVE_AUDIO, value)
            .apply()
    }

    fun deleteAllRetainedAudio(filesDir: File): Int {
        val root = File(filesDir, AUDIO_TRACES_DIR)
        if (!root.exists()) return 0
        var count = 0
        root.walkBottomUp().forEach { entry ->
            if (entry.isFile) {
                if (entry.delete()) count += 1
            }
        }
        root.listFiles()?.forEach { child ->
            if (child.isDirectory) child.delete()
        }
        root.delete()
        return count
    }
}
