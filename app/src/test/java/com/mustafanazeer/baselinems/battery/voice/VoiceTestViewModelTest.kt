package com.mustafanazeer.baselinems.battery.voice

import com.mustafanazeer.baselinems.signals.AudioCapture
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceTestViewModelTest {

    private class FakeAudioCapture(
        private val fixture: ShortArray = ShortArray(44_100 * 30),
        private val throwSecurity: Boolean = false,
        private val throwIllegalState: Boolean = false,
        private val suspendForever: Boolean = false
    ) : AudioCapture {
        var recordCallCount: Int = 0
            private set
        var lastDurationSec: Int = -1
            private set

        override suspend fun record(durationSec: Int): ShortArray {
            recordCallCount += 1
            lastDurationSec = durationSec
            if (throwSecurity) throw SecurityException("denied")
            if (throwIllegalState) throw IllegalStateException("hal failed")
            if (suspendForever) {
                kotlinx.coroutines.delay(Long.MAX_VALUE)
            }
            return fixture
        }
    }

    private fun TestScope.newViewModel(
        capture: AudioCapture = FakeAudioCapture()
    ): VoiceTestViewModel {
        return VoiceTestViewModel(
            audioCapture = capture,
            scope = this,
            clockMs = { currentTime }
        )
    }

    @Test
    fun `starts in Instructions state`() = runTest(UnconfinedTestDispatcher()) {
        val vm = newViewModel()
        assertTrue(vm.state.value is VoiceTestState.Instructions)
    }

    @Test
    fun `onStart transitions to RecordAudioRequested`() = runTest(UnconfinedTestDispatcher()) {
        val vm = newViewModel()
        vm.onStart()
        assertTrue(vm.state.value is VoiceTestState.RecordAudioRequested)
    }

    @Test
    fun `permission granted routes to AudioQualityCheck Pending band`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel()
            vm.onStart()
            vm.onPermissionResult(granted = true)
            val s = vm.state.value as VoiceTestState.AudioQualityCheck
            assertEquals(NoiseBand.Pending, s.band)
            assertNull(s.noiseFloorRmsDbFs)
        }

    @Test
    fun `permission denied routes to RecordAudioDenied`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel()
            vm.onStart()
            vm.onPermissionResult(granted = false)
            assertTrue(vm.state.value is VoiceTestState.RecordAudioDenied)
        }

    @Test
    fun `denied path does not re request in the same session`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel()
            vm.onStart()
            vm.onPermissionResult(granted = false)
            assertTrue(vm.state.value is VoiceTestState.RecordAudioDenied)
            vm.onStart()
            assertTrue(vm.state.value is VoiceTestState.RecordAudioDenied)
        }

    @Test
    fun `noise floor below green threshold yields green band`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel()
            vm.onStart(); vm.onPermissionResult(granted = true)
            vm.onNoiseFloorMeasured(-60.0)
            val s = vm.state.value as VoiceTestState.AudioQualityCheck
            assertEquals(NoiseBand.Green, s.band)
        }

    @Test
    fun `noise floor in yellow band yields yellow`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel()
            vm.onStart(); vm.onPermissionResult(granted = true)
            vm.onNoiseFloorMeasured(-40.0)
            val s = vm.state.value as VoiceTestState.AudioQualityCheck
            assertEquals(NoiseBand.Yellow, s.band)
        }

    @Test
    fun `noise floor above yellow threshold yields red`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel()
            vm.onStart(); vm.onPermissionResult(granted = true)
            vm.onNoiseFloorMeasured(-20.0)
            val s = vm.state.value as VoiceTestState.AudioQualityCheck
            assertEquals(NoiseBand.Red, s.band)
        }

    @Test
    fun `red band blocks onQualityCheckPassed`() = runTest(UnconfinedTestDispatcher()) {
        val vm = newViewModel()
        vm.onStart(); vm.onPermissionResult(granted = true)
        vm.onNoiseFloorMeasured(-10.0)
        vm.onQualityCheckPassed()
        assertTrue(vm.state.value is VoiceTestState.AudioQualityCheck)
    }

    @Test
    fun `green band onQualityCheckPassed transitions to Running and produces Done`() =
        runTest(UnconfinedTestDispatcher()) {
            val capture = FakeAudioCapture()
            val vm = newViewModel(capture)
            vm.onStart(); vm.onPermissionResult(granted = true)
            vm.onNoiseFloorMeasured(-60.0)
            vm.onQualityCheckPassed()
            advanceUntilIdle()
            val s = vm.state.value
            assertTrue("expected Done, was ${s::class.simpleName}", s is VoiceTestState.Done)
            assertEquals(1, capture.recordCallCount)
            assertEquals(30, capture.lastDurationSec)
        }

    @Test
    fun `cancel from Running transitions to Cancelled`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel(FakeAudioCapture(suspendForever = true))
            vm.onStart(); vm.onPermissionResult(granted = true)
            vm.onNoiseFloorMeasured(-60.0)
            vm.onQualityCheckPassed()
            assertTrue(vm.state.value is VoiceTestState.Running)
            vm.onCancel()
            advanceUntilIdle()
            assertTrue(vm.state.value is VoiceTestState.Cancelled)
        }

    @Test
    fun `SecurityException from AudioCapture maps to RecordAudioDenied`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel(FakeAudioCapture(throwSecurity = true))
            vm.onStart(); vm.onPermissionResult(granted = true)
            vm.onNoiseFloorMeasured(-60.0)
            vm.onQualityCheckPassed()
            advanceUntilIdle()
            assertTrue(vm.state.value is VoiceTestState.RecordAudioDenied)
        }

    @Test
    fun `IllegalStateException from AudioCapture maps to Cancelled`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel(FakeAudioCapture(throwIllegalState = true))
            vm.onStart(); vm.onPermissionResult(granted = true)
            vm.onNoiseFloorMeasured(-60.0)
            vm.onQualityCheckPassed()
            advanceUntilIdle()
            assertTrue(vm.state.value is VoiceTestState.Cancelled)
        }

    @Test
    fun `capture buffer is zeroed after a successful Done transition`() =
        runTest(UnconfinedTestDispatcher()) {
            val shared = ShortArray(44_100 * 30) { (it % 100).toShort() }
            val capture = FakeAudioCapture(fixture = shared)
            val vm = newViewModel(capture)
            vm.onStart(); vm.onPermissionResult(granted = true)
            vm.onNoiseFloorMeasured(-60.0)
            vm.onQualityCheckPassed()
            advanceUntilIdle()
            assertTrue(vm.state.value is VoiceTestState.Done)
            assertTrue(
                "buffer not zeroed after Done; first nonzero index = " +
                    shared.indexOfFirst { it != 0.toShort() },
                shared.all { it == 0.toShort() }
            )
        }

    @Test
    fun `capture buffer is zeroed after a SecurityException`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel(FakeAudioCapture(throwSecurity = true))
            vm.onStart(); vm.onPermissionResult(granted = true)
            vm.onNoiseFloorMeasured(-60.0)
            vm.onQualityCheckPassed()
            advanceUntilIdle()
            assertTrue(vm.state.value is VoiceTestState.RecordAudioDenied)
        }

    @Test
    fun `permission result with no pending request is a no op`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel()
            vm.onPermissionResult(granted = true)
            assertTrue(vm.state.value is VoiceTestState.Instructions)
        }

    @Test
    fun `start is a no op outside Instructions`() = runTest(UnconfinedTestDispatcher()) {
        val vm = newViewModel()
        vm.onStart()
        val before = vm.state.value
        vm.onStart()
        assertEquals(before, vm.state.value)
    }
}
