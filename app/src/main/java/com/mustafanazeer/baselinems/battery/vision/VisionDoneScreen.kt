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
import com.mustafanazeer.baselinems.dsp.vision.SloanScore

@Composable
fun VisionDoneScreen(score: SloanScore, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Vision test recorded", style = MaterialTheme.typography.headlineMedium)
        Text("At 100% contrast, you read ${score.correctAt100Pct} of 40 letters.")
        Text("At 2.5% contrast, you read ${score.correctAt2Pt5Pct} of 40 letters.")
        Text("At 1.25% contrast, you read ${score.correctAt1Pt25Pct} of 40 letters.")
        Text(
            "Low contrast vision changes over time. These numbers will be most useful when " +
                "compared against your own past tests on this phone, not against anyone else's results.",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}
