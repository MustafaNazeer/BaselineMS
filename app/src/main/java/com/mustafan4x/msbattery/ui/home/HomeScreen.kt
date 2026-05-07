package com.mustafan4x.msbattery.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mustafan4x.msbattery.data.SessionDao
import com.mustafan4x.msbattery.data.SessionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    sessionDao: SessionDao,
    onStartSession: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val sessions by sessionDao.observeAll().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MS Battery") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onStartSession, modifier = Modifier.padding(0.dp)) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Text("  Start weekly battery", style = MaterialTheme.typography.titleMedium)
            }
            Text("History", style = MaterialTheme.typography.titleMedium)
            if (sessions.isEmpty()) {
                Text("No sessions yet. Run the weekly battery to get started.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sessions) { session ->
                        SessionRow(session)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: SessionEntity) {
    val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    Column {
        Text(df.format(Date(session.startedAtEpochMs)), style = MaterialTheme.typography.bodyLarge)
        Text(
            if (session.completedAtEpochMs != null) "Completed" else "In progress",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
