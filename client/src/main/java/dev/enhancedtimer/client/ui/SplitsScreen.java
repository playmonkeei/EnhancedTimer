package dev.enhancedtimer.client.ui;

import dev.enhancedtimer.client.*;
import dev.enhancedtimer.core.TimeFormat;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

public final class SplitsScreen extends EnhancedScreen {
    private int page;
    public SplitsScreen(Screen parent) { super(parent, "splits"); }
    @Override protected void init() {
        super.init(); int cell = (contentWidth - 12) / 4;
        button("<", left, top + 188, cell, () -> { page = Math.max(0, page - 1); });
        button(">", left + cell + 4, top + 188, cell, () -> { page = Math.min(Math.max(0, (client.activeSession().splits().size() - 1) / 7), page + 1); });
        button(I18n.text("split"), left + (cell + 4) * 2, top + 188, cell, () -> {
            try { client.addSplit("Split " + (client.activeSession().splits().size() + 1)); } catch (RuntimeException e) { error = e.getMessage(); }
        });
        button("CSV", left + (cell + 4) * 3, top + 188, contentWidth - (cell + 4) * 3, client::exportSplits);
        button(I18n.text("done"), width / 2 - 50, Math.min(height - 24, top + 214), 100, this::onClose);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) {
        heading(g); var splits = client.activeSession().splits();
        for (int i = page * 7; i < Math.min(splits.size(), page * 7 + 7); i++) {
            var split = splits.get(i);
            String label = split.name(); if (font.width(label) > contentWidth / 2) label = font.plainSubstrByWidth(label, contentWidth / 2);
            g.text(font, label, left, top + 48 + (i % 7) * 18, 0xFFFFFFFF, true);
            String time = TimeFormat.PRECISE.format(split.elapsedMillis(), false);
            g.text(font, time, left + contentWidth - font.width(time), top + 48 + (i % 7) * 18, 0xFFFFFF55, true);
        }
        super.extractRenderState(g, x, y, delta);
    }
}
