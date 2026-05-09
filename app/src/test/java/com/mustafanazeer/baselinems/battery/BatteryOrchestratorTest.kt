package com.mustafanazeer.baselinems.battery

import com.mustafanazeer.baselinems.data.SessionDao
import com.mustafanazeer.baselinems.data.SessionEntity
import com.mustafanazeer.baselinems.data.TestResultDao
import com.mustafanazeer.baselinems.data.TestResultEntity
import com.mustafanazeer.baselinems.data.TestType
import com.mustafanazeer.baselinems.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private class FakeSessionDao : SessionDao {
    val sessions = mutableListOf<SessionEntity>()
    override suspend fun insert(session: SessionEntity) { sessions.add(session) }
    override suspend fun update(session: SessionEntity) {
        val i = sessions.indexOfFirst { it.id == session.id }
        if (i >= 0) sessions[i] = session
    }
    override suspend fun delete(session: SessionEntity) { sessions.removeAll { it.id == session.id } }
    override fun observeAll(): Flow<List<SessionEntity>> = flowOf(sessions.toList())
    override suspend fun getById(id: String): SessionEntity? = sessions.find { it.id == id }
}

private class FakeTestResultDao : TestResultDao {
    val results = mutableListOf<TestResultEntity>()
    override suspend fun insert(result: TestResultEntity) { results.add(result) }
    override suspend fun getForSession(sessionId: String): List<TestResultEntity> =
        results.filter { it.sessionId == sessionId }.sortedBy { it.startedAtEpochMs }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BatteryOrchestratorTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialStateIsIdle() {
        val orchestrator = BatteryOrchestrator(
            modules = listOf(MockTestModule()),
            sessionDao = FakeSessionDao(),
            testResultDao = FakeTestResultDao(),
            deviceInfo = "test"
        )
        assertEquals(BatteryOrchestrator.State.Idle, orchestrator.state.value)
    }

    @Test
    fun startTransitionsToRunningAndCreatesSession() = runTest {
        val sessionDao = FakeSessionDao()
        val orchestrator = BatteryOrchestrator(
            modules = listOf(MockTestModule()),
            sessionDao = sessionDao,
            testResultDao = FakeTestResultDao(),
            deviceInfo = "Pixel"
        )
        orchestrator.start()
        advanceUntilIdle()

        val s = orchestrator.state.value
        assertTrue("Expected Running, got $s", s is BatteryOrchestrator.State.Running)
        assertEquals(0, (s as BatteryOrchestrator.State.Running).index)

        assertEquals(1, sessionDao.sessions.size)
        assertEquals("Pixel", sessionDao.sessions.first().deviceInfo)
        assertNull(sessionDao.sessions.first().completedAtEpochMs)
    }

    @Test
    fun completingAllModulesPersistsResultsAndCompletesSession() = runTest {
        val sessionDao = FakeSessionDao()
        val resultDao = FakeTestResultDao()
        val orchestrator = BatteryOrchestrator(
            modules = listOf(MockTestModule(), MockTestModule()),
            sessionDao = sessionDao,
            testResultDao = resultDao,
            deviceInfo = "Pixel"
        )
        orchestrator.start(); advanceUntilIdle()
        orchestrator.recordResult(
            testType = TestType.TAP,
            payload = MockTestModule.MockResult(qualityScore = 0.9, features = mapOf("a" to 1.0))
        )
        advanceUntilIdle()
        orchestrator.recordResult(
            testType = TestType.GAIT,
            payload = MockTestModule.MockResult(qualityScore = 0.8, features = mapOf("b" to 2.0))
        )
        advanceUntilIdle()

        assertEquals(BatteryOrchestrator.State.Completed, orchestrator.state.value)
        assertEquals(1, sessionDao.sessions.size)
        assertNotNull(sessionDao.sessions.first().completedAtEpochMs)
        assertEquals(2, resultDao.results.size)
        val types = resultDao.results.map { it.testType }.sortedBy { it.name }
        assertEquals(listOf(TestType.GAIT, TestType.TAP), types)
    }

    @Test
    fun cancelDiscardsActiveSession() = runTest {
        val sessionDao = FakeSessionDao()
        val orchestrator = BatteryOrchestrator(
            modules = listOf(MockTestModule()),
            sessionDao = sessionDao,
            testResultDao = FakeTestResultDao(),
            deviceInfo = "Pixel"
        )
        orchestrator.start(); advanceUntilIdle()
        orchestrator.cancel(); advanceUntilIdle()

        assertEquals(BatteryOrchestrator.State.Idle, orchestrator.state.value)
        assertEquals(0, sessionDao.sessions.size)
    }
}
