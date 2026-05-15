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
import com.mustafanazeer.baselinems.dsp.sdmt.SdmtScore

@Composable
fun SdmtDoneScreen(score: SdmtScore, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("SDMT recorded", style = MaterialTheme.typography.headlineMedium)
        Text(
            "You answered ${score.totalCorrect} of ${score.totalAttempted} correctly in 90 seconds."
        )
        Text(
            "Cognitive performance changes over time. These numbers will be most useful when " +
                "compared against your own past tests on this phone, not against anyone else's results.",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}
