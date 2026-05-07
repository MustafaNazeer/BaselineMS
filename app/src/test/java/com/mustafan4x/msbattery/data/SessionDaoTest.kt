package com.mustafan4x.msbattery.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class SessionDaoTest {

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
    fun insertAndFetchSession() = runTest {
        val session = SessionEntity(deviceInfo = "Pixel Test, Android 14")
        sessionDao.insert(session)

        val all = sessionDao.observeAll().first()
        assertEquals(1, all.size)
        assertEquals("Pixel Test, Android 14", all.first().deviceInfo)
        assertNull(all.first().completedAtEpochMs)
    }

    @Test
    fun deletingSessionCascadesToResults() = runTest {
        val session = SessionEntity(deviceInfo = "Pixel")
        sessionDao.insert(session)
        val result = TestResultEntity(
            sessionId = session.id,
            testType = TestType.TAP,
            startedAtEpochMs = 0L,
            completedAtEpochMs = 100L,
            qualityScore = 1.0,
            featuresJson = "{}"
        )
        resultDao.insert(result)
        assertEquals(1, resultDao.getForSession(session.id).size)

        sessionDao.delete(session)

        assertNull(sessionDao.getById(session.id))
        assertEquals(0, resultDao.getForSession(session.id).size)
    }

    @Test
    fun getByIdReturnsCorrectSession() = runTest {
        val a = SessionEntity(deviceInfo = "A")
        val b = SessionEntity(deviceInfo = "B")
        sessionDao.insert(a); sessionDao.insert(b)

        val fetched = sessionDao.getById(b.id)
        assertNotNull(fetched)
        assertEquals("B", fetched!!.deviceInfo)
    }
}
