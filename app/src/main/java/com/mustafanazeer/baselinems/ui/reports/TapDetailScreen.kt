package com.mustafanazeer.baselinems.ui.reports

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mustafanazeer.baselinems.R

object TapFeatureKeys {
    const val DOMINANT_RATE = "dominant_tap_rate_hz"
    const val NON_DOMINANT_RATE = "non_dominant_tap_rate_hz"
    const val DOMINANT_ITI_CV = "dominant_iti_cv"
    const val NON_DOMINANT_ITI_CV = "non_dominant_iti_cv"
    const val ASYMMETRY = "asymmetry_index"
    const val MISS_RATE = "miss_rate"
    const val DOMINANT_OFF_TARGET = "dominant_off_target_taps"
    const val NON_DOMINANT_OFF_TARGET = "non_dominant_off_target_taps"
    const val DOMINANT_IN_TARGET = "dominant_in_target_taps"
    const val NON_DOMINANT_IN_TARGET = "non_dominant_in_target_taps"
}

@Composable
fun TapDetailScreen(
    state: TestDetailScreenState,
    onBack: () -> Unit,
    onAbout: () -> Unit
) {
    val labels = mapOf(
        TapFeatureKeys.DOMINANT_RATE to stringResource(R.string.phase9_chart_axis_tap_dominant),
        TapFeatureKeys.NON_DOMINANT_RATE to stringResource(R.string.phase9_chart_axis_tap_non_dominant),
        TapFeatureKeys.DOMINANT_ITI_CV to stringResource(R.string.phase9_chart_axis_tap_inter_tap_cv),
        TapFeatureKeys.NON_DOMINANT_ITI_CV to stringResource(R.string.phase9_chart_axis_tap_inter_tap_cv),
        TapFeatureKeys.ASYMMETRY to stringResource(R.string.phase9_chart_axis_tap_asymmetry),
        TapFeatureKeys.MISS_RATE to stringResource(R.string.phase9_chart_axis_tap_miss_rate),
        TapFeatureKeys.DOMINANT_OFF_TARGET to stringResource(R.string.phase9_chart_axis_tap_off_target),
        TapFeatureKeys.NON_DOMINANT_OFF_TARGET to stringResource(R.string.phase9_chart_axis_tap_off_target)
    )
    TestDetailScreen(
        titleResId = R.string.phase9_test_detail_title_tap,
        state = state,
        perFeatureKeys = listOf(
            TapFeatureKeys.DOMINANT_RATE,
            TapFeatureKeys.NON_DOMINANT_RATE,
            TapFeatureKeys.DOMINANT_ITI_CV,
            TapFeatureKeys.NON_DOMINANT_ITI_CV,
            TapFeatureKeys.ASYMMETRY,
            TapFeatureKeys.MISS_RATE,
            TapFeatureKeys.DOMINANT_OFF_TARGET,
            TapFeatureKeys.NON_DOMINANT_OFF_TARGET
        ),
        perFeatureLabels = labels,
        includeSteadinessColumn = false,
        progressiveDisclosure = null,
        perFeatureCaveats = emptyMap(),
        referenceBands = emptyMap(),
        overlayDisclaimerResId = R.string.phase9_test_detail_no_overlay_disclaimer,
        onBack = onBack,
        onAbout = onAbout
    )
}
