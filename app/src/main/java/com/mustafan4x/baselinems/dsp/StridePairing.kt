package com.mustafan4x.baselinems.dsp

enum class Foot { LEFT, RIGHT }

data class FootStep(val step: StepEvent, val foot: Foot)

data class Stride(
    val foot: Foot,
    val startTimeSeconds: Double,
    val endTimeSeconds: Double,
    val startSampleIndex: Int,
    val endSampleIndex: Int
) {
    val durationSeconds: Double get() = endTimeSeconds - startTimeSeconds
}

/**
 * Assigns each detected step to a foot based on the sign of the world frame lateral (X)
 * acceleration at the step instant, then groups successive same foot steps into strides.
 *
 * The Foot.LEFT and Foot.RIGHT labels are arbitrary at the API level: a positive lateral sign
 * is mapped to Foot.LEFT and negative to Foot.RIGHT. Which way the patient's body sways during
 * a left versus right strike depends on the phone's orientation in the pocket, so the absolute
 * label is not meaningful for asymmetry analysis. What matters is consistency: a same direction
 * lateral sign across two consecutive steps produces the same foot label, which lets the
 * pipeline detect when the alternation breaks (a real signal artifact) versus when it holds.
 *
 * Per SPEC.md Section 7.1 step 6 the sign of lateral acceleration at each step assigns left or
 * right; this module implements that rule.
 */
class StridePairing {

    fun assignFeet(steps: List<StepEvent>, lateralAtStep: DoubleArray): List<FootStep> {
        require(steps.size == lateralAtStep.size) {
            "steps and lateralAtStep must have the same length"
        }
        val out = ArrayList<FootStep>(steps.size)
        for (i in steps.indices) {
            val foot = if (lateralAtStep[i] >= 0.0) Foot.LEFT else Foot.RIGHT
            out.add(FootStep(steps[i], foot))
        }
        return out
    }

    fun pairStrides(labelled: List<FootStep>): List<Stride> {
        if (labelled.size < 2) return emptyList()
        val out = ArrayList<Stride>()
        for (i in 0 until labelled.size - 2) {
            val a = labelled[i]
            val b = labelled[i + 2]
            if (a.foot == b.foot) {
                out.add(Stride(
                    foot = a.foot,
                    startTimeSeconds = a.step.timeSeconds,
                    endTimeSeconds = b.step.timeSeconds,
                    startSampleIndex = a.step.sampleIndex,
                    endSampleIndex = b.step.sampleIndex
                ))
            }
        }
        return out
    }
}
