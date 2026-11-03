package dev.groceryledger.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShoppingListRepositoryTest {
    private lateinit var database: GroceryDatabase
    private lateinit var repository: OfflineShoppingListRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, GroceryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = OfflineShoppingListRepository(database.shoppingListDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun itemsCanBeAddedCheckedAndDeleted() = runBlocking {
        repository.add("Milk", 6_000)
        repository.add("Bread")

        val initial = repository.observeItems().first()
        assertEquals(listOf("Milk", "Bread"), initial.map { it.name })
        assertEquals(6_000L, initial.first().amountPaise)
        assertNull(initial.last().amountPaise)

        repository.setChecked(initial.first().id, true)
        val afterCheck = repository.observeItems().first()
        assertEquals(listOf("Bread", "Milk"), afterCheck.map { it.name })
        assertTrue(afterCheck.last().isChecked)
        assertEquals(listOf("Milk"), repository.checkedItems().map { it.name })

        repository.delete(initial.last().id)
        assertEquals(listOf("Milk"), repository.observeItems().first().map { it.name })

        repository.clear()
        assertTrue(repository.observeItems().first().isEmpty())
    }

    @Test
    fun invalidNamesAreRejectedAndNonPositiveAmountsAreDropped() = runBlocking {
        assertNull(repository.add("   "))
        assertNull(repository.add("x".repeat(81)))

        val item = repository.add("Milk", -100)
        assertEquals("Milk", item?.name)
        assertNull(item?.amountPaise)
    }
}
