package dev.enhancedtimer.core;

public final class HudPosition {
    public enum Anchor { TOP_LEFT, TOP_CENTER, TOP_RIGHT, CENTER_LEFT, CENTER, CENTER_RIGHT,
        BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT, CUSTOM }
    public record Point(int x, int y) {}
    private HudPosition() {}
    public static Point place(Anchor anchor, double x, double y, int screenWidth, int screenHeight, int width, int height) {
        int availableX = Math.max(0, screenWidth - width);
        int availableY = Math.max(0, screenHeight - height);
        if (anchor != Anchor.CUSTOM) {
            x = (anchor.ordinal() % 3) / 2.0;
            y = (anchor.ordinal() / 3) / 2.0;
            int margin = 8;
            return new Point(clamp((int) Math.round(margin + x * (availableX - 2 * margin)), availableX),
                    clamp((int) Math.round(margin + y * (availableY - 2 * margin)), availableY));
        }
        return new Point(clamp((int) Math.round(finite(x) * availableX), availableX),
                clamp((int) Math.round(finite(y) * availableY), availableY));
    }
    public static double normalize(double position, int screen, int size) {
        return Math.max(0, Math.min(1, position / Math.max(1, screen - size)));
    }
    private static double finite(double value) { return Double.isFinite(value) ? Math.max(0, Math.min(1, value)) : 0.5; }
    private static int clamp(int value, int max) { return Math.max(0, Math.min(max, value)); }
}
