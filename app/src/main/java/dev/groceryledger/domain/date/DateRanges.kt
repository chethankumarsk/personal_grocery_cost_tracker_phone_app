package dev.groceryledger.domain.date

import dev.groceryledger.domain.model.WeekStart
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class DateRange(
    val start: LocalDate,
    val endInclusive: LocalDate,
) {
    init {
        require(!endInclusive.isBefore(start)) { "End date must not be before start date" }
    }

    fun contains(date: LocalDate): Boolean =
        !date.isBefore(start) && !date.isAfter(endInclusive)
}

object DateRanges {
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private val monthTitle = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    private val monthShort = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)
    private val fullDate = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)
    private val editableDate = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)

    fun weekRange(date: LocalDate, weekStart: WeekStart): DateRange =
        weekRange(date, weekStart.dayOfWeek)

    fun weekRange(date: LocalDate, weekStart: DayOfWeek): DateRange {
        val start = date.with(TemporalAdjusters.previousOrSame(weekStart))
        return DateRange(start, start.plusDays(6))
    }

    fun monthRange(month: YearMonth): DateRange =
        DateRange(month.atDay(1), month.atEndOfMonth())

    /** Always 42 dates (six rows), starting on the configured week start. */
    fun monthGridDates(month: YearMonth, weekStart: WeekStart): List<LocalDate> {
        val first = month.atDay(1)
        val gridStart = first.with(TemporalAdjusters.previousOrSame(weekStart.dayOfWeek))
        return (0 until 42).map { gridStart.plusDays(it.toLong()) }
    }

    fun monthGridRange(month: YearMonth, weekStart: WeekStart): DateRange {
        val dates = monthGridDates(month, weekStart)
        return DateRange(dates.first(), dates.last())
    }

    fun formatDay(date: LocalDate): String = date.format(dayMonth)

    fun formatDayWithYear(date: LocalDate): String = date.format(dayMonthYear)

    fun formatFullDate(date: LocalDate): String = date.format(fullDate)

    fun formatEditableDate(date: LocalDate): String = date.format(editableDate)

    fun formatMonthTitle(month: YearMonth): String = month.format(monthTitle)

    fun formatMonthShort(month: YearMonth): String = month.format(monthShort)

    fun formatWeekRange(range: DateRange): String =
        if (range.start.year == range.endInclusive.year) {
            "${range.start.format(dayMonth)} – ${range.endInclusive.format(dayMonth)}"
        } else {
            "${range.start.format(dayMonthYear)} – ${range.endInclusive.format(dayMonthYear)}"
        }

    fun weekdayHeaders(weekStart: WeekStart): List<String> {
        val days = DayOfWeek.entries
        val startIndex = days.indexOf(weekStart.dayOfWeek)
        return List(7) { offset ->
            days[(startIndex + offset) % 7].getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
        }
    }

    fun toUtcMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun fromUtcMillis(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
}
