# AppStow – Project status

[Deutsch](PROJECT_STATUS.de.md) | **English**

As of October 14, 2026 – AppStow 3.0.2 stable release (versionCode 15).

## Published stable version

- **Stable 3.0.2**, versionCode **15**, tag `v3.0.2`.
- The `main` branch contains the source for the tagged 3.0.2 stable release.
- GitHub's signed stable APK and SHA-256 file: https://github.com/1260er/AppStow/releases/tag/v3.0.2.
- Previous stable release: AppStow 3.0.1 (versionCode 14), tag `v3.0.1`.
- F-Droid has a separate update/build schedule; do not infer F-Droid 3.0.2 availability from the GitHub release.

## Tested development baseline

- `dev/backup-ui-fix`, commit `fefd4421861cfcfee7bb2b09fff16edcd4759203`, Dev-232.
- Three commits above 3.0.1 stable: startup view, Android system backup, backup-picker navigation fix.
- The two centered horizontal startup-view selector rows preserve the old Off behavior and add Last category / Favorites / Home.
- **Seedvault backup and restore** succeeded on a physical device with Dev-232.
- **Google Backup** is supported by the Android integration but has not yet been independently restore-tested.
- Strict system-backup allowlist excludes usage statistics, usage-derived sorting snapshots, WebView data, and internal restore journal. Cloud encryption capabilities are required.
- The independent manual **backup v3** format is unchanged and intentionally incompatible with 2.x backups.
- All four backup-UI tests passed on the device; unit tests, lint and builds passed, including new regression tests.

## Stable 3.0.2 – verified release checks

**Completed:**

1. Prepare versionName **3.0.2**, versionCode **15**, same application ID `de.pritcloud.appstow` and separate `.dev` test package.
2. Update German and English README, changelog, project status, and bilingual release notes, including the missing 3.0.1 changelog entry.
3. Signed GitHub Actions [upgrade run 38472809157](https://github.com/1260er/AppStow/actions/runs/38472809157) succeeded: same package ID and signing certificate, versionCode 14 → 15, unit tests, lint and optimized APK.
4. Install the signed 3.0.2 candidate **in place** over GitHub-stable 3.0.1 on a physical device, without uninstalling. Categories, favorites, custom shortcuts, ordering and settings remained intact.
5. On-device startup-view, backup-UI, and Seedvault backup/restore tests passed with Dev-232.

**After-release F-Droid follow-up:**

- The GitLab fdroiddata recipe was checked against the stable-only tag filter, version extraction, signing-key allowlist, public source links, license and donation link before the GitHub release.
- F-Droid must independently detect/build/publish version 3.0.2 (15). Its build status and APK signature must be checked after publishing; no immediate F-Droid availability is implied.
- Google Backup restore has **not** been separately tested on a physical device.

## Security and compatibility

- Treat published stable tags and signed release artifacts as immutable.
- Manual backup v3 uses AES-256-GCM but a shared, non-secret derived key. This is not strong confidentiality against determined third parties.
- Android system backup excludes local usage data, while the user's selected provider controls transfer and storage.
- F-Droid builds its own APK from the tagged source; signing keys may differ from GitHub-signed APKs. Do not assume cross-source upgrade compatibility.

## Documentation

- [German README](README.de.md) | [English README](README.md)
- [German changelog](CHANGELOG.de.md) | [English changelog](CHANGELOG.md)
- [Bilingual 3.0.2 release notes](RELEASE_NOTES_3.0.2.md)
- [F-Droid release checklist](FDROID_RELEASE_CHECKLIST.md)
- [Historical detailed status (German)](PROJECT_STATUS.de.md#archivierte-projektnotizen-bis-version-300)
