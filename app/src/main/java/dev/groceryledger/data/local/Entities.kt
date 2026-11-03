package dev.groceryledger.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.groceryledger.domain.model.Categories

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["entry_uid"], unique = true),
        Index(value = ["expense_date"]),
        Index(value = ["name_normalized"]),
    ],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "entry_uid")
    val entryUid: String,
    val name: String,
    @ColumnInfo(name = "name_normalized")
    val nameNormalized: String,
    @ColumnInfo(name = "amount_paise")
    val amountPaise: Long,
    @ColumnInfo(name = "expense_date")
    val expenseDate: Long,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "category", defaultValue = "Uncategorized")
    val category: String = Categories.UNCATEGORIZED,
)

/**
 * Categories available for selection. Entries store the category *name*, so this table is
 * the picker list, not a foreign key target.
 */
@Entity(
    tableName = "categories",
    indices = [Index(value = ["name_normalized"], unique = true)],
)
data class CategoryEntity(
    @PrimaryKey
    val name: String,
    @ColumnInfo(name = "name_normalized")
    val nameNormalized: String,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
    @ColumnInfo(name = "is_preset")
    val isPreset: Boolean,
)

@Entity(
    tableName = "item_catalog",
    indices = [Index(value = ["name_normalized"], unique = true)],
)
data class ItemCatalogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "name_normalized")
    val nameNormalized: String,
    @ColumnInfo(name = "last_price_paise")
    val lastPricePaise: Long,
    @ColumnInfo(name = "last_expense_date")
    val lastExpenseDate: Long,
    @ColumnInfo(name = "usage_count")
    val usageCount: Int,
    @ColumnInfo(name = "last_used_at")
    val lastUsedAt: Long,
)

data class DailyTotalRow(
    @ColumnInfo(name = "dateEpochDay")
    val dateEpochDay: Long,
    @ColumnInfo(name = "totalPaise")
    val totalPaise: Long,
    @ColumnInfo(name = "itemCount")
    val itemCount: Int,
)

data class CategoryTotalRow(
    @ColumnInfo(name = "category")
    val category: String,
    @ColumnInfo(name = "totalPaise")
    val totalPaise: Long,
    @ColumnInfo(name = "itemCount")
    val itemCount: Int,
)

/** One entry of the quick-add chip row: catalog price plus the last category used. */
data class RecentItemRow(
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "lastPricePaise")
    val lastPricePaise: Long,
    @ColumnInfo(name = "category")
    val category: String,
)
