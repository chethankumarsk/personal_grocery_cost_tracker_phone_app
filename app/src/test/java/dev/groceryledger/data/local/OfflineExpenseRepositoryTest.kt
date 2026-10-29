package dev.groceryledger.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.groceryledger.domain.date.DateRange
import dev.groceryledger.domain.model.Expense
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfflineExpenseRepositoryTest {
    private lateinit var database: GroceryDatabase
    private lateinit var repository: OfflineExpenseRepository

    private val date = LocalDate.of(2026, 10, 29)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, GroceryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = OfflineExpenseRepository(
            database = database,
            expenseDao = database.expenseDao(),
            catalogDao = database.itemCatalogDao(),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun totalsAndSuggestionsReflectEntries() = runBlocking {
        repository.add("Milk", 6_000, date)
        repository.add("Milk", 6_200, date.plusDays(1))
        repository.add("Bread", 4_000, date)

        val dayTotal = repository.observeTotal(DateRange(date, date)).first()
        assertEquals(10_000L, dayTotal)

        val twoDayTotal = repository.observeTotal(DateRange(date, date.plusDays(1))).first()
        assertEquals(16_200L, twoDayTotal)

        val suggestions = repository.observeSuggestions("mi").first()
        assertEquals(1, suggestions.size)
        assertEquals("Milk", suggestions.single().name)
        assertEquals(6_200L, suggestions.single().lastPricePaise)
        assertEquals(date.plusDays(1), suggestions.single().lastDate)
        assertEquals(2, suggestions.single().usageCount)
    }

    @Test
    fun deleteAndRestoreKeepTheCatalogConsistent() = runBlocking {
        val milk = repository.add("Milk", 6_000, date)
        repository.add("Bread", 4_000, date)

        repository.delete(milk.id)
        assertTrue(repository.observeSuggestions("mi").first().isEmpty())

        repository.restore(milk)
        val restored = repository.observeSuggestions("mi").first()
        assertEquals(1, restored.size)
        assertEquals("Milk", restored.single().name)
    }

    @Test
    fun importRebuildsTheCatalog() = runBlocking {
        val entries = listOf(
            Expense(
                id = 0,
                uid = UUID.randomUUID().toString(),
                name = "Rice",
                amountPaise = 12_000,
                date = date,
                createdAt = 1,
                updatedAt = 1,
            ),
            Expense(
                id = 0,
                uid = UUID.randomUUID().toString(),
                name = "Rice",
                amountPaise = 13_500,
                date = date.plusDays(2),
                createdAt = 2,
                updatedAt = 2,
            ),
        )

        repository.importAll(entries)

        assertEquals(2, database.expenseDao().count())
        val suggestions = repository.observeSuggestions("ri").first()
        assertEquals(1, suggestions.size)
        assertEquals("Rice", suggestions.single().name)
        assertEquals(13_500L, suggestions.single().lastPricePaise)
        assertEquals(2, suggestions.single().usageCount)
    }

    @Test
    fun clearingAllRemovesEntriesAndSuggestions() = runBlocking {
        repository.add("Milk", 6_000, date)
        repository.clearAll()

        assertEquals(0, database.expenseDao().count())
        assertEquals(0, database.itemCatalogDao().count())
        assertEquals(0L, repository.observeTotal(DateRange(date, date)).first())
    }
}
