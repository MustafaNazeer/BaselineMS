package com.mustafanazeer.baselinems.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TestResultDao {

    @Insert
    suspend fun insert(result: TestResultEntity)

    @Query("SELECT * FROM test_result WHERE session_id = :sessionId ORDER BY startedAtEpochMs ASC")
    suspend fun getForSession(sessionId: String): List<TestResultEntity>

    @Query("SELECT * FROM test_result WHERE testType = :testType ORDER BY startedAtEpochMs ASC")
    fun observeByType(testType: TestType): Flow<List<TestResultEntity>>

    @Query("SELECT * FROM test_result ORDER BY startedAtEpochMs ASC")
    fun observeAll(): Flow<List<TestResultEntity>>
}
