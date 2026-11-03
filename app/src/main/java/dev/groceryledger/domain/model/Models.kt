package dev.groceryledger.domain.model

import java.time.LocalDate

data class Expense(
    val id: Long,
    val uid: String,
    val name: String,
    val amountPaise: Long,
    val date: LocalDate,
    val createdAt: Long,
    val updatedAt: Long,
    val category: String = Categories.UNCATEGORIZED,
    val quantityMilli: Long? = null,
    val unit: String? = null,
) {
    /** Normalized price per kg, litre or piece, when a quantity was recorded. */
    val unitPricePaise: Long?
        get() = UnitPrices.perBaseUnitPaise(
            amountPaise = amountPaise,
            quantityMilli = quantityMilli,
            unit = QuantityUnit.fromId(unit),
        )

    val unitPriceLabel: String?
        get() = QuantityUnit.fromId(unit)?.let { UnitPrices.priceUnitLabel(it) }
}

data class DailyTotal(
    val date: LocalDate,
    val totalPaise: Long,
    val itemCount: Int,
)

data class CategoryTotal(
    val name: String,
    val totalPaise: Long,
    val itemCount: Int,
)

data class ItemSuggestion(
    val name: String,
    val lastPricePaise: Long,
    val lastDate: LocalDate,
    val usageCount: Int,
    val lastUsedAt: Long,
)

/** A one-tap chip that restores the name, last price and last category of a recent item. */
data class RecentItem(
    val name: String,
    val lastPricePaise: Long,
    val category: String = Categories.UNCATEGORIZED,
)

data class SpendingSummary(
    val dayPaise: Long,
    val weekPaise: Long,
    val monthPaise: Long,
)
