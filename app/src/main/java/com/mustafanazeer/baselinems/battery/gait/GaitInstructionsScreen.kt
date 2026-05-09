package com.mustafanazeer.baselinems.battery.gait

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Instructions screen for the gait test. Body copy is fixed verbatim per
 * `docs/plans/phase-4-gait-test-module-integration.md` Task 6 (Patient Advocate driven safety
 * framing): straight line, front pocket, flat surface with wall in reach, opt out if unsafe.
 *
 * Sizing follows `docs/design/tokens.md` Section 5.1: primary action button at 64 dp height with
 * 32 dp horizontal margin, body in `bodyLarge` per Section 3.2.
 */
@Composable
fun GaitInstructionsScreen(
    onStart: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Gait test",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Walk in a straight line for 30 seconds. Place your phone in a front pocket. " +
                "Use a flat, unobstructed surface; have a wall or counter within reach if you " +
                "need it. If you do not feel safe walking right now, you can skip this test.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier)
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .heightIn(min = 64.dp),
            contentPadding = ButtonDefaults.ContentPadding
        ) {
            Text(
                text = "I am ready",
                style = MaterialTheme.typography.labelLarge
            )
        }
        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Text(
                text = "Skip for now",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
