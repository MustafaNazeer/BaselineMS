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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mustafanazeer.baselinems.R

/**
 * Post test confirmation screen. Renders a thank you and a quality aware status line that uses
 * the Phase 9 ratified Reports chip vocabulary verbatim so the same recording reads the same
 * across the Done screen and the Reports screen:
 *
 * - "Reliable" when the quality score exceeds 0.8.
 * - "Mostly reliable" when the score sits between 0.5 and 0.8 (inclusive).
 * - "Less reliable" when the score is below 0.5.
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
        state.features.qualityScore > 0.8 -> stringResource(R.string.phase9_quality_chip_reliable)
        state.features.qualityScore >= 0.5 -> stringResource(R.string.phase9_quality_chip_mostly_reliable)
        else -> stringResource(R.string.phase9_quality_chip_less_reliable)
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
