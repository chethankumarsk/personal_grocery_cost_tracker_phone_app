package dev.groceryledger.data.backup

import android.content.Context
import android.net.Uri
import dev.groceryledger.data.csv.CsvBackup
import dev.groceryledger.data.csv.ImportAnalysis
import dev.groceryledger.domain.repository.ExpenseRepository
import dev.groceryledger.domain.time.DateProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class CsvBackupManager(
    private val context: Context,
    private val repository: ExpenseRepository,
    private val dateProvider: DateProvider,
) {
    suspend fun export(uri: Uri): Int = withContext(Dispatchers.IO) {
        val entries = repository.all()
        val csv = CsvBackup.write(entries)
        val stream = context.contentResolver.openOutputStream(uri)
            ?: throw IOException("Could not open the selected file")
        stream.use { output ->
            output.write(csv.toByteArray(Charsets.UTF_8))
        }
        entries.size
    }

    suspend fun analyze(uri: Uri): ImportAnalysis = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        } ?: throw IOException("Could not open the selected file")

        if (text.length > MAX_BACKUP_CHARS) {
            throw IOException("Backup file is too large")
        }

        CsvBackup.analyze(
            text = text,
            existingUids = repository.existingUids(),
            today = dateProvider.today(),
            nowMillis = System.currentTimeMillis(),
        )
    }

    suspend fun import(analysis: ImportAnalysis): Int = withContext(Dispatchers.IO) {
        repository.importAll(analysis.entries)
    }

    companion object {
        private const val MAX_BACKUP_CHARS = 25_000_000
    }
}
