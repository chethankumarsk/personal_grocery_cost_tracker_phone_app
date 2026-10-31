# Changelog

## 1.3.2 — 31 October 2026

- **Monthly category totals.** A new collapsible **By category** card under the Day/Week/Month summary shows every category's total and item count for the selected month.
- `Uncategorized` is included and labelled consistently, so the category totals add up to the month total.
- Categories are ordered by spend, highest first.
- Adds repository and Compose tests for the breakdown (60 JVM tests total).

## 1.3.1 — 30 October 2026

- **Manage categories in Settings.** Every category can now be renamed or deleted from a new Categories section.
- Renaming a category updates all entries that use it; names are still trimmed, case-insensitively unique, at most 40 characters, and cannot be `Uncategorized`.
- Deleting a category asks for confirmation and moves its entries to `Uncategorized`. `Uncategorized` itself is virtual and cannot be renamed or deleted.
- Adds four repository tests for category rename/delete, including duplicate/reserved names and entry reassignment (58 JVM tests total).

## 1.3.0 — 30 October 2026

- **In-app updates.** The update dialog now offers **Download and install** as well as the existing **Open release page** browser flow. Nothing installs silently: Android's installer shows its own confirmation.
- The APK is checked against GitHub's published size and SHA-256 digest, then verified to be the same package, a higher `versionCode`, and signed with the same key as the installed app before it is handed to Android. Failed verification discards the file.
- Adds `REQUEST_INSTALL_PACKAGES` and a cache-only `FileProvider`; Android may ask once for permission to install unknown apps. Downloads happen only after you tap the button, use a 100 MB cap, and are cleaned up on failure or cancellation.
- Adds the Room v1→v2 migration instrumentation test (entries preserved, presets seeded) and APK verifier instrumentation tests (package/version/signature acceptance and rejection).
- Network use is unchanged otherwise: no background checks, analytics, or app server.

## 1.2.0 — 30 October 2026

- **Categories for entries.** Every entry now carries a category: Vegetables & Fruits, Dairy & Eggs, Grains & Pulses, Snacks & Beverages, Household, Personal Care, Other, plus any you add yourself from the editor ("+ New category").
- Categories are optional and default to **Uncategorized**, so adding an entry is still two fields and existing data is untouched.
- The day sheet shows the category under the item name.
- Database schema v2 with an additive migration; existing entries become Uncategorized and the preset list is seeded.
- CSV format v2 adds a `category` column. v1 backups still import (their rows become Uncategorized); **v2 backups cannot be read by older app versions**, so export on 1.1.x if you ever need to go back.

## 1.1.3 — 30 October 2026

- The add/edit form is no longer a bottom sheet in its own dialog window. On some devices (reproduced on a Nothing Phone 3a running Nothing OS 4.1) that window was torn down and re-created while the keyboard was open, so the form vanished and re-appeared mid-typing. It is now a full-screen editor rendered inside the activity window, with a close button and Back handling.
- Save and "Save & add another" are pinned at the bottom, so they stay reachable with the keyboard open on any screen size.

## 1.1.2 — 30 October 2026

- Fixed the add-entry sheet sliding behind the on-screen keyboard while typing. The sheet used to size itself to its content, so with the IME open it fought the keyboard inset and alternated between sitting above the keyboard and collapsing to a sliver showing only "Add entry". It now has a fixed frame with an inner scroll area.
- The suggestion panel sizes to its matches again (one match is a short panel, many matches scroll), now that the sheet frame itself is stable.

## 1.1.1 — 29 October 2026

- Fixed the add-entry sheet growing and shrinking on every keystroke, which made it look like the "Add entry" sheet kept reopening and moved the fields and buttons under the finger. The suggestion list now has a fixed height and scrolls internally.
- Fixed a blank "Add entry" form appearing right after saving. The two save actions are separated by a divider, and a tap that lands in the instant after a save can no longer reopen the sheet.
- Added regression tests for both cases.

## 1.1.0 — 29 October 2026

- Added **Check for updates** in Settings.
- Update checks read the public GitHub Releases `releases/latest` endpoint and compare numeric semver tags.
- Update dialog shows version, release notes, and opens the GitHub release page; no in-app download/install.
- Added friendly handling for up-to-date, no releases, offline, rate-limited, server, and malformed-response cases.
- Added version comparison and GitHub release parsing tests.
- Added `AGENTS.md`, a machine-readable project knowledge graph, a query tool, and a completion matrix in the requirements.
- Added release instructions for publishing signed APKs as GitHub releases.
- Version bumped to 1.1.0 / versionCode 2.

## 1.0.0 — 29 October 2026

Initial release.

- Calendar home screen with per-day totals.
- Day detail sheet with item list, count, and total.
- Add/edit/delete entries, undo, and save-and-add-another.
- Day, Monday–Sunday week, and calendar-month totals.
- Item autocomplete with last price/date and a price-reuse chip.
- INR amounts with Indian digit grouping and integer-paise arithmetic.
- Settings for week start and theme.
- CSV export/import with `format_version`, stable UUIDs, duplicate skipping, and validation preview.
- Local-only storage, no internet permission, Android auto-backup disclosure.
- Unit, Room, CSV, and Compose/Robolectric tests.
- Signed release APK generation with a protected keystore outside the repo.
