package dev.enhancedtimer.client.ui;

import dev.enhancedtimer.client.*;
import dev.enhancedtimer.core.*;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class HudRenderer {
    public static long expiredUntil;
    public static long noticeUntil;
    public static String notice = "";
    public record Bounds(int x, int y, int width, int height) {
        public boolean contains(double px, double py) { return px >= x && py >= y && px < x + width && py < y + height; }
    }
    private HudRenderer() {}
    private record Content(String time, String label, int width, int height, int padding) {}
    private static Content content() {
        var s = EnhancedTimerClient.get().settings;
        var font = Minecraft.getInstance().font;
        String time = time(), label = s.showLabel ? label() : "";
        int padding = (s.background >>> 24) == 0 ? 0 : 2;
        int width = Math.max(TimerText.width(font, time), font.width(label)) + padding * 2;
        int height = font.lineHeight + (s.showLabel ? font.lineHeight + 2 : 0) + padding * 2;
        return new Content(time, label, width, height, padding);
    }
    public static Bounds bounds(int screenWidth, int screenHeight) {
        return bounds(screenWidth, screenHeight, content());
    }
    private static Bounds bounds(int screenWidth, int screenHeight, Content content) {
        var s = EnhancedTimerClient.get().settings;
        int w = Math.max(1, (int)Math.ceil(content.width * s.scale));
        int h = Math.max(1, (int)Math.ceil(content.height * s.scale));
        // Fit even on very small windows and at large GUI scales.
        double fit = Math.min(1, Math.min(screenWidth / (double)Math.max(1, w), screenHeight / (double)Math.max(1, h)));
        w = (int)Math.floor(w * fit); h = (int)Math.floor(h * fit);
        // Vanilla's action-bar area: keep bottom presets above the hotbar, health and XP.
        boolean bottom = s.anchor == HudPosition.Anchor.BOTTOM_LEFT || s.anchor == HudPosition.Anchor.BOTTOM_CENTER
                || s.anchor == HudPosition.Anchor.BOTTOM_RIGHT;
        int anchorHeight = bottom ? Math.max(h, screenHeight - 52) : screenHeight;
        var pos = HudPosition.place(s.anchor, s.x, s.y, screenWidth, anchorHeight, w, h);
        return new Bounds(pos.x(), pos.y(), w, h);
    }
    public static String time() {
        var c = EnhancedTimerClient.get();
        var timer = c.activeTimer(); var s = c.settings;
        if (s.displayClock.equals("REAL")) return java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        if (s.displayClock.equals("GAME") && Minecraft.getInstance().level != null) {
            long ticks = Minecraft.getInstance().level.getOverworldClockTime();
            return TimeFormat.CLOCK.format(Math.floorMod(ticks + 6000, 24000) * 3600, false);
        }
        boolean paused = timer.state() == TimerEngine.State.PAUSED || timer.state() == TimerEngine.State.STOPPED
                || timer.state() == TimerEngine.State.RUNNING && timer.suspended();
        boolean finished = timer.state() == TimerEngine.State.FINISHED;
        String pattern = finished ? s.finishedTemplate : paused ? s.pausedTemplate : s.template;
        return (finished ? s.finishedPrefix : paused ? s.pausedPrefix : s.prefix)
                + s.format.format(timer.displayMillis(), timer.mode() == TimerEngine.Mode.COUNTDOWN, pattern)
                + (finished ? s.finishedSuffix : paused ? s.pausedSuffix : s.suffix);
    }
    private static String label() {
        var timer = EnhancedTimerClient.get().activeTimer();
        return I18n.text("mode." + timer.mode().name().toLowerCase(Locale.ROOT)) + " · "
                + I18n.text(timer.suspended() && timer.state() == TimerEngine.State.RUNNING ? "state.paused" : "state." + timer.state().name().toLowerCase(Locale.ROOT));
    }
    public static Bounds draw(GuiGraphicsExtractor g, boolean editing) {
        var s = EnhancedTimerClient.get().settings;
        var font = Minecraft.getInstance().font;
        Content content = content();
        Bounds b = bounds(g.guiWidth(), g.guiHeight(), content);
        float scale = Math.min(s.scale, Math.min(b.width / (float)Math.max(1, content.width),
                b.height / (float)Math.max(1, content.height)));
        g.pose().pushMatrix();
        g.pose().translate(b.x, b.y);
        g.pose().scale(scale, scale);
        int width = Math.round(b.width / scale), height = Math.round(b.height / scale);
        if ((s.background >>> 24) != 0) g.fill(0, 0, width, height, s.background);
        if (s.showLabel) g.text(font, content.label, content.padding, content.padding, 0xFFAAAAAA, s.shadow);
        TimerText.draw(g, font, content.time, content.padding,
                content.padding + (s.showLabel ? font.lineHeight + 2 : 0), s.shadow);
        if (editing) {
            g.fill(-1, -1, width + 1, 0, 0xFFFFFFFF); g.fill(-1, height, width + 1, height + 1, 0xFFFFFFFF);
            g.fill(-1, 0, 0, height, 0xFFFFFFFF); g.fill(width, 0, width + 1, height, 0xFFFFFFFF);
        }
        g.pose().popMatrix();
        return b;
    }
    public static void expired(GuiGraphicsExtractor g) {
        var font = Minecraft.getInstance().font;
        String text = I18n.text("countdown.expired");
        float scale = Math.min(3f, (g.guiWidth() - 20f) / Math.max(1, font.width(text)));
        g.pose().pushMatrix();
        g.pose().translate(g.guiWidth() / 2f, g.guiHeight() / 2f - 4 * scale);
        g.pose().scale(scale, scale);
        g.text(font, text, -font.width(text) / 2, 0, 0xFFFF5555, true);
        g.pose().popMatrix();
    }
    public static void notice(GuiGraphicsExtractor g) {
        var font = Minecraft.getInstance().font;
        g.text(font, notice, g.guiWidth() / 2 - font.width(notice) / 2, Math.max(8, g.guiHeight() / 2 - 24), 0xFFFFFF55, true);
    }
}
