package dev.groceryledger.domain.repository

import dev.groceryledger.domain.date.DateRange
import dev.groceryledger.domain.model.AppSettings
import dev.groceryledger.domain.model.Categories
import dev.groceryledger.domain.model.CategoryTotal
import dev.groceryledger.domain.model.DailyTotal
import dev.groceryledger.domain.model.Expense
import dev.groceryledger.domain.model.ItemSuggestion
import dev.groceryledger.domain.model.ThemeMode
import dev.groceryledger.domain.model.WeekStart
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface ExpenseRepository {
    fun observeDailyTotals(range: DateRange): Flow<List<DailyTotal>>
    fun observeTotal(range: DateRange): Flow<Long>
    fun observeCategoryTotals(range: DateRange): Flow<List<CategoryTotal>>
    fun observeEntries(date: LocalDate): Flow<List<Expense>>
    fun observeSuggestions(query: String): Flow<List<ItemSuggestion>>

    /** Category names for the picker, sorted. [Categories.UNCATEGORIZED] is virtual. */
    fun observeCategories(): Flow<List<String>>

    suspend fun add(
        name: String,
        amountPaise: Long,
        date: LocalDate,
        category: String = Categories.UNCATEGORIZED,
    ): Expense

    suspend fun update(
        id: Long,
        name: String,
        amountPaise: Long,
        date: LocalDate,
        category: String = Categories.UNCATEGORIZED,
    )

    suspend fun delete(id: Long)
    suspend fun restore(expense: Expense)

    /** Returns false when the name is blank, too long, reserved, or already exists. */
    suspend fun addCategory(name: String): Boolean

    /** Renames a category and moves its entries with it. */
    suspend fun renameCategory(oldName: String, newName: String): Boolean

    /** Deletes a category, moving its entries to [Categories.UNCATEGORIZED]. */
    suspend fun deleteCategory(name: String): Boolean

    suspend fun all(): List<Expense>
    suspend fun importAll(expenses: List<Expense>): Int
    suspend fun existingUids(): Set<String>
    suspend fun clearAll()
}

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setWeekStart(weekStart: WeekStart)
    suspend fun setTheme(theme: ThemeMode)
}
