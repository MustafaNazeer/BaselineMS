package com.mustafanazeer.baselinems.battery.vision

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import com.mustafanazeer.baselinems.battery.TestModule
import com.mustafanazeer.baselinems.battery.TestResultPayload
import com.mustafanazeer.baselinems.data.TestType
import com.mustafanazeer.baselinems.dsp.vision.SloanScore
import com.mustafanazeer.baselinems.signals.camera.AmbientLightAnalyzer
import com.mustafanazeer.baselinems.signals.camera.AndroidCameraSource
import com.mustafanazeer.baselinems.signals.camera.FaceDistanceAnalyzer
import java.util.UUID

class VisionTest(
    private val focalLengthPx: Double = 1500.0
) : TestModule {

    override val testType: TestType = TestType.VISION
    override val displayName: String = "Low contrast vision"
    override val instructions: String = "Read low contrast letters at three levels of contrast."
    override val estimatedDurationSeconds: Int = 240

    @Composable
    override fun Content(onComplete: (TestResultPayload) -> Unit) {
        MaterialTheme(
            colorScheme = HighContrastVisionScheme,
            typography = MaterialTheme.typography
        ) {
            VisionTestBody(focalLengthPx = focalLengthPx, onComplete = onComplete)
        }
    }
}

@Composable
private fun VisionTestBody(focalLengthPx: Double, onComplete: (TestResultPayload) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sessionId = remember { UUID.randomUUID().toString() }
    val viewModel = remember { VisionTestViewModel(sessionId) }
    val state by viewModel.state.collectAsState()
    val cameraSource = remember { AndroidCameraSource(context.applicationContext) }
    val ambient = remember {
        AmbientLightAnalyzer().apply { onLuxEstimated = { viewModel.onLuxEstimated(it) } }
    }
    val face = remember {
        FaceDistanceAnalyzer(focalLengthPx).apply { onDistanceEstimated = { viewModel.onDistanceEstimated(it) } }
    }

    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
        if (!granted) viewModel.onCameraDenied()
    }

    LaunchedEffect(state, cameraPermissionGranted) {
        if (state is VisionTestState.QualityCheck) {
            if (cameraPermissionGranted) {
                cameraSource.start(lifecycleOwner, listOf(ambient, face))
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    LaunchedEffect(state) {
        if (state is VisionTestState.Running ||
            state is VisionTestState.Done ||
            state is VisionTestState.Cancelled ||
            state is VisionTestState.CameraDenied
        ) {
            cameraSource.stop()
        }
    }
    DisposableEffect(Unit) {
        onDispose { cameraSource.stop() }
    }

    when (val s = state) {
        VisionTestState.Instructions -> VisionInstructionsScreen(
            onStart = { viewModel.onStart() },
            onCancel = { onComplete(emptyPayload(qualityScore = 0.0)) }
        )
        is VisionTestState.QualityCheck -> VisionQualityCheckScreen(
            state = s,
            onContinue = { viewModel.onQualityCheckPassed() },
            onCancel = { viewModel.onCancel() }
        )
        is VisionTestState.Running -> VisionRunnerScreen(
            state = s,
            onTap = { viewModel.onLetterTapped(it) },
            onCancel = { viewModel.onCancel() }
        )
        is VisionTestState.Done -> VisionDoneScreen(
            score = s.score,
            onDone = { onComplete(visionPayload(s.score)) }
        )
        VisionTestState.Cancelled -> VisionCancelledScreen(
            onDone = { onComplete(emptyPayload(qualityScore = 0.0)) }
        )
        VisionTestState.CameraDenied -> VisionCameraDeniedScreen(
            onDone = { onComplete(emptyPayload(qualityScore = 0.0)) }
        )
    }
}

private val HighContrastVisionScheme = lightColorScheme(
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

private fun visionPayload(score: SloanScore): TestResultPayload {
    return object : TestResultPayload {
        override val qualityScore: Double = 1.0
        override val features: Map<String, Double> = mapOf(
            "sloan_total" to score.totalCorrect.toDouble(),
            "sloan_100pct" to score.correctAt100Pct.toDouble(),
            "sloan_2pt5pct" to score.correctAt2Pt5Pct.toDouble(),
            "sloan_1pt25pct" to score.correctAt1Pt25Pct.toDouble()
        )
    }
}

private fun emptyPayload(qualityScore: Double): TestResultPayload {
    return object : TestResultPayload {
        override val qualityScore: Double = qualityScore
        override val features: Map<String, Double> = emptyMap()
    }
}
