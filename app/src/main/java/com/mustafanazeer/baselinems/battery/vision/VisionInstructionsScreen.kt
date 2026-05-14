package com.mustafanazeer.baselinems.battery.vision

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun VisionInstructionsScreen(onStart: () -> Unit, onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Vision test", style = MaterialTheme.typography.headlineMedium)
        Text(
            "You will see three sets of letters, going from clear to very faint. " +
                "Some of the letters will be very faint. That is expected. " +
                "Tap your best guess, or tap Skip if you cannot see a letter."
        )
        Text(
            "Hold the phone at a comfortable reading distance, about 40 cm. " +
                "We will check the lighting and your distance before starting.",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "This test uses your front camera to check the lighting and how far your face is from the screen. " +
                "Camera frames are read in memory only and are never saved or sent anywhere.",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "If you need to stop the entire test, tap Cancel. Your data will not be saved for this session.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Start") }
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}
