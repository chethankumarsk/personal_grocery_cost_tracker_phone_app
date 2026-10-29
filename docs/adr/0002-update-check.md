# ADR 0002: Update checks via GitHub Releases

- **Status:** Accepted
- **Date:** 29 October 2026

## Context

The app is sideloaded from a public GitHub repository. Users need a way to discover newer versions without a store. The app is otherwise offline and private, so any network feature should be minimal and explicit.

Options considered:

1. **GitHub Releases API + release page** — publish a signed APK and release notes as a GitHub release; the app reads `releases/latest` and opens the release page in the browser.
2. **Raw Git tags / `latest.json` in the repo** — fewer GitHub API limits, but no standard release page, no release notes, and no guarantee that a matching APK asset exists.
3. **In-app APK download/install** — better UX, but requires `REQUEST_INSTALL_PACKAGES`, file-provider/install flow, and more security surface.
4. **Play Store / F-Droid** — distribution infrastructure; not appropriate for a personal sideloaded app yet.

## Decision

Use **GitHub Releases** as the update source.

- `GET https://api.github.com/repos/chethankumarsk/personal_grocery_cost_tracker_phone_app/releases/latest`
- Compare the release tag (e.g. `v1.1.0`) with `BuildConfig.VERSION_NAME` using numeric semver comparison.
- Show a dialog with the version, release notes, and a button to the release page.
- Do **not** download or install the APK in-app. The browser/download flow keeps the app free of install-package permissions.
- The check is **manual only** in v1 (Settings → Check for updates). No automatic/background polling.
- The only permission added is `INTERNET`; no analytics or app server is involved.
- Publishing a release must use a tag matching the app version, a signed APK asset, and the same `applicationId`/signing key.

## Consequences

- Users can discover updates without a store, and the app can show release notes.
- The app now makes one optional outbound request; About and requirements disclose this. Auto backup remains a separate OS-level off-device copy.
- GitHub rate limits (60 unauthenticated requests/hour) are irrelevant for manual checks.
- Until the first GitHub release is published, the UI reports “No releases yet”.
- Automatic daily checks and in-app download/install remain future options; they require a separate decision about privacy and install permissions.
