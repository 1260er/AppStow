# AppStow – Projektstatus

Stand: 2. Oktober 2026

## Stabiler Release

- Version: 2.0.0
- Tag: v2.0.0
- Release-Commit: 079b951
- Hauptbranch: main
- Signierte, R8-optimierte APK veröffentlicht.
- SHA-256-Prüfsumme erfolgreich verifiziert.
- APK-Signatur erfolgreich geprüft.
- Signaturzertifikat mit Version 0.1.3 identisch.
- Paketname und Versionsnummer geprüft.
- Versionscode gegenüber 0.1.3 erhöht.

## Abgeschlossene Entwicklung

- Globale Listen- und Kachelansicht.
- Globale Spaltenzahl mit 3, 4 oder 5 Spalten.
- Individuelle Spaltenzahl für geöffnete Bereiche.
- Apps zuweisen übernimmt die globale Ansicht.
- Kategoriezuweisung durch kurzes Antippen.
- Favoritenstern mit kompakter 32-dp-Tippfläche.
- Verschiebegriff ersetzt beim Sortieren den Stern
  an derselben Position.
- Anzeigeeinstellungen im Backup berücksichtigt.
- Hilfe auf Deutsch und Englisch überarbeitet.
- App-Informationen und Ko-fi-Link ergänzt.

## Qualitätssicherung

- Debug- und Release-Unit-Tests erfolgreich.
- Android Lint für Debug und Release erfolgreich.
- Lokale Debug- und Release-Builds erfolgreich.
- Signierte Dev-Version auf dem Gerät getestet.
- Listen- und Kachelansichten freigegeben.
- Darstellung mit 3 und 5 Spalten geprüft.
- Favoritenstern und Verschiebegriff getestet.
- Stabiler Release-Workflow erfolgreich.
- Stabile APK und SHA-256-Datei geprüft.
- Update von der stabilen Version 0.1.3
  auf die stabile Version 2.0.0 auf dem Gerät
  erfolgreich durchgeführt.

## Bekannter offener Prüfpunkt

Die Authelia-Passkey-Anfrage wird sofort abgebrochen.
Das Verhalten wurde bereits für 0.1.3 dokumentiert
und tritt auch im normalen Browser auf.
Die Ursache ist weiterhin ungeklärt.
Die Anmeldung mit 2FA funktioniert.

Andere Herstellergeräte und WebView-Versionen
wurden nicht vollständig getestet.

## Abschluss

- Dev-Releases dev-138 bis dev-149 entfernt.
- Alle zugehörigen Dev-Tags entfernt.
- Entwicklungsbranch dev/ui-search-grid
  lokal und auf GitHub entfernt.
- Alle fünf stabilen Releases erhalten.
- main enthält die vollständige Release-Dokumentation.
- Release-Tag v2.0.0 bleibt unverändert
  auf Commit 079b951.
- AppStow 2.0.0 erfolgreich abgeschlossen.
