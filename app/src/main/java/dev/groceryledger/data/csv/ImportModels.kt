package dev.groceryledger.data.csv

import dev.groceryledger.domain.model.Expense
import java.time.LocalDate

enum class ImportRowErrorReason {
    INVALID_UID,
    INVALID_DATE,
    FUTURE_DATE,
    INVALID_NAME,
    INVALID_AMOUNT,
    INVALID_QUANTITY,
    INVALID_UNIT,
    COLUMN_COUNT,
    INVALID_VERSION,
}

data class ImportRowError(
    val line: Int,
    val reason: ImportRowErrorReason,
)

data class ImportAnalysis(
    val entries: List<Expense>,
    val duplicateCount: Int,
    val errors: List<ImportRowError>,
)

class BadBackupException : Exception("Not a Grocery Ledger backup")

class NewerBackupException : Exception("Backup created by a newer app version")
