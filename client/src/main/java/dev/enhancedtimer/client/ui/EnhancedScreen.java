package dev.enhancedtimer.client.ui;

import dev.enhancedtimer.client.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

abstract class EnhancedScreen extends Screen {
    protected final Screen parent;
    protected final EnhancedTimerClient client = EnhancedTimerClient.get();
    private final String titleKey;
    protected int left, top, contentWidth;
    protected String error = "";
    EnhancedScreen(Screen parent, String titleKey) {
        super(I18n.component(titleKey)); this.parent = parent; this.titleKey = titleKey;
    }
    @Override public Component getTitle() { return I18n.component(titleKey); }
    @Override protected void init() {
        contentWidth = Math.min(310, width - 24);
        left = (width - contentWidth) / 2;
        top = Math.max(6, (height - 240) / 2);
    }
    protected Button button(String label, int x, int y, int w, Runnable action) {
        return addRenderableWidget(Button.builder(Component.literal(label), b -> action.run()).bounds(x, y, w, 20).build());
    }
    protected Button row(String key, String value, int y, Runnable action) {
        return button(I18n.text(key) + ": " + value, left, top + y, contentWidth, action);
    }
    protected String onOff(boolean on) { return I18n.text(on ? "on" : "off"); }
    protected void heading(GuiGraphicsExtractor g) {
        var heading = getTitle();
        g.text(font, heading, width / 2 - font.width(heading) / 2, top + 16, 0xFFFFFFFF, true);
        if (!error.isEmpty()) g.text(font, error, left, top + errorOffset(), 0xFFFF5555, true);
    }
    protected int errorOffset() { return 178; }
    @Override public boolean isPauseScreen() { return true; }
    @Override public void onClose() { client.save(); Platform.screen(parent); }
    @Override public void removed() { client.save(); }
}

