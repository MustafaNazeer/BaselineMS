package com.mustafanazeer.baselinems.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mustafanazeer.baselinems.battery.BatteryOrchestrator
import com.mustafanazeer.baselinems.ui.common.displayLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionRunnerScreen(
    orchestrator: BatteryOrchestrator,
    onFinished: () -> Unit
) {
    val state by orchestrator.state.collectAsState()
    var confirmingCancel by remember { mutableStateOf(false) }

    val currentOrchestrator by rememberUpdatedState(orchestrator)
    DisposableEffect(currentOrchestrator) {
        onDispose {
            if (currentOrchestrator.state.value is BatteryOrchestrator.State.Running) {
                currentOrchestrator.cancelSession()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("This week's check in") },
                actions = {
                    if (state !is BatteryOrchestrator.State.Completed) {
                        TextButton(onClick = { confirmingCancel = true }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val s = state) {
                BatteryOrchestrator.State.Idle -> {
                    CircularProgressIndicator()
                    Text(
                        "Getting your check in ready",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                is BatteryOrchestrator.State.Running -> {
                    val module = orchestrator.modules[s.index]
                    val total = orchestrator.modules.size
                    val current = s.index + 1
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "${module.testType.displayLabel()} (test $current of $total)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Estimated ${module.estimatedDurationSeconds} seconds",
                            style = MaterialTheme.typography.bodySmall
                        )
                        LinearProgressIndicator(
                            progress = { (s.index).toFloat() / total.toFloat() },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                    }
                    module.Content { result ->
                        orchestrator.recordResult(
                            testType = module.testType,
                            payload = result
                        )
                    }
                }
                BatteryOrchestrator.State.Completed -> {
                    Text(
                        "All done. Your check in has been saved.",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "You can view your results from the home screen, " +
                            "or share a report later from Settings.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = onFinished) { Text("Done") }
                }
            }
        }
    }

    if (confirmingCancel) {
        AlertDialog(
            onDismissRequest = { confirmingCancel = false },
            title = { Text("Stop this check in?") },
            text = {
                Text("Your results so far will not be saved. You can come back to it later.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmingCancel = false
                    orchestrator.cancel()
                    onFinished()
                }) { Text("Stop and discard") }
            },
            dismissButton = {
                TextButton(onClick = { confirmingCancel = false }) {
                    Text("Keep going")
                }
            }
        )
    }
}
