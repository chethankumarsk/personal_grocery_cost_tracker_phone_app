package dev.groceryledger.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.groceryledger.domain.model.Categories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards the upgrade path: opening a v1 database with 1.2+ must keep every entry and add the
 * default category, without a destructive fallback.
 */
@RunWith(AndroidJUnit4::class)
class GroceryDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GroceryDatabase::class.java,
    )

    @Test
    fun migrationFrom1To2PreservesEntriesAndSeedsCategories() {
        helper.createDatabase(TEST_DB, 1).use { database ->
            database.execSQL(
                """
                INSERT INTO expenses
                    (entry_uid, name, name_normalized, amount_paise, expense_date, created_at, updated_at)
                VALUES
                    ('11111111-1111-1111-1111-111111111111', 'Milk', 'milk', 6000, 20328, 1000, 1000)
                """.trimIndent(),
            )
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB,
            2,
            true,
            GroceryDatabase.MIGRATION_1_2,
        )

        migrated.query("SELECT name, amount_paise, category FROM expenses").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Milk", cursor.getString(0))
            assertEquals(6_000L, cursor.getLong(1))
            assertEquals(Categories.UNCATEGORIZED, cursor.getString(2))
        }

        migrated.query("SELECT name FROM categories ORDER BY sort_order").use { cursor ->
            val names = mutableListOf<String>()
            while (cursor.moveToNext()) names.add(cursor.getString(0))
            assertEquals(Categories.presets, names)
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
