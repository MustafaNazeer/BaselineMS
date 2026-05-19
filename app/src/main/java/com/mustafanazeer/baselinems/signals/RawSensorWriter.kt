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
 * Raw sensor trace writer. Persists captured `ImuSample` values as a gzipped CSV at a target file.
 *
 * Format: a 14 column header line, then one row per sample. Columns are
 * `timestampNanos,accelerometerX,accelerometerY,accelerometerZ,gyroscopeX,gyroscopeY,gyroscopeZ,linearAccelerationX,linearAccelerationY,linearAccelerationZ,rotationVectorW,rotationVectorX,rotationVectorY,rotationVectorZ`.
 * Rows where `ImuSample.rotationVector` is null write `NaN` into the four rotation columns; the
 * `signals/AndroidImuSource` implementation always supplies a rotation (platform fused or from
 * scratch Madgwick fallback) so this only matters for non production callers.
 *
 * Stream layering: a `GZIPOutputStream` wraps a `BufferedOutputStream` wraps a
 * `FileOutputStream`. The file is opened lazily on the first `appendRow` call so a
 * `RawSensorWriter` constructed at composition time does not touch the filesystem until capture
 * actually begins. `close()` flushes and releases the underlying file handle; subsequent calls
 * are no ops.
 */
class RawSensorWriter(private val target: File) : SensorTraceWriter {

    private val rowBuffer = StringBuilder(ROW_BUFFER_INITIAL_CAPACITY)

    private var writer: Writer? = null
    private var closed: Boolean = false

    override fun appendRow(sample: ImuSample) {
        if (closed) return
        val out = ensureOpen()
        writeRow(out, sample)
    }

    override fun close() {
        if (closed) return
        closed = true
        val out = writer ?: return
        try {
            out.flush()
        } finally {
            out.close()
            writer = null
        }
    }

    /**
     * Backwards compatible convenience: collects `samples` into the writer and closes on
     * completion. Existing callers and tests that want the previous "give the writer a flow,
     * get a count back" semantics use this. On flow failure the writer closes before rethrowing
     * so partial data is recoverable from the gzip stream's last complete deflate block.
     */
    suspend fun write(samples: Flow<ImuSample>): Long {
        var count = 0L
        try {
            samples.collect { sample ->
                appendRow(sample)
                count++
            }
        } catch (flowFailure: Throwable) {
            try {
                close()
            } catch (suppressed: Throwable) {
                flowFailure.addSuppressed(suppressed)
            }
            throw flowFailure
        }
        close()
        return count
    }

    private fun ensureOpen(): Writer {
        val existing = writer
        if (existing != null) return existing
        target.parentFile?.mkdirs()
        val fileOut = FileOutputStream(target)
        val gz: GZIPOutputStream
        try {
            val buffered = BufferedOutputStream(fileOut)
            gz = GZIPOutputStream(buffered)
        } catch (initFailure: Throwable) {
            fileOut.close()
            throw initFailure
        }
        val newWriter: Writer = OutputStreamWriter(gz, Charsets.UTF_8)
        newWriter.append(HEADER).append('\n')
        writer = newWriter
        return newWriter
    }

    private fun writeRow(out: Writer, sample: ImuSample) {
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
        out.append(rowBuffer)
    }

    companion object {
        const val HEADER: String = "timestampNanos," +
            "accelerometerX,accelerometerY,accelerometerZ," +
            "gyroscopeX,gyroscopeY,gyroscopeZ," +
            "linearAccelerationX,linearAccelerationY,linearAccelerationZ," +
            "rotationVectorW,rotationVectorX,rotationVectorY,rotationVectorZ"

        private const val ROW_BUFFER_INITIAL_CAPACITY: Int = 256
    }
}
