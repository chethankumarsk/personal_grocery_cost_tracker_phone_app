package dev.groceryledger.domain.model

/** Optional unit for a purchase; the amount stays the total price of the whole quantity. */
enum class QuantityUnit(val id: String, val dimension: Dimension, val millisPerUnit: Long) {
    KILOGRAM("kg", Dimension.WEIGHT, 1_000L),
    GRAM("g", Dimension.WEIGHT, 1L),
    LITRE("L", Dimension.VOLUME, 1_000L),
    MILLILITRE("ml", Dimension.VOLUME, 1L),
    PIECE("piece", Dimension.COUNT, 1L),
    ;

    enum class Dimension { WEIGHT, VOLUME, COUNT }

    companion object {
        fun fromId(id: String?): QuantityUnit? =
            id?.let { value -> entries.firstOrNull { it.id.equals(value.trim(), ignoreCase = true) } }

        /** The unit a normalized price is shown in: ₹/kg, ₹/L or ₹/piece. */
        fun priceUnitFor(dimension: Dimension): String = when (dimension) {
            Dimension.WEIGHT -> KILOGRAM.id
            Dimension.VOLUME -> LITRE.id
            Dimension.COUNT -> PIECE.id
        }
    }
}

/**
 * Quantities are stored in thousandths of the chosen unit, so 1.5 kg is 1_500 and
 * 500 ml is 500_000. Keeps decimals out of floating point, like money.
 */
object Quantities {
    const val MILLIS = 1_000L
    const val MAX_MILLIS = 1_000_000_000L

    /** Parses "1", "1.5", "0.25" with up to three decimals. Returns null when invalid. */
    fun parse(text: String): Long? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        val parts = trimmed.split('.')
        if (parts.size > 2) return null
        val whole = parts[0]
        val fraction = parts.getOrNull(1).orEmpty()
        if (whole.isEmpty() && fraction.isEmpty()) return null
        if (whole.any { !it.isDigit() } || fraction.any { !it.isDigit() }) return null
        if (fraction.length > 3) return null

        val wholeValue = whole.ifEmpty { "0" }.toLongOrNull() ?: return null
        val fractionMillis = fraction.padEnd(3, '0').ifEmpty { "0" }.toLongOrNull() ?: return null
        val millis = wholeValue * MILLIS + fractionMillis
        return millis.takeIf { it in 1..MAX_MILLIS }
    }

    /** "1.5", "0.25", "500" - trailing zeros are dropped. */
    fun format(millis: Long): String {
        val whole = millis / MILLIS
        val fraction = millis % MILLIS
        if (fraction == 0L) return whole.toString()

        val fractionText = fraction.toString().padStart(3, '0').trimEnd('0')
        return "$whole.$fractionText"
    }
}

/** Normalized unit price, e.g. ₹40/kg for 2 kg at ₹80. */
object UnitPrices {
    /**
     * Price in paise per kg, litre or piece, rounded half up. Null when the quantity is
     * missing or zero.
     */
    fun perBaseUnitPaise(
        amountPaise: Long,
        quantityMilli: Long?,
        unit: QuantityUnit?,
    ): Long? {
        if (quantityMilli == null || quantityMilli <= 0L || unit == null) return null
        val baseMillis = quantityMilli * unit.millisPerUnit
        if (baseMillis <= 0L) return null

        // Weight and volume are shown per kg/L (1000 base units); count is per piece.
        val scale = when (unit.dimension) {
            QuantityUnit.Dimension.WEIGHT, QuantityUnit.Dimension.VOLUME -> 1_000_000L
            QuantityUnit.Dimension.COUNT -> 1_000L
        }
        return (amountPaise * scale + baseMillis / 2) / baseMillis
    }

    fun priceUnitLabel(unit: QuantityUnit): String =
        QuantityUnit.priceUnitFor(unit.dimension)
}
