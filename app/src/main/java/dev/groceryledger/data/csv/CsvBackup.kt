package dev.groceryledger.data.csv

import dev.groceryledger.domain.model.Categories
import dev.groceryledger.domain.model.Expense
import dev.groceryledger.domain.model.QuantityUnit
import dev.groceryledger.domain.model.Quantities
import dev.groceryledger.domain.money.Money
import dev.groceryledger.domain.money.MoneyParseResult
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

object CsvBackup {
    /**
     * v2 adds the `category` column; v3 adds optional `quantity` and `unit` for unit-price
     * comparison. Older files still import, and older app versions reject v3 files.
     */
    const val CURRENT_FORMAT_VERSION = 3
    const val HEADER = "format_version,entry_uid,date,item,amount_inr,category,quantity,unit"

    private val baseColumns = listOf(
        "format_version",
        "entry_uid",
        "date",
        "item",
        "amount_inr",
    )
    private const val CATEGORY_COLUMN = "category"
    private const val QUANTITY_COLUMN = "quantity"
    private const val UNIT_COLUMN = "unit"

    fun write(expenses: List<Expense>): String {
        val builder = StringBuilder()
        builder.append('\uFEFF')
        builder.append(HEADER).append("\r\n")

        expenses
            .sortedWith(compareBy({ it.date }, { it.createdAt }, { it.id }))
            .forEach { expense ->
                builder
                    .append(CURRENT_FORMAT_VERSION).append(',')
                    .append(escape(expense.uid)).append(',')
                    .append(expense.date).append(',')
                    .append(escape(expense.name)).append(',')
                    .append(Money.formatPlain(expense.amountPaise)).append(',')
                    .append(escape(expense.category)).append(',')
                    .append(expense.quantityMilli?.let { Quantities.format(it) }.orEmpty()).append(',')
                    .append(expense.unit.orEmpty())
                    .append("\r\n")
            }

        return builder.toString()
    }

    fun analyze(
        text: String,
        existingUids: Set<String>,
        today: LocalDate,
        nowMillis: Long,
    ): ImportAnalysis {
        val records = try {
            CsvParser.parse(text)
        } catch (_: CsvFormatException) {
            throw BadBackupException()
        }

        if (records.isEmpty()) throw BadBackupException()

        val header = records.first().fields.map { it.trim().lowercase(Locale.ROOT) }
        if (header.size != header.distinct().size) throw BadBackupException()

        val columnIndexes = (baseColumns + CATEGORY_COLUMN + QUANTITY_COLUMN + UNIT_COLUMN)
            .associateWith { header.indexOf(it) }
        if (baseColumns.any { columnIndexes.getValue(it) < 0 }) throw BadBackupException()
        val categoryIndex = columnIndexes.getValue(CATEGORY_COLUMN)
        val quantityIndex = columnIndexes.getValue(QUANTITY_COLUMN)
        val unitIndex = columnIndexes.getValue(UNIT_COLUMN)

        val entries = mutableListOf<Expense>()
        val errors = mutableListOf<ImportRowError>()
        val seenUids = mutableSetOf<String>()
        var duplicates = 0

        for (record in records.drop(1)) {
            if (record.fields.size != header.size) {
                errors.add(ImportRowError(record.line, ImportRowErrorReason.COLUMN_COUNT))
                continue
            }

            fun field(name: String): String = record.fields[columnIndexes.getValue(name)]

            val version = field("format_version").trim().toIntOrNull()
            if (version == null || version <= 0) {
                errors.add(ImportRowError(record.line, ImportRowErrorReason.INVALID_VERSION))
                continue
            }
            if (version > CURRENT_FORMAT_VERSION) throw NewerBackupException()
            if (version < 1) {
                errors.add(ImportRowError(record.line, ImportRowErrorReason.INVALID_VERSION))
                continue
            }
            // v2 carries a category column; v3 adds quantity and unit.
            if (version >= 2 && categoryIndex < 0) throw BadBackupException()
            if (version >= 3 && (quantityIndex < 0 || unitIndex < 0)) throw BadBackupException()

            val uid = runCatching {
                UUID.fromString(field("entry_uid").trim()).toString()
            }.getOrNull()
            if (uid == null) {
                errors.add(ImportRowError(record.line, ImportRowErrorReason.INVALID_UID))
                continue
            }

            val date = runCatching { LocalDate.parse(field("date").trim()) }.getOrNull()
            if (date == null) {
                errors.add(ImportRowError(record.line, ImportRowErrorReason.INVALID_DATE))
                continue
            }
            if (date.isAfter(today)) {
                errors.add(ImportRowError(record.line, ImportRowErrorReason.FUTURE_DATE))
                continue
            }

            val name = field("item").trim()
            if (name.isEmpty() || name.length > 80) {
                errors.add(ImportRowError(record.line, ImportRowErrorReason.INVALID_NAME))
                continue
            }

            val amount = when (val parsed = Money.parse(field("amount_inr"))) {
                is MoneyParseResult.Valid -> parsed.paise
                else -> {
                    errors.add(ImportRowError(record.line, ImportRowErrorReason.INVALID_AMOUNT))
                    continue
                }
            }

            val category = if (version >= 2) {
                Categories.sanitize(field(CATEGORY_COLUMN)) ?: Categories.UNCATEGORIZED
            } else {
                Categories.UNCATEGORIZED
            }

            var quantityMilli: Long? = null
            var unit: String? = null
            if (version >= 3) {
                val rawQuantity = field(QUANTITY_COLUMN).trim()
                val rawUnit = field(UNIT_COLUMN).trim()
                if (rawQuantity.isNotEmpty() || rawUnit.isNotEmpty()) {
                    val parsedUnit = QuantityUnit.fromId(rawUnit)
                    if (parsedUnit == null) {
                        errors.add(ImportRowError(record.line, ImportRowErrorReason.INVALID_UNIT))
                        continue
                    }
                    val parsedQuantity = Quantities.parse(rawQuantity)
                    if (parsedQuantity == null) {
                        errors.add(ImportRowError(record.line, ImportRowErrorReason.INVALID_QUANTITY))
                        continue
                    }
                    quantityMilli = parsedQuantity
                    unit = parsedUnit.id
                }
            }

            if (uid in existingUids || !seenUids.add(uid)) {
                duplicates++
                continue
            }

            entries.add(
                Expense(
                    id = 0,
                    uid = uid,
                    name = name,
                    amountPaise = amount,
                    date = date,
                    createdAt = nowMillis + entries.size,
                    updatedAt = nowMillis,
                    category = category,
                    quantityMilli = quantityMilli,
                    unit = unit,
                ),
            )
        }

        return ImportAnalysis(
            entries = entries,
            duplicateCount = duplicates,
            errors = errors,
        )
    }

    private fun escape(value: String): String {
        if (value.none { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            return value
        }
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
}
