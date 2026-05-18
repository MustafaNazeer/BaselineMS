package com.mustafanazeer.baselinems.signals

import com.mustafanazeer.baselinems.dsp.ImuSample

/**
 * Signals layer contract for persisting captured IMU samples to a backing store.
 *
 * Implementations append rows one sample at a time and release the underlying resource on
 * `close()`. The orchestrator drives the lifecycle: it calls `appendRow` for every sample
 * collected from `ImuSource.stream()` and `close` exactly once when capture ends, whether the
 * capture completed normally or was cancelled mid stream.
 *
 * The interface is the indirection that lets tests substitute an in memory `FakeSensorTraceWriter`
 * for the production `RawSensorWriter` so the orchestrator's cancel and flush paths can be
 * verified without touching the filesystem (per SPE Phase 4 finding I3).
 */
interface SensorTraceWriter {
    fun appendRow(sample: ImuSample)
    fun close()
}
