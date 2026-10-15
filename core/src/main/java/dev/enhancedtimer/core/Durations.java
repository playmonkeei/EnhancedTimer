package dev.enhancedtimer.core;

import java.util.regex.Pattern;

public final class Durations {
    private static final Pattern UNITS = Pattern.compile("(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?");
    private Durations() {}

    /** Accepts 2h30m, 90m, seconds, MM:SS or HH:MM:SS. */
    public static long parse(String input) {
        try {
            String text = input.strip().toLowerCase(java.util.Locale.ROOT);
            long seconds;
            if (text.matches("\\d+")) seconds = Long.parseLong(text);
            else if (text.contains(":")) {
                String[] parts = text.split(":", -1);
                if (parts.length < 2 || parts.length > 3) throw new IllegalArgumentException();
                seconds = 0;
                for (int i = 0; i < parts.length; i++) {
                    if (!parts[i].matches("\\d+")) throw new IllegalArgumentException();
                    long part = Long.parseLong(parts[i]);
                    if (i > 0 && part >= 60) throw new IllegalArgumentException();
                    seconds = Math.addExact(Math.multiplyExact(seconds, 60), part);
                }
            } else {
                var match = UNITS.matcher(text);
                if (text.isEmpty() || !match.matches()) throw new IllegalArgumentException();
                seconds = 0;
                int[] units = {86400, 3600, 60, 1};
                for (int i = 1; i <= 4; i++) if (match.group(i) != null)
                    seconds = Math.addExact(seconds, Math.multiplyExact(Long.parseLong(match.group(i)), units[i - 1]));
            }
            long millis = Math.multiplyExact(seconds, 1000);
            if (millis <= 0 || millis > TimerEngine.MAX_DURATION) throw new IllegalArgumentException();
            return millis;
        } catch (ArithmeticException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Use 2d, 2h, 90m, 30s or HH:MM:SS (maximum 365 days)");
        }
    }
}

