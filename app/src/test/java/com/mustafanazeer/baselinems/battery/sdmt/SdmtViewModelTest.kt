package com.mustafanazeer.baselinems.battery.sdmt

import com.mustafanazeer.baselinems.dsp.sdmt.SDMT_FIXED_KEY
import com.mustafanazeer.baselinems.dsp.sdmt.SdmtSymbol
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SdmtViewModelTest {

    private fun TestScope.newViewModel(
        sessionId: String = "session-a",
        showCountdown: Boolean = false,
        onTenSecondsRemaining: () -> Unit = {}
    ): SdmtViewModel {
        val scope = this
        return SdmtViewModel(
            sessionId = sessionId,
            scope = scope,
            showCountdown = showCountdown,
            onTenSecondsRemaining = onTenSecondsRemaining,
            clockMs = { scope.currentTime }
        )
    }

    private fun correctDigit(sym: SdmtSymbol): Int = SDMT_FIXED_KEY.getValue(sym)

    @Test
    fun `starts in Instructions state`() = runTest(UnconfinedTestDispatcher()) {
        val vm = newViewModel()
        assertTrue(vm.state.value is SdmtTestState.Instructions)
    }

    @Test
    fun `onStart transitions to Running with first prompt at elapsedMs=0`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel()
            vm.onStart()
            val s = vm.state.value
            assertTrue(s is SdmtTestState.Running)
            s as SdmtTestState.Running
            assertEquals(0, s.totalAttempted)
            assertEquals(0, s.totalCorrect)
            assertEquals(0, s.consecutiveErrors)
            assertEquals(0L, s.promptShownAtMs)
            assertEquals(0L, s.elapsedMs)
            assertFalse(s.showCountdown)
        }

    @Test
    fun `correct tap advances totalCorrect and totalAttempted and resets consecutiveErrors`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel()
            vm.onStart()
            val first = vm.state.value as SdmtTestState.Running
            advanceTimeBy(800)
            vm.onDigitTapped(correctDigit(first.currentPrompt))
            val second = vm.state.value as SdmtTestState.Running
            assertEquals(1, second.totalAttempted)
            assertEquals(1, second.totalCorrect)
            assertEquals(0, second.consecutiveErrors)
        }

    @Test
    fun `wrong tap increments consecutiveErrors without incrementing totalCorrect`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel()
            vm.onStart()
            val first = vm.state.value as SdmtTestState.Running
            val wrong = if (correctDigit(first.currentPrompt) == 1) 2 else 1
            advanceTimeBy(800)
            vm.onDigitTapped(wrong)
            val second = vm.state.value as SdmtTestState.Running
            assertEquals(1, second.totalAttempted)
            assertEquals(0, second.totalCorrect)
            assertEquals(1, second.consecutiveErrors)
        }

    @Test
    fun `three consecutive errors triggers the reassurance threshold`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel()
            vm.onStart()
            repeat(3) {
                val cur = vm.state.value as SdmtTestState.Running
                val wrong = if (correctDigit(cur.currentPrompt) == 1) 2 else 1
                advanceTimeBy(500)
                vm.onDigitTapped(wrong)
            }
            val s = vm.state.value as SdmtTestState.Running
            assertEquals(3, s.consecutiveErrors)
            assertTrue(s.consecutiveErrors >= SdmtViewModel.MID_TEST_REASSURANCE_THRESHOLD)
        }

    @Test
    fun `slow correct tap does not raise consecutiveErrors`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel()
            vm.onStart()
            val first = vm.state.value as SdmtTestState.Running
            advanceTimeBy(5_000)
            vm.onDigitTapped(correctDigit(first.currentPrompt))
            val s = vm.state.value as SdmtTestState.Running
            assertEquals(0, s.consecutiveErrors)
        }

    @Test
    fun `90 second timer fires Done with no taps when user does not respond`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel()
            vm.onStart()
            advanceTimeBy(90_500L)
            runCurrent()
            val s = vm.state.value
            assertTrue("expected Done, was ${s::class.simpleName}", s is SdmtTestState.Done)
        }

    @Test
    fun `cancel from Running transitions to Cancelled`() = runTest(UnconfinedTestDispatcher()) {
        val vm = newViewModel()
        vm.onStart()
        vm.onCancel()
        assertTrue(vm.state.value is SdmtTestState.Cancelled)
    }

    @Test
    fun `audible ping fires at 80 seconds`() = runTest(UnconfinedTestDispatcher()) {
        var pingCount = 0
        val vm = newViewModel(onTenSecondsRemaining = { pingCount += 1 })
        vm.onStart()
        advanceTimeBy(79_500L)
        runCurrent()
        assertEquals(0, pingCount)
        advanceTimeBy(1_000L)
        runCurrent()
        assertEquals(1, pingCount)
    }

    @Test
    fun `audible ping fires exactly once`() = runTest(UnconfinedTestDispatcher()) {
        var pingCount = 0
        val vm = newViewModel(onTenSecondsRemaining = { pingCount += 1 })
        vm.onStart()
        advanceTimeBy(89_000L)
        runCurrent()
        assertEquals(1, pingCount)
    }

    @Test
    fun `same session id produces same symbol sequence`() {
        val seqA = mutableListOf<SdmtSymbol>()
        val seqB = mutableListOf<SdmtSymbol>()
        runTest(UnconfinedTestDispatcher()) {
            val a = newViewModel(sessionId = "deterministic-seed")
            a.onStart()
            repeat(20) {
                val cur = a.state.value as SdmtTestState.Running
                seqA += cur.currentPrompt
                advanceTimeBy(500)
                a.onDigitTapped(correctDigit(cur.currentPrompt))
            }
            a.onCancel()
        }
        runTest(UnconfinedTestDispatcher()) {
            val b = newViewModel(sessionId = "deterministic-seed")
            b.onStart()
            repeat(20) {
                val cur = b.state.value as SdmtTestState.Running
                seqB += cur.currentPrompt
                advanceTimeBy(500)
                b.onDigitTapped(correctDigit(cur.currentPrompt))
            }
            b.onCancel()
        }
        assertEquals(seqA, seqB)
    }

    @Test
    fun `different session ids produce different symbol sequences`() {
        val seqA = mutableListOf<SdmtSymbol>()
        val seqB = mutableListOf<SdmtSymbol>()
        runTest(UnconfinedTestDispatcher()) {
            val a = newViewModel(sessionId = "seed-alpha")
            a.onStart()
            repeat(20) {
                val cur = a.state.value as SdmtTestState.Running
                seqA += cur.currentPrompt
                advanceTimeBy(500)
                a.onDigitTapped(correctDigit(cur.currentPrompt))
            }
            a.onCancel()
        }
        runTest(UnconfinedTestDispatcher()) {
            val b = newViewModel(sessionId = "seed-beta")
            b.onStart()
            repeat(20) {
                val cur = b.state.value as SdmtTestState.Running
                seqB += cur.currentPrompt
                advanceTimeBy(500)
                b.onDigitTapped(correctDigit(cur.currentPrompt))
            }
            b.onCancel()
        }
        assertFalse("two distinct seeds should usually produce distinct sequences", seqA == seqB)
    }

    @Test
    fun `immediate repeat constraint is honored across 200 prompts`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel(sessionId = "repeat-check")
            vm.onStart()
            var prev: SdmtSymbol? = null
            repeat(200) {
                val cur = vm.state.value as SdmtTestState.Running
                if (prev != null) {
                    assertNotNull(cur.currentPrompt)
                    assertFalse(
                        "immediate repeat at iteration $it: $prev followed by ${cur.currentPrompt}",
                        cur.currentPrompt == prev
                    )
                }
                prev = cur.currentPrompt
                advanceTimeBy(100)
                vm.onDigitTapped(correctDigit(cur.currentPrompt))
            }
            vm.onCancel()
        }

    @Test
    fun `showCountdown flag is forwarded to Running state`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel(showCountdown = true)
            vm.onStart()
            val s = vm.state.value as SdmtTestState.Running
            assertTrue(s.showCountdown)
        }

    @Test
    fun `done score reflects total correct after 30 correct taps spread over 90 seconds`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = newViewModel()
            vm.onStart()
            repeat(30) {
                val cur = vm.state.value as SdmtTestState.Running
                advanceTimeBy(2_900L)
                vm.onDigitTapped(correctDigit(cur.currentPrompt))
            }
            advanceTimeBy(5_000L)
            advanceUntilIdle()
            val s = vm.state.value as SdmtTestState.Done
            assertEquals(30, s.score.totalCorrect)
        }
}
