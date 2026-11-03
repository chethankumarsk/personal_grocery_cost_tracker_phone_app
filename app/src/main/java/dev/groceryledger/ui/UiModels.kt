package dev.groceryledger.ui

import dev.groceryledger.data.csv.ImportRowError
import dev.groceryledger.data.update.ReleaseInfo
import dev.groceryledger.data.update.UpdateFailureReason
import dev.groceryledger.data.update.UpdateInstallFailure
import dev.groceryledger.domain.date.DateRange
import dev.groceryledger.domain.history.PriceHistory
import dev.groceryledger.domain.model.Categories
import dev.groceryledger.domain.model.CategoryTotal
import dev.groceryledger.domain.model.Expense
import dev.groceryledger.domain.model.SpendingSummary
import dev.groceryledger.domain.model.WeekStart
import java.time.LocalDate
import java.time.YearMonth

data class CalendarDayUi(
    val date: LocalDate,
    val totalPaise: Long,
    val itemCount: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val isFuture: Boolean,
)

data class DayDetail(
    val date: LocalDate,
    val entries: List<Expense>,
    val totalPaise: Long,
    val itemCount: Int,
)

data class CalendarUiState(
    val visibleMonth: YearMonth,
    val selectedDate: LocalDate,
    val today: LocalDate,
    val weekStart: WeekStart,
    val weekRange: DateRange,
    val monthRange: DateRange,
    val days: List<CalendarDayUi>,
    val summary: SpendingSummary,
    val selectedDay: DayDetail,
    val categoryTotals: List<CategoryTotal> = emptyList(),
    val categoryFilter: String? = null,
)

/** [history] is null only for the first frame after the sheet opens. */
data class PriceHistoryUiState(
    val name: String,
    val history: PriceHistory?,
    val returnToDaySheet: Boolean,
)

enum class EditorMode {
    ADD,
    EDIT,
}

enum class NameError {
    REQUIRED,
    TOO_LONG,
}

enum class AmountError {
    REQUIRED,
    INVALID,
    TOO_LARGE,
}

enum class QuantityError {
    INVALID,
}

data class EditorUiState(
    val mode: EditorMode,
    val date: LocalDate,
    val name: String = "",
    val amountText: String = "",
    val category: String = Categories.UNCATEGORIZED,
    val nameError: NameError? = null,
    val amountError: AmountError? = null,
    val quantityText: String = "",
    val unitId: String? = null,
    val quantityError: QuantityError? = null,
    val showQuantity: Boolean = false,
    val isSaving: Boolean = false,
    val showSuggestions: Boolean = false,
    val existing: Expense? = null,
    val returnToDaySheet: Boolean = false,
    val originalName: String = "",
    val originalAmountText: String = "",
    val originalDate: LocalDate = date,
    val originalCategory: String = Categories.UNCATEGORIZED,
    val originalQuantityText: String = "",
    val originalUnitId: String? = null,
) {
    val isDirty: Boolean
        get() = when (mode) {
            EditorMode.ADD -> name.isNotBlank() || amountText.isNotBlank()
            EditorMode.EDIT ->
                name != originalName ||
                    amountText != originalAmountText ||
                    date != originalDate ||
                    category != originalCategory ||
                    quantityText != originalQuantityText ||
                    unitId != originalUnitId
        }
}

data class ImportPreviewUiState(
    val newCount: Int,
    val duplicateCount: Int,
    val errors: List<ImportRowError>,
)

enum class ImportFailureReason {
    UNREADABLE,
    BAD_HEADER,
    NEWER_VERSION,
}

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class UpToDate(val currentVersion: String) : UpdateUiState
    data class UpdateAvailable(
        val currentVersion: String,
        val release: ReleaseInfo,
    ) : UpdateUiState
    data object NoReleases : UpdateUiState
    data class Failed(val reason: UpdateFailureReason) : UpdateUiState

    /** [percent] is 0..100, or -1 while the server has not declared a size. */
    data class Downloading(
        val versionName: String,
        val percent: Int,
    ) : UpdateUiState

    data class InstallPermissionRequired(val versionName: String) : UpdateUiState

    data class InstallFailed(
        val release: ReleaseInfo,
        val reason: UpdateInstallFailure,
    ) : UpdateUiState
}
