package com.mustafanazeer.baselinems.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class Migration2To3Test {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate2To3AddsWasCancelledColumnDefaultingToZero() {
        helper.createDatabase("session_wc.db", 2).use { db ->
            db.execSQL(
                """
                INSERT INTO session (id, startedAtEpochMs, completedAtEpochMs, deviceInfo)
                VALUES ('s1', 100, 200, 'Legacy Pixel')
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO session (id, startedAtEpochMs, completedAtEpochMs, deviceInfo)
                VALUES ('s2', 300, NULL, 'In flight Pixel')
                """.trimIndent()
            )
        }

        helper.runMigrationsAndValidate(
            "session_wc.db",
            3,
            true,
            MIGRATION_1_2,
            MIGRATION_2_3
        ).use { db ->
            db.query(
                "SELECT id, startedAtEpochMs, completedAtEpochMs, deviceInfo, wasCancelled FROM session ORDER BY startedAtEpochMs ASC"
            ).use { c ->
                assertTrue("Expected first row", c.moveToFirst())
                assertEquals("s1", c.getString(0))
                assertEquals(100L, c.getLong(1))
                assertEquals(200L, c.getLong(2))
                assertEquals("Legacy Pixel", c.getString(3))
                assertEquals(0, c.getInt(4))

                assertTrue("Expected second row", c.moveToNext())
                assertEquals("s2", c.getString(0))
                assertEquals(0, c.getInt(4))
            }
        }
    }
}
