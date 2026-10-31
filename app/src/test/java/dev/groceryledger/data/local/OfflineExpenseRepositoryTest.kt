package dev.groceryledger.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.groceryledger.domain.date.DateRange
import dev.groceryledger.domain.model.Categories
import dev.groceryledger.domain.model.Expense
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
            categoryDao = database.categoryDao(),
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
    fun categoryTotalsAggregateTheSelectedRange() = runBlocking {
        repository.add("Milk", 6_000, date, category = "Dairy")
        repository.add("Cheese", 4_000, date.plusDays(1), category = "Dairy")
        repository.add("Chips", 2_000, date, category = "Snacks")
        repository.add("Soap", 1_000, date)
        repository.add("Rice", 9_000, date.plusMonths(1), category = "Grains")

        val totals = repository.observeCategoryTotals(
            DateRange(date, date.plusDays(6)),
        ).first()

        assertEquals(listOf("Dairy", "Snacks", Categories.UNCATEGORIZED), totals.map { it.name })
        assertEquals(10_000L, totals[0].totalPaise)
        assertEquals(2, totals[0].itemCount)
        assertEquals(2_000L, totals[1].totalPaise)
        assertEquals(1_000L, totals[2].totalPaise)
    }

    @Test
    fun clearingAllRemovesEntriesAndSuggestions() = runBlocking {
        repository.add("Milk", 6_000, date)
        repository.clearAll()

        assertEquals(0, database.expenseDao().count())
        assertEquals(0, database.itemCatalogDao().count())
        assertEquals(0L, repository.observeTotal(DateRange(date, date)).first())
    }

    @Test
    fun renamingCategoryMovesItsEntries() = runBlocking {
        repository.add("Milk", 6_000, date, category = "Dairy & Eggs")

        assertTrue(repository.renameCategory("Dairy & Eggs", "Dairy"))

        assertEquals("Dairy", repository.observeEntries(date).first().single().category)
        val categories = repository.observeCategories().first()
        assertTrue(categories.contains("Dairy"))
        assertFalse(categories.contains("Dairy & Eggs"))
    }

    @Test
    fun renamingCategoryRejectsDuplicateAndReservedNames() = runBlocking {
        repository.add("Milk", 6_000, date, category = "Dairy")
        repository.add("Bread", 4_000, date, category = "Bakery")

        assertFalse(repository.renameCategory("Bakery", "dairy"))
        assertFalse(repository.renameCategory("Bakery", Categories.UNCATEGORIZED))
        assertFalse(repository.renameCategory("Bakery", "   "))

        val bread = repository.observeEntries(date).first().single { it.name == "Bread" }
        assertEquals("Bakery", bread.category)
    }

    @Test
    fun renamingCategoryCanChangeOnlyItsCase() = runBlocking {
        repository.add("Chips", 2_000, date, category = "Snacks")

        assertTrue(repository.renameCategory("Snacks", "snacks"))

        assertEquals("snacks", repository.observeEntries(date).first().single().category)
        assertTrue(repository.observeCategories().first().contains("snacks"))
    }

    @Test
    fun deletingCategoryMovesEntriesToUncategorized() = runBlocking {
        repository.add("Chips", 2_000, date, category = "Snacks")

        assertTrue(repository.deleteCategory("Snacks"))

        assertEquals(
            Categories.UNCATEGORIZED,
            repository.observeEntries(date).first().single().category,
        )
        assertFalse(repository.observeCategories().first().contains("Snacks"))
        assertFalse(repository.deleteCategory(Categories.UNCATEGORIZED))
        assertFalse(repository.deleteCategory("Does not exist"))
    }
}
