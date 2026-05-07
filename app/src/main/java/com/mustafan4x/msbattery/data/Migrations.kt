package com.mustafan4x.msbattery.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE user_profile_new (
                id TEXT NOT NULL PRIMARY KEY,
                dateOfBirthEpochMs INTEGER NOT NULL,
                biologicalSex TEXT NOT NULL,
                dominantHand TEXT NOT NULL,
                msTypeDisclosed TEXT NOT NULL,
                heightCm REAL,
                createdAtEpochMs INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO user_profile_new (id, dateOfBirthEpochMs, biologicalSex, dominantHand, msTypeDisclosed, heightCm, createdAtEpochMs)
            SELECT id, dateOfBirthEpochMs, biologicalSex, dominantHand, msTypeDisclosed, heightCm, createdAtEpochMs FROM user_profile
            """.trimIndent()
        )
        db.execSQL("DROP TABLE user_profile")
        db.execSQL("ALTER TABLE user_profile_new RENAME TO user_profile")
    }
}
