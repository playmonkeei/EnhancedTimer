package dev.enhancedtimer.client.ui;

import dev.enhancedtimer.client.*;
import dev.enhancedtimer.core.*;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TimerScreen extends EnhancedScreen {
    private EditBox duration;
    private Button start, pause, stop;
    private TimerEngine.Mode shownMode;
    private boolean shownControl;
    private String shownSource;
    public TimerScreen(Screen parent) { super(parent, "Enhanced Timer"); }
    @Override protected void init() {
        super.init();
        shownMode = client.activeTimer().mode(); shownControl = client.canControl(); shownSource = client.usingServer() ? client.sync.scope : "local";
        int half = (contentWidth - 6) / 2;
        row("mode", I18n.text("mode." + client.activeTimer().mode().name().toLowerCase(Locale.ROOT)), 94, () -> {
            client.configure(client.activeTimer().mode() == TimerEngine.Mode.STOPWATCH ? TimerEngine.Mode.COUNTDOWN : TimerEngine.Mode.STOPWATCH,
                    client.usingServer() ? client.activeTimer().durationMillis() : client.settings.durationMillis);
            rebuildWidgets();
        }).active = client.canControl();
        duration = addRenderableWidget(new EditBox(font, left, top + 120, contentWidth - 86, 20, I18n.component("duration")));
        duration.setMaxLength(24);
        duration.setValue(TimeFormat.CLOCK.format(client.usingServer() ? client.activeTimer().durationMillis() : client.settings.durationMillis, false));
        duration.setHint(Component.literal("02:00:00 / 2h / 90m"));
        var apply = button(I18n.text("apply"), left + contentWidth - 80, top + 120, 80, () -> {
            try { client.configure(TimerEngine.Mode.COUNTDOWN, Durations.parse(duration.getValue())); error = ""; rebuildWidgets(); }
            catch (IllegalArgumentException e) { error = I18n.text("error.duration.short"); }
        });
        duration.active = apply.active = client.canControl();
        int third = (contentWidth - 12) / 3;
        start = button(I18n.text("start"), left, top + 146, third, () -> client.action("start"));
        pause = button(I18n.text("pause"), left + third + 6, top + 146, third, () -> client.action("pause"));
        stop = button(I18n.text("stop"), left + (third + 6) * 2, top + 146, contentWidth - (third + 6) * 2, () -> client.action("stop"));
        button(I18n.text("appearance"), left, top + 188, half, () -> Platform.screen(new AppearanceScreen(this)));
        button(I18n.text("settings"), left + half + 6, top + 188, contentWidth - half - 6, () -> Platform.screen(new PreferencesScreen(this)));
        button(I18n.text("done"), width / 2 - 50, Math.min(height - 24, top + 214), 100, this::onClose);
    }
    @Override public void tick() {
        if (shownMode != client.activeTimer().mode() || shownControl != client.canControl()
                || !shownSource.equals(client.usingServer() ? client.sync.scope : "local")) rebuildWidgets();
        var state = client.activeTimer().state();
        start.active = client.inWorld() && client.canControl() && state != TimerEngine.State.RUNNING;
        start.setMessage(I18n.component(state == TimerEngine.State.PAUSED ? "resume" : "start"));
        pause.active = client.inWorld() && client.canControl() && state == TimerEngine.State.RUNNING;
        stop.active = client.inWorld() && client.canControl() && (state == TimerEngine.State.RUNNING || state == TimerEngine.State.PAUSED);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) {
        heading(g);
        String source = !client.usingServer() ? I18n.text("source.personal")
                : client.sync.scope.startsWith("personal:") ? I18n.text("source.server_personal")
                : client.sync.scope.startsWith("team:") ? I18n.text("team") + ": " + client.sync.scope.substring(5) : I18n.text("source.server");
        g.text(font, source, width / 2 - font.width(source) / 2, top + 32, 0xFFAAAAAA, true);
        String time = HudRenderer.time();
        float scale = Math.min(1.5f, (contentWidth - 16f) / TimerText.width(font, time));
        g.pose().pushMatrix();
        g.pose().translate(width / 2f, top + 46f);
        g.pose().scale(scale, scale);
        TimerText.draw(g, font, time, -TimerText.width(font, time) / 2, 0, client.settings.shadow);
        g.pose().popMatrix();
        String status = I18n.text(client.inWorld() ? (client.activeTimer().suspended() && client.activeTimer().state() == TimerEngine.State.RUNNING
                ? "state.suspended" : "state." + client.activeTimer().state().name().toLowerCase(Locale.ROOT)) : "menu.join_world");
        g.text(font, status, width / 2 - font.width(status) / 2, top + 78, 0xFFAAAAAA, true);
        if (error.isEmpty()) g.text(font, I18n.text("menu.duration_hint"), left, top + 174, 0xFFAAAAAA, true);
        super.extractRenderState(g, x, y, delta);
    }
}

