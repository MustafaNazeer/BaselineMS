package com.mustafanazeer.baselinems.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert
    suspend fun insert(session: SessionEntity)

    @Update
    suspend fun update(session: SessionEntity)

    @Delete
    suspend fun delete(session: SessionEntity)

    @Query("SELECT * FROM session ORDER BY startedAtEpochMs DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM session WHERE id = :id")
    suspend fun getById(id: String): SessionEntity?

    @Query("SELECT COUNT(*) FROM session WHERE completedAtEpochMs IS NOT NULL AND wasCancelled = 0")
    fun observeCompletedSessionCount(): Flow<Int>

    @Query(
        "UPDATE session SET completedAtEpochMs = :nowEpochMs, wasCancelled = 1 " +
            "WHERE completedAtEpochMs IS NULL AND startedAtEpochMs < :strandedBeforeEpochMs"
    )
    suspend fun reclaimStrandedSessions(nowEpochMs: Long, strandedBeforeEpochMs: Long): Int
}
