package com.mustafan4x.baselinems.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.mustafan4x.baselinems.data.SessionDao
import com.mustafan4x.baselinems.data.SessionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
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
                title = { Text("BaselineMS") },
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
            Button(onClick = onStartSession) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start this week's check in", style = MaterialTheme.typography.titleMedium)
            }
            Text("History", style = MaterialTheme.typography.titleMedium)
            if (sessions.isEmpty()) {
                Text(
                    "You have not run a check in yet. The first one takes about ten minutes " +
                        "and produces a record you can share with your neurologist next visit.",
                    style = MaterialTheme.typography.bodyLarge
                )
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
    Column {
        Text(
            formatRelative(session.startedAtEpochMs, Locale.getDefault()),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            if (session.completedAtEpochMs != null) "Completed" else "In progress",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

internal fun formatRelative(epochMs: Long, locale: Locale): String {
    val now = Calendar.getInstance()
    val that = Calendar.getInstance().apply { timeInMillis = epochMs }
    val timeFmt = SimpleDateFormat("HH:mm", locale)
    val daysApart = run {
        val a = now.clone() as Calendar
        a.set(Calendar.HOUR_OF_DAY, 0); a.set(Calendar.MINUTE, 0)
        a.set(Calendar.SECOND, 0); a.set(Calendar.MILLISECOND, 0)
        val b = that.clone() as Calendar
        b.set(Calendar.HOUR_OF_DAY, 0); b.set(Calendar.MINUTE, 0)
        b.set(Calendar.SECOND, 0); b.set(Calendar.MILLISECOND, 0)
        ((a.timeInMillis - b.timeInMillis) / (24L * 60 * 60 * 1000)).toInt()
    }
    return when {
        daysApart == 0 -> "Today, ${timeFmt.format(Date(epochMs))}"
        daysApart == 1 -> "Yesterday, ${timeFmt.format(Date(epochMs))}"
        daysApart in 2..6 -> {
            val dow = SimpleDateFormat("EEEE", locale).format(Date(epochMs))
            "$dow, ${timeFmt.format(Date(epochMs))}"
        }
        else -> SimpleDateFormat("MMM d", locale).format(Date(epochMs))
    }
}
