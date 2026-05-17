package com.mustafanazeer.baselinems.ui.reports

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mustafanazeer.baselinems.R

object SdmtFeatureKeys {
    const val CORRECT_IN_90S = "sdmt_total_correct"
    const val ERROR_RATE = "sdmt_error_rate"
    const val RESPONSE_TIME_MEAN = "sdmt_response_time_mean_ms"
    const val RESPONSE_TIME_SD = "sdmt_response_time_sd_ms"
    const val TOTAL_ATTEMPTED = "sdmt_total_attempted"
    const val TOTAL_ERRORS = "sdmt_total_errors"
}

@Composable
fun SdmtDetailScreen(
    state: TestDetailScreenState,
    onBack: () -> Unit,
    onAbout: () -> Unit
) {
    val labels = mapOf(
        SdmtFeatureKeys.CORRECT_IN_90S to stringResource(R.string.phase9_chart_axis_sdmt_correct),
        SdmtFeatureKeys.RESPONSE_TIME_MEAN to stringResource(R.string.phase9_chart_axis_sdmt_rt_mean),
        SdmtFeatureKeys.RESPONSE_TIME_SD to stringResource(R.string.phase9_chart_axis_sdmt_rt_sd),
        SdmtFeatureKeys.ERROR_RATE to stringResource(R.string.phase9_chart_axis_sdmt_error_rate)
    )
    TestDetailScreen(
        titleResId = R.string.phase9_test_detail_title_sdmt,
        state = state,
        perFeatureKeys = listOf(
            SdmtFeatureKeys.CORRECT_IN_90S,
            SdmtFeatureKeys.RESPONSE_TIME_MEAN,
            SdmtFeatureKeys.RESPONSE_TIME_SD,
            SdmtFeatureKeys.ERROR_RATE
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
