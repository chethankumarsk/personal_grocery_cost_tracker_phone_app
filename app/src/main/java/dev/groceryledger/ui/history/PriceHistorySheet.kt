package dev.groceryledger.ui.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.groceryledger.R
import dev.groceryledger.domain.date.DateRanges
import dev.groceryledger.domain.history.PriceHistory
import dev.groceryledger.domain.history.PriceHistoryCalculator
import dev.groceryledger.domain.history.PricePoint
import dev.groceryledger.domain.history.PriceStats
import dev.groceryledger.domain.history.formatPercentTenths
import dev.groceryledger.domain.model.Expense
import dev.groceryledger.domain.money.Money
import dev.groceryledger.ui.PriceHistoryUiState

@Composable
fun PriceHistorySheet(
    state: PriceHistoryUiState,
    onDismiss: () -> Unit,
    onEditEntry: (Expense) -> Unit,
) {
    BackHandler(enabled = true) { onDismiss() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                    )
                }
            }

            val history = state.history
            when {
                history == null -> LoadingState()
                history.points.isEmpty() -> EmptyState()
                else -> HistoryContent(
                    history = history,
                    onEditEntry = onEditEntry,
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.price_history_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.price_history_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HistoryContent(
    history: PriceHistory,
    onEditEntry: (Expense) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("price_history_content"),
    ) {
        LastPaidHeader(history)
        Spacer(Modifier.height(16.dp))
        StatsGrid(history.stats)

        if (history.isChartable) {
            PriceTrendChart(
                points = PriceHistoryCalculator.sampleForChart(history.points),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(top = 16.dp),
            )
        }

        if (history.points.size >= HISTORY_LIMIT) {
            Text(
                text = stringResource(R.string.price_history_truncated),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (!history.isChartable) {
            Text(
                text = stringResource(R.string.price_history_single),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        history.points.forEachIndexed { index, point ->
            PurchaseRow(point = point, onEdit = { onEditEntry(point.expense) })
            if (index < history.points.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LastPaidHeader(history: PriceHistory) {
    val stats = history.stats
    Text(
        text = stringResource(R.string.last_paid_amount, Money.format(stats.latestPaise)),
        style = MaterialTheme.typography.headlineSmall,
    )

    val previous = stats.previousPaise
    val percent = stats.changePercentTenths
    val change = stats.changePaise
    val previousDate = history.points.getOrNull(1)?.date
    if (previous == null || percent == null || change == null || previousDate == null) return

    val previousText = Money.format(previous)
    val previousDateText = DateRanges.formatDayWithYear(previousDate)
    val (text, color) = when {
        change > 0 -> stringResource(
            R.string.price_change_up,
            Money.format(change),
            formatPercentTenths(percent),
            previousText,
            previousDateText,
        ) to MaterialTheme.colorScheme.error

        change < 0 -> stringResource(
            R.string.price_change_down,
            Money.format(-change),
            formatPercentTenths(percent),
            previousText,
            previousDateText,
        ) to MaterialTheme.colorScheme.primary

        else -> stringResource(
            R.string.price_change_same,
            previousText,
            previousDateText,
        ) to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun StatsGrid(stats: PriceStats) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCell(
                label = stringResource(R.string.price_history_average),
                value = Money.format(stats.averagePaise),
                modifier = Modifier.weight(1f),
            )
            StatCell(
                label = stringResource(R.string.price_history_low),
                value = Money.format(stats.minPaise),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCell(
                label = stringResource(R.string.price_history_high),
                value = Money.format(stats.maxPaise),
                modifier = Modifier.weight(1f),
            )
            StatCell(
                label = stringResource(R.string.price_history_purchases),
                value = pluralStringResource(
                    R.plurals.purchases_count,
                    stats.count,
                    stats.count,
                ),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun PriceTrendChart(
    points: List<PricePoint>,
    modifier: Modifier = Modifier,
) {
    if (points.size < 2) return

    val lineColor = MaterialTheme.colorScheme.primary
    val dotColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val topPad = 8.dp.toPx()
        val bottomPad = 8.dp.toPx()
        val chartHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)
        val min = points.minOf { it.amountPaise }
        val max = points.maxOf { it.amountPaise }
        val span = (max - min).coerceAtLeast(1L)
        val stepX = if (points.size > 1) size.width / (points.size - 1) else 0f

        fun yFor(paise: Long): Float =
            topPad + chartHeight - ((paise - min).toFloat() / span) * chartHeight

        val path = Path()
        points.forEachIndexed { index, point ->
            val x = index * stepX
            val y = yFor(point.amountPaise)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )

        points.forEachIndexed { index, point ->
            drawCircle(
                color = dotColor,
                radius = 3.dp.toPx(),
                center = Offset(index * stepX, yFor(point.amountPaise)),
            )
        }
    }
}

@Composable
private fun PurchaseRow(
    point: PricePoint,
    onEdit: () -> Unit,
) {
    val description = stringResource(
        R.string.price_history_edit_description,
        point.expense.name,
        DateRanges.formatDayWithYear(point.date),
        Money.format(point.amountPaise),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = description, onClick = onEdit)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = DateRanges.formatDayWithYear(point.date),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = Money.format(point.amountPaise),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** Mirrors the repository cap so the truncation note can be shown. */
private const val HISTORY_LIMIT = 500
