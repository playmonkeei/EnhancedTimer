# Prüfstand Enhanced Timer

## Release 1.0.0

Enhanced Timer 1.0.0 führt die unten dokumentierte, bisher BetterTimer genannte Implementierung fort. Die Umbenennung betrifft auch Mod-ID, Java-Packages, Netzwerkkanal, Commands und Datenpfade. Frische Build-, Paket- und Laufzeitprüfungen mit den konkreten Enhanced-Timer-JAR-Prüfsummen stehen in [RELEASE_1.0.0.json](RELEASE_1.0.0.json). Die vollständige Loader- und manuelle Baseline bleibt als historischer Nachweis mit ihren ursprünglichen 0.2.0-Prüfsummen erhalten; sie wurde nicht nachträglich als vollständiger 1.0.0-Matrixtest umetikettiert.

Die erste lokale 1.0.0-Prüfung vor der Umbenennung bleibt separat in [PRE_RENAME_1.0.0.json](PRE_RENAME_1.0.0.json) erhalten. Alte Dateinamen und Rohlogpfade in den historischen Berichten beziehen sich auf die damals getesteten BetterTimer-Artefakte.

## Entwicklungsbaseline BetterTimer 0.2.0

Stand: 15. Oktober 2026. Getestet auf macOS ARM64 mit Java 25, getrennten Minecraft-Prozessen und ausschließlich lokalen Testservern. Die Tabellen bezeichnen konkrete Laufzeittests der installierbaren JARs. Sie sind keine pauschale Freigabe unbekannter Modpacks, Forks oder zukünftiger 26.x-Versionen.

Die folgenden 50 Szenarien dokumentieren die Baseline **vor** den anschließenden manuellen UI-Korrekturen. Deren konkrete Ergebnisse und die Nachtests der finalen JARs stehen in [MANUAL_TESTS.md](MANUAL_TESTS.md) und [MANUAL_TESTS.json](MANUAL_TESTS.json). Die Prüfsummen der Baseline werden nicht nachträglich auf neue JARs umgeschrieben.

## Spieltestmatrix

**20 Clientkombinationen → Spigot:** Jede Zelle bedeutet: Weltbeitritt, private Timerauswahl mit Steuerrecht, Countdown konfigurieren und sofort starten, direkter Aufruf des lokalen `/bt split`-Handlers für den Servertimer, Zwischenzeit synchronisieren und bei laufender Uhr rückgängig machen, Pause, Restdauer setzen, erneut starten, Ablauf, Undo und Wechsel zur lokalen Anzeige. Anschließend wurden alle 13 Konfigurations-/Editor-Screens gerendert und das echte Plugin-Inventarmenü geöffnet und geschlossen.

| Minecraft | Fabric | Quilt | Forge | NeoForge |
| --- | --- | --- | --- | --- |
| 26.1 | PASS · 0.19.5 | PASS · 0.30.1 | PASS · 62.0.9 | PASS · 26.1.0.19-beta |
| 26.1.1 | PASS · 0.19.5 | PASS · 0.30.1 | PASS · 63.0.2 | PASS · 26.1.1.15-beta |
| 26.1.2 | PASS · 0.19.5 | PASS · 0.30.1 | PASS · 64.1.5 | PASS · 26.1.2.121 |
| 26.2 | PASS · 0.19.5 | PASS · 0.30.1 | PASS · 65.1.5 | PASS · 26.2.0.93 |
| 26.3 | PASS · 0.19.5 | PASS · 0.30.1 | PASS · 66.0.12 | PASS · 26.3.0.83 |

**20 dedizierte Mod-Server → Fabric-Testclient:** Dieselben versionierten BetterTimer-JARs wurden zusätzlich auf den dedizierten Fabric-, Quilt-, Forge- und NeoForge-Servern geladen. Jeder Server wurde mit zwölf echten RCON-Kommandos geprüft und von einem Fabric-Client derselben Minecraft-Version betreten. Synchronisierung und alle 13 Screens wurden erneut geprüft. Das sind zwei gezielte Testmatrizen, keine Prüfung jeder möglichen Kreuzkombination verschiedener Loader und Server.

| Minecraft | Fabric | Quilt | Forge | NeoForge |
| --- | --- | --- | --- | --- |
| 26.1 | PASS | PASS | PASS | PASS |
| 26.1.1 | PASS | PASS | PASS | PASS |
| 26.1.2 | PASS | PASS | PASS | PASS |
| 26.2 | PASS | PASS | PASS | PASS |
| 26.3 | PASS | PASS | PASS | PASS |

Die native Servermatrix verwendet jeweils exakt 26.1, 26.1.1, 26.1.2, 26.2 beziehungsweise 26.3. Für die beiden ersten Reihen ist die getestete NeoForge-Distribution als **Beta** veröffentlicht.

## Bukkit-Server

Zusätzlich zu Spigot wurden folgende eigenständigen Serverpakete mit dem Bukkit-Plugin, echten RCON-Kommandos und einem passenden Fabric-Client geprüft. Alle Durchläufe umfassen die oben genannte Timerfolge, 13 Client-Screens und das echte Server-Inventarmenü.

| Server | Minecraft | Build | Anbieterkanal | Ergebnis |
| --- | --- | --- | --- | --- |
| Paper | 26.1.1 | 29 | ALPHA | PASS |
| Paper | 26.1.2 | 74 | STABLE | PASS |
| Paper | 26.2 | 134 | STABLE | PASS |
| Paper | 26.3 | 190 | STABLE | PASS |
| Purpur | 26.1.2 | 2592 | – | PASS |
| Purpur | 26.2 | 2633 | – | PASS |
| Purpur | 26.3 | 2651 | – | PASS |
| Folia | 26.1.2 | 8 | STABLE | PASS |
| Folia | 26.2 | 7 | BETA | PASS |
| Folia | 26.3 | 3 | ALPHA | PASS |

**Spigot:** Offizielle BuildTools #201 erzeugten eigenständige Spigot-JARs für 26.1.2, 26.2 und 26.3. Die offiziellen BuildTools-Zuordnungen für `26.1` und `26.1.1` zeigen beide auf die 26.1.2-Quellstände. Deshalb wurden die Clients **26.1, 26.1.1 und 26.1.2 tatsächlich gegen Spigot 26.1.2** getestet. Alle drei Clientversionen verwenden Protokoll 775. Es werden keine separaten historischen Spigot-26.1-/26.1.1-Serverläufe behauptet. Die genauen Serverversionen stehen im maschinenlesbaren Bericht.

RCON prüft jeweils: Countdown mit Tagen, Pause, Zwischenzeit, Zwischenzeitenliste, Stopp, Undo, Reset, Zeitsetzen, Team anlegen/auswählen, Team-Countdown, Administratorregel `freeze` und Team löschen. Die Testserver wurden anschließend sauber gestoppt. Die EULA wurde ausschließlich für die isolierten Testinstallationen nach ausdrücklicher Benutzerzustimmung akzeptiert.

## Nachweise und gefundene Korrekturen

[COMPATIBILITY_TESTS.json](COMPATIBILITY_TESTS.json) enthält die **50 erfolgreichen Client-Server-Szenarien**, gepinnte Loader-Versionen, Serverversionsausgaben, JAR-Prüfsummen, Zeitstempel und Pfade der Rohlogs. Die Rohlogs liegen im ignorierten Projektverzeichnis `.cache/matrix/`. Der Bericht wird nur erzeugt, wenn alle erwarteten Zellen bestanden sind und deren Mod-Prüfsumme zum eingefrorenen Release-Kandidaten passt. Ergebnisdateien werden vor einem Durchlauf entfernt; ein Start ohne frisches PASS zählt nicht als Erfolg.

Im Ausbau dieser Matrix korrigiert:

- Die Forge-Metadaten erlauben jetzt auch Forge 62 und 63 für Minecraft 26.1 beziehungsweise 26.1.1.
- `pack.mcmeta` macht die Mod-Ressourcen auch für Forge vollständig erkennbar.
- Der eigene Custom-Payload-Handler des Spielers wird nun direkt abgefangen. Der zuvor ausschließlich im gemeinsamen Listener liegende Empfangspfad wurde unter anderem auf Forge umgangen: Beitritt und Serverkommandos funktionierten, die Timer-Synchronisierung fehlte. Der zuvor fehlgeschlagene Forge-Spieltest und die übrige native Matrix wurden mit der Korrektur wiederholt.

Frühere Laufzeittests hatten bereits die beidseitige `minecraft:register`-Kanalregistrierung, Forge-kompatible Mixin-Angaben, Tastenregistrierung außerhalb eingeschränkter Konstruktor-Injektionen und die Folia-Schedulerbehandlung verbessert.

Forge-Clients wurden in dieser Matrix über den **offiziellen Installer als Produktionsinstallation** gestartet. Der Forge-26.3-Entwicklungslauncher war beim Vanilla-/Spigot-Beitritt über eine nicht geladene Forge-Serverkonfiguration abgestürzt; BetterTimer enthält dafür keinen globalen Eingriff in Forge-Konfigurationen. Fabric/Quilt laden die fertige Mod-JAR über Knot; NeoForge-Clients verwenden den offiziellen Entwicklungslauncher mit der fertigen Mod-JAR. Dedizierte Forge-/NeoForge-Server sind normale Installer-Installationen.

In den isolierten macOS-Testlaunchern wird bei NeoForge das frühe Ladefenster deaktiviert (`earlyWindowControl=false`). Forge erhält die von macOS benötigte Option `-XstartOnFirstThread`. Diese Einstellungen verändern keine Benutzerinstallation. Die nicht angemeldeten Testclients verwenden Dummy-Zugangsdaten; erwartete Realms-/Authentifizierungsfehler in deren Logs sind kein fehlgeschlagener Timer-Test.

## Automatisierte Prüfungen

| Prüfung | Ergebnis |
| --- | --- |
| Gemeinsamer Kern | 34 JUnit-Tests bestanden |
| MiniMessage / Serializer | 2 JUnit-Tests bestanden |
| Minecraft 26.1, 26.2, 26.3 | Je 8 Clienttests bestanden |
| Gesamt | 60 erfolgreiche Testausführungen, keine Fehler |
| Minecraft 26.1.1 und 26.1.2 | Kompiliert; anschließend Laufzeittests mit derselben auslieferbaren 26.1-JAR |
| Bukkit gegen Paper- und reine Spigot-API | Kompiliert; reale Serverläufe siehe oben |
| Fertige JARs | Vier aktuelle Archive; Metadaten, Mixin-Klassen, Ressourcen-Metadaten und Service-Provider geprüft |
| Eingebettete Bibliotheken | Adventure/Gson privat umbenannt; keine fremden Moduldeskriptoren oder doppelten Einträge |
| Sprachen | DE/EN/FR/ES mit je 142 identischen, nicht leeren Schlüsseln; statische UI-Verweise geprüft |

Die Kernprüfungen umfassen Zeitmessung und Wiederherstellung, Parsergrenzen, Rundung, Positionierung, Farben/Regenbogen, Tages- und Bedingungsformate, Undo, gesetzte Zeitstände, Zwischenzeiten, Meilensteine, einmalige Warnungen, Rechte, Teamlöschung, transaktionale Archive und begrenzte Netzwerkpakete mit allen 256 Zwischenzeiten. Clienttests prüfen native Textkomponenten, unveränderte Plugin-Message-Bytes, Weiterleitung fremder Payloads, Kanalregistrierung, Größenbegrenzung, Zustandsfarben und begrenzte Designimporte.

## Wiederholung

Die Testmod unter `tools/smoke-driver/` ist in keiner Release-JAR enthalten. Sie beendet ihren Client nach PASS oder Fehler. Die Skripte verändern Timer und Konfigurationen ausschließlich in eigenen Testordnern. Benötigt werden Java 25, Gradle und die jeweiligen offiziellen Server-JARs. `--accept-eula` darf nur nach eigener Zustimmung verwendet werden.

```sh
./gradlew build releaseBundle
python3 tools/verify_artifacts.py
python3 tools/prepare_game_matrix.py
# Server-JAR mit den offiziellen Spigot-BuildTools erzeugen; hier beispielhaft 26.3:
python3 tools/game_matrix.py --game 26.3 --server-jar /absoluter/pfad/spigot-26.3.jar --accept-eula
python3 tools/native_server_matrix.py --games 26.3 --accept-eula
# Entsprechend alle weiteren Tabellenzeilen ausführen; für Bukkit-Forks:
python3 tools/game_matrix.py --game 26.3 --server-kind paper --server-jar /absoluter/pfad/paper.jar --loaders fabric --accept-eula
python3 tools/summarize_game_matrix.py
```

Für die 26.1-Clientreihe beim Spigot-Test die 26.1.2-Server-JAR übergeben. `--game 26.1.1` und `--game 26.1.2` bauen den passenden Testtreiber; die Mod selbst bleibt der eingefrorene 26.1-Release-Kandidat. `--skip-passed` überspringt nur bestandene Clientzellen mit passender Mod-Prüfsumme. Die zwei nativen und Spigot-Testreihen verwenden getrennte Clientordner und lokale Ports; parallele Spigot-Reihen benötigen eigene `--port`-/`--rcon-port`-Werte und verschiedene Minecraft-Zeilen.

## Grenzen dieser Freigabe

Die automatisierte Matrix rendert die 13 Screens; die anschließend ausgeführten nativen Bedienprüfungen sind separat in [MANUAL_TESTS.md](MANUAL_TESTS.md) dokumentiert. Drag-and-drop einschließlich Speichern/Abbrechen, Hardcore ohne Cheats, zwei echte Multiplayer-Spieler, Rechte, private Timer, Folia-Regionsgrenzen, Neustart, Sprachen/Fallback, Farben und F1 sind dort konkret nachgewiesen. Zwei Testgruppen sind nur teilweise abgedeckt.

Eine vollständige manuelle Prüfung sämtlicher Loader-/Patchkombinationen, anderer HUD-Mods, aller Inventarvarianten, Spieler ohne Client-Mod und sämtlicher Challenge-Ereignisse bleibt offen. Folia 26.1.2 ist STABLE, 26.2 BETA und 26.3 ALPHA; Paper 26.1.1 ist ebenfalls ALPHA. Diese Anbieterkennzeichnungen bleiben trotz bestandenem BetterTimer-Test bestehen.

Ein manuell geladener HTTPS-Designkatalog ist implementiert. Ein öffentlicher BetterTimer-Dienst mit Konten/Uploads, ein veröffentlichtes Maven-API-Artefakt und öffentliche Modrinth-/CurseForge-Releases sind nicht Teil dieser lokalen Prüfungen. Windows/Linux wurden nicht als eigene Spielumgebungen getestet. Eine allgemeine Fehlerfreiheit oder vollständige Funktionsparität jeder MTimer-Ausgabe wird daraus nicht abgeleitet.
