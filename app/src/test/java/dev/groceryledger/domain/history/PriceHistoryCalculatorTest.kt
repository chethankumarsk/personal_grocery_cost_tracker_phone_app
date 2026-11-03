package dev.groceryledger.domain.history

import dev.groceryledger.domain.model.Expense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PriceHistoryCalculatorTest {
    private val today = LocalDate.of(2026, 11, 3)

    private fun point(daysAgo: Long, paise: Long, name: String = "Milk"): PricePoint =
        PricePoint(
            Expense(
                id = daysAgo + 1,
                uid = "uid-$daysAgo-$paise",
                name = name,
                amountPaise = paise,
                date = today.minusDays(daysAgo),
                createdAt = daysAgo,
                updatedAt = daysAgo,
            ),
        )

    @Test
    fun emptyHistoryHasNoStats() {
        val history = PriceHistoryCalculator.build("Milk", emptyList())

        assertEquals(0, history.stats.count)
        assertEquals(0L, history.stats.averagePaise)
        assertNull(history.stats.previousPaise)
        assertNull(history.stats.changePaise)
        assertNull(history.stats.changePercentTenths)
        assertTrue(history.points.isEmpty())
    }

    @Test
    fun singlePurchaseHasNoTrend() {
        val stats = PriceHistoryCalculator.stats(listOf(point(0, 6_000)))

        assertEquals(1, stats.count)
        assertEquals(6_000L, stats.latestPaise)
        assertEquals(6_000L, stats.averagePaise)
        assertEquals(6_000L, stats.minPaise)
        assertEquals(6_000L, stats.maxPaise)
        assertNull(stats.previousPaise)
        assertNull(stats.changePaise)
        assertNull(stats.changePercentTenths)
    }

    @Test
    fun increasingPricesReportAPositiveChange() {
        val stats = PriceHistoryCalculator.stats(
            listOf(
                point(0, 6_400),
                point(10, 6_200),
                point(20, 6_000),
            ),
        )

        assertEquals(3, stats.count)
        assertEquals(6_400L, stats.latestPaise)
        assertEquals(6_200L, stats.previousPaise)
        assertEquals(200L, stats.changePaise)
        // 200 / 6200 = 3.2%
        assertEquals(32L, stats.changePercentTenths)
        assertEquals(6_200L, stats.averagePaise)
        assertEquals(6_000L, stats.minPaise)
        assertEquals(6_400L, stats.maxPaise)
    }

    @Test
    fun decreasingPricesReportANegativeChange() {
        val stats = PriceHistoryCalculator.stats(
            listOf(
                point(0, 6_400),
                point(10, 7_000),
            ),
        )

        assertEquals(-600L, stats.changePaise)
        // 600 / 7000 = 8.57% -> 8.6%
        assertEquals(-86L, stats.changePercentTenths)
    }

    @Test
    fun equalPricesAreNoChange() {
        val stats = PriceHistoryCalculator.stats(
            listOf(
                point(0, 6_000),
                point(10, 6_000),
            ),
        )

        assertEquals(0L, stats.changePaise)
        assertEquals(0L, stats.changePercentTenths)
    }

    @Test
    fun averageTruncatesPaise() {
        val stats = PriceHistoryCalculator.stats(
            listOf(
                point(0, 6_100),
                point(1, 6_100),
                point(2, 6_000),
            ),
        )

        // 18_200 / 3 = 6_066.66 -> 6_066
        assertEquals(6_066L, stats.averagePaise)
    }

    @Test
    fun percentFormattingAlwaysShowsOneDecimal() {
        assertEquals("6.7%", formatPercentTenths(67))
        assertEquals("8.6%", formatPercentTenths(-86))
        assertEquals("0.0%", formatPercentTenths(0))
        assertEquals("100.0%", formatPercentTenths(1_000))
    }

    @Test
    fun chartPointsAreChronologicalAndKeepTheEnds() {
        val points = (0L..4L).map { point(it, 6_000 + it) }

        val chart = PriceHistoryCalculator.sampleForChart(points, maxPoints = 10)

        // Newest first in storage, oldest first on the chart.
        assertEquals(points.last(), chart.first())
        assertEquals(points.first(), chart.last())
    }

    @Test
    fun longHistoriesAreThinnedWithoutLosingTheEnds() {
        val points = (0L..299L).map { point(it, 6_000 + it) }

        val chart = PriceHistoryCalculator.sampleForChart(points)

        assertEquals(PriceHistoryCalculator.MAX_CHART_POINTS, chart.size)
        assertEquals(points.last(), chart.first())
        assertEquals(points.first(), chart.last())
    }
}
