package com.mustafanazeer.baselinems.signals

import com.mustafanazeer.baselinems.dsp.ImuSample
import com.mustafanazeer.baselinems.dsp.Quaternion
import com.mustafanazeer.baselinems.dsp.Vector3
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.io.IOException
import java.util.zip.GZIPInputStream

/**
 * Verifies the gzipped CSV trace persistence contract. The on disk format is set by
 * `docs/plans/phase-4-gait-test-module-integration.md` architectural rail 5: a 14 column header
 * line listing column names, then one row per sample. The two test cases mirror Task 5 of the
 * Phase 4 plan: round trip preservation within floating point precision, and exception propagation
 * with the file closed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RawSensorWriterTest {

    private fun sample(
        timestampNanos: Long,
        accelerometer: Vector3,
        gyroscope: Vector3,
        linearAcceleration: Vector3,
        rotationVector: Quaternion
    ): ImuSample = ImuSample(
        timestampNanos = timestampNanos,
        accelerometer = accelerometer,
        gyroscope = gyroscope,
        linearAcceleration = linearAcceleration,
        rotationVector = rotationVector
    )

    private fun readDecompressedLines(file: File): List<String> {
        GZIPInputStream(file.inputStream()).use { gz ->
            return gz.bufferedReader().readLines()
        }
    }

    @Test
    fun `writes header plus one row per sample preserving every field within float precision`() = runTest {
        val target = File.createTempFile("raw_sensor_round_trip", ".csv.gz")
        target.deleteOnExit()

        val samples = listOf(
            sample(
                timestampNanos = 1_000_000L,
                accelerometer = Vector3(0.1, 0.2, 9.81),
                gyroscope = Vector3(0.01, 0.02, 0.03),
                linearAcceleration = Vector3(0.0, 0.0, 0.0),
                rotationVector = Quaternion(1.0, 0.0, 0.0, 0.0)
            ),
            sample(
                timestampNanos = 11_000_000L,
                accelerometer = Vector3(0.15, 0.18, 9.79),
                gyroscope = Vector3(0.02, 0.04, 0.06),
                linearAcceleration = Vector3(0.05, 0.04, -0.02),
                rotationVector = Quaternion(0.7071068, 0.0, 0.7071068, 0.0)
            ),
            sample(
                timestampNanos = 21_000_000L,
                accelerometer = Vector3(-0.25, 0.1, 9.85),
                gyroscope = Vector3(-0.01, 0.0, 0.07),
                linearAcceleration = Vector3(-0.20, 0.08, 0.04),
                rotationVector = Quaternion(0.5, 0.5, 0.5, 0.5)
            )
        )
        val writer = RawSensorWriter(target)
        val flow: Flow<ImuSample> = flow { samples.forEach { emit(it) } }

        val written = writer.write(flow)

        assertEquals(samples.size.toLong(), written)
        val lines = readDecompressedLines(target)
        assertEquals(samples.size + 1, lines.size)

        val expectedHeader = "timestampNanos," +
            "accelerometerX,accelerometerY,accelerometerZ," +
            "gyroscopeX,gyroscopeY,gyroscopeZ," +
            "linearAccelerationX,linearAccelerationY,linearAccelerationZ," +
            "rotationVectorW,rotationVectorX,rotationVectorY,rotationVectorZ"
        assertEquals(expectedHeader, lines[0])

        val tolerance = 1e-6
        samples.forEachIndexed { index, expected ->
            val parts = lines[index + 1].split(",")
            assertEquals("row $index column count", 14, parts.size)
            assertEquals(expected.timestampNanos, parts[0].toLong())
            assertEquals(expected.accelerometer.x, parts[1].toDouble(), tolerance)
            assertEquals(expected.accelerometer.y, parts[2].toDouble(), tolerance)
            assertEquals(expected.accelerometer.z, parts[3].toDouble(), tolerance)
            assertEquals(expected.gyroscope.x, parts[4].toDouble(), tolerance)
            assertEquals(expected.gyroscope.y, parts[5].toDouble(), tolerance)
            assertEquals(expected.gyroscope.z, parts[6].toDouble(), tolerance)
            assertEquals(expected.linearAcceleration.x, parts[7].toDouble(), tolerance)
            assertEquals(expected.linearAcceleration.y, parts[8].toDouble(), tolerance)
            assertEquals(expected.linearAcceleration.z, parts[9].toDouble(), tolerance)
            val rot = expected.rotationVector
            assertNotNull("test sample $index must have a rotationVector", rot)
            rot!!
            assertEquals(rot.w, parts[10].toDouble(), tolerance)
            assertEquals(rot.x, parts[11].toDouble(), tolerance)
            assertEquals(rot.y, parts[12].toDouble(), tolerance)
            assertEquals(rot.z, parts[13].toDouble(), tolerance)
        }
    }

    @Test
    fun `flow exception propagates and the file is closed`() = runTest {
        val target = File.createTempFile("raw_sensor_exception", ".csv.gz")
        target.deleteOnExit()

        val firstTwo = listOf(
            sample(
                timestampNanos = 1_000_000L,
                accelerometer = Vector3(0.0, 0.0, 9.81),
                gyroscope = Vector3.ZERO,
                linearAcceleration = Vector3.ZERO,
                rotationVector = Quaternion.IDENTITY
            ),
            sample(
                timestampNanos = 11_000_000L,
                accelerometer = Vector3(0.01, 0.0, 9.80),
                gyroscope = Vector3(0.001, 0.0, 0.0),
                linearAcceleration = Vector3(0.01, 0.0, -0.01),
                rotationVector = Quaternion.IDENTITY
            )
        )
        val sentinel = IOException("synthetic flow failure after 2 emissions")
        val flow: Flow<ImuSample> = flow {
            firstTwo.forEach { emit(it) }
            throw sentinel
        }

        val writer = RawSensorWriter(target)
        try {
            writer.write(flow)
            fail("expected the writer to rethrow the synthetic IOException")
        } catch (thrown: IOException) {
            assertEquals(sentinel, thrown)
        }

        assertTrue("the file must exist after the writer closes it", target.exists())
        assertTrue(
            "the file must remain re openable from the test process after the writer closed it",
            target.canRead()
        )
        // After close, the writer must release its OS file handle so a fresh InputStream can read
        // and decompress whatever was flushed. The first two rows plus the header should be
        // recoverable; if the writer leaked the FileOutputStream, the GZIP stream would either be
        // truncated mid block or fail to open at all.
        val lines = readDecompressedLines(target)
        assertFalse("the file is not empty after exception propagation", lines.isEmpty())
        assertTrue(
            "the header must have been written before the exception (lines=${lines.size})",
            lines.first().startsWith("timestampNanos,")
        )
    }
}
