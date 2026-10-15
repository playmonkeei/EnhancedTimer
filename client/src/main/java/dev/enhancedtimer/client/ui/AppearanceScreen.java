package dev.enhancedtimer.client.ui;

import dev.enhancedtimer.client.*;
import dev.enhancedtimer.core.*;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

public final class AppearanceScreen extends EnhancedScreen {
    public AppearanceScreen(Screen parent) { super(parent, "appearance"); }
    @Override protected void init() {
        super.init();
        var s = client.settings;
        row("format", I18n.text("format." + s.format.name().toLowerCase(Locale.ROOT)), 48, () -> {
            s.format = TimeFormat.values()[(s.format.ordinal() + 1) % TimeFormat.values().length]; rebuildWidgets();
        });
        row("position", I18n.text("anchor." + s.anchor.name().toLowerCase(Locale.ROOT)), 74, () -> {
            s.anchor = HudPosition.Anchor.values()[(s.anchor.ordinal() + 1) % HudPosition.Anchor.values().length]; rebuildWidgets();
        });
        row("scale", String.format(Locale.ROOT, "%.2fx", s.scale), 100, () -> {
            s.scale = s.scale >= 3 ? 0.75f : s.scale + 0.25f; rebuildWidgets();
        });
        row("color", I18n.text("color." + s.colorMode.name().toLowerCase(Locale.ROOT)), 130,
                () -> Platform.screen(new ColorScreen(this)));
        row("background", (s.background >>> 24) * 100 / 255 + "%", 156, () -> {
            int alpha = s.background >>> 24; alpha = alpha >= 240 ? 0 : Math.min(255, alpha + 51);
            s.background = (alpha << 24) | (s.background & 0xFFFFFF); rebuildWidgets();
        });
        button(I18n.text("editor"), left, top + 188, contentWidth, () -> Platform.screen(new LayoutScreen(this)));
        button(I18n.text("done"), width / 2 - 50, Math.min(height - 24, top + 214), 100, this::onClose);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) {
        heading(g);
        super.extractRenderState(g, x, y, delta);
    }
}

