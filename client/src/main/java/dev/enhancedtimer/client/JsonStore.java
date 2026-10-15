package dev.enhancedtimer.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public final class JsonStore {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final System.Logger LOG = System.getLogger("Enhanced Timer");
    private JsonStore() {}
    public static <T> T copy(T value, Class<T> type) { return JSON.fromJson(JSON.toJson(value), type); }
    public static <T> T read(Path path, Class<T> type, T fallback) {
        if (!Files.exists(path)) return fallback;
        try {
            if (Files.size(path) > 262_144) throw new IOException("EnhancedTimer JSON exceeds 256 KiB");
            T value = JSON.fromJson(Files.readString(path), type);
            return value == null ? fallback : value;
        } catch (IOException | RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "Cannot read " + path + "; keeping a backup", e);
            try { Files.copy(path, path.resolveSibling(path.getFileName() + ".broken-" + System.currentTimeMillis())); }
            catch (IOException backupError) { LOG.log(System.Logger.Level.WARNING, "Backup failed", backupError); }
            return fallback;
        }
    }
    public static void write(Path path, Object value) {
        try {
            Files.createDirectories(path.getParent());
            Path temporary = Files.createTempFile(path.getParent(), "enhancedtimer-", ".tmp");
            try {
                Files.writeString(temporary, JSON.toJson(value), StandardCharsets.UTF_8);
                try { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
                catch (AtomicMoveNotSupportedException e) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
            } finally { Files.deleteIfExists(temporary); }
        } catch (IOException e) { LOG.log(System.Logger.Level.ERROR, "Cannot save " + path, e); }
    }
}

