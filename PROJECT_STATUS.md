# AppStow – Projektstatus

Stand: 26. September 2026

## Status

Die Entwicklung von AppStow ist vorläufig abgeschlossen.

- Stabiler Release: `0.1.2`
- Tag: `v0.1.2`
- Release-Commit: `5cf6c689914a038a27486c057b39ea70051ad483`
- Hauptbranch: `main`

## Veröffentlichung

AppStow 0.1.2 wurde erfolgreich über den Signed-Release-Workflow veröffentlicht.

Release-Dateien:

- `AppStow-0.1.2.apk`
- `AppStow-0.1.2.apk.sha256`

Der Release-Build umfasst Signaturprüfung, R8-Codeoptimierung, Resource Shrinking, Baseline Profile, Unit-Tests und Android Lint.

## GitHub Actions

Der Signed-Release-Workflow wird für Tags nach dem Schema `v*` ausgeführt.

Der Dev-Release-Workflow ist nach Abschluss der Entwicklung nur noch manuell über `workflow_dispatch` startbar.

## Entwicklung wieder aufnehmen

Für eine spätere Weiterentwicklung wird ein neuer Entwicklungsbranch vom aktuellen `main` erstellt. Nach Tests wird der geprüfte Stand wieder nach `main` übernommen und mit einem neuen `v*`-Tag veröffentlicht.
