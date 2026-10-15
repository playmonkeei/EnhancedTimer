package dev.enhancedtimer.core;

/** Minecraft's sixteen text colors, plus a saved custom color and an animated gradient. */
public enum TimerColor {
    BLACK(0x000000), DARK_BLUE(0x0000AA), DARK_GREEN(0x00AA00), DARK_AQUA(0x00AAAA),
    DARK_RED(0xAA0000), DARK_PURPLE(0xAA00AA), GOLD(0xFFAA00), GRAY(0xAAAAAA),
    DARK_GRAY(0x555555), BLUE(0x5555FF), GREEN(0x55FF55), AQUA(0x55FFFF),
    RED(0xFF5555), LIGHT_PURPLE(0xFF55FF), YELLOW(0xFFFF55), WHITE(0xFFFFFF),
    RAINBOW(0), CUSTOM(0);

    private static final long RAINBOW_PERIOD = 8_000_000_000L;
    private final int rgb;
    TimerColor(int rgb) { this.rgb = rgb; }

    public int argb(int customColor, long animationNanos, int characterIndex) {
        if (this == CUSTOM) return customColor | 0xFF000000;
        if (this != RAINBOW) return rgb | 0xFF000000;
        double phase = Math.floorMod(animationNanos, RAINBOW_PERIOD) / (double)RAINBOW_PERIOD;
        double hue = phase + characterIndex / 12.0;
        hue = (hue - Math.floor(hue)) * 6;
        int sector = (int)hue;
        int rising = (int)Math.round((hue - sector) * 255);
        int falling = 255 - rising;
        int color = switch (sector) {
            case 0 -> 0xFF0000 | rising << 8;
            case 1 -> falling << 16 | 0xFF00;
            case 2 -> 0xFF00 | rising;
            case 3 -> falling << 8 | 0xFF;
            case 4 -> rising << 16 | 0xFF;
            default -> 0xFF0000 | falling;
        };
        return 0xFF000000 | color;
    }

    public static TimerColor fromLegacyColor(int color) {
        for (TimerColor value : values())
            if (value != RAINBOW && value != CUSTOM && value.rgb == (color & 0xFFFFFF)) return value;
        return CUSTOM;
    }
}
