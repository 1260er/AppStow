# Changelog

[Deutsch](CHANGELOG.md) | **English**

## 3.0.2 – Release candidate (not yet published)

- New **Start view** setting with two centered horizontal radio rows: **Off / On start / Always** and **Last category / Favorites / Home**.
- **Off** preserves the previous startup behavior and disables the second row. Existing startup settings migrate without changing user preferences.
- Reopen the last visited category on launch, supporting list/grid layouts and safe fallback for deleted categories.
- Android system backup prepared for Seedvault, Google Backup, and compatible providers, using a strict allowlist of user-configuration files.
- Exclude usage statistics, automatic sorting snapshots, WebView sessions and the internal recovery journal; require Android encryption capabilities for cloud backup.
- Restore sorting configuration without usage history, with a safe fallback to manual sorting when needed.
- Keep AppStow's separate manual backup and restore format **v3** compatible.
- Returning from Android's backup file picker no longer triggers unwanted **Always** startup navigation; cancellation and activity recreation are also handled.
- Add regression tests for start-view selection, system-backup policy, and file-picker navigation.
- Seedvault backup and restore succeeded on a device. Google Backup is prepared, but has not yet been tested separately on a device.
- Stable versionCode 15; the signed CI upgrade check from 3.0.1 (14) to 3.0.2 (15) passed. A direct in-place upgrade on a physical device was also successful without uninstalling; categories, favorites, shortcuts, ordering and settings were preserved. Stable release approval remains pending.

## 3.0.1 – October 12, 2026

- Introduce interval-based sorting snapshots for semi-/fully automatic sorting and optional usage-based suggestions.
- Improve sorting and favorite behavior, with additional tests for manual ordering.
- Improve backup writing and verification through Android document providers.
- Additional UI refinements and regression tests; increase versionCode to 14.

## 3.0.0 – October 9, 2026

- New local usage statistics for apps and shortcuts launched from AppStow, including periods, most-used and unused apps, and category usage.
- Manual, semi-automatic, and fully automatic sorting, optional suggestions and day/evening profiles.
- Appearance, handedness, startup behavior, and theme settings.
- New encrypted v3 configuration backup, including visible automatic favorites and their app/shortcut order.
- Restores reset usage statistics and use an atomic private journal for interrupted restore recovery.
- Remove references to missing apps on manual restore while retaining categories and user-created shortcuts.
- Backup format v3 is intentionally incompatible with AppStow 2.x backups.
- Add regression tests, Android baseline profiles and a successful on-device upgrade test from 2.0.1.
- Improve WebView blob download limits and message-queue handling.

## 2.0.1 – October 4, 2026

- Add German/English in-app language selection and F-Droid publishing preparations.
- Increase the version code to support future updates.

## 2.0.0 – October 2, 2026

- List/grid views for launch overview, configurable grid columns, and matching assignment views.
- App-category assignment by tap, drag handles when sorting, and unified favorite controls.
- Search, filtering and sorting within sections; additional backup validation and bilingual help.
- Expanded app information and a Ko-fi link supporting the projects.

## 0.1.3 – October 1, 2026

- Extended HTTPS web apps, file/photo/camera upload, zoom, fullscreen, web permissions and downloads.
- Improved handling of Blob/Papra PDFs, cookies on redirects, filename collisions and cleanup.

## 0.1.2 – Stable release

- Improvements to backup and restore, with further stability fixes.
