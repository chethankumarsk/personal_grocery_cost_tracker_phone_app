package dev.groceryledger.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.groceryledger.AppContainer
import dev.groceryledger.R
import dev.groceryledger.ui.calendar.CalendarScreen
import dev.groceryledger.ui.entry.EntrySheet
import dev.groceryledger.ui.history.PriceHistorySheet
import dev.groceryledger.ui.settings.SettingsScreen
import dev.groceryledger.ui.theme.GroceryLedgerTheme
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun GroceryLedgerApp(container: AppContainer) {
    val viewModel: MainViewModel = viewModel(
        factory = remember(container) { MainViewModelFactory(container) },
    )
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    GroceryLedgerTheme(themeMode = settings.theme) {
        AppContent(viewModel = viewModel, settings = settings)
    }
}

@Composable
private fun AppContent(
    viewModel: MainViewModel,
    settings: dev.groceryledger.domain.model.AppSettings,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    val calendarState by viewModel.calendarState.collectAsStateWithLifecycle()
    val isDaySheetOpen by viewModel.isDaySheetOpen.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val recentItems by viewModel.recentItems.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val importPreview by viewModel.importPreview.collectAsStateWithLifecycle()
    val importFailure by viewModel.importFailure.collectAsStateWithLifecycle()
    val isImportBusy by viewModel.isImportBusy.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val priceHistory by viewModel.priceHistory.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshToday()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.events.collect { event ->
            val message = event.message.resolve(context)
            val actionLabel = event.action?.let { context.getString(R.string.undo) }
            val result = if (event.action == SnackbarAction.UNDO) {
                withTimeoutOrNull(5_000) {
                    snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = actionLabel,
                        duration = SnackbarDuration.Indefinite,
                    )
                } ?: run {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    SnackbarResult.Dismissed
                }
            } else {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short,
                )
            }

            if (result == SnackbarResult.ActionPerformed && event.deletedEntry != null) {
                viewModel.restoreDeletedEntry(event.deletedEntry)
            }
        }
    }

    BackHandler(enabled = showSettings) {
        showSettings = false
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        val historyState = priceHistory
        if (showSettings) {
            SettingsScreen(
                settings = settings,
                today = calendarState.today,
                categories = categories,
                importPreview = importPreview,
                importFailure = importFailure,
                isImportBusy = isImportBusy,
                updateState = updateState,
                snackbarHostState = snackbarHostState,
                onBack = { showSettings = false },
                onWeekStartChange = viewModel::setWeekStart,
                onThemeChange = viewModel::setTheme,
                onRenameCategory = viewModel::renameCategory,
                onDeleteCategory = viewModel::deleteCategory,
                onExport = viewModel::exportTo,
                onImport = viewModel::analyzeImport,
                onConfirmImport = viewModel::confirmImport,
                onCancelImport = viewModel::cancelImport,
                onDismissImportFailure = viewModel::clearImportFailure,
                onDeleteAll = viewModel::deleteAllData,
                onCheckForUpdates = viewModel::checkForUpdates,
                onDownloadUpdate = viewModel::downloadAndInstallUpdate,
                onInstallUpdate = viewModel::installDownloadedUpdate,
                onCancelUpdateDownload = viewModel::cancelUpdateDownload,
                onDismissUpdate = viewModel::dismissUpdateState,
            )
        } else if (historyState != null) {
            PriceHistorySheet(
                state = historyState,
                onDismiss = viewModel::closePriceHistory,
                onEditEntry = viewModel::editFromPriceHistory,
            )
        } else {
            CalendarScreen(
                state = calendarState,
                isDaySheetOpen = isDaySheetOpen,
                snackbarHostState = snackbarHostState,
                onPreviousMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth,
                onToday = viewModel::goToToday,
                onSelectDate = viewModel::selectDate,
                onAdd = { viewModel.openAdd(returnToDaySheet = false) },
                onOpenSettings = { showSettings = true },
                onCloseDaySheet = viewModel::closeDaySheet,
                onAddFromDay = { viewModel.openAdd(returnToDaySheet = true) },
                onEditEntry = { entry ->
                    viewModel.openEdit(entry, returnToDaySheet = true)
                },
                onHistoryEntry = viewModel::openPriceHistory,
                entrySheet = {
                    editor?.let { current ->
                        EntrySheet(
                            editor = current,
                            suggestions = suggestions,
                            recentItems = recentItems,
                            categories = categories,
                            today = calendarState.today,
                            onDismiss = viewModel::closeEditor,
                            onNameChange = viewModel::updateName,
                            onAmountChange = viewModel::updateAmount,
                            onDateChange = viewModel::updateDate,
                            onCategoryChange = viewModel::updateCategory,
                            onAddCategory = viewModel::addCategory,
                            onSuggestionSelected = viewModel::selectSuggestion,
                            onUseLastPrice = viewModel::useLastPrice,
                            onRecentItemSelected = viewModel::useRecentItem,
                            onOpenPriceHistory = viewModel::openPriceHistoryFromEditor,
                            onSave = viewModel::save,
                            onSaveAndAddAnother = viewModel::saveAndAddAnother,
                            onDelete = viewModel::deleteCurrentEntry,
                        )
                    }
                },
            )
        }
    }
}
