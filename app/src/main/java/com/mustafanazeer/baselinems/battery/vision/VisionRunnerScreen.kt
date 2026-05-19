package com.mustafanazeer.baselinems.battery.vision

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun VisionRunnerScreen(
    state: VisionTestState.Running,
    onTap: (Char?) -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Contrast ${formatContrastPercent(state.contrast.percent)} percent", style = MaterialTheme.typography.titleMedium)
        SloanLetter(letter = state.currentLetter, contrast = state.contrast)

        if (state.consecutiveSkipCount >= 4) {
            Text(
                "These letters are designed to be very faint. Keep going if you can.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            state.multipleChoiceLetters.take(2).forEach { letter ->
                Button(
                    onClick = { onTap(letter) },
                    modifier = Modifier.size(96.dp)
                ) {
                    Text(letter.toString(), style = MaterialTheme.typography.displaySmall)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            state.multipleChoiceLetters.drop(2).take(2).forEach { letter ->
                Button(
                    onClick = { onTap(letter) },
                    modifier = Modifier.size(96.dp)
                ) {
                    Text(letter.toString(), style = MaterialTheme.typography.displaySmall)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { onTap(null) }, modifier = Modifier.fillMaxWidth()) { Text("Skip") }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}

internal fun formatContrastPercent(percent: Double): String {
    return if (percent % 1.0 == 0.0) {
        percent.toInt().toString()
    } else {
        percent.toString()
    }
}
