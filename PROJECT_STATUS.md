# AppStow – Projektstatus

Stand: 9. Oktober 2026

## Veröffentlicht: AppStow 3.0.0

- Aktuelle stabile Version: **3.0.0**, Versionscode **13**.
- Signierter GitHub-Release: `v3.0.0`, veröffentlicht am **9. Oktober 2026**.
- Unveränderlicher Release-Commit: `1682d9b8acec54f18c1f05e3c93e935b8207bd06`.
- Release-Dateien: `AppStow-3.0.0.apk` und `AppStow-3.0.0.apk.sha256`.
- GitHub Actions Stable-Build `37975741096`: erfolgreich.
- `main` enthält den veröffentlichten 3.0.0-Quellcode; anschließende reine Dokumentationsänderungen verändern das Release-Tag nicht.
- Phase 8 (Backup v3), Phase 9 (Hardening) und Deep-Review sind abgeschlossen.
- Geräte-Smoke-Tests Runden 1 bis 4 bestanden: Kategorien, manuelle und halbautomatische Sortierung, Sortiervorschläge sowie Vollautomatik einschließlich Rückkehr zu Manuell.
- Deep-Review-Nachtest 1 bestanden: Sicherung und Wiederherstellung der sichtbaren automatischen Favoriten mit Reihenfolge und Wechsel auf Manuell.
- Deep-Review-Nachtest 2 bestanden: Update, Backup-Wiederherstellung, Papra-Blob-Downloads, HTTPS-Downloads, Wiederholung, Abbruch und Neustart.
- Das private Recovery-Journal schützt unterbrochene Wiederherstellungen; eine Regression prüft die Sperre gegen parallele Start-Rücksicherung.
- Der ehemalige Entwicklungsbranch `dev/phase9` und die anderen alten Entwicklungsbranches wurden am 9. Oktober bereinigt.
- `dev/3.0-final-hardening` dient nur noch als letzte Release-Referenz und kann nach den Dokumentationsprüfungen separat entfernt werden.
- Die ältere Version `v2.0.1` und der getestete Ausgangsstand `tested-3.0.0-7f2db12` bleiben vorerst als Vergleichs- und Sicherungsreferenzen erhalten.

## Phase 9 – Hardening

- Nicht verwendetes Statistik-Reset-Icon entfernt.
- Regressionstests für gespeicherte Konfigurationen aus Stable 2.0.1 ergänzt.
- Kategorien, Favoriten, Shortcuts und manuelle Reihenfolgen werden mit den aktuellen Speicherklassen erfolgreich gelesen.
- Neue Sortier- und UI-Einstellungen besitzen sichere Standardwerte.
- Vollständige Unit-Tests, Android Lint sowie Debug- und Release-Builds waren erfolgreich.
- Baseline- und Startup-Profile mit dem Pixel-6-API-33-Managed-Device neu generiert.
- Die lokale Release-APK enthält das eingebettete Baseline Profile.
- Der direkte APK-Upgrade-Test von Stable 2.0.1 auf 3.0.0 wurde am 8. Oktober 2026 auf einem physischen Smartphone erfolgreich durchgeführt.
- Android akzeptierte das Update ohne vorherige Deinstallation.
- Kategorien, Favoriten, Shortcuts, Zuordnungen, Reihenfolgen und Einstellungen blieben laut Geräteprüfung erhalten.
- GitHub Actions Run 37773022513 bestätigte Build, Versionsfolge und identische Signaturzertifikate.
- Der veröffentlichte Release 3.0.0 verwendet Versionscode 13 und unterstützt so auch die Aktualisierung älterer Testinstallationen mit Code 12.
- Die Release-Dokumentation wurde für 3.0.0 aktualisiert.

## Phase 8 – Backup v3

- Neues verschlüsseltes Sicherungsformat v3.
- Gesichert werden Kategorien, Zuweisungen, Favoriten, eigene Shortcuts und manuelle Reihenfolgen.
- Listen-/Kachelansicht, Spaltenzahlen und weitere Anzeigeeinstellungen werden gesichert.
- Sortiermodus, Halbautomatik-Einstellungen, Vorschläge, Favoritenzahl und optionales Zeitprofil werden gesichert.
- Bedienseite, Favoriten-Startverhalten, Design und AppStow-Sprache werden gesichert.
- Statistik-Anzeigeeinstellungen werden gesichert; die Nutzungsstatistiken selbst werden nicht gesichert.
- Eine erfolgreiche Wiederherstellung setzt die Nutzungsstatistiken vollständig zurück.
- Nicht mehr installierte Apps werden aus Favoriten, Kategoriezuweisungen und App-Reihenfolgen entfernt.
- Kategorien und eigene Shortcuts bleiben erhalten.
- Web-App-Anmeldungen und Cookies sind nicht Bestandteil der Sicherung.
- Die Wiederherstellung wird validiert und bei einem Fehler auf den vorherigen Zustand zurückgerollt.
- Ein privates, atomar geschriebenes Journal sichert die zehn Preference-Stores einschließlich Nutzungsstatistik und Sprache vor Beginn des Restore-Vorgangs.
- Nach einem Prozessabbruch wird das Journal vor dem Laden der Oberfläche zur Rücksicherung verwendet; bei fehlerhafter Rücksicherung bleibt die Oberfläche gesperrt.
- Ein Activity-Neustart während einer laufenden Wiederherstellung darf keine parallele Journal-Rücksicherung ausführen: Beide Pfade sind synchronisiert.
- Die Sicherung aus Vollautomatisch enthält die zuletzt sichtbaren Favoriten einschließlich der gemischten App-/Shortcut-Reihenfolge.
- Die Backup-Verschlüsselung hat keinen persönlichen Schlüssel; die Einschränkung wird in der App und README erklärt.
- Cloud-/DocumentsProvider werden beim Lesen mit begrenzten Wiederholungen behandelt.
- Die Backup-Prüfung verwendet maximal 1 Sekunde zusätzliche Retry-Wartezeit.
- Die Wiederherstellung verwendet maximal 5 Sekunden zusätzliche Retry-Wartezeit.
- Das Sicherungsformat von AppStow 3.0 ist absichtlich nicht mit AppStow 2.x kompatibel.
- Der Kompatibilitätshinweis steht auf GitHub; in der App selbst wird kein 2.x-Hinweis angezeigt.

## Stabiler Release

- Version: **3.0.0**, Versionscode **13**.
- Tag: `v3.0.0`.
- Release-Commit: `1682d9b8acec54f18c1f05e3c93e935b8207bd06`.
- Veröffentlichungsdatum: 9. Oktober 2026.
- Signierter Stable-Workflow: `37975741096` (erfolgreich).
- APK und SHA256-Prüfsumme wurden beim Release geprüft.
- Die vorherige Version `v2.0.1` (Versionscode 11) bleibt als Referenz für den signierten Upgrade-Test erhalten.
- Der F-Droid-Aufnahme-MR !51175 wurde am 8. Oktober 2026 als gemergt gemeldet. Die öffentliche Verfügbarkeit und das Update auf F-Droid werden separat kontrolliert.

## Qualitätssicherung

- Backup-v3-Verschlüsselung und -Validierung sind durch Unit-Tests abgedeckt.
- Backup-Zustand, Restore-Filter und Backup-Zusammenfassung sind durch Regressionstests abgedeckt.
- Unit-Tests, Android Lint und Debug-Build werden vor jedem Dev-Release ausgeführt.
- Backup und Wiederherstellung wurden auf einem physischen Gerät geprüft.
- Beim Speichern über Androids Storage Access Framework kann das Cache-Verhalten eines Cloud-Providers nicht von AppStow gesteuert werden.
- AppStow bestätigt Cloud-Sicherungen deshalb nur innerhalb eines kurzen Prüfzeitraums und behandelt die Wiederherstellung separat mit längerer Lesewiederholung.

## Bekannter offener Prüfpunkt

Die Authelia-Passkey-Anfrage wird sofort abgebrochen.
Das Verhalten wurde bereits für 0.1.3 dokumentiert
und tritt auch im normalen Browser auf.
Die Ursache ist weiterhin ungeklärt.
Die Anmeldung mit 2FA funktioniert.

Andere Herstellergeräte und WebView-Versionen
wurden nicht vollständig getestet.

## Verbindliche Arbeitsweise

- UI-Änderungen werden zuerst gemeinsam abgestimmt und erst nach ausdrücklicher Zustimmung umgesetzt.
- Einstellungsseiten bleiben kompakt und sollen ohne unnötiges Scrollen bedienbar sein.
- Kurze Erklärungen stehen vor der zugehörigen Option; ausführliche Erklärungen gehören in die Hilfe.
- Fragezeichen führen direkt zum passenden Hilfeabschnitt.
- Ein vollständiger Fish-kompatibler Abschlussblock prüft den Arbeitsbaum, führt Tests, Lint und Builds aus, erstellt Commit und Push, startet bei Bedarf den signierten GitHub-Testbuild und prüft dessen Ergebnis.
- APKs werden bei Bedarf direkt über GitHub heruntergeladen; ein Stable-Release erfolgt nur nach ausdrücklicher Freigabe.

## Nächster Schritt

- Nachveröffentlichungs-Dokumentation abschließen: englische und vollständige deutsche README gegenseitig verlinken und den Stand 3.0.0 dokumentieren.
- Den unveränderlichen Release-Tag `v3.0.0` und die signierte APK nicht mehr verändern.
- Nach erfolgreicher Dokumentationskontrolle `dev/3.0-final-hardening` separat und mit Sicherheitsprüfungen entfernen, sodass nur `main` als Branch bestehen bleibt.
- Die F-Droid-Aufnahme und das erste tatsächliche Update auf Version 3.0.0 unabhängig von GitHub kontrollieren.
- `v2.0.1` und `tested-3.0.0-7f2db12` bleiben bis zu einer gesonderten Entscheidung als Referenzen erhalten.
