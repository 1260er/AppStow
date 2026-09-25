# AppStow

AppStow is an Android launcher and organizer for installed apps, favorites, categories, custom shortcuts, and web apps.

## Features

- Organize installed apps in custom categories
- Mark apps and shortcuts as favorites
- Assign emoji symbols to categories
- Offline emoji picker with German and English search
- Create website, web app, and deep-link shortcuts
- Reorder overview sections and their contents
- Backup and restore the AppStow configuration
- German and English interface
- Light and dark system themes
- Android themed / monochrome launcher icon support

## Requirements

- Android 13 or newer
- minSdk 33
- targetSdk 36

## Installation

The current stable release is **AppStow 0.1.0**.

https://github.com/1260er/AppStow/releases/latest

Release files:

- `AppStow-0.1.0.apk`
- `AppStow-0.1.0.apk.sha256`

Obtainium repository:

```text
https://github.com/1260er/AppStow
```

## Categories

Categories can be created, renamed, deleted, and assigned an emoji symbol.
Apps can belong to one or multiple categories.

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

## Backup and restore

Backups include categories, symbols, assignments, favorites, shortcuts, overview order, item order, and related settings.

Web-app sessions, cookies, Android permissions, and other application data are not included.

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
