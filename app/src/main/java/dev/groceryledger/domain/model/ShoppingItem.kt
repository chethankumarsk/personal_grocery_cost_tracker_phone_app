package dev.groceryledger.domain.model

/** A scratch item on the shopping list, optionally with a planned/actual amount. */
data class ShoppingItem(
    val id: Long,
    val name: String,
    val amountPaise: Long? = null,
    val isChecked: Boolean = false,
    val createdAt: Long,
)
