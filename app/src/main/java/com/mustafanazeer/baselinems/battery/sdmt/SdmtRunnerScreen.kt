package com.mustafanazeer.baselinems.battery.sdmt

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mustafanazeer.baselinems.dsp.sdmt.SdmtSymbol

@Composable
fun SdmtRunnerScreen(
    state: SdmtTestState.Running,
    onDigitTap: (Int) -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SdmtSymbolKey()

        if (state.showCountdown) {
            val secondsRemaining = (
                (SdmtViewModel.TEST_DURATION_MS - state.elapsedMs).coerceAtLeast(0L) / 1000L
            ).toInt()
            Text(
                "$secondsRemaining s remaining",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (state.consecutiveErrors >= SdmtViewModel.MID_TEST_REASSURANCE_THRESHOLD) {
            Text(
                "The mapping takes a moment. The key is at the top of the screen.",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(Modifier.height(8.dp))
        PromptSymbol(state.currentPrompt)
        Spacer(Modifier.height(8.dp))

        SdmtNumericKeypad(onDigitTap = onDigitTap)

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }
    }
}

@Composable
private fun PromptSymbol(symbol: SdmtSymbol) {
    Surface(
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = symbol.drawableRes),
                contentDescription = null,
                modifier = Modifier.size(140.dp)
            )
        }
    }
}
