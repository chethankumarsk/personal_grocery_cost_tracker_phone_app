# Changelog

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
