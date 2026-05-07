package com.mustafan4x.msbattery.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dateOfBirthEpochMs: Long,
    val biologicalSex: Sex,
    val dominantHand: Hand,
    val msTypeDisclosed: MSType = MSType.UNDISCLOSED,
    val heightCm: Double? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
