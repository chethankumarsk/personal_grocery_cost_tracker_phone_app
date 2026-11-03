package dev.groceryledger.domain.repository

import dev.groceryledger.domain.model.ShoppingItem
import kotlinx.coroutines.flow.Flow

/**
 * The shopping list is a scratch pad: it is stored on the device only and is deliberately
 * not part of CSV backups. Checked items can be turned into entries and removed.
 */
interface ShoppingListRepository {
    fun observeItems(): Flow<List<ShoppingItem>>

    /** Returns null for a blank or oversized name; a non-positive amount is dropped. */
    suspend fun add(name: String, amountPaise: Long? = null): ShoppingItem?

    suspend fun setChecked(id: Long, checked: Boolean)
    suspend fun delete(id: Long)
    suspend fun clear()
    suspend fun checkedItems(): List<ShoppingItem>
}
