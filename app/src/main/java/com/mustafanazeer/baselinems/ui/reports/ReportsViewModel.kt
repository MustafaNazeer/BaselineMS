package com.mustafanazeer.baselinems.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mustafanazeer.baselinems.data.TestType
import com.mustafanazeer.baselinems.data.TrendsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ReportsViewModel(
    private val repository: TrendsRepository,
    scope: CoroutineScope? = null
) : ViewModel() {

    private val activeScope: CoroutineScope = scope ?: viewModelScope

    val reportsState: StateFlow<ReportsScreenState> = repository.observeReportsState()
        .stateIn(
            scope = activeScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = ReportsScreenState.Loading
        )

    fun observeTestDetail(testType: TestType): StateFlow<TestDetailScreenState> {
        return repository.observeTestDetailState(testType)
            .stateIn(
                scope = activeScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = TestDetailScreenState.Loading
            )
    }

    companion object {
        private const val STOP_TIMEOUT_MS: Long = 5_000L
    }
}
