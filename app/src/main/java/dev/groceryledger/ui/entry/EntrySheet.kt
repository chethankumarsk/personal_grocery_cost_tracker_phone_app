package dev.groceryledger.ui.entry

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.groceryledger.R
import dev.groceryledger.domain.date.DateRanges
import dev.groceryledger.domain.model.Categories
import dev.groceryledger.domain.model.ItemSuggestion
import dev.groceryledger.domain.model.RecentItem
import dev.groceryledger.domain.money.Money
import dev.groceryledger.ui.AmountError
import dev.groceryledger.ui.EditorMode
import dev.groceryledger.ui.EditorUiState
import dev.groceryledger.ui.NameError
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrySheet(
    editor: EditorUiState,
    suggestions: List<ItemSuggestion>,
    recentItems: List<RecentItem>,
    categories: List<String>,
    today: LocalDate,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onCategoryChange: (String) -> Unit,
    onAddCategory: (String) -> Unit,
    onSuggestionSelected: (ItemSuggestion) -> Unit,
    onUseLastPrice: (Long) -> Unit,
    onRecentItemSelected: (RecentItem) -> Unit,
    onOpenPriceHistory: () -> Unit,
    onSave: () -> Unit,
    onSaveAndAddAnother: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var nameFocused by remember { mutableStateOf(false) }

    val allCategories = remember(categories) {
        listOf(Categories.UNCATEGORIZED) + categories.filter { it != Categories.UNCATEGORIZED }
    }

    // The form used to live in a ModalBottomSheet, i.e. its own dialog window. On some
    // devices that window is torn down and re-created while the keyboard is up, which made
    // the sheet vanish and re-appear mid-typing. Rendering it inside the activity window
    // removes that failure mode entirely.
    val requestClose = {
        if (editor.isDirty) showDiscardDialog = true else onDismiss()
    }

    BackHandler(enabled = true) { requestClose() }

    val nameFocus = remember { FocusRequester() }
    val amountFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        runCatching { nameFocus.requestFocus() }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                // In-window editor: system bars and the IME are handled by the activity
                // window, where inset dispatch is reliable.
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(
                        if (editor.mode == EditorMode.ADD) R.string.add_entry else R.string.edit_entry,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = requestClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Only the fields scroll. The actions stay pinned, so Save remains reachable
            // with the keyboard open even on a short screen.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = editor.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.item_name)) },
                    placeholder = { Text(stringResource(R.string.item_name_hint)) },
                    singleLine = true,
                    enabled = !editor.isSaving,
                    isError = editor.nameError != null,
                    supportingText = editor.nameError?.let { error ->
                        {
                            Text(
                                when (error) {
                                    NameError.REQUIRED -> stringResource(R.string.error_name_required)
                                    NameError.TOO_LONG -> stringResource(R.string.error_name_too_long)
                                },
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { amountFocus.requestFocus() },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(nameFocus)
                        .onFocusChanged { nameFocused = it.isFocused }
                        .testTag("entry_name"),
                )

                if (nameFocused && editor.showSuggestions && suggestions.isNotEmpty()) {
                    SuggestionPanel(
                        suggestions = suggestions,
                        onSelectName = { suggestion ->
                            onSuggestionSelected(suggestion)
                            amountFocus.requestFocus()
                        },
                        onUsePrice = { suggestion ->
                            onUseLastPrice(suggestion.lastPricePaise)
                            amountFocus.requestFocus()
                        },
                    )
                }

                if (editor.mode == EditorMode.ADD &&
                    editor.name.isBlank() &&
                    recentItems.isNotEmpty() &&
                    !editor.isSaving
                ) {
                    RecentItemChips(
                        items = recentItems,
                        onSelect = { item ->
                            onRecentItemSelected(item)
                            amountFocus.requestFocus()
                        },
                    )
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = editor.amountText,
                    onValueChange = onAmountChange,
                    label = { Text(stringResource(R.string.amount)) },
                    placeholder = { Text(stringResource(R.string.amount_hint)) },
                    prefix = { Text(stringResource(R.string.currency_symbol)) },
                    singleLine = true,
                    enabled = !editor.isSaving,
                    isError = editor.amountError != null,
                    supportingText = editor.amountError?.let { error ->
                        {
                            Text(
                                when (error) {
                                    AmountError.REQUIRED -> stringResource(R.string.error_amount_required)
                                    AmountError.INVALID -> stringResource(R.string.error_amount_invalid)
                                    AmountError.TOO_LARGE -> stringResource(R.string.error_amount_too_large)
                                },
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onSave() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(amountFocus)
                        .testTag("entry_amount"),
                )

                if (editor.mode == EditorMode.EDIT && !editor.isDirty) {
                    TextButton(
                        onClick = {
                            focusManager.clearFocus()
                            onOpenPriceHistory()
                        },
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .testTag("entry_price_history"),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_price_history),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.price_history))
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.category),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    allCategories.forEach { category ->
                        FilterChip(
                            selected = editor.category == category,
                            onClick = { onCategoryChange(category) },
                            enabled = !editor.isSaving,
                            label = {
                                Text(
                                    if (category == Categories.UNCATEGORIZED) {
                                        stringResource(R.string.category_uncategorized)
                                    } else {
                                        category
                                    },
                                )
                            },
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    AssistChip(
                        onClick = { showCategoryDialog = true },
                        enabled = !editor.isSaving,
                        label = { Text("+ " + stringResource(R.string.new_category)) },
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.date),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    enabled = !editor.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                ) {
                    Text(
                        text = DateRanges.formatEditableDate(editor.date),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.change_date),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }

                Spacer(Modifier.height(12.dp))
            }

            Button(
                onClick = onSave,
                enabled = !editor.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("entry_save"),
            ) {
                if (editor.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    stringResource(if (editor.isSaving) R.string.saving else R.string.save),
                )
            }

            if (editor.mode == EditorMode.ADD) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
                TextButton(
                    onClick = onSaveAndAddAnother,
                    enabled = !editor.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.save_and_add_another))
                }
            }

            if (editor.mode == EditorMode.EDIT) {
                TextButton(
                    onClick = onDelete,
                    enabled = !editor.isSaving,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.delete))
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }

    if (showCategoryDialog) {
        var newCategory by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text(stringResource(R.string.new_category)) },
            text = {
                OutlinedTextField(
                    value = newCategory,
                    onValueChange = { newCategory = it.take(Categories.MAX_NAME_LENGTH) },
                    label = { Text(stringResource(R.string.category_name)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAddCategory(newCategory)
                        showCategoryDialog = false
                    },
                ) {
                    Text(stringResource(R.string.add_category))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateRanges.toUtcMillis(editor.date),
            selectableDates = remember(today) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                        !DateRanges.fromUtcMillis(utcTimeMillis).isAfter(today)

                    override fun isSelectableYear(year: Int): Boolean = year <= today.year
                }
            },
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateChange(DateRanges.fromUtcMillis(millis))
                        }
                        showDatePicker = false
                    },
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.discard_changes_title)) },
            text = { Text(stringResource(R.string.discard_changes_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onDismiss()
                    },
                ) {
                    Text(stringResource(R.string.discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun RecentItemChips(
    items: List<RecentItem>,
    onSelect: (RecentItem) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .testTag("entry_recent_items"),
    ) {
        Text(
            text = stringResource(R.string.recent_items),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items.forEach { item ->
                AssistChip(
                    onClick = { onSelect(item) },
                    label = {
                        Text(
                            text = item.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    modifier = Modifier.testTag("entry_recent_item"),
                )
            }
        }
    }
}

@Composable
private fun SuggestionPanel(
    suggestions: List<ItemSuggestion>,
    onSelectName: (ItemSuggestion) -> Unit,
    onUsePrice: (ItemSuggestion) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            // A bounded height (rather than a fixed one) keeps the list tidy for one match,
            // while the scroll inside keeps the sheet frame stable for many.
            .heightIn(max = SUGGESTION_PANEL_MAX_HEIGHT),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.previous_items),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
            suggestions.forEachIndexed { index, suggestion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectName(suggestion) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = suggestion.name,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(
                                R.string.last_paid_on,
                                Money.format(suggestion.lastPricePaise),
                                DateRanges.formatDay(suggestion.lastDate),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    AssistChip(
                        onClick = { onUsePrice(suggestion) },
                        label = {
                            Text(
                                text = stringResource(
                                    R.string.use_last_price,
                                    Money.format(suggestion.lastPricePaise),
                                ),
                                fontWeight = FontWeight.Medium,
                            )
                        },
                    )
                }
                if (index < suggestions.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }
}

/**
 * Upper bound for the suggestion panel. The editor no longer resizes with its content,
 * so the panel can size to its matches (one match is a short panel, six scroll).
 */
private val SUGGESTION_PANEL_MAX_HEIGHT = 232.dp
