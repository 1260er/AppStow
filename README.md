# AppStow

**English** | [Deutsch](README.de.md)

AppStow is an Android launcher and organizer for installed apps, favorites, categories, custom shortcuts, and web apps.

## Features

- Organize installed apps in custom categories
- Mark apps and shortcuts as favorites
- Assign emoji symbols to categories
- Offline emoji picker with German and English search
- Create website, web app, and deep-link shortcuts
- Switch between list and grid views
- Choose 3, 4 or 5 global grid columns
- Set individual column counts for opened sections
- Use the global view when assigning apps
- Assign categories by tapping an app
- Reorder overview sections and their contents
- Replace favorite stars with drag handles while sorting
- View local usage statistics for launches started through AppStow
- Use manual, semi-automatic, or fully automatic sorting
- Configure sorting suggestions and optional day/evening profiles
- Customize appearance, controls, and the startup view (off, on start, or always)
- Create encrypted manual v3 backups and restore the AppStow configuration
- Use Android system backup through Seedvault or compatible providers such as Google Backup
- German and English interface
- Light and dark system themes
- Android themed / monochrome launcher icon support

## Requirements

- Android 13 or newer
- minSdk 33
- targetSdk 36

## Installation

**Current stable release: AppStow 3.0.3 (versionCode 16).**

The latest **published stable** release and its SHA-256 checksum are available at:

https://github.com/1260er/AppStow/releases/latest

Release assets use the naming pattern `AppStow-X.Y.Z.apk` and `AppStow-X.Y.Z.apk.sha256`. Development builds are separate pre-releases and are not stable updates.

Obtainium repository:

```text
https://github.com/1260er/AppStow
```

## Categories

Categories can be created, renamed, deleted, and assigned an emoji symbol.
Apps can belong to one or multiple categories.

In Assign apps, tap an app to choose its categories.
This page follows the global list or grid view.
The favorite star works independently of category assignments.

## Custom shortcuts

AppStow supports websites, HTTPS web apps, and deep links.

Examples:

```text
https://www.google.com/maps/dir/?api=1&destination=Berlin
whatsapp://send?phone=+491701234567&text=Hallo
tel:+491701234567
intent:#Intent;action=android.media.action.IMAGE_CAPTURE;end
package:com.example.app
```

## Integrated web apps

HTTPS web apps run in a separate Android WebView process.

Supported features include file and photo uploads, camera
capture, zoom, fullscreen, location and microphone requests,
HTTPS downloads and compatible blob downloads.

External links from integrated web apps are only handed to Android
when the navigation comes from a user action in the main frame.
Unsafe internal URL schemes are blocked. Explicit targets and
arbitrary extras in web-originated intent URIs are not forwarded.
Custom deep-link shortcuts created in AppStow are unaffected.

Web permissions are restricted to the configured HTTPS origin.
Authenticated HTTPS downloads do not forward session cookies
or the original Referer to a different origin.

Downloads are saved to the Android Downloads collection.
The file size limit is 512 MiB. Ordinary HTTPS downloads
stop when the web app activity is destroyed and do not
provide a persistent system download notification.

Web app functionality depends on the installed Android
WebView and the website. WebAuthn may require correctly
configured Digital Asset Links.

Integrated web apps require a validated network connection.
Local-only networks without validated internet access
may therefore be rejected.

## Statistics and sorting (3.0.0)

AppStow records only launches started through AppStow. Usage statistics
remain on the device, are retained for at most one year, and can be
reset independently of the display settings.

Sorting supports manual, semi-automatic, and fully automatic modes.

Semi-automatic mode has four independent controls:
- **Favorites:** sort all apps and custom shortcuts in Favorites.
- **Categories:** sort the category sections themselves.
- **Category contents:** sort all apps and custom shortcuts inside each category.
- **Custom shortcuts:** sort the separate Custom shortcuts section.

An enabled content area is sorted as a whole, regardless of entry type,
and cannot be rearranged manually. Disabled content areas remain
manually sortable. Semi-automatic sorting saves the resulting orders
as the new baseline; returning to Manual does not restore older orders.

Fully automatic mode sorts sections and contents and selects favorites
based on usage. Leaving fully automatic mode saves the current
automatic ordering and favorite selection as the new manual state.

In Manual mode, optional usage-based suggestions can be reviewed and
applied individually. Reassign favorites changes favorite membership;
Sort favorites changes their order. Category order, Category contents
and Custom shortcuts can also be applied independently.

Optional day/evening profiles are available.
Time profiles help automatic sorting reflect different usage patterns
during the day and evening. Everyday apps can appear higher during
the day, while games and leisure apps may move up in the evening.
Users choose when each period starts. Manual mode remains the default.

## Start view (3.0.2)

In **Appearance & controls**, select when AppStow opens the chosen start view: **Off**, **On start**, or **Always**. The second, centered horizontal row chooses **Last category**, **Favorites**, or **Home**.

**Off** starts on Home after a complete restart and leaves the currently opened page unchanged when returning from the background. The target row is disabled but its selection is retained. **On start** opens the chosen view on a new launch only. **Always** also applies it when returning to AppStow from another app. Returning from AppStow's own Android backup file picker does not trigger this navigation.

**Last category** recalls a category opened within the launch overview, in list or grid mode. If it no longer exists, AppStow falls back to Home. Settings, statistics and backup screens are not treated as categories.

## Improvements in 3.0.3

- More efficient foreground return for the configured Home, Favorites or Last category start view.
- Configurable visibility in Android Recents, independent of start-view behavior.
- Reduced redundant sorting writes and intermediate overview refreshes.
- Shared asynchronous icon-loading requests and protection against stale icon results.
- Additional regression tests and successful on-device testing of Dev-241.
- The normal Android Recents preview remains available. Depending on Android, a brief transition to the configured start view may still be visible.

## Backup and restore

AppStow 3.x uses encrypted manual backup format v3. Backups include:

- categories, symbols, assignments, and favorites
- custom shortcuts
- manual section and item orders
- list/grid and column settings
- sorting settings, suggestions, and the optional time profile
- appearance and control settings, including the AppStow language
- statistics display settings

Usage statistics themselves, web-app sessions, and cookies are not included.

Backups created in fully automatic mode capture the currently visible
automatic favorites, including their combined app/shortcut order.
After restore, these favorites remain available even though usage
statistics are reset. New usage data can subsequently change the
automatic selection. Switching to Manual before that retains the
restored favorite selection and order.

**Backup security:** Backup v3 uses AES-256-GCM with a random salt and IV. No user password is required. The key is derived from a fixed value shared across installations. The encryption prevents casual reading and detects accidental corruption, but it does not provide strong confidentiality or tamper protection against determined third parties. Keep backup files in a trusted, private location.

A successful restore resets usage statistics. During restore, AppStow writes
an atomic recovery snapshot in its private app storage. If the Android process
stops during restoration, AppStow attempts to restore the previous preference
state before loading its UI on the next launch. It refuses to load the UI if
recovery cannot be completed. Restore writes and startup journal recovery
share a process-wide lock, including during activity recreation after a language
change. This protects against interrupted restore operations, but cannot
guarantee recovery from storage hardware failures.

References to apps that are no longer installed are removed from favorites, category assignments, and app orders. Categories and custom shortcuts remain.

> **Important note about AppStow 3.0:** Version 3.0 fundamentally revises AppStow's backup format. Backups created with AppStow 2.x cannot be restored in AppStow 3.0. Likewise, backups created with AppStow 3.0 are not compatible with older AppStow versions.

## Android system backup (3.0.2)

AppStow also supports Android's system backup framework. Users choose and manage the backup service in Android settings; AppStow does not upload backups by itself. It is intended for **Seedvault**, **Google Backup** and other compatible Android backup services. Actual availability depends on the device, OS and provider.

Only these configuration stores are allowed: categories, favorites, custom shortcuts, overview/section ordering, overview display, UI preferences, statistics **display settings**, and a sanitized copy of sorting configuration. This allows restoring user organization and preferences without exporting usage-derived snapshots.

**Not included:** recorded usage statistics, automatic sorting snapshots, web-app data, cookies, databases, transient backup state, or the private restore journal. Cloud backup is allowed only if Android reports the required encryption capabilities. Device-to-device transfers use the same file allowlist.

After an Android system restore, AppStow rehydrates sorting preferences without importing usage history; fully automatic sorting may safely fall back to Manual until new local history is available. Unlike the manual v3 restore, the system backup is controlled by Android and is not imported from within AppStow.

Seedvault backup **and restore** were tested successfully on a device. Google Backup is supported by the same Android integration but has **not** yet been separately restore-tested; no Google-specific account, SDK or dependency is required.

## Tested upgrade from 3.0.1 to 3.0.2

The signed AppStow 3.0.2 (versionCode 15) release candidate was installed **in place** over the published GitHub-signed AppStow 3.0.1 (versionCode 14) on a physical device. The update succeeded without uninstalling the app. The existing categories, favorites, custom shortcuts, ordering, and settings were checked and preserved. The GitHub Actions signing and version-comparison run passed: [3.0.2 upgrade test](https://github.com/1260er/AppStow/actions/runs/38472809157).

The device test used the signed 3.0.2 upgrade candidate built from the same application source as the stable release. GitHub and F-Droid installations may have different signing certificates; cross-source upgrades must not be assumed compatible.

## Upgrading from 2.0.1 to 3.0.0

AppStow 3.0.0 supports an in-place Android update from
2.0.1 while preserving existing on-device configuration such as
categories, favorites, shortcuts, and manual ordering. This update
does not require importing a backup file.

Backup files created with AppStow 2.x cannot be imported into 3.0.0.
Likewise, backups created with 3.0.0 cannot be imported into 2.x.
The in-place APK upgrade from 2.0.1 to 3.0.0 was successfully tested on a physical Android device.

## Privacy

Android system backup sends only allowlisted configuration through the user's selected Android provider; please consult that provider's privacy terms. Usage statistics are excluded from AppStow's manual backups and Android system backups.

AppStow does not require an account.
Network access is used for configured websites and web apps.
Usage statistics remain local and are not uploaded by AppStow.

## Development

- Java 17
- Gradle 8.11.1
- Android Gradle Plugin 8.10.1
- compileSdk 36
- targetSdk 36
- minSdk 33

## Unicode emoji data

The offline emoji search index is generated from Unicode Emoji and CLDR data.

License and attribution files:

- `app/src/main/assets/emoji_search_LICENSE.txt`
- `app/src/main/assets/emoji_search_NOTICE.txt`

## Releases and changes

- [English changelog](CHANGELOG.md) | [Deutsches Changelog](CHANGELOG.de.md)
- [AppStow 3.0.3 release notes (German and English)](RELEASE_NOTES_3.0.3.md)
- [Current project status](PROJECT_STATUS.md)

https://github.com/1260er/AppStow/releases

## Support

If you would like to support the work on AppStow and other projects, visit **[Ko-fi – 1260er](https://ko-fi.com/1260er)**.

## License

AppStow is free software licensed under the GNU General Public License, version 3 or (at your option) any later version (SPDX: GPL-3.0-or-later). See [LICENSE](LICENSE).

Unicode emoji data retain their separate license and attribution noted above.
