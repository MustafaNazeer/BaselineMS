package com.mustafanazeer.baselinems.battery.vision

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun VisionCameraDeniedScreen(onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Vision test unavailable on this device", style = MaterialTheme.typography.headlineMedium)
        Text(
            "The vision test needs the front camera to check the lighting and how far your face is from the screen. " +
                "You can still run the other four tests in the battery."
        )
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Back to home") }
    }
}
