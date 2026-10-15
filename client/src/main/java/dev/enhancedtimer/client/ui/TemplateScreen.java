package dev.enhancedtimer.client.ui;

import dev.enhancedtimer.client.*;
import dev.enhancedtimer.core.TimeFormat;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;

public final class TemplateScreen extends EnhancedScreen {
    private int state;
    public TemplateScreen(Screen parent) { super(parent, "templates"); }
    @Override protected void init() {
        super.init(); var s = client.settings;
        row("state", I18n.text(state == 0 ? "state.running" : state == 1 ? "state.paused" : "state.finished"), 48, () -> { state = (state + 1) % 3; rebuildWidgets(); });
        EditBox pattern = field(88, state == 0 ? s.template : state == 1 ? s.pausedTemplate : s.finishedTemplate, "format");
        EditBox prefix = field(128, state == 0 ? s.prefix : state == 1 ? s.pausedPrefix : s.finishedPrefix, "prefix");
        EditBox suffix = field(168, state == 0 ? s.suffix : state == 1 ? s.pausedSuffix : s.finishedSuffix, "suffix");
        button(I18n.text("apply"), left, top + 194, contentWidth, () -> {
            if (state == 0) { s.template = pattern.getValue(); s.prefix = prefix.getValue(); s.suffix = suffix.getValue(); }
            else if (state == 1) { s.pausedTemplate = pattern.getValue(); s.pausedPrefix = prefix.getValue(); s.pausedSuffix = suffix.getValue(); }
            else { s.finishedTemplate = pattern.getValue(); s.finishedPrefix = prefix.getValue(); s.finishedSuffix = suffix.getValue(); }
            s.format = TimeFormat.TEMPLATE; client.save();
        });
        button(I18n.text("done"), width / 2 - 50, Math.min(height - 24, top + 218), 100, this::onClose);
    }
    private EditBox field(int y, String text, String label) {
        var field = addRenderableWidget(new EditBox(font, left, top + y, contentWidth, 20, I18n.component(label)));
        field.setMaxLength(160); field.setValue(text); return field;
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) {
        heading(g);
        g.text(font, "{d} {H} {hh} {mm} {ss} {ms}", left, top + 74, 0xFFAAAAAA, true);
        g.text(font, I18n.text("prefix"), left, top + 114, 0xFFAAAAAA, true);
        g.text(font, I18n.text("suffix"), left, top + 154, 0xFFAAAAAA, true);
        super.extractRenderState(g, x, y, delta);
    }
}
