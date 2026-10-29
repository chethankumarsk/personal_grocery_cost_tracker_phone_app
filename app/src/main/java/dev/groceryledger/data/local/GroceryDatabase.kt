package dev.groceryledger.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ExpenseEntity::class, ItemCatalogEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class GroceryDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun itemCatalogDao(): ItemCatalogDao

    companion object {
        const val NAME = "grocery-ledger.db"
    }
}
