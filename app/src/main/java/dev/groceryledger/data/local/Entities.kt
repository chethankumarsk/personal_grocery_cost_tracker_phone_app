package dev.groceryledger.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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
