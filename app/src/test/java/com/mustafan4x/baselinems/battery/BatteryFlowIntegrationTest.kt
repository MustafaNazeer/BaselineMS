package com.mustafan4x.baselinems.battery

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mustafan4x.baselinems.data.AppDatabase
import com.mustafan4x.baselinems.data.TestType
import com.mustafan4x.baselinems.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
class BatteryFlowIntegrationTest {

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
    fun endToEndBatteryRunPersistsCompleteSession() = runTest {
        val modules = listOf(MockTestModule(), MockTestModule(), MockTestModule())
        val orchestrator = BatteryOrchestrator(
            modules = modules,
            sessionDao = db.sessionDao(),
            testResultDao = db.testResultDao(),
            deviceInfo = "Integration Pixel"
        )

        assertEquals(BatteryOrchestrator.State.Idle, orchestrator.state.value)
        orchestrator.start(); advanceUntilIdle()
        orchestrator.recordResult(
            TestType.TAP,
            MockTestModule.MockResult(0.9, mapOf("a" to 1.0))
        ); advanceUntilIdle()
        orchestrator.recordResult(
            TestType.GAIT,
            MockTestModule.MockResult(0.8, mapOf("b" to 2.0))
        ); advanceUntilIdle()
        orchestrator.recordResult(
            TestType.VISION,
            MockTestModule.MockResult(0.7, mapOf("c" to 3.0))
        ); advanceUntilIdle()

        assertEquals(BatteryOrchestrator.State.Completed, orchestrator.state.value)

        val sessionList = db.sessionDao().observeAll().first()
        assertEquals(1, sessionList.size)
        val session = sessionList.first()
        assertNotNull(session.completedAtEpochMs)

        val results = db.testResultDao().getForSession(session.id)
        assertEquals(3, results.size)
        val visionFeatures = results.first { it.testType == TestType.VISION }.featuresJson
        assertTrue(visionFeatures.contains("c") && visionFeatures.contains("3"))
    }
}
