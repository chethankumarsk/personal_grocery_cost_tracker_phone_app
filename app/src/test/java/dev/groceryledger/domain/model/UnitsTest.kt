package dev.groceryledger.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnitsTest {
    @Test
    fun parsesQuantitiesWithUpToThreeDecimals() {
        assertEquals(1_000L, Quantities.parse("1"))
        assertEquals(1_500L, Quantities.parse("1.5"))
        assertEquals(250L, Quantities.parse("0.25"))
        assertEquals(1_250L, Quantities.parse("1.25"))
        assertEquals(500_000L, Quantities.parse("500"))
        assertEquals(1_000L, Quantities.parse("1."))
    }

    @Test
    fun rejectsInvalidQuantities() {
        assertNull(Quantities.parse(""))
        assertNull(Quantities.parse("0"))
        assertNull(Quantities.parse("1.2345"))
        assertNull(Quantities.parse("-1"))
        assertNull(Quantities.parse("abc"))
        assertNull(Quantities.parse("1,5"))
    }

    @Test
    fun formatsQuantitiesWithoutTrailingZeros() {
        assertEquals("1", Quantities.format(1_000))
        assertEquals("1.5", Quantities.format(1_500))
        assertEquals("0.25", Quantities.format(250))
        assertEquals("500", Quantities.format(500_000))
        assertEquals("1.25", Quantities.format(1_250))
    }

    @Test
    fun unitPricesNormalizeToKilogramsLitresAndPieces() {
        // 2 kg for ₹80 -> ₹40/kg
        assertEquals(
            4_000L,
            UnitPrices.perBaseUnitPaise(8_000, 2_000, QuantityUnit.KILOGRAM),
        )
        // 500 g for ₹40 -> ₹80/kg
        assertEquals(
            8_000L,
            UnitPrices.perBaseUnitPaise(4_000, 500_000, QuantityUnit.GRAM),
        )
        // 1.5 L for ₹90 -> ₹60/L
        assertEquals(
            6_000L,
            UnitPrices.perBaseUnitPaise(9_000, 1_500, QuantityUnit.LITRE),
        )
        // 750 ml for ₹45 -> ₹60/L
        assertEquals(
            6_000L,
            UnitPrices.perBaseUnitPaise(4_500, 750_000, QuantityUnit.MILLILITRE),
        )
        // 3 pieces for ₹60 -> ₹20/piece
        assertEquals(
            2_000L,
            UnitPrices.perBaseUnitPaise(6_000, 3_000, QuantityUnit.PIECE),
        )
    }

    @Test
    fun unitPriceNeedsBothParts() {
        assertNull(UnitPrices.perBaseUnitPaise(8_000, null, QuantityUnit.KILOGRAM))
        assertNull(UnitPrices.perBaseUnitPaise(8_000, 2_000, null))
        assertNull(UnitPrices.perBaseUnitPaise(8_000, 0, QuantityUnit.KILOGRAM))
    }

    @Test
    fun priceUnitFollowsTheDimension() {
        assertEquals("kg", UnitPrices.priceUnitLabel(QuantityUnit.GRAM))
        assertEquals("L", UnitPrices.priceUnitLabel(QuantityUnit.MILLILITRE))
        assertEquals("piece", UnitPrices.priceUnitLabel(QuantityUnit.PIECE))
    }
}
