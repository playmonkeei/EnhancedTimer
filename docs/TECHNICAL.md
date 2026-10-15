[← Übersicht für Spieler](../README.de.md) · [English overview](../README.md)

# Enhanced Timer — Technische Anleitung

Stoppuhr, Countdown und frei platzierbarer HUD für Minecraft Java **26.1–26.3**. Version **1.0.0**. Der Standard bleibt schlicht: goldene Minecraft-Schrift über der Hotbar, ohne Kasten oder Statuszeile.

## Installation und Plattformen

Java **25** verwenden und die passende Datei aus den [GitHub-Releases](https://github.com/playmonkeei/EnhancedTimer/releases) installieren. Lokale Builds liegen in `build/releases/`:

| Minecraft | Mod-Datei für Client oder Mod-Server |
| --- | --- |
| 26.1, 26.1.1, 26.1.2 | `enhancedtimer-26.1-1.0.0.jar` |
| 26.2 | `enhancedtimer-26.2-1.0.0.jar` |
| 26.3 | `enhancedtimer-26.3-1.0.0.jar` |

Die Mod-JAR gehört nach `mods/`. Sie enthält die Einstiegspunkte für **Fabric, Quilt, Forge und NeoForge**, einschließlich dediziertem Mod-Server. Fabric API, QSL und eine zusätzliche Konfigurationsbibliothek werden von Enhanced Timer nicht benötigt. [Mod Menu](https://modrinth.com/mod/modmenu) ist auf Fabric/Quilt optional; seine eigenen Abhängigkeiten ebenfalls installieren.

Für Bukkit-Server gehört **`enhancedtimer-server-1.0.0.jar`** nach `plugins/`. Das Plugin verwendet die gemeinsame Bukkit/Spigot-API und hat einen separaten Scheduler für Folia. Die historische BetterTimer-Baseline umfasst 20 Client-/Loader-Kombinationen auf Spigot, 20 dedizierte Mod-Server und zehn zusätzliche Paper-/Purpur-/Folia-Szenarien. Der Nachtest unter dem neuen Namen wird separat ausgewiesen. Konkrete Builds, Prüfumfang und verbleibende manuelle Bedienprüfungen stehen in [docs/TESTING.md](TESTING.md); die Prüfsummen und Logpfade in [docs/COMPATIBILITY_TESTS.json](COMPATIBILITY_TESTS.json).

**Auf einem fremden Server reicht die Client-Mod für den eigenen Timer.** Gemeinsame Server- und Teamtimer benötigen Enhanced Timer auf dem Server: entweder die passende Mod-JAR oder das Plugin. Auf einem Server jeweils nur eine EnhancedTimer-Implementierung installieren. Proxy-Software wie Velocity/BungeeCord ist kein Installationsziel; dort läuft keine Spielwelt. Bedrock und zukünftige Versionen wie 26.4 sind nicht Teil dieser Ausgabe.

Das ZIP-Paket `build/releases/EnhancedTimer-1.0.0.zip` enthält alle vier JARs, diese Anleitung, den Prüfstand und SHA-256-Prüfsummen. Pro Installation nur die passende JAR auswählen.

Quellcode und Fehlerberichte: [playmonkeei/EnhancedTimer](https://github.com/playmonkeei/EnhancedTimer). Der Veröffentlichungsablauf steht in [docs/RELEASING.md](RELEASING.md).

## Bedienung

Im Spiel **B**, `/et` oder **Mod Menu → Enhanced Timer → Konfigurieren** öffnen. Die Tasten lassen sich unter **Steuerung → EnhancedTimer** ändern. Bereits gespeicherte Belegungen bleiben erhalten; falls O noch mit der Freunde-Funktion kollidiert, dort B oder eine freie Taste wählen. Enhanced Timer fügt keine Schaltflächen in die Vanilla-Einstellungen oder ins Pausenmenü ein.

- **Start** startet oder setzt nach einer Pause fort. Nach Stopp oder Ablauf beginnt ein neuer Durchlauf.
- **Pause** erhält den Stand; **Stopp** hält ihn an; **Zurücksetzen** setzt ihn auf die Startanzeige zurück.
- **Rückgängig** stellt bis zu 20 Bedienaktionen einschließlich Zeitstand und Zwischenzeiten wieder her.
- **Zeit setzen** setzt einen angehaltenen Anzeigewert. Bei der Stoppuhr ist dies die verstrichene Zeit, beim Countdown eine neue Restdauer; danach mit Start fortsetzen.
- Ein Countdown bleibt bei null stehen und meldet den Ablauf groß auf dem Bildschirm. Vorwarnungen, etwa 60/10/5 Sekunden, sind anpassbar. Töne: Orb, Glocke, Chime, Level-up oder aus.
- Im echten Einzelspieler-Pausenmenü pausiert die lokale Uhr standardmäßig mit dem Spiel. Multiplayer-Menüs pausieren den Server nicht. Laufende gespeicherte Uhren werden nach einem Neustart pausiert wiederhergestellt; Offline-Zeit zählt nicht mit.

```text
/et                          Menü
/et timer                    Stoppuhr neu starten
/et countdown 2h             Zwei-Stunden-Countdown starten
/et start                    Start / fortsetzen
/et pause                    Pause
/et stop                     Stopp
/et reset                    Zurücksetzen
/et undo                     Rückgängig
/et set 1h30m                Anzeigewert setzen, pausiert
/et split Nether             Zwischenzeit erfassen
/et splits                   Zwischenzeiten anzeigen
/et edit                     HUD verschieben
/et source local             Lokalen Timer anzeigen
/et source server            Gemeinsamen Servertimer anzeigen
/et design export mein_hud   Design speichern
/et design import mein_hud   Design laden
```

Dauern: `2d`, `2h`, `1h30m`, `90m`, `30s`, Sekunden, `MM:SS` oder `HH:MM:SS`; maximal 365 Tage. `set 0` ist bei einer Stoppuhr erlaubt. Ungültige Eingaben verändern den bisherigen Stand nicht.

Lokale `/et`-Commands werden im Client verarbeitet und benötigen keine Cheats, auch in Hardcore. Sie erteilen keine Serverrechte. Bei gewähltem Servertimer werden Timeraktionen an den Server geschickt und dort erneut auf Berechtigung geprüft. Server-Chatlinks führen keine lokalen EnhancedTimer-Commands aus. Der allgemeine Name `/timer` bleibt für andere Mods frei.

## HUD und Designs

Neun Ankerpositionen, freie Positionierung mit Maus oder Pfeiltasten, Skalierung, Schatten, Transparenz und optionaler Hintergrund. Speichern übernimmt die Position; Abbrechen/Escape verwirft Änderungen im Positionseditor. Das Layout bleibt relativ zur Bildschirmgröße und wird an die sichtbaren Grenzen gebunden.

Alle **16 Minecraft-Farben**, animierter Regenbogen pro Zeichen und eine eigene Hex-Farbe sind enthalten. Eigene Farben bleiben beim Wechsel zu einer anderen Palette erhalten. Pausen- und Ablaufzustände können eigene Farben bekommen. Vorschau, Editor und HUD verwenden dieselbe Textdarstellung und Breitenmessung.

Formate: Uhr, kompakt, Millisekunden, Tage, automatisch ohne leere Einheiten und eigene Vorlagen. Zusätzlich können reale Ortszeit oder die Uhrzeit der Spielwelt angezeigt werden. Eigene Vorlagen, Präfix und Suffix sind getrennt für laufend, pausiert und abgelaufen einstellbar.

| Platzhalter | Inhalt |
| --- | --- |
| `{d}` | Tage |
| `{H}` | Stunden insgesamt |
| `{h}`, `{hh}` | Stunden innerhalb eines Tages, unaufgefüllt / zweistellig |
| `{m}`, `{mm}` | Minuten, unaufgefüllt / zweistellig |
| `{s}`, `{ss}` | Sekunden, unaufgefüllt / zweistellig |
| `{ms}` | Millisekunden, dreistellig |

`[?d:{d} Tage ]` blendet die gesamte Gruppe bei null Tagen aus. Entsprechende Gruppen gibt es für `H`, `h`, `m` und `s`; Gruppen werden nicht verschachtelt. Damit sind Präfix, Suffix, Stil und Ausblendung jeder Einheit in einer Vorlage möglich. Beispiel:

```text
[?d:<gold>{d}d </gold>][?h:{h}h ]{mm}:{ss}
<gradient:#ffaa00:#ff55ff:{phase}>{hh}:{mm}:{ss}</gradient>
```

[Adventure MiniMessage](https://docs.advntr.dev/minimessage/format.html) ermöglicht unter anderem Textstile, Farben und Verläufe. `{phase}` animiert einen Verlauf. Für die Darstellung werden Adventure und seine Abhängigkeiten privat eingebettet, damit keine gemeinsam geladenen Bibliotheksversionen überschrieben werden.

Vier mitgelieferte Designs und lokale JSON-Dateien lassen sich ohne Konto verwenden. Die **Designbibliothek** kann auf ausdrücklichen Klick einen selbst angegebenen HTTPS-Katalog laden. Es gibt keine Hintergrundabfragen oder Uploads. Ein öffentlicher EnhancedTimer-Dienst mit Konten und Uploads ist noch nicht betrieben.

Katalogformat, maximal 256 Designs / 256 KiB:

```json
{"version":1,"designs":[{"name":"Gold","design":{"colorMode":"GOLD","format":"CLOCK","shadow":true}}]}
```

Importe übernehmen ausschließlich Aussehen und Layout, keine Timerregeln, Sprache oder Serverauswahl. Lokale Dateien liegen unter `config/enhancedtimer/designs/`.

## Mehrspieler, Teams und Regeln

Unter **Einstellungen → Timerquelle** zwischen lokalem Timer, gemeinsamem Servertimer, privatem Servertimer oder einem vorhandenen Team wechseln. Synchronisierte Zwischenzeiten sind auch im Client-Menü sichtbar und als CSV exportierbar. Ein Team wird mit `/ets team create name` erstellt; `join`, `leave`, `list` und `delete` sind ebenfalls vorhanden.

Serverkommandos beginnen mit `/ets`, `/servertimer` oder `/enhancedtimer-server`. Auf Bukkit öffnet `/ets` ein Inventarmenü; Mod-Server bieten eine klickbare Chatsteuerung. Spieler ohne Client-Mod sehen eine Bossbar oder optional eine Actionbar, Warnungen und die Ablaufmeldung.

```text
/ets countdown 2h
/ets pause
/ets set 30m
/ets undo
/ets split Festung
/ets personal
/ets global
/ets team create challenge
/ets team join challenge
/ets rules start-move true
/ets rules pause-death true
/ets rules stop-dragon true
/ets rules splits true
/ets rules freeze true
/ets rules protect true
/ets rules blind true
/ets rules death-spectator true
/ets rules expiry-spectator true
/ets hide
/ets show
```

Regeln ermöglichen Start bei Bewegung, Pause bei Tod, Stopp nach dem Enderdrachen, automatische Nether-/End-/Drachen-Zwischenzeiten, Bewegungssperre/Schutz/Blindheit während einer Pause sowie Zuschauermodus bei Tod oder Ablauf. Weltverändernde Regeln sind ausschließlich durch Timeradministratoren änderbar. In einem fremden Multiplayer-Spiel kann die reine Client-Mod solche Serverregeln nicht erzwingen. Lokaler Start bei Bewegung, Pause bei Tod und automatische Meilensteine sind separat im Client konfigurierbar.

Gemeinsame Uhren steuern Administratoren, private Uhren ihr Besitzer, Teamuhren der Besitzer oder Administrator. Andere Teammitglieder können zuschauen. Private Servertimer sind für andere Spieler nicht auswählbar. Bukkit-Rechte: `enhancedtimer.view` für alle, `enhancedtimer.control` standardmäßig für OPs. Mod-Server verwenden Spielleiterrechte beziehungsweise den Einzelspieler-Weltbesitzer.

Standardmäßig setzt eine Serveruhr ohne aktive Teilnehmer aus. Die ausgewählte Serverquelle wird etwa zweimal pro Sekunde synchronisiert; dazwischen zählt der HUD lokal weiter. Nach Verbindungsabbruch pausiert die entfernte Uhr und die Anzeige fällt auf den lokalen Timer zurück. Die zusätzliche Server-Bossbar wird bei ausgewähltem Client-HUD ausgeblendet.

## Sprachen und Speicherung

**Deutsch, Englisch, Französisch, Spanisch**. Standard ist die Spielsprache: genaue Sprache → unterstützte Sprachfamilie → Englisch. Beispielsweise `de_at` → Deutsch und `ja_jp` → Englisch. Die Mod-Sprache lässt sich unabhängig wählen und gilt für den gesamten Client.

Clientdaten unter `.minecraft/config/enhancedtimer/`:

- `settings.json`: Vorgaben für neue Welten/Server und globale Mod-Sprache.
- `profiles/`: Layout und Einstellungen je Welt/Server, mit gehashten Dateinamen.
- `sessions/`: Zeitstände, Zwischenzeiten und Meilensteine je Welt/Server.
- `designs/`, `exports/`: Designs und exportierte CSV-Zwischenzeiten.

Mod-Server: `<Welt>/enhancedtimer/server-settings.json` und `timers.dat`. Bukkit: `plugins/EnhancedTimer/config.yml` und `timers.dat`; alte `timer.yml` wird beim ersten Umstieg übernommen. Serverseitiges Anzeigeformat, Uhrmodus, Farbe, Warnungen, Bossbar/Actionbar und Leerstandspause werden dort eingestellt. Konfigurationsdateien werden beim Start geladen.

Gespeichert wird nach Bedienaktionen, regelmäßig nach zehn Sekunden sowie beim sauberen Beenden. Dateiaustausch erfolgt atomar, soweit vom Dateisystem unterstützt. Ein Absturz kann die letzten zehn Sekunden verlieren. Fehlerhaftes Client-JSON wird als `.broken-…` gesichert; Archive werden vor der Übernahme vollständig validiert.

## Umbenennung vor dem ersten Release

Die früheren lokalen Entwicklungsstände hießen BetterTimer. Enhanced Timer verwendet eigene Mod-IDs, Packages, Commands und Datenordner. Details und Hinweise für vorhandene lokale Entwicklungsdaten stehen in [BRANDING.md](BRANDING.md). Die historische Testmatrix bleibt unverändert; der aktuelle Nachtest steht in [RELEASE_1.0.0.json](RELEASE_1.0.0.json).

## API und Entwicklung

Der gemeinsame Java-Kern unter `core/` enthält `TimerEngine`, `TimerSession`, `TimerHub`, Formate, Regeln und das Netzwerkprotokoll. `TimerSession.listen(Consumer<Change>)` beobachtet Bedienaktionen und Zwischenzeiten; das zurückgegebene `AutoCloseable` meldet den Listener ab. Zeitablauf wird weiterhin über den Timerstatus abgefragt. API-Aufrufe gehören auf den besitzenden Spiel-/Timerthread.

Das Bukkit-Plugin registriert `TimerHub` im ServicesManager. `EnhancedTimerPlugin.executeOnTimerThread(...)` führt Integrationen auf dem richtigen globalen Scheduler aus, auch unter Folia. Auf Mod-Servern ist der Hub über `VanillaTimerServer.get(server).hub` erreichbar; dort auf dem Serverthread arbeiten. Es gibt noch kein separat veröffentlichtes Maven-Artefakt oder eine stabile API-Versionsgarantie.

```sh
./gradlew clean build releaseBundle
python3 tools/verify_artifacts.py
./gradlew :client-26.3:runClient -PwithModMenu
./gradlew :client-26.1:compileJava -Pminecraft261=26.1.1
./gradlew :client-26.1:compileJava -Pminecraft261=26.1.2
./gradlew :paper:compileJava -PbukkitApi=spigot
```

Der Gradle Wrapper verwendet 9.6.0 mit SHA-256-Prüfung. Abhängigkeiten kommen aus den jeweiligen offiziellen Repositories. CI prüft alle drei Spielreihen, beide 26.1-Patches, die Spigot-API, Tests und die fertigen JARs. Der Timer zählt mit einer monotonen Uhr, unabhängig von Renderframes und Tickrate.

| Verzeichnis | Aufgabe |
| --- | --- |
| `core/` | Zeitlogik, Teams, Rechte, Regeln, Zwischenzeiten, Protokoll und Archive |
| `richtext/` | Privat eingebettetes MiniMessage und Serializer |
| `client/` | Gemeinsamer Client, Netzwerk und Vanilla-Serveranbindung |
| `versions/26.x/` | Versionsabhängige Bildschirm-/HUD-Einstiegspunkte |
| `paper/` | Bukkit-Plugin mit optionalem Folia-Scheduler |
| `tools/` | JAR-Prüfung, Loader-Testlauncher und getrennte Testmod |

Der Quellcode wurde eigenständig erstellt. Referenzen: [MTimer](https://github.com/MUtils-MC/MTimer), [mTimer-Datapack](https://modrinth.com/datapack/mtimer), [Timer & Clock](https://modrinth.com/plugin/timer), [AlphaSlither Timer](https://modrinth.com/mod/timer-alphaslither). Eine vollständig nachgewiesene Funktionsgleichheit mit allen Ausgaben dieser Projekte wird noch nicht behauptet.

MIT-Lizenz; eingebettete Drittanbieter-Lizenzen liegen in den JARs. Kein offizielles Minecraft-Produkt; keine Verbindung zu Mojang oder Microsoft.
