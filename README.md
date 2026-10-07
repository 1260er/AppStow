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

## Backup and restore

AppStow 3.0 uses encrypted backup format v3. Backups include:

- categories, symbols, assignments, and favorites
- custom shortcuts
- manual section and item orders
- list/grid and column settings
- sorting settings, suggestions, and the optional time profile
- appearance and control settings, including the AppStow language
- statistics display settings

Usage statistics themselves, web-app sessions, and cookies are not included.

A successful restore resets usage statistics. References to apps that are no longer installed are removed from favorites, category assignments, and app orders. Categories and custom shortcuts remain.

> **Important note about AppStow 3.0:** Version 3.0 fundamentally revises AppStow's backup format. Backups created with AppStow 2.x cannot be restored in AppStow 3.0. Likewise, backups created with AppStow 3.0 are not compatible with older AppStow versions.
>
> **Wichtiger Hinweis zu AppStow 3.0:** Mit Version 3.0 wird das Sicherungsformat von AppStow grundlegend überarbeitet. Sicherungen aus AppStow 2.x können in AppStow 3.0 nicht wiederhergestellt werden. Ebenso sind mit AppStow 3.0 erstellte Sicherungen nicht mit älteren AppStow-Versionen kompatibel.

## Privacy

AppStow does not require an account.
Network access is used for configured websites and web apps.

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
