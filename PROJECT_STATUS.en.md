# AppStow – Project status

[Deutsch](PROJECT_STATUS.md) | **English**

As of October 14, 2026 – 3.0.2 release candidate; signed in-place upgrade test passed.

## Published stable version

- **Stable 3.0.1**, versionCode **14**, tag `v3.0.1`.
- GitHub `main` remains at `c8620dc5f454ecc494773d5d8f58f76a7dea1b43` until final approval.
- The signed stable APK and SHA-256 file are available at https://github.com/1260er/AppStow/releases/tag/v3.0.1.
- **3.0.2 is not a stable release yet**. VersionCode **15** is prepared for the candidate; do not create `v3.0.2` before approval.

## Tested development baseline

- `dev/backup-ui-fix`, commit `fefd4421861cfcfee7bb2b09fff16edcd4759203`, Dev-232.
- Three commits above 3.0.1 stable: startup view, Android system backup, backup-picker navigation fix.
- The two centered horizontal startup-view selector rows preserve the old Off behavior and add Last category / Favorites / Home.
- **Seedvault backup and restore** succeeded on a physical device with Dev-232.
- **Google Backup** is supported by the Android integration but has not yet been independently restore-tested.
- Strict system-backup allowlist excludes usage statistics, usage-derived sorting snapshots, WebView data, and internal restore journal. Cloud encryption capabilities are required.
- The independent manual **backup v3** format is unchanged and intentionally incompatible with 2.x backups.
- All four backup-UI tests passed on the device; unit tests, lint and builds passed, including new regression tests.

## Release candidate: completed and outstanding checks

**Completed:**

1. Prepare versionName **3.0.2**, versionCode **15**, same application ID `de.pritcloud.appstow` and separate `.dev` test package.
2. Update German and English README, changelog, project status, and bilingual release notes, including the missing 3.0.1 changelog entry.
3. Signed GitHub Actions [upgrade run 38472809157](https://github.com/1260er/AppStow/actions/runs/38472809157) succeeded: same package ID and signing certificate, versionCode 14 → 15, unit tests, lint and optimized APK.
4. Install the signed 3.0.2 candidate **in place** over GitHub-stable 3.0.1 on a physical device, without uninstalling. Categories, favorites, custom shortcuts, ordering and settings remained intact.
5. On-device startup-view, backup-UI, and Seedvault backup/restore tests passed with Dev-232.

**Still outstanding:**

1. Confirm the live F-Droid recipe **through the GitLab web UI**: stable-tag restriction, update mode, dependencies, licenses and donation link; do not publish 3.0.2 prematurely.
2. Complete the final repository/documentation review and release checks. Only after explicit approval merge to `main`, tag `v3.0.2`, publish signed APK/SHA-256 and monitor F-Droid.
3. Google Backup restore has **not** been separately tested on a physical device; keep that limitation clear.

## Security and compatibility

- Do not change any published stable artifacts or create a premature release tag.
- Manual backup v3 uses AES-256-GCM but a shared, non-secret derived key. This is not strong confidentiality against determined third parties.
- Android system backup excludes local usage data, while the user's selected provider controls transfer and storage.
- F-Droid builds its own APK from the tagged source; signing keys may differ from GitHub-signed APKs. Do not assume cross-source upgrade compatibility.

## Documentation

- [German README](README.de.md) | [English README](README.md)
- [German changelog](CHANGELOG.md) | [English changelog](CHANGELOG.en.md)
- [Bilingual 3.0.2 release notes](RELEASE_NOTES_3.0.2.md)
- [F-Droid release checklist](FDROID_RELEASE_CHECKLIST.md)
- [Historical detailed status (German)](PROJECT_STATUS.md#archivierte-projektnotizen-bis-version-300)
