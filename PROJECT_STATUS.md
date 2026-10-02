# AppStow – Projektstatus

Stand: 2. Oktober 2026

## Stabiler Release

- Version: 0.1.3
- Tag: v0.1.3
- Release-Commit: 9b86b95
- Hauptbranch: main
- Signierte APK und SHA-256-Prüfsumme veröffentlicht und geprüft.
- Version 2.0.0 ist noch nicht stabil veröffentlicht.

## Entwicklungsstand 2.0.0

- Entwicklungsbranch: dev/ui-search-grid
- Freigegebener Entwicklungsstand: aa448e4
- Globale Listen- und Kachelansicht abgeschlossen.
- Globale Spaltenzahl mit 3, 4 oder 5 Spalten umgesetzt.
- Individuelle Spaltenzahlen für geöffnete Bereiche umgesetzt.
- Apps zuweisen übernimmt die globale Ansicht.
- Kurzes Antippen öffnet die Kategoriezuweisung.
- Favoritenstern mit kompakter 32-dp-Tippfläche.
- Verschiebegriff ersetzt beim Sortieren den Favoritenstern
  an derselben Position.
- Anzeigeeinstellungen in Backup und Wiederherstellung integriert.
- Hilfe auf Deutsch und Englisch überarbeitet.
- App-Informationen mit Namenserklärung und Ko-fi-Link ergänzt.
- Die UI wurde auf dem Gerät getestet und freigegeben.

## Qualitätssicherung

- Debug- und Release-Unit-Tests erfolgreich.
- Android Lint für Debug und Release erfolgreich.
- Lokale Debug- und Release-Builds erfolgreich.
- Signierter, R8-optimierter Dev-Build erfolgreich erstellt.
- Listen- und Kachelansichten auf dem Gerät getestet.
- Darstellung bei 3 und 5 Spalten geprüft.
- Favoritenstern und Verschiebegriff getestet.
- Die abschließende stabile Release-Prüfung steht noch aus.

## Bekannter offener Prüfpunkt

Die Authelia-Passkey-Anfrage wird sofort abgebrochen.
Das Verhalten wurde bereits für 0.1.3 dokumentiert
und tritt auch im normalen Browser auf.
Die Ursache ist noch ungeklärt.
Die Anmeldung mit 2FA funktioniert.

Andere Herstellergeräte und WebView-Versionen wurden
nicht vollständig getestet.

## Nächste Schritte

1. Dokumentation für 2.0.0 abschließen.
2. Entwicklungsstand kontrolliert auf main übernehmen.
3. Abschließende Release-Prüfung durchführen.
4. Signierten stabilen Release v2.0.0 erstellen.
5. APK, Signatur, Prüfsumme und Installation prüfen.
6. README und Projektstatus auf den veröffentlichten
   stabilen Release aktualisieren.
7. Erst danach abgeschlossene Dev-Releases und den
   Entwicklungsbranch bereinigen.
