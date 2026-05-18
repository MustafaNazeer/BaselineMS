package com.mustafanazeer.baselinems.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mustafanazeer.baselinems.R

private const val DEFAULT_ROW_LIMIT: Int = 12

@Composable
fun SummaryTable(
    rows: List<SummaryRow>,
    perFeatureKeys: List<String>,
    perFeatureLabels: Map<String, String>,
    includeSteadinessColumn: Boolean = false,
    modifier: Modifier = Modifier
) {
    val emDash = stringResource(R.string.phase9_summary_table_em_dash_placeholder)
    var expanded by remember { mutableStateOf(false) }
    val visibleRows = if (rows.size <= DEFAULT_ROW_LIMIT || expanded) {
        rows
    } else {
        rows.takeLast(DEFAULT_ROW_LIMIT)
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.phase9_summary_table_column_date),
                style = MaterialTheme.typography.labelMedium
            )
            perFeatureKeys.forEach { key ->
                Text(
                    text = perFeatureLabels[key] ?: key,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            if (includeSteadinessColumn) {
                Text(
                    text = stringResource(R.string.phase9_summary_table_column_steadiness),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Text(
                text = stringResource(R.string.phase9_summary_table_column_quality),
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = stringResource(R.string.phase9_summary_table_column_notes),
                style = MaterialTheme.typography.labelMedium
            )
        }
        HorizontalDivider()
        visibleRows.forEach { row ->
            SummaryRowView(
                row = row,
                perFeatureKeys = perFeatureKeys,
                includeSteadinessColumn = includeSteadinessColumn,
                emDash = emDash
            )
            HorizontalDivider()
        }
        if (rows.size > DEFAULT_ROW_LIMIT && !expanded) {
            TextButton(onClick = { expanded = true }) {
                Text(stringResource(R.string.phase9_test_detail_show_all_sessions))
            }
        }
    }
}

@Composable
private fun SummaryRowView(
    row: SummaryRow,
    perFeatureKeys: List<String>,
    includeSteadinessColumn: Boolean,
    emDash: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = row.dateLabel,
                style = MaterialTheme.typography.bodyMedium
            )
            perFeatureKeys.forEach { key ->
                Text(
                    text = row.perFeatureValues[key] ?: emDash,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (includeSteadinessColumn) {
                Text(
                    text = row.steadinessBandLabel ?: emDash,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            QualityBandChip(band = row.qualityBand)
        }
        if (row.qualityBand == QualityBand.LOW && row.contextCellResId != null) {
            Text(
                text = stringResource(row.contextCellResId),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        row.sessionAnnotationResIds.forEach { resId ->
            Text(
                text = stringResource(resId),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        row.perFeatureAnnotations.values.forEach { annotation ->
            Text(
                text = annotation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        row.perFeatureAnnotationResIds.values.forEach { resId ->
            Text(
                text = stringResource(resId),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
