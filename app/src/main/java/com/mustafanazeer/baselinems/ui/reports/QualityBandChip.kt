package com.mustafanazeer.baselinems.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mustafanazeer.baselinems.R

private val ReliableBackground = Color(0xFFE6F4EA)
private val ReliableForeground = Color(0xFF0B5C2E)
private val MostlyReliableBackground = Color(0xFFFFF4E5)
private val MostlyReliableForeground = Color(0xFF6B4A00)
private val LessReliableBackground = Color(0xFFEEEEEE)
private val LessReliableForeground = Color(0xFF3D3D3D)

@Composable
fun QualityBandChip(
    band: QualityBand,
    modifier: Modifier = Modifier
) {
    val labelRes = when (band) {
        QualityBand.HIGH -> R.string.phase9_quality_chip_reliable
        QualityBand.MEDIUM -> R.string.phase9_quality_chip_mostly_reliable
        QualityBand.LOW -> R.string.phase9_quality_chip_less_reliable
    }
    val (background, foreground) = when (band) {
        QualityBand.HIGH -> ReliableBackground to ReliableForeground
        QualityBand.MEDIUM -> MostlyReliableBackground to MostlyReliableForeground
        QualityBand.LOW -> LessReliableBackground to LessReliableForeground
    }
    Text(
        text = stringResource(labelRes),
        style = MaterialTheme.typography.labelMedium,
        color = foreground,
        modifier = modifier
            .background(color = background, shape = RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}
