package dev.enhancedtimer.smoke;

import com.google.gson.Gson;
import dev.enhancedtimer.client.*;
import dev.enhancedtimer.client.ui.HudRenderer;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Read-only observation of real GUI sessions. Never packaged in a release JAR. */
@Mixin(Minecraft.class)
abstract class ManualProbeMixin {
    @Unique private long enhancedtimer$lastProbe;
    @Inject(method = "tick", at = @At("TAIL"))
    private void enhancedtimer$observe(CallbackInfo ci) {
        if (!Boolean.getBoolean("enhancedtimer.manualProbe")) return;
        long now = System.nanoTime();
        if (now - enhancedtimer$lastProbe < 500_000_000L) return;
        enhancedtimer$lastProbe = now;
        var mc = Minecraft.getInstance(); var client = EnhancedTimerClient.get();
        var data = new LinkedHashMap<String, Object>();
        data.put("at", java.time.Instant.now().toString());
        data.put("player", mc.getUser().getName()); data.put("inWorld", client.inWorld());
        var screen = Platform.screen(); data.put("screen", screen == null ? "game" : screen.getClass().getSimpleName());
        data.put("gameLanguage", mc.options.languageCode);
        if (screen != null) data.put("screenTitle", screen.getTitle().getString());
        data.put("focused", mc.isWindowActive()); data.put("mouse", List.of(mc.mouseHandler.xpos(), mc.mouseHandler.ypos()));
        data.put("paused", mc.isPaused()); data.put("hudHidden", Platform.hudHidden());
        data.put("guiWidth", mc.getWindow().getGuiScaledWidth()); data.put("guiHeight", mc.getWindow().getGuiScaledHeight());
        data.put("settings", client.settings); data.put("usingServer", client.usingServer());
        data.put("syncConnected", client.sync.connected); data.put("scope", client.sync.scope); data.put("canControl", client.canControl());
        data.put("timer", client.activeTimer().snapshot()); data.put("suspended", client.activeTimer().suspended());
        data.put("displayMillis", client.activeTimer().displayMillis()); data.put("splits", client.activeSession().splits());
        data.put("hudText", HudRenderer.time()); data.put("hudBounds", HudRenderer.bounds(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight()));
        data.put("translatedStart", I18n.text("start"));
        if (screen != null) data.put("widgets", screen.children().stream().filter(c -> c instanceof AbstractWidget).map(c -> {
            var w = (AbstractWidget)c;
            return Map.of("label", w.getMessage().getString(), "active", w.active, "x", w.getX(), "y", w.getY(), "width", w.getWidth(), "height", w.getHeight());
        }).toList());
        if (mc.getSingleplayerServer() != null) {
            var world = mc.getSingleplayerServer().getWorldData();
            data.put("hardcore", world.isHardcore()); data.put("allowCommands", world.isAllowCommands());
        }
        if (mc.player != null) {
            data.put("uuid", mc.player.getUUID().toString()); data.put("position", List.of(mc.player.getX(), mc.player.getY(), mc.player.getZ()));
            data.put("dimension", mc.level.dimension().identifier().toString()); data.put("health", mc.player.getHealth());
            data.put("spectator", mc.player.isSpectator()); data.put("containerId", mc.player.containerMenu.containerId);
            var inventory = new ArrayList<String>();
            for (int i=0; i<mc.player.getInventory().getContainerSize(); i++) inventory.add(mc.player.getInventory().getItem(i).toString());
            data.put("inventory", inventory);
            data.put("containerSlots", mc.player.containerMenu.slots.stream().map(s -> s.getItem().toString()).toList());
            data.put("carried", mc.player.containerMenu.getCarried().toString());
        }
        try {
            var folder = mc.gameDirectory.toPath().resolve("manual-evidence"); Files.createDirectories(folder);
            var json = new Gson().toJson(data);
            Files.writeString(folder.resolve("state.json"), json+"\n");
            Files.writeString(folder.resolve("trace.jsonl"), json+"\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
    }
}
