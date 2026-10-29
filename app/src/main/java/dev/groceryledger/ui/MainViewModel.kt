package dev.groceryledger.ui

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import dev.groceryledger.AppContainer
import dev.groceryledger.BuildConfig
import dev.groceryledger.R
import dev.groceryledger.data.backup.CsvBackupManager
import dev.groceryledger.data.csv.BadBackupException
import dev.groceryledger.data.csv.ImportAnalysis
import dev.groceryledger.data.csv.NewerBackupException
import dev.groceryledger.data.update.UpdateCheckResult
import dev.groceryledger.data.update.UpdateChecker
import dev.groceryledger.data.update.UpdateFailureReason
import dev.groceryledger.domain.date.DateRange
import dev.groceryledger.domain.date.DateRanges
import dev.groceryledger.domain.model.AppSettings
import dev.groceryledger.domain.model.Expense
import dev.groceryledger.domain.model.ItemSuggestion
import dev.groceryledger.domain.model.SpendingSummary
import dev.groceryledger.domain.model.ThemeMode
import dev.groceryledger.domain.model.WeekStart
import dev.groceryledger.domain.money.Money
import dev.groceryledger.domain.money.MoneyParseResult
import dev.groceryledger.domain.repository.ExpenseRepository
import dev.groceryledger.domain.repository.SettingsRepository
import dev.groceryledger.domain.time.DateProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: ExpenseRepository,
    private val settingsRepository: SettingsRepository,
    private val backupManager: CsvBackupManager,
    private val updateChecker: UpdateChecker,
    private val dateProvider: DateProvider,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val initialToday = dateProvider.today()
    private val restoredDate = savedStateHandle.get<String>(KEY_SELECTED_DATE)
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?.takeUnless { it.isAfter(initialToday) }

    private val _today = MutableStateFlow(initialToday)
    private val _selectedDate = MutableStateFlow(restoredDate ?: initialToday)
    private val _visibleMonth = MutableStateFlow(YearMonth.from(_selectedDate.value))
    private val _isDaySheetOpen = MutableStateFlow(false)
    private val _editor = MutableStateFlow<EditorUiState?>(null)
    private val _importPreview = MutableStateFlow<ImportPreviewUiState?>(null)
    private val _pendingImport = MutableStateFlow<ImportAnalysis?>(null)
    private val _importFailure = MutableStateFlow<ImportFailureReason?>(null)
    private val _isImportBusy = MutableStateFlow(false)
    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 16)

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val editor: StateFlow<EditorUiState?> = _editor.asStateFlow()
    val isDaySheetOpen: StateFlow<Boolean> = _isDaySheetOpen.asStateFlow()
    val importPreview: StateFlow<ImportPreviewUiState?> = _importPreview.asStateFlow()
    val importFailure: StateFlow<ImportFailureReason?> = _importFailure.asStateFlow()
    val isImportBusy: StateFlow<Boolean> = _isImportBusy.asStateFlow()
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()
    val events = _events.asSharedFlow()

    val suggestions: StateFlow<List<ItemSuggestion>> = _editor
        .map { it?.name.orEmpty() }
        .distinctUntilChanged()
        .debounce(120)
        .flatMapLatest { name ->
            if (name.trim().length >= 2 && _editor.value != null) {
                repository.observeSuggestions(name)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val calendarState: StateFlow<CalendarUiState> = combine(
        _visibleMonth,
        _selectedDate,
        _today,
        settings,
    ) { month, selected, today, appSettings ->
        CalendarKey(month, selected, today, appSettings.weekStart)
    }
        .flatMapLatest { key ->
            val gridRange = DateRanges.monthGridRange(key.month, key.weekStart)
            val weekRange = DateRanges.weekRange(key.selectedDate, key.weekStart)
            val monthRange = DateRanges.monthRange(key.month)

            combine(
                repository.observeDailyTotals(gridRange),
                repository.observeTotal(weekRange),
                repository.observeTotal(monthRange),
                repository.observeTotal(DateRange(key.selectedDate, key.selectedDate)),
                repository.observeEntries(key.selectedDate),
            ) { dailyTotals, weekTotal, monthTotal, dayTotal, entries ->
                val totalsByDate = dailyTotals.associateBy { it.date }
                val days = DateRanges.monthGridDates(key.month, key.weekStart).map { date ->
                    val total = totalsByDate[date]
                    CalendarDayUi(
                        date = date,
                        totalPaise = total?.totalPaise ?: 0L,
                        itemCount = total?.itemCount ?: 0,
                        isCurrentMonth = YearMonth.from(date) == key.month,
                        isToday = date == key.today,
                        isSelected = date == key.selectedDate,
                        isFuture = date.isAfter(key.today),
                    )
                }

                CalendarUiState(
                    visibleMonth = key.month,
                    selectedDate = key.selectedDate,
                    today = key.today,
                    weekStart = key.weekStart,
                    weekRange = weekRange,
                    monthRange = monthRange,
                    days = days,
                    summary = SpendingSummary(dayTotal, weekTotal, monthTotal),
                    selectedDay = DayDetail(
                        date = key.selectedDate,
                        entries = entries,
                        totalPaise = dayTotal,
                        itemCount = entries.size,
                    ),
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = initialCalendarState(),
        )

    fun refreshToday() {
        val newToday = dateProvider.today()
        val oldToday = _today.value
        if (newToday == oldToday) return

        _today.value = newToday
        if (_selectedDate.value == oldToday || _selectedDate.value.isAfter(newToday)) {
            setSelection(newToday)
            _isDaySheetOpen.value = false
        }
    }

    fun previousMonth() {
        moveToMonth(_visibleMonth.value.minusMonths(1))
    }

    fun nextMonth() {
        val target = _visibleMonth.value.plusMonths(1)
        if (!target.isAfter(YearMonth.from(_today.value))) {
            moveToMonth(target)
        }
    }

    fun goToToday() {
        val today = dateProvider.today()
        _today.value = today
        setSelection(today)
        _isDaySheetOpen.value = false
    }

    fun selectDate(date: LocalDate) {
        if (date.isAfter(_today.value)) return
        setSelection(date)
        _editor.value = null
        _isDaySheetOpen.value = true
    }

    fun closeDaySheet() {
        _isDaySheetOpen.value = false
    }

    fun openAdd(returnToDaySheet: Boolean = false) {
        if (_editor.value != null) return
        _isDaySheetOpen.value = false
        val date = _selectedDate.value.takeUnless { it.isAfter(_today.value) } ?: _today.value
        _editor.value = EditorUiState(
            mode = EditorMode.ADD,
            date = date,
            returnToDaySheet = returnToDaySheet,
            originalDate = date,
        )
    }

    fun openEdit(expense: Expense, returnToDaySheet: Boolean = false) {
        if (_editor.value != null) return
        _isDaySheetOpen.value = false
        val amountText = Money.formatEditable(expense.amountPaise)
        _editor.value = EditorUiState(
            mode = EditorMode.EDIT,
            date = expense.date,
            name = expense.name,
            amountText = amountText,
            existing = expense,
            returnToDaySheet = returnToDaySheet,
            originalName = expense.name,
            originalAmountText = amountText,
            originalDate = expense.date,
        )
    }

    fun closeEditor() {
        val returnToDaySheet = _editor.value?.returnToDaySheet == true
        _editor.value = null
        if (returnToDaySheet) _isDaySheetOpen.value = true
    }

    fun updateName(name: String) {
        _editor.update { current ->
            current?.copy(
                name = name.take(80),
                nameError = null,
                showSuggestions = true,
            )
        }
    }

    fun updateAmount(amount: String) {
        val filtered = amount
            .filter { it.isDigit() || it == '.' }
            .take(12)
        _editor.update { current ->
            current?.copy(amountText = filtered, amountError = null)
        }
    }

    fun selectSuggestion(suggestion: ItemSuggestion) {
        _editor.update { current ->
            current?.copy(
                name = suggestion.name,
                nameError = null,
                showSuggestions = false,
            )
        }
    }

    fun useLastPrice(pricePaise: Long) {
        _editor.update { current ->
            current?.copy(
                amountText = Money.formatEditable(pricePaise),
                amountError = null,
                showSuggestions = false,
            )
        }
    }

    fun updateDate(date: LocalDate) {
        if (date.isAfter(_today.value)) return
        _editor.update { current -> current?.copy(date = date) }
    }

    fun save() = saveInternal(addAnother = false)

    fun saveAndAddAnother() = saveInternal(addAnother = true)

    fun deleteCurrentEntry() {
        val current = _editor.value ?: return
        val existing = current.existing ?: return
        if (current.isSaving) return

        _editor.update { it?.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                repository.delete(existing.id)
                _editor.value = null
                if (current.returnToDaySheet) _isDaySheetOpen.value = true
                _events.emit(
                    UiEvent(
                        message = uiText(
                            R.string.entry_deleted,
                            existing.name,
                            Money.format(existing.amountPaise),
                        ),
                        action = SnackbarAction.UNDO,
                        deletedEntry = existing,
                    ),
                )
            } catch (_: Exception) {
                _editor.update { it?.copy(isSaving = false) }
                _events.emit(UiEvent(uiText(R.string.delete_failed)))
            }
        }
    }

    fun restoreDeletedEntry(expense: Expense) {
        viewModelScope.launch {
            try {
                repository.restore(expense)
                _events.emit(UiEvent(uiText(R.string.entry_restored)))
            } catch (_: Exception) {
                _events.emit(UiEvent(uiText(R.string.restore_failed)))
            }
        }
    }

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            try {
                val count = backupManager.export(uri)
                _events.emit(
                    UiEvent(uiPlural(R.plurals.exported_entries, count, count)),
                )
            } catch (_: Exception) {
                _events.emit(UiEvent(uiText(R.string.export_failed)))
            }
        }
    }

    fun analyzeImport(uri: Uri) {
        if (_isImportBusy.value) return
        viewModelScope.launch {
            _isImportBusy.value = true
            try {
                val analysis = backupManager.analyze(uri)
                _pendingImport.value = analysis
                _importPreview.value = ImportPreviewUiState(
                    newCount = analysis.entries.size,
                    duplicateCount = analysis.duplicateCount,
                    errors = analysis.errors,
                )
            } catch (_: NewerBackupException) {
                _importFailure.value = ImportFailureReason.NEWER_VERSION
            } catch (_: BadBackupException) {
                _importFailure.value = ImportFailureReason.BAD_HEADER
            } catch (_: Exception) {
                _importFailure.value = ImportFailureReason.UNREADABLE
            } finally {
                _isImportBusy.value = false
            }
        }
    }

    fun confirmImport() {
        val analysis = _pendingImport.value ?: return
        if (_isImportBusy.value) return

        viewModelScope.launch {
            _isImportBusy.value = true
            try {
                val added = backupManager.import(analysis)
                _pendingImport.value = null
                _importPreview.value = null
                val message = if (added == 0) {
                    uiText(R.string.import_no_new)
                } else {
                    uiText(
                        R.string.import_summary,
                        added,
                        analysis.duplicateCount,
                        analysis.errors.size,
                    )
                }
                _events.emit(UiEvent(message))
            } catch (_: Exception) {
                _importFailure.value = ImportFailureReason.UNREADABLE
            } finally {
                _isImportBusy.value = false
            }
        }
    }

    fun cancelImport() {
        _pendingImport.value = null
        _importPreview.value = null
    }

    fun clearImportFailure() {
        _importFailure.value = null
    }

    fun checkForUpdates() {
        if (_updateState.value == UpdateUiState.Checking) return

        _updateState.value = UpdateUiState.Checking
        viewModelScope.launch {
            val result = try {
                updateChecker.checkForUpdates()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                UpdateCheckResult.Failure(UpdateFailureReason.NETWORK)
            }

            _updateState.value = when (result) {
                is UpdateCheckResult.UpdateAvailable -> UpdateUiState.UpdateAvailable(
                    currentVersion = BuildConfig.VERSION_NAME,
                    release = result.release,
                )
                UpdateCheckResult.UpToDate -> UpdateUiState.UpToDate(
                    currentVersion = BuildConfig.VERSION_NAME,
                )
                UpdateCheckResult.NoReleases -> UpdateUiState.NoReleases
                is UpdateCheckResult.Failure -> UpdateUiState.Failed(result.reason)
            }
        }
    }

    fun dismissUpdateState() {
        _updateState.value = UpdateUiState.Idle
    }

    fun deleteAllData() {
        viewModelScope.launch {
            try {
                repository.clearAll()
                _isDaySheetOpen.value = false
                _editor.value = null
                _events.emit(UiEvent(uiText(R.string.delete_all_success)))
            } catch (_: Exception) {
                _events.emit(UiEvent(uiText(R.string.delete_all_failed)))
            }
        }
    }

    fun setWeekStart(weekStart: WeekStart) {
        viewModelScope.launch { settingsRepository.setWeekStart(weekStart) }
    }

    fun setTheme(theme: ThemeMode) {
        viewModelScope.launch { settingsRepository.setTheme(theme) }
    }

    private fun saveInternal(addAnother: Boolean) {
        val current = _editor.value ?: return
        if (current.isSaving) return

        val validated = validate(current)
        if (validated.nameError != null || validated.amountError != null) {
            _editor.value = validated
            return
        }

        val amount = when (val parsed = Money.parse(validated.amountText)) {
            is MoneyParseResult.Valid -> parsed.paise
            else -> return
        }
        val name = validated.name.trim()
        val date = validated.date

        _editor.value = validated.copy(name = name, isSaving = true)
        viewModelScope.launch {
            try {
                when (validated.mode) {
                    EditorMode.ADD -> repository.add(name, amount, date)
                    EditorMode.EDIT -> repository.update(
                        id = requireNotNull(validated.existing).id,
                        name = name,
                        amountPaise = amount,
                        date = date,
                    )
                }

                if (addAnother && validated.mode == EditorMode.ADD) {
                    _editor.value = EditorUiState(
                        mode = EditorMode.ADD,
                        date = date,
                        returnToDaySheet = validated.returnToDaySheet,
                        originalDate = date,
                    )
                } else {
                    _editor.value = null
                    if (validated.returnToDaySheet) {
                        setSelection(date)
                        _isDaySheetOpen.value = true
                    }
                }
            } catch (_: Exception) {
                _editor.update { it?.copy(isSaving = false) }
                _events.emit(UiEvent(uiText(R.string.save_failed)))
            }
        }
    }

    private fun validate(current: EditorUiState): EditorUiState {
        val name = current.name.trim()
        val nameError = when {
            name.isEmpty() -> NameError.REQUIRED
            name.length > 80 -> NameError.TOO_LONG
            else -> null
        }
        val amountError = when (Money.parse(current.amountText)) {
            MoneyParseResult.Empty -> AmountError.REQUIRED
            MoneyParseResult.Invalid -> AmountError.INVALID
            MoneyParseResult.TooLarge -> AmountError.TOO_LARGE
            is MoneyParseResult.Valid -> null
        }
        return current.copy(
            name = name,
            nameError = nameError,
            amountError = amountError,
        )
    }

    private fun moveToMonth(target: YearMonth) {
        val date = if (target == YearMonth.from(_today.value)) {
            _today.value
        } else {
            target.atDay(1)
        }
        setSelection(date)
        _isDaySheetOpen.value = false
    }

    private fun setSelection(date: LocalDate) {
        _selectedDate.value = date
        _visibleMonth.value = YearMonth.from(date)
        savedStateHandle[KEY_SELECTED_DATE] = date.toString()
    }

    private fun initialCalendarState(): CalendarUiState {
        val today = _today.value
        val month = YearMonth.from(today)
        return CalendarUiState(
            visibleMonth = month,
            selectedDate = today,
            today = today,
            weekStart = WeekStart.MONDAY,
            weekRange = DateRanges.weekRange(today, WeekStart.MONDAY),
            monthRange = DateRanges.monthRange(month),
            days = DateRanges.monthGridDates(month, WeekStart.MONDAY).map { date ->
                CalendarDayUi(
                    date = date,
                    totalPaise = 0,
                    itemCount = 0,
                    isCurrentMonth = YearMonth.from(date) == month,
                    isToday = date == today,
                    isSelected = date == today,
                    isFuture = date.isAfter(today),
                )
            },
            summary = SpendingSummary(0, 0, 0),
            selectedDay = DayDetail(today, emptyList(), 0, 0),
        )
    }

    private data class CalendarKey(
        val month: YearMonth,
        val selectedDate: LocalDate,
        val today: LocalDate,
        val weekStart: WeekStart,
    )

    private companion object {
        const val KEY_SELECTED_DATE = "selected_date"
    }
}

class MainViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        create(modelClass, CreationExtras.Empty)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: CreationExtras,
    ): T {
        if (!modelClass.isAssignableFrom(MainViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }

        val savedStateHandle = runCatching { extras.createSavedStateHandle() }
            .getOrElse { SavedStateHandle() }

        return MainViewModel(
            repository = container.expenseRepository,
            settingsRepository = container.settingsRepository,
            backupManager = container.backupManager,
            updateChecker = container.updateChecker,
            dateProvider = container.dateProvider,
            savedStateHandle = savedStateHandle,
        ) as T
    }
}
