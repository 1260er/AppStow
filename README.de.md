# AppStow

[English](README.md) | **Deutsch**

AppStow ist ein Android-Launcher und Organizer für installierte Apps, Favoriten, Kategorien, eigene Verknüpfungen und Web-Apps.

## Funktionen

- Installierte Apps in eigenen Kategorien organisieren
- Apps und eigene Verknüpfungen als Favoriten markieren
- Kategorien mit Emoji-Symbolen versehen
- Offline-Emoji-Auswahl mit deutscher und englischer Suche
- Webseiten-, Web-App- und Deep-Link-Verknüpfungen erstellen
- Zwischen Listen- und Kachelansicht wechseln
- Globale Kachelansicht mit 3, 4 oder 5 Spalten auswählen
- Spaltenzahl für geöffnete Bereiche individuell festlegen
- Bei der App-Zuweisung die globale Ansicht übernehmen
- Apps durch Antippen Kategorien zuweisen
- Übersicht und Inhalte der Bereiche manuell sortieren
- Beim Sortieren Verschiebegriffe statt Favoritensterne anzeigen
- Lokale Nutzungsstatistik für Starts über AppStow anzeigen
- Manuelle, halbautomatische und vollautomatische Sortierung nutzen
- Sortiervorschläge und optionale Tag-/Abendprofile einstellen
- Darstellung, Bedienung und Startansicht (Aus, Bei Start oder Immer) anpassen
- Verschlüsselte manuelle v3-Sicherungen erstellen und wiederherstellen
- Android-Systembackups über Seedvault oder kompatible Dienste wie Google-Backup nutzen
- Deutsche und englische Benutzeroberfläche
- Helles und dunkles Systemdesign
- Unterstützung für dynamische bzw. einfarbige Launcher-Symbole unter Android

## Voraussetzungen

- Android 13 oder neuer
- minSdk 33
- targetSdk 36

## Installation

**Aktuelle stabile Version: AppStow 3.0.3 (Versionscode 16).**

Die **zuletzt veröffentlichte stabile Version** und ihre SHA-256-Prüfsumme findest du hier:

https://github.com/1260er/AppStow/releases/latest

Die Dateien heißen `AppStow-X.Y.Z.apk` und `AppStow-X.Y.Z.apk.sha256`. Entwicklungs-Builds sind gesonderte Vorabversionen und keine stabilen Updates.

Repository-Adresse für Obtainium:

```text
https://github.com/1260er/AppStow
```

## Kategorien

Du kannst Kategorien erstellen, umbenennen, löschen und ihnen ein Emoji-Symbol zuweisen. Eine App kann mehreren Kategorien angehören.

Unter **Apps zuweisen** tippst du eine App an, um ihre Kategorien festzulegen. Die Seite übernimmt die globale Listen- oder Kachelansicht. Der Favoritenstern funktioniert unabhängig von den Kategoriezuweisungen.

## Eigene Verknüpfungen

AppStow unterstützt Webseiten, HTTPS-Web-Apps und Deep Links.

Beispiele:

```text
https://www.google.com/maps/dir/?api=1&destination=Berlin
whatsapp://send?phone=+491701234567&text=Hallo
tel:+491701234567
intent:#Intent;action=android.media.action.IMAGE_CAPTURE;end
package:com.example.app
```

## Integrierte Web-Apps

HTTPS-Web-Apps laufen in einem separaten Android-WebView-Prozess.

Unterstützt werden unter anderem Datei- und Foto-Uploads, Kameraaufnahmen, Zoom, Vollbild, Standort- und Mikrofonanfragen sowie HTTPS-Downloads und kompatible Blob-Downloads.

Externe Links aus integrierten Web-Apps werden nur dann an Android übergeben, wenn die Navigation durch eine Benutzeraktion im Hauptfenster ausgelöst wurde. Unsichere interne URL-Schemata werden blockiert. Explizite Ziele und beliebige Zusatzparameter aus von Webseiten erzeugten Intent-URIs werden nicht weitergegeben. Eigene Deep-Link-Verknüpfungen in AppStow bleiben davon unberührt.

Webberechtigungen sind auf den konfigurierten HTTPS-Ursprung beschränkt. Bei authentifizierten HTTPS-Downloads werden Sitzungscookies und der ursprüngliche `Referer` nicht an einen anderen Ursprung weitergegeben.

Downloads werden in der Android-Downloads-Sammlung gespeichert. Die maximale Dateigröße beträgt 512 MiB. Gewöhnliche HTTPS-Downloads werden beendet, wenn die Web-App-Activity zerstört wird, und besitzen keine dauerhafte System-Downloadbenachrichtigung.

Welche Funktionen verfügbar sind, hängt von der installierten Android-WebView-Version und der jeweiligen Webseite ab. Für WebAuthn können korrekt konfigurierte Digital Asset Links erforderlich sein.

Integrierte Web-Apps benötigen eine vom System bestätigte Internetverbindung. Rein lokale Netze ohne bestätigten Internetzugang können deshalb abgewiesen werden.

## Nutzungsstatistik und Sortierung (3.0.0)

AppStow erfasst ausschließlich Starts, die über AppStow erfolgen. Die Nutzungsstatistiken bleiben auf dem Gerät, werden höchstens ein Jahr gespeichert und können unabhängig von den Anzeigeeinstellungen zurückgesetzt werden.

Die Sortierung unterstützt die Modi **Manuell**, **Halbautomatisch** und **Vollautomatisch**.

Im halbautomatischen Modus lassen sich vier Bereiche unabhängig steuern:

- **Favoriten:** Alle Apps und eigenen Verknüpfungen innerhalb der Favoriten sortieren.
- **Kategorien:** Die Kategorie-Bereiche selbst sortieren.
- **Kategorieinhalte:** Apps und eigene Verknüpfungen innerhalb jeder Kategorie sortieren.
- **Eigene Verknüpfungen:** Den separaten Bereich für eigene Verknüpfungen sortieren.

Ein aktivierter Inhaltsbereich wird als Ganzes sortiert, unabhängig davon, ob seine Einträge Apps oder Verknüpfungen sind; eine manuelle Umordnung ist dort nicht möglich. Deaktivierte Bereiche bleiben manuell sortierbar. Die Halbautomatik speichert die resultierenden Reihenfolgen als neue Ausgangslage. Bei der Rückkehr zu **Manuell** werden frühere Reihenfolgen nicht automatisch wiederhergestellt.

Der vollautomatische Modus sortiert Bereiche und Inhalte und wählt Favoriten anhand der Nutzung aus. Beim Verlassen der Vollautomatik werden die aktuelle automatische Reihenfolge und Favoritenauswahl als neuer manueller Zustand übernommen.

Im manuellen Modus können optionale nutzungsbasierte Sortiervorschläge einzeln geprüft und übernommen werden. **Favoriten neu zuweisen** verändert die Favoritenauswahl; **Favoriten sortieren** verändert ihre Reihenfolge. Kategorien, Kategorieinhalte und eigene Verknüpfungen können ebenfalls unabhängig übernommen werden.

Optional lassen sich Tag-/Abendprofile einrichten. Zeitprofile helfen der automatischen Sortierung, unterschiedliche Nutzungsgewohnheiten zu berücksichtigen: Alltags-Apps können tagsüber weiter oben stehen, Spiele oder Freizeit-Apps am Abend. Die Anfangszeiten legst du selbst fest. **Manuell** ist der Standardmodus.

## Startansicht (3.0.2)

Unter **Darstellung & Bedienung** legst du fest, wann AppStow die gewählte Ansicht öffnet: **Aus**, **Bei Start** oder **Immer**. In der zweiten horizontalen, mittig angeordneten Reihe wählst du **Letzte Kategorie**, **Favoriten** oder **Startseite**.

**Aus** öffnet bei einem vollständigen Neustart die Startseite und lässt beim Zurückkehren aus dem Hintergrund die aktuelle Seite bestehen. Die zweite Reihe ist dann deaktiviert; ihre Auswahl bleibt gespeichert. **Bei Start** öffnet die Auswahl bei einem neuen Start, **Immer** zusätzlich bei der Rückkehr aus einer anderen App. Der eigene Android-Dateidialog für Sicherungen gilt dabei nicht als normaler App-Wechsel.

**Letzte Kategorie** merkt sich die zuletzt geöffnete Kategorie innerhalb von „Apps starten“ (Liste und Kachel). Ist sie nicht mehr vorhanden, wird die Startseite geöffnet. Einstellungen, Statistik und Backup sind keine Kategorien.

## Verbesserungen in 3.0.3

- Schnellere Rückkehr zu Startseite, Favoriten oder letzter Kategorie bei entsprechend eingestellter Startansicht.
- Sichtbarkeit in Androids „Zuletzt verwendet“ unabhängig von der Startansicht einstellbar.
- Überflüssige Schreibvorgänge bei der Sortierung und mehrfache Zwischenaktualisierungen reduziert.
- Gleichzeitige Icon-Ladevorgänge zusammengefasst und veraltete Icon-Ergebnisse abgesichert.
- Zusätzliche Regressionstests und erfolgreicher Praxistest mit Dev-241.
- Die normale Android-Vorschau bleibt erhalten. Je nach Android-System kann beim Öffnen weiterhin ein kurzer Ansichtswechsel sichtbar sein.

## Backup und Wiederherstellung

AppStow 3.x verwendet das verschlüsselte manuelle Sicherungsformat v3. Gesichert werden:

- Kategorien, Symbole, Zuordnungen und Favoriten
- Eigene Verknüpfungen
- Manuelle Reihenfolgen von Bereichen und Einträgen
- Listen-/Kachelansicht und Spalteneinstellungen
- Sortiereinstellungen, Vorschläge und das optionale Zeitprofil
- Darstellungs- und Bedienungseinstellungen einschließlich der AppStow-Sprache
- Anzeigeeinstellungen der Statistik

**Nicht enthalten** sind die Nutzungsstatistiken selbst sowie Web-App-Sitzungen und Cookies.

Sicherungen aus dem vollautomatischen Modus enthalten die zuletzt sichtbaren automatischen Favoriten einschließlich der gemeinsamen Reihenfolge von Apps und eigenen Verknüpfungen. Nach der Wiederherstellung bleiben diese Favoriten zunächst erhalten, obwohl die Nutzungsstatistik zurückgesetzt wird. Neue Nutzungsdaten können die automatische Auswahl später verändern. Wer vorher auf **Manuell** wechselt, übernimmt die wiederhergestellte Favoritenauswahl samt Reihenfolge.

**Sicherheit der Sicherung:** Backup v3 verwendet AES-256-GCM mit zufälligem Salt und Initialisierungsvektor (IV). Es ist kein Benutzerpasswort erforderlich. Der Schlüssel wird aus einem fest hinterlegten Wert abgeleitet, der für alle Installationen gleich ist. Die Verschlüsselung erschwert das beiläufige Auslesen und erkennt Beschädigungen oder Veränderungen, bietet aber keinen starken Schutz der Vertraulichkeit oder Manipulationssicherheit gegenüber entschlossenen Dritten. Bewahre Sicherungsdateien deshalb an einem vertrauenswürdigen, privaten Ort auf.

Eine erfolgreiche Wiederherstellung setzt die Nutzungsstatistiken zurück. Während der Wiederherstellung schreibt AppStow einen atomaren Wiederanlauf-Schnappschuss in den privaten App-Speicher. Wird der Android-Prozess dabei beendet, versucht AppStow beim nächsten Start den vorherigen Einstellungszustand wiederherzustellen, bevor die Oberfläche geladen wird. Falls diese Rücksicherung fehlschlägt, bleibt die Oberfläche zur Sicherheit gesperrt. Die eigentliche Wiederherstellung und die Rücksicherung beim Start verwenden dieselbe prozessweite Sperre, auch beim Activity-Neustart nach einem Sprachwechsel. Das schützt vor unterbrochenen Wiederherstellungen, kann jedoch keinen Schutz vor Hardwarefehlern des Speichers garantieren.

Verweise auf nicht mehr installierte Apps werden aus Favoriten, Kategoriezuweisungen und App-Reihenfolgen entfernt. Kategorien und eigene Verknüpfungen bleiben erhalten.

> **Wichtiger Hinweis zu AppStow 3.0:** Version 3.0 überarbeitet das Sicherungsformat grundlegend. Sicherungen aus AppStow 2.x können nicht in AppStow 3.0 wiederhergestellt werden. Umgekehrt sind mit AppStow 3.0 erstellte Sicherungen nicht mit älteren AppStow-Versionen kompatibel.

## Android-Systembackup (3.0.2)

AppStow unterstützt außerdem Androids Systembackup-Schnittstelle. Den Backup-Dienst wählst und verwaltest du in den Android-Einstellungen; AppStow lädt selbst keine Sicherungen hoch. Vorgesehen sind **Seedvault**, **Google-Backup** und kompatible Android-Dienste. Die tatsächliche Verfügbarkeit hängt vom Gerät, Betriebssystem und Anbieter ab.

Die Freigabeliste umfasst ausschließlich: Kategorien, Favoriten, eigene Verknüpfungen, Bereichs- und Eintragsreihenfolgen, Übersichtsdarstellung, Bedienungseinstellungen, **Anzeigeeinstellungen** der Statistik und eine bereinigte Kopie der Sortierkonfiguration. Nutzungsbasierte Sortier-Snapshots werden nicht übertragen.

**Ausgeschlossen:** Nutzungsstatistiken, berechnete automatische Sortierdaten, Web-App-Daten, Cookies, Datenbanken, temporäre Backupdaten und das private Wiederherstellungsjournal. Cloud-Sicherungen setzen von Android gemeldete Verschlüsselungsfähigkeiten voraus. Für Geräteübertragungen gilt dieselbe Dateifreigabeliste.

Nach einer Android-Systemwiederherstellung werden die Sortiereinstellungen ohne Nutzungshistorie übernommen. Falls erforderlich, wechselt die Vollautomatik sicher zu Manuell, bis neue lokale Nutzungshistorie vorhanden ist. Anders als die manuelle v3-Sicherung wird das Systembackup von Android gesteuert und nicht innerhalb von AppStow importiert.

**Seedvault-Sicherung und -Wiederherstellung wurden auf einem Gerät erfolgreich getestet.** Google-Backup wird über dieselbe Android-Schnittstelle unterstützt, wurde aber noch **nicht** gesondert auf einem Gerät wiederhergestellt. Eine Google-Anmeldung, ein Google-SDK oder eine zusätzliche Abhängigkeit in AppStow sind nicht erforderlich.

## Getestetes Update von 3.0.1 auf 3.0.2

Der signierte Releasekandidat AppStow 3.0.2 (Versionscode 15) wurde auf einem physischen Smartphone **direkt über** die veröffentlichte, auf GitHub signierte Version 3.0.1 (Versionscode 14) installiert. Das Update gelang ohne Deinstallation. Kategorien, Favoriten, eigene Verknüpfungen, Reihenfolgen und Einstellungen wurden anschließend kontrolliert und blieben vollständig erhalten. Auch die Signatur- und Versionsprüfung in GitHub Actions war erfolgreich: [Upgrade-Test 3.0.2](https://github.com/1260er/AppStow/actions/runs/38472809157).

Der Gerätetest verwendete den signierten 3.0.2-Upgrade-Kandidaten aus demselben App-Quellstand wie das Stable-Release. GitHub- und F-Droid-Installationen können unterschiedliche Signaturzertifikate besitzen; ein Update zwischen beiden Quellen darf nicht ohne Prüfung als kompatibel gelten.

## Update von 2.0.1 auf 3.0.0

AppStow 3.0.0 unterstützt das direkte Android-Update von 2.0.1. Vorhandene Konfigurationsdaten wie Kategorien, Favoriten, Verknüpfungen und manuelle Reihenfolgen bleiben erhalten. Dafür muss keine Sicherungsdatei importiert werden.

Sicherungen aus AppStow 2.x können nicht in 3.0.0 importiert werden. Sicherungen aus 3.0.0 lassen sich auch nicht in 2.x importieren. Das direkte APK-Update von 2.0.1 auf 3.0.0 wurde erfolgreich auf einem physischen Android-Gerät getestet.

## Datenschutz

Beim Android-Systembackup gelangen nur die ausdrücklich freigegebenen Konfigurationsdateien an den ausgewählten Android-Backup-Anbieter. Für dessen Datenverarbeitung gelten die jeweiligen Datenschutzbedingungen. Nutzungsstatistiken sind sowohl aus AppStows manuellen Sicherungen als auch aus Android-Systembackups ausgeschlossen.

AppStow benötigt kein Benutzerkonto. Netzwerkzugriffe erfolgen für konfigurierte Webseiten und Web-Apps. Nutzungsstatistiken verbleiben lokal auf dem Gerät und werden von AppStow nicht hochgeladen.

## Entwicklung

- Java 17
- Gradle 8.11.1
- Android Gradle Plugin 8.10.1
- compileSdk 36
- targetSdk 36
- minSdk 33

## Unicode-Emoji-Daten

Der Offline-Emoji-Suchindex wird aus Unicode-Emoji- und CLDR-Daten erzeugt.

Lizenz- und Urheberrechtshinweise:

- `app/src/main/assets/emoji_search_LICENSE.txt`
- `app/src/main/assets/emoji_search_NOTICE.txt`

## Releases und Änderungen

- [Deutsches Changelog](CHANGELOG.de.md) | [English changelog](CHANGELOG.md)
- [AppStow 3.0.3 – Release-Hinweise (Deutsch und Englisch)](RELEASE_NOTES_3.0.3.md)
- [Aktueller Projektstatus](PROJECT_STATUS.de.md)

https://github.com/1260er/AppStow/releases

## Unterstützung

Wenn du die Arbeit an AppStow und anderen Projekten unterstützen möchtest, findest du die Spendenseite hier: **[Ko-fi – 1260er](https://ko-fi.com/1260er)**.

## Lizenz

AppStow ist freie Software unter der GNU General Public License, Version 3 oder (nach deiner Wahl) einer späteren Version (SPDX: GPL-3.0-or-later). Siehe [LICENSE](LICENSE).

Für Unicode-Emoji-Daten gelten die gesonderten Lizenz- und Urheberrechtshinweise oben.
