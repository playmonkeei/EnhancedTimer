package dev.enhancedtimer.smoke;

import dev.enhancedtimer.client.EnhancedTimerClient;
import dev.enhancedtimer.core.TimerEngine;
import java.nio.file.*;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only packaged in build/test-tools; never included in a release archive. */
@Mixin(Minecraft.class)
abstract class DriverMixin {
    @Unique private long enhancedtimer$began, enhancedtimer$stepAt;
    @Unique private int enhancedtimer$step;
    @Unique private boolean enhancedtimer$done;
    @Unique private boolean enhancedtimer$connecting;
    @Unique private int enhancedtimer$menu;
    @Unique private boolean enhancedtimer$pluginMenuRequested, enhancedtimer$pluginMenuVerified;
    @Unique private boolean enhancedtimer$splitUndoRequested;
    @Inject(method = "tick", at = @At("TAIL"))
    private void enhancedtimer$test(CallbackInfo ci) {
        if (!Boolean.getBoolean("enhancedtimer.smoke") || enhancedtimer$done) return;
        long now = System.nanoTime(); if (enhancedtimer$began == 0) enhancedtimer$began = now;
        var mc = Minecraft.getInstance(); var client = EnhancedTimerClient.get();
        try {
            if (now - enhancedtimer$began > 90_000_000_000L) throw new IllegalStateException("Timeout at step " + enhancedtimer$step + "; joined=" + client.inWorld() + "; sync=" + client.sync.connected);
            if (!client.inWorld() && !enhancedtimer$connecting && now - enhancedtimer$began > 10_000_000_000L) {
                enhancedtimer$connecting = true;
                var address = System.getProperty("enhancedtimer.smokeAddress", "127.0.0.1:25576");
                net.minecraft.client.gui.screens.ConnectScreen.startConnecting(new net.minecraft.client.gui.screens.TitleScreen(), mc,
                        net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(address),
                        new net.minecraft.client.multiplayer.ServerData("EnhancedTimer localhost test", address, net.minecraft.client.multiplayer.ServerData.Type.OTHER), false, null);
            }
            if (!client.inWorld() || !client.sync.connected) return;
            switch (enhancedtimer$step) {
                case 0 -> { client.sync.select("personal"); enhancedtimer$advance(now); }
                case 1 -> {
                    if (!client.sync.scope.startsWith("personal:") || !client.canControl()) return;
                    client.configure(TimerEngine.Mode.COUNTDOWN, 7000); client.action("start"); enhancedtimer$advance(now);
                }
                case 2 -> {
                    if (now - enhancedtimer$stepAt < 1_000_000_000L) return;
                    if (client.activeTimer().state() != TimerEngine.State.RUNNING) throw new IllegalStateException("Countdown did not start");
                    client.command("/et split Network-Smoke"); enhancedtimer$advance(now);
                }
                case 3 -> {
                    if (now - enhancedtimer$stepAt < 800_000_000L) return;
                    if (!enhancedtimer$splitUndoRequested) {
                        if (client.activeSession().splits().stream().noneMatch(s -> s.name().equals("Network-Smoke"))) throw new IllegalStateException("Split did not synchronize");
                        client.action("undo"); enhancedtimer$splitUndoRequested = true; enhancedtimer$stepAt = now; return;
                    }
                    if (!client.activeSession().splits().isEmpty() || client.activeTimer().state() != TimerEngine.State.RUNNING)
                        throw new IllegalStateException("Undo split changed the running clock or did not synchronize");
                    client.addSplit("Network-Smoke");
                    client.action("pause"); enhancedtimer$advance(now);
                }
                case 4 -> {
                    if (now - enhancedtimer$stepAt < 800_000_000L) return;
                    if (client.activeTimer().state() != TimerEngine.State.PAUSED) throw new IllegalStateException("Pause did not synchronize");
                    client.setDisplay(1500); client.action("start"); enhancedtimer$advance(now);
                }
                case 5 -> {
                    if (now - enhancedtimer$stepAt < 2_500_000_000L) return;
                    if (client.activeTimer().state() != TimerEngine.State.FINISHED) throw new IllegalStateException("Countdown did not finish");
                    client.action("undo"); enhancedtimer$advance(now);
                }
                case 6 -> {
                    if (now - enhancedtimer$stepAt < 800_000_000L) return;
                    if (client.activeTimer().state() != TimerEngine.State.PAUSED) throw new IllegalStateException("Undo did not synchronize");
                    client.sync.useLocal();
                    if (client.usingServer()) throw new IllegalStateException("Local selection failed");
                    if (Boolean.getBoolean("enhancedtimer.smokeMenus")) enhancedtimer$advance(now);
                    else enhancedtimer$finish(mc, "PASS: personal selection, permissions, configure+start, split command, pause, set+start, expiry, undo, local selection");
                }
                case 7 -> {
                    if (now - enhancedtimer$stepAt < 400_000_000L) return;
                    if (Boolean.getBoolean("enhancedtimer.smokePluginMenus") && !enhancedtimer$pluginMenuRequested) {
                        enhancedtimer$pluginMenuRequested = true; mc.getConnection().sendCommand("ets"); enhancedtimer$stepAt = now; return;
                    }
                    if (enhancedtimer$pluginMenuRequested && !enhancedtimer$pluginMenuVerified) {
                        if (now - enhancedtimer$stepAt < 1_000_000_000L) return;
                        if (mc.player.containerMenu.containerId == 0) throw new IllegalStateException("Server inventory menu did not open");
                        mc.player.closeContainer(); enhancedtimer$pluginMenuVerified = true;
                    }
                    net.minecraft.client.gui.screens.Screen menu = switch (enhancedtimer$menu++) {
                        case 0 -> new dev.enhancedtimer.client.ui.TimerScreen(null);
                        case 1 -> new dev.enhancedtimer.client.ui.AppearanceScreen(null);
                        case 2 -> new dev.enhancedtimer.client.ui.ColorScreen(null);
                        case 3 -> new dev.enhancedtimer.client.ui.LayoutScreen(null);
                        case 4 -> new dev.enhancedtimer.client.ui.PreferencesScreen(null);
                        case 5 -> new dev.enhancedtimer.client.ui.AdvancedScreen(null);
                        case 6 -> new dev.enhancedtimer.client.ui.WarningScreen(null);
                        case 7 -> new dev.enhancedtimer.client.ui.TemplateScreen(null);
                        case 8 -> new dev.enhancedtimer.client.ui.SplitsScreen(null);
                        case 9 -> new dev.enhancedtimer.client.ui.SourceScreen(null);
                        case 10 -> new dev.enhancedtimer.client.ui.DesignScreen(null);
                        case 11 -> new dev.enhancedtimer.client.ui.DesignLibraryScreen(null);
                        case 12 -> new dev.enhancedtimer.client.ui.SetTimeScreen(null);
                        default -> null;
                    };
                    if (menu == null) enhancedtimer$finish(mc, "PASS: timer synchronization, 13 rendered configuration/editor screens; server inventory menu=" + enhancedtimer$pluginMenuVerified);
                    else { dev.enhancedtimer.client.Platform.screen(menu); enhancedtimer$stepAt = now; }
                }
            }
        } catch (RuntimeException e) { enhancedtimer$finish(mc, "FAIL: " + e); }
    }
    @Unique private void enhancedtimer$advance(long now) { enhancedtimer$step++; enhancedtimer$stepAt = now; }
    @Unique private void enhancedtimer$finish(Minecraft mc, String result) {
        enhancedtimer$done = true;
        try { Files.writeString(mc.gameDirectory.toPath().resolve("enhancedtimer-smoke-result.txt"), result + "\n"); }
        catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
        System.getLogger("EnhancedTimerSmoke").log(System.Logger.Level.INFO, result); mc.stop();
    }
}
