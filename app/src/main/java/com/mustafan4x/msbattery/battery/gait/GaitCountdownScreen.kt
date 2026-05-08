package com.mustafan4x.msbattery.battery.gait

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Countdown screen for the gait test. Renders the remaining seconds in `displayLarge` so the
 * number is clearly visible from a distance with the phone in hand. Each tick announces the
 * number through `Modifier.semantics` so TalkBack reads "3", "2", "1" rather than the visual
 * label only.
 */
@Composable
fun GaitCountdownScreen(
    state: GaitTestState.Countdown,
    modifier: Modifier = Modifier
) {
    val secondsLabel = state.secondsRemaining.toString()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Get ready to walk",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = secondsLabel,
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.semantics { contentDescription = secondsLabel }
        )
    }
}
