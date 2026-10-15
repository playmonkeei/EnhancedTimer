package dev.enhancedtimer.paper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class Messages {
    private static final Map<String, Properties> LANGUAGES = new HashMap<>();
    static String text(String language, String key) {
        return dev.enhancedtimer.core.ServerLanguage.text(language, key);
    }
    private static Properties load(String code) {
        var values = new Properties();
        try (var stream = Messages.class.getResourceAsStream("/lang/" + code + ".properties")) {
            if (stream != null) values.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException e) { throw new UncheckedIOException(e); }
        return values;
    }
}

