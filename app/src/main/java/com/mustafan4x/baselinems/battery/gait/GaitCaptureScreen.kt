package com.mustafan4x.baselinems.battery.gait

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Capture screen for the gait test. Shows a circular progress indicator wrapping a "Walking"
 * label, the elapsed and total seconds, and a Cancel button. The user is not expected to be
 * looking at this screen during the walk (the phone is in a front pocket), so the visual is
 * deliberately minimal: no animated map, no decorative motion, in line with
 * `docs/design/tokens.md` Section 9.2.
 */
private const val TOTAL_DURATION_MILLIS = 30_000

@Composable
fun GaitCaptureScreen(
    state: GaitTestState.Capturing,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalSeconds = TOTAL_DURATION_MILLIS / 1000
    val elapsedSeconds = (state.progressMillis / 1000).coerceIn(0, totalSeconds)
    val progress = (state.progressMillis.toFloat() / TOTAL_DURATION_MILLIS).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(160.dp),
                strokeWidth = 8.dp
            )
            Text(
                text = "Walking",
                style = MaterialTheme.typography.titleLarge
            )
        }
        Text(
            text = "$elapsedSeconds of $totalSeconds seconds",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .heightIn(min = 64.dp)
        ) {
            Text(
                text = "Cancel",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
