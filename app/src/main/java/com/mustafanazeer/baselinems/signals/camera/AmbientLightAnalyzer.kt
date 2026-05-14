package com.mustafanazeer.baselinems.signals.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class AmbientLightAnalyzer(
    private val luxPerYUnit: Double = 4.0,
    private val centerCropFraction: Double = 0.5,
    private val pixelStride: Int = 16
) : ImageAnalysis.Analyzer {

    var onLuxEstimated: ((Double) -> Unit)? = null

    override fun analyze(image: ImageProxy) {
        val yPlane = image.planes[0]
        val buffer = yPlane.buffer
        val rowStride = yPlane.rowStride

        val width = image.width
        val height = image.height
        val cropMargin = (1.0 - centerCropFraction) / 2.0
        val xStart = (width * cropMargin).toInt()
        val xEnd = (width * (1.0 - cropMargin)).toInt()
        val yStart = (height * cropMargin).toInt()
        val yEnd = (height * (1.0 - cropMargin)).toInt()

        var sum = 0L
        var count = 0
        var y = yStart
        while (y < yEnd) {
            var x = xStart
            while (x < xEnd) {
                val byteIndex = y * rowStride + x
                sum += (buffer.get(byteIndex).toInt() and 0xFF)
                count++
                x += pixelStride
            }
            y += pixelStride
        }

        if (count == 0) return
        val meanY = sum.toDouble() / count
        onLuxEstimated?.invoke(meanY * luxPerYUnit)
    }
}
