package dev.groceryledger.domain.shopping

import dev.groceryledger.domain.repository.ExpenseRepository
import dev.groceryledger.domain.repository.ShoppingListRepository
import java.time.LocalDate

/**
 * Turns checked shopping-list items into entries. Items without an amount are left on the
 * list because every entry needs at least ₹1. Converted items are removed so the list
 * reflects what is still to be bought/recorded.
 */
class ShoppingListConverter(
    private val shoppingListRepository: ShoppingListRepository,
    private val expenseRepository: ExpenseRepository,
) {
    suspend fun convertChecked(date: LocalDate): Int {
        val priced = shoppingListRepository.checkedItems()
            .filter { (it.amountPaise ?: 0L) > 0L }

        priced.forEach { item ->
            expenseRepository.add(
                name = item.name,
                amountPaise = requireNotNull(item.amountPaise),
                date = date,
            )
        }
        priced.forEach { shoppingListRepository.delete(it.id) }
        return priced.size
    }
}
