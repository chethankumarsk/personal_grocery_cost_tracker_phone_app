# Releasing an update

The app checks **GitHub Releases** for updates. A release is discoverable only after it is published as a normal (non-draft, non-prerelease) GitHub release with a signed APK asset.

## 1. Bump the version

Edit `app/build.gradle.kts`:

```kotlin
versionCode = 8          // must increase for every release
versionName = "1.3.1"    // semver; tag it as v1.3.1
```

Also update:

- `CHANGELOG.md`
- `README.md` if behavior or install steps changed
- `REQUIREMENTS.md` status matrix
- `docs/KNOWLEDGE_GRAPH.md` and `docs/knowledge-graph.json`

## 2. Build and verify

```powershell
$env:JAVA_HOME = "C:\Users\Administrator\toolchains\jdk-21.0.12.1+1"
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleRelease
```

Verify the signature:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\apksigner.bat" verify `
  --verbose app\build\outputs\apk\release\app-release.apk
```

Copy/rename the artifact:

```powershell
Copy-Item app\build\outputs\apk\release\app-release.apk `
  dist\GroceryLedger-1.3.1-release.apk
```

Generate the checksum file published beside the APK:

```powershell
$hash = (Get-FileHash dist\GroceryLedger-1.3.1-release.apk -Algorithm SHA256).Hash.ToLower()
"$hash  GroceryLedger-1.3.1-release.apk" |
  Set-Content -Encoding ascii dist\GroceryLedger-1.3.1-release.apk.sha256
```

Do **not** change `applicationId` or the signing key. The APK must be installable over the previous release.

## 3. Publish on GitHub

Use the GitHub web UI:

1. Open <https://github.com/chethankumarsk/personal_grocery_cost_tracker_phone_app/releases/new>.
2. Choose a new tag: `v1.3.1` (must start with `v`; semver after that).
3. Set the release title, e.g. `Grocery Ledger 1.3.1`.
4. Add release notes. These are shown in the app's update dialog.
5. Attach the signed APK and its `.sha256` checksum file as release assets.
6. Leave **Set as the latest release** selected.
7. Do not mark it as a pre-release if it is the update you want all users to receive.
8. Publish.

If you have the GitHub CLI and Git installed, the equivalent is:

```powershell
git tag v1.3.1
git push origin v1.3.1
gh release create v1.3.1 .\dist\GroceryLedger-1.3.1-release.apk `
  .\dist\GroceryLedger-1.3.1-release.apk.sha256 `
  --title "Grocery Ledger 1.3.1" `
  --notes "Release notes here"
```

Git is not currently installed in this environment; the web UI is the reliable path.

## 4. Verify the app sees it

The app calls:

```text
https://api.github.com/repos/chethankumarsk/personal_grocery_cost_tracker_phone_app/releases/latest
```

You can verify from a terminal:

```powershell
curl.exe -s -H "Accept: application/vnd.github+json" `
  -H "User-Agent: GroceryLedger/1.0.1" `
  "https://api.github.com/repos/chethankumarsk/personal_grocery_cost_tracker_phone_app/releases/latest"
```

Then in the app: **Settings → Check for updates**. The update dialog should show the new version and notes.

- **Download and install** downloads the `.apk` asset, checks GitHub's `size` and `sha256:` digest, verifies the package name, `versionCode`, and signing certificate, then opens Android's installer. The first time, allow installs from Grocery Ledger when prompted.
- **Open release page** is the browser fallback and works even if the asset is missing.

The tag must be numerically newer than the installed `versionName`. `v1.3.1` is newer than `1.3.0`. Do not rely on `versionCode` alone because GitHub does not know the APK's versionCode. The APK itself must have a higher `versionCode` than the installed build, or the in-app verifier refuses it.

GitHub computes the asset digest automatically. Confirm the response includes a `digest` beginning with `sha256:`; without it, the app still verifies the signing key but cannot check the download checksum. The digest must match the local `.sha256` file.

The APK is sideloaded, so Play Protect may warn about it. The README explains the verification steps; publish the `.sha256` file so users can confirm a download.

## 5. Install the update

Either use **Download and install** in the app, or download the APK from the release page and install it over the existing app. Android will accept it because:

- package is still `dev.groceryledger`;
- signing key is unchanged;
- `versionCode` is higher.

Existing data remains in the Room database.

To test the in-app path end to end, install 1.3.1 manually once (older builds predate the button), publish a newer release such as 1.3.2, then check for updates and use **Download and install**. The app must reject a debug-signed or differently signed APK, which is expected.

## Notes

- Only the latest normal release is returned. Drafts and pre-releases are ignored by `/releases/latest`.
- GitHub's unauthenticated API limit is 60 requests per hour per IP; manual checks are far below that.
- The app downloads only after the user taps **Download and install**; there is no background check or download.
- `REQUEST_INSTALL_PACKAGES` is used solely to open Android's installer for the verified APK. The release page remains the fallback.
- Automatic update checks are deferred; the current privacy model is “network only when the user checks, downloads, or opens a release”.
