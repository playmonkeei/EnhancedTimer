package dev.enhancedtimer.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerLanguage {
    private static final Map<String, Properties> LANGUAGES = new ConcurrentHashMap<>();
    private ServerLanguage() {}
    public static String scope(String language, String id) {
        if (id.startsWith("personal:")) return text(language, "personal");
        if (id.startsWith("team:")) return text(language, "team") + ": " + id.substring(5);
        return text(language, "shared");
    }
    public static String text(String language, String key) {
        String code = language == null ? "en" : language.toLowerCase(Locale.ROOT).split("[_-]")[0];
        if (!Set.of("en", "de", "fr", "es").contains(code)) code = "en";
        return LANGUAGES.computeIfAbsent(code, ServerLanguage::load).getProperty(key, key);
    }
    private static Properties load(String code) {
        var properties = new Properties();
        try (var stream = ServerLanguage.class.getResourceAsStream("/server-lang/" + code + ".properties")) {
            if (stream != null) properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException e) { throw new UncheckedIOException(e); }
        return properties;
    }
}
