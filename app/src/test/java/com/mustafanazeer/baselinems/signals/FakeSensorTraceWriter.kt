package com.mustafanazeer.baselinems.signals

import com.mustafanazeer.baselinems.dsp.ImuSample

/**
 * In memory `SensorTraceWriter` test double. Records the sequence of `appendRow` invocations and
 * whether `close` was called, in the order they happened, so tests can assert that the orchestrator
 * drained the producer buffer to the writer before closing it (per SPE Phase 4 finding I3 and the
 * cancel path fix for finding I1).
 */
class FakeSensorTraceWriter : SensorTraceWriter {

    private val rows: MutableList<ImuSample> = mutableListOf()

    var appendCount: Int = 0
        private set
    var closeCount: Int = 0
        private set

    /** Records the snapshot of `appendCount` at the moment `close` was first called. */
    var appendCountAtClose: Int = -1
        private set

    override fun appendRow(sample: ImuSample) {
        rows += sample
        appendCount += 1
    }

    override fun close() {
        if (closeCount == 0) appendCountAtClose = appendCount
        closeCount += 1
    }

    fun appendedSamples(): List<ImuSample> = rows.toList()
}
