package com.mustafanazeer.baselinems.ui.reports

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.mustafanazeer.baselinems.data.TestType
import com.mustafanazeer.baselinems.data.TrendsRepository

@Composable
fun ReportsRoute(
    repository: TrendsRepository,
    exportViewModel: ReportsExportViewModel,
    onBack: () -> Unit,
    onCardSelected: (TestType) -> Unit,
    onRunFirstCheckIn: () -> Unit
) {
    val viewModel = remember(repository) { ReportsViewModel(repository = repository) }
    val state by viewModel.reportsState.collectAsState()
    val exportState by exportViewModel.state.collectAsState()
    ReportsScreen(
        state = state,
        exportState = exportState,
        onBack = onBack,
        onCardSelected = onCardSelected,
        onRunFirstCheckIn = onRunFirstCheckIn,
        onShareClicked = { exportViewModel.onShareClicked() },
        onShareConsumed = { exportViewModel.onShareConsumed() }
    )
}

@Composable
fun TestDetailRoute(
    repository: TrendsRepository,
    testType: TestType,
    onBack: () -> Unit,
    onAbout: () -> Unit
) {
    val viewModel = remember(repository) { ReportsViewModel(repository = repository) }
    val state by viewModel.observeTestDetail(testType).collectAsState()
    when (testType) {
        TestType.TAP -> TapDetailScreen(state = state, onBack = onBack, onAbout = onAbout)
        TestType.GAIT -> GaitDetailScreen(state = state, onBack = onBack, onAbout = onAbout)
        TestType.VISION -> VisionDetailScreen(state = state, onBack = onBack, onAbout = onAbout)
        TestType.SDMT -> SdmtDetailScreen(state = state, onBack = onBack, onAbout = onAbout)
        TestType.VOICE -> VoiceDetailScreen(state = state, onBack = onBack, onAbout = onAbout)
    }
}
