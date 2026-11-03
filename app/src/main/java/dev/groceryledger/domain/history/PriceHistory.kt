package dev.groceryledger.domain.history

import dev.groceryledger.domain.model.Expense
import kotlin.math.abs
import kotlin.math.roundToInt

/** A single recorded purchase of one item. */
data class PricePoint(val expense: Expense) {
    val date get() = expense.date
    val amountPaise get() = expense.amountPaise
}

/**
 * Aggregates for one item's purchases. [changePaise] and [changePercentTenths] compare the
 * latest purchase with the one before it and are null until there are two purchases.
 * Percentages are stored in tenths of a percent (67 == 6.7%) so money stays integer-only.
 */
data class PriceStats(
    val count: Int,
    val averagePaise: Long,
    val minPaise: Long,
    val maxPaise: Long,
    val latestPaise: Long,
    val previousPaise: Long?,
    val changePaise: Long?,
    val changePercentTenths: Long?,
) {
    val hasTrend: Boolean get() = previousPaise != null
}

/** One item's purchases, newest first, plus the derived totals. */
data class PriceHistory(
    val name: String,
    val points: List<PricePoint>,
    val stats: PriceStats,
) {
    val isChartable: Boolean get() = points.size >= 2
}

object PriceHistoryCalculator {
    /** Keeps charts readable on small screens; the list still shows every purchase. */
    const val MAX_CHART_POINTS = 200

    fun build(name: String, pointsNewestFirst: List<PricePoint>): PriceHistory =
        PriceHistory(
            name = name,
            points = pointsNewestFirst,
            stats = stats(pointsNewestFirst),
        )

    fun stats(points: List<PricePoint>): PriceStats {
        if (points.isEmpty()) {
            return PriceStats(
                count = 0,
                averagePaise = 0,
                minPaise = 0,
                maxPaise = 0,
                latestPaise = 0,
                previousPaise = null,
                changePaise = null,
                changePercentTenths = null,
            )
        }

        val amounts = points.map { it.amountPaise }
        val latest = amounts.first()
        val previous = amounts.getOrNull(1)
        val change = previous?.let { latest - it }

        return PriceStats(
            count = points.size,
            averagePaise = amounts.sum() / points.size,
            minPaise = amounts.min(),
            maxPaise = amounts.max(),
            latestPaise = latest,
            previousPaise = previous,
            changePaise = change,
            changePercentTenths = if (previous != null && previous > 0 && change != null) {
                percentTenths(change, previous)
            } else {
                null
            },
        )
    }

    /**
     * Oldest-to-newest points for the chart, evenly thinned when a history is very long.
     * The first and last purchases are always kept so the ends stay truthful.
     */
    fun sampleForChart(
        pointsNewestFirst: List<PricePoint>,
        maxPoints: Int = MAX_CHART_POINTS,
    ): List<PricePoint> {
        val chronological = pointsNewestFirst.asReversed()
        if (maxPoints <= 1 || chronological.size <= maxPoints) return chronological

        val step = (chronological.lastIndex).toDouble() / (maxPoints - 1)
        return (0 until maxPoints).map { index ->
            chronological[(index * step).roundToInt().coerceIn(chronological.indices)]
        }
    }

    /** Rounds half away from zero so +6.66% and -6.66% format symmetrically. */
    private fun percentTenths(changePaise: Long, previousPaise: Long): Long {
        val scaled = changePaise * 1_000
        val half = previousPaise / 2
        return if (scaled >= 0) {
            (scaled + half) / previousPaise
        } else {
            (scaled - half) / previousPaise
        }
    }
}

/** Formats tenths of a percent without a sign, e.g. 67 -> "6.7%". */
fun formatPercentTenths(tenths: Long): String {
    val magnitude = abs(tenths)
    return "${magnitude / 10}.${magnitude % 10}%"
}
