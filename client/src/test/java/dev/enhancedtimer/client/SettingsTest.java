package dev.enhancedtimer.client;

import dev.enhancedtimer.core.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SettingsTest {
    @Test void everyTimerStateSelectsColorsWithoutUnboxingMissingOverride() {
        var settings = new Settings(); var timer = new TimerEngine(() -> 0L);
        for (var state : TimerEngine.State.values()) {
            timer.synchronize(new TimerEngine.Snapshot(TimerEngine.Mode.STOPWATCH, state, 0, 60_000), false);
            var actual = settings.stateColor(timer);
            switch (state) {
                case READY, RUNNING -> assertNull(actual);
                case PAUSED, STOPPED -> assertEquals(settings.pausedColor, actual);
                case FINISHED -> assertEquals(settings.finishedColor, actual);
            }
        }
        timer.configure(TimerEngine.Mode.COUNTDOWN, 5000); assertNull(settings.stateColor(timer));
        timer.start(); assertEquals(settings.finishedColor, settings.stateColor(timer));
        timer.suspend(true); assertEquals(settings.pausedColor, settings.stateColor(timer));
        settings.displayClock = "REAL"; assertNull(settings.stateColor(timer));
    }
    @Test void importedDesignCannotChangeLanguageRulesOrServerSelection() {
        var settings = new Settings(); var imported = new Settings();
        imported.language = "fr_fr"; imported.serverHud = true; imported.startOnMove = true; imported.durationMillis = 1000;
        imported.designLibraryUrl = "https://example.com/catalog.json"; imported.colorMode = TimerColor.RAINBOW;
        settings.applyDesign(imported);
        assertEquals(TimerColor.RAINBOW, settings.colorMode); assertEquals("auto", settings.language);
        assertFalse(settings.serverHud); assertFalse(settings.startOnMove); assertEquals(7_200_000, settings.durationMillis);
        assertEquals("", settings.designLibraryUrl);
    }
}
