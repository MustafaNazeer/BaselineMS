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

    @Query("SELECT COUNT(*) FROM session WHERE completedAtEpochMs IS NOT NULL")
    fun observeCompletedSessionCount(): Flow<Int>
}
