# ADR 0003: In-app APK download and install

- **Status:** Accepted
- **Date:** 30 October 2026
- **Supersedes in part:** ADR 0002 (update checks via GitHub Releases)

## Context

ADR 0002 chose a browser handoff: the app checked GitHub Releases, showed release notes, and opened the release page; the user downloaded and installed the APK themselves. That kept the app free of install permissions, but every update required leaving the app, finding the right asset, and handing the file to the browser's download flow.

The app remains a sideloaded, single-user, offline-first personal app. There is no store to deliver updates, so the update path is a core usability feature. The trade-offs are permission scope and the security of installing a downloaded file.

## Decision

Keep GitHub Releases as the only update source and support **both** paths:

1. **Download and install** (new, primary): stream the `.apk` asset from GitHub, verify it, and open Android's package installer.
2. **Open release page** (existing, fallback): open the GitHub release in the browser for a manual download and install.

The in-app path is deliberately narrow:

- **User initiated only.** No background check and no background download.
- **Cache-only file.** The APK is written under `cache/updates/` through a `.part` file and promoted only after checks pass. It is exposed to the installer through a `FileProvider` limited to that directory.
- **Size cap.** Downloads larger than 100 MB are refused before and during transfer.
- **Integrity.** The download must match GitHub's asset `size` and, when GitHub supplies it, the `sha256:` digest. GitHub computes the digest for release assets; older releases without one still go through the signature check.
- **Authenticity.** `getPackageArchiveInfo` must report the same `applicationId`, a higher `versionCode`, and a signing certificate matching the installed app. A mismatch deletes the file and is never installed.
- **No silent install.** Android shows its own confirmation; the first time, the user must enable "install unknown apps" for Grocery Ledger. The app opens that setting when needed and retries on return.
- **Permissions.** `REQUEST_INSTALL_PACKAGES` is added solely for the installer handoff. `INTERNET` already existed for the check; no other permission is added.
- **Failure handling.** Interrupted, oversized, checksum-mismatched, wrong-package, older-version, or wrong-signer files are discarded with a friendly message, and the release page remains available.

## Consequences

- Updates are one tap (plus Android's confirmation) and no longer require a browser round-trip.
- The app can install code, so its signing key becomes even more important. The signature check means a compromised repository cannot ship a malicious APK unless the release key itself is compromised. The keystore and its credentials remain outside the repository.
- Android may warn about installing unknown apps, and Play Protect may scan the APK on first install. This is expected for sideloaded distribution.
- Debug builds cannot install published release APKs (different signing key), and release builds reject debug/unsigned files. That is intentional.
- The checksum relies on GitHub's asset digest. Release publishing must attach the signed APK as an asset; an asset replaced after publication would also change the digest and be rejected.
- Automatic update checks and unattended installation remain out of scope.
