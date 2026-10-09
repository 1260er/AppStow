# AppStow

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
- Customize appearance, controls, and start behavior
- Create encrypted backups and restore the AppStow configuration
- German and English interface
- Light and dark system themes
- Android themed / monochrome launcher icon support

## Requirements

- Android 13 or newer
- minSdk 33
- targetSdk 36

## Installation

The current stable release is **AppStow 2.0.1**.

https://github.com/1260er/AppStow/releases/latest

Release files:

- `AppStow-2.0.1.apk`
- `AppStow-2.0.1.apk.sha256`

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

## Statistics and sorting (3.0.0, in development)

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

## Backup and restore (3.0.0, in development)

AppStow 3.0.0 will use encrypted backup format v3. Backups include:

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
recovery cannot be completed. This protects against interrupted restore
operations, but cannot guarantee recovery from storage hardware failures.

References to apps that are no longer installed are removed from favorites, category assignments, and app orders. Categories and custom shortcuts remain.

> **Important note about AppStow 3.0:** Version 3.0 fundamentally revises AppStow's backup format. Backups created with AppStow 2.x cannot be restored in AppStow 3.0. Likewise, backups created with AppStow 3.0 are not compatible with older AppStow versions.
>
> **Wichtiger Hinweis zu AppStow 3.0:** Mit Version 3.0 wird das Sicherungsformat von AppStow grundlegend überarbeitet. Sicherungen aus AppStow 2.x können in AppStow 3.0 nicht wiederhergestellt werden. Ebenso sind mit AppStow 3.0 erstellte Sicherungen nicht mit älteren AppStow-Versionen kompatibel.

## Upgrading from 2.0.1 to 3.0.0

AppStow 3.0.0 is intended to support an in-place Android update from
2.0.1 while preserving existing on-device configuration such as
categories, favorites, shortcuts, and manual ordering. This update
does not require importing a backup file.

Backup files created with AppStow 2.x cannot be imported into 3.0.0.
Likewise, backups created with 3.0.0 cannot be imported into 2.x.
The on-device APK upgrade test remains a release requirement.

## Privacy

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

## Releases

https://github.com/1260er/AppStow/releases

## License

AppStow is free software licensed under the GNU General Public License, version 3 or (at your option) any later version (SPDX: GPL-3.0-or-later). See [LICENSE](LICENSE).

Unicode emoji data retain their separate license and attribution noted above.
