package com.mustafanazeer.baselinems.ui.reports

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mustafanazeer.baselinems.R
import com.mustafanazeer.baselinems.data.TestType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    state: ReportsScreenState,
    onBack: () -> Unit,
    onCardSelected: (TestType) -> Unit,
    onRunFirstCheckIn: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.phase9_reports_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (state) {
                ReportsScreenState.Loading -> {
                    Text(
                        text = stringResource(R.string.phase9_loading_placeholder),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                ReportsScreenState.Empty -> {
                    ReportsEmptyState(
                        onRunFirstCheckIn = onRunFirstCheckIn,
                        onBackToHome = onBack
                    )
                }
                is ReportsScreenState.Ready -> {
                    Text(
                        text = stringResource(R.string.phase9_reports_warmth_line),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    state.testSummaries.forEach { card ->
                        TestSummaryCardView(card = card, onClick = { onCardSelected(card.testType) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportsEmptyState(
    onRunFirstCheckIn: () -> Unit,
    onBackToHome: () -> Unit
) {
    Text(
        text = stringResource(R.string.phase9_reports_empty_body),
        style = MaterialTheme.typography.bodyLarge
    )
    TextButton(onClick = onRunFirstCheckIn) {
        Text(stringResource(R.string.phase9_reports_run_first_check_in))
    }
    TextButton(onClick = onBackToHome) {
        Text(stringResource(R.string.phase9_reports_back_to_home))
    }
}

@Composable
private fun TestSummaryCardView(
    card: TestSummaryCard,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = card.displayName,
                style = MaterialTheme.typography.titleMedium
            )
            val sparklineSize = card.primaryFeatureSparkline.size
            val headerText = when {
                card.sessionCount == 0 -> ""
                card.sessionCount == 1 -> stringResource(R.string.phase9_reports_sessions_recorded_single)
                else -> stringResource(
                    R.string.phase9_reports_sessions_recorded_template,
                    card.sessionCount,
                    sparklineSize
                )
            }
            if (headerText.isNotEmpty()) {
                Text(
                    text = headerText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            card.latestQualityBand?.let { QualityBandChip(band = it) }
            if (card.primaryFeatureSparkline.size >= 2) {
                val series = FeatureSeries(
                    key = "sparkline",
                    displayName = card.displayName,
                    unit = "",
                    points = card.primaryFeatureSparkline.mapIndexed { index, value ->
                        TimedPoint(
                            epochMs = index.toLong(),
                            value = value,
                            qualityBand = QualityBand.HIGH
                        )
                    }
                )
                TrendLineChart(series = series, isSparkline = true)
            }
        }
    }
}
