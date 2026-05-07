package com.mustafan4x.msbattery.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class TestResultDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var sessionDao: SessionDao
    private lateinit var resultDao: TestResultDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        sessionDao = db.sessionDao()
        resultDao = db.testResultDao()
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun resultsForSessionReturnedInChronologicalOrder() = runTest {
        val session = SessionEntity(deviceInfo = "Pixel")
        sessionDao.insert(session)

        val first = TestResultEntity(
            sessionId = session.id, testType = TestType.TAP,
            startedAtEpochMs = 100, completedAtEpochMs = 110,
            qualityScore = 0.9, featuresJson = "{}"
        )
        val second = TestResultEntity(
            sessionId = session.id, testType = TestType.GAIT,
            startedAtEpochMs = 200, completedAtEpochMs = 230,
            qualityScore = 0.8, featuresJson = "{}"
        )
        resultDao.insert(second)
        resultDao.insert(first)

        val fetched = resultDao.getForSession(session.id)
        assertEquals(2, fetched.size)
        assertEquals(TestType.TAP, fetched[0].testType)
        assertEquals(TestType.GAIT, fetched[1].testType)
    }
}
