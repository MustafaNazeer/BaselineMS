package com.mustafanazeer.baselinems.signals.camera

import androidx.camera.core.ImageAnalysis
import androidx.lifecycle.LifecycleOwner

interface CameraSource {
    suspend fun start(lifecycleOwner: LifecycleOwner, analyzers: List<ImageAnalysis.Analyzer>)
    fun stop()
}
