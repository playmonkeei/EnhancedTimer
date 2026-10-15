package dev.enhancedtimer.client.ui;

import dev.enhancedtimer.client.*;
import dev.enhancedtimer.core.TimerColor;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ColorScreen extends EnhancedScreen {
    public ColorScreen(Screen parent) { super(parent, "color"); }

    @Override protected void init() {
        super.init();
        var settings = client.settings;
        int cell = (contentWidth - 12) / 4;
        for (int i = 0; i < 16; i++) {
            TimerColor choice = TimerColor.values()[i];
            int column = i % 4;
            int x = left + column * (cell + 4);
            int width = column == 3 ? left + contentWidth - x : cell;
            var option = button("", x, top + 52 + (i / 4) * 24, width, () -> {
                settings.colorMode = choice;
                error = "";
                rebuildWidgets();
            });
            var label = Component.literal("■ ").withStyle(style -> style.withColor(choice.argb(0, 0, 0) & 0xFFFFFF))
                    .append(Component.literal(I18n.text("color." + choice.name().toLowerCase(Locale.ROOT)))
                            .withStyle(ChatFormatting.WHITE));
            if (settings.colorMode == choice) label.withStyle(ChatFormatting.UNDERLINE);
            option.setMessage(label);
        }
        int half = (contentWidth - 4) / 2;
        button(I18n.text("color.rainbow"), left, top + 152, half, () -> {
            settings.colorMode = TimerColor.RAINBOW; error = ""; rebuildWidgets();
        });
        button(I18n.text("color.custom"), left + half + 4, top + 152, contentWidth - half - 4, () -> {
            settings.colorMode = TimerColor.CUSTOM; error = ""; rebuildWidgets();
        });
        var hex = addRenderableWidget(new EditBox(font, left, top + 188, contentWidth - 84, 20, I18n.component("color.custom")));
        hex.setMaxLength(7);
        hex.setValue(String.format(Locale.ROOT, "#%06X", settings.color & 0xFFFFFF));
        button(I18n.text("apply"), left + contentWidth - 80, top + 188, 80, () -> {
            String value = hex.getValue().replaceFirst("^#", "");
            if (value.matches("[0-9a-fA-F]{6}")) {
                settings.color = 0xFF000000 | Integer.parseInt(value, 16);
                settings.colorMode = TimerColor.CUSTOM;
                error = "";
            } else error = I18n.text("error.color");
        });
        button(I18n.text("done"), width / 2 - 50, Math.min(height - 24, top + 214), 100, this::onClose);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) {
        heading(g);
        String time = HudRenderer.time();
        String selection = I18n.text("color." + client.settings.colorMode.name().toLowerCase(Locale.ROOT));
        int total = font.width(selection) + 12 + TimerText.width(font, time);
        int start = width / 2 - total / 2;
        g.text(font, selection, start, top + 36, 0xFFAAAAAA, true);
        TimerText.drawPalette(g, font, time, start + font.width(selection) + 12, top + 36, client.settings.shadow);
        super.extractRenderState(g, x, y, delta);
    }
}
