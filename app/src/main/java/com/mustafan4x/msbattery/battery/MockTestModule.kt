package com.mustafan4x.msbattery.battery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mustafan4x.msbattery.data.TestType

class MockTestModule : TestModule {

    data class MockResult(
        override val qualityScore: Double,
        override val features: Map<String, Double>
    ) : TestResultPayload

    override val testType: TestType = TestType.TAP
    override val displayName: String = "Mock Test"
    override val instructions: String = "This is a mock test used during scaffolding. Tap Continue."
    override val estimatedDurationSeconds: Int = 1

    @Composable
    override fun Content(onComplete: (TestResultPayload) -> Unit) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(displayName)
            Text(instructions, textAlign = TextAlign.Center)
            Button(onClick = {
                onComplete(MockResult(qualityScore = 1.0, features = mapOf("mock_value" to 42.0)))
            }) {
                Text("Continue")
            }
        }
    }
}
