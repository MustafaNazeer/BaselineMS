package com.mustafanazeer.baselinems.battery.sdmt

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mustafanazeer.baselinems.dsp.sdmt.SDMT_FIXED_KEY
import com.mustafanazeer.baselinems.dsp.sdmt.SdmtSymbol

@Composable
fun SdmtSymbolKey(
    key: Map<SdmtSymbol, Int> = SDMT_FIXED_KEY,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SdmtSymbol.entries.forEach { sym ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = sym.drawableRes),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = key.getValue(sym).toString(),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
