# Grocery Ledger

A small, offline-first Android app for tracking grocery spending in INR. The home screen is a calendar; each day holds item entries, and the selected day shows day, week, and month totals. Item names autocomplete from previous purchases, including the last price paid.

- **Package:** `dev.groceryledger`
- **Version:** 1.4.0 (versionCode 10)
- **Platform:** Android 8.0+ (minSdk 26), compile/target SDK 36
- **UI:** Kotlin + Jetpack Compose + Material 3
- **Storage:** Room + DataStore, entirely on-device
- **Requirements:** [REQUIREMENTS.md](REQUIREMENTS.md)
- **LLM/developer handoff:** [AGENTS.md](AGENTS.md), [docs/KNOWLEDGE_GRAPH.md](docs/KNOWLEDGE_GRAPH.md)
- **Release process:** [docs/RELEASING.md](docs/RELEASING.md)

## Status

The v1 feature set, categories with Settings management, monthly category totals, recent-item chips, per-item price history, and both update paths are implemented. 81 JVM tests and 5 instrumentation tests pass, lint reports 0 errors, and a signed 1.4.0 release APK builds. Manual QA on a physical phone is still recommended before daily use.

## Features

- Month calendar with per-day totals and today/selected highlighting.
- Day sheet listing items, item count, and exact total.
- Add, edit, and delete entries, with a 5-second undo snackbar.
- Day, week (Mon–Sun by default), and month totals for the selected date.
- Autocomplete after two characters, ranked by prefix, word-start, contains, then usage/recency.
- Last price/date shown per suggestion, with a chip to reuse the previous price.
- Up to five recent-item chips on a new entry: one tap restores the name, last price and last category.
- Per-item **price history** from the day sheet or the edit form: latest and previous price, change amount and percentage, average/low/high, a trend chart, and every purchase (tap one to edit it).
- "Save & add another" for entering a whole shopping trip quickly.
- Settings for week start (Monday/Sunday) and theme (system/light/dark).
- CSV export and duplicate-safe import, including versioned backups.
- Categories per entry: presets plus your own, optional with `Uncategorized` as the default, shown in the day sheet.
- Category management in Settings: rename a category (entries follow) or delete it (entries move to Uncategorized), with confirmation.
- Collapsible **By category** monthly totals under the Day/Week/Month summary, including Uncategorized.
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
- A convenient signed copy is also at `dist/GroceryLedger-1.4.0-release.apk`, with its checksum in `dist/README.txt` and `dist/GroceryLedger-1.4.0-release.apk.sha256`.

Install on a connected phone with USB debugging:

```powershell
adb install -r app/build/outputs/apk/release/app-release.apk
```

**Play Protect note:** this app is sideloaded and can install its own updates, so Android may show a warning even for a genuine build. See [Play Protect and sideload warnings](#play-protect-and-sideload-warnings) for verification steps before installing.

## Play Protect and sideload warnings

Grocery Ledger is distributed as a signed APK on GitHub, not through Google Play. Google Play Protect may show **“App may be harmful”**, **“Unsafe app blocked”**, or a generic scan warning when installing it. That is a reputation/sideload heuristic, not a malware verdict. It happens because:

- the app is not distributed or Play-signed by Google Play;
- the signing certificate is new and has little install history;
- the app requests `REQUEST_INSTALL_PACKAGES` so it can install its own updates. Self-updating apps are treated as higher risk by Play Protect, even when every update is verified.

The app has no analytics, ads, accounts, or app server. Spending data stays on-device, and network use is limited to the GitHub update check/download that you start. In-app installation is optional; **Open release page** remains available if you prefer not to grant install permission.

### Verify the APK before installing

Download only from this repository’s **Releases** page, never from a mirror, forwarded file, or messaging app. From 1.3.1 onward, release assets include the APK and a matching `.sha256` file.

PowerShell:

```powershell
Get-FileHash .\GroceryLedger-1.4.0-release.apk -Algorithm SHA256
```

Linux/macOS:

```bash
sha256sum -c GroceryLedger-1.4.0-release.apk.sha256
```

Verify the signing certificate (the path to `apksigner` depends on your Android SDK):

```powershell
apksigner verify --print-certs .\GroceryLedger-1.4.0-release.apk
```

The signing certificate SHA-256 is stable across releases:

```text
2d05264a172287b9f2eb022fe30c7cc30ab3c813ace9e8dfc524d49a0ffbf90f
```

Use the `.sha256` file attached to the release you downloaded; it is the authoritative checksum for that APK. Rebuilding the same version can change the APK hash, so do not compare against a hash copied from the README or release notes of another build.

The package must be `dev.groceryledger` with versionCode `10` and versionName `1.4.0`. If the hash or certificate does not match, stop and re-download from the Releases page.

### If Play Protect warns or blocks the install

1. Tap **More details** → **Install anyway** if that option is shown.
2. If the install is blocked with no bypass, temporarily open **Play Store → profile → Play Protect → gear → Scan apps with Play Protect**, turn scanning off, install, then turn it back on and run a scan.
3. On Android 13+, if **Install unknown apps** is greyed out for Grocery Ledger, open **Settings → Apps → Grocery Ledger → ⋮ → Allow restricted settings**, then retry.
4. After installing, re-enable Play Protect. Do not leave it disabled permanently.

There is no per-app allowlist, so Play Protect may warn again after an in-app update. The hash and certificate checks above are the reliable way to confirm that a build came from this project.

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
- A release tag must look like `v1.4.0`; the app compares it numerically with its installed version.
- The dialog shows the latest version and release notes, with two choices:
  - **Download and install** downloads the APK asset into the app cache, verifies its size and GitHub's SHA-256 digest (when present), then verifies the package name, a higher `versionCode`, and that it is signed with the same key as the installed app. Android's package installer then shows its own confirmation. The first time, Android asks you to allow installs from Grocery Ledger.
  - **Open release page** is the previous browser flow: open the GitHub release and install the signed APK yourself. It remains available when a release has no APK asset or the in-app path fails.
- The app never installs silently and never updates itself in the background.
- Checks are manual only. No background polling and no analytics.
- The latest published release is `v1.4.0`. From 1.3.2, publishing `v1.4.0` makes **Download and install** testable end to end; older builds must be updated manually once.
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

Coverage includes (60 tests):

- INR parsing, Indian digit grouping, compact cell formatting.
- Week/month boundaries, including month/year-crossing weeks.
- Suggestion ranking.
- CSV round-trip, duplicate/version/invalid-row handling.
- Room repository totals, catalog maintenance, delete/restore, import rebuild.
- Category rename/delete, entry reassignment, and duplicate/reserved name rejection.
- Monthly category totals aggregation and the expandable calendar card.
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
