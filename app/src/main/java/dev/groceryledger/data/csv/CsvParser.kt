package dev.groceryledger.data.csv

data class CsvRecord(
    val line: Int,
    val fields: List<String>,
)

class CsvFormatException(message: String) : Exception(message)

object CsvParser {
    /**
     * RFC 4180-style parser. Quoted fields may contain commas and newlines;
     * doubled quotes are unescaped. Line endings are normalised first.
     */
    fun parse(text: String): List<CsvRecord> {
        val input = text
            .removePrefix("\uFEFF")
            .replace("\r\n", "\n")
            .replace('\r', '\n')

        val records = mutableListOf<CsvRecord>()
        val fields = mutableListOf<String>()
        val field = StringBuilder()
        var index = 0
        var line = 1
        var recordStartLine = 1
        var inQuotes = false
        var recordHasContent = false

        while (index < input.length) {
            val char = input[index]
            if (inQuotes) {
                when {
                    char == '"' && index + 1 < input.length && input[index + 1] == '"' -> {
                        field.append('"')
                        index += 2
                    }
                    char == '"' -> {
                        inQuotes = false
                        index++
                    }
                    else -> {
                        if (char == '\n') line++
                        field.append(char)
                        index++
                    }
                }
            } else {
                when (char) {
                    '"' -> {
                        inQuotes = true
                        recordHasContent = true
                        index++
                    }
                    ',' -> {
                        fields.add(field.toString())
                        field.clear()
                        recordHasContent = true
                        index++
                    }
                    '\n' -> {
                        if (recordHasContent || fields.isNotEmpty() || field.isNotEmpty()) {
                            fields.add(field.toString())
                            records.add(CsvRecord(recordStartLine, fields.toList()))
                            fields.clear()
                            field.clear()
                        }
                        line++
                        recordStartLine = line
                        recordHasContent = false
                        index++
                    }
                    else -> {
                        field.append(char)
                        recordHasContent = true
                        index++
                    }
                }
            }
        }

        if (inQuotes) throw CsvFormatException("Unclosed quoted field")
        if (recordHasContent || fields.isNotEmpty() || field.isNotEmpty()) {
            fields.add(field.toString())
            records.add(CsvRecord(recordStartLine, fields.toList()))
        }
        return records
    }
}
