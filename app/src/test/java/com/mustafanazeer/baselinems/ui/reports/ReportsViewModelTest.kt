package com.mustafanazeer.baselinems.ui.reports

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mustafanazeer.baselinems.data.AppDatabase
import com.mustafanazeer.baselinems.data.SessionEntity
import com.mustafanazeer.baselinems.data.TestResultEntity
import com.mustafanazeer.baselinems.data.TestType
import com.mustafanazeer.baselinems.data.TrendsRepository
import com.mustafanazeer.baselinems.data.deriveQualityBand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ReportsViewModelTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: TrendsRepository
    private lateinit var testScope: CoroutineScope
    private lateinit var viewModel: ReportsViewModel

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = TrendsRepository(
            sessionDao = db.sessionDao(),
            testResultDao = db.testResultDao()
        )
        testScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher())
        viewModel = ReportsViewModel(repository = repository, scope = testScope)
    }

    @After
    fun teardown() {
        testScope.cancel()
        db.close()
    }

    @Test
    fun emptyDatabaseEmitsEmptyState() = runTest {
        val state = viewModel.reportsState
            .first { it !is ReportsScreenState.Loading }
        assertTrue(state is ReportsScreenState.Empty)
    }

    @Test
    fun seededSessionEmitsReadyState() = runTest {
        val session = SessionEntity(
            startedAtEpochMs = 1_000L,
            completedAtEpochMs = 60_000L,
            deviceInfo = "Pixel"
        )
        db.sessionDao().insert(session)
        db.testResultDao().insert(
            TestResultEntity(
                sessionId = session.id,
                testType = TestType.GAIT,
                startedAtEpochMs = 1_000L,
                completedAtEpochMs = 30_000L,
                qualityScore = 0.85,
                featuresJson = """{"cadence_steps_per_minute": 95.0}"""
            )
        )
        val state = viewModel.reportsState
            .first { it !is ReportsScreenState.Loading }
        assertTrue(state is ReportsScreenState.Ready)
        val ready = state as ReportsScreenState.Ready
        val gait = ready.testSummaries.first { it.testType == TestType.GAIT }
        assertEquals(QualityBand.HIGH, gait.latestQualityBand)
    }

    @Test
    fun observeTestDetailStartsAtLoadingOrResolvesToEmpty() = runTest {
        val flow = viewModel.observeTestDetail(TestType.GAIT)
        val resolved = flow.first { it !is TestDetailScreenState.Loading }
        assertTrue(resolved is TestDetailScreenState.Empty)
    }

    @Test
    fun observeTestDetailEmitsReadyWhenSeeded() = runTest {
        val session = SessionEntity(
            startedAtEpochMs = 1_000L,
            completedAtEpochMs = 60_000L,
            deviceInfo = "Pixel"
        )
        db.sessionDao().insert(session)
        db.testResultDao().insert(
            TestResultEntity(
                sessionId = session.id,
                testType = TestType.TAP,
                startedAtEpochMs = 1_000L,
                completedAtEpochMs = 30_000L,
                qualityScore = 0.45,
                featuresJson = """{"dominant_tap_rate_hz": 5.0}"""
            )
        )
        val resolved = viewModel.observeTestDetail(TestType.TAP)
            .first { it !is TestDetailScreenState.Loading }
        assertTrue(resolved is TestDetailScreenState.Ready)
        val ready = resolved as TestDetailScreenState.Ready
        assertEquals(QualityBand.MEDIUM, ready.latestQualityBand)
        assertEquals(1, ready.sessionCount)
    }

    @Test
    fun qualityBandDerivationPassesThrough() {
        assertEquals(QualityBand.HIGH, deriveQualityBand(0.95))
        assertEquals(QualityBand.MEDIUM, deriveQualityBand(0.5))
        assertEquals(QualityBand.LOW, deriveQualityBand(0.1))
    }
}
