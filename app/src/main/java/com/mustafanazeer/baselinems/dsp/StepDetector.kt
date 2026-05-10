package com.mustafanazeer.baselinems.dsp

/**
 * One detected step. The amplitude is the peak value of the world frame vertical (Z) component
 * of linear acceleration after the Butterworth low pass; the timestamp is the seconds since the
 * start of the trial of the sample at the peak.
 */
data class StepEvent(
    val timeSeconds: Double,
    val amplitude: Double,
    val sampleIndex: Int,
    val gapToPreviousExceededMaxInterval: Boolean
)

/**
 * Peak finder over a low pass filtered vertical acceleration trace. Three constraints fix the
 * design:
 *
 *  - Minimum peak prominence: peaks under this amplitude (in m/s^2) are rejected as wobbles.
 *    The default of 1.0 m/s^2 is a heuristic chosen to comfortably reject the lateral and
 *    forward sway baselines on the synthetic generator (1.0 and 0.5 m/s^2 respectively) while
 *    admitting heel strike spikes that scale with body mass.
 *  - Minimum inter peak distance: peaks within this interval are non maximum suppressed. The
 *    default of 250 ms corresponds to a maximum cadence of 240 steps per minute, well above
 *    the brisk walk fixture's 130 steps per minute.
 *  - Maximum inter peak distance: peaks separated by more than this are emitted but the second
 *    peak is flagged with `gapToPreviousExceededMaxInterval = true` so quality scoring can
 *    downgrade trials that contain pauses or stops.
 *
 * The detect method allocates a single output list. The internal state is method local; the
 * detector itself holds no per sample state and is safe to reuse.
 */
class StepDetector(
    private val minPeakProminence: Double = 1.0,
    private val minIntervalSeconds: Double = 0.25,
    private val maxIntervalSeconds: Double = 0.80
) {
    fun detect(verticalLinearAcceleration: DoubleArray, timestampSeconds: DoubleArray): List<StepEvent> {
        require(verticalLinearAcceleration.size == timestampSeconds.size) {
            "verticalLinearAcceleration and timestampSeconds must have the same length"
        }
        val n = verticalLinearAcceleration.size
        if (n < 3) return emptyList()

        val candidates = ArrayList<StepEvent>()
        for (i in 1 until n - 1) {
            val v = verticalLinearAcceleration[i]
            if (v >= minPeakProminence &&
                v > verticalLinearAcceleration[i - 1] &&
                v >= verticalLinearAcceleration[i + 1]
            ) {
                candidates.add(StepEvent(
                    timeSeconds = timestampSeconds[i],
                    amplitude = v,
                    sampleIndex = i,
                    gapToPreviousExceededMaxInterval = false
                ))
            }
        }
        if (candidates.isEmpty()) return emptyList()

        val accepted = ArrayList<StepEvent>()
        var lastAccepted: StepEvent? = null
        for (c in candidates) {
            val prev = lastAccepted
            if (prev == null) {
                accepted.add(c)
                lastAccepted = c
                continue
            }
            val gap = c.timeSeconds - prev.timeSeconds
            if (gap < minIntervalSeconds) {
                if (c.amplitude > prev.amplitude) {
                    accepted[accepted.size - 1] = c.copy(
                        gapToPreviousExceededMaxInterval = prev.gapToPreviousExceededMaxInterval
                    )
                    lastAccepted = accepted.last()
                }
                continue
            }
            val flagged = gap > maxIntervalSeconds
            val emitted = c.copy(gapToPreviousExceededMaxInterval = flagged)
            accepted.add(emitted)
            lastAccepted = emitted
        }
        return accepted
    }
}
