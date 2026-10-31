# AGENTS.md — Grocery Ledger

This file is the starting context for any LLM or developer picking up this project in a new session. Read it before changing code.

## Read next

1. `docs/KNOWLEDGE_GRAPH.md` — human-readable map, invariants, and current status
2. `REQUIREMENTS.md` — requirements plus the implementation-status matrix
3. `docs/ARCHITECTURE.md` — layers, data flow, CSV format, update path
4. `README.md` — build, install, signing, and release notes
5. `docs/RELEASING.md` — how updates are published

For a code-free query of the machine-readable graph:

```powershell
python tools/knowledge_graph.py autocomplete
python tools/knowledge_graph.py --id data.csv
python tools/knowledge_graph.py --status pending
```

## Identity

- App: **Grocery Ledger**
- Package / application ID: **`dev.groceryledger`**
- Current version: **1.3.2** / versionCode **9**
- Repository: `https://github.com/chethankumarsk/personal_grocery_cost_tracker_phone_app`
- Platform: Android 8.0+ (minSdk 26), compile/target SDK 36
- Stack: Kotlin, Jetpack Compose, Material 3, Room, DataStore, coroutines
- Release signing key: `C:\Users\Administrator\.groceryledger\groceryledger-release.jks`
  (credentials beside it; `keystore.properties` in the repo root is gitignored)

## Non-negotiable invariants

1. **Money is integer paise.** Never store or sum money as floating point.
2. **Dates are epoch days.** An entry has a calendar date, not a timestamp; timezone changes must not move it.
3. **Totals are derived.** Day/week/month values are SQL sums over inclusive ranges; do not store totals.
4. **`entry_uid` is stable.** Generate UUIDs on insert and preserve them across export/import. Import is merge-only and idempotent.
5. **CSV is versioned.** `format_version` is **2** (v2 adds `category`). New versions must import all older versions; older apps reject newer files. Do not silently change column meanings.
6. **No destructive Room migrations.** The database is at **version 2** (categories). Export schema JSON, add migrations and migration tests, preserve existing data. The catalog is rebuildable; entries are not.
7. **`applicationId` and signing key are permanent.** Changing either breaks in-place updates. Never commit the keystore, `keystore.properties`, `local.properties`, or `dist/`.
8. **Network policy:** the app makes no network calls except the user-initiated GitHub Releases update check and the update APK download the user starts. No analytics, ads, or background sync. The only install-related permission is `REQUEST_INSTALL_PACKAGES`, used solely to hand a verified APK to Android's installer. Keep it that way unless requirements are explicitly changed.
9. **UI does not touch Room.** Compose → `MainViewModel` → repository interfaces → Room/CSV. Business rules stay in pure Kotlin.
10. **All user-facing text is in `res/values/strings.xml`.** Keep accessibility descriptions, 48 dp targets, and large-font behavior.

## Layout

```
domain/        models, money, date ranges, suggestion ranking, repository interfaces
data/local/    Room entities, DAOs, database, offline repository
data/csv/      pure CSV writer/parser and import models
data/backup/   Android file-picker integration
data/settings/ DataStore preferences
data/update/   GitHub Releases checker and version comparison
ui/            MainViewModel, Compose screens, entry sheet, settings, theme
```

## Build and verify

```powershell
$env:JAVA_HOME = "C:\Users\Administrator\toolchains\jdk-21.0.12.1+1"
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleRelease
```

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (package `dev.groceryledger.debug`)
- Release APK: `app/build/outputs/apk/release/app-release.apk` (package `dev.groceryledger`)
- Convenience copy: `dist/GroceryLedger-1.3.2-release.apk`

Use a release-signed APK for real data. Debug builds cannot be updated into release builds.

## Current status

- v1 feature set is implemented: calendar, day/week/month totals, monthly category totals, add/edit/delete/undo, autocomplete + last price, categories (including rename/delete in Settings), CSV export/import, settings, update check.
- 60 JVM tests and 5 instrumentation tests (Room migration + APK verifier) pass; lint has 0 errors.
- Signed release APK builds (1.3.2 / versionCode 9).
- Physical-device visual/performance QA is pending.
- Emulator QA passes: AVD `gl36` (Android 16 / API 36, AEHD 2.2) runs the UiAutomator suite in
  `D:\githubProjects\grocery_ledger_avd_aehd_automation` — 6/6, including the add-entry sheet
  layout regressions. Verified on a Nothing Phone 3a (Android 16, Nothing OS 4.1) over adb with
  Gboard; physical-device visual/accessibility QA is still outstanding.
- GitHub release **v1.3.1** is the latest published release (signed APK asset, release notes). The update checker finds it; an app already on 1.3.1 reports "You're up to date".
- In-app update path is implemented (download → size/digest check → package/version/signature check → Android installer) with the release page retained as fallback. Publishing 1.3.2 from 1.3.1 exercises it end to end.
- To exercise the "update available" path, publish `v1.3.2` from 1.3.1 and use Download and install. GitHub computes the asset `digest` used for checksum verification.

## When making a change

1. Update `REQUIREMENTS.md` status if behavior changes.
2. Add a migration for any schema change, plus a migration test.
3. Keep domain logic pure and add/adjust tests.
4. If CSV changes, bump `format_version` and add a round-trip/old-version test.
5. Update `docs/KNOWLEDGE_GRAPH.md` and `docs/knowledge-graph.json` when modules/features/status changes.
6. Update `README.md`/`CHANGELOG.md` for user-visible changes.
7. Build, test, lint, bump `versionCode`, keep the same key/package, and verify an in-place install. For local releases, commit/push in GitHub Desktop and run the gitignored `tools\publish-release.cmd -Build`; it verifies the APK, writes `dist\*.sha256` and release notes, and opens the GitHub release page.
