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
class Migration1To2Test {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate1To2_preservesProfileRow_andAllowsNullHeight() {
        helper.createDatabase("test.db", 1).use { db ->
            db.execSQL(
                """
                INSERT INTO user_profile (id, dateOfBirthEpochMs, biologicalSex, dominantHand, msTypeDisclosed, heightCm, createdAtEpochMs)
                VALUES ('u1', 0, 'FEMALE', 'RIGHT', 'UNDISCLOSED', 168.0, 0)
                """.trimIndent()
            )
        }

        helper.runMigrationsAndValidate("test.db", 2, true, MIGRATION_1_2).use { db ->
            db.query(
                "SELECT id, dateOfBirthEpochMs, biologicalSex, dominantHand, msTypeDisclosed, heightCm, createdAtEpochMs FROM user_profile WHERE id = 'u1'"
            ).use { c ->
                assertTrue("Expected one row post migration", c.moveToFirst())
                assertEquals("u1", c.getString(0))
                assertEquals(0L, c.getLong(1))
                assertEquals("FEMALE", c.getString(2))
                assertEquals("RIGHT", c.getString(3))
                assertEquals("UNDISCLOSED", c.getString(4))
                assertEquals(168.0, c.getDouble(5), 0.0)
                assertEquals(0L, c.getLong(6))
            }

            db.execSQL(
                """
                INSERT INTO user_profile (id, dateOfBirthEpochMs, biologicalSex, dominantHand, msTypeDisclosed, heightCm, createdAtEpochMs)
                VALUES ('u2', 0, 'FEMALE', 'RIGHT', 'UNDISCLOSED', NULL, 0)
                """.trimIndent()
            )

            db.query("SELECT heightCm FROM user_profile WHERE id = 'u2'").use { c ->
                assertTrue("Expected u2 row to exist", c.moveToFirst())
                assertTrue("Expected heightCm to be null for u2", c.isNull(0))
            }
        }
    }

    @Test
    fun migrate1To2_emptyTable_succeeds() {
        helper.createDatabase("test_empty.db", 1).use { }

        helper.runMigrationsAndValidate("test_empty.db", 2, true, MIGRATION_1_2).use { db ->
            db.query("SELECT COUNT(*) FROM user_profile").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(0L, c.getLong(0))
            }
        }
    }
}
