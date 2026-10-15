package dev.enhancedtimer.server;

import dev.enhancedtimer.core.*;
import dev.enhancedtimer.network.TimerPayload;
import java.nio.file.*;
import java.util.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.*;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

/** Server-only functionality uses vanilla hooks, avoiding a separate API for each loader. */
public final class VanillaTimerServer {
    private static final Map<MinecraftServer, VanillaTimerServer> SERVERS = new WeakHashMap<>();
    public static synchronized VanillaTimerServer get(MinecraftServer server) { return SERVERS.computeIfAbsent(server, VanillaTimerServer::new); }
    public final TimerHub hub = new TimerHub();
    private final ServerCommands commands = new ServerCommands(hub);
    private final MinecraftServer server;
    private final Path data;
    private final ServerOptions options;
    private final Set<UUID> clients = new HashSet<>(), hidden = new HashSet<>(), hudClients = new HashSet<>();
    private final Set<UUID> advertised = new HashSet<>();
    private final Set<UUID> actionbars = new HashSet<>();
    private final Map<UUID, ServerBossEvent> bars = new HashMap<>();
    private final Map<UUID, Vec3> positions = new HashMap<>();
    private final Map<String, Boolean> expired = new HashMap<>();
    private final RequestLimiter requests = new RequestLimiter();
    private int ticks;
    private long sequence;
    private VanillaTimerServer(MinecraftServer server) {
        this.server = server; data = server.getWorldPath(LevelResource.ROOT).resolve("enhancedtimer/timers.dat");
        Path settings = data.resolveSibling("server-settings.json");
        options = dev.enhancedtimer.client.JsonStore.read(settings, ServerOptions.class, new ServerOptions());
        options.sanitize(); dev.enhancedtimer.client.JsonStore.write(settings, options);
        try { TimerArchive.load(data, hub); } catch (java.io.IOException e) { System.getLogger("Enhanced Timer").log(System.Logger.Level.WARNING, "Cannot read server timers", e); }
    }
    public boolean administrator(ServerPlayer player) {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) || server.isSingleplayerOwner(new NameAndId(player.getGameProfile()));
    }
    public boolean paused(ServerPlayer player) { var timer = hub.session(hub.selected(player.getUUID())).timer; return timer.state() == TimerEngine.State.PAUSED || timer.suspended(); }
    public TimerSession.Rules rules(ServerPlayer player) { return hub.rules(hub.selected(player.getUUID())); }
    public void receive(ServerPlayer player, SyncProtocol.Message message) {
        if (server.getPlayerList().getPlayer(player.getUUID()) != player) return;
        if (!requests.allow(player.getUUID())) return;
        try {
            hub.request(player.getUUID(), administrator(player), message);
            clients.add(player.getUUID());
            if (message.kind() == SyncProtocol.Kind.SELECT) hudClients.add(player.getUUID());
            if (message.kind() == SyncProtocol.Kind.HELLO && message.action().equals("local")) hudClients.remove(player.getUUID());
            send(player, hub.state(player.getUUID(), administrator(player), ++sequence));
            if (message.kind() != SyncProtocol.Kind.HELLO) save();
        } catch (RuntimeException e) {
            String reason = "EnhancedTimer: " + e.getMessage();
            send(player, new SyncProtocol.Message(SyncProtocol.Kind.ERROR, "", reason.substring(0, Math.min(reason.length(), 160)), 0, null, false, false, 0));
        }
    }
    private void send(ServerPlayer player, SyncProtocol.Message message) {
        for (byte[] bytes : SyncProtocol.encodePackets(message)) player.connection.send(new ClientboundCustomPayloadPacket(new TimerPayload(bytes)));
    }
    public int command(CommandSourceStack source, String raw) {
        var player = source.getPlayer(); UUID actor = player == null ? ServerCommands.CONSOLE : player.getUUID();
        if (player != null && (raw.equals("hide") || raw.equals("show"))) {
            if (raw.equals("hide")) hidden.add(actor); else hidden.remove(actor); return 1;
        }
        try {
            String result = commands.execute(actor, player == null || administrator(player), raw.isBlank() ? new String[0] : raw.strip().split("\\s+"));
            source.sendSuccess(() -> Component.literal("[Enhanced Timer] " + result), false);
            if (raw.isBlank() && player != null) {
                var menu = Component.empty();
                for (String action : new String[]{"start", "pause", "stop", "undo", "reset"}) menu.append(Component.literal("[" + ServerLanguage.text(player.clientInformation().language(), action) + "] ")
                        .withStyle(style -> style.withColor(0xFFAA00).withClickEvent(new ClickEvent.RunCommand("/ets " + action))));
                player.sendSystemMessage(menu);
            }
            save(); return 1;
        } catch (RuntimeException e) { source.sendFailure(Component.literal("EnhancedTimer: " + e.getMessage())); return 0; }
    }
    public void event(ServerPlayer player, String event) {
        hub.event(player.getUUID(), event);
        if (event.equals("death") && rules(player).spectatorOnDeath()) player.setGameMode(GameType.SPECTATOR);
    }
    public void tick() {
        if (++ticks % 2 != 0) return;
        var online = server.getPlayerList().getPlayers();
        var ids = new HashSet<UUID>(); for (var player : online) ids.add(player.getUUID());
        clients.retainAll(ids); hidden.retainAll(ids); hudClients.retainAll(ids); actionbars.retainAll(ids); advertised.retainAll(ids); positions.keySet().retainAll(ids); requests.retain(ids);
        bars.entrySet().removeIf(entry -> { if (ids.contains(entry.getKey())) return false; return true; });
        var warnings = new HashMap<String, Long>(); var justExpired = new HashSet<String>();
        for (var entry : hub.sessions().entrySet()) {
            var session = entry.getValue(); boolean hasViewer = online.stream().anyMatch(p -> hub.selected(p.getUUID()).equals(entry.getKey()));
            session.timer.suspend(options.pauseWhenEmpty && !hasViewer || server.isPaused());
            var thresholds = session.warnings(options.warningMillis);
            if (!thresholds.isEmpty()) warnings.put(entry.getKey(), thresholds.stream().mapToLong(Long::longValue).min().orElse(0));
            boolean finished = session.timer.state() == TimerEngine.State.FINISHED;
            if (finished && !expired.getOrDefault(entry.getKey(), true)) justExpired.add(entry.getKey());
            expired.put(entry.getKey(), finished);
        }
        for (var player : online) {
            UUID id = player.getUUID(); String scope = hub.selected(id); var session = hub.session(scope); var timer = session.timer;
            if (advertised.add(id)) player.connection.send(new ClientboundCustomPayloadPacket(new dev.enhancedtimer.network.ChannelRegistration()));
            var previous = positions.put(id, player.position());
            if (previous != null && previous.distanceToSqr(player.position()) > 0.0001) session.event("move", hub.rules(scope));
            if (paused(player) && rules(player).freezePaused() && previous != null) { player.teleportTo(previous.x, previous.y, previous.z); positions.put(id, previous); }
            if (paused(player) && rules(player).blindPaused() && !player.hasEffect(MobEffects.BLINDNESS)) player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 8, 0, false, false));
            if (ticks % 10 == 0 && clients.contains(id)) send(player, hub.state(id, administrator(player), ++sequence));
            if (warnings.containsKey(scope) && !hudClients.contains(id)) {
                player.sendSystemMessage(Component.literal(ServerLanguage.text(player.clientInformation().language(), "warning.remaining")
                        .replace("{time}", TimeFormat.AUTO.format(warnings.get(scope), true))).withStyle(style -> style.withColor(0xFFFF55)));
                alert(player, 1);
            }
            if (justExpired.contains(scope)) {
                if (!hudClients.contains(id)) {
                    player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 100, 20));
                    player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(ServerLanguage.text(player.clientInformation().language(), "expired")).withStyle(style -> style.withColor(0xFF5555))));
                    alert(player, 0.65f);
                }
                if (hub.rules(scope).spectatorOnExpiry()) player.setGameMode(GameType.SPECTATOR);
            }
            if (!options.bossbar || timer.state() == TimerEngine.State.READY && options.clock.equals("TIMER") || hidden.contains(id) || hudClients.contains(id)) {
                var old = bars.remove(id); if (old != null) old.removePlayer(player);
                if (actionbars.remove(id)) player.connection.send(new ClientboundSetActionBarTextPacket(Component.empty()));
                continue;
            }
            if (options.display.equals("ACTIONBAR")) {
                var old = bars.remove(id); if (old != null) old.removePlayer(player);
                player.connection.send(new ClientboundSetActionBarTextPacket(serverText(player, scope, timer))); actionbars.add(id); continue;
            }
            var bar = bars.computeIfAbsent(id, key -> {
                var created = new ServerBossEvent(UUID.randomUUID(), Component.literal("Enhanced Timer"), BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
                created.addPlayer(player); return created;
            });
            bar.setName(serverText(player, scope, timer));
            bar.setProgress(timer.mode() == TimerEngine.Mode.COUNTDOWN ? Math.clamp((float)timer.displayMillis() / timer.durationMillis(), 0, 1) : 1);
            bar.setColor(timer.state() == TimerEngine.State.FINISHED ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.GREEN);
        }
        if (ticks % 200 == 0 || !justExpired.isEmpty()) save();
    }
    private Component serverText(ServerPlayer player, String scope, TimerEngine timer) {
        String pattern = timer.state() == TimerEngine.State.FINISHED ? options.finishedFormat : paused(player) ? options.pausedFormat : options.format;
        String text = TimeFormat.TEMPLATE.format(timer.displayMillis(), timer.mode() == TimerEngine.Mode.COUNTDOWN, pattern);
        if (options.clock.equals("REAL")) text = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        if (options.clock.equals("GAME")) text = TimeFormat.CLOCK.format(Math.floorMod(player.level().getOverworldClockTime() + 6000, 24000) * 3600, false);
        Component component = Component.literal(text);
        if (text.contains("<")) try {
            component = ComponentSerialization.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,
                    com.google.gson.JsonParser.parseString(dev.enhancedtimer.text.RichText.json(dev.enhancedtimer.text.RichText.animate(text, System.nanoTime())))).result().orElse(component);
        } catch (RuntimeException ignored) { }
        int color = timer.state() == TimerEngine.State.FINISHED ? 0xFF5555 : paused(player) ? 0xAAAAAA : options.color.argb(options.customColor, System.nanoTime(), 0) & 0xFFFFFF;
        return component.copy().withStyle(style -> style.getColor() == null ? style.withColor(color) : style).append(Component.literal(" · " + ServerLanguage.scope(player.clientInformation().language(), scope)).withStyle(style -> style.withColor(0xAAAAAA)));
    }
    private void alert(ServerPlayer player, float pitch) {
        if (options.sound) player.connection.send(new ClientboundSoundPacket(net.minecraft.core.Holder.direct(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP),
                net.minecraft.sounds.SoundSource.MASTER, player.getX(), player.getY(), player.getZ(), 0.8f, pitch, 0));
    }
    public void save() { try { TimerArchive.save(data, hub); } catch (java.io.IOException e) { System.getLogger("Enhanced Timer").log(System.Logger.Level.ERROR, "Cannot save server timers", e); } }
    public void close() { for (var session : hub.sessions().values()) session.timer.pause(); save(); synchronized (SERVERS) { SERVERS.remove(server); } }
}
