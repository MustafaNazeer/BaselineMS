package com.mustafanazeer.baselinems.battery.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mustafanazeer.baselinems.R
import com.mustafanazeer.baselinems.dsp.voice.VoiceFeatureSet
import com.mustafanazeer.baselinems.dsp.voice.VoiceFeatures
import com.mustafanazeer.baselinems.dsp.voice.VoiceQualityScore
import com.mustafanazeer.baselinems.dsp.voice.VoiceSteadinessBand

@Composable
fun VoiceDoneScreen(
    features: VoiceFeatureSet,
    @Suppress("UNUSED_PARAMETER") quality: VoiceQualityScore,
    onDone: () -> Unit
) {
    val voicedSeconds = features.voicedSeconds.toInt().coerceAtLeast(0)
    val perFeatureLineRes = when (VoiceFeatures.steadinessBand(features)) {
        VoiceSteadinessBand.STEADY -> R.string.voice_test_done_per_feature_steady
        VoiceSteadinessBand.MOSTLY_STEADY -> R.string.voice_test_done_per_feature_mostly_steady
        VoiceSteadinessBand.VARIED -> R.string.voice_test_done_per_feature_varied
        VoiceSteadinessBand.UNMEASURABLE -> R.string.voice_test_done_per_feature_unmeasurable
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(R.string.voice_test_done_headline),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(stringResource(perFeatureLineRes, voicedSeconds))
        Text(
            stringResource(R.string.voice_test_done_trend_warmth),
            style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.voice_test_continue))
        }
    }
}
