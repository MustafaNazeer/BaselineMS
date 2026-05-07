package com.mustafan4x.msbattery.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mustafan4x.msbattery.battery.BatteryOrchestrator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionRunnerScreen(
    orchestrator: BatteryOrchestrator,
    onFinished: () -> Unit
) {
    val state by orchestrator.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly Battery") },
                actions = {
                    if (state !is BatteryOrchestrator.State.Completed) {
                        Button(onClick = { orchestrator.cancel(); onFinished() }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val s = state) {
                BatteryOrchestrator.State.Idle -> Text("Session not started")
                is BatteryOrchestrator.State.Running -> {
                    val module = orchestrator.modules[s.index]
                    module.Content { result ->
                        orchestrator.recordResult(
                            testType = module.testType,
                            qualityScore = result.qualityScore,
                            features = result.features
                        )
                    }
                }
                BatteryOrchestrator.State.Completed -> {
                    Text("Session complete")
                    Button(onClick = onFinished) { Text("Done") }
                }
            }
        }
    }
}
