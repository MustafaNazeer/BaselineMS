package com.mustafanazeer.baselinems.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mustafanazeer.baselinems.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestDetailScreen(
    titleResId: Int,
    state: TestDetailScreenState,
    perFeatureKeys: List<String>,
    perFeatureLabels: Map<String, String>,
    includeSteadinessColumn: Boolean,
    progressiveDisclosure: ProgressiveDisclosure?,
    perFeatureCaveats: Map<String, Int>,
    referenceBands: Map<String, ReferenceRange>,
    overlayDisclaimerResId: Int,
    onBack: () -> Unit,
    onAbout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleResId)) },
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
                TestDetailScreenState.Loading -> {
                    Text(
                        text = stringResource(R.string.phase9_loading_placeholder),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                TestDetailScreenState.Empty -> {
                    Text(
                        text = stringResource(R.string.phase9_test_detail_empty_body),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.phase9_test_detail_empty_back_to_trends))
                    }
                }
                is TestDetailScreenState.Ready -> {
                    TestDetailReadyContent(
                        state = state,
                        perFeatureKeys = perFeatureKeys,
                        perFeatureLabels = perFeatureLabels,
                        includeSteadinessColumn = includeSteadinessColumn,
                        progressiveDisclosure = progressiveDisclosure,
                        perFeatureCaveats = perFeatureCaveats,
                        referenceBands = referenceBands,
                        overlayDisclaimerResId = overlayDisclaimerResId,
                        onAbout = onAbout
                    )
                }
            }
        }
    }
}

data class ProgressiveDisclosure(
    val primaryFeatureKey: String,
    val expanderLabelResId: Int
)

@Composable
private fun TestDetailReadyContent(
    state: TestDetailScreenState.Ready,
    perFeatureKeys: List<String>,
    perFeatureLabels: Map<String, String>,
    includeSteadinessColumn: Boolean,
    progressiveDisclosure: ProgressiveDisclosure?,
    perFeatureCaveats: Map<String, Int>,
    referenceBands: Map<String, ReferenceRange>,
    overlayDisclaimerResId: Int,
    onAbout: () -> Unit
) {
    Text(
        text = if (state.sessionCount == 1) {
            stringResource(R.string.phase9_test_detail_session_count_single)
        } else if (state.sessionsSinceLabel != null) {
            stringResource(
                R.string.phase9_test_detail_session_count_template,
                state.sessionCount,
                state.sessionsSinceLabel
            )
        } else {
            stringResource(
                R.string.phase9_test_detail_session_count_no_anchor,
                state.sessionCount
            )
        },
        style = MaterialTheme.typography.bodyMedium
    )
    state.latestQualityBand?.let { band ->
        val label = when (band) {
            QualityBand.HIGH -> stringResource(R.string.phase9_quality_chip_reliable)
            QualityBand.MEDIUM -> stringResource(R.string.phase9_quality_chip_mostly_reliable)
            QualityBand.LOW -> stringResource(R.string.phase9_quality_chip_less_reliable)
        }
        Text(
            text = stringResource(R.string.phase9_test_detail_last_session_prefix, label),
            style = MaterialTheme.typography.bodyMedium
        )
    }

    if (progressiveDisclosure != null) {
        val primary = state.featureSeries.firstOrNull { it.key == progressiveDisclosure.primaryFeatureKey }
            ?: state.featureSeries.firstOrNull()
        val others = state.featureSeries.filter { it.key != primary?.key }

        primary?.let {
            ChartWithFooter(
                series = it,
                displayLabel = perFeatureLabels[it.key] ?: it.displayName,
                caveatResId = perFeatureCaveats[it.key],
                referenceBand = referenceBands[it.key],
                overlayDisclaimerResId = overlayDisclaimerResId
            )
        }

        var expanded by remember { mutableStateOf(false) }
        TextButton(onClick = { expanded = !expanded }) {
            Text(stringResource(progressiveDisclosure.expanderLabelResId))
        }
        if (expanded) {
            others.forEach { series ->
                ChartWithFooter(
                    series = series,
                    displayLabel = perFeatureLabels[series.key] ?: series.displayName,
                    caveatResId = perFeatureCaveats[series.key],
                    referenceBand = referenceBands[series.key],
                    overlayDisclaimerResId = overlayDisclaimerResId
                )
            }
        }
    } else {
        state.featureSeries.forEach { series ->
            ChartWithFooter(
                series = series,
                displayLabel = perFeatureLabels[series.key] ?: series.displayName,
                caveatResId = perFeatureCaveats[series.key],
                referenceBand = referenceBands[series.key],
                overlayDisclaimerResId = overlayDisclaimerResId
            )
        }
    }

    if (state.summaryRows.isNotEmpty()) {
        SummaryTable(
            rows = state.summaryRows,
            perFeatureKeys = perFeatureKeys,
            perFeatureLabels = perFeatureLabels,
            includeSteadinessColumn = includeSteadinessColumn
        )
    }

    if (state.sessionCount < 2) {
        Text(
            text = stringResource(R.string.phase9_test_detail_run_another),
            style = MaterialTheme.typography.bodyMedium
        )
    }

    TextButton(onClick = onAbout) {
        Text(stringResource(R.string.phase9_test_detail_about_link))
    }
}

@Composable
private fun ChartWithFooter(
    series: FeatureSeries,
    displayLabel: String,
    caveatResId: Int?,
    referenceBand: ReferenceRange?,
    overlayDisclaimerResId: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TrendLineChart(
            series = series,
            isSparkline = false,
            referenceBand = referenceBand,
            displayLabel = displayLabel
        )
        PerChartSummaryLine(series = series, displayLabel = displayLabel)
        caveatResId?.let { res ->
            Text(
                text = stringResource(res),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val footerRes = if (referenceBand != null) {
            R.string.phase9_reference_band_givon_footnote
        } else {
            overlayDisclaimerResId
        }
        Text(
            text = stringResource(footerRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
