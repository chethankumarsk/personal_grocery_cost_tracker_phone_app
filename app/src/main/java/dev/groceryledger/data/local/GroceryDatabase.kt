package dev.groceryledger.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.groceryledger.domain.model.Categories

@Database(
    entities = [
        ExpenseEntity::class,
        ItemCatalogEntity::class,
        CategoryEntity::class,
        ShoppingItemEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class GroceryDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun itemCatalogDao(): ItemCatalogDao
    abstract fun categoryDao(): CategoryDao
    abstract fun shoppingListDao(): ShoppingListDao

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

        /**
         * v2 -> v3: optional quantity (thousandths of the chosen unit) and its unit for
         * unit-price comparison. Additive only; existing rows keep no quantity.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN quantity_milli INTEGER")
                db.execSQL("ALTER TABLE expenses ADD COLUMN unit TEXT")
            }
        }

        /**
         * v3 -> v4: the shopping list lives in its own table. It is a scratch list and is
         * not part of CSV backups.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS shopping_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        amount_paise INTEGER,
                        is_checked INTEGER NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_shopping_items_created_at " +
                        "ON shopping_items (created_at)",
                )
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
