package com.mustafan4x.baselinems.signals

import com.mustafan4x.baselinems.dsp.ImuSample
import kotlinx.coroutines.flow.Flow
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.Writer
import java.util.zip.GZIPOutputStream

/**
 * Phase 4 raw sensor trace writer. Persists a `Flow<ImuSample>` as a gzipped CSV at a target file.
 *
 * Format: a 14 column header line, then one row per sample. Columns are
 * `timestampNanos,accelerometerX,accelerometerY,accelerometerZ,gyroscopeX,gyroscopeY,gyroscopeZ,linearAccelerationX,linearAccelerationY,linearAccelerationZ,rotationVectorW,rotationVectorX,rotationVectorY,rotationVectorZ`.
 * Rows where `ImuSample.rotationVector` is null write `NaN` into the four rotation columns; the
 * `signals/AndroidImuSource` Phase 4 implementation always supplies a rotation (platform fused or
 * from scratch Madgwick fallback) so this only matters for non production callers.
 *
 * Stream layering follows `docs/plans/phase-4-gait-test-module-integration.md` Task 5: a
 * `GZIPOutputStream` wraps a `BufferedOutputStream` wraps a `FileOutputStream`. The writer flushes
 * and closes on completion; if the source flow throws, the writer closes the file and rethrows so
 * partial data is still recoverable from the gzip stream's last complete deflate block.
 */
open class RawSensorWriter(private val target: File) {

    open suspend fun write(samples: Flow<ImuSample>): Long {
        target.parentFile?.mkdirs()
        var count = 0L
        val fileOut = FileOutputStream(target)
        val gz: GZIPOutputStream
        try {
            val buffered = BufferedOutputStream(fileOut)
            gz = GZIPOutputStream(buffered)
        } catch (initFailure: Throwable) {
            fileOut.close()
            throw initFailure
        }
        val writer: Writer = OutputStreamWriter(gz, Charsets.UTF_8)
        try {
            writer.append(HEADER).append('\n')
            samples.collect { sample ->
                appendRow(writer, sample)
                count++
            }
            writer.flush()
        } catch (flowFailure: Throwable) {
            try {
                writer.flush()
            } catch (suppressed: Throwable) {
                flowFailure.addSuppressed(suppressed)
            }
            try {
                writer.close()
            } catch (suppressed: Throwable) {
                flowFailure.addSuppressed(suppressed)
            }
            throw flowFailure
        }
        writer.close()
        return count
    }

    private fun appendRow(writer: Writer, sample: ImuSample) {
        val rotW: Double
        val rotX: Double
        val rotY: Double
        val rotZ: Double
        val rotation = sample.rotationVector
        if (rotation == null) {
            rotW = Double.NaN
            rotX = Double.NaN
            rotY = Double.NaN
            rotZ = Double.NaN
        } else {
            rotW = rotation.w
            rotX = rotation.x
            rotY = rotation.y
            rotZ = rotation.z
        }
        writer.append(sample.timestampNanos.toString())
            .append(',').append(sample.accelerometer.x.toString())
            .append(',').append(sample.accelerometer.y.toString())
            .append(',').append(sample.accelerometer.z.toString())
            .append(',').append(sample.gyroscope.x.toString())
            .append(',').append(sample.gyroscope.y.toString())
            .append(',').append(sample.gyroscope.z.toString())
            .append(',').append(sample.linearAcceleration.x.toString())
            .append(',').append(sample.linearAcceleration.y.toString())
            .append(',').append(sample.linearAcceleration.z.toString())
            .append(',').append(rotW.toString())
            .append(',').append(rotX.toString())
            .append(',').append(rotY.toString())
            .append(',').append(rotZ.toString())
            .append('\n')
    }

    companion object {
        const val HEADER: String = "timestampNanos," +
            "accelerometerX,accelerometerY,accelerometerZ," +
            "gyroscopeX,gyroscopeY,gyroscopeZ," +
            "linearAccelerationX,linearAccelerationY,linearAccelerationZ," +
            "rotationVectorW,rotationVectorX,rotationVectorY,rotationVectorZ"
    }
}
