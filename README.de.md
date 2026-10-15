<div align="center">

![Enhanced Timer — Make every second yours.](docs/assets/brand/hero.svg)

![Minecraft 26.1–26.3 · 1.0.0 · 4 Mod Loader · EN, DE, FR, ES · MIT](docs/assets/brand/badges.svg)

**Ein besserer Timer für dein nächstes Minecraft-Abenteuer.**

Hochzählen. Herunterzählen. Genau dort, wo du ihn haben willst.

[Loslegen](#get-started) · [Dein HUD](#customize) · [Mehrspieler](#multiplayer) · [English](README.md) · [Technische Anleitung](docs/TECHNICAL.md)

</div>

[**1.0.0 herunterladen**](https://github.com/playmonkeei/EnhancedTimer/releases/latest) · Die Bilder zeigen die echte Mod in Minecraft; die Screenshots verwenden die englische Oberfläche.

![Ein schlichter goldener Timer über der Hotbar in Minecraft](docs/assets/screenshots/gameplay.png)

## ✨ Deine Zeit. Dein Abenteuer.

Eine Survival-Welt für die nächsten Monate. Ein Bauprojekt, das seine eigene Stoppuhr verdient. Eine Zwei-Stunden-Challenge mit Freunden. **Enhanced Timer misst die Zeit, während du spielst** – im vertrauten Minecraft-Look und mit einem Menü, das du jederzeit per Taste öffnen kannst.

| | Das steckt drin |
| :--- | :--- |
| ⏱️ **Stoppuhr** | Starten, pausieren, stoppen und später weitermachen. Für Sessions, Builds und Runs. |
| ⌛ **Countdown** | Ein Zeitlimit setzen, Vorwarnungen bekommen und den Ablauf groß auf dem Bildschirm sehen. |
| 🖱️ **Freie Position** | Einen festen Platz wählen oder den Timer mit der Maus dorthin ziehen, wo er passt. |
| 💚 **Hardcore geeignet** | Mit **B** das Menü öffnen. Dein lokaler Timer braucht keine Cheats. |
| 💾 **Deine Welten merken sich die Zeit** | Eigene Zeitstände und Einstellungen je Welt oder Server. Nach einem Neustart pausiert fortsetzen. |
| 🌍 **Vier Sprachen** | Deutsch, Englisch, Französisch und Spanisch. Automatisch nach Spielsprache oder frei auswählbar. |

<a id="get-started"></a>

## 🚀 In einer Minute startklar

1. **Installiere deinen Mod Loader** und lege die passende Enhanced-Timer-JAR in den Ordner `mods`.
2. **Betritt eine Welt oder einen Server.** Öffne Enhanced Timer mit **B**.
3. **Wähle Stoppuhr oder Countdown und drücke Start.** Los geht's.

Auf Fabric und Quilt geht es zusätzlich über **Mod Menu → Enhanced Timer → Konfigurieren**. Mod Menu ist optional; seine eigenen Abhängigkeiten musst du bei Bedarf mitinstallieren.

![Das Enhanced-Timer-Menü mit Start, Pause und Stopp](docs/assets/screenshots/menu.png)

<details>
<summary><strong>📦 Welche Datei brauche ich?</strong></summary>

Eine Mod-Datei pro Minecraft-Reihe – passend für **Fabric, Quilt, Forge und NeoForge**:

| Deine Minecraft-Version | Deine Datei |
| :--- | :--- |
| **26.1 / 26.1.1 / 26.1.2** | `enhancedtimer-26.1-1.0.0.jar` |
| **26.2** | `enhancedtimer-26.2-1.0.0.jar` |
| **26.3** | `enhancedtimer-26.3-1.0.0.jar` |

Für Spieler gehört eine passende JAR nach `mods/`. Enhanced Timer selbst benötigt weder Fabric API noch QSL. Minecraft 26.x verwendet Java 25; nutze einen passenden Launcher bzw. eine passende Java-Laufzeit.

**Für Serverbesitzer:** Die Mod-JAR läuft auch auf einem passenden Mod-Server. Für **Spigot, Paper, Purpur und Folia** gibt es `enhancedtimer-server-1.0.0.jar` für den Ordner `plugins/`. Pro Server nur eine Enhanced-Timer-Variante installieren.

[Lade die passende JAR bei den Releases herunter](https://github.com/playmonkeei/EnhancedTimer/releases/latest). Das ZIP enthält alle Varianten; pro Installation brauchst du nur die passende JAR.

</details>

<a id="customize"></a>

## 🎨 Dein HUD. Dein Stil.

Der Standard bleibt schlicht: **goldene Minecraft-Schrift über der Hotbar**. Wenn du mehr möchtest, stellst du es dir selbst zusammen.

| Position | Farben | Anzeige |
| :--- | :--- | :--- |
| Neun feste Anker | Alle 16 Minecraft-Farben | Uhr oder kompakte Einheiten |
| Drag-and-drop-Editor | Animierter Regenbogen | Tage oder Millisekunden |
| Größe und Textschatten | Eigene Hex-Farbe | Leere Zeiteinheiten ausblenden |
| Optionaler Hintergrund | Eigene Zustandsfarben | Eigene Stile und Farbverläufe |

<table>
<tr>
<td width="50%"><a href="docs/assets/screenshots/editor.png"><img src="docs/assets/screenshots/editor.png" alt="Der Positionseditor mit frei platziertem Timer"/></a><br/><strong>Platz für dein Setup.</strong><br/>Verschiebe den Timer neben andere HUD-Mods. Speichern übernimmt die Position, Abbrechen stellt die vorherige wieder her.</td>
<td width="50%"><a href="docs/assets/screenshots/rainbow.png"><img src="docs/assets/screenshots/rainbow.png" alt="Der Timer mit Regenbogenfarben"/></a><br/><strong>So bunt, wie du möchtest.</strong><br/>Klassisch, deine Lieblingsfarbe oder Regenbogen – du entscheidest.</td>
</tr>
</table>

Lieblingslooks kannst du als Designs speichern. Verschiedene Welten und Server können eigene Einstellungen haben.

<a id="multiplayer"></a>

## 🤝 Zusammen spielen

**Dein lokaler Timer funktioniert auch auf gewöhnlichen Servern.** Du kannst also dein Bauprojekt oder deinen Run messen, ohne dass der Serverbesitzer etwas installieren muss.

Mit Enhanced Timer auf dem Server kommen gemeinsame Uhren dazu:

| 🧍 Persönlich | 🌐 Serverweit | 👥 Team |
| :--- | :--- | :--- |
| Dein privater Servertimer | Eine gemeinsame Uhr für den Server | Eine gemeinsame Uhr für deine Gruppe |
| Von dir gesteuert | Von Administratoren gesteuert | Vom Besitzer oder Administrator gesteuert |

Mit Client-Mod siehst du den HUD und die Konfigurationsmenüs. Ohne Client-Mod kannst du gemeinsame Uhren über die Server-Bossbar oder optional die Actionbar verfolgen. Das Bukkit-Plugin bietet zusätzlich ein Inventarmenü im Spiel.

## 🏁 Für Runs, Builds und Challenges

Aktiviere die Helfer, die zu deinem Spiel passen:

- **Start bei Bewegung**, wenn es losgehen soll.
- **Pause bei Tod**, wenn deine Challenge das vorsieht.
- **Zwischenzeiten**, auf Wunsch automatisch für Nether, End und Drachen.
- **Stopp beim Drachen-Advancement**, um deinen Run abzuschließen.
- **Countdown** mit Warntönen und einer großen Ablaufmeldung.

Die Regeln deiner Challenge bestimmst du. Auf dem Server können Administratoren weitere Regeln aktivieren, etwa Zuschauermodus bei Tod oder Zeitablauf.

![Die große Ablaufmeldung eines Countdowns in Minecraft](docs/assets/screenshots/countdown.png)

## ⌨️ Erst die Buttons. Commands gehen auch.

| Du möchtest … | So geht's |
| :--- | :--- |
| Den Timer öffnen | **B** drücken oder `/et` eingeben |
| Starten, pausieren oder stoppen | Die Buttons im Timer-Menü verwenden |
| Zwei Stunden Zeitlimit setzen | Countdown wählen, `2h` eingeben, übernehmen und starten |
| Die Anzeige verschieben | Aussehen → HUD verschieben öffnen |
| Die Sprache ändern | Einstellungen → Sprache öffnen |
| Eine Taste ändern | Minecraft-Optionen → Steuerung → Enhanced Timer |

Wenn du lieber tippst: `/et timer`, `/et countdown 2h`, `/et pause` oder `/et start`. Lokale Commands brauchen keine Cheats. Gemeinsame Servertimer beachten die Serverrechte.

## 💬 Gut zu wissen

<details>
<summary><strong>Funktioniert das in Hardcore ohne Cheats?</strong></summary>

Ja. Taste und lokales Menü funktionieren ohne Cheats. Lokale `/et`-Commands werden im Client verarbeitet und geben dir keine zusätzlichen Serverrechte.

</details>

<details>
<summary><strong>Was passiert, wenn ich Minecraft schließe?</strong></summary>

Dein Timer wird gespeichert. Eine laufende Uhr wird beim nächsten Besuch pausiert wiederhergestellt; Offline-Zeit zählt nicht mit. Im Einzelspieler folgt der lokale Timer standardmäßig der Spielpause. Im Mehrspieler läuft die Zeit beim Öffnen eines Menüs weiter.

</details>

<details>
<summary><strong>Was passiert bei einer nicht unterstützten Spielsprache?</strong></summary>

Dann verwendet Enhanced Timer Englisch. Du kannst Deutsch, Englisch, Französisch oder Spanisch auch direkt in den Mod-Einstellungen auswählen und deine Minecraft-Sprache beibehalten.

</details>

<details>
<summary><strong>Kann ich damit Speedruns messen?</strong></summary>

Ja, inklusive Zwischenzeiten. Der Drachen-Stopp reagiert auf das Drachen-Advancement des Spielers. Eine eigene RTA-/IGT-Auswertung oder Leaderboard-Anbindung ist nicht enthalten. Für eingereichte Runs gelten die Anforderungen deiner Kategorie.

</details>

<details>
<summary><strong>Welche Minecraft-Versionen werden unterstützt?</strong></summary>

Minecraft Java 26.1, 26.1.1, 26.1.2, 26.2 und 26.3. Bedrock, eine Installation auf Proxys, zukünftige Versionen und jedes denkbare Modpack sind nicht Teil der aktuellen Kompatibilitätszusage. Die technische Anleitung verlinkt die konkreten Testnachweise und deren Grenzen.

</details>

---

<div align="center">

**Dein nächstes Abenteuer beginnt bei `00:00:00`.**

Von **playmonkeei** · [English](README.md) · [Technische Anleitung](docs/TECHNICAL.md) · [Testnachweise](docs/TESTING.md) · [MIT-Lizenz](LICENSE)

<sub>Enhanced Timer ist ein eigenständiges Minecraft-Projekt. Kein offizielles Minecraft-Produkt. Keine Verbindung zu Mojang oder Microsoft.</sub>

</div>
