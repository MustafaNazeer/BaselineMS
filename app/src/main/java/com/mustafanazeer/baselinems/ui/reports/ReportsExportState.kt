package com.mustafanazeer.baselinems.ui.reports

import android.net.Uri

sealed interface ReportsExportState {
    data object Idle : ReportsExportState
    data object Rendering : ReportsExportState
    data class Ready(val pdfUri: Uri, val csvUri: Uri) : ReportsExportState
    data class Error(val messageResId: Int) : ReportsExportState
}
