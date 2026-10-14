# AppStow 3.0.2 – Release notes / Versionshinweise

## Deutsch

**Neu in 3.0.2**

- **Startansicht:** Aus, Bei Start oder Immer; dazu Letzte Kategorie, Favoriten oder Startseite. Beide Reihen sind mittig und horizontal angeordnet. „Aus“ bewahrt das bisherige Verhalten.
- **Android-Systembackup:** AppStow lässt Sicherungen über Seedvault, Google-Backup und kompatible Android-Dienste zu. Es werden nur ausgewählte Konfigurationsdaten einbezogen; Nutzungsstatistiken und WebView-Daten bleiben ausgeschlossen. Cloud-Sicherungen setzen Android-Verschlüsselungsfähigkeiten voraus.
- **Backup-UI:** Nach dem Android-Dateidialog bleibt die Backup-Seite sichtbar, auch mit „Startansicht: Immer“; Abbruch und Activity-Neuerstellung wurden berücksichtigt.
- **Manuelles Backupformat v3:** unverändert und kompatibel mit bestehenden 3.x-Sicherungen; nicht kompatibel mit AppStow 2.x-Sicherungen.
- Zusätzliche Regressionstests und Praxistests: Seedvault-Sicherung/Wiederherstellung und Backup-UI bestanden. Google-Wiederherstellung wurde noch nicht separat getestet.

**Update:** Das direkte Update von der auf GitHub signierten AppStow-Version 3.0.1 (Versionscode 14) auf 3.0.2 (Versionscode 15) mit unveränderter Paketkennung wurde erfolgreich auf einem Smartphone getestet. Kategorien, Favoriten, Verknüpfungen, Reihenfolgen und Einstellungen blieben erhalten. Der signierte CI-Upgrade-Test war ebenfalls erfolgreich. Bei F-Droid-Installationen kann das Signaturzertifikat abweichen.

## English

**What's new in 3.0.2**

- **Start view:** Off, On start, or Always; plus Last category, Favorites, or Home. Both radio rows are horizontal and centered; Off keeps the previous behavior.
- **Android system backup:** AppStow can participate in Seedvault, Google Backup and compatible Android backup services. Only selected configuration files are included; usage statistics and WebView data are excluded. Cloud backup requires Android encryption capabilities.
- **Backup UI:** Returning from Android's file picker now preserves the backup page even with Start view set to Always; cancellation and activity recreation are handled.
- **Manual backup format v3:** unchanged and compatible with existing 3.x backups; 2.x backup files are not compatible.
- More regression and on-device tests: Seedvault backup/restore and backup UI passed. Google restore has not yet been tested separately.

**Upgrade:** The in-place update from GitHub-signed AppStow 3.0.1 (versionCode 14) to 3.0.2 (versionCode 15), with the same package ID, passed on a physical device. Categories, favorites, shortcuts, ordering and settings were preserved. The signed CI upgrade check also passed. F-Droid installations may use different signing credentials.

**Sources and changelogs:** [Deutsch](CHANGELOG.md) | [English](CHANGELOG.en.md)
