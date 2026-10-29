package dev.groceryledger.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.groceryledger.BuildConfig
import dev.groceryledger.R
import dev.groceryledger.data.csv.ImportRowErrorReason
import dev.groceryledger.data.update.UpdateFailureReason
import dev.groceryledger.domain.model.AppSettings
import dev.groceryledger.domain.model.ThemeMode
import dev.groceryledger.domain.model.WeekStart
import dev.groceryledger.ui.ImportFailureReason
import dev.groceryledger.ui.ImportPreviewUiState
import dev.groceryledger.ui.UpdateUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    today: java.time.LocalDate,
    importPreview: ImportPreviewUiState?,
    importFailure: ImportFailureReason?,
    isImportBusy: Boolean,
    updateState: UpdateUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onWeekStartChange: (WeekStart) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onExport: (Uri) -> Unit,
    onImport: (Uri) -> Unit,
    onConfirmImport: () -> Unit,
    onCancelImport: () -> Unit,
    onDismissImportFailure: () -> Unit,
    onDeleteAll: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onDismissUpdate: () -> Unit,
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        uri?.let(onExport)
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(onImport)
    }

    val launchExport = {
        exportLauncher.launch("grocery-spending-$today.csv")
    }

    val uriHandler = LocalUriHandler.current
    val isCheckingForUpdates = updateState == UpdateUiState.Checking

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            SectionHeader(stringResource(R.string.settings_general))

            Text(
                text = stringResource(R.string.week_starts_on),
                style = MaterialTheme.typography.titleSmall,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WeekStart.entries.forEach { weekStart ->
                    FilterChip(
                        selected = settings.weekStart == weekStart,
                        onClick = { onWeekStartChange(weekStart) },
                        label = {
                            Text(
                                stringResource(
                                    when (weekStart) {
                                        WeekStart.MONDAY -> R.string.monday
                                        WeekStart.SUNDAY -> R.string.sunday
                                    },
                                ),
                            )
                        },
                    )
                }
            }
            Text(
                text = stringResource(R.string.week_start_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            SectionHeader(stringResource(R.string.settings_appearance))

            Text(
                text = stringResource(R.string.theme),
                style = MaterialTheme.typography.titleSmall,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ThemeMode.entries.forEach { theme ->
                    FilterChip(
                        selected = settings.theme == theme,
                        onClick = { onThemeChange(theme) },
                        label = {
                            Text(
                                stringResource(
                                    when (theme) {
                                        ThemeMode.SYSTEM -> R.string.theme_system
                                        ThemeMode.LIGHT -> R.string.theme_light
                                        ThemeMode.DARK -> R.string.theme_dark
                                    },
                                ),
                            )
                        },
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            SectionHeader(stringResource(R.string.settings_data))

            Text(
                text = stringResource(R.string.backup_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (isImportBusy) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
            }

            OutlinedButton(
                onClick = launchExport,
                enabled = !isImportBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Text(stringResource(R.string.export_csv))
            }

            OutlinedButton(
                onClick = {
                    importLauncher.launch(
                        arrayOf("text/*", "application/csv", "application/octet-stream"),
                    )
                },
                enabled = !isImportBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.import_csv))
            }

            TextButton(
                onClick = { showDeleteDialog = true },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.delete_all_data))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            SectionHeader(stringResource(R.string.settings_updates))

            Text(
                text = stringResource(R.string.update_check_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (isCheckingForUpdates) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
            }

            OutlinedButton(
                onClick = onCheckForUpdates,
                enabled = !isCheckingForUpdates,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                if (isCheckingForUpdates) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text(
                    stringResource(
                        if (isCheckingForUpdates) {
                            R.string.checking_for_updates
                        } else {
                            R.string.check_for_updates
                        },
                    ),
                )
            }

            Text(
                text = stringResource(R.string.current_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            SectionHeader(stringResource(R.string.settings_about))

            Text(
                text = stringResource(R.string.about_privacy),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_all_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.delete_all_message))
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            launchExport()
                        },
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(stringResource(R.string.export_first))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteAll()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.delete_all_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    importPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = onCancelImport,
            title = { Text(stringResource(R.string.import_title)) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 340.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = stringResource(
                            R.string.import_preview_summary,
                            preview.newCount,
                            preview.duplicateCount,
                            preview.errors.size,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    if (preview.errors.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.import_errors_heading),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        preview.errors.take(6).forEach { error ->
                            Text(
                                text = stringResource(
                                    R.string.import_error_line,
                                    error.line,
                                    stringResource(error.reason.labelRes()),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        if (preview.errors.size > 6) {
                            Text(
                                text = stringResource(
                                    R.string.import_error_more,
                                    preview.errors.size - 6,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (preview.newCount > 0) {
                    TextButton(
                        onClick = onConfirmImport,
                        enabled = !isImportBusy,
                    ) {
                        Text(stringResource(R.string.import_confirm))
                    }
                } else {
                    TextButton(onClick = onCancelImport) {
                        Text(stringResource(R.string.close))
                    }
                }
            },
            dismissButton = {
                if (preview.newCount > 0) {
                    TextButton(onClick = onCancelImport) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            },
        )
    }

    importFailure?.let { failure ->
        AlertDialog(
            onDismissRequest = onDismissImportFailure,
            title = { Text(stringResource(R.string.import_failed_title)) },
            text = {
                Text(
                    stringResource(
                        when (failure) {
                            ImportFailureReason.BAD_HEADER -> R.string.import_bad_header
                            ImportFailureReason.NEWER_VERSION -> R.string.import_newer_version
                            ImportFailureReason.UNREADABLE -> R.string.import_unreadable
                        },
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = onDismissImportFailure) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }

    when (val state = updateState) {
        is UpdateUiState.UpdateAvailable -> {
            AlertDialog(
                onDismissRequest = onDismissUpdate,
                title = { Text(stringResource(R.string.update_available_title)) },
                text = {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            stringResource(
                                R.string.update_available_message,
                                state.release.versionName,
                                state.currentVersion,
                            ),
                        )
                        if (state.release.notes.isNotBlank()) {
                            Text(
                                text = stringResource(R.string.update_release_notes),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                            Text(
                                text = state.release.notes.take(2_000),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        Text(
                            text = stringResource(R.string.update_open_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            runCatching { uriHandler.openUri(state.release.pageUrl) }
                            onDismissUpdate()
                        },
                    ) {
                        Text(stringResource(R.string.update_open_page))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissUpdate) {
                        Text(stringResource(R.string.update_later))
                    }
                },
            )
        }

        is UpdateUiState.UpToDate -> {
            AlertDialog(
                onDismissRequest = onDismissUpdate,
                title = { Text(stringResource(R.string.update_up_to_date_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.update_up_to_date_message,
                            state.currentVersion,
                        ),
                    )
                },
                confirmButton = {
                    TextButton(onClick = onDismissUpdate) {
                        Text(stringResource(R.string.close))
                    }
                },
            )
        }

        UpdateUiState.NoReleases -> {
            AlertDialog(
                onDismissRequest = onDismissUpdate,
                title = { Text(stringResource(R.string.update_no_releases_title)) },
                text = { Text(stringResource(R.string.update_no_releases_message)) },
                confirmButton = {
                    TextButton(onClick = onDismissUpdate) {
                        Text(stringResource(R.string.close))
                    }
                },
            )
        }

        is UpdateUiState.Failed -> {
            AlertDialog(
                onDismissRequest = onDismissUpdate,
                title = { Text(stringResource(R.string.update_failed_title)) },
                text = {
                    Text(
                        stringResource(
                            when (state.reason) {
                                UpdateFailureReason.NETWORK -> R.string.update_failed_network
                                UpdateFailureReason.RATE_LIMITED -> R.string.update_failed_rate_limited
                                UpdateFailureReason.SERVER,
                                UpdateFailureReason.PARSE -> R.string.update_failed_server
                            },
                        ),
                    )
                },
                confirmButton = {
                    TextButton(onClick = onCheckForUpdates) {
                        Text(stringResource(R.string.update_try_again))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissUpdate) {
                        Text(stringResource(R.string.close))
                    }
                },
            )
        }

        UpdateUiState.Idle,
        UpdateUiState.Checking -> Unit
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

private fun ImportRowErrorReason.labelRes(): Int = when (this) {
    ImportRowErrorReason.INVALID_UID -> R.string.import_reason_invalid_uid
    ImportRowErrorReason.INVALID_DATE -> R.string.import_reason_invalid_date
    ImportRowErrorReason.FUTURE_DATE -> R.string.import_reason_future_date
    ImportRowErrorReason.INVALID_NAME -> R.string.import_reason_invalid_name
    ImportRowErrorReason.INVALID_AMOUNT -> R.string.import_reason_invalid_amount
    ImportRowErrorReason.COLUMN_COUNT -> R.string.import_reason_column_count
    ImportRowErrorReason.INVALID_VERSION -> R.string.import_reason_invalid_version
}
