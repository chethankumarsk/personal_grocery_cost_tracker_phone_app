package dev.groceryledger.domain.date

import dev.groceryledger.domain.model.WeekStart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class DateRangesTest {
    private val october29 = LocalDate.of(2026, 10, 29)

    @Test
    fun mondayWeekContainsTheSelectedDate() {
        val range = DateRanges.weekRange(october29, WeekStart.MONDAY)
        assertEquals(LocalDate.of(2026, 10, 26), range.start)
        assertEquals(LocalDate.of(2026, 11, 1), range.endInclusive)
        assertTrue(range.contains(october29))
    }

    @Test
    fun sundayWeekStartsOnSunday() {
        val range = DateRanges.weekRange(october29, WeekStart.SUNDAY)
        assertEquals(LocalDate.of(2026, 10, 25), range.start)
        assertEquals(LocalDate.of(2026, 10, 31), range.endInclusive)
    }

    @Test
    fun weekCanCrossAYearBoundary() {
        val range = DateRanges.weekRange(LocalDate.of(2026, 1, 1), WeekStart.MONDAY)
        assertEquals(LocalDate.of(2025, 12, 29), range.start)
        assertEquals(LocalDate.of(2026, 1, 4), range.endInclusive)
    }

    @Test
    fun monthGridAlwaysHasSixWeeksStartingOnConfiguredDay() {
        val dates = DateRanges.monthGridDates(YearMonth.of(2026, 10), WeekStart.MONDAY)
        assertEquals(42, dates.size)
        assertEquals(LocalDate.of(2026, 9, 28), dates.first())
        assertEquals(LocalDate.of(2026, 11, 8), dates.last())
        assertEquals(28, dates.first().dayOfMonth)
    }

    @Test
    fun monthGridStartsSundayWhenConfigured() {
        val dates = DateRanges.monthGridDates(YearMonth.of(2026, 10), WeekStart.SUNDAY)
        assertEquals(LocalDate.of(2026, 9, 27), dates.first())
    }

    @Test
    fun formatsWeekRangesAndMonthTitles() {
        assertEquals("26 Oct – 1 Nov", DateRanges.formatWeekRange(
            DateRanges.weekRange(october29, WeekStart.MONDAY),
        ))
        assertEquals(
            "29 Dec 2025 – 4 Jan 2026",
            DateRanges.formatWeekRange(
                DateRanges.weekRange(LocalDate.of(2026, 1, 1), WeekStart.MONDAY),
            ),
        )
        assertEquals("October 2026", DateRanges.formatMonthTitle(YearMonth.of(2026, 10)))
        assertEquals("Oct 2026", DateRanges.formatMonthShort(YearMonth.of(2026, 10)))
        assertEquals("29 Oct", DateRanges.formatDay(october29))
    }

    @Test
    fun weekdayHeadersFollowWeekStart() {
        assertEquals(
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
            DateRanges.weekdayHeaders(WeekStart.MONDAY),
        )
        assertEquals(
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"),
            DateRanges.weekdayHeaders(WeekStart.SUNDAY),
        )
    }

    @Test
    fun utcDateConversionRoundTrips() {
        assertEquals(october29, DateRanges.fromUtcMillis(DateRanges.toUtcMillis(october29)))
    }
}
