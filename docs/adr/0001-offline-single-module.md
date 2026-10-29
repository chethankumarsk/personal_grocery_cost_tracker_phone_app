# ADR 0001: Offline, single-module architecture

- **Status:** Accepted
- **Date:** 29 October 2026

## Context

Grocery Ledger is a personal, single-user Android app with a small feature set: a calendar, item entries, totals, autocomplete, and CSV backup. It must stay easy to modify and must never lose data during an update. A large modular architecture or a DI framework would add build complexity without a current need.

## Decision

- Keep one Gradle `:app` module.
- Separate packages by responsibility (`data`, `domain`, `ui`) and depend on repository interfaces, not implementations.
- Use manual dependency injection through `AppContainer`.
- Keep business rules (money, dates, ranking) as pure Kotlin with unit tests.
- Use Room with exported schemas and additive migrations; no destructive migrations.
- Use a versioned, UID-based CSV format for export/import.
- Keep `applicationId` (`dev.groceryledger`) and the release signing key stable; increment `versionCode` for every release.

## Consequences

- Fast builds and a small project surface.
- Future features can be added inside the existing layers; extracting a module is possible because packages are already separated.
- Repository and CSV abstractions add a small amount of code but make tests and storage changes straightforward.
- The release keystore must be backed up outside the repository; losing it prevents in-place updates.
