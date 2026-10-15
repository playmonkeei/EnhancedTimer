package dev.enhancedtimer.client.ui;

import dev.enhancedtimer.client.*;
import dev.enhancedtimer.core.Durations;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;

public final class SetTimeScreen extends EnhancedScreen {
    private String error = "";
    public SetTimeScreen(Screen parent) { super(parent, "set_time"); }
    @Override protected void init() {
        super.init();
        var value = addRenderableWidget(new EditBox(font, left, top + 74, contentWidth, 20, I18n.component("duration")));
        value.setMaxLength(40); value.setValue("00:00:00");
        button(I18n.text("apply"), left, top + 108, contentWidth, () -> {
            try { client.setDisplay(value.getValue().equals("0") || value.getValue().equals("00:00:00") ? 0 : Durations.parse(value.getValue())); onClose(); }
            catch (IllegalArgumentException e) { error = I18n.text("error.duration"); }
        });
        button(I18n.text("done"), width / 2 - 50, Math.min(height - 24, top + 214), 100, this::onClose);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) {
        heading(g); g.text(font, error, left, top + 146, 0xFFFF5555, true); super.extractRenderState(g, x, y, delta);
    }
}
