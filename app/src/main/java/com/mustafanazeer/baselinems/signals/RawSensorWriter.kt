package com.mustafanazeer.baselinems.signals

import com.mustafanazeer.baselinems.dsp.ImuSample
import kotlinx.coroutines.flow.Flow
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.Writer
import java.util.zip.GZIPOutputStream

/**
 * Raw sensor trace writer. Persists a `Flow<ImuSample>` as a gzipped CSV at a target file.
 *
 * Format: a 14 column header line, then one row per sample. Columns are
 * `timestampNanos,accelerometerX,accelerometerY,accelerometerZ,gyroscopeX,gyroscopeY,gyroscopeZ,linearAccelerationX,linearAccelerationY,linearAccelerationZ,rotationVectorW,rotationVectorX,rotationVectorY,rotationVectorZ`.
 * Rows where `ImuSample.rotationVector` is null write `NaN` into the four rotation columns; the
 * `signals/AndroidImuSource` implementation always supplies a rotation (platform fused or from
 * scratch Madgwick fallback) so this only matters for non production callers.
 *
 * Stream layering: a `GZIPOutputStream` wraps a `BufferedOutputStream` wraps a
 * `FileOutputStream`. The writer flushes and closes on completion; if the source flow throws,
 * the writer closes the file and rethrows so partial data is still recoverable from the gzip
 * stream's last complete deflate block.
 */
open class RawSensorWriter(private val target: File) {

    private val rowBuffer = StringBuilder(ROW_BUFFER_INITIAL_CAPACITY)

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
        val rotation = sample.rotationVector
        val rotW = rotation?.w ?: Double.NaN
        val rotX = rotation?.x ?: Double.NaN
        val rotY = rotation?.y ?: Double.NaN
        val rotZ = rotation?.z ?: Double.NaN

        rowBuffer.setLength(0)
        rowBuffer
            .append(sample.timestampNanos)
            .append(',').append(sample.accelerometer.x)
            .append(',').append(sample.accelerometer.y)
            .append(',').append(sample.accelerometer.z)
            .append(',').append(sample.gyroscope.x)
            .append(',').append(sample.gyroscope.y)
            .append(',').append(sample.gyroscope.z)
            .append(',').append(sample.linearAcceleration.x)
            .append(',').append(sample.linearAcceleration.y)
            .append(',').append(sample.linearAcceleration.z)
            .append(',').append(rotW)
            .append(',').append(rotX)
            .append(',').append(rotY)
            .append(',').append(rotZ)
            .append('\n')
        writer.append(rowBuffer)
    }

    companion object {
        const val HEADER: String = "timestampNanos," +
            "accelerometerX,accelerometerY,accelerometerZ," +
            "gyroscopeX,gyroscopeY,gyroscopeZ," +
            "linearAccelerationX,linearAccelerationY,linearAccelerationZ," +
            "rotationVectorW,rotationVectorX,rotationVectorY,rotationVectorZ"

        // 14 columns plus separators plus newline. 256 chars covers the widest realistic row
        // (timestamp in 19 chars; each Double in up to about 22 chars including sign and exponent).
        private const val ROW_BUFFER_INITIAL_CAPACITY: Int = 256
    }
}
