package com.mustafanazeer.baselinems.battery.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.mustafanazeer.baselinems.R

@Composable
fun VoiceRunnerScreen(
    state: VoiceTestState.Running,
    onCancel: () -> Unit,
    recordingDurationSec: Int = VoiceTestViewModel.RECORDING_DURATION_SEC
) {
    val totalMs = recordingDurationSec * 1000L
    val secondsRemaining = ((totalMs - state.elapsedMs).coerceAtLeast(0L) / 1000L).toInt()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = null
            )
            Text(stringResource(R.string.voice_test_runner_recording_affordance))
            Spacer(Modifier.fillMaxWidth(0.6f))
            Text(
                stringResource(
                    R.string.voice_test_runner_seconds_remaining,
                    secondsRemaining
                ),
                style = MaterialTheme.typography.titleMedium
            )
        }
        Text(
            stringResource(R.string.voice_test_passage_grandfather),
            style = MaterialTheme.typography.titleLarge.copy(
                lineHeight = 1.5.em,
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None
                )
            )
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.voice_test_cancel))
        }
    }
}
