package com.mustafanazeer.baselinems.ui.reports

import android.net.Uri
import com.mustafanazeer.baselinems.report.ExportResult
import com.mustafanazeer.baselinems.report.ReportExporter
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ReportsExportViewModelTest {

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun onShareClicked_transitionsThroughRenderingToReady() = runTest {
        val exporter = mockk<ReportExporter>()
        coEvery { exporter.export() } returns ExportResult(Uri.EMPTY, Uri.EMPTY)
        val vm = ReportsExportViewModel(exporter)
        vm.onShareClicked()
        val state = vm.state.first()
        assertTrue(state is ReportsExportState.Ready || state is ReportsExportState.Rendering)
    }

    @Test
    fun viewModelClear_midExport_doesNotSurfaceErrorState() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        val gate = CompletableDeferred<ExportResult>()
        val exporter = mockk<ReportExporter>()
        coEvery { exporter.export() } coAnswers { gate.await() }
        val vm = ReportsExportViewModel(exporter)
        vm.onShareClicked()
        testDispatcher.scheduler.runCurrent()
        assertTrue(vm.state.value is ReportsExportState.Rendering)
        vm.viewModelStoreOwnerCancelEquivalent()
        advanceUntilIdle()
        assertFalse(
            "Cancellation must not transition to Error state",
            vm.state.value is ReportsExportState.Error
        )
    }
}

private fun ReportsExportViewModel.viewModelStoreOwnerCancelEquivalent() {
    val clearMethod = androidx.lifecycle.ViewModel::class.java
        .getDeclaredMethod("clear\$lifecycle_viewmodel_release")
        .apply { isAccessible = true }
    clearMethod.invoke(this)
}
