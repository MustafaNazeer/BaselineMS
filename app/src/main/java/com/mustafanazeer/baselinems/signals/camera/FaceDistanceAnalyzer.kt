package com.mustafanazeer.baselinems.signals.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.TimeUnit

class FaceDistanceAnalyzer(
    private val focalLengthPx: Double,
    private val typicalFaceWidthCm: Double = 15.0,
    private val mlKitTimeoutMs: Long = 1000L
) : ImageAnalysis.Analyzer {

    var onDistanceEstimated: ((Double?) -> Unit)? = null

    private val detector: FaceDetector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .setMinFaceSize(0.15f)
                .build()
        )
    }

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(image: ImageProxy) {
        val mediaImage = image.image ?: return
        val input = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
        val faces: List<Face> = try {
            Tasks.await(detector.process(input), mlKitTimeoutMs, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            emptyList()
        }
        processFaces(faces, image.width)
    }

    fun processFaces(faces: List<Face>, frameWidth: Int) {
        val biggest = faces.maxByOrNull { it.boundingBox.width() }
        if (biggest == null) {
            onDistanceEstimated?.invoke(null)
            return
        }
        val faceWidthPx = biggest.boundingBox.width().toDouble()
        if (faceWidthPx <= 0.0) {
            onDistanceEstimated?.invoke(null)
            return
        }
        val distanceCm = (typicalFaceWidthCm * focalLengthPx) / faceWidthPx
        onDistanceEstimated?.invoke(distanceCm)
    }
}
