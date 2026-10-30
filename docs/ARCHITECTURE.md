# Architecture

## Summary

Grocery Ledger is a single-module Android app with a layered structure:

```
Compose UI
   ↓ events / state
MainViewModel
   ↓ interfaces
ExpenseRepository, SettingsRepository, UpdateChecker
   ↓
Room / DataStore / CSV / GitHub Releases
```

The app is deliberately small. There is no multi-module Gradle split or dependency-injection framework; a single `AppContainer` wires the database, repositories, settings, backup manager, update/download/install helpers, and date provider. The package boundaries keep future extraction or replacement possible.

The core is offline-first. The only network paths are the user-initiated GitHub Releases update check and the update APK download the user starts.

## Layers

### `domain`

Pure Kotlin and tests:

- `Money` parses and formats INR amounts in integer paise. Display uses Indian grouping; cells use compact values; CSV uses fixed two decimals.
- `DateRanges` computes Monday/Sunday weeks, calendar months, 42-cell month grids, and English date labels.
- `SuggestionRanker` ranks suggestions by prefix, word-start, contains, usage, then recency.
- Repository interfaces, models, and the injectable `DateProvider`.

No Android or Room types cross into `domain`.

### `data`

- Room entities/DAOs/database (schema exported to `app/schemas`).
- `OfflineExpenseRepository` implements totals, entries, autocomplete, CRUD, undo restore, import, clearing, and category CRUD.
- Categories are stored by name on entries and listed in the `categories` table (schema v2). Renaming updates every entry using the category; deleting reassigns those entries to `Uncategorized`.
- The `item_catalog` table is a derived cache for suggestions and last-price memory. It is maintained transactionally with entry writes and fully rebuilt after an import. It can always be rebuilt from `expenses`.
- `CsvBackup` is a pure writer/parser for the versioned CSV format. `CsvBackupManager` handles Android `ContentResolver` streams.
- `DataStoreSettingsRepository` stores week start and theme with defaults for missing/unknown values.
- `GitHubUpdateChecker` calls the public GitHub Releases API on IO with 10-second timeouts, compares numeric semver tags, and parses release metadata (including the APK asset's size and SHA-256 digest). It is only invoked from Settings.
- `ApkDownloader` streams the chosen APK with timeouts, a size cap, digest verification, and atomic `.part` promotion. `ApkVerifier` checks package/version/signature; `ApkInstaller` opens Android's installer through a cache-scoped `FileProvider`.
- No third-party network library is used.

### `ui`

- `MainViewModel` owns selected date/month, day sheet visibility, editor state, suggestions, import preview, update state, and snackbar events.
- `CalendarScreen` shows the month, summary card, grid, and day sheet; `EntrySheet` handles add/edit/delete and the date picker; `SettingsScreen` handles preferences, category rename/delete, backup, delete-all, the update check, and the download/install dialogs.
- `UiText` lets the ViewModel emit localized resource-backed snackbar messages without holding a `Context`.
- The theme uses Material 3 dynamic colors on Android 12+ and a green fallback palette below that.

## Key calculations

- Entries store only a calendar date (epoch day), not a timestamp, so timezone changes cannot move an entry.
- Day/week/month totals are SQL `SUM` queries over inclusive date ranges. Weeks are Monday–Sunday by default and can cross months or years; months are calendar months.
- No totals are stored. Room `Flow` queries keep the UI live after every mutation.
- Amounts are integer paise everywhere; floating point is never used for stored values.
- Calendar cells show compact, rounded amounts; the summary and day sheet show exact amounts with paise when present.

## Autocomplete

1. Typing two or more characters debounces for 120 ms.
2. The catalog is searched with an escaped `LIKE` query (maximum 500 candidates).
3. `SuggestionRanker` applies prefix → word-start → contains, then usage, recency, and alphabetical order.
4. Results show the last price and date; the price is a separate chip that fills the amount field.

Catalog maintenance:

- Add/update/delete calls refresh the affected normalized names inside the same transaction.
- If an item has no entries left, its catalog row is removed.
- Import clears and rebuilds the catalog from imported entries.
- Restoring an undone delete reinserts the entry and refreshes the catalog.

## CSV format

Version 1:

```
format_version,entry_uid,date,item,amount_inr
1,<uuid>,2026-10-29,Milk,60.00
```

- UTF-8 with BOM, CRLF records, RFC 4180 quoting.
- `entry_uid` is generated on insert (UUID v4) and preserved across exports/imports. It makes re-imports idempotent.
- Import validates the header, format version, UUIDs, ISO dates (not future), names (1–80 chars), and amounts.
- Valid rows are imported in one transaction; invalid rows are reported by line. Newer format versions are rejected before any write.
- Import is merge-only; it never overwrites or deletes existing entries.

## State and lifecycle

- Selected date is kept in `SavedStateHandle`, so it survives process death; in-progress editor drafts survive configuration changes through the ViewModel.
- `refreshToday()` runs on `ON_RESUME` to handle midnight rollover.
- The day sheet and editor are separate modal sheets; opening one closes the other, and the day sheet reopens after a save/delete that started from it.

## Update check and install

- **Source:** GitHub Releases (`/releases/latest`) for the public repository.
- **Trigger:** only the **Check for updates** button in Settings. No polling or background work.
- **Comparison:** numeric semver of the release tag (`v1.3.1`) against `BuildConfig.VERSION_NAME`; build/pre-release suffixes are ignored and `versionCode` is not available from GitHub.
- **Result:** update available with release notes, up to date, no releases, or a friendly failure (offline, rate limit, server, malformed response).
- **In-app install:** `ApkDownloader` streams the APK asset to `cache/updates/update.apk` through a `.part` file, enforcing a 100 MB cap and verifying GitHub's size and SHA-256 `digest` when present. `ApkVerifier` then checks the package name, a higher `versionCode`, and that the APK's signing certificate matches the installed app. `ApkInstaller` exposes the verified file through a cache-scoped `FileProvider` and opens Android's package installer; `REQUEST_INSTALL_PACKAGES` is only used for that handoff.
- **Fallback:** **Open release page** opens GitHub in the browser and keeps the previous manual install path.
- **Publishing:** signed APK + matching `vX.Y.Z` tag + release notes; same package/key; higher `versionCode`; GitHub supplies the SHA-256 digest checked by the app. See `docs/RELEASING.md`.

## Testing

JVM tests use JUnit 4 + Robolectric:

- Pure tests for money, dates, suggestion ranking, and update version comparison.
- Room in-memory tests for repository behavior.
- CSV round-trip and validation tests.
- GitHub release JSON parsing tests.
- APK download tests (size/checksum/cancellation cleanup) against a local HTTP server.
- Compose/Robolectric smoke tests for activity launch, settings, opening the add sheet, and saving an entry.

There are 58 JVM tests plus 5 instrumentation tests (Room migration and APK verifier). Physical-device performance and visual checks are still pending.

## Update path

- Room schema JSON is committed; every future schema version needs a migration and migration test.
- CSV is versioned; newer app versions import all older versions.
- Keep the `applicationId` and signing key stable; increase `versionCode`; back up the keystore.
- Publish signed APKs as GitHub releases with matching semver tags so the update checker can find them; keep the asset name ending in `.apk` and leave GitHub's computed SHA-256 digest intact.
- Use the version catalog for dependency/SDK changes and run the full build/lint/test before shipping.
