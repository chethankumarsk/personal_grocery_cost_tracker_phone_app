package dev.groceryledger.ui.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.groceryledger.R
import dev.groceryledger.domain.date.DateRanges
import dev.groceryledger.domain.model.Categories
import dev.groceryledger.domain.model.Expense
import dev.groceryledger.domain.money.Money
import dev.groceryledger.ui.DayDetail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaySheet(
    detail: DayDetail,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Expense) -> Unit,
    onHistory: (Expense) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = DateRanges.formatFullDate(detail.date),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "${pluralStringResource(R.plurals.items_count, detail.itemCount, detail.itemCount)} · ${
                    Money.format(detail.totalPaise)
                }",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(Modifier.height(16.dp))

            if (detail.entries.isEmpty()) {
                EmptyDayState(onAdd = onAdd)
            } else {
                detail.entries.forEach { entry ->
                    EntryRow(
                        entry = entry,
                        onClick = { onEdit(entry) },
                        onHistory = { onHistory(entry) },
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.day_total),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = Money.format(detail.totalPaise),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.add_item))
                }
            }
        }
    }
}

@Composable
private fun EmptyDayState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier
                .size(48.dp)
                .alpha(0.8f),
        )
        Text(
            text = stringResource(R.string.no_items_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = stringResource(R.string.no_items_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, start = 16.dp, end = 16.dp),
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAdd) {
            Text(stringResource(R.string.add_item))
        }
    }
}

@Composable
private fun EntryRow(
    entry: Expense,
    onClick: () -> Unit,
    onHistory: () -> Unit,
) {
    val description = stringResource(
        R.string.edit_item_description,
        entry.name,
        Money.format(entry.amountPaise),
    )
    val historyDescription = stringResource(R.string.price_history_for, entry.name)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(onClickLabel = description, onClick = onClick)
                .semantics(mergeDescendants = true) { contentDescription = description }
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (entry.category != Categories.UNCATEGORIZED) {
                    Text(
                        text = entry.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                entry.unitPricePaise?.let { perUnit ->
                    Text(
                        text = stringResource(
                            R.string.unit_price_compact,
                            Money.format(perUnit),
                            entry.unitPriceLabel.orEmpty(),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                text = Money.format(entry.amountPaise),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        }

        IconButton(
            onClick = onHistory,
            modifier = Modifier
                .testTag("entry_history")
                .semantics { contentDescription = historyDescription },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_price_history),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
