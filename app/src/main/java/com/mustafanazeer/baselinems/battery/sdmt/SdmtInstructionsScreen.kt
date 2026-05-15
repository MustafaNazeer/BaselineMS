package com.mustafanazeer.baselinems.battery.sdmt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SdmtInstructionsScreen(onStart: () -> Unit, onCancel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Symbol Digit Modalities Test (SDMT), smartphone variant",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            "This is a smartphone variant of the Symbol Digit Modalities Test (SDMT) developed " +
                "for personal tracking. The Symbol Digit Modalities Test is a registered " +
                "instrument of Western Psychological Services; the symbols used here are custom " +
                "abstract shapes, not the official SDMT stimulus materials, and scores from this " +
                "app are not directly comparable to a clinician administered SDMT.",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "This is a 90 second timed task. You will see a series of symbols at the top of " +
                "the screen. Tap the digit that matches each symbol per the key shown. Some " +
                "people answer 15 to 20; some answer 50 or more. There is no right number. " +
                "The point is to give your honest effort across the full 90 seconds. If you " +
                "are not sure about a symbol, tap your best guess. There is no skip option " +
                "on this test."
        )
        Text("Here is the key you will use:", style = MaterialTheme.typography.titleMedium)
        SdmtSymbolKey()
        Spacer(Modifier.height(8.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Start") }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel the test and discard this session.")
        }
    }
}
