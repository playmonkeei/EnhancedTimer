package dev.enhancedtimer.client.ui;

import dev.enhancedtimer.client.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;

public final class DesignScreen extends EnhancedScreen {
    private int preset;
    public DesignScreen(Screen parent) { super(parent, "designs"); }
    @Override protected void init() {
        super.init();
        var name = addRenderableWidget(new EditBox(font, left, top + 64, contentWidth, 20, I18n.component("name")));
        name.setMaxLength(40); name.setValue("my-design");
        button(I18n.text("export"), left, top + 100, contentWidth, () -> apply("export", name.getValue()));
        button(I18n.text("import"), left, top + 126, contentWidth, () -> apply("import", name.getValue()));
        button(I18n.text("design.presets"), left, top + 152, contentWidth, () -> {
            var s = client.settings; s.resetAppearance();
            switch (preset++ % 4) {
                case 1 -> s.colorMode = dev.enhancedtimer.core.TimerColor.WHITE;
                case 2 -> { s.colorMode = dev.enhancedtimer.core.TimerColor.RAINBOW; s.stateColors = false; }
                case 3 -> { s.format = dev.enhancedtimer.core.TimeFormat.TEMPLATE; s.template = "<bold><gradient:#ffaa00:#ff55ff:{phase}>{d}d {hh}:{mm}:{ss}</gradient></bold>"; }
            }
            client.save();
        });
        button(I18n.text("design.library"), left, top + 188, contentWidth, () -> Platform.screen(new DesignLibraryScreen(this)));
        button(I18n.text("done"), width / 2 - 50, Math.min(height - 24, top + 214), 100, this::onClose);
    }
    private void apply(String action, String name) { try { client.design(action, name); error = ""; } catch (RuntimeException e) { error = I18n.text("error.file"); } }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) {
        heading(g); g.text(font, I18n.text("name"), left, top + 50, 0xFFAAAAAA, true);
        super.extractRenderState(g, x, y, delta);
    }
}
