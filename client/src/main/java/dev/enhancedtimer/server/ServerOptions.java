package dev.enhancedtimer.server;

import dev.enhancedtimer.core.*;

/** Stored per modded server world, independently of each player's HUD preferences. */
public final class ServerOptions {
    public boolean pauseWhenEmpty = true;
    public boolean bossbar = true;
    public String display = "BOSSBAR";
    public boolean sound = true;
    public String format = "{d}d {hh}:{mm}:{ss}";
    public String pausedFormat = "{d}d {hh}:{mm}:{ss}";
    public String finishedFormat = "{d}d {hh}:{mm}:{ss}";
    public TimerColor color = TimerColor.GOLD;
    public int customColor = 0xFFAA00;
    public String clock = "TIMER";
    public long[] warningMillis = {60_000, 10_000, 5_000};
    public void sanitize() {
        if (format == null || format.length() > 160) format = "{d}d {hh}:{mm}:{ss}";
        if (pausedFormat == null || pausedFormat.length() > 160) pausedFormat = format;
        if (finishedFormat == null || finishedFormat.length() > 160) finishedFormat = format;
        if (color == null) color = TimerColor.GOLD;
        if (!java.util.Set.of("BOSSBAR", "ACTIONBAR").contains(display == null ? "" : display)) display = "BOSSBAR";
        if (!java.util.Set.of("TIMER", "REAL", "GAME").contains(clock == null ? "" : clock)) clock = "TIMER";
        if (warningMillis == null) warningMillis = new long[]{60_000, 10_000, 5_000};
        warningMillis = java.util.Arrays.stream(warningMillis).filter(n -> n > 0 && n <= TimerEngine.MAX_DURATION).distinct().limit(16).toArray();
    }
}
