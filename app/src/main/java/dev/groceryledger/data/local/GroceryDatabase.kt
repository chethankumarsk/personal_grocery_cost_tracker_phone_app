package dev.groceryledger.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.groceryledger.domain.model.Categories

@Database(
    entities = [ExpenseEntity::class, ItemCatalogEntity::class, CategoryEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class GroceryDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun itemCatalogDao(): ItemCatalogDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        const val NAME = "grocery-ledger.db"

        /**
         * v1 -> v2: adds the category column (existing rows become "Uncategorized") and
         * seeds the category list. Additive only; no data is dropped.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE expenses ADD COLUMN category TEXT NOT NULL DEFAULT '${Categories.UNCATEGORIZED}'",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS categories (
                        name TEXT NOT NULL PRIMARY KEY,
                        name_normalized TEXT NOT NULL,
                        sort_order INTEGER NOT NULL,
                        is_preset INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_categories_name_normalized " +
                        "ON categories (name_normalized)",
                )

                Categories.presets.forEachIndexed { index, preset ->
                    db.execSQL(
                        "INSERT OR IGNORE INTO categories (name, name_normalized, sort_order, is_preset) " +
                            "VALUES (?, ?, ?, 1)",
                        arrayOf(preset, preset.lowercase(), index),
                    )
                }
            }
        }

        /** Room needs a v2 schema for a fresh install; the presets are inserted on create. */
        val CALLBACK_SEED_CATEGORIES = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                Categories.presets.forEachIndexed { index, preset ->
                    db.execSQL(
                        "INSERT OR IGNORE INTO categories (name, name_normalized, sort_order, is_preset) " +
                            "VALUES (?, ?, ?, 1)",
                        arrayOf(preset, preset.lowercase(), index),
                    )
                }
            }
        }
    }
}
