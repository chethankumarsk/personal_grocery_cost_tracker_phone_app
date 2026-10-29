package dev.groceryledger.data.local

import androidx.room.withTransaction
import dev.groceryledger.data.local.GroceryDatabase
import dev.groceryledger.data.local.ExpenseDao
import dev.groceryledger.data.local.ExpenseEntity
import dev.groceryledger.data.local.ItemCatalogDao
import dev.groceryledger.data.local.ItemCatalogEntity
import dev.groceryledger.domain.date.DateRange
import dev.groceryledger.domain.model.DailyTotal
import dev.groceryledger.domain.model.Expense
import dev.groceryledger.domain.model.ItemSuggestion
import dev.groceryledger.domain.repository.ExpenseRepository
import dev.groceryledger.domain.suggestion.ItemNames
import dev.groceryledger.domain.suggestion.SuggestionRanker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

class OfflineExpenseRepository(
    private val database: GroceryDatabase,
    private val expenseDao: ExpenseDao,
    private val catalogDao: ItemCatalogDao,
) : ExpenseRepository {

    override fun observeDailyTotals(range: DateRange): Flow<List<DailyTotal>> =
        expenseDao.observeDailyTotals(range.start.toEpochDay(), range.endInclusive.toEpochDay())
            .map { rows ->
                rows.map {
                    DailyTotal(
                        date = LocalDate.ofEpochDay(it.dateEpochDay),
                        totalPaise = it.totalPaise,
                        itemCount = it.itemCount,
                    )
                }
            }

    override fun observeTotal(range: DateRange): Flow<Long> =
        expenseDao.observeTotal(range.start.toEpochDay(), range.endInclusive.toEpochDay())
            .map { it ?: 0L }

    override fun observeEntries(date: LocalDate): Flow<List<Expense>> =
        expenseDao.observeEntriesForDate(date.toEpochDay())
            .map { rows -> rows.map(::toDomain) }

    override fun observeSuggestions(query: String): Flow<List<ItemSuggestion>> {
        val normalized = ItemNames.normalize(query)
        if (normalized.length < 2) return flowOf(emptyList())

        val escaped = escapeLike(normalized)
        return catalogDao.observeSearch(escaped)
            .map { rows ->
                SuggestionRanker.rank(
                    items = rows.map(::toSuggestion),
                    rawQuery = normalized,
                )
            }
            .distinctUntilChanged()
    }

    override suspend fun add(name: String, amountPaise: Long, date: LocalDate): Expense {
        val trimmed = validateName(name)
        val now = System.currentTimeMillis()
        val entity = ExpenseEntity(
            entryUid = UUID.randomUUID().toString(),
            name = trimmed,
            nameNormalized = ItemNames.normalize(trimmed),
            amountPaise = amountPaise,
            expenseDate = date.toEpochDay(),
            createdAt = now,
            updatedAt = now,
        )

        return database.withTransaction {
            val id = expenseDao.insert(entity)
            refreshCatalog(entity.nameNormalized)
            toDomain(entity.copy(id = id))
        }
    }

    override suspend fun update(id: Long, name: String, amountPaise: Long, date: LocalDate) {
        val trimmed = validateName(name)
        database.withTransaction {
            val existing = expenseDao.getById(id) ?: return@withTransaction
            val updated = existing.copy(
                name = trimmed,
                nameNormalized = ItemNames.normalize(trimmed),
                amountPaise = amountPaise,
                expenseDate = date.toEpochDay(),
                updatedAt = System.currentTimeMillis(),
            )
            expenseDao.update(updated)
            refreshCatalog(existing.nameNormalized)
            if (updated.nameNormalized != existing.nameNormalized) {
                refreshCatalog(updated.nameNormalized)
            }
        }
    }

    override suspend fun delete(id: Long) {
        database.withTransaction {
            val existing = expenseDao.getById(id) ?: return@withTransaction
            expenseDao.delete(existing)
            refreshCatalog(existing.nameNormalized)
        }
    }

    override suspend fun restore(expense: Expense) {
        val normalized = ItemNames.normalize(expense.name)
        database.withTransaction {
            expenseDao.insert(
                ExpenseEntity(
                    entryUid = expense.uid,
                    name = expense.name,
                    nameNormalized = normalized,
                    amountPaise = expense.amountPaise,
                    expenseDate = expense.date.toEpochDay(),
                    createdAt = expense.createdAt,
                    updatedAt = expense.updatedAt,
                ),
            )
            refreshCatalog(normalized)
        }
    }

    override suspend fun all(): List<Expense> = expenseDao.getAll().map(::toDomain)

    override suspend fun importAll(expenses: List<Expense>): Int {
        if (expenses.isEmpty()) return 0
        val now = System.currentTimeMillis()
        val entities = expenses.mapIndexed { index, expense ->
            ExpenseEntity(
                entryUid = expense.uid,
                name = expense.name,
                nameNormalized = ItemNames.normalize(expense.name),
                amountPaise = expense.amountPaise,
                expenseDate = expense.date.toEpochDay(),
                createdAt = if (expense.createdAt > 0) expense.createdAt else now + index,
                updatedAt = now,
            )
        }

        database.withTransaction {
            expenseDao.insertAll(entities)
            rebuildCatalog()
        }
        return entities.size
    }

    override suspend fun existingUids(): Set<String> = expenseDao.getAllUids().toSet()

    override suspend fun clearAll() {
        database.withTransaction {
            expenseDao.deleteAll()
            catalogDao.clear()
        }
    }

    private suspend fun rebuildCatalog() {
        catalogDao.clear()
        expenseDao.distinctNormalizedNames().forEach { normalized ->
            refreshCatalog(normalized)
        }
    }

    private suspend fun refreshCatalog(normalizedName: String) {
        val latest = expenseDao.latestForName(normalizedName)
        if (latest == null) {
            catalogDao.delete(normalizedName)
            return
        }

        catalogDao.upsert(
            ItemCatalogEntity(
                name = latest.name,
                nameNormalized = normalizedName,
                lastPricePaise = latest.amountPaise,
                lastExpenseDate = latest.expenseDate,
                usageCount = expenseDao.countForName(normalizedName),
                lastUsedAt = expenseDao.maxCreatedAtForName(normalizedName) ?: latest.createdAt,
            ),
        )
    }

    private fun validateName(name: String): String {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Item name cannot be blank" }
        require(trimmed.length <= 80) { "Item name is too long" }
        return trimmed
    }

    private fun toDomain(entity: ExpenseEntity): Expense = Expense(
        id = entity.id,
        uid = entity.entryUid,
        name = entity.name,
        amountPaise = entity.amountPaise,
        date = LocalDate.ofEpochDay(entity.expenseDate),
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
    )

    private fun toSuggestion(entity: ItemCatalogEntity): ItemSuggestion = ItemSuggestion(
        name = entity.name,
        lastPricePaise = entity.lastPricePaise,
        lastDate = LocalDate.ofEpochDay(entity.lastExpenseDate),
        usageCount = entity.usageCount,
        lastUsedAt = entity.lastUsedAt,
    )

    private fun escapeLike(value: String): String = buildString {
        for (char in value) {
            when (char) {
                '\\', '%', '_' -> {
                    append('\\')
                    append(char)
                }
                else -> append(char)
            }
        }
    }
}
