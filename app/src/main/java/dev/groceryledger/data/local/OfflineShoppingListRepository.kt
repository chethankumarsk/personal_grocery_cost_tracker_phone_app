package dev.groceryledger.data.local

import dev.groceryledger.domain.model.ShoppingItem
import dev.groceryledger.domain.repository.ShoppingListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineShoppingListRepository(
    private val dao: ShoppingListDao,
) : ShoppingListRepository {

    override fun observeItems(): Flow<List<ShoppingItem>> =
        dao.observeAll().map { rows -> rows.map(::toDomain) }

    override suspend fun add(name: String, amountPaise: Long?): ShoppingItem? {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_NAME_LENGTH) return null
        val entity = ShoppingItemEntity(
            name = trimmed,
            amountPaise = amountPaise?.takeIf { it > 0L },
            isChecked = false,
            createdAt = System.currentTimeMillis(),
        )
        val id = dao.insert(entity)
        return toDomain(entity.copy(id = id))
    }

    override suspend fun setChecked(id: Long, checked: Boolean) = dao.setChecked(id, checked)

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun clear() = dao.clear()

    override suspend fun checkedItems(): List<ShoppingItem> = dao.checkedItems().map(::toDomain)

    private fun toDomain(entity: ShoppingItemEntity): ShoppingItem = ShoppingItem(
        id = entity.id,
        name = entity.name,
        amountPaise = entity.amountPaise,
        isChecked = entity.isChecked,
        createdAt = entity.createdAt,
    )

    private companion object {
        const val MAX_NAME_LENGTH = 80
    }
}
