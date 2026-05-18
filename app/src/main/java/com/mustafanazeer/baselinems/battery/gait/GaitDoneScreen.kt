package com.mustafanazeer.baselinems.battery.gait

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Post test confirmation screen. Renders a thank you and a quality aware status line:
 *
 * - "Captured with high confidence" when the quality score exceeds 0.8.
 * - "Captured with limited confidence" when the score sits between 0.5 and 0.8 (inclusive).
 * - "Captured but quality is low" when the score is below 0.5.
 *
 * The plain language framing intentionally hides the raw 0.78 number.
 */
@Composable
fun GaitDoneScreen(
    state: GaitTestState.Done,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val confidenceLine = when {
        state.features.qualityScore > 0.8 -> "Captured with high confidence"
        state.features.qualityScore >= 0.5 -> "Captured with limited confidence"
        else -> "Captured but quality is low"
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Thank you",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Walk recorded. Trends are most reliable across several weeks.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = confidenceLine,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier)
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .heightIn(min = 64.dp)
        ) {
            Text(
                text = "Continue",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
