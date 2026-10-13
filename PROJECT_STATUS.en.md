# AppStow – Project status

[Deutsch](PROJECT_STATUS.md) | **English**

As of October 13, 2026 – preparing the 3.0.2 release candidate.

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

## Remaining before 3.0.2 release

1. Update both READMEs, both changelogs, project status, bilingual release notes and F-Droid checklist.
2. Set `versionName=3.0.2`, stable `versionCode=15`; preserve `de.pritcloud.appstow` and the separate `.dev` package for test builds.
3. Build a **signed 3.0.1 → 3.0.2 upgrade candidate**; confirm certificate, version progression, and no data loss on a physical device.
4. Check F-Droid's existing recipe **through GitLab web UI**: stable-tag filter, auto-update mode, build dependencies, source version. Do not publish/update 3.0.2 before a stable tag exists.
5. Repeat release checks and deep review, then only with approval merge to `main`, create tag `v3.0.2`, publish APK/SHA-256 and follow the F-Droid update.

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
