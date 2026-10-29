package dev.groceryledger.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.groceryledger.R
import dev.groceryledger.domain.date.DateRanges
import dev.groceryledger.domain.money.Money
import dev.groceryledger.domain.model.SpendingSummary
import dev.groceryledger.ui.CalendarDayUi
import dev.groceryledger.ui.CalendarUiState
import dev.groceryledger.ui.DayDetail
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    state: CalendarUiState,
    isDaySheetOpen: Boolean,
    snackbarHostState: SnackbarHostState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onSelectDate: (java.time.LocalDate) -> Unit,
    onAdd: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseDaySheet: () -> Unit,
    onAddFromDay: () -> Unit,
    onEditEntry: (dev.groceryledger.domain.model.Expense) -> Unit,
    entrySheet: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            val addDescription = stringResource(R.string.add_item)
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_item)) },
                modifier = Modifier.semantics { contentDescription = addDescription },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            MonthSelector(
                month = state.visibleMonth,
                today = state.today,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onToday = onToday,
            )

            SummaryCard(
                summary = state.summary,
                dayDetail = state.selectedDay,
                weekRange = DateRanges.formatWeekRange(state.weekRange),
                monthLabel = DateRanges.formatMonthTitle(state.visibleMonth),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))

            WeekdayHeader(weekStart = state.weekStart)

            MonthGrid(
                days = state.days,
                onSelectDate = onSelectDate,
            )

            Spacer(Modifier.height(88.dp))
        }
    }

    if (isDaySheetOpen) {
        DaySheet(
            detail = state.selectedDay,
            onDismiss = onCloseDaySheet,
            onAdd = onAddFromDay,
            onEdit = onEditEntry,
        )
    }

    entrySheet()
}

@Composable
private fun MonthSelector(
    month: YearMonth,
    today: java.time.LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
) {
    val canGoNext = month.isBefore(YearMonth.from(today))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.previous_month),
            )
        }
        Text(
            text = DateRanges.formatMonthTitle(month),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNextMonth, enabled = canGoNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.next_month),
            )
        }
        TextButton(onClick = onToday) {
            Text(stringResource(R.string.today))
        }
    }
}

@Composable
private fun SummaryCard(
    summary: SpendingSummary,
    dayDetail: DayDetail,
    weekRange: String,
    monthLabel: String,
    modifier: Modifier = Modifier,
) {
    val stacked = LocalDensity.current.fontScale >= 1.3f

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        if (stacked) {
            Column(modifier = Modifier.padding(18.dp)) {
                SummaryTile(
                    label = stringResource(R.string.summary_day),
                    amountPaise = summary.dayPaise,
                    caption = DateRanges.formatEditableDate(dayDetail.date),
                    modifier = Modifier.fillMaxWidth(),
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                SummaryTile(
                    label = stringResource(R.string.summary_week),
                    amountPaise = summary.weekPaise,
                    caption = weekRange,
                    modifier = Modifier.fillMaxWidth(),
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                SummaryTile(
                    label = stringResource(R.string.summary_month),
                    amountPaise = summary.monthPaise,
                    caption = monthLabel,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SummaryTile(
                    label = stringResource(R.string.summary_day),
                    amountPaise = summary.dayPaise,
                    caption = DateRanges.formatEditableDate(dayDetail.date),
                    modifier = Modifier.weight(1f),
                )
                VerticalDivider(
                    modifier = Modifier.height(56.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                )
                SummaryTile(
                    label = stringResource(R.string.summary_week),
                    amountPaise = summary.weekPaise,
                    caption = weekRange,
                    modifier = Modifier.weight(1f),
                )
                VerticalDivider(
                    modifier = Modifier.height(56.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                )
                SummaryTile(
                    label = stringResource(R.string.summary_month),
                    amountPaise = summary.monthPaise,
                    caption = monthLabel,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SummaryTile(
    label: String,
    amountPaise: Long,
    caption: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
        )
        Text(
            text = Money.format(amountPaise),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun WeekdayHeader(weekStart: dev.groceryledger.domain.model.WeekStart) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
    ) {
        DateRanges.weekdayHeaders(weekStart).forEach { day ->
            Text(
                text = day,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MonthGrid(
    days: List<CalendarDayUi>,
    onSelectDate: (java.time.LocalDate) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        days.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    DayCell(
                        day = day,
                        onClick = { onSelectDate(day.date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: CalendarDayUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    val isDimmed = !day.isCurrentMonth || day.isFuture
    val backgroundColor = when {
        day.isSelected -> MaterialTheme.colorScheme.primary
        day.isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        else -> Color.Transparent
    }
    val contentColor = when {
        day.isSelected -> MaterialTheme.colorScheme.onPrimary
        day.isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
        day.isCurrentMonth -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    }

    val itemCountLabel = pluralStringResource(R.plurals.items_count, day.itemCount, day.itemCount)
    val description = if (day.totalPaise > 0) {
        stringResource(
            R.string.calendar_day_description,
            DateRanges.formatFullDate(day.date),
            Money.format(day.totalPaise),
            itemCountLabel,
        )
    } else {
        stringResource(
            R.string.calendar_day_empty_description,
            DateRanges.formatFullDate(day.date),
        )
    }

    Column(
        modifier = modifier
            .padding(2.dp)
            .clip(shape)
            .background(backgroundColor)
            .then(
                if (day.isToday && !day.isSelected) {
                    Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary), shape)
                } else {
                    Modifier
                },
            )
            .clickable(
                enabled = !day.isFuture,
                onClickLabel = stringResource(R.string.select_date),
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {
                contentDescription = description
                role = Role.Button
            }
            .alpha(if (isDimmed && !day.isToday) 0.55f else 1f)
            .heightIn(min = 56.dp)
            .padding(vertical = 5.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
        )
        if (day.totalPaise > 0) {
            Text(
                text = Money.formatCompact(day.totalPaise),
                fontSize = 8.sp,
                lineHeight = 10.sp,
                color = if (day.isSelected) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
    }
}
