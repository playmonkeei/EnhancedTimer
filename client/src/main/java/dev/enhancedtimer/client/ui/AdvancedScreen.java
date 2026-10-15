package dev.enhancedtimer.client.ui;

import dev.enhancedtimer.client.*;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

public final class AdvancedScreen extends EnhancedScreen {
    private int page;
    public AdvancedScreen(Screen parent) { super(parent, "advanced"); }
    @Override protected void init() {
        super.init(); var s = client.settings;
        if (page == 0) {
            row("warnings", onOff(s.warnings), 48, () -> Platform.screen(new WarningScreen(this)));
            row("state_colors", onOff(s.stateColors), 74, () -> { s.stateColors = !s.stateColors; rebuildWidgets(); });
            row("alert_tone", I18n.text("tone." + s.alertTone.toLowerCase(Locale.ROOT)), 100, () -> {
                var values = List.of("ORB", "BELL", "CHIME", "LEVELUP", "NONE");
                s.alertTone = values.get((values.indexOf(s.alertTone) + 1) % values.size()); client.playAlert(); rebuildWidgets();
            });
            row("start_move", onOff(s.startOnMove), 126, () -> { s.startOnMove = !s.startOnMove; rebuildWidgets(); });
            row("pause_death", onOff(s.pauseOnDeath), 152, () -> { s.pauseOnDeath = !s.pauseOnDeath; rebuildWidgets(); });
        } else {
            row("stop_dragon", onOff(s.stopOnDragon), 48, () -> { s.stopOnDragon = !s.stopOnDragon; rebuildWidgets(); });
            row("milestones", onOff(s.splitMilestones), 74, () -> { s.splitMilestones = !s.splitMilestones; rebuildWidgets(); });
            row("clock", I18n.text("clock." + s.displayClock.toLowerCase(Locale.ROOT)), 100, () -> {
                var values = List.of("TIMER", "REAL", "GAME"); s.displayClock = values.get((values.indexOf(s.displayClock) + 1) % values.size()); rebuildWidgets();
            });
            button(I18n.text("templates"), left, top + 126, contentWidth, () -> Platform.screen(new TemplateScreen(this)));
            button(I18n.text("splits"), left, top + 152, contentWidth, () -> Platform.screen(new SplitsScreen(this)));
        }
        button(I18n.text("next") + " (" + (page + 1) + "/2)", left, top + 188, contentWidth, () -> { page = 1 - page; rebuildWidgets(); });
        button(I18n.text("done"), width / 2 - 50, Math.min(height - 24, top + 214), 100, this::onClose);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) { heading(g); super.extractRenderState(g, x, y, delta); }
}
