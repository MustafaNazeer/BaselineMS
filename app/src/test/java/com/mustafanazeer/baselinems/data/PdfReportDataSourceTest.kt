package com.mustafanazeer.baselinems.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mustafanazeer.baselinems.ui.reports.QualityBand
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PdfReportDataSourceTest {

    private lateinit var db: AppDatabase
    private lateinit var trends: TrendsRepository
    private lateinit var source: PdfReportDataSource

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        trends = TrendsRepository(db.sessionDao(), db.testResultDao())
        source = PdfReportDataSource(db.sessionDao(), db.testResultDao(), trends)
    }

    @After fun tearDown() { db.close() }

    @Test
    fun snapshot_emptyDatabase_yieldsEmptySnapshot() = runBlocking {
        val snap = source.snapshot()
        assertEquals(0, snap.totalCompletedSessions)
        assertEquals(0, snap.perTestSections.size)
    }

    @Test
    fun snapshot_singleTapSession_yieldsOneSection() = runBlocking {
        val sessionId = "sess-1"
        db.sessionDao().insert(SessionEntity(
            id = sessionId,
            startedAtEpochMs = 1_700_000_000_000L,
            completedAtEpochMs = 1_700_000_030_000L,
            deviceInfo = "test"
        ))
        db.testResultDao().insert(TestResultEntity(
            id = "tr-1",
            sessionId = sessionId,
            testType = TestType.TAP,
            startedAtEpochMs = 1_700_000_000_000L,
            completedAtEpochMs = 1_700_000_030_000L,
            qualityScore = 0.85,
            featuresJson = """{"dominant_tap_rate_hz":5.2,"miss_rate":0.02}""",
            rawSensorRelativePath = null
        ))
        val snap = source.snapshot()
        assertEquals(1, snap.totalCompletedSessions)
        assertEquals(1, snap.perTestSections.size)
        val tapSection = snap.perTestSections.first { it.testType == TestType.TAP }
        assertEquals(1, tapSection.summaryRows.size)
        assertEquals(QualityBand.HIGH, tapSection.summaryRows.first().qualityBand)
        assertNotNull(tapSection.featureSeries.firstOrNull { it.key == "dominant_tap_rate_hz" })
    }
}
