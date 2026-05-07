package com.mustafan4x.msbattery.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profile ORDER BY createdAtEpochMs ASC LIMIT 1")
    suspend fun getFirst(): UserProfileEntity?
}
