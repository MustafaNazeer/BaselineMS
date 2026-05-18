package com.mustafanazeer.baselinems.ui.reports

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mustafanazeer.baselinems.R

object VisionFeatureKeys {
    const val CORRECT_100PCT = "sloan_100pct"
    const val CORRECT_2PT5PCT = "sloan_2pt5pct"
    const val CORRECT_1PT25PCT = "sloan_1pt25pct"
    const val SLOAN_TOTAL = "sloan_total"
}

@Composable
fun VisionDetailScreen(
    state: TestDetailScreenState,
    onBack: () -> Unit,
    onAbout: () -> Unit
) {
    val labels = mapOf(
        VisionFeatureKeys.CORRECT_100PCT to stringResource(R.string.phase9_chart_axis_vision_100pct),
        VisionFeatureKeys.CORRECT_2PT5PCT to stringResource(R.string.phase9_chart_axis_vision_2pt5pct),
        VisionFeatureKeys.CORRECT_1PT25PCT to stringResource(R.string.phase9_chart_axis_vision_1pt25pct),
        VisionFeatureKeys.SLOAN_TOTAL to stringResource(R.string.phase9_chart_axis_vision_total)
    )
    TestDetailScreen(
        titleResId = R.string.phase9_test_detail_title_vision,
        state = state,
        perFeatureKeys = listOf(
            VisionFeatureKeys.CORRECT_100PCT,
            VisionFeatureKeys.CORRECT_2PT5PCT,
            VisionFeatureKeys.CORRECT_1PT25PCT,
            VisionFeatureKeys.SLOAN_TOTAL
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
