# AppStow – Project status

[Deutsch](PROJECT_STATUS.de.md) | **English**

As of October 15, 2026 – AppStow 3.0.3 stable release (versionCode 16).

## Current stable release

- Version: 3.0.3, versionCode 16, tag `v3.0.3`.
- Application ID: `de.pritcloud.appstow`; development builds use a separate `.dev` package.
- Previous stable version: 3.0.2, versionCode 15.
- Stable release: https://github.com/1260er/AppStow/releases/tag/v3.0.3
- Signed release APK and SHA-256 checksum are published through GitHub Actions.

## Quality and compatibility

- Dev-241 was successfully tested on a physical Android device.
- Full debug/release unit tests, lint, optimized builds and final audit passed.
- Performance improvements cover icon loading, sorting persistence and overview refreshes.
- The normal Android Recents preview is preserved.
- The manual v3 backup format remains unchanged and compatible with AppStow 3.x.
- Google Backup restore has not been separately tested on a physical device.
- An in-place upgrade of the GitHub-signed 3.0.2 stable APK to 3.0.3 has not been separately reported as a physical-device test.
- F-Droid detection, building and publishing are independent of the GitHub stable release.
- F-Droid update status must be verified independently after the release.

## Security and compatibility

- Treat published stable tags and signed release artifacts as immutable.
- Manual backup v3 uses AES-256-GCM but a shared, non-secret derived key. This is not strong confidentiality against determined third parties.
- Android system backup excludes local usage data, while the user's selected provider controls transfer and storage.
- F-Droid builds its own APK from the tagged source; signing keys may differ from GitHub-signed APKs. Do not assume cross-source upgrade compatibility.

## Documentation

- [German README](README.de.md) | [English README](README.md)
- [German changelog](CHANGELOG.de.md) | [English changelog](CHANGELOG.md)
- [Bilingual 3.0.3 release notes](RELEASE_NOTES_3.0.3.md)
- [F-Droid release checklist](FDROID_RELEASE_CHECKLIST.md)
- [Historical detailed status (German)](PROJECT_STATUS.de.md#archivierte-projektnotizen-bis-version-300)
