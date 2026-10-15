# Manuelle Spieltests vom 15. Oktober 2026

14 Testgruppen bestanden, zwei teilweise geprüft. Native Maus- und Tastaturbedienung auf macOS ARM64 mit Java 25, Fabric 26.1 und 26.3 sowie lokalen Paper-/Folia-26.3-Servern. Zwei getrennte echte Spielclients: BTManualAlice und BTManualBob. Test-Mixins lesen den Zustand mit; sie bedienen keine Oberfläche und sind nicht in den Release-JARs enthalten. [Messwerte und Einzelfälle](MANUAL_TESTS.json).

| Test | Ergebnis |
| --- | --- |
| Echte Hardcore-Welt, Cheats aus; Vanilla-Cheat abgewiesen, BetterTimer-Menü/Commands nutzbar | PASS |
| Stoppuhr über Start-/Pauseknopf und Countdown mit großer Ablaufmeldung | PASS |
| Drag-and-drop, Speichern, erneutes Öffnen, Abbrechen und Escape | PASS · 26.1 |
| Beide Bildschirmränder, Position nach Clientneustart, GUI-Skalierung 2 und 3 | PASS |
| Zwei Spieler: gemeinsamer Timer, OP-Rechte, schreibgeschützter Teamteilnehmer | PASS · Paper 26.3 |
| Teameigentümer ohne OP, Teamlöschung/Fallback und getrennte private Timer | PASS |
| Zwei Spieler in mehr als 14.000 Blöcke entfernten Folia-Regionen, Timer und Inventarmenü | PASS · Folia 26.3 Build 3 ALPHA |
| Countdown versetzt beide Spieler in den Zuschauermodus | PASS |
| Vollständiger Server- und Clientneustart, erneuter Beitritt, gleicher gespeicherter Endstand | PASS |
| 16 Standardfarben, Regenbogen und eigene Hexfarbe | PASS |
| DE/EN/FR/ES, sofortiger Sprachwechsel mit Überschrift, Auto-Fallback bei niederländischem Minecraft auf Englisch | PASS |
| Negative Dauer erzeugt Fehlermeldung ohne Timeränderung; F1 blendet HUD aus | PASS |
| Inventar: Zifferntaste, Shiftklick, Doppelklick auf einen Menüslot | TEILWEISE · Inventar und Mausgegenstand unverändert; weitere Slots/Drag-/Drop-Varianten offen |
| Folia-Challenges: echte Laufbewegung blockiert/freigegeben, Blindheit sichtbar, Gesundheit mit Schutz erhalten | TEILWEISE · isolierter Vergleich desselben Schadensereignisses offen; Kontrolllauf wurde zusätzlich durch Mobs beeinflusst |

Im Spiel gefunden und korrigiert:

- Die frühere Standardtaste O kollidiert in 26.3 mit der Freunde-Funktion. Neue Installationen verwenden B. Bereits gespeicherte eigene Belegungen werden nicht überschrieben.
- Sehr schnelle Ziehgesten konnten zwischen zwei Bildern enden. Der Editor übernimmt jetzt auch beim linken Loslassen die endgültige Position. Dieselbe native Geste, die vorher wirkungslos blieb, verschiebt den Timer nach der Korrektur korrekt.
- Beim Sprachwechsel blieb die Überschrift zunächst in der alten Sprache. Überschriften werden jetzt dynamisch übersetzt; Französisch, Spanisch und zurück zur Spielsprache wurden erneut visuell geprüft.

Die CUA-Steuerung liefert ihre Drag-Geste hier in ungefähr zwei Millisekunden. Damit wurden echte native Drücken-/Loslassen-Ereignisse mit unterschiedlichen Koordinaten geprüft. Ein langsamer, kontinuierlicher Ziehvorgang über viele Bilder bleibt eine zusätzliche manuelle Prüfmöglichkeit. Unter 26.3 lieferte die Fernsteuerung zeitweise keine Mausbewegung an Minecraft; der abgeschlossene Zieh-/Speichern-/Abbrechen-Test erfolgte deshalb unter 26.1 mit dem gemeinsamen Editorcode.

Die 50 Szenarien in [COMPATIBILITY_TESTS.json](COMPATIBILITY_TESTS.json) sind die vorherige Loader-/Server-Baseline mit ihren damaligen JAR-Prüfsummen. Nach den drei UI-Korrekturen wurden alle Module erneut gebaut und die Konfigurationsansichten mit den finalen JARs für 26.1, 26.2 und 26.3 zusätzlich nachgetestet; die jeweiligen Nachweise stehen im JSON-Bericht. Die 50 Baseline-Szenarien wurden dabei nicht erneut vollständig ausgeführt.

Offen bleiben insbesondere vollständige Inventarvarianten, Spieler ohne Client-Mod, andere HUD-Mods/große Modpacks, sämtliche Challenge-Ereignisse sowie manuelle Bedienung jeder Loader-/Patchkombination und Windows/Linux. Die erledigten Tests sind keine pauschale Garantie für jede mögliche Umgebung.
