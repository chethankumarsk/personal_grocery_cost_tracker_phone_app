# Grocery Cost Tracker — Requirements

- **Version:** 1.0 (price history)
- **Date:** 3 November 2026
- **Status:** v1 + categories + category management + monthly category totals + recent-item chips + per-item price history + manual check/in-app update implemented; physical-device QA pending
- **App name:** Grocery Ledger
- **Platform:** Android, single user, offline-first, INR

## 0. Implementation status (30 October 2026)

- v1, categories, category management, monthly category totals, and both update paths are implemented in Kotlin + Jetpack Compose with Room and DataStore.
- JVM test suite: 60 tests covering domain logic, CSV, Room repository behavior, category management/totals, update parsing/versioning, APK download verification, entry-sheet state, activity launch, settings, and add-entry/calendar UI flows.
- Instrumentation tests (5): v1→v2 migration preserves entries, defaults them to Uncategorized, and seeds presets; the APK verifier accepts a correctly signed newer build and rejects same-version, wrong-package, and non-APK files.
- Debug and signed release APKs build; `lintDebug` reports no errors.
- Release package: `dev.groceryledger`; version 1.4.0 / versionCode 10; minSdk 26, compile/target SDK 36.
- Release keystore created outside the repository; the first real install should use the signed release APK.
- Network is used only for the user-initiated GitHub Releases check and the update APK download the user explicitly starts.
- LLM/developer context: `AGENTS.md`, `docs/KNOWLEDGE_GRAPH.md`, and the machine-readable `docs/knowledge-graph.json`.
- GitHub release **v1.3.2** is the latest published release; the update checker resolves it. Publishing `v1.4.0` exercises the in-app update path from 1.3.2.
- Remaining: manual visual/performance QA on a physical Android phone and an end-to-end in-app update test against a published newer release.

---

## 1. Summary

A calendar-first Android app for recording grocery purchases item by item and seeing exactly how much was spent on any day, week, or month. Each calendar day can hold any number of entries (item name + cost). Day cells show that day's spend, a summary always shows the selected date's day/week/month totals, and typing an item name suggests previously used items along with the last price paid.

The app is deliberately small: no accounts, no cloud, no ads, no analytics. Spending data is stored locally. The only network features are the manual GitHub Releases update check and the optional in-app update download the user starts; Android's optional system backup may include app data if enabled. Nothing is deleted automatically, and a one-tap CSV export/import provides a backup the user controls, so data can be restored after an accidental clear.

## 2. Goals and success criteria

| Goal | Success criterion |
| --- | --- |
| G1 — Fast entry | A repeat item can be logged in under 10 seconds: tap day → type 2–3 letters → tap suggestion → enter amount → save. |
| G2 — Correct totals | Day, week, and month totals are exact to the paisa and update immediately after any add/edit/delete. |
| G3 — Good recall | Previous items appear as suggestions while typing; the last price and date are visible before confirming. |
| G4 — Private and offline-first | All core features work offline. The app requests no network data except a manual GitHub update check and an optional update download the user starts; it stores everything locally and has no account or app server. |
| G5 — Low friction | No mandatory setup, onboarding, account, or category assignment. |
| G6 — Data safety | Export → clear → import restores all entries, totals, and suggestions with no duplicates; nothing is deleted automatically. |
| G7 — Easy to evolve | Adding a field or feature, bumping Android versions, and shipping an update are localized changes; every update preserves existing data. |

## 3. Scope

### In scope (v1)

- Month calendar with per-day totals.
- Day view listing all items bought that day.
- Add, edit, and delete item entries (name, amount, date).
- Day, week, and month spending totals for the selected date.
- Autocomplete from previously entered item names, with last price/date.
- Minimal settings (week start, theme) and local data safety: Android auto-backup plus CSV export and import.
- Manual update check against the project's GitHub Releases page, with optional in-app download and install plus the release page as a browser fallback.

### Out of scope (v1)

- Accounts, cloud sync, sharing, or multi-device use.
- Income tracking, refunds, or negative amounts.
- Budgets, alerts, or spending limits.
- Receipt photos, OCR, barcode scanning.
- Shopping lists or recurring entries.
- Charts, cross-store price comparison, and category filtering.
- Multi-currency; currency is fixed to INR.
- iOS or web versions.
- Ads, telemetry, or crash reporting that sends data off-device.
- Automatic update polling and in-app APK download/install in v1 (manual check only).

## 4. Target user and key journeys

**Primary user:** One person in India who buys groceries several times a week and wants a lightweight record of what was spent, without a full personal-finance app.

**Journey 1 — Log a shopping trip:** Open app → today is already selected → tap **Add** → type "mi" → pick "Milk" → last price ₹60 is shown → enter ₹62 → Save → add the next item with **Save & add another**.

**Journey 2 — Check spending:** Open app → see today's day/week/month totals → tap a date in the calendar → see the items and that day's total.

**Journey 3 — Fix a mistake:** Tap a day → tap an entry → change the amount or date → save; totals update. Or delete and use **Undo** from the snackbar.

## 5. Functional requirements

Priorities use MoSCoW: **Must**, **Should**, **Could**.

### 5.1 Calendar (home)

- **CAL-1 (Must):** Show a month grid for the current month on launch. Today is visually outlined; the selected day is filled/highlighted.
- **CAL-2 (Must):** Each day cell shows the day number and, when the day has entries, the day's total in compact form. Days with no entries show no amount.
- **CAL-3 (Must):** Previous/next month navigation via arrow buttons. Swipe navigation is a Should. The next-month arrow is disabled when the current month is displayed, because entries cannot be future-dated.
- **CAL-4 (Must):** A **Today** action returns to the current month and selects today.
- **CAL-5 (Must):** Weekday headers and grid columns follow the week-start setting. Days from adjacent months are shown muted; tapping one navigates to that month and selects the date (not allowed for future dates).
- **CAL-6 (Must):** A summary card for the selected date shows three totals with explicit period labels:
  - **Day** — e.g. "Day · 29 Oct"
  - **Week** — e.g. "Week · 26 Oct – 1 Nov"
  - **Month** — e.g. "Month · Oct 2026"
- **CAL-7 (Must):** Tapping a day opens that day's detail sheet. The summary card updates to the selected day.
- **CAL-8 (Must):** The month title is shown as "October 2026". No future dates can be selected or added.
- **CAL-9 (Should):** Spend-intensity shading on day cells (higher spend = slightly stronger tint), using the theme color at low opacity. Never the only indicator of a value.

### 5.2 Day detail

- **DAY-1 (Must):** A bottom sheet for the selected date showing the weekday/date, item count, and day total.
- **DAY-2 (Must):** Entries are listed as rows with item name and amount, in the order they were added (oldest first).
- **DAY-3 (Must):** Tapping a row opens the edit sheet for that entry.
- **DAY-4 (Must):** Empty state: "No items on this day" with an **Add item** button.
- **DAY-5 (Must):** An **Add item** action is always available in the day sheet.
- **DAY-6 (Should):** The sheet is scrollable and handles long lists without layout breakage.

### 5.3 Add / edit / delete entry

- **ENT-1 (Must):** Fields: **Item name** (required), **Amount in ₹** (required), **Date** (required, defaults to the selected calendar day).
- **ENT-2 (Must):** Name is trimmed, must be 1–80 characters, and may contain any Unicode (English, Devanagari, etc.).
- **ENT-3 (Must):** Amount accepts digits and at most two decimal places, must be greater than ₹0 and at most ₹10,00,000 per entry. Numeric keypad with a ₹ prefix. Invalid input shows an inline error and is never written; Save is disabled while a write is in progress.
- **ENT-4 (Must):** Date defaults to the selected day and cannot be later than today. Changing the date moves the entry to that day and updates all affected totals.
- **ENT-5 (Must):** Save writes the entry and closes the sheet; the calendar and totals update immediately. Double-tapping Save must not create duplicates.
- **ENT-6 (Should):** **Save & add another** keeps the date, clears name and amount, and focuses the name field for the next item.
- **ENT-7 (Must):** Editing any field (including name and date) updates the entry, totals, and suggestion data consistently.
- **ENT-8 (Must):** Delete is available from the edit sheet and removes the entry immediately, followed by a 5-second snackbar with **Undo**. No blocking confirmation dialog.
- **ENT-9 (Must):** The same item name may be added more than once on the same day (e.g. two milk purchases); entries are not merged.
- **ENT-10 (Must):** Dismissing the add/edit sheet with unsaved changes asks "Discard entry?" before closing.
- **ENT-11 (Must):** Keyboard flow: name → Next → amount → Save. The amount field opens the numeric keypad.

### 5.4 Autocomplete and price memory

- **SUG-1 (Must):** Suggestions appear after at least 2 characters are typed in the item name field. Maximum 6 suggestions, displayed in a dropdown below the field.
- **SUG-2 (Must):** Matching is case- and whitespace-insensitive, using this rank order:
  1. Name starts with the query.
  2. Any word in the name starts with the query.
  3. Name contains the query anywhere.
  4. Within the same rank: most-used items first, then most recently purchased.
- **SUG-3 (Must):** Each suggestion shows the most recent price paid and the date it was paid, e.g. "Milk — ₹60 on 12 Oct".
- **SUG-4 (Should):** The last price appears as a separate "₹60" chip/button inside the suggestion. Tapping the chip fills the amount field with that price; tapping the name fills only the name and leaves the amount untouched.
- **SUG-5 (Must):** Free text is always allowed; selecting a suggestion is optional and never required.
- **SUG-6 (Must):** Names that differ only by case or extra spaces are treated as the same item for suggestions. The most recent spelling is displayed.
- **SUG-7 (Must):** Suggestions work fully offline and respond within 150 ms with 20,000 entries and 5,000 distinct items.
- **SUG-8 (Must):** With no matching history, no suggestions are shown; the user can still save the new name.
- **SUG-9 (Must):** Deleting or renaming entries keeps suggestion data consistent. An item with no remaining entries disappears from suggestions.
- **SUG-10 (Could):** When the name field is empty, show up to 5 recent item chips for quick re-entry.

**Last-price definition:** the amount of the item's most recent entry by expense date, with creation time breaking ties. It is display-only unless the user taps the price chip.

### 5.5 Totals and calculations

- **TOT-1 (Must):** **Day total** = sum of all entries whose date equals the selected date.
- **TOT-2 (Must):** **Week total** = sum of entries from the start of the week containing the selected date through the following 6 days, using the configured week start (default Monday). A week may span two months or two years.
- **TOT-3 (Must):** **Month total** = sum of entries whose date falls in the calendar month of the selected date. Entries from adjacent months shown in the grid are never included.
- **TOT-4 (Must):** All arithmetic uses integer paise, never floating point. No rounding of stored values.
- **TOT-5 (Must):** Totals and the calendar refresh immediately after any add, edit, delete, or import, without a manual refresh.
- **TOT-6 (Must):** Amounts display in INR with Indian digit grouping (e.g. ₹1,23,456.78). Paise are shown only when non-zero (₹60, not ₹60.00), except in CSV export, which always uses two decimals.
- **TOT-7 (Must):** Calendar cells show amounts rounded to the nearest rupee for glanceability; the day sheet and summary card show exact amounts. Full exact values are available to accessibility services.
- **TOT-8 (Must):** An empty day/week/month shows ₹0, never a blank or an error.
- **TOT-9 (Must):** Dates are based on the device's local calendar date. Entries store a date only, never a time-of-day, so timezone changes cannot move an entry to another day.
- **TOT-10 (Should):** After midnight, "today" refreshes when the app resumes.

**Worked example (Monday week start, today = Thu 29 Oct 2026):**
Entries: 25 Oct ₹100, 26 Oct ₹200, 29 Oct ₹50, 1 Nov ₹75.

- Day (29 Oct) = ₹50.
- Week (26 Oct – 1 Nov) = ₹325.
- Month (Oct 2026) = ₹250 (₹200 + ₹50; 1 Nov is excluded).

With Sunday week start, the week containing 29 Oct is 25–31 Oct and totals ₹350.

### 5.6 Settings

- **SET-1 (Must):** Week starts on **Monday** (default) or **Sunday**. Changing it updates the grid and week totals immediately.
- **SET-2 (Should):** Theme: System / Light / Dark. Material 3 dynamic color is used on Android 12+.
- **SET-3 (Must):** Export all entries to CSV via the Android share/save sheet.
- **SET-4 (Must):** Import a CSV created by the app. Import merges into existing data, skips duplicates by `entry_uid`, previews the counts before writing, and never deletes existing entries. See DAT-6 to DAT-9.
- **SET-5 (Must):** "Delete all data" with a strong confirmation dialog. The dialog offers **Export backup first**. Deletes all entries and suggestion history but keeps preferences.
- **SET-6 (Must):** About section: app version and a plain-language privacy note: core features work offline; network is used only for the manual update check and an update download the user starts; spending data is stored locally; Android system backup may include the app's data if the user has backup enabled. No external links required.
- **SET-7 (Must):** No currency selector; INR is fixed.
- **SET-8 (Must):** A **Check for updates** action that queries GitHub Releases only when tapped. See section 5.8.

### 5.7 Data management

- **DAT-1 (Must):** Entries persist locally across app restarts and updates. Data is never auto-deleted or expired.
- **DAT-2 (Must):** Android Auto Backup is enabled so data can be restored when migrating devices. It is a system-level feature and may upload app data to the user's Google account if system backup is enabled; this is disclosed in About. It is best-effort and does not replace the CSV backup.
- **DAT-3 (Must):** An entry write and its suggestion-catalog update happen in one database transaction; a crash cannot leave them inconsistent.
- **DAT-4 (Must):** CSV export format: RFC 4180 compliant, UTF-8 with BOM for Excel compatibility, columns `format_version,entry_uid,date,item,amount_inr,category`, dates as `yyyy-MM-dd`, amounts always with two decimals, sorted by date then creation order. `format_version` is `2` for this release. Filename `grocery-spending-YYYY-MM-DD.csv`. Future fields are added as new columns with a version bump; existing columns never change meaning.
- **DAT-5 (Must):** The display name of an item in suggestions follows the most recent entry's spelling (latest expense date, then creation time).
- **DAT-6 (Must):** CSV import reads a file through the system file picker. The header is required and columns may be in any order; `format_version` must be an integer the app supports (currently `1`) and each row's `entry_uid` must be a valid UUID. A file with a newer `format_version` is rejected with an "update the app first" message instead of importing a partial file. Unknown extra columns are ignored. Unreadable files or files missing required columns are rejected before anything is written; individual bad rows are handled per DAT-8.
- **DAT-7 (Must):** Import merges by `entry_uid`: rows whose UID already exists (in the app or earlier in the file) are skipped as duplicates. Existing entries are never overwritten or deleted by an import. CSV import is for backup/restore, not bulk editing: changing a row without changing its UID means the change is not applied.
- **DAT-8 (Must):** Before writing, show a preview: number of new entries, duplicates skipped, and invalid rows. A row is invalid if its UID is not a UUID, its date is malformed or in the future, its name is blank or over 80 characters, or its amount is not a valid value within the entry limits. Valid rows are imported in one transaction; invalid rows are reported with line numbers and reasons, and the rest still import. Totals and suggestions refresh afterwards.
- **DAT-9 (Must):** Importing an export into an empty app fully restores the entries and suggestion history, with the same day/week/month totals.
- **DAT-10 (Must):** Data remains until the user deletes individual entries, uses **Delete all data**, or clears/uninstalls the app (which removes the local copy; system backup may restore it). No automatic cleanup or expiry.

### 5.8 Update check and in-app update

- **UPD-1 (Must):** Settings includes **Check for updates**, which contacts the public project's GitHub Releases API (`/releases/latest`) only when the user taps it. No background or automatic polling.
- **UPD-2 (Must):** Compare the latest published release tag (expected `vX.Y.Z`) numerically with the installed `versionName`. Show one of: update available, up to date, no releases published, or a friendly failure.
- **UPD-3 (Must):** On update available, show the latest version, installed version, release notes (plain text), and two actions: **Download and install** when the release has an APK asset, and **Open release page** which opens the GitHub release in the browser (retained as the manual fallback). The app never installs silently; Android's package installer shows its own confirmation.
- **UPD-4 (Must):** Network failures, timeouts, GitHub rate limits, invalid responses, and `404` (no releases) are handled without crashing. Metadata requests use a 10-second connect/read timeout; APK downloads use 15-second connect and 60-second read timeouts. Requests send no spending data.
- **UPD-5 (Must):** Release publishing requires a normal (not draft/pre-release) GitHub release, a tag matching `versionName` with a `v` prefix, a signed APK asset, the same `applicationId`, the same signing key, and a higher `versionCode`. See `docs/RELEASING.md`.
- **UPD-6 (Should, deferred):** An optional daily automatic check with a non-blocking notification. Requires an explicit privacy/UX decision; checks remain manual-only.
- **UPD-7 (Must):** `INTERNET` covers the update check/download. `REQUEST_INSTALL_PACKAGES` is added solely so the in-app path can hand a verified APK to Android's installer. No analytics, accounts, or app server are introduced.
- **UPD-8 (Must):** Before an in-app install, the downloaded file must match GitHub's published size and SHA-256 digest (when the asset provides one), declare the same package name, have a higher `versionCode`, and be signed with a key matching the installed app. A failed check deletes the file and shows a friendly error; a signature mismatch is never installed.
- **UPD-9 (Must):** APK downloads start only on an explicit tap, write to a temporary file in the app cache, never exceed 100 MB, are removed on failure or cancellation, and are exposed to the installer only through a cache-scoped `FileProvider`. No background downloading.

### 5.9 Categories

- **CAT-1 (Must):** Every entry carries exactly one category. The default is **Uncategorized**; choosing a category is optional so quick entry stays two fields.
- **CAT-2 (Must):** A preset list is seeded on first run: Vegetables & Fruits, Dairy & Eggs, Grains & Pulses, Snacks & Beverages, Household, Personal Care, Other.
- **CAT-3 (Must):** New categories can be added from the entry editor; names are trimmed, whitespace-collapsed, case-insensitively unique, at most 40 characters, and cannot be "Uncategorized".
- **CAT-4 (Must):** The day sheet shows the category under the item name, hidden for Uncategorized.
- **CAT-5 (Must):** CSV `format_version` 2 adds a `category` column. v1 files still import and their rows become Uncategorized. Older app versions reject v2 files.
- **CAT-6 (Must):** Schema v2 is additive: an `ALTER TABLE` with a default plus a new `categories` table, seeded by the migration. No entries are lost.
- **CAT-7 (Must):** Settings lists every category except the virtual `Uncategorized` one. A category can be renamed; the new name follows the same validation as adding and all entries using it are updated. A category can be deleted after confirmation; its entries move to `Uncategorized`. `Uncategorized` cannot be renamed or deleted.
- **CAT-8 (Must):** A collapsible **By category** card under the Day/Week/Month summary lists every category with its total and item count for the selected month. `Uncategorized` is included and labelled consistently; categories are ordered by spend, highest first.
- **CAT-9 (Should, deferred):** Filtering the day/week/month summary and day sheet to a single category.

### 5.10 Price history

- **PRICE-1 (Must):** A purchase history is available for any item name, opened from the day sheet or from the edit form. It lists every recorded purchase with date and amount, newest first.
- **PRICE-2 (Must):** The header shows the latest price paid and, once there are two purchases, the change versus the previous purchase as an amount, a percentage in tenths, and the previous price/date.
- **PRICE-3 (Must):** Aggregate stats show average, low, high, and purchase count. All arithmetic is integer paise; the percentage is the only derived ratio and never affects stored money.
- **PRICE-4 (Must):** A trend chart is shown for two or more purchases and is decorative for accessibility; the stats and text carry the meaning.
- **PRICE-5 (Must):** A single purchase shows that purchase plus an explanation that there is nothing to compare yet. No chart is drawn.
- **PRICE-6 (Must):** Tapping a purchase opens that entry in the editor. Editing or deleting entries refreshes the history immediately.
- **PRICE-7 (Must):** History is matched by normalized item name (case and whitespace insensitive), uses the existing `expenses(name_normalized)` index, and caps at the latest 500 purchases with a visible note when truncated.

## 6. Data model

Amounts are stored as 64-bit integer paise. Dates are stored as epoch days (`LocalDate.toEpochDay()`), not timestamps.

### `expenses`

| Field | Type | Notes |
| --- | --- | --- |
| `id` | INTEGER PK | Auto-increment |
| `entry_uid` | TEXT UNIQUE | UUID v4; generated on insert and preserved across export/import |
| `name` | TEXT | As entered, trimmed |
| `name_normalized` | TEXT | Lowercased (root locale), whitespace collapsed |
| `amount_paise` | INTEGER | > 0 |
| `expense_date` | INTEGER | Epoch day |
| `created_at` | INTEGER | Epoch millis |
| `updated_at` | INTEGER | Epoch millis |

Indexes: `expense_date`, `name_normalized`.

### `item_catalog`

Maintained transactionally for fast, ranked suggestions and price memory.

| Field | Type | Notes |
| --- | --- | --- |
| `id` | INTEGER PK | Auto-increment |
| `name` | TEXT | Latest display spelling |
| `name_normalized` | TEXT UNIQUE | Match key |
| `last_price_paise` | INTEGER | From latest entry by date, then creation time |
| `last_expense_date` | INTEGER | Date of that entry |
| `usage_count` | INTEGER | Number of entries; row removed at 0 |
| `last_used_at` | INTEGER | Epoch millis of latest write, for recency ranking |

### Settings

Stored in DataStore Preferences: `week_start` (MONDAY/SUNDAY), `theme` (SYSTEM/LIGHT/DARK).

### Derived values

No totals are stored. Day/week/month totals are computed with `SUM(amount_paise)` over inclusive date ranges and exposed as observable queries.

### Capacity target

Designed to stay responsive at 50,000 entries and 10,000 distinct items; tested at 20,000 entries.

## 7. Screens and navigation

```
Calendar (home)
├── Day sheet (selected date)
│   ├── Add entry sheet
│   └── Edit entry sheet (with Delete)
├── Settings
│   └── Export CSV / Import CSV / Delete all data / About
└── Date picker dialog
```

- Single-activity app, Material 3, no bottom navigation.
- Add/edit are modal bottom sheets; the calendar remains visible behind them.
- The primary action is a FAB with **+** on the calendar, plus **Add item** inside the day sheet.
- Layout works from 320 dp width upward, in portrait; tablets use the same layout centered, with a max content width.
- Dark and light themes follow the system by default; all colors meet WCAG AA contrast.

## 8. Non-functional requirements

- **NFR-1 (Must):** Offline-first. The manifest requests no storage permission; `INTERNET` is used only for the user-initiated GitHub update check. CSV export/import use the system file picker/share sheet.
- **NFR-2 (Must):** Cold start under 2 seconds on a mid-range Android phone; month navigation under 100 ms; suggestions under 150 ms; scrolling at 60 fps.
- **NFR-3 (Must):** No background services, alarms, periodic update polling, or wake locks; negligible battery use.
- **NFR-4 (Must):** Accessibility: TalkBack labels include the date, total, and item count (e.g. "29 October, total ₹450, 3 items"); touch targets ≥ 48 dp; layouts tolerate 200% font scaling; no meaning conveyed by color alone.
- **NFR-5 (Must):** English (India) UI with all strings externalized. Item names accept any Unicode script.
- **NFR-6 (Must):** Android 8.0 (API 26) minimum; compile and target SDK 36 for v1. Future SDK bumps are handled centrally as part of the update checklist.
- **NFR-7 (Must):** No analytics, ads, tracking, or app servers. Network traffic is limited to the manual GitHub Releases update check and the update download the user starts; no spending data is sent. Android's system-level backup may copy app data off-device if enabled and disclosed.
- **NFR-8 (Must):** State survives configuration changes; selected date survives process death. In-progress editor drafts survive configuration changes where reasonable.
- **NFR-9 (Must):** Unit tests for totals, week/month boundaries, INR formatting, suggestion ranking, and update version/JSON parsing; database tests for catalog consistency and import dedupe; UI tests for launch, settings, add-entry, calendar, and add-sheet flows.
- **NFR-10 (Must):** Debug APK under 20 MB.
- **NFR-11 (Must):** Schema migrations are provided for every database version change; existing data is never dropped on upgrade.

## 9. Proposed technical approach

- **Language/UI:** Kotlin, Jetpack Compose, Material 3.
- **Persistence:** Room (KSP), integer paise, epoch days; DataStore Preferences for settings.
- **Async:** Coroutines and Flow; ViewModels expose `StateFlow` UI state.
- **Dates/money:** `java.time` (available from API 26); `BigDecimal` only while parsing/formatting input; `NumberFormat` with `Locale("en","IN")`. "Today" comes from an injectable provider.
- **Architecture:** Single `:app` module, MVVM + repository, manual dependency injection (small app; Hilt is unnecessary overhead). Packages: `data` (Room, CSV, backup, settings, update), `domain` (models, pure calculations, use cases), `ui/calendar`, `ui/entry`, `ui/settings`, `ui/theme`. UI depends on domain; data implements domain-facing repository interfaces.
- **Data evolution:** Exported Room schemas checked into the repo, migration tests, and a versioned CSV mapper. No destructive migrations.
- **Build:** Gradle Kotlin DSL, version catalog, latest stable Android Gradle Plugin and Kotlin, debug/release build types. Release signing config is supplied from outside the repo.
- **Testing:** JUnit4 + kotlin.test for domain logic; Robolectric for Room and Compose UI tests; update parsing/versioning tests. One command runs tests and lint.
- **Network:** No third-party network library. `HttpURLConnection` is used only by `GitHubUpdateChecker` and `ApkDownloader`, with timeouts and friendly failures.

## 10. Error handling and edge cases

- Amount `0`, negative, blank, more than 2 decimals, or above the per-entry cap → inline error; invalid values are never written.
- Name blank or whitespace only → inline error; input trimmed before saving.
- Future date → rejected in the picker and by validation.
- Duplicate fast taps on Save → only one entry created.
- Deleting an entry while its day sheet is open → list and totals update; undo restores both entry and catalog data.
- Editing an entry's name → both old and new catalog rows are recalculated; an unused old item is removed.
- Week spanning a month/year boundary → correct inclusion, verified by tests.
- Device date/timezone changes → entries keep their calendar dates; "today" uses the current local date on resume.
- Very large item names or amounts → truncated display with ellipsis, full text in the editor/detail.
- Empty database → no suggestions, zero totals, empty states everywhere.
- App killed mid-entry → no partial write is committed.
- Import file with a missing/incorrect header → rejected with a clear message; no data changes.
- Import file from a newer app version → rejected with an "update the app first" message; no data changes.
- Import file containing already-present entries → those rows skipped; counts shown.
- Import file with some invalid rows → valid rows imported, invalid rows listed with line numbers; re-importing the corrected file does not duplicate the already-imported rows.
- Import into a non-empty app → merge only; existing entries are never modified or removed.
- Delete all data → confirmation dialog offers an export first; afterwards the empty state is shown and suggestion history is gone.
- Update check offline/timeout → friendly "check your connection" message; no crash and no data changes.
- Update check rate-limited or server error → friendly retry message; no crash.
- No published releases → "No releases yet" message.
- Malformed release metadata → "unexpected response" failure rather than a broken dialog.
- Update available → release notes, **Download and install**, and the browser fallback.
- Download interrupted, oversized, checksum-mismatched, wrong package, older `versionCode`, or wrong signer → file discarded with a friendly message; nothing is installed.
- Install permission not granted → dialog explains, opens Android's "install unknown apps" setting, and continues after returning.
- Android installer unavailable or cancelled → the release page remains available; a verified cached APK can be retried without re-downloading.

## 11. Acceptance scenarios

| # | Scenario | Expected |
| --- | --- | --- |
| S1 | Add "Milk" ₹60 and "Bread" ₹40 on 29 Oct | Calendar cell shows ₹100; day total ₹100; week and month include ₹100 |
| S2 | Add an entry from a past date via calendar | Entry is stored on that date; selected day's totals and calendar update |
| S3 | Entries on 26 Oct and 1 Nov, Monday week start | Week containing 29 Oct totals both; October month total excludes 1 Nov |
| S4 | Edit an amount from ₹60 to ₹65 | Day/week/month totals increase by ₹5 immediately |
| S5 | Delete an entry | Totals decrease; snackbar Undo restores the entry and its suggestions |
| S6 | Type "mi" after having "Milk", "Amul Milk", "Millet" | "Milk" and "Millet" are prefix matches (ordered by usage, then recency); "Amul Milk" follows as a word-start match |
| S7 | Tap the "₹60 on 12 Oct" chip | Amount field becomes ₹60; name is unchanged |
| S8 | Type a brand-new name | No suggestion is required; save succeeds |
| S9 | Kill and reopen the app | All entries and totals persist |
| S10 | Switch week start from Monday to Sunday | Calendar columns shift; week total recalculates for the same selected date |
| S11 | Open a day with no entries | "No items on this day" empty state; totals ₹0 |
| S12 | Navigate to a future month | Next arrow is disabled; no future date can be added |
| S13 | Rotate the device with the add sheet open | Draft input and selected date survive |
| S14 | Export CSV | Valid CSV with `format_version,entry_uid,date,item,amount_inr,category` header and all entries; amounts with 2 decimals |
| S15 | Enable 200% font size | Calendar and summary remain readable without clipped totals |
| S16 | 20,000 entries, type 2 letters | Suggestions in under 150 ms; scrolling stays smooth |
| S17 | Delete all data, then import the export taken beforehand | All entries, totals, and suggestions restored; no duplicates |
| S18 | Import the same file twice | Second import adds 0 entries and reports them as duplicates |
| S19 | Import a file with one invalid amount row | Valid rows imported; invalid row reported with its line number; totals reflect only valid rows |
| S20 | Import a file while entries already exist | New entries merge in; existing entries are untouched |
| S21 | Import a file with a newer `format_version` | Rejected with an "update the app" message; no data changes |
| S22 | Tap Check for updates when the latest release is `v1.0.0` and the app is 1.0.0 | "You're up to date" dialog; no install action |
| S23 | Tap Check for updates when `v1.4.0` is published and the app is 1.3.2 | Update dialog shows 1.4.0 and notes; Download and install verifies and opens Android's installer, and Open release page opens the browser |
| S24 | Tap Check for updates when the repository has no releases | Friendly "No releases yet" dialog |
| S25 | Tap Check for updates while offline or rate-limited | Friendly failure message with retry/close; no crash |
| S26 | Open Settings without tapping Check for updates | No network request is made |
| S27 | Download an update whose checksum or signing key does not match | File is discarded; friendly error; nothing is installed |
| S28 | Tap Download and install without install permission | App explains, opens the permission setting, and installs after returning |
| S29 | Open the By category card for a month with Dairy ₹100 and Snacks ₹20 | Shows Dairy ₹100 (2 items) then Snacks ₹20 (1 item), highest spend first |
| S30 | A month with only Uncategorized entries | By category shows Uncategorized with the month total and item count |

## 12. Assumptions

- **A1:** Single user, local-only, no login.
- **A2:** One entry = one item purchase line. There is no trip/receipt parent.
- **A3:** Week and month totals are calendar periods of the selected date, not rolling 7/30-day windows.
- **A4:** Weeks are Monday–Sunday by default; Sunday–Saturday is available in settings.
- **A5:** Currency is INR only, with Indian digit grouping.
- **A6:** v1 records name, amount, and date; categories were added in 1.2.0. There is still no quantity, unit, store, or notes field.
- **A7:** Future-dated entries are not allowed.
- **A8:** Advertising, accounts, app servers, analytics, and cloud sync are out of scope permanently. The manual GitHub Releases update check and the update download the user starts are the only network features; Android's optional device backup is the only off-device data copy and is disclosed.
- **A9:** Primary use is a phone in portrait; tablets are supported but not optimized separately.
- **A10:** Distribution is a sideloaded APK for now; Play Store publication is a later decision.
- **A11:** Money values use paise precision; the UI accepts whole rupees and optional paise.
- **A12:** CSV is both the manual backup and the restore format; `entry_uid` makes re-imports idempotent.
- **A13:** The app never deletes data on its own. Only an explicit user action or clearing/uninstalling the app removes the local copy; Android system backup, if enabled, is best-effort and may restore it.
- **A14:** The app stays a single Gradle module for v1; package boundaries provide the seams for future growth.
- **A15:** `applicationId` and the final release signing key are fixed before the first build installed for real use; changing either breaks in-place updates.
- **A16:** The CSV `format_version` is 2 (category added) and only increments when columns or their meaning change.
- **A17:** Updates are distributed as GitHub Releases with a signed APK and a `vX.Y.Z` tag. The app offers an in-app download/install path and retains the release page as the manual browser fallback.
- **A18:** Automatic update checks remain deferred until a separate privacy/UX decision. Manual in-app installation is implemented (ADR 0003).

## 13. Decisions and remaining questions

### 13.1 Confirmed decisions (29 October 2026)

| Topic | Decision |
| --- | --- |
| App identity | "Grocery Ledger"; `applicationId` / package `dev.groceryledger`; display name can change later without affecting updates |
| Implementation stack | Kotlin + Jetpack Compose |
| Entry structure | One flat entry per item (no trips) |
| Week definition | Monday–Sunday calendar week for the selected date; calendar month. Sunday start remains a setting. |
| Optional fields | Category is optional and defaults to `Uncategorized`; name, amount, and date are required |
| Backup and restore | CSV export and import. UID-based, idempotent merge; data persists until the user explicitly clears it. |
| Update strategy | Single module with layered/package seams; additive Room migrations with tests; versioned CSV; stable `applicationId` and signing key; one-command build/test. |
| Signing | 4096-bit RSA release key created before the first real install; keystore and credentials stored outside the repository |
| Language/currency | English (India) UI; INR with paise accepted and displayed when non-zero |
| Distribution | Sideloaded signed APK for v1; Play Store is a later decision |
| Update source | GitHub Releases (`/releases/latest`), manual check, semver tag; in-app download/install with checksum + signature verification, plus browser fallback; no background polling |

### 13.2 Remaining validation

| Item | Status |
| --- | --- |
| Physical-device visual and performance QA | Pending |
| Publish the first GitHub release (`v1.1.0` or later) | Done - `v1.3.1` is the latest published release with signed APK |
| End-to-end in-app update against a published newer release | Pending: `v1.4.0` is published; verify the 1.3.2 → 1.4.0 upgrade on a device |
| Optional Play Store publication | Not planned for v1 |

## 14. Recommended future enhancements

These are intentionally excluded from v1 but worth designing for:

1. **Category and monthly breakdown** — where the money went, not just how much.
2. **Monthly budget and progress** — a single target with a simple bar, no complex envelopes.
3. **Home-screen widget / quick-add shortcut** — log a purchase in two taps.
4. **Shopping-list mode** — check items off while shopping, then convert the list into entries.
5. **CSV bulk editing / replace-all restore** — apply changes to existing entries by UID, or wipe and restore exactly. v1 import is merge-only for safety.
6. **Unit-price tracking** — optional quantity + unit (e.g. 1 kg, 500 ml) so ₹/kg can be compared across purchases.
7. **Receipt photo attachment** — optional image per day or trip, stored locally.
8. **Weekly/monthly summary notification** — a local-only recap, if ever wanted.
9. **Optional automatic update check** — a daily check with a non-blocking notification; v1 is manual-only to keep the network policy explicit.

## 15. Maintainability and future updates

**Principle:** v1 stays a single-module app. Updateability comes from clear seams, tests, and data compatibility, not from premature modularization. Prefer simple, direct code and stable platform APIs; add abstraction only where a seam is already planned.

### 15.1 Architecture and code organization

- **MNT-1 (Must):** Clear package boundaries: `data` (Room, CSV, backup, settings, update), `domain` (models, calculations, use cases), `ui` (screens), and `ui/theme` (theme and shared components). UI code must not import Room entities; mapping happens at the data boundary.
- **MNT-2 (Must):** ViewModels depend on repository interfaces; Room and file access live in repositories. Dependencies are wired in one app container (manual DI), and Composables never query the database.
- **MNT-3 (Must):** Totals, week/month ranges, INR parsing/formatting, validation, and suggestion ranking are pure Kotlin with unit tests and no Android dependencies. A single injectable date provider supplies "today".
- **MNT-4 (Must):** Each screen has one immutable UI state object and a clear set of events. No shared mutable state; screen state survives configuration changes.
- **MNT-5 (Must):** One design system for colors, typography, spacing, and shared components. User-facing strings, plurals, dimensions, and colors live in resources; no hard-coded UI text.
- **MNT-6 (Must):** Keep one Gradle module until there is a measured need to split. The package boundaries above must make later extraction possible without a rewrite.

### 15.2 Data and format evolution

- **MNT-7 (Must):** Room schema JSON is exported and committed. Every schema version has a migration test, and destructive migrations are prohibited.
- **MNT-8 (Must):** Schema changes are additive: nullable/defaulted columns or new tables, with existing rows preserved. Totals stay derived and the item catalog stays rebuildable.
- **MNT-9 (Must):** New item metadata (category, store, quantity/unit, notes) can be added as optional fields without changing entry math; the amount remains authoritative.
- **MNT-10 (Must):** CSV is versioned via `format_version` (starting at 1). Import supports all older versions, rejects newer versions with a clear "update the app" message, ignores unknown columns, and always preserves `entry_uid`.
- **MNT-11 (Must):** Settings keys are stable; missing or unknown values fall back to defaults. Changing a setting never rewrites existing entries.
- **MNT-12 (Must):** CSV mapping lives in one place and is covered by round-trip tests. Adding a CSV column touches the mapper and tests, not the UI.

### 15.3 Build, release, and upgrade path

- **MNT-13 (Must):** `applicationId` is finalized before the first release and never changes. In-place updates require the same package, a higher `versionCode`, and the same signing key.
- **MNT-14 (Must):** The release keystore is stored outside the repository and backed up securely. Losing it permanently prevents in-place updates. The first build installed for real use must already use the final signing key; switching from debug to release signing later requires an uninstall (and data restore via CSV).
- **MNT-15 (Must):** `versionCode` increases with every release; `versionName` follows semantic versioning; each release gets a short changelog entry and a git tag.
- **MNT-16 (Must):** Dependency and SDK versions are centralized in a Gradle version catalog; dependencies stay minimal and stable. The Gradle wrapper is committed for reproducible builds, and Android SDK bumps happen in one place.
- **MNT-17 (Must):** Debug and release build types are configured. Release builds are signed (once the keystore exists) and install over the previous release without data loss. The app checks GitHub Releases manually and can hand a verified APK to Android's installer, with the release page retained as a fallback.
- **MNT-18 (Must):** One command builds and tests the app (e.g. `gradlew build`); Android Lint passes with no errors. CI is optional but easy to add.
- **MNT-19 (Must):** No feature flags or remote config in v1 (offline). New features are additive and must not require rewriting the calendar, entry, or totals core.

### 15.4 Documentation and regression safety

- **MNT-20 (Must):** README covers setup, build/test commands, package map, architecture, and release steps. Significant decisions get a short ADR.
- **MNT-21 (Must):** Tests cover totals, week/month boundaries, INR formatting, validation, suggestion ranking, CSV round-trip, and database migrations. Every fixed bug gets a regression test where practical.
- **MNT-22 (Must):** New screens follow existing UI patterns and continue to meet accessibility and localization requirements. This requirements document is updated when behavior changes.

### 15.5 How planned features slot in

| Future feature | Seam already provided |
| --- | --- |
| Per-item price history (done) | Entries already store item, amount, and date; history is one indexed query plus a pure stats calculator. |
| Categories | Add an optional field/table; totals stay amount sums; filtering is additive. |
| Monthly budget | Reads the existing month total; separate settings/table; no entry changes. |
| Quick-add chips (done) | Reuses the suggestion repository and add sheet; no schema change. |
| Home-screen widget | Reads through a repository/use case; does not touch calendar UI. |
| New CSV fields | Version bump; older app versions reject safely; mapper localized. |
| Automatic update check | `GitHubUpdateChecker` is isolated; add a setting and a non-blocking notification without touching core data. |

### 15.6 Checklist for a future change

When adding a feature or field:

1. Update this document; add an ADR if the decision is significant.
2. Add the database change as a migration (nullable/defaulted) and a migration test; never drop existing data.
3. Keep business rules in `domain` and add unit tests before wiring UI.
4. If the change affects CSV, bump `format_version`, keep importing older versions, and add round-trip tests.
5. Update the README/changelog, then run the full build, lint, and tests.
6. Ship with the same `applicationId` and signing key, increment `versionCode`, and verify the update installs over the previous version with data intact.

## 16. Definition of done (v1)

- All **Must** requirements implemented and verified where device testing is not required.
- Unit, database, CSV, update, download-verification, and UI tests pass (60 tests); totals, week/month boundaries, suggestion ranking, import dedupe, CSV versioning, category rename/delete, monthly category totals, update parsing/version comparison, APK checksum/size handling, entry-sheet state, and the UI smoke flows are covered.
- `gradlew build` succeeds and Android Lint reports no errors.
- Room schema JSON is checked in and the v1→v2 migration instrumentation test passes.
- UI code imports no Room entities; totals/date/formatting logic is unit-tested and independent of Android.
- No hard-coded user-facing strings or colors; shared components and the design system are used.
- Manual QA on at least Android 8, 12, 14, and 16 (emulator acceptable for older versions) is complete.
- `INTERNET` exists only for the manual GitHub update check/download; `REQUEST_INSTALL_PACKAGES` exists only for the in-app installer handoff; no crashes in add/edit/delete/undo/import/update-check flows.
- Update check handles up-to-date, available, no-releases, offline, rate-limited, and malformed responses. In-app install refuses a bad checksum, wrong package, older version, or different signer.
- Fresh install → add entries → kill app → reopen: data intact.
- Export → delete all data → import the export: all entries, totals, and suggestions restored; importing again adds nothing.
- Importing an older-format CSV works; importing a newer format is rejected without changing data.
- No data is removed without an explicit user action.
- APK installs on a clean device and meets the size/performance targets.
- README, architecture notes, knowledge graph, and release steps exist.
- `applicationId`, versioning, and release signing are configured according to MNT-13 to MNT-17; the first build installed for real use is signed with the final key.
- For every release after v1: install it over the previous release, verify existing data survives, and record the changelog and tag.
- A GitHub release is published only with a signed APK, matching `vX.Y.Z` tag, and the same key/package.
- This document is updated to reflect any decisions made during implementation.

## 17. Implementation status matrix (29 October 2026)

**Status values:** **Done** = implemented and verified where automated verification is possible; **Partial** = implemented but a manual/device or deferred sub-item remains; **Pending** = not done; **Deferred** = intentionally out of v1.

### 17.1 Functional requirements

| Requirement | Status | Evidence / remaining work |
| --- | --- | --- |
| CAL-1..CAL-8 | Done | Calendar grid, per-day totals, month navigation, Today, adjacent days, future-date blocking, summary card, day sheet. |
| CAL-9 | Deferred | Optional spend-intensity shading not implemented. |
| DAY-1..DAY-6 | Done | Day header, item rows, exact total, empty state, add/edit, scroll handling. |
| ENT-1..ENT-11 | Done | Add/edit/delete/undo, save-and-add-another, validation, discard prompt, date rules, duplicate-tap guard. |
| SUG-1..SUG-9 | Done | Debounced suggestions, ranking, last price/date, price chip, normalization, offline behavior, catalog cleanup. |
| SUG-10 | Done | Up to five recent-item chips under an empty name field; one tap fills name, last price and last category. |
| TOT-1..TOT-10 | Done | Exact paise sums, Monday/Sunday weeks, calendar months, Indian formatting, empty zero, local-date semantics, midnight refresh. |
| SET-1..SET-8 | Done | Week start, theme, CSV export/import, delete all, about/privacy, INR, manual update check with in-app install and browser fallback. |
| DAT-1..DAT-10 | Done | Local persistence, auto backup, transactional catalog, versioned CSV, UID merge, preview/errors, restore, no expiry. |
| UPD-1..UPD-5, UPD-7..UPD-9 | Done | Manual GitHub Releases check, semver comparison, notes + Download and install + browser fallback, checksum/package/version/signature verification, friendly failures, release policy, scoped permissions. |
| UPD-6 | Deferred | Optional automatic daily check; checks remain manual-only. |
| CAT-1..CAT-8 | Done | Category column + `categories` table (schema v2), presets, editor chips with "+ New category", day-sheet label, CSV v2 with v1 import, Settings rename/delete with entry reassignment, monthly category totals card. |
| CAT-9 | Deferred | Category filtering of the summary and day sheet. |
| PRICE-1..PRICE-7 | Done | Item history sheet with latest/previous price, amount + percent change, average/low/high/count, trend chart, purchase list and tap-to-edit; name-normalized query, 500-point cap. |

### 17.2 Non-functional requirements

| Requirement | Status | Evidence / remaining work |
| --- | --- | --- |
| NFR-1 | Done | Offline core; INTERNET only for the update check/download; REQUEST_INSTALL_PACKAGES only for the installer; no storage permission. |
| NFR-2 | Partial | Targets defined; physical-device timing not measured. |
| NFR-3 | Done | No background services, alarms, polling, or wake locks. |
| NFR-4 | Partial | Content descriptions, 48 dp targets, scalable layouts implemented; TalkBack/200% font device QA pending. |
| NFR-5 | Done | All strings externalized; Unicode names accepted. |
| NFR-6 | Done | minSdk 26; compile/target SDK 36. |
| NFR-7 | Done | No analytics/ads/servers; manual GitHub check plus user-started APK download; backup disclosed. |
| NFR-8 | Partial | Selected date survives process death; editor draft survives configuration changes but not process death. |
| NFR-9 | Done | 81 JVM tests across money, dates, ranking, CSV, Room, category management/totals, recent-item chips, price history, update/download logic, entry-sheet state, and Compose UI smoke flows. |
| NFR-10 | Done | Release APK ~8 MB; debug under 20 MB. |
| NFR-11 | Done | Migration policy and exported schemas in place; v2 migration and its instrumentation test pass. |

### 17.3 Maintainability requirements

| Requirement | Status | Evidence / remaining work |
| --- | --- | --- |
| MNT-1..MNT-6 | Done | Layer/package separation, repository interfaces, pure domain, manual DI, design system, single module. |
| MNT-7 | Done | Schema JSON exported; the v1→v2 migration is covered by an instrumentation test. |
| MNT-8..MNT-13 | Done | Additive policy, rebuildable catalog, versioned CSV, stable settings keys, UID stability, stable applicationId, signed release. |
| MNT-14 | Done | Release keystore outside the repo; first real install should use the final release-signed APK. |
| MNT-15 | Partial | versionCode/versionName/changelog present; no git tag yet because Git is not installed in the build environment. |
| MNT-16 | Done | Version catalog and wrapper committed. |
| MNT-17 | Done | Debug/release variants; signed release builds; manual update check; verified in-app download/install plus browser fallback. |
| MNT-18 | Done | One-command tests/lint/build; 0 lint errors. |
| MNT-19 | Done | No feature flags or remote config; additive seams. |
| MNT-20 | Done | README, architecture, ADRs, release guide, AGENTS.md, knowledge graph. |
| MNT-21 | Done | Broad JVM tests pass (60) plus migration and APK verifier instrumentation tests (5). |
| MNT-22 | Partial | Patterns followed; physical-device accessibility QA pending. |

### 17.4 Acceptance scenarios

| Scenario | Status | Evidence / remaining work |
| --- | --- | --- |
| S1 | Partial | Add-entry UI flow tested; calendar cell/summary rendering not asserted on device. |
| S2 | Partial | Past-date add path implemented; manual UI check pending. |
| S3 | Done | Week/month range and repository total tests. |
| S4 | Partial | Edit path implemented; manual UI check pending. |
| S5 | Partial | Repository delete/restore test; snackbar undo UI manual. |
| S6 | Done | SuggestionRanker tests. |
| S7 | Partial | Price chip implemented; manual UI check pending. |
| S8 | Partial | Free-text save path implemented; manual UI check pending. |
| S9 | Partial | Repository/activity persistence tested; real kill/reopen manual. |
| S10 | Partial | Week-start date math tested; settings switching manual. |
| S11 | Partial | Empty-state implementation; manual check. |
| S12 | Partial | Future-date/next-arrow logic; manual check. |
| S13 | Partial | ViewModel state retention; rotation manual. |
| S14 | Done | CSV writer/parser tests; file-picker UI manual. |
| S15 | Pending | 200% font device QA. |
| S16 | Pending | 20,000-entry performance QA. |
| S17 | Done | CSV round-trip + repository import/rebuild tests. |
| S18 | Done | CSV duplicate test. |
| S19 | Done | CSV invalid-row test. |
| S20 | Done | CSV merge/repository import test. |
| S21 | Done | Newer-format rejection test. |
| S22 | Partial | Version comparison logic tested; live GitHub/UI manual. |
| S23 | Partial | Release parsing/update-available logic tested; downloader/checksum tests pass; live GitHub download + installer needs a published newer release. |
| S24 | Partial | 404/no-release handling implemented; live check manual. |
| S25 | Partial | Failure mapping implemented; offline/rate-limit UI manual. |
| S26 | Partial | Code only contacts network on tap; manual verification pending. |
| S27 | Partial | Checksum/size unit tests pass; package/version/non-APK checks pass on device; a wrong-signer APK is rejected by the implemented comparison but needs a second signed fixture for a direct test. |
| S28 | Partial | Permission flow implemented; manual device check pending. |
| S29 | Done | Repository aggregation test plus the expandable-card Compose test. |
| S30 | Done | Uncategorized is included by the same aggregation and labelled in the card. |

### 17.5 Automated test inventory

| Test class | Tests |
| --- | --- |
| MoneyTest | 6 |
| DateRangesTest | 8 |
| SuggestionRankerTest | 5 |
| CsvBackupTest | 10 |
| OfflineExpenseRepositoryTest | 9 |
| VersionComparatorTest | 3 |
| GitHubReleaseParserTest | 5 |
| ApkDownloaderTest | 5 |
| EntrySheetStateTest | 4 |
| MainActivityTest | 1 |
| CalendarUiTest | 3 |
| AddEntryUiTest | 1 |
| **Total** | **60** |

Instrumented: `GroceryDatabaseMigrationTest` (1) covers v1→v2 entry preservation and preset seeding; `ApkVerifierTest` (4) covers package, version, signature acceptance, and non-APK rejection against the installed APK.

Build verification: `testDebugUnitTest` passes, `lintDebug` reports 0 errors, and the release APK verifies with apksigner.

### 17.6 Remaining work

1. Manual device QA: visual layout, TalkBack, 200% font, performance, install-over-update, and the in-app permission/installer dialogs.
2. ~~Publish the first GitHub release with the signed APK~~ - done: `v1.4.0` is the latest published release. Next: verify the 1.3.2 → 1.4.0 upgrade with **Download and install**.
3. Optional: automatic update checks, spend-intensity shading.
