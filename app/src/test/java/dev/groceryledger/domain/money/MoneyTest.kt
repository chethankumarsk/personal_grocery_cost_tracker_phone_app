package dev.groceryledger.domain.money

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {
    @Test
    fun formatsWithIndianDigitGrouping() {
        assertEquals("₹1,23,456.78", Money.format(12_345_678))
        assertEquals("₹1,00,000", Money.format(10_000_000))
        assertEquals("₹60", Money.format(6_000))
        assertEquals("₹60.50", Money.format(6_050))
        assertEquals("₹0", Money.format(0))
    }

    @Test
    fun parsesWholeRupeesAndPaise() {
        assertEquals(MoneyParseResult.Valid(6_000), Money.parse("60"))
        assertEquals(MoneyParseResult.Valid(6_050), Money.parse("60.5"))
        assertEquals(MoneyParseResult.Valid(123_450), Money.parse("₹1,234.50"))
        assertEquals(MoneyParseResult.Valid(100_000_000), Money.parse("1000000"))
    }

    @Test
    fun rejectsInvalidOrOutOfRangeAmounts() {
        assertEquals(MoneyParseResult.Empty, Money.parse("  "))
        assertEquals(MoneyParseResult.Invalid, Money.parse("0"))
        assertEquals(MoneyParseResult.Invalid, Money.parse("-1"))
        assertEquals(MoneyParseResult.Invalid, Money.parse("60.555"))
        assertEquals(MoneyParseResult.TooLarge, Money.parse("1000000.01"))
    }

    @Test
    fun formatsCompactAmountsForCells() {
        assertEquals("₹850", Money.formatCompact(85_000))
        assertEquals("₹1.2k", Money.formatCompact(123_400))
        assertEquals("₹1.3L", Money.formatCompact(12_500_000))
        assertEquals("₹1.2Cr", Money.formatCompact(1_234_567_890))
        assertEquals("₹1k", Money.formatCompact(100_000))
    }

    @Test
    fun formatsEditableAndPlainAmounts() {
        assertEquals("60", Money.formatEditable(6_000))
        assertEquals("60.50", Money.formatEditable(6_050))
        assertEquals("60.00", Money.formatPlain(6_000))
        assertEquals("60.05", Money.formatPlain(6_005))
    }

    @Test
    fun groupsIndianNumbers() {
        assertEquals("0", Money.groupIndian(0))
        assertEquals("999", Money.groupIndian(999))
        assertEquals("1,000", Money.groupIndian(1_000))
        assertEquals("1,00,000", Money.groupIndian(100_000))
        assertEquals("12,34,567", Money.groupIndian(1_234_567))
    }
}
