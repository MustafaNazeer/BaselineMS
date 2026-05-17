package com.mustafanazeer.baselinems.ui.reports

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mustafanazeer.baselinems.R

object VoiceFeatureKeys {
    const val JITTER_LOCAL = "jitter_local"
    const val SHIMMER_LOCAL = "shimmer_local"
    const val HNR_DB = "hnr_db"
    const val F0_SD = "f0_sd_hz"
    const val SPEAKING_RATE_WPM = "speaking_rate_wpm"
    const val PAUSE_FRACTION = "pause_fraction"
    const val F0_MEAN = "f0_mean_hz"
}

@Composable
fun VoiceDetailScreen(
    state: TestDetailScreenState,
    onBack: () -> Unit,
    onAbout: () -> Unit,
    primaryFeatureKey: String = VoiceFeatureKeys.JITTER_LOCAL
) {
    val labels = mapOf(
        VoiceFeatureKeys.JITTER_LOCAL to stringResource(R.string.phase9_chart_axis_voice_jitter),
        VoiceFeatureKeys.SHIMMER_LOCAL to stringResource(R.string.phase9_chart_axis_voice_shimmer),
        VoiceFeatureKeys.HNR_DB to stringResource(R.string.phase9_chart_axis_voice_hnr),
        VoiceFeatureKeys.F0_SD to stringResource(R.string.phase9_chart_axis_voice_f0_sd),
        VoiceFeatureKeys.SPEAKING_RATE_WPM to stringResource(R.string.phase9_chart_axis_voice_speaking_rate),
        VoiceFeatureKeys.PAUSE_FRACTION to stringResource(R.string.phase9_chart_axis_voice_pause_fraction)
    )
    TestDetailScreen(
        titleResId = R.string.phase9_test_detail_title_voice,
        state = state,
        perFeatureKeys = listOf(
            VoiceFeatureKeys.JITTER_LOCAL,
            VoiceFeatureKeys.SHIMMER_LOCAL,
            VoiceFeatureKeys.HNR_DB,
            VoiceFeatureKeys.F0_SD,
            VoiceFeatureKeys.SPEAKING_RATE_WPM,
            VoiceFeatureKeys.PAUSE_FRACTION
        ),
        perFeatureLabels = labels,
        includeSteadinessColumn = true,
        progressiveDisclosure = ProgressiveDisclosure(
            primaryFeatureKey = primaryFeatureKey,
            expanderLabelResId = R.string.phase9_test_detail_show_all_voice_measures
        ),
        perFeatureCaveats = emptyMap(),
        referenceBands = emptyMap(),
        overlayDisclaimerResId = R.string.phase9_test_detail_no_overlay_disclaimer,
        onBack = onBack,
        onAbout = onAbout
    )
}
