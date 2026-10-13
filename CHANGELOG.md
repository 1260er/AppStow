# Changelog

**Deutsch** | [English](CHANGELOG.en.md)

## 3.0.2 – Releasekandidat (noch nicht veröffentlicht)

- Neue Einstellung **Startansicht** mit zwei horizontal und mittig angeordneten Auswahlreihen: **Aus / Bei Start / Immer** und **Letzte Kategorie / Favoriten / Startseite**.
- **Aus** behält die bisherige Startlogik bei und deaktiviert die zweite Auswahlreihe. Bereits gespeicherte Startverhalten werden übernommen.
- Zuletzt besuchte Kategorien können beim Start geöffnet werden; die Listen- und Kachelansicht und gelöschte Kategorien werden berücksichtigt.
- Android-Systembackup für Seedvault, Google-Backup und kompatible Backup-Dienste vorbereitet, mit strikter Positivliste der Konfigurationsdateien.
- Nutzungsstatistiken, automatische Sortier-Snapshots, WebView-Sitzungen und das interne Wiederherstellungsjournal sind aus dem Systembackup ausgeschlossen; für Cloud-Backups werden Android-Verschlüsselungsfähigkeiten verlangt.
- Systembackup stellt die gespeicherte Sortierkonfiguration ohne Nutzungshistorie wieder her und wählt, falls erforderlich, einen sicheren manuellen Sortiermodus.
- Der eigene manuelle Sicherungs- und Wiederherstellungsweg bleibt beim kompatiblen AppStow-Backupformat **v3**.
- Backup-Dateidialoge lösen unter **Immer** keinen unerwünschten Wechsel zur Startansicht mehr aus; auch Abbruch und Activity-Neuerstellung wurden abgesichert.
- Neue Regressionstests für Startansicht, Systembackup-Regeln und Backup-Dateidialog.
- Seedvault: Sicherung und Wiederherstellung auf einem Gerät erfolgreich getestet. Google-Backup: vorbereitet, jedoch noch nicht separat auf einem Gerät getestet.
- Versionscode 15; signierter Update-Test von Stable 3.0.1 auf 3.0.2 und finale Freigabe stehen noch aus.

## 3.0.1 – 12. Oktober 2026

- Neue Intervall-Snapshots für halb- und vollautomatische Sortierung und optionale nutzungsbasierte Vorschläge.
- Verbesserte Sortier- und Favoritenlogik sowie weitere Tests der manuellen Reihenfolgen.
- Verbesserungen beim Schreiben und Verifizieren von Sicherungen über Android-Dokumentanbieter.
- Zusätzliche UI-Anpassungen und Regressionstests; Versionscode auf 14 erhöht.

## 3.0.0

- Neue lokale Nutzungsstatistik für über AppStow gestartete Apps und Shortcuts.
- Auswertungen nach Zeitraum, meistgenutzten und nicht gestarteten Apps sowie Nutzung von Kategorien.
- Statistikdaten bleiben lokal, werden höchstens ein Jahr gespeichert und können separat zurückgesetzt werden.
- Neue manuelle, halbautomatische und vollautomatische Sortierung.
- Halbautomatik steuert Favoriten, Kategorien, Kategorieinhalte und eigene Shortcuts unabhängig; Apps und Shortcuts werden innerhalb eines Bereichs gemeinsam behandelt.
- Sortiervorschläge lassen sich bereichsweise übernehmen.
- Optionale nutzungsbasierte Sortiervorschläge und Tag-/Abendprofile.
- Neue Einstellungen für Darstellung, Bedienseite, Startverhalten und Design.
- Der direkte APK-Upgrade-Test von Stable 2.0.1 auf 3.0.0 wurde auf einem physischen Smartphone erfolgreich durchgeführt; die bestehende Konfiguration blieb erhalten.
- Baseline Profiles für den aktuellen Entwicklungsstand neu generiert.
- Neues verschlüsseltes Backupformat v3 für die AppStow-Konfiguration.
- Sicherungen aus der Vollautomatik bewahren die zuletzt angezeigten Favoriten einschließlich App-/Shortcut-Reihenfolge; Rückfall nach Restore ohne Nutzungsstatistik abgesichert.
- Wiederherstellung gegen Prozessabbrüche durch privates, atomar geschriebenes Recovery-Journal gehärtet; Start-Rücksicherung und laufender Restore sind synchronisiert.
- Blob-Downloads aus integrierten Web-Apps gegen übergroße Datenblöcke und unbegrenzte Nachrichtenwarteschlangen abgesichert.
- Hinweis zum begrenzten Schutz der Backup-Verschlüsselung in Deutsch und Englisch ergänzt.
- Sortier-, Anzeige-, Bedienungs-, Sprach- und Statistik-Anzeigeeinstellungen werden mitgesichert.
- Nutzungsstatistiken selbst werden nicht gesichert und nach erfolgreicher Wiederherstellung zurückgesetzt.
- Nicht mehr installierte Apps werden bei der Wiederherstellung aus Favoriten, Kategoriezuweisungen und App-Reihenfolgen entfernt.
- Kategorien und eigene Shortcuts bleiben bei der Wiederherstellung erhalten.
- Das Backupformat von AppStow 3.0 ist absichtlich nicht mit AppStow 2.x kompatibel.
- Vollständiger Deep-Review einschließlich Smartphone-Nachtests, paralleler Restore-Sperre und signiertem Upgrade-Test erfolgreich abgeschlossen.

## 2.0.1 – 4. Oktober 2026

- Android-App-Sprachauswahl für Deutsch und Englisch ergänzt.
- Vorbereitungen für die Veröffentlichung auf F-Droid.
- Versionscode für zukünftige Updates erhöht.

## 2.0.0 – 2. Oktober 2026

- Neue Listen- und Kachelansicht für App starten.
- Globale Kachelansicht mit 3, 4 oder 5 Spalten.
- Individuelle Spaltenzahl für geöffnete Bereiche.
- Apps zuweisen übernimmt die globale Listen- oder Kachelansicht.
- Kategoriezuweisung durch kurzes Antippen einer App.
- Einheitliche Favoritensterne in den Kachelansichten.
- Kompakte 32-dp-Tippfläche bei unveränderter Sternposition.
- Verschiebegriff ersetzt beim Sortieren den Favoritenstern.
- Suche, Filter und Sortierung der Inhalte.
- Anzeigeeinstellungen werden im Backup berücksichtigt.
- Verbesserte Validierung und Absicherung der Wiederherstellung.
- Überarbeitete Hilfe auf Deutsch und Englisch.
- Erweiterte App-Informationen mit Erklärung des Namens.
- Ko-fi-Link zur Unterstützung der Projekte insgesamt.

## 0.1.3 – 1. Oktober 2026

- Erweiterte HTTPS-Web-App-Unterstützung.
- Verbesserte Datei-, Foto- und Kamera-Uploads.
- Zoom, Vollbild und Webberechtigungen.
- HTTPS- und Blob-Downloads einschließlich Papra-PDFs.
- Geschützte Sitzungscookies bei HTTPS-Weiterleitungen.
- Korrekte Dateinamen bei wiederholten Blob-Downloads.
- Verbesserte WebView-Bereinigung und Regressionstests.

## 0.1.2 – Stabiler Release

- Verbesserungen bei Backup und Wiederherstellung.
- Weitere Stabilitätskorrekturen.
