# Project Knowledge Graph

This is the map for LLM sessions and humans joining the project. It complements the requirements and code; it does not replace either.

- **Machine-readable graph:** [`docs/knowledge-graph.json`](knowledge-graph.json) - 80 nodes, 128 edges
- **LLM handoff:** [`AGENTS.md`](../AGENTS.md)
- **Requirements + completion matrix:** [`REQUIREMENTS.md`](../REQUIREMENTS.md)
- **Architecture:** [`docs/ARCHITECTURE.md`](ARCHITECTURE.md)
- **Release process:** [`docs/RELEASING.md`](RELEASING.md)
- **Next feature design:** [`docs/PRICE_HISTORY_DESIGN.md`](PRICE_HISTORY_DESIGN.md) (proposed)

Last updated: 3 November 2026.

Current release: **1.5.1** (versionCode 12), published on GitHub. It adds category filtering, an optional monthly budget, optional quantity/unit pricing (schema v3, CSV v3), a device-only shopping list and a home-screen widget (schema v4), and fixes the 200% font layout of the budget card and shopping list plus raw Markdown in the update dialog. The verified in-app update path is live, so the next device step is confirming 1.4.0 → 1.5.1.

## How to use it in a new session

1. Read `AGENTS.md` first for invariants and commands.
2. Ask the graph for the area you are changing:

```powershell
python tools/knowledge_graph.py autocomplete
python tools/knowledge_graph.py --id data.csv
python tools/knowledge_graph.py --status pending
python tools/knowledge_graph.py --type feature
python tools/knowledge_graph.py --json update
```

3. Open the `path` values on the matching nodes.
4. Check `REQUIREMENTS.md` for acceptance criteria and current status.
5. Make the change, add tests, and update the graph/matrix if the feature or status changes.

The graph is intentionally small: it links the important modules, features, decisions, tests, and external dependencies. It does not duplicate source code.

## Graph at a glance

```mermaid
flowchart TD
    App[Grocery Ledger]

    App --> Activity[MainActivity]
    Activity --> Root[GroceryLedgerApp]
    Activity --> Container[AppContainer]

    Root --> Calendar[CalendarScreen]
    Root --> Settings[SettingsScreen]
    Calendar --> Day[DaySheet]
    Calendar --> Entry[EntrySheet]
    Root --> VM[MainViewModel]

    VM --> Repo[Repository interfaces]
    VM --> Backup[CsvBackupManager]
    VM --> Update[GitHubUpdateChecker]
    VM --> Downloader[ApkDownloader]
    VM --> Installer[ApkInstaller]

    Repo --> Room[Room: expenses + item_catalog + categories]
    Repo --> DataStore[DataStore settings]
    Backup --> CSV[Versioned CSV]
    Update --> GH[GitHub Releases]
    Downloader --> GH
    Downloader --> Verifier[ApkVerifier]
    Installer --> Android[Android package installer]

    Calendar --> Money[Money / INR]
    Calendar --> Dates[DateRanges]
    Entry --> Ranker[SuggestionRanker]
```

## Completed v1

| Area | Status | Where |
| --- | --- | --- |
| Calendar month grid, day totals, navigation, Today | Done | `ui/calendar/CalendarScreen.kt` |
| Day sheet, item list, exact total, empty state | Done | `ui/calendar/DaySheet.kt` |
| Add/edit/delete, undo, save & add another, validation | Done | `ui/entry/EntrySheet.kt`, `ui/MainViewModel.kt` |
| Day / week / month totals, integer paise, INR formatting | Done | `domain/`, `data/local/` |
| Autocomplete, ranking, last price/date, price chip | Done | `domain/suggestion/`, `data/local/` |
| Recent-item quick-add chips (name, price, category) | Done | `ui/entry/`, `data/local/` |
| Per-item price history: change, stats, chart, tap-to-edit | Done | `domain/history/`, `ui/history/`, `data/local/` |
| Category filter for totals, calendar and day sheet | Done | `ui/calendar/`, `data/local/` |
| Optional monthly budget with progress card | Done | `ui/settings/`, `ui/calendar/`, `data/settings/` |
| Optional quantity/unit with derived unit price (schema/CSV v3) | Done | `domain/model/Units.kt`, `ui/entry/`, `data/csv/`, `data/local/` |
| Shopping list with checked-to-entry conversion (schema v4) | Done | `ui/shopping/`, `domain/shopping/`, `data/local/` |
| Home-screen widget with today total and quick add | Done | `widget/`, `MainActivity.kt` |
| Categories: presets, custom, day-sheet label, Settings rename/delete | Done | `ui/entry/`, `ui/settings/`, `data/local/` |
| Monthly category totals under the summary | Done | `ui/calendar/`, `data/local/` |
| CSV export/import, versioning, UID dedupe, preview/errors | Done | `data/csv/`, `data/backup/`, `ui/settings/` |
| Settings: week start, theme, delete all, about | Done | `ui/settings/SettingsScreen.kt` |
| Manual GitHub Releases update check | Done | `data/update/`, `ui/settings/` |
| In-app download, checksum/signature verification, Android installer | Done | `data/update/ApkDownloader.kt`, `ApkVerifier.kt`, `ApkInstaller.kt` |
| Signed release build, stable package/key, versioning | Done | `app/build.gradle.kts`, keystore outside repo |
| Unit, Room, CSV, update/download, and Compose/Robolectric tests | Done — 116 JVM tests + 7 instrumentation tests | `app/src/test/`, `app/src/androidTest/` |
| Lint / build | Done — 0 lint errors | `./gradlew lintDebug` |
| Physical-device visual/performance QA | Pending | Needs a real phone |
| Automatic update checks | Deferred | Decision needed on privacy/UX |
| Spend-intensity shading | Deferred optional requirement | `REQUIREMENTS.md` |

Full per-requirement status is in `REQUIREMENTS.md` section 17.

## Invariants an LLM must preserve

1. Money is integer paise; never floats.
2. Entries store epoch-day dates, not timestamps.
3. Totals are derived SQL sums; never stored.
4. `entry_uid` is stable across export/import; import is merge-only and idempotent.
5. CSV is versioned (`format_version=3`, category plus quantity/unit); older app versions must reject newer files.
6. Room schema changes are additive with migrations; no destructive migrations. Schema v3 adds nullable quantity/unit to entries; schema v4 adds the device-only shopping list.
7. `applicationId` (`dev.groceryledger`) and the release signing key never change.
8. Network use is limited to the user-started GitHub update check and APK download. No analytics, ads, or sync. `REQUEST_INSTALL_PACKAGES` exists only to hand a verified APK to Android's installer.
9. UI → ViewModel → repository interfaces → Room/CSV; UI never imports Room entities.
10. All user-facing strings live in resources; keep accessibility and large-font behavior.

## Important flows

### Add an entry

`CalendarScreen` → `MainViewModel.openAdd` → `EntrySheet` → `save` → `OfflineExpenseRepository.add` → Room transaction + `item_catalog` refresh → Flow emits → summary/day sheet update.

### Autocomplete

Name input → 120 ms debounce → `observeSuggestions` → escaped catalog `LIKE` query (≤500) → `SuggestionRanker` → six suggestions with last price/date.

With the name field empty on a new entry, `EntrySheet` instead shows up to five `recentItems` from `ItemCatalogDao.observeRecent` (newest by `last_used_at`, category taken from that item's latest entry). One tap fills name, last price and last category via `MainViewModel.useRecentItem`.

### Category management

Settings → Categories → rename or delete. Renames update the category row and every entry using it; deletes reassign entries to `Uncategorized` and remove the row. `Uncategorized` is virtual and never edited. Repository methods are `renameCategory` and `deleteCategory`.

### Category totals

The calendar's **By category** card reads `observeCategoryTotals` for the selected month: a grouped SQL sum over `expenses`, ordered by spend. It is collapsible and includes `Uncategorized`; no totals are stored.

### Price history

The day-sheet history icon or the editor's **Price history** button sets `MainViewModel.priceHistory`; `OfflineExpenseRepository.observePriceHistory` queries `expenses` by `name_normalized` (newest first, limit 500) and `PriceHistoryCalculator` derives stats in integer paise (percent in tenths). `PriceHistorySheet` renders the header, stats grid, sampled chart (≤200 points) and the tap-to-edit list.

### Category filter and budget

`MainViewModel.categoryFilter` is part of the calendar key, so every total, daily aggregate and day-sheet query takes a nullable category and re-emits when it changes. The monthly budget lives in DataStore (`monthly_budget_paise`); `BudgetProgress` derives the fraction, remainder and overspend in integer paise, and the calendar shows it only when no filter is active.

### Unit price

Entries may carry `quantity_milli` (thousandths of the unit) and `unit` (kg, g, L, ml, piece). `UnitPrices.perBaseUnitPaise` normalizes to per-kg, per-litre or per-piece for display and never stores the result; the editor previews it live. Schema v3 adds both columns additively, and CSV v3 writes `quantity`/`unit` while still importing v1/v2.

### Shopping list and widget

The list lives in its own `shopping_items` table (schema v4) and is never exported. `ShoppingListConverter` turns checked items with an amount into entries for today and deletes them. The widget asks the repository for today's total on each update (and on app pause) and opens `MainActivity` with a quick-add extra handled by the Compose root.

### CSV restore

File picker → `CsvBackupManager.analyze` → `CsvBackup.analyze` (validate, dedupe by UID) → preview → `importAll` transaction → catalog rebuild → totals/suggestions refresh.

### Update check and install

Settings “Check for updates” → `GitHubUpdateChecker` → GitHub `/releases/latest` → semver comparison → dialog with notes. **Download and install** → `ApkDownloader` (cache, size cap, GitHub SHA-256 digest) → `ApkVerifier` (package, higher versionCode, signing certificate) → `ApkInstaller` (FileProvider) → Android's package installer. **Open release page** remains the browser fallback.

## External dependencies and source

- Repository: <https://github.com/chethankumarsk/personal_grocery_cost_tracker_phone_app>
- Releases: <https://github.com/chethankumarsk/personal_grocery_cost_tracker_phone_app/releases>
- Update API: `https://api.github.com/repos/chethankumarsk/personal_grocery_cost_tracker_phone_app/releases/latest`
- Published releases include **v1.1.0**, **v1.1.1** and **v1.3.1**; the latest is `v1.3.1`, asset `GroceryLedger-1.3.1-release.apk`, SHA-256 `1dda3f5125d157cb5bce222135047d8cca0b130798aa30d519f31c9930071f97`.
- GitHub Releases is preferred over raw Git tags because it provides release notes, a stable latest release, a signed APK asset, and a SHA-256 digest for verification.

## Maintaining the graph

When adding or changing a feature:

1. Add/update nodes in `docs/knowledge-graph.json` for the feature, module, requirement group, or decision.
2. Add edges to the files/modules/tests it uses or implements.
3. Set `status` to `done`, `partial`, `pending`, or `deferred`.
4. Update this document’s summary table and `REQUIREMENTS.md` section 17.
5. Validate:

```powershell
python -c "import json; json.load(open('docs/knowledge-graph.json', encoding='utf-8'))"
python tools/knowledge_graph.py
```

The graph schema is intentionally simple: every node has `id`, `type`, `label`, `status`, and optional `path`/`summary`; every edge has `from`, `to`, and `type`.
