package dev.enhancedtimer.text;

import java.util.*;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/** Public boundary contains strings only; vendor types never leak into the mod/plugin API. */
public final class RichText {
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final Map<String, String> JSON = new LinkedHashMap<>(32, 0.75f, true);
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder().character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();
    private RichText() {}
    public static synchronized String json(String input) {
        if (input == null || input.length() > 1024) throw new IllegalArgumentException("Text too long");
        String value = JSON.get(input);
        if (value == null) {
            value = GsonComponentSerializer.gson().serialize(MINI.deserialize(input)); JSON.put(input, value);
            if (JSON.size() > 64) JSON.remove(JSON.keySet().iterator().next());
        }
        return value;
    }
    public static String legacy(String input) {
        if (input == null || input.length() > 1024) throw new IllegalArgumentException("Text too long");
        return LEGACY.serialize(MINI.deserialize(input));
    }
    public static String animate(String input, long nanos) {
        if (!input.contains("{phase}")) return input;
        double phase = Math.sin(Math.floorMod(nanos, 8_000_000_000L) / 8_000_000_000.0 * Math.PI * 2);
        return input.replace("{phase}", String.format(Locale.ROOT, "%.3f", phase));
    }
}
