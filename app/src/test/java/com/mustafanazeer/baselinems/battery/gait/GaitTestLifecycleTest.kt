package com.mustafanazeer.baselinems.battery.gait

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mustafanazeer.baselinems.battery.BatteryOrchestrator
import com.mustafanazeer.baselinems.battery.MockTestModule
import com.mustafanazeer.baselinems.data.AppDatabase
import com.mustafanazeer.baselinems.data.SessionEntity
import com.mustafanazeer.baselinems.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.concurrent.Executor

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class GaitTestLifecycleTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val synchronousExecutor = Executor { it.run() }
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .setQueryExecutor(synchronousExecutor)
            .setTransactionExecutor(synchronousExecutor)
            .build()
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun `cancelSession marks the active session cancelled and preserves the row`() = runTest {
        val orchestrator = BatteryOrchestrator(
            modules = listOf(MockTestModule(), MockTestModule()),
            sessionDao = db.sessionDao(),
            testResultDao = db.testResultDao(),
            deviceInfo = "Lifecycle Pixel"
        )
        orchestrator.start(); advanceUntilIdle()
        assertTrue(orchestrator.state.value is BatteryOrchestrator.State.Running)
        val sessionId = orchestrator.activeSessionId
        assertNotNull(sessionId)

        orchestrator.cancelSession(); advanceUntilIdle()

        assertEquals(BatteryOrchestrator.State.Idle, orchestrator.state.value)
        val persisted = db.sessionDao().getById(sessionId!!)
        assertNotNull("Row must be preserved after a stranded back gesture cancel", persisted)
        assertNotNull(
            "completedAtEpochMs must be set so the row no longer shows as In progress",
            persisted!!.completedAtEpochMs
        )
        assertTrue(
            "wasCancelled must be true so the home screen renders Cancelled, not Completed",
            persisted.wasCancelled
        )
    }

    @Test
    fun `cancelSession is a no op when no session is active`() = runTest {
        val orchestrator = BatteryOrchestrator(
            modules = listOf(MockTestModule()),
            sessionDao = db.sessionDao(),
            testResultDao = db.testResultDao(),
            deviceInfo = "Lifecycle Pixel"
        )
        orchestrator.cancelSession(); advanceUntilIdle()
        assertEquals(BatteryOrchestrator.State.Idle, orchestrator.state.value)
        assertEquals(0, db.sessionDao().observeAll().first().size)
    }

    @Test
    fun `cancelled sessions do not inflate the completed session count`() = runTest {
        val orchestrator = BatteryOrchestrator(
            modules = listOf(MockTestModule()),
            sessionDao = db.sessionDao(),
            testResultDao = db.testResultDao(),
            deviceInfo = "Lifecycle Pixel"
        )
        orchestrator.start(); advanceUntilIdle()
        orchestrator.cancelSession(); advanceUntilIdle()

        assertEquals(
            "Completed count must exclude cancelled sessions",
            0,
            db.sessionDao().observeCompletedSessionCount().first()
        )
    }

    @Test
    fun `reclaimStrandedSessions marks a stranded row cancelled`() = runTest {
        val started = 1_000_000L
        val now = started + 120_000L

        val stranded = SessionEntity(
            startedAtEpochMs = started,
            completedAtEpochMs = null,
            deviceInfo = "Stranded Pixel"
        )
        db.sessionDao().insert(stranded)

        val updated = db.sessionDao().reclaimStrandedSessions(
            nowEpochMs = now,
            strandedBeforeEpochMs = now - 60_000L
        )

        assertEquals(1, updated)
        val row = db.sessionDao().getById(stranded.id)
        assertNotNull(row)
        assertEquals(now, row!!.completedAtEpochMs)
        assertTrue(row.wasCancelled)
    }

    @Test
    fun `reclaimStrandedSessions leaves fresh in flight rows alone`() = runTest {
        val now = 1_500_000L
        val fresh = SessionEntity(
            startedAtEpochMs = now - 5_000L,
            completedAtEpochMs = null,
            deviceInfo = "Fresh Pixel"
        )
        db.sessionDao().insert(fresh)

        val updated = db.sessionDao().reclaimStrandedSessions(
            nowEpochMs = now,
            strandedBeforeEpochMs = now - 60_000L
        )

        assertEquals(0, updated)
        val row = db.sessionDao().getById(fresh.id)
        assertNotNull(row)
        assertNull(
            "A session started within the stranded threshold must stay in progress",
            row!!.completedAtEpochMs
        )
        assertEquals(false, row.wasCancelled)
    }

    @Test
    fun `reclaimStrandedSessions leaves completed rows untouched`() = runTest {
        val now = 2_000_000L
        val completed = SessionEntity(
            startedAtEpochMs = now - 120_000L,
            completedAtEpochMs = now - 60_000L,
            deviceInfo = "Completed Pixel"
        )
        db.sessionDao().insert(completed)

        val updated = db.sessionDao().reclaimStrandedSessions(
            nowEpochMs = now,
            strandedBeforeEpochMs = now - 60_000L
        )

        assertEquals(0, updated)
        val row = db.sessionDao().getById(completed.id)
        assertNotNull(row)
        assertEquals(now - 60_000L, row!!.completedAtEpochMs)
        assertEquals(false, row.wasCancelled)
    }
}
