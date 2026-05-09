package com.mustafanazeer.baselinems.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class UserProfileDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: UserProfileDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.userProfileDao()
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun insertAndFetchSingleProfile() = runTest {
        val profile = UserProfileEntity(
            dateOfBirthEpochMs = 0L,
            biologicalSex = Sex.FEMALE,
            dominantHand = Hand.RIGHT,
            heightCm = 170.0
        )
        dao.insert(profile)

        val fetched = dao.getFirst()
        assertNotNull(fetched)
        assertEquals(170.0, fetched!!.heightCm!!, 0.0001)
        assertEquals(Sex.FEMALE, fetched.biologicalSex)
        assertEquals(MSType.UNDISCLOSED, fetched.msTypeDisclosed)
    }

    @Test
    fun emptyTableReturnsNull() = runTest {
        assertNull(dao.getFirst())
    }
}
