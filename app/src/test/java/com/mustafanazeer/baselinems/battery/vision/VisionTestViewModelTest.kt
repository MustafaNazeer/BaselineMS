package com.mustafanazeer.baselinems.battery.vision

import com.mustafanazeer.baselinems.dsp.vision.SloanScoring.ContrastLevel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VisionTestViewModelTest {

    @Test
    fun `starts in Instructions state`() = runTest {
        val vm = VisionTestViewModel(sessionId = "test-session-1")
        assertTrue(vm.state.value is VisionTestState.Instructions)
    }

    @Test
    fun `onStart transitions to QualityCheck with both gates closed`() = runTest {
        val vm = VisionTestViewModel(sessionId = "test-session-1")
        vm.onStart()
        val s = vm.state.value
        assertTrue(s is VisionTestState.QualityCheck)
        s as VisionTestState.QualityCheck
        assertEquals(null, s.lux)
        assertEquals(null, s.distanceCm)
        assertEquals(false, s.luxOk)
        assertEquals(false, s.distanceOk)
    }

    @Test
    fun `lux and distance updates flip OK flags inside ranges`() = runTest {
        val vm = VisionTestViewModel(sessionId = "test-session-1")
        vm.onStart()

        vm.onLuxEstimated(200.0)
        vm.onDistanceEstimated(40.0)
        val s = vm.state.value as VisionTestState.QualityCheck
        assertTrue(s.luxOk)
        assertTrue(s.distanceOk)
    }

    @Test
    fun `out-of-range lux keeps gate closed`() = runTest {
        val vm = VisionTestViewModel(sessionId = "test-session-1")
        vm.onStart()
        vm.onLuxEstimated(5.0)
        vm.onDistanceEstimated(40.0)
        val s = vm.state.value as VisionTestState.QualityCheck
        assertEquals(false, s.luxOk)
    }

    @Test
    fun `proceeding from QualityCheck enters Running at C100 contrast`() = runTest {
        val vm = VisionTestViewModel(sessionId = "test-session-1")
        vm.onStart()
        vm.onLuxEstimated(300.0)
        vm.onDistanceEstimated(40.0)
        vm.onQualityCheckPassed()

        val s = vm.state.value as VisionTestState.Running
        assertEquals(ContrastLevel.C100, s.contrast)
        assertEquals(0, s.lineIndex)
        assertEquals(0, s.letterIndex)
        assertEquals(4, s.multipleChoiceLetters.size)
        assertTrue(s.currentLetter in s.multipleChoiceLetters)
    }

    @Test
    fun `correct tap advances to next letter on the same line`() = runTest {
        val vm = VisionTestViewModel(sessionId = "test-session-1")
        vm.onStart()
        vm.onLuxEstimated(300.0)
        vm.onDistanceEstimated(40.0)
        vm.onQualityCheckPassed()

        val first = vm.state.value as VisionTestState.Running
        vm.onLetterTapped(first.currentLetter)
        val second = vm.state.value as VisionTestState.Running
        assertEquals(ContrastLevel.C100, second.contrast)
        assertEquals(1, second.letterIndex)
    }

    @Test
    fun `completing all 3 charts transitions to Done with score 120`() = runTest {
        val vm = VisionTestViewModel(sessionId = "test-session-1")
        vm.onStart()
        vm.onLuxEstimated(300.0)
        vm.onDistanceEstimated(40.0)
        vm.onQualityCheckPassed()

        repeat(120) {
            val cur = vm.state.value as VisionTestState.Running
            vm.onLetterTapped(cur.currentLetter)
        }

        val s = vm.state.value as VisionTestState.Done
        assertEquals(120, s.score.totalCorrect)
    }

    @Test
    fun `cancel from Running transitions to Cancelled`() = runTest {
        val vm = VisionTestViewModel(sessionId = "test-session-1")
        vm.onStart()
        vm.onLuxEstimated(300.0)
        vm.onDistanceEstimated(40.0)
        vm.onQualityCheckPassed()
        vm.onCancel()
        assertTrue(vm.state.value is VisionTestState.Cancelled)
    }

    @Test
    fun `same session id produces same letter sequence (determinism)`() = runTest {
        val a = VisionTestViewModel(sessionId = "deterministic-seed")
        a.onStart(); a.onLuxEstimated(300.0); a.onDistanceEstimated(40.0); a.onQualityCheckPassed()
        val b = VisionTestViewModel(sessionId = "deterministic-seed")
        b.onStart(); b.onLuxEstimated(300.0); b.onDistanceEstimated(40.0); b.onQualityCheckPassed()

        val seqA = mutableListOf<Char>()
        val seqB = mutableListOf<Char>()
        repeat(40) {
            val sa = a.state.value as VisionTestState.Running
            val sb = b.state.value as VisionTestState.Running
            seqA += sa.currentLetter
            seqB += sb.currentLetter
            a.onLetterTapped(sa.currentLetter)
            b.onLetterTapped(sb.currentLetter)
        }
        assertEquals(seqA, seqB)
    }

    @Test
    fun `four consecutive skips raise consecutiveSkipCount to 4`() = runTest {
        val vm = VisionTestViewModel(sessionId = "test-session-1")
        vm.onStart()
        vm.onLuxEstimated(300.0)
        vm.onDistanceEstimated(40.0)
        vm.onQualityCheckPassed()

        repeat(4) { vm.onLetterTapped(null) }
        val s = vm.state.value as VisionTestState.Running
        assertEquals(4, s.consecutiveSkipCount)
    }

    @Test
    fun `non-skip tap resets consecutiveSkipCount`() = runTest {
        val vm = VisionTestViewModel(sessionId = "test-session-1")
        vm.onStart()
        vm.onLuxEstimated(300.0)
        vm.onDistanceEstimated(40.0)
        vm.onQualityCheckPassed()

        repeat(3) { vm.onLetterTapped(null) }
        val cur = vm.state.value as VisionTestState.Running
        vm.onLetterTapped(cur.currentLetter)
        val after = vm.state.value as VisionTestState.Running
        assertEquals(0, after.consecutiveSkipCount)
    }
}
