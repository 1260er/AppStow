# AppStow – Projektstatus

Stand: 9. Oktober 2026

## Aktuelle Entwicklung – AppStow 3.0

- Entwicklungsbranch: `dev/phase9`, abgezweigt von `dev/automatic-sorting` bei `dev-208`.
- Der stabile Release `v2.0.1` bleibt unverändert.
- Statistik, automatische Sortierung sowie Darstellung & Bedienung sind umgesetzt.
- Phase 8 – Backup v3 ist abgeschlossen.
- Phase 9 – Hardening und Deep-Review abgeschlossen; finaler Release wird vorbereitet.
- Getesteter Ausgangsstand: Commit 7f2db12.
- Dauerhaftes GitHub-Prerelease: tested-3.0.0-7f2db12.
- Gesonderter Abschlussbranch: dev/3.0-final-hardening.
- Letzter funktional geprüfter Hardening-Stand vor Release-Dokumentation: Commit d0eee22 auf dev/3.0-final-hardening.
- Geräte-Smoke-Tests Runden 1 bis 4 bestanden: Kategorien, manuelle und halbautomatische Sortierung, Sortiervorschläge sowie Vollautomatik mit Rückkehr zu Manuell.
- Die Halbautomatik sortiert Apps und eigene Shortcuts gemeinsam innerhalb eines Bereichs.
- Deep-Review-Nachtest 1 bestanden: sichtbare automatische Favoriten samt Reihenfolge werden gesichert, wiederhergestellt und nach Manuell übernommen.
- Deep-Review-Nachtest 2 bestanden: Update, normale Backup-Wiederherstellung, Papra-Blob-Downloads, HTTPS-Downloads, Wiederholung, Abbruch und Neustart.
- Ein privates Recovery-Journal schützt bei einem Abbruch während der Wiederherstellung; die Wiederherstellung und die Start-Rücksicherung nutzen bereits dieselbe Klassensperre.
- Die Synchronisierung gegen parallele Start-Rücksicherung wird durch einen gezielten Regressionstest abgesichert.
- Die Übernahme auf main und die Veröffentlichung von 3.0.0 erfordern ausdrückliche Freigabe.
- GitHub-Bereinigung am 9. Oktober abgeschlossen: Übrig sind nur main, dev/3.0-final-hardening und die Releases v2.0.1 sowie tested-3.0.0-7f2db12. Alle vier früheren Entwicklungsbranches sowie 64 ältere Releases wurden entfernt.
- main und Stable 2.0.1 bleiben bis zur ausdrücklichen Freigabe unverändert.

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
- Für den finalen Release 3.0.0 ist Versionscode 13 vorgesehen, damit auch bereits installierte Testversionen mit Code 12 aktualisiert werden können.
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

- Version: 2.0.1.
- Versionscode: 11.
- Tag: `v2.0.1`.
- Release-Commit: `56c9ffa489980506ce9266d3bdfea702bc75f2a7`.
- Der stabile Tag und der stabile Release werden durch die 3.0-Entwicklung nicht verändert.
- Deutsche und englische F-Droid-Metadaten sind vorhanden.
- Der F-Droid-Aufnahme-MR !51175 wurde am 8. Oktober 2026 als gemergt gemeldet.
- Der öffentliche F-Droid-Paketstatus ist damit noch nicht separat bestätigt.

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

Nach Abschluss der Release-Dokumentation und erneut bestandenem Upgrade-Test
den vollständig geprüften Stand per Fast-Forward auf main übernehmen. Erst nach
expliziter Freigabe v3.0.0 taggen und den signierten Stable-Release durch GitHub
Actions erstellen. Release-APK, SHA256, Signatur und Updatefähigkeit überprüfen.
Anschließend die F-Droid-Verfügbarkeit und den Update-/Signaturweg getrennt
prüfen. Der Stable-Release v2.0.1 und der getestete Ausgangsstand bleiben bis
zur ausdrücklichen Entscheidung darüber als Referenzen bestehen.
Der Hardening-Branch kann erst nach der erfolgreichen Veröffentlichung entfallen.
