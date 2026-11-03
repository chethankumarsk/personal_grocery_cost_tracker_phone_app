package dev.groceryledger.domain.shopping

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.groceryledger.data.local.GroceryDatabase
import dev.groceryledger.data.local.OfflineExpenseRepository
import dev.groceryledger.data.local.OfflineShoppingListRepository
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShoppingListConverterTest {
    private lateinit var database: GroceryDatabase
    private lateinit var shoppingListRepository: OfflineShoppingListRepository
    private lateinit var expenseRepository: OfflineExpenseRepository
    private lateinit var converter: ShoppingListConverter

    private val date = LocalDate.of(2026, 11, 3)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, GroceryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        shoppingListRepository = OfflineShoppingListRepository(database.shoppingListDao())
        expenseRepository = OfflineExpenseRepository(
            database = database,
            expenseDao = database.expenseDao(),
            catalogDao = database.itemCatalogDao(),
            categoryDao = database.categoryDao(),
        )
        converter = ShoppingListConverter(shoppingListRepository, expenseRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun convertsOnlyCheckedItemsThatHaveAnAmount() = runBlocking {
        val milk = shoppingListRepository.add("Milk", 6_000)!!
        val bread = shoppingListRepository.add("Bread", 4_000)!!
        val soap = shoppingListRepository.add("Soap")!!

        shoppingListRepository.setChecked(milk.id, true)
        shoppingListRepository.setChecked(bread.id, true)
        shoppingListRepository.setChecked(soap.id, true)

        val converted = converter.convertChecked(date)

        assertEquals(2, converted)
        val entries = expenseRepository.observeEntries(date).first()
        assertEquals(setOf("Milk", "Bread"), entries.map { it.name }.toSet())
        assertEquals(10_000L, entries.sumOf { it.amountPaise })

        val remaining = shoppingListRepository.observeItems().first()
        assertEquals(listOf("Soap"), remaining.map { it.name })
        assertTrue(remaining.single().isChecked)
    }

    @Test
    fun nothingCheckedConvertsNothing() = runBlocking {
        shoppingListRepository.add("Milk", 6_000)

        assertEquals(0, converter.convertChecked(date))
        assertTrue(expenseRepository.observeEntries(date).first().isEmpty())
    }
}
