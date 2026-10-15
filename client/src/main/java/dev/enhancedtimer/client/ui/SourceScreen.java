package dev.enhancedtimer.client.ui;

import dev.enhancedtimer.client.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;

public final class SourceScreen extends EnhancedScreen {
    private boolean connected;
    public SourceScreen(Screen parent) { super(parent, "source"); }
    @Override protected void init() {
        super.init();
        connected = client.sync.connected;
        button(I18n.text("source.personal"), left, top + 48, contentWidth, () -> { client.settings.serverHud = false; client.sync.useLocal(); client.save(); });
        button(I18n.text("source.server"), left, top + 74, contentWidth, () -> { client.settings.serverHud = true; client.sync.select("global"); client.save(); }).active = client.sync.connected;
        button(I18n.text("source.server_personal"), left, top + 100, contentWidth, () -> { client.settings.serverHud = true; client.sync.select("personal"); client.save(); }).active = client.sync.connected;
        var team = addRenderableWidget(new EditBox(font, left, top + 130, contentWidth - 84, 20, I18n.component("team")));
        team.setMaxLength(24); team.setHint(I18n.component("team"));
        button(I18n.text("join"), left + contentWidth - 80, top + 130, 80, () -> {
            if (team.getValue().matches("[a-z0-9_-]{1,24}")) { client.settings.serverHud = true; client.sync.select("team:" + team.getValue()); client.save(); }
        }).active = client.sync.connected;
        button(I18n.text("done"), width / 2 - 50, Math.min(height - 24, top + 214), 100, this::onClose);
    }
    @Override public void tick() { if (connected != client.sync.connected) rebuildWidgets(); }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) {
        heading(g);
        String status = I18n.text(client.sync.connected ? "server.connected" : "server.unavailable");
        g.text(font, status, left, top + 168, 0xFFAAAAAA, true);
        g.text(font, client.profileName(), left, top + 186, 0xFFAAAAAA, true);
        super.extractRenderState(g, x, y, delta);
    }
}
