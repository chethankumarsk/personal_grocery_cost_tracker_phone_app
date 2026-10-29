package dev.groceryledger.domain.repository

import dev.groceryledger.domain.date.DateRange
import dev.groceryledger.domain.model.AppSettings
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
    fun observeEntries(date: LocalDate): Flow<List<Expense>>
    fun observeSuggestions(query: String): Flow<List<ItemSuggestion>>

    suspend fun add(name: String, amountPaise: Long, date: LocalDate): Expense
    suspend fun update(id: Long, name: String, amountPaise: Long, date: LocalDate)
    suspend fun delete(id: Long)
    suspend fun restore(expense: Expense)

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
