package com.mustafanazeer.baselinems.signals.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executors
import kotlin.coroutines.resume

class AndroidCameraSource(private val context: Context) : CameraSource {
    private val analyzerExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    override suspend fun start(lifecycleOwner: LifecycleOwner, analyzers: List<ImageAnalysis.Analyzer>) {
        val provider = awaitCameraProvider()
        cameraProvider = provider

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { ia ->
                ia.setAnalyzer(analyzerExecutor) { imageProxy ->
                    try {
                        analyzers.forEach { it.analyze(imageProxy) }
                    } finally {
                        imageProxy.close()
                    }
                }
            }

        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, imageAnalysis)
    }

    override fun stop() {
        cameraProvider?.unbindAll()
        cameraProvider = null
    }

    private suspend fun awaitCameraProvider(): ProcessCameraProvider {
        return suspendCancellableCoroutine { cont ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({ cont.resume(future.get()) }, analyzerExecutor)
        }
    }
}
