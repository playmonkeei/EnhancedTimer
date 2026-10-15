package dev.enhancedtimer.client.ui;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.enhancedtimer.client.EnhancedTimerClient;
import dev.enhancedtimer.core.TimerColor;
import dev.enhancedtimer.text.RichText;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.*;

/** Shared styled representation for measuring, previewing, dragging and rendering. */
final class TimerText {
    private TimerText() {}
    static Component component(String text) {
        return component(text, true);
    }
    private static Component component(String text, boolean stateStyle) {
        var client = EnhancedTimerClient.get(); var settings = client.settings; var timer = client.activeTimer();
        long now = System.nanoTime(); Component parsed = Component.literal(text);
        if (text.contains("<")) try {
            parsed = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(RichText.json(RichText.animate(text, now))))
                    .result().orElse(parsed);
        } catch (RuntimeException ignored) { }
        Integer override = stateStyle ? settings.stateColor(timer) : null;
        int base = settings.colorMode.argb(settings.color, now, 0);
        if (override == null && settings.colorMode != TimerColor.RAINBOW)
            return parsed.copy().withStyle(style -> style.getColor() == null ? style.withColor(base & 0xFFFFFF) : style);
        var styled = Component.empty(); int[] index = {0}; Integer uniform = override;
        parsed.visit((style, part) -> {
            for (int offset = 0; offset < part.length();) {
                int cp = part.codePointAt(offset);
                int color = uniform != null ? uniform : TimerColor.RAINBOW.argb(settings.color, now, index[0]++);
                styled.append(Component.literal(new String(Character.toChars(cp))).withStyle(style.withColor(color & 0xFFFFFF)));
                offset += Character.charCount(cp);
            }
            return Optional.<Void>empty();
        }, Style.EMPTY);
        return styled;
    }
    static int width(Font font, String text) { return font.width(component(text)); }
    static void draw(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, boolean shadow) {
        graphics.text(font, component(text), x, y, 0xFFFFFFFF, shadow);
    }
    static void drawPalette(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, boolean shadow) {
        graphics.text(font, component(text, false), x, y, 0xFFFFFFFF, shadow);
    }
}
