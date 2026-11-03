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

    @Test
    fun migrationFrom2To3AddsEmptyQuantityAndKeepsEntries() {
        helper.createDatabase(TEST_DB, 2).use { database ->
            database.execSQL(
                """
                INSERT INTO expenses
                    (entry_uid, name, name_normalized, amount_paise, expense_date, created_at, updated_at, category)
                VALUES
                    ('22222222-2222-2222-2222-222222222222', 'Rice', 'rice', 9000, 20328, 1000, 1000, 'Grains')
                """.trimIndent(),
            )
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB,
            3,
            true,
            GroceryDatabase.MIGRATION_2_3,
        )

        migrated.query("SELECT name, amount_paise, category, quantity_milli, unit FROM expenses")
            .use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Rice", cursor.getString(0))
                assertEquals(9_000L, cursor.getLong(1))
                assertEquals("Grains", cursor.getString(2))
                assertTrue(cursor.isNull(3))
                assertTrue(cursor.isNull(4))
            }
    }

    @Test
    fun migrationFrom3To4AddsAnEmptyShoppingList() {
        helper.createDatabase(TEST_DB, 3).use { database ->
            database.execSQL(
                """
                INSERT INTO expenses
                    (entry_uid, name, name_normalized, amount_paise, expense_date,
                     created_at, updated_at, category, quantity_milli, unit)
                VALUES
                    ('33333333-3333-3333-3333-333333333333', 'Milk', 'milk', 6000, 20328,
                     1000, 1000, 'Dairy', 1500, 'L')
                """.trimIndent(),
            )
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB,
            4,
            true,
            GroceryDatabase.MIGRATION_3_4,
        )

        migrated.query("SELECT COUNT(*) FROM shopping_items").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        migrated.query("SELECT name, quantity_milli, unit FROM expenses").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Milk", cursor.getString(0))
            assertEquals(1_500L, cursor.getLong(1))
            assertEquals("L", cursor.getString(2))
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
