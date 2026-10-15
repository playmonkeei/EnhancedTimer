package dev.enhancedtimer.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.enhancedtimer.client.*;
import dev.enhancedtimer.core.HudPosition;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;

public final class LayoutScreen extends Screen {
    private final Screen parent;
    private final Settings settings = EnhancedTimerClient.get().settings;
    private final HudPosition.Anchor originalAnchor;
    private final double originalX, originalY;
    private boolean dragging;
    private double offsetX, offsetY;
    public LayoutScreen(Screen parent) {
        super(I18n.component("editor")); this.parent = parent;
        originalAnchor = settings.anchor; originalX = settings.x; originalY = settings.y;
    }
    @Override protected void init() {
        int w = Math.min(120, (width - 24) / 2);
        addRenderableWidget(Button.builder(I18n.component("save"), b -> { EnhancedTimerClient.get().save(); Platform.screen(parent); })
                .bounds(width / 2 - w - 3, height - 28, w, 20).build());
        addRenderableWidget(Button.builder(I18n.component("cancel"), b -> onClose())
                .bounds(width / 2 + 3, height - 28, w, 20).build());
    }
    @Override public void extractBackground(GuiGraphicsExtractor g, int x, int y, float delta) {}
    @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) {
        g.fill(0, 0, width, height, 0x40000000);
        g.fill(width / 2, 0, width / 2 + 1, height, 0x40FFFFFF);
        g.fill(0, height / 2, width, height / 2 + 1, 0x40FFFFFF);
        HudRenderer.draw(g, true);
        String hint = I18n.text("editor.hint");
        g.text(font, hint, width / 2 - font.width(hint) / 2, height - 44, 0xFFFFFFFF, true);
        super.extractRenderState(g, x, y, delta);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        var bounds = HudRenderer.bounds(width, height);
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && bounds.contains(event.x(), event.y())) {
            dragging = true; offsetX = event.x() - bounds.x(); offsetY = event.y() - bounds.y(); return true;
        }
        return false;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!dragging) return super.mouseDragged(event, dx, dy);
        moveTo(event);
        return true;
    }
    private void moveTo(MouseButtonEvent event) {
        var bounds = HudRenderer.bounds(width, height);
        settings.anchor = HudPosition.Anchor.CUSTOM;
        settings.x = HudPosition.normalize(event.x() - offsetX, width, bounds.width());
        settings.y = HudPosition.normalize(event.y() - offsetY, height, bounds.height());
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            // A quick drag can begin and end between two rendered frames.
            moveTo(event);
            dragging = false;
            return true;
        }
        return super.mouseReleased(event);
    }
    @Override public boolean keyPressed(KeyEvent event) {
        int dx = event.key() == InputConstants.KEY_LEFT ? -1 : event.key() == InputConstants.KEY_RIGHT ? 1 : 0;
        int dy = event.key() == InputConstants.KEY_UP ? -1 : event.key() == InputConstants.KEY_DOWN ? 1 : 0;
        if (dx != 0 || dy != 0) {
            var b = HudRenderer.bounds(width, height); settings.anchor = HudPosition.Anchor.CUSTOM;
            settings.x = HudPosition.normalize(b.x() + dx, width, b.width());
            settings.y = HudPosition.normalize(b.y() + dy, height, b.height());
            return true;
        }
        return super.keyPressed(event);
    }
    @Override public boolean isPauseScreen() { return true; }
    @Override public void onClose() {
        settings.anchor = originalAnchor; settings.x = originalX; settings.y = originalY;
        EnhancedTimerClient.get().save(); Platform.screen(parent);
    }
}
