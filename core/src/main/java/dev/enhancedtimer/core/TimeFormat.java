package dev.enhancedtimer.core;

import java.util.Locale;

public enum TimeFormat {
    CLOCK, COMPACT, PRECISE, DAYS, AUTO, TEMPLATE;

    public String format(long millis, boolean countdown) {
        return format(millis, countdown, "{d}d {h}h {m}m {s}s");
    }

    public String format(long millis, boolean countdown, String template) {
        millis = Math.max(0, millis);
        long seconds = countdown ? (millis + 999) / 1000 : millis / 1000;
        long hours = seconds / 3600;
        long minutes = seconds / 60 % 60;
        long secs = seconds % 60;
        return switch (this) {
            case CLOCK -> String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, secs);
            case COMPACT -> hours > 0 ? String.format(Locale.ROOT, "%dh %02dm %02ds", hours, minutes, secs)
                    : String.format(Locale.ROOT, "%dm %02ds", seconds / 60, secs);
            case PRECISE -> String.format(Locale.ROOT, "%02d:%02d:%02d.%03d", millis / 3_600_000,
                    millis / 60_000 % 60, millis / 1000 % 60, millis % 1000);
            case DAYS -> String.format(Locale.ROOT, "%dd %02d:%02d:%02d", seconds / 86400, hours % 24, minutes, secs);
            case AUTO -> auto(seconds);
            case TEMPLATE -> template(template, seconds, millis);
        };
    }
    private static String auto(long seconds) {
        var parts = new java.util.ArrayList<String>();
        if (seconds / 86400 > 0) parts.add(seconds / 86400 + "d");
        if (seconds / 3600 % 24 > 0) parts.add(seconds / 3600 % 24 + "h");
        if (seconds / 60 % 60 > 0) parts.add(seconds / 60 % 60 + "m");
        if (seconds % 60 > 0 || parts.isEmpty()) parts.add(seconds % 60 + "s");
        return String.join(" ", parts);
    }

    private static String template(String pattern, long seconds, long millis) {
        if (pattern == null || pattern.length() > 160) pattern = "{H}:{mm}:{ss}";
        // Optional unit groups: [?d:{d} days ] hides both the value and its surrounding text at zero.
        var groups = java.util.regex.Pattern.compile("\\[\\?([dHhms]):([^\\[\\]]*)]").matcher(pattern);
        var conditional = new StringBuilder();
        while (groups.find()) {
            long value = switch (groups.group(1)) {
                case "d" -> seconds / 86400; case "H" -> seconds / 3600; case "h" -> seconds / 3600 % 24;
                case "m" -> seconds / 60 % 60; default -> seconds % 60;
            };
            groups.appendReplacement(conditional, java.util.regex.Matcher.quoteReplacement(value == 0 ? "" : groups.group(2)));
        }
        pattern = groups.appendTail(conditional).toString();
        return pattern.replace("{d}", Long.toString(seconds / 86400))
                .replace("{H}", Long.toString(seconds / 3600)).replace("{h}", Long.toString(seconds / 3600 % 24))
                .replace("{m}", Long.toString(seconds / 60 % 60)).replace("{s}", Long.toString(seconds % 60))
                .replace("{hh}", String.format(Locale.ROOT, "%02d", seconds / 3600 % 24))
                .replace("{mm}", String.format(Locale.ROOT, "%02d", seconds / 60 % 60))
                .replace("{ss}", String.format(Locale.ROOT, "%02d", seconds % 60))
                .replace("{ms}", String.format(Locale.ROOT, "%03d", millis % 1000));
    }
}

