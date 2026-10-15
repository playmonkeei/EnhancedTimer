package dev.enhancedtimer.client.ui;

import dev.enhancedtimer.client.*;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;

public final class WarningScreen extends EnhancedScreen {
    public WarningScreen(Screen parent) { super(parent, "warnings"); }
    @Override protected int errorOffset() { return 32; }
    @Override protected void init() {
        super.init(); var s = client.settings;
        row("warnings", onOff(s.warnings), 48, () -> { s.warnings = !s.warnings; rebuildWidgets(); });
        row("state_colors", onOff(s.stateColors), 74, () -> { s.stateColors = !s.stateColors; rebuildWidgets(); });
        var seconds = field(108, String.join(", ", Arrays.stream(s.warningMillis).mapToObj(n -> Long.toString(n / 1000)).toList()), "warning.seconds");
        var paused = field(136, String.format(Locale.ROOT, "#%06X", s.pausedColor & 0xFFFFFF), "color.paused");
        var finished = field(164, String.format(Locale.ROOT, "#%06X", s.finishedColor & 0xFFFFFF), "color.finished");
        button(I18n.text("apply"), left, top + 188, contentWidth, () -> {
            try {
                String[] parts = seconds.getValue().split(",", -1); if (parts.length > 16) throw new IllegalArgumentException();
                var values = new long[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    long value = Long.parseLong(parts[i].strip()); if (value <= 0 || value > 31_536_000) throw new IllegalArgumentException();
                    values[i] = value * 1000;
                }
                int pauseColor = color(paused.getValue()), finishColor = color(finished.getValue());
                s.warningMillis = values; s.pausedColor = pauseColor; s.finishedColor = finishColor;
                s.sanitize(); client.save(); error = "";
            } catch (IllegalArgumentException e) { error = I18n.text("error.warning"); }
        });
        button(I18n.text("done"), width / 2 - 50, Math.min(height - 24, top + 214), 100, this::onClose);
    }
    private int color(String value) {
        String raw = value.replaceFirst("^#", ""); if (!raw.matches("[a-fA-F0-9]{6}")) throw new IllegalArgumentException();
        return 0xFF000000 | Integer.parseInt(raw, 16);
    }
    private EditBox field(int y, String value, String label) {
        var box = addRenderableWidget(new EditBox(font, left + 120, top + y, contentWidth - 120, 20, I18n.component(label)));
        box.setMaxLength(160); box.setValue(value); return box;
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) {
        heading(g);
        g.text(font, I18n.text("warning.seconds"), left, top + 114, 0xFFAAAAAA, true);
        g.text(font, I18n.text("color.paused"), left, top + 142, 0xFFAAAAAA, true);
        g.text(font, I18n.text("color.finished"), left, top + 170, 0xFFAAAAAA, true);
        super.extractRenderState(g, x, y, delta);
    }
}
