package dev.groceryledger.domain.money

import java.math.BigDecimal

sealed interface MoneyParseResult {
    data class Valid(val paise: Long) : MoneyParseResult
    data object Empty : MoneyParseResult
    data object Invalid : MoneyParseResult
    data object TooLarge : MoneyParseResult
}

object Money {
    /** ₹10,00,000 per entry. */
    const val MAX_AMOUNT_PAISE: Long = 100_000_000L

    private val amountPattern = Regex("""^\d{1,9}(\.\d{1,2})?$""")

    fun parse(raw: String): MoneyParseResult {
        val cleaned = raw
            .trim()
            .replace("₹", "")
            .replace(",", "")
            .replace(" ", "")

        if (cleaned.isEmpty()) return MoneyParseResult.Empty
        if (!amountPattern.matches(cleaned)) return MoneyParseResult.Invalid

        val value = cleaned.toBigDecimal()
        if (value <= BigDecimal.ZERO) return MoneyParseResult.Invalid

        val paise = value.movePointRight(2).longValueExact()
        return if (paise > MAX_AMOUNT_PAISE) {
            MoneyParseResult.TooLarge
        } else {
            MoneyParseResult.Valid(paise)
        }
    }

    /** Exact display amount with Indian digit grouping, e.g. ₹1,23,456.78. */
    fun format(paise: Long): String {
        val rupees = paise / 100
        val fraction = paise % 100
        val grouped = groupIndian(rupees)
        return if (fraction == 0L) {
            "₹$grouped"
        } else {
            "₹$grouped.${fraction.toString().padStart(2, '0')}"
        }
    }

    /** Editable amount text without separators, e.g. 60 or 60.50. */
    fun formatEditable(paise: Long): String =
        if (paise % 100 == 0L) (paise / 100).toString() else formatPlain(paise)

    /** CSV amount, always two decimals, e.g. 60.00. */
    fun formatPlain(paise: Long): String {
        val rupees = paise / 100
        val fraction = (paise % 100).toString().padStart(2, '0')
        return "$rupees.$fraction"
    }

    /**
     * Compact display used inside calendar cells. Values are rounded to the
     * nearest rupee, then shortened (₹850, ₹1.2k, ₹1.3L, ₹1.2Cr).
     */
    fun formatCompact(paise: Long): String {
        val rupees = (paise + 50) / 100
        return when {
            rupees >= 10_000_000 -> "₹${formatTenths((rupees + 500_000) / 1_000_000)}Cr"
            rupees >= 100_000 -> "₹${formatTenths((rupees + 5_000) / 10_000)}L"
            rupees >= 1_000 -> "₹${formatTenths((rupees + 50) / 100)}k"
            else -> "₹${groupIndian(rupees)}"
        }
    }

    fun groupIndian(value: Long): String {
        val digits = value.toString()
        if (digits.length <= 3) return digits

        val lastThree = digits.takeLast(3)
        val rest = digits.dropLast(3)
        val groups = mutableListOf<String>()
        var end = rest.length
        while (end > 2) {
            groups.add(0, rest.substring(end - 2, end))
            end -= 2
        }
        groups.add(0, rest.substring(0, end))
        return groups.joinToString(",") + "," + lastThree
    }

    private fun formatTenths(tenths: Long): String =
        if (tenths % 10 == 0L) (tenths / 10).toString() else "${tenths / 10}.${tenths % 10}"
}
