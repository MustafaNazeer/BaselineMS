package com.mustafanazeer.baselinems.battery.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mustafanazeer.baselinems.R

@Composable
fun VoiceAudioQualityCheckScreen(
    state: VoiceTestState.AudioQualityCheck,
    onStartReading: () -> Unit,
    onCancel: () -> Unit
) {
    val bandCopy = when (state.band) {
        NoiseBand.Pending -> stringResource(R.string.voice_test_audio_quality_pending)
        NoiseBand.Green -> stringResource(R.string.voice_test_audio_quality_green)
        NoiseBand.Yellow -> stringResource(R.string.voice_test_audio_quality_yellow)
        NoiseBand.Red -> stringResource(R.string.voice_test_audio_quality_red)
    }
    val startEnabled = state.band == NoiseBand.Green || state.band == NoiseBand.Yellow
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            stringResource(R.string.voice_test_audio_quality_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(bandCopy, style = MaterialTheme.typography.bodyLarge)
        if (state.band == NoiseBand.Red) {
            Text(
                stringResource(R.string.voice_test_audio_quality_red_helper),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Button(
            onClick = onStartReading,
            enabled = startEnabled,
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.voice_test_audio_quality_start_reading)) }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.voice_test_cancel))
        }
    }
}
