package com.mustafanazeer.baselinems.battery.sdmt

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
fun SdmtCancelledScreen(onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Test cancelled", style = MaterialTheme.typography.headlineMedium)
        Text("Nothing was saved for this session.")
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Back to home") }
    }
}
