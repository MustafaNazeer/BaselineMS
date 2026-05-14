package com.mustafanazeer.baselinems.battery.vision

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun VisionQualityCheckScreen(
    state: VisionTestState.QualityCheck,
    onContinue: () -> Unit,
    onCancel: () -> Unit
) {
    val luxCopy = when {
        state.lux == null -> "Measuring lighting..."
        state.lux < VisionTestViewModel.LUX_ACCEPT_MIN -> "Too dim. Move to a brighter spot."
        state.lux > VisionTestViewModel.LUX_ACCEPT_MAX -> "Too bright. Move out of direct light."
        else -> "Lighting looks good."
    }
    val distanceCopy = when {
        state.distanceCm == null -> "Looking for your face..."
        state.distanceCm < VisionTestViewModel.DIST_ACCEPT_MIN -> "Too close. Hold the phone a little further."
        state.distanceCm > VisionTestViewModel.DIST_ACCEPT_MAX -> "Too far. Hold the phone a little closer."
        else -> "Distance looks good."
    }
    val ready = state.luxOk && state.distanceOk
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Quality check", style = MaterialTheme.typography.headlineMedium)
        Text(luxCopy)
        Text(distanceCopy)
        if (ready) {
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
        }
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}
