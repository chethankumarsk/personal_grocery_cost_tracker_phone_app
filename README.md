# Grocery Ledger

A small, offline-first Android app for tracking grocery spending in INR. The home screen is a calendar; each day holds item entries, and the selected day shows day, week, and month totals. Item names autocomplete from previous purchases, including the last price paid.

- **Package:** `dev.groceryledger`
- **Version:** 1.1.1 (versionCode 3)
- **Platform:** Android 8.0+ (minSdk 26), compile/target SDK 36
- **UI:** Kotlin + Jetpack Compose + Material 3
- **Storage:** Room + DataStore, entirely on-device
- **Requirements:** [REQUIREMENTS.md](REQUIREMENTS.md)
- **LLM/developer handoff:** [AGENTS.md](AGENTS.md), [docs/KNOWLEDGE_GRAPH.md](docs/KNOWLEDGE_GRAPH.md)
- **Release process:** [docs/RELEASING.md](docs/RELEASING.md)

## Status

The v1 feature set and the manual GitHub update check are implemented. 46 JVM tests pass, lint reports 0 errors, and a signed 1.1.1 release APK builds. Manual QA on a physical phone is still recommended before daily use.

## Features

- Month calendar with per-day totals and today/selected highlighting.
- Day sheet listing items, item count, and exact total.
- Add, edit, and delete entries, with a 5-second undo snackbar.
- Day, week (Mon–Sun by default), and month totals for the selected date.
- Autocomplete after two characters, ranked by prefix, word-start, contains, then usage/recency.
- Last price/date shown per suggestion, with a chip to reuse the previous price.
- "Save & add another" for entering a whole shopping trip quickly.
- Settings for week start (Monday/Sunday) and theme (system/light/dark).
- CSV export and duplicate-safe import, including versioned backups.
- Manual "Check for updates" against GitHub Releases, with version comparison and a browser handoff.
- No accounts, ads, or analytics. The only network feature is the user-initiated update check.

## Project layout

```
app/src/main/java/dev/groceryledger/
├── data/
│   ├── local/        Room entities, DAOs, database, repository
│   ├── csv/          CSV writer/parser, import models
│   ├── backup/       File picker export/import manager
│   ├── update/       GitHub Releases check and version comparison
│   └── settings/     DataStore preferences
├── domain/
│   ├── model/        Expense, totals, suggestions, settings
│   ├── money/        INR parsing/formatting and paise arithmetic
│   ├── date/         Day/week/month ranges and date labels
│   ├── suggestion/   Pure suggestion ranking
│   ├── repository/   Repository interfaces
│   └── time/         Injectable date provider
├── ui/
│   ├── calendar/     Calendar grid, summary card, day sheet
│   ├── entry/        Add/edit entry sheet and date picker
│   ├── settings/     Settings, backup, import preview
│   ├── theme/        Material 3 theme
│   └── MainViewModel.kt / UiText.kt / UiModels.kt
├── AppContainer.kt
├── GroceryLedgerApplication.kt
└── MainActivity.kt
```

Business rules (money, date ranges, ranking) are pure Kotlin and unit tested. The UI does not touch Room directly; it talks to `MainViewModel`, which uses repository interfaces.

More detail: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Build

Prerequisites:

- JDK 17 or newer (JDK 21 is installed locally at `C:\Users\Administrator\toolchains\jdk-21.0.12.1+1`).
- Android SDK platform 36 and build-tools 36.0.0 (`local.properties` points at the SDK on this machine).
- Android Studio is optional; the Gradle wrapper is committed.

On Windows PowerShell:

```powershell
$env:JAVA_HOME = "C:\Users\Administrator\toolchains\jdk-21.0.12.1+1"
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleRelease
```

Outputs:

- Debug: `app/build/outputs/apk/debug/app-debug.apk` (package suffix `.debug`, so it can coexist with release builds).
- Release: `app/build/outputs/apk/release/app-release.apk` (package `dev.groceryledger`).
- A convenient signed copy is also at `dist/GroceryLedger-1.1.1-release.apk` with a checksum in `dist/README.txt`.

Install on a connected phone with USB debugging:

```powershell
adb install -r app/build/outputs/apk/release/app-release.apk
```

## Signing and updates

The release signing key was created before any real install:

- Keystore: `C:\Users\Administrator\.groceryledger\groceryledger-release.jks`
- Credentials and backup notes: `C:\Users\Administrator\.groceryledger\README.txt`
- The project reads `keystore.properties` (gitignored) which points at that keystore.

**Back up the keystore and credentials.** Losing them prevents installing updates over the existing app; the only recovery is uninstalling (and restoring data from a CSV backup). Do not commit the keystore or `keystore.properties`.

Installing the release APK over a previous release works as long as:

1. the `applicationId` stays `dev.groceryledger`,
2. the signing key stays the same, and
3. `versionCode` increases.

Debug APKs use `dev.groceryledger.debug` and a debug key; they are for development only. Use a release APK for real data so later releases can update in place.

## Update checks

The app can check for updates from **Settings → Check for updates**.

- Source: the public repository's GitHub Releases (`releases/latest`), not raw Git tags.
- A release tag must look like `v1.1.0`; the app compares it numerically with its installed version.
- The dialog shows the latest version and release notes, then opens the GitHub release page in the browser. The app does not download or install an APK itself, so it does not need install-package permissions.
- Checks are manual only in v1. No background polling and no analytics.
- Release `v1.1.0` is published with the signed APK. An app installed at 1.1.0 reports "You're up to date"; the update dialog appears once a newer tag (e.g. `v1.1.1`) is published.
- Publish the APK as a GitHub release asset; see [docs/RELEASING.md](docs/RELEASING.md).

The app requests `INTERNET` permission solely for this check.

## Data, backup, and privacy

- Data is stored in a local Room database and DataStore preferences.
- The app works offline for all core features. The only outbound request is the manual GitHub update check; it sends no spending data.
- Android Auto Backup is enabled; if system backup is enabled, Android may copy app data to the user's Google account. This is disclosed in Settings → About.
- CSV export is the explicit backup. Import merges by `entry_uid`, skips duplicates, and never deletes or overwrites existing entries.
- "Delete all data" clears entries and suggestion history after confirmation and offers to export first.
- Data is never auto-expired.

CSV format (v1):

```csv
format_version,entry_uid,date,item,amount_inr
1,<uuid>,2026-10-29,Milk,60.00
```

- UTF-8 with BOM, RFC 4180 quoting.
- `format_version` starts at 1; import accepts older versions and rejects newer versions with an "update the app" message.
- Import previews new/duplicate/invalid row counts and reports bad lines.

## Tests

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Coverage includes (46 tests):

- INR parsing, Indian digit grouping, compact cell formatting.
- Week/month boundaries, including month/year-crossing weeks.
- Suggestion ranking.
- CSV round-trip, duplicate/version/invalid-row handling.
- Room repository totals, catalog maintenance, delete/restore, import rebuild.
- Update-version comparison and GitHub release JSON parsing.
- Robolectric activity launch, settings screen, add-sheet, and add-entry flow.

There is no emulator/device attached in this environment, so performance and visual QA on a real phone remain open.

## Updating this app

1. Update `REQUIREMENTS.md` and add a short ADR for significant decisions.
2. Add database changes as migrations; never use destructive migrations.
3. If the CSV changes, bump `format_version` and keep importing older files.
4. Update `docs/KNOWLEDGE_GRAPH.md` and `docs/knowledge-graph.json` for module/feature changes.
5. Run tests, lint, and the release build.
6. Increase `versionCode`, keep the same signing key and package, then install over the previous release and verify existing data.
7. Publish the APK as a GitHub release tagged `vX.Y.Z`; see [docs/RELEASING.md](docs/RELEASING.md).

For LLM handoff, start with [AGENTS.md](AGENTS.md) and [docs/KNOWLEDGE_GRAPH.md](docs/KNOWLEDGE_GRAPH.md).
