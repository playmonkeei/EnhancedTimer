package dev.enhancedtimer.client.ui;

import dev.enhancedtimer.client.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

public final class PreferencesScreen extends EnhancedScreen {
    private int page;
    public PreferencesScreen(Screen parent) { super(parent, "settings"); }
    @Override protected void init() {
        super.init();
        var s = client.settings;
        if (page == 0) {
            row("language", I18n.languageName(s.language), 48, () -> {
                s.language = I18n.CODES.get((I18n.CODES.indexOf(s.language) + 1) % I18n.CODES.size()); rebuildWidgets();
            });
            row("visible", onOff(s.visible), 74, () -> { s.visible = !s.visible; rebuildWidgets(); });
            row("show_label", onOff(s.showLabel), 100, () -> { s.showLabel = !s.showLabel; rebuildWidgets(); });
            row("shadow", onOff(s.shadow), 126, () -> { s.shadow = !s.shadow; rebuildWidgets(); });
            row("sound", onOff(s.alertSound), 152, () -> { s.alertSound = !s.alertSound; rebuildWidgets(); });
        } else if (page == 1) {
            row("pause_game", onOff(s.pauseWithGame), 48, () -> { s.pauseWithGame = !s.pauseWithGame; rebuildWidgets(); });
            row("pause_menus", onOff(s.pauseInMenus), 74, () -> { s.pauseInMenus = !s.pauseInMenus; rebuildWidgets(); });
            button(I18n.text("set_time"), left, top + 100, contentWidth, () -> Platform.screen(new SetTimeScreen(this))).active = client.inWorld() && client.canControl();
            button(I18n.text("reset"), left, top + 126, contentWidth, () -> client.action("reset"));
            button(I18n.text("reset_layout"), left, top + 152, contentWidth, () -> {
                s.resetAppearance();
            });
        } else {
            button(I18n.text("advanced"), left, top + 48, contentWidth, () -> Platform.screen(new AdvancedScreen(this)));
            button(I18n.text("source"), left, top + 74, contentWidth, () -> Platform.screen(new SourceScreen(this)));
            button(I18n.text("splits"), left, top + 100, contentWidth, () -> Platform.screen(new SplitsScreen(this)));
            button(I18n.text("designs"), left, top + 126, contentWidth, () -> Platform.screen(new DesignScreen(this)));
            button(I18n.text("undo"), left, top + 152, contentWidth, () -> client.action("undo"));
        }
        button(I18n.text("next") + " (" + (page + 1) + "/3)", left, top + 188, contentWidth, () -> { page = (page + 1) % 3; rebuildWidgets(); });
        button(I18n.text("done"), width / 2 - 50, Math.min(height - 24, top + 214), 100, this::onClose);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) {
        heading(g);
        super.extractRenderState(g, x, y, delta);
    }
}

