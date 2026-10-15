package dev.enhancedtimer.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class I18n {
    private static final Map<String, Map<String, String>> LANGUAGES = new HashMap<>();
    public static final List<String> CODES = List.of("auto", "de_de", "en_us", "fr_fr", "es_es");
    private I18n() {}
    private static Map<String, String> load(String code) {
        return LANGUAGES.computeIfAbsent(code, key -> {
            var stream = I18n.class.getResourceAsStream("/assets/enhancedtimer/lang/" + key + ".json");
            if (stream == null) return Map.of();
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return new Gson().fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
            } catch (java.io.IOException e) { return Map.of(); }
        });
    }
    public static String text(String key) {
        String code = EnhancedTimerClient.get().settings.language;
        if (code.equals("auto")) code = Minecraft.getInstance().options.languageCode;
        Map<String, String> language = load(code);
        if (language.isEmpty() && code.contains("_")) language = load(switch (code.substring(0, code.indexOf('_'))) {
            case "de" -> "de_de"; case "fr" -> "fr_fr"; case "es" -> "es_es"; default -> "en_us";
        });
        return language.getOrDefault(key, load("en_us").getOrDefault(key, key));
    }
    public static Component component(String key) { return Component.literal(text(key)); }
    public static String languageName(String code) { return switch (code) {
        case "de_de" -> "Deutsch"; case "en_us" -> "English"; case "fr_fr" -> "Français";
        case "es_es" -> "Español"; default -> text("language.auto");
    }; }
}

