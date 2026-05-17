package com.mustafanazeer.baselinems.ui.reports

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mustafanazeer.baselinems.R

object GaitFeatureKeys {
    const val CADENCE = "cadence_steps_per_minute"
    const val STRIDE_LENGTH = "mean_stride_length_meters"
    const val STEP_TIME_VARIABILITY = "step_time_cv"
    const val STRIDE_ASYMMETRY = "stride_asymmetry_index"
    const val DOUBLE_SUPPORT = "double_support_time_seconds"
    const val DETECTED_STEP_COUNT = "detected_step_count"
}

@Composable
fun GaitDetailScreen(
    state: TestDetailScreenState,
    onBack: () -> Unit,
    onAbout: () -> Unit,
    cadenceReferenceBand: ReferenceRange? = null
) {
    val labels = mapOf(
        GaitFeatureKeys.CADENCE to stringResource(R.string.phase9_chart_axis_gait_cadence),
        GaitFeatureKeys.STRIDE_LENGTH to stringResource(R.string.phase9_chart_axis_gait_stride_length),
        GaitFeatureKeys.STEP_TIME_VARIABILITY to stringResource(R.string.phase9_chart_axis_gait_step_time_variability),
        GaitFeatureKeys.STRIDE_ASYMMETRY to stringResource(R.string.phase9_chart_axis_gait_stride_asymmetry),
        GaitFeatureKeys.DOUBLE_SUPPORT to stringResource(R.string.phase9_chart_axis_gait_double_support)
    )
    val caveats = mapOf(
        GaitFeatureKeys.STRIDE_LENGTH to R.string.phase9_chart_caveat_stride_length
    )
    val referenceBands = if (cadenceReferenceBand != null) {
        mapOf(GaitFeatureKeys.CADENCE to cadenceReferenceBand)
    } else {
        emptyMap()
    }
    TestDetailScreen(
        titleResId = R.string.phase9_test_detail_title_gait,
        state = state,
        perFeatureKeys = listOf(
            GaitFeatureKeys.CADENCE,
            GaitFeatureKeys.STRIDE_LENGTH,
            GaitFeatureKeys.STEP_TIME_VARIABILITY,
            GaitFeatureKeys.STRIDE_ASYMMETRY,
            GaitFeatureKeys.DOUBLE_SUPPORT
        ),
        perFeatureLabels = labels,
        includeSteadinessColumn = false,
        progressiveDisclosure = null,
        perFeatureCaveats = caveats,
        referenceBands = referenceBands,
        overlayDisclaimerResId = R.string.phase9_test_detail_no_overlay_disclaimer,
        onBack = onBack,
        onAbout = onAbout
    )
}
