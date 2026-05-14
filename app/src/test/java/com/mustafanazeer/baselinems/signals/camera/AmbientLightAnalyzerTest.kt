package com.mustafanazeer.baselinems.signals.camera

import androidx.camera.core.ImageProxy
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer

class AmbientLightAnalyzerTest {

    @Test
    fun `uniform mid gray YUV frame yields half of lux range`() {
        val analyzer = AmbientLightAnalyzer(luxPerYUnit = 4.0)
        var reported = -1.0

        val proxy = makeProxy(width = 320, height = 240, yValue = 128)
        analyzer.onLuxEstimated = { reported = it }
        analyzer.analyze(proxy)

        assertEquals(512.0, reported, 1.0)
    }

    @Test
    fun `uniform black YUV frame yields zero lux`() {
        val analyzer = AmbientLightAnalyzer(luxPerYUnit = 4.0)
        var reported = -1.0

        val proxy = makeProxy(width = 320, height = 240, yValue = 0)
        analyzer.onLuxEstimated = { reported = it }
        analyzer.analyze(proxy)

        assertEquals(0.0, reported, 0.5)
    }

    @Test
    fun `uniform white YUV frame yields max lux`() {
        val analyzer = AmbientLightAnalyzer(luxPerYUnit = 4.0)
        var reported = -1.0

        val proxy = makeProxy(width = 320, height = 240, yValue = 255)
        analyzer.onLuxEstimated = { reported = it }
        analyzer.analyze(proxy)

        assertEquals(1020.0, reported, 1.0)
    }

    private fun makeProxy(width: Int, height: Int, yValue: Int): ImageProxy {
        val plane = mockk<ImageProxy.PlaneProxy>()
        val buffer = ByteBuffer.allocate(width * height).apply {
            repeat(width * height) { put(yValue.toByte()) }
            rewind()
        }
        every { plane.buffer } returns buffer
        every { plane.rowStride } returns width
        every { plane.pixelStride } returns 1

        val proxy = mockk<ImageProxy>(relaxed = true)
        every { proxy.planes } returns arrayOf(plane)
        every { proxy.width } returns width
        every { proxy.height } returns height
        return proxy
    }
}
