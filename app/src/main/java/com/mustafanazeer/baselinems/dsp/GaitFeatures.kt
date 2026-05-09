package com.mustafanazeer.baselinems.dsp

/**
 * The five gait features named in SPEC.md Section 7.1 step 8 plus the quality score from step 9
 * plus the raw step count for downstream sanity checks. The toMap projection is the format the
 * Phase 4 persistence layer will hand to the Room TestResultEntity featuresJson column.
 */
data class GaitFeatures(
    val cadenceStepsPerMinute: Double,
    val meanStrideLengthMeters: Double,
    val stepTimeCv: Double,
    val strideAsymmetryIndex: Double,
    val doubleSupportTimeSeconds: Double,
    val qualityScore: Double,
    val detectedStepCount: Int
) {
    fun toMap(): Map<String, Double> = mapOf(
        "cadence_steps_per_minute" to cadenceStepsPerMinute,
        "mean_stride_length_meters" to meanStrideLengthMeters,
        "step_time_cv" to stepTimeCv,
        "stride_asymmetry_index" to strideAsymmetryIndex,
        "double_support_time_seconds" to doubleSupportTimeSeconds,
        "quality_score" to qualityScore,
        "detected_step_count" to detectedStepCount.toDouble()
    )
}
