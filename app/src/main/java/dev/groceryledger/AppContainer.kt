package dev.groceryledger

import android.content.Context
import androidx.room.Room
import dev.groceryledger.data.backup.CsvBackupManager
import dev.groceryledger.data.local.GroceryDatabase
import dev.groceryledger.data.local.OfflineExpenseRepository
import dev.groceryledger.data.settings.DataStoreSettingsRepository
import dev.groceryledger.data.update.GitHubUpdateChecker
import dev.groceryledger.data.update.UpdateChecker
import dev.groceryledger.data.update.ApkDownloader
import dev.groceryledger.data.update.ApkInstaller
import dev.groceryledger.data.update.ApkVerifier
import dev.groceryledger.domain.repository.ExpenseRepository
import dev.groceryledger.domain.repository.SettingsRepository
import dev.groceryledger.domain.time.DateProvider
import dev.groceryledger.domain.time.SystemDateProvider
import java.io.File

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: GroceryDatabase = Room.databaseBuilder(
        appContext,
        GroceryDatabase::class.java,
        GroceryDatabase.NAME,
    )
        .addMigrations(GroceryDatabase.MIGRATION_1_2)
        .addCallback(GroceryDatabase.CALLBACK_SEED_CATEGORIES)
        .build()

    val expenseRepository: ExpenseRepository = OfflineExpenseRepository(
        database = database,
        expenseDao = database.expenseDao(),
        catalogDao = database.itemCatalogDao(),
        categoryDao = database.categoryDao(),
    )

    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(appContext)

    val dateProvider: DateProvider = SystemDateProvider

    val backupManager: CsvBackupManager = CsvBackupManager(
        context = appContext,
        repository = expenseRepository,
        dateProvider = dateProvider,
    )

    val updateChecker: UpdateChecker = GitHubUpdateChecker(
        currentVersion = BuildConfig.VERSION_NAME,
    )

    val apkDownloader: ApkDownloader = ApkDownloader(
        downloadDir = File(appContext.cacheDir, "updates"),
        userAgent = "GroceryLedger/${BuildConfig.VERSION_NAME}",
    )

    val apkVerifier: ApkVerifier = ApkVerifier(
        context = appContext,
        expectedPackageName = appContext.packageName,
        currentVersionCode = BuildConfig.VERSION_CODE.toLong(),
    )

    val apkInstaller: ApkInstaller = ApkInstaller(appContext)
}
