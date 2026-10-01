# AppStow – Projektstatus

Stand: 1. Oktober 2026

## Stabiler Release

- Version: 0.1.3
- Tag: v0.1.3
- Release-Commit: 9b86b95
- Hauptbranch: main
- Signierte APK und SHA-256-Prüfsumme veröffentlicht und geprüft.

## Qualitätssicherung

- Vollständige Debug- und Release-Prüfung erfolgreich.
- Unit-Tests und Android Lint erfolgreich.
- R8-Optimierung und Baseline Profile geprüft.
- Dev-136 auf GrapheneOS erfolgreich getestet.
- Aktualisierte CI mit Dev-137 erfolgreich geprüft.
- Stable-Update auf GrapheneOS getestet.
- 2FA-Anmeldung, Downloads und ChatGPT erfolgreich getestet.

## Offener Prüfpunkt

Die Authelia-Passkey-Anfrage wird sofort abgebrochen.
Das Verhalten tritt auch im normalen Browser auf.
Die Ursache ist noch ungeklärt.
Die Anmeldung mit 2FA funktioniert.

Andere Herstellergeräte und WebView-Versionen wurden
nicht vollständig getestet.

## Nacharbeiten

Die CI-Action upload-artifact wurde auf v6 vorbereitet.
Der aktualisierte Workflow muss noch geprüft werden.
Anschließend werden alte Dev-Releases und der
abgeschlossene Entwicklungsbranch bereinigt.
