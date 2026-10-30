# Grocery Ledger

A small, offline-first Android app for tracking grocery spending in INR. The home screen is a calendar; each day holds item entries, and the selected day shows day, week, and month totals. Item names autocomplete from previous purchases, including the last price paid.

- **Package:** `dev.groceryledger`
- **Version:** 1.3.1 (versionCode 8)
- **Platform:** Android 8.0+ (minSdk 26), compile/target SDK 36
- **UI:** Kotlin + Jetpack Compose + Material 3
- **Storage:** Room + DataStore, entirely on-device
- **Requirements:** [REQUIREMENTS.md](REQUIREMENTS.md)
- **LLM/developer handoff:** [AGENTS.md](AGENTS.md), [docs/KNOWLEDGE_GRAPH.md](docs/KNOWLEDGE_GRAPH.md)
- **Release process:** [docs/RELEASING.md](docs/RELEASING.md)

## Status

The v1 feature set, categories with Settings management, and both update paths are implemented. 58 JVM tests and 5 instrumentation tests pass, lint reports 0 errors, and a signed 1.3.1 release APK builds. Manual QA on a physical phone is still recommended before daily use.

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
- Categories per entry: presets plus your own, optional with `Uncategorized` as the default, shown in the day sheet.
- Category management in Settings: rename a category (entries follow) or delete it (entries move to Uncategorized), with confirmation.
- Manual "Check for updates" against GitHub Releases, with version comparison.
- In-app **Download and install** (checksum + signing-key verified, Android's installer confirms) or **Open release page** as the browser fallback.
- No accounts, ads, or analytics. Network is used only when you check for updates or download one.

## Project layout

```
app/src/main/java/dev/groceryledger/
├── data/
│   ├── local/        Room entities, DAOs, database, repository
│   ├── csv/          CSV writer/parser, import models
│   ├── backup/       File picker export/import manager
│   ├── update/       GitHub release check, APK download/verify/install, version comparison
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
- A convenient signed copy is also at `dist/GroceryLedger-1.3.1-release.apk` with a checksum in `dist/README.txt`.

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
- A release tag must look like `v1.3.1`; the app compares it numerically with its installed version.
- The dialog shows the latest version and release notes, with two choices:
  - **Download and install** downloads the APK asset into the app cache, verifies its size and GitHub's SHA-256 digest (when present), then verifies the package name, a higher `versionCode`, and that it is signed with the same key as the installed app. Android's package installer then shows its own confirmation. The first time, Android asks you to allow installs from Grocery Ledger.
  - **Open release page** is the previous browser flow: open the GitHub release and install the signed APK yourself. It remains available when a release has no APK asset or the in-app path fails.
- The app never installs silently and never updates itself in the background.
- Checks are manual only. No background polling and no analytics.
- The latest published release is `v1.1.1`. Install 1.3.1 manually once (1.2.0 predates the button); after that, publishing a newer tag such as `v1.3.2` makes **Download and install** testable end to end.
- Publish the APK as a GitHub release asset; GitHub supplies the SHA-256 digest used for verification. See [docs/RELEASING.md](docs/RELEASING.md).

The app requests `INTERNET` for the update check/download and `REQUEST_INSTALL_PACKAGES` solely so the in-app path can hand a verified APK to Android's installer.

## Data, backup, and privacy

- Data is stored in a local Room database and DataStore preferences.
- The app works offline for all core features. Outbound requests happen only for the manual GitHub update check and the update download you explicitly start; they send no spending data.
- Android Auto Backup is enabled; if system backup is enabled, Android may copy app data to the user's Google account. This is disclosed in Settings → About.
- CSV export is the explicit backup. Import merges by `entry_uid`, skips duplicates, and never deletes or overwrites existing entries.
- "Delete all data" clears entries and suggestion history after confirmation and offers to export first.
- Data is never auto-expired.

CSV format (v2):

```csv
format_version,entry_uid,date,item,amount_inr,category
2,<uuid>,2026-10-29,Milk,60.00,Dairy & Eggs
```

- UTF-8 with BOM, RFC 4180 quoting.
- `format_version` is 2; import accepts v1 (rows become `Uncategorized`) and rejects newer versions with an "update the app" message. v2 backups cannot be read by app versions before 1.2.0.
- Import previews new/duplicate/invalid row counts and reports bad lines.

## Tests

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Instrumented tests (Room migration + APK verifier) need a connected device or emulator:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

Coverage includes (58 tests):

- INR parsing, Indian digit grouping, compact cell formatting.
- Week/month boundaries, including month/year-crossing weeks.
- Suggestion ranking.
- CSV round-trip, duplicate/version/invalid-row handling.
- Room repository totals, catalog maintenance, delete/restore, import rebuild.
- Category rename/delete, entry reassignment, and duplicate/reserved name rejection.
- Update-version comparison, GitHub release JSON parsing (including digest/size), and APK download checksum/size handling.
- Robolectric activity launch, settings screen, add-sheet, and add-entry flow.
- Room v1→v2 migration (instrumentation): existing entries survive and become Uncategorized; presets are seeded.
- APK verifier (instrumentation): accepts a correctly signed newer APK and rejects same-version, wrong-package, and non-APK files.

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
