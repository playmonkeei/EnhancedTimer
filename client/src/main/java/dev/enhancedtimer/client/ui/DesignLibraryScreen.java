package dev.enhancedtimer.client.ui;

import com.google.gson.Gson;
import dev.enhancedtimer.client.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;

/** Explicitly opened HTTPS catalog; never polls or uploads player/server information. */
public final class DesignLibraryScreen extends EnhancedScreen {
    public record Item(String name, Settings design) {}
    public record Catalog(int version, List<Item> designs) {}
    private List<Item> designs = List.of();
    private boolean loading;
    private int page;
    public DesignLibraryScreen(Screen parent) { super(parent, "design.library"); }
    @Override protected int errorOffset() { return 174; }
    @Override protected void init() {
        super.init();
        var url = addRenderableWidget(new EditBox(font, left, top + 42, contentWidth - 64, 20, I18n.component("design.catalog")));
        url.setMaxLength(2048); url.setValue(client.settings.designLibraryUrl);
        button(I18n.text("load"), left + contentWidth - 60, top + 42, 60, () -> load(url.getValue())).active = !loading;
        int start = page * 4;
        for (int i = start; i < Math.min(designs.size(), start + 4); i++) {
            Item item = designs.get(i);
            button(item.name(), left, top + 72 + (i % 4) * 24, contentWidth, () -> { client.settings.applyDesign(item.design()); client.save(); });
        }
        button("<", left, top + 188, 60, () -> { page = Math.max(0, page - 1); rebuildWidgets(); });
        button(">", left + contentWidth - 60, top + 188, 60, () -> { page = Math.min(Math.max(0, (designs.size() - 1) / 4), page + 1); rebuildWidgets(); });
        button(I18n.text("done"), width / 2 - 50, Math.min(height - 24, top + 214), 100, this::onClose);
    }
    private void load(String address) {
        if (loading) return;
        try {
            URI uri = URI.create(address);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) throw new IllegalArgumentException();
            loading = true; error = ""; client.settings.designLibraryUrl = address; client.save(); rebuildWidgets();
            Thread.ofVirtual().name("EnhancedTimer-design-catalog").start(() -> {
                try (HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build()) {
                    var request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(15)).header("User-Agent", "EnhancedTimer (https://github.com/playmonkeei/EnhancedTimer)").GET().build();
                    var response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
                    try (var body = response.body()) {
                        java.util.concurrent.CompletableFuture.delayedExecutor(15, java.util.concurrent.TimeUnit.SECONDS)
                                .execute(() -> { try { body.close(); } catch (java.io.IOException ignored) { } });
                        if (response.statusCode() != 200) throw new IllegalArgumentException("HTTP " + response.statusCode());
                        byte[] bytes = body.readNBytes(262145); if (bytes.length > 262144) throw new IllegalArgumentException("Catalog too large");
                        Catalog catalog = new Gson().fromJson(new String(bytes, java.nio.charset.StandardCharsets.UTF_8), Catalog.class);
                        if (catalog == null || catalog.version() != 1 || catalog.designs() == null || catalog.designs().size() > 256) throw new IllegalArgumentException();
                        var validated = new ArrayList<Item>();
                        for (var item : catalog.designs()) {
                            if (item == null || item.name() == null || item.name().isBlank() || item.name().length() > 64 || item.design() == null) throw new IllegalArgumentException();
                            item.design().sanitize(); validated.add(item);
                        }
                        Minecraft.getInstance().execute(() -> { designs = List.copyOf(validated); loading = false; page = 0; if (Platform.screen() == this) rebuildWidgets(); });
                    }
                } catch (Exception e) { Minecraft.getInstance().execute(() -> { loading = false; error = I18n.text("error.file"); if (Platform.screen() == this) rebuildWidgets(); }); }
            });
        } catch (IllegalArgumentException e) { error = I18n.text("error.https"); }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) {
        heading(g); if (loading) g.text(font, I18n.text("loading"), left, top + 164, 0xFFAAAAAA, true);
        super.extractRenderState(g, x, y, delta);
    }
}
