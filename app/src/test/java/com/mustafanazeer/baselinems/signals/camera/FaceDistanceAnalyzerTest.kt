package com.mustafanazeer.baselinems.signals.camera

import android.graphics.Rect
import com.google.mlkit.vision.face.Face
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FaceDistanceAnalyzerTest {

    @Test
    fun `no faces yields null distance estimate`() {
        val analyzer = FaceDistanceAnalyzer(focalLengthPx = 1500.0)
        var reported: Double? = -1.0
        analyzer.onDistanceEstimated = { reported = it }

        analyzer.processFaces(emptyList())

        assertNull(reported)
    }

    @Test
    fun `face width 562 px at 1500 px focal length yields 40 cm`() {
        val analyzer = FaceDistanceAnalyzer(focalLengthPx = 1500.0)
        var reported: Double? = -1.0
        analyzer.onDistanceEstimated = { reported = it }

        val face = makeFaceWithWidth(562)
        analyzer.processFaces(listOf(face))

        assertEquals(40.0, reported!!, 0.5)
    }

    @Test
    fun `multiple faces uses largest bounding box`() {
        val analyzer = FaceDistanceAnalyzer(focalLengthPx = 1500.0)
        var reported: Double? = -1.0
        analyzer.onDistanceEstimated = { reported = it }

        val far = makeFaceWithWidth(200)
        val near = makeFaceWithWidth(750)
        analyzer.processFaces(listOf(far, near))

        assertEquals(30.0, reported!!, 0.5)
    }

    @Test
    fun `analyze with null mediaImage returns without scheduling async work`() {
        val analyzer = FaceDistanceAnalyzer(focalLengthPx = 1500.0)
        var reported: Double? = -1.0
        analyzer.onDistanceEstimated = { reported = it }

        val proxy = mockk<androidx.camera.core.ImageProxy>(relaxed = true)
        every { proxy.image } returns null

        analyzer.analyze(proxy)

        assertEquals(-1.0, reported)
    }

    private fun makeFaceWithWidth(width: Int): Face {
        val rect = mockk<Rect>()
        every { rect.width() } returns width
        val face = mockk<Face>()
        every { face.boundingBox } returns rect
        return face
    }
}
