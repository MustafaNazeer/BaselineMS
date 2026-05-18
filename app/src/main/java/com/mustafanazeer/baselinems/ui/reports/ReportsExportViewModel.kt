package com.mustafanazeer.baselinems.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mustafanazeer.baselinems.R
import com.mustafanazeer.baselinems.report.ReportExporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReportsExportViewModel(
    private val exporter: ReportExporter
) : ViewModel() {

    private val _state = MutableStateFlow<ReportsExportState>(ReportsExportState.Idle)
    val state: StateFlow<ReportsExportState> = _state.asStateFlow()

    fun onShareClicked() {
        _state.value = ReportsExportState.Rendering
        viewModelScope.launch {
            try {
                val result = exporter.export()
                _state.value = ReportsExportState.Ready(result.pdfUri, result.csvUri)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                _state.value = ReportsExportState.Error(R.string.phase10_export_error_generic)
            }
        }
    }

    fun onShareConsumed() { _state.value = ReportsExportState.Idle }
}
