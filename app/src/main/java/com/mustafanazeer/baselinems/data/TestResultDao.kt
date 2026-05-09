package com.mustafanazeer.baselinems.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TestResultDao {

    @Insert
    suspend fun insert(result: TestResultEntity)

    @Query("SELECT * FROM test_result WHERE session_id = :sessionId ORDER BY startedAtEpochMs ASC")
    suspend fun getForSession(sessionId: String): List<TestResultEntity>
}
