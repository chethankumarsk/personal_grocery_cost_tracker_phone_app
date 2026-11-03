package dev.groceryledger.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query(
        """
        SELECT * FROM expenses
        WHERE expense_date = :dateEpochDay
          AND (:category IS NULL OR category = :category)
        ORDER BY created_at ASC, id ASC
        """,
    )
    fun observeEntriesForDate(
        dateEpochDay: Long,
        category: String?,
    ): Flow<List<ExpenseEntity>>

    @Query(
        """
        SELECT * FROM expenses
        WHERE name_normalized = :normalizedName
        ORDER BY expense_date DESC, created_at DESC, id DESC
        LIMIT :limit
        """,
    )
    fun observePriceHistory(normalizedName: String, limit: Int): Flow<List<ExpenseEntity>>

    @Query(
        """
        SELECT expense_date AS dateEpochDay,
               SUM(amount_paise) AS totalPaise,
               COUNT(*) AS itemCount
        FROM expenses
        WHERE expense_date BETWEEN :startEpochDay AND :endEpochDay
          AND (:category IS NULL OR category = :category)
        GROUP BY expense_date
        """,
    )
    fun observeDailyTotals(
        startEpochDay: Long,
        endEpochDay: Long,
        category: String?,
    ): Flow<List<DailyTotalRow>>

    @Query(
        """
        SELECT category,
               SUM(amount_paise) AS totalPaise,
               COUNT(*) AS itemCount
        FROM expenses
        WHERE expense_date BETWEEN :startEpochDay AND :endEpochDay
        GROUP BY category
        ORDER BY totalPaise DESC, category ASC
        """,
    )
    fun observeCategoryTotals(
        startEpochDay: Long,
        endEpochDay: Long,
    ): Flow<List<CategoryTotalRow>>

    @Query(
        """
        SELECT SUM(amount_paise) FROM expenses
        WHERE expense_date BETWEEN :startEpochDay AND :endEpochDay
          AND (:category IS NULL OR category = :category)
        """,
    )
    fun observeTotal(
        startEpochDay: Long,
        endEpochDay: Long,
        category: String?,
    ): Flow<Long?>

    @Query("SELECT * FROM expenses ORDER BY expense_date ASC, created_at ASC, id ASC")
    suspend fun getAll(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): ExpenseEntity?

    @Query("SELECT entry_uid FROM expenses")
    suspend fun getAllUids(): List<String>

    @Query(
        """
        SELECT * FROM expenses
        WHERE name_normalized = :normalizedName
        ORDER BY expense_date DESC, created_at DESC, id DESC
        LIMIT 1
        """,
    )
    suspend fun latestForName(normalizedName: String): ExpenseEntity?

    @Query("SELECT COUNT(*) FROM expenses WHERE name_normalized = :normalizedName")
    suspend fun countForName(normalizedName: String): Int

    @Query("SELECT MAX(created_at) FROM expenses WHERE name_normalized = :normalizedName")
    suspend fun maxCreatedAtForName(normalizedName: String): Long?

    @Query("SELECT DISTINCT name_normalized FROM expenses")
    suspend fun distinctNormalizedNames(): List<String>

    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Insert
    suspend fun insertAll(expenses: List<ExpenseEntity>)

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun count(): Int

    @Query("UPDATE expenses SET category = :to WHERE category = :from")
    suspend fun renameCategory(from: String, to: String)

    @Query("UPDATE expenses SET category = :to WHERE category = :from")
    suspend fun reassignCategory(from: String, to: String)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sort_order ASC, name ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sort_order ASC, name ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE name_normalized = :normalizedName LIMIT 1")
    suspend fun findByNormalizedName(normalizedName: String): CategoryEntity?

    @Query("SELECT MAX(sort_order) FROM categories")
    suspend fun maxSortOrder(): Int?

    @Query("SELECT COUNT(*) FROM expenses WHERE category = :name")
    suspend fun countEntries(name: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("UPDATE categories SET name = :newName, name_normalized = :newNormalized WHERE name = :oldName")
    suspend fun rename(oldName: String, newName: String, newNormalized: String)

    @Query("DELETE FROM categories WHERE name = :name")
    suspend fun delete(name: String)

    @Query("DELETE FROM categories")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}

@Dao
interface ItemCatalogDao {
    @Query(
        """
        SELECT * FROM item_catalog
        WHERE name_normalized LIKE '%' || :escapedQuery || '%' ESCAPE '\'
        ORDER BY usage_count DESC, last_expense_date DESC
        LIMIT 500
        """,
    )
    fun observeSearch(escapedQuery: String): Flow<List<ItemCatalogEntity>>

    /**
     * Most recently used items, newest first, for the quick-add chips. The category comes
     * from that item's latest entry, so a chip restores name, price and category without
     * a schema change.
     */
    @Query(
        """
        SELECT ic.name AS name,
               ic.last_price_paise AS lastPricePaise,
               COALESCE(
                   (SELECT e.category FROM expenses e
                    WHERE e.name_normalized = ic.name_normalized
                    ORDER BY e.expense_date DESC, e.created_at DESC, e.id DESC
                    LIMIT 1),
                   'Uncategorized'
               ) AS category
        FROM item_catalog ic
        ORDER BY ic.last_used_at DESC, ic.last_expense_date DESC, ic.name ASC
        LIMIT :limit
        """,
    )
    fun observeRecent(limit: Int): Flow<List<RecentItemRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ItemCatalogEntity)

    @Query("DELETE FROM item_catalog WHERE name_normalized = :normalizedName")
    suspend fun delete(normalizedName: String)

    @Query("DELETE FROM item_catalog")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM item_catalog")
    suspend fun count(): Int
}

@Dao
interface ShoppingListDao {
    @Query("SELECT * FROM shopping_items ORDER BY is_checked ASC, created_at ASC, id ASC")
    fun observeAll(): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items WHERE is_checked = 1 ORDER BY created_at ASC, id ASC")
    suspend fun checkedItems(): List<ShoppingItemEntity>

    @Insert
    suspend fun insert(item: ShoppingItemEntity): Long

    @Query("UPDATE shopping_items SET is_checked = :checked WHERE id = :id")
    suspend fun setChecked(id: Long, checked: Boolean)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM shopping_items")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM shopping_items")
    suspend fun count(): Int
}
