package com.mustafanazeer.baselinems.ui.reports

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.mustafanazeer.baselinems.R

@Composable
fun ShareReportLauncher(state: ReportsExportState, onLaunched: () -> Unit) {
    val context = LocalContext.current
    val chooserTitle = stringResource(R.string.phase10_share_chooser_title)
    LaunchedEffect(state) {
        if (state is ReportsExportState.Ready) {
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "application/octet-stream"
                putParcelableArrayListExtra(
                    Intent.EXTRA_STREAM,
                    arrayListOf(state.pdfUri, state.csvUri)
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ContextCompat.startActivity(
                context,
                Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                null
            )
            onLaunched()
        }
    }
}
