package dev.enhancedtimer.client;

import dev.enhancedtimer.core.HudPosition;
import dev.enhancedtimer.core.TimeFormat;
import dev.enhancedtimer.core.TimerEngine;
import dev.enhancedtimer.core.TimerColor;

public final class Settings {
    public int schemaVersion = 4;
    public String language = "auto";
    public boolean visible = true;
    public boolean shadow = true;
    public boolean showLabel = false;
    public boolean pauseWithGame = true;
    public boolean pauseInMenus = false;
    public boolean alertSound = true;
    public float scale = 1;
    public int color = 0xFFFFAA00;
    public TimerColor colorMode = TimerColor.GOLD;
    public int background = 0;
    public HudPosition.Anchor anchor = HudPosition.Anchor.BOTTOM_CENTER;
    public double x = 0.5;
    public double y = 0.1;
    public TimeFormat format = TimeFormat.CLOCK;
    public TimerEngine.Mode defaultMode = TimerEngine.Mode.STOPWATCH;
    public long durationMillis = 7_200_000;
    public boolean warnings = true;
    public long[] warningMillis = {60_000, 10_000, 5_000};
    public boolean stateColors = true;
    public int pausedColor = 0xFFAAAAAA;
    public int finishedColor = 0xFFFF5555;
    public String alertTone = "ORB";
    public String template = "{d}d {hh}:{mm}:{ss}";
    public String pausedTemplate = "{d}d {hh}:{mm}:{ss}";
    public String finishedTemplate = "{d}d {hh}:{mm}:{ss}";
    public String prefix = "", suffix = "";
    public String pausedPrefix = "", pausedSuffix = "";
    public String finishedPrefix = "", finishedSuffix = "";
    public boolean startOnMove = false;
    public boolean pauseOnDeath = false;
    public boolean stopOnDragon = false;
    public boolean splitMilestones = true;
    public String displayClock = "TIMER";
    public boolean serverHud = false;
    public String designLibraryUrl = "";

    public void sanitize() {
        if (schemaVersion < 2) {
            // Replace the original defaults; keep independently customized appearance and coordinates.
            if (color == 0xFF65E0BA) color = 0xFFFFAA00;
            if (background == 0x990C141B) background = 0;
            if (scale == 1.5f) scale = 1;
            showLabel = false;
            if (anchor == HudPosition.Anchor.TOP_CENTER) anchor = HudPosition.Anchor.BOTTOM_CENTER;
            schemaVersion = 2;
        }
        if (schemaVersion < 3) {
            colorMode = TimerColor.fromLegacyColor(color);
            schemaVersion = 3;
        }
        if (colorMode == null) colorMode = TimerColor.fromLegacyColor(color);
        schemaVersion = Math.max(4, schemaVersion);
        if (warningMillis == null) warningMillis = new long[]{60_000, 10_000, 5_000};
        warningMillis = java.util.Arrays.stream(warningMillis).filter(v -> v > 0 && v <= TimerEngine.MAX_DURATION).distinct().sorted().limit(16).toArray();
        if (!java.util.Set.of("ORB", "BELL", "CHIME", "LEVELUP", "NONE").contains(alertTone == null ? "" : alertTone)) alertTone = "ORB";
        if (!java.util.Set.of("TIMER", "REAL", "GAME").contains(displayClock == null ? "" : displayClock)) displayClock = "TIMER";
        template = bounded(template, "{d}d {hh}:{mm}:{ss}");
        if (designLibraryUrl == null || designLibraryUrl.length() > 2048) designLibraryUrl = "";
        pausedTemplate = bounded(pausedTemplate, template); finishedTemplate = bounded(finishedTemplate, template);
        prefix = bounded(prefix, ""); suffix = bounded(suffix, "");
        pausedPrefix = bounded(pausedPrefix, ""); pausedSuffix = bounded(pausedSuffix, "");
        finishedPrefix = bounded(finishedPrefix, ""); finishedSuffix = bounded(finishedSuffix, "");
        pausedColor |= 0xFF000000; finishedColor |= 0xFF000000;
        if (!java.util.Set.of("auto", "en_us", "de_de", "fr_fr", "es_es").contains(language == null ? "" : language)) language = "auto";
        if (!Float.isFinite(scale)) scale = 1;
        scale = Math.max(0.5f, Math.min(4, scale));
        x = Double.isFinite(x) ? Math.max(0, Math.min(1, x)) : 0.5;
        y = Double.isFinite(y) ? Math.max(0, Math.min(1, y)) : 0.1;
        if (anchor == null) anchor = HudPosition.Anchor.BOTTOM_CENTER;
        if (format == null) format = TimeFormat.CLOCK;
        if (defaultMode == null) defaultMode = TimerEngine.Mode.STOPWATCH;
        durationMillis = Math.max(1000, Math.min(TimerEngine.MAX_DURATION, durationMillis));
        color |= 0xFF000000;
    }

    private static String bounded(String value, String fallback) { return value == null || value.length() > 160 ? fallback : value; }
    public Integer stateColor(TimerEngine timer) {
        if (!stateColors || !displayClock.equals("TIMER")) return null;
        var state = timer.state();
        if (state == TimerEngine.State.FINISHED) return finishedColor;
        if (state == TimerEngine.State.PAUSED || state == TimerEngine.State.STOPPED
                || state == TimerEngine.State.RUNNING && timer.suspended()) return pausedColor;
        if (state == TimerEngine.State.RUNNING && warnings && timer.mode() == TimerEngine.Mode.COUNTDOWN && timer.displayMillis() <= 10_000)
            return finishedColor;
        return null;
    }
    public dev.enhancedtimer.core.TimerSession.Rules rules() {
        return new dev.enhancedtimer.core.TimerSession.Rules(startOnMove, pauseOnDeath, stopOnDragon, splitMilestones, false, false, false, false, false);
    }
    public void applyDesign(Settings design) {
        design.sanitize();
        color = design.color; colorMode = design.colorMode; background = design.background; scale = design.scale;
        format = design.format; anchor = design.anchor; x = design.x; y = design.y; shadow = design.shadow;
        showLabel = design.showLabel; stateColors = design.stateColors; pausedColor = design.pausedColor; finishedColor = design.finishedColor;
        template = design.template; pausedTemplate = design.pausedTemplate; finishedTemplate = design.finishedTemplate;
        prefix = design.prefix; suffix = design.suffix; pausedPrefix = design.pausedPrefix; pausedSuffix = design.pausedSuffix;
        finishedPrefix = design.finishedPrefix; finishedSuffix = design.finishedSuffix;
    }

    public void resetAppearance() {
        anchor = HudPosition.Anchor.BOTTOM_CENTER;
        scale = 1;
        color = 0xFFFFAA00;
        colorMode = TimerColor.GOLD;
        background = 0;
        format = TimeFormat.CLOCK;
        showLabel = false;
        shadow = true;
        stateColors = true; pausedColor = 0xFFAAAAAA; finishedColor = 0xFFFF5555;
        template = pausedTemplate = finishedTemplate = "{d}d {hh}:{mm}:{ss}";
        prefix = suffix = pausedPrefix = pausedSuffix = finishedPrefix = finishedSuffix = "";
        x = 0.5; y = 0.1;
    }
}

