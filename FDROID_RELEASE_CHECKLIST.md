# AppStow 3.0.2 – F-Droid release checklist / Prüfliste

> This file describes the planned process, **not proof that F-Droid has published 3.0.2**.

## Deutsch

- [x] Sprachdateien vereinheitlicht: `CHANGELOG.md` ist Englisch (weiterhin das Ziel des F-Droid-Changelog-Links), `CHANGELOG.de.md` ist Deutsch; F-Droid-Metadaten bleiben unverändert.

**Quelle:** https://gitlab.com/fdroid/fdroiddata/-/blob/master/metadata/de.pritcloud.appstow.yml

- [x] Live-Rezeptur im GitLab-fdroiddata-Master vor Veröffentlichung als Originaldatei geprüft: `AutoUpdateMode: Version`, `UpdateCheckMode: Tags ^v[0-9]+\.[0-9]+\.[0-9]+$`, Versionscode-Auswertung, Changelog-, Lizenz-, APK-, Signatur- und Ko-fi-Links. Keine Änderung auf GitLab notwendig.
- [ ] Die Paketkennung bleibt `de.pritcloud.appstow`; Versionsname `3.0.2` und Versionscode `15` müssen im **Stable-Tag** stehen.
- [ ] `dev-*`-Tags und `dev/`-Branches dürfen keinen F-Droid-Autoupdate-Build auslösen.
- [ ] Lizenz GPL-3.0-or-later, Unicode/CLDR-Datenhinweise, nur bestehende freie AndroidX-Abhängigkeiten; keine Google-Play-Dienste und kein proprietäres SDK erforderlich.
- [ ] App-Version und README-/Changelog-Links, Quellcode, Issue-Tracker und Unterstützungs-Link kontrollieren; Donate/Ko-fi-Eintrag nicht unabsichtlich verlieren.
- [ ] F-Droid-Build ist ein **eigener** Build. Signatur und Updatekompatibilität gegenüber GitHub-APK nicht ohne Zertifikatsvergleich behaupten.
- [x] Signierten Kandidaten und Geräte-Upgrade von 3.0.1 (14) auf 3.0.2 (15) erfolgreich geprüft; bestehende Daten blieben erhalten.
- [ ] Erst danach Stable-Merge nach `main`, Tag `v3.0.2`, signiertes GitHub-Release sowie `AppStow-3.0.2.apk.sha256` erstellen.
- [ ] Anschließend F-Droid-Autoupdate und Build-Logs beobachten; nur bei Bedarf GitLab-Rezept bearbeiten, Fehlermeldungen dokumentieren.
- [ ] `CurrentVersion`/`CurrentVersionCode` auf F-Droid erst als aktualisiert markieren, wenn der Upstream-Check/Build sie tatsächlich auf 3.0.2/15 gesetzt hat.

## English

- [x] Documentation filenames normalized: `CHANGELOG.md` is English (the existing F-Droid changelog target), `CHANGELOG.de.md` is German; F-Droid metadata is unchanged.

**Metadata:** https://gitlab.com/fdroid/fdroiddata/-/blob/master/metadata/de.pritcloud.appstow.yml

- [x] Validated the live GitLab fdroiddata master recipe before publication: stable-only tag filter, update/version extraction, changelog, license, APK/source links, signing-key allowlist and Ko-fi metadata. No GitLab recipe edit required.
- [ ] Preserve application ID `de.pritcloud.appstow`, stable versionName `3.0.2` and versionCode `15` in the tagged source.
- [ ] Do not allow `dev-*` pre-release tags or development branches to trigger F-Droid updates.
- [ ] Verify GPL-3.0-or-later, bundled Unicode/CLDR notices and free AndroidX dependencies; no Google Play services or proprietary SDK is needed.
- [ ] Check README/changelog links, source, issue tracker and donation/Ko-fi metadata.
- [ ] F-Droid builds and signs an APK independently; do not assume signing compatibility with the GitHub APK.
- [x] Signed 3.0.1 (14) to 3.0.2 (15) candidate and on-device upgrade test passed; existing configuration was preserved.
- [ ] After approval only, merge to `main`, create tag `v3.0.2`, and publish GitHub-signed APK and SHA-256.
- [ ] Monitor F-Droid automatic update/build logs; edit its GitLab metadata only if required and document any issues.
- [ ] Mark `CurrentVersion` / `CurrentVersionCode` updated only after F-Droid actually reports 3.0.2 / 15.
