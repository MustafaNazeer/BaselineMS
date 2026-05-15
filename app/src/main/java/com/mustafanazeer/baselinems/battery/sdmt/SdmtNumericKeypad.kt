package com.mustafanazeer.baselinems.battery.sdmt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SdmtNumericKeypad(
    onDigitTap: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf(1, 2, 3),
        listOf(4, 5, 6),
        listOf(7, 8, 9)
    )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                row.forEach { digit ->
                    Button(
                        onClick = { onDigitTap(digit) },
                        modifier = Modifier.size(96.dp)
                    ) {
                        Text(
                            text = digit.toString(),
                            style = MaterialTheme.typography.displaySmall
                        )
                    }
                }
            }
        }
    }
}
