# AppStow 3.0.3 – F-Droid release checklist / Prüfliste

This checklist is not proof of publication on F-Droid.
Diese Prüfliste ist kein Nachweis für eine Veröffentlichung auf F-Droid.

## Deutsch

- [x] Paketkennung `de.pritcloud.appstow`, versionName `3.0.3`, versionCode `16` im Stable-Quellcode kontrolliert.
- [x] Stable-Tagfilter `^v[0-9]+\.[0-9]+\.[0-9]+$` und F-Droid-Versionscode-Auswertung kontrolliert.
- [x] Lizenz GPL-3.0-or-later, Unicode/CLDR-Hinweise und freie AndroidX-Abhängigkeiten beibehalten.
- [x] README, Changelog, Quellcode, Issue-Tracker und Ko-fi-Link geprüft.
- [x] GitHub-Signaturschlüssel anhand der F-Droid-Allowlist kontrolliert.
- [x] Getrennter Dev-241-Praxistest, Unit-Tests, Lint und Final-Audit abgeschlossen.
- [ ] Stable-Tag `v3.0.3` sowie signierte APK und SHA-256-Prüfsumme auf GitHub bestätigt.
- [ ] F-Droid-Autoupdate, Build, Reproduzierbarkeitsprüfung und Veröffentlichung beobachten.
- [ ] `CurrentVersion` und `CurrentVersionCode` in fdroiddata erst als aktualisiert betrachten, wenn dort tatsächlich 3.0.3 / 16 steht.
- [ ] Nur bei Bedarf die F-Droid-Rezeptur über GitLab korrigieren.

F-Droid-Metadaten:
https://gitlab.com/fdroid/fdroiddata/-/blob/master/metadata/de.pritcloud.appstow.yml

Der geprüfte öffentliche Metadatenstand nannte noch 3.0.1 / 14; die öffentliche F-Droid-App-Seite zeigte 3.0.0 / 13. Die Aktualisierung ist davon unabhängig.

Die Rezeptur verwendet den upstream-signierten GitHub-Release als Referenz für den reproduzierbaren F-Droid-Build. Eine Signaturkompatibilität wird über den erlaubten Schlüssel geprüft, nicht allein aus der Paketkennung abgeleitet.

## English

- [x] Confirm stable application ID, versionName 3.0.3, versionCode 16.
- [x] Confirm stable-only tag matching and version extraction.
- [x] Preserve GPL-3.0-or-later, Unicode/CLDR notices and free AndroidX dependencies.
- [x] Verify README, changelog, source, issue tracker and Ko-fi links.
- [x] Validate upstream signing certificate against the allowed F-Droid key.
- [x] Complete Dev-241 device testing, unit tests, lint and final audit.
- [ ] Confirm stable tag, signed APK and SHA-256 checksum on GitHub.
- [ ] Monitor F-Droid auto-update, build, reproducibility verification and publication.
- [ ] Confirm fdroiddata actually reports CurrentVersion 3.0.3 and CurrentVersionCode 16.
- [ ] Change GitLab metadata only if required.

F-Droid publication is independent of GitHub publication.
