package dev.enhancedtimer.paper;

import dev.enhancedtimer.core.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.potion.*;

/** Bukkit-only UI and messaging. Timer ownership stays on the server/global thread. */
public final class EnhancedTimerPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter, PluginMessageListener {
    public record View(String scope, TimerEngine.Snapshot timer, boolean suspended, TimerSession.Rules rules,
                       List<TimerSession.Split> splits, boolean owner) {
        boolean paused() { return timer.state() == TimerEngine.State.PAUSED || suspended; }
        long remaining() { return Math.max(0, timer.durationMillis() - timer.elapsedMillis()); }
    }
    private final TimerHub hub = new TimerHub();
    private final ServerCommands commands = new ServerCommands(hub);
    private final Map<UUID, View> views = new ConcurrentHashMap<>();
    private final Set<UUID> clients = ConcurrentHashMap.newKeySet(), hidden = ConcurrentHashMap.newKeySet(), hudClients = ConcurrentHashMap.newKeySet(), pendingSpectator = ConcurrentHashMap.newKeySet();
    private final Map<UUID, BossBar> bars = new ConcurrentHashMap<>();
    private final Set<UUID> actionbars = ConcurrentHashMap.newKeySet();
    private final RequestLimiter requests = new RequestLimiter();
    private final Map<String, Boolean> expired = new HashMap<>();
    private SchedulerBridge scheduler;
    private int updates;
    private long sequence;
    public TimerHub getTimerHub() { return hub; }
    public void executeOnTimerThread(java.util.function.Consumer<TimerHub> action) { scheduler.global(() -> action.accept(hub)); }
    private Path archive() { return getDataFolder().toPath().resolve("timers.dat"); }
    @Override public void onEnable() {
        saveDefaultConfig(); scheduler = new SchedulerBridge(this);
        long seconds = Math.max(1, Math.min(TimerEngine.MAX_DURATION / 1000, getConfig().getLong("countdown-seconds", 7200)));
        hub.session("global").timer.configure(TimerEngine.Mode.STOPWATCH, seconds * 1000);
        try {
            if (!Files.exists(archive()) && new java.io.File(getDataFolder(), "timer.yml").exists()) {
                var old = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(new java.io.File(getDataFolder(), "timer.yml"));
                hub.session("global").timer.restore(new TimerEngine.Snapshot(TimerEngine.Mode.valueOf(old.getString("mode", "STOPWATCH")),
                        TimerEngine.State.valueOf(old.getString("state", "READY")), old.getLong("elapsed-ms"), old.getLong("duration-ms", 7_200_000)));
                saveTimers();
            } else TimerArchive.load(archive(), hub);
        }
        catch (IOException | IllegalArgumentException e) {
            try { Files.copy(archive(), archive().resolveSibling("timers.broken-" + System.currentTimeMillis())); } catch (IOException ignored) { }
            getLogger().warning("Cannot read timers: " + e.getMessage());
        }
        getServer().getServicesManager().register(TimerHub.class, hub, this, ServicePriority.Normal);
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("ets")).setExecutor(this); getCommand("ets").setTabCompleter(this);
        getServer().getMessenger().registerIncomingPluginChannel(this, SyncProtocol.CHANNEL, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, SyncProtocol.CHANNEL);
        scheduler.repeat(this::update, 2);
    }
    @Override public void onDisable() {
        if (scheduler != null) scheduler.close();
        for (var session : hub.sessions().values()) session.timer.pause(); saveTimers();
        for (var bar : bars.values()) bar.removeAll(); bars.clear(); views.clear();
        getServer().getServicesManager().unregisterAll(this);
        getServer().getMessenger().unregisterIncomingPluginChannel(this); getServer().getMessenger().unregisterOutgoingPluginChannel(this);
    }
    private void update() {
        var players = List.copyOf(getServer().getOnlinePlayers()); var online = new HashSet<UUID>();
        for (var player : players) online.add(player.getUniqueId());
        var warnings = new HashMap<String, Long>(); var finished = new HashSet<String>();
        long[] thresholds = getConfig().getLongList("warning-seconds").stream().filter(n -> n > 0 && n <= 31_536_000).mapToLong(n -> n * 1000).toArray();
        for (var entry : hub.sessions().entrySet()) {
            var session = entry.getValue(); boolean participants = online.stream().anyMatch(id -> hub.selected(id).equals(entry.getKey()));
            session.timer.suspend(getConfig().getBoolean("pause-when-empty", true) && !participants);
            var crossed = session.warnings(thresholds);
            if (!crossed.isEmpty()) warnings.put(entry.getKey(), crossed.stream().mapToLong(Long::longValue).min().orElse(0));
            boolean done = session.timer.state() == TimerEngine.State.FINISHED;
            if (done && !expired.getOrDefault(entry.getKey(), true)) finished.add(entry.getKey());
            expired.put(entry.getKey(), done);
        }
        boolean syncNow = updates % 5 == 0;
        for (var player : players) {
            UUID id = player.getUniqueId(); String scope = hub.selected(id); var session = hub.session(scope);
            var view = new View(scope, session.timer.snapshot(), session.timer.suspended(), hub.rules(scope), session.splits(), hub.canControl(id, scope, false));
            views.put(id, view); long seq = ++sequence;
            scheduler.player(player, () -> {
                if (!player.hasPermission("enhancedtimer.view")) { hideBar(player); return; }
                if (clients.contains(id) && syncNow) for (byte[] bytes : SyncProtocol.encodePackets(new SyncProtocol.Message(
                        SyncProtocol.Kind.STATE, scope, "", 0, view.timer(), view.suspended(), view.owner() || player.hasPermission("enhancedtimer.control"), seq, view.splits())))
                    player.sendPluginMessage(this, SyncProtocol.CHANNEL, bytes);
                updateBar(player, view);
                if (view.paused() && view.rules().blindPaused() && !player.hasPotionEffect(PotionEffectType.BLINDNESS))
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 8, 0, false, false));
                if (warnings.containsKey(scope) && !hudClients.contains(id)) {
                    player.sendMessage("§eEnhancedTimer: " + TimeFormat.AUTO.format(warnings.get(scope), true));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.7f, 1);
                }
                if (finished.contains(scope)) {
                    if (!hudClients.contains(id)) {
                        player.sendTitle("§c" + msg(player, "expired"), "Enhanced Timer", 10, 100, 20);
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 0.65f);
                    }
                    if (view.rules().spectatorOnExpiry()) player.setGameMode(GameMode.SPECTATOR);
                }
            });
        }
        clients.retainAll(online); hidden.retainAll(online); hudClients.retainAll(online); actionbars.retainAll(online); views.keySet().retainAll(online); requests.retain(online);
        if (++updates % 100 == 0 || !finished.isEmpty()) saveTimers();
    }
    private void updateBar(Player player, View view) {
        UUID id = player.getUniqueId();
        if (!getConfig().getBoolean("show-bossbar", true) || hidden.contains(id) || hudClients.contains(id) || view.timer().state() == TimerEngine.State.READY && getConfig().getString("clock", "TIMER").equals("TIMER")) { hideBar(player); return; }
        long millis = view.timer().mode() == TimerEngine.Mode.COUNTDOWN ? view.remaining() : view.timer().elapsedMillis();
        String pattern = getConfig().getString(view.timer().state() == TimerEngine.State.FINISHED ? "finished-format" : view.paused() ? "paused-format" : "format", "{d}d {hh}:{mm}:{ss}");
        String text = TimeFormat.TEMPLATE.format(millis, view.timer().mode() == TimerEngine.Mode.COUNTDOWN, pattern);
        String clock = getConfig().getString("clock", "TIMER");
        if (clock.equals("REAL")) text = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        if (clock.equals("GAME")) text = TimeFormat.CLOCK.format(Math.floorMod(player.getWorld().getTime() + 6000, 24000) * 3600, false);
        String styled = text.contains("<") ? dev.enhancedtimer.text.RichText.legacy(dev.enhancedtimer.text.RichText.animate(text, System.nanoTime())) : colorText(view, text);
        if (getConfig().getString("display", "BOSSBAR").equals("ACTIONBAR")) {
            var old = bars.remove(id); if (old != null) old.removePlayer(player);
            sendActionbar(player, styled); actionbars.add(id); return;
        }
        var bar = bars.computeIfAbsent(id, ignored -> { var created = Bukkit.createBossBar("Enhanced Timer", BarColor.GREEN, BarStyle.SOLID); created.addPlayer(player); return created; });
        bar.setTitle(styled + " §7· " + ServerLanguage.scope(player.getLocale(), view.scope()));
        bar.setProgress(view.timer().mode() == TimerEngine.Mode.COUNTDOWN ? Math.clamp((double)millis / view.timer().durationMillis(), 0, 1) : 1);
        bar.setColor(view.timer().state() == TimerEngine.State.FINISHED ? BarColor.RED : view.paused() ? BarColor.WHITE : BarColor.GREEN);
    }
    private String colorText(View view, String text) {
        if (view.timer().state() == TimerEngine.State.FINISHED) return "§c" + text;
        if (view.paused()) return "§7" + text;
        TimerColor mode;
        try { mode = TimerColor.valueOf(getConfig().getString("color", "GOLD").toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException e) { mode = TimerColor.GOLD; }
        var result = new StringBuilder(); long now = System.nanoTime(); int index = 0;
        for (int offset = 0; offset < text.length();) {
            int cp = text.codePointAt(offset); int rgb = mode.argb(getConfig().getInt("custom-color", 0xFFAA00), now, index++);
            result.append("§x"); for (char c : String.format(Locale.ROOT, "%06X", rgb & 0xFFFFFF).toCharArray()) result.append('§').append(c);
            result.appendCodePoint(cp); offset += Character.charCount(cp);
        }
        return result.toString();
    }
    private void sendActionbar(Player player, String text) {
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText(text));
    }
    private void hideBar(Player player) {
        var bar = bars.remove(player.getUniqueId()); if (bar != null) bar.removePlayer(player);
        if (actionbars.remove(player.getUniqueId())) sendActionbar(player, "");
    }
    @Override public void onPluginMessageReceived(String channel, Player player, byte[] bytes) {
        if (!channel.equals(SyncProtocol.CHANNEL) || !player.hasPermission("enhancedtimer.view")) return;
        if (!requests.allow(player.getUniqueId())) return;
        try {
            var message = SyncProtocol.decode(bytes); UUID id = player.getUniqueId(); boolean admin = player.hasPermission("enhancedtimer.control");
            scheduler.global(() -> {
                try {
                    hub.request(id, admin, message); clients.add(id);
                    if (message.kind() == SyncProtocol.Kind.SELECT) hudClients.add(id);
                    if (message.kind() == SyncProtocol.Kind.HELLO && message.action().equals("local")) hudClients.remove(id);
                    var response = hub.state(id, admin, ++sequence);
                    scheduler.player(player, () -> { for (byte[] data : SyncProtocol.encodePackets(response)) player.sendPluginMessage(this, SyncProtocol.CHANNEL, data); });
                    if (message.kind() != SyncProtocol.Kind.HELLO) saveTimers();
                } catch (RuntimeException e) { reply(player, "§cEnhancedTimer: " + e.getMessage()); }
            });
        } catch (IOException ignored) { }
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("enhancedtimer.view")) return true;
        if (sender instanceof Player p && args.length == 0) { scheduler.player(p, () -> open(p)); return true; }
        if (sender instanceof Player p && args.length == 1 && Set.of("hide", "show").contains(args[0])) {
            if (args[0].equals("hide")) { hidden.add(p.getUniqueId()); hideBar(p); } else hidden.remove(p.getUniqueId()); return true;
        }
        boolean admin = sender.hasPermission("enhancedtimer.control"); UUID id = sender instanceof Player p ? p.getUniqueId() : ServerCommands.CONSOLE;
        scheduler.global(() -> {
            try { reply(sender, "§a[EnhancedTimer] " + commands.execute(id, admin, args)); saveTimers(); }
            catch (RuntimeException e) { reply(sender, "§cEnhancedTimer: " + e.getMessage()); }
        }); return true;
    }
    private void reply(CommandSender sender, String text) { if (sender instanceof Player p) scheduler.player(p, () -> p.sendMessage(text)); else scheduler.global(() -> sender.sendMessage(text)); }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        var choices = args.length == 1 ? List.of("start", "pause", "stop", "reset", "undo", "timer", "countdown", "set", "split", "splits", "personal", "global", "team", "rules", "hide", "show")
                : args.length == 2 && args[0].equals("countdown") ? List.of("30m", "2h", "2d")
                : args.length == 2 && args[0].equals("team") ? List.of("create", "join", "leave", "delete", "list")
                : args.length == 2 && args[0].equals("rules") ? List.of("start-move", "pause-death", "stop-dragon", "splits", "freeze", "protect", "blind", "death-spectator", "expiry-spectator")
                : args.length == 3 && args[0].equals("rules") ? List.of("true", "false") : List.<String>of();
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT); return choices.stream().filter(s -> s.startsWith(prefix)).toList();
    }
    private static final class TimerMenu implements InventoryHolder {
        private final Inventory inventory = Bukkit.createInventory(this, 36, "Enhanced Timer");
        @Override public Inventory getInventory() { return inventory; }
    }
    private void open(Player player) {
        var inv = new TimerMenu().inventory; var view = views.get(player.getUniqueId());
        item(inv, 4, Material.CLOCK, view == null ? "Enhanced Timer" : ServerLanguage.scope(player.getLocale(), view.scope()));
        item(inv, 10, Material.LIME_DYE, msg(player, "start")); item(inv, 11, Material.YELLOW_DYE, msg(player, "pause"));
        item(inv, 12, Material.RED_DYE, msg(player, "stop")); item(inv, 13, Material.MILK_BUCKET, msg(player, "reset"));
        item(inv, 14, Material.REPEATER, msg(player, "undo")); item(inv, 15, Material.COMPARATOR, msg(player, "mode")); item(inv, 16, Material.CLOCK, msg(player, "duration"));
        item(inv, 19, Material.PLAYER_HEAD, msg(player, "personal")); item(inv, 20, Material.BEACON, msg(player, "shared"));
        item(inv, 21, Material.PAPER, msg(player, "split")); item(inv, 22, Material.ENDER_EYE, msg(player, "visibility")); item(inv, 23, Material.BOOK, msg(player, "splits"));
        item(inv, 24, Material.IRON_BARS, msg(player, "freeze")); item(inv, 25, Material.SHIELD, msg(player, "protect")); item(inv, 28, Material.INK_SAC, msg(player, "blind"));
        item(inv, 29, Material.DRAGON_HEAD, msg(player, "stop-dragon")); item(inv, 30, Material.SKELETON_SKULL, msg(player, "death-spectator")); item(inv, 31, Material.END_CRYSTAL, msg(player, "expiry-spectator"));
        player.openInventory(inv);
    }
    private void item(Inventory inventory, int slot, Material material, String name) { var item = new ItemStack(material); var meta = item.getItemMeta(); meta.setDisplayName("§f" + name); item.setItemMeta(meta); inventory.setItem(slot, item); }
    @EventHandler public void click(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof TimerMenu)) return; event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return; var view = views.get(player.getUniqueId()); if (view == null) return;
        String action = switch (event.getRawSlot()) {
            case 10 -> "start"; case 11 -> "pause"; case 12 -> "stop"; case 13 -> "reset"; case 14 -> "undo";
            case 15 -> view.timer().mode() == TimerEngine.Mode.COUNTDOWN ? "timer" : "countdown 2h";
            case 16 -> "countdown " + (view.timer().durationMillis() >= 10_800_000 ? "15m" : (view.timer().durationMillis() / 60_000 + 15) + "m");
            case 19 -> "personal"; case 20 -> "global"; case 21 -> "split"; case 22 -> hidden.contains(player.getUniqueId()) ? "show" : "hide"; case 23 -> "splits";
            case 24 -> "rules freeze " + !view.rules().freezePaused(); case 25 -> "rules protect " + !view.rules().protectPaused(); case 28 -> "rules blind " + !view.rules().blindPaused();
            case 29 -> "rules stop-dragon " + !view.rules().stopOnDragon(); case 30 -> "rules death-spectator " + !view.rules().spectatorOnDeath(); case 31 -> "rules expiry-spectator " + !view.rules().spectatorOnExpiry(); default -> null;
        };
        if (action != null) {
            onCommand(player, Objects.requireNonNull(getCommand("ets")), "ets", action.split(" "));
            scheduler.player(player, () -> { if (player.getOpenInventory().getTopInventory().getHolder() instanceof TimerMenu) open(player); });
        }
    }
    @EventHandler public void drag(InventoryDragEvent event) { if (event.getView().getTopInventory().getHolder() instanceof TimerMenu) event.setCancelled(true); }
    @EventHandler public void quit(PlayerQuitEvent event) { hideBar(event.getPlayer()); clients.remove(event.getPlayer().getUniqueId()); }
    @EventHandler(ignoreCancelled = true) public void move(PlayerMoveEvent event) {
        if (event instanceof PlayerTeleportEvent) return; var view = views.get(event.getPlayer().getUniqueId()); if (view == null || event.getTo() == null) return;
        boolean moved = event.getFrom().getX() != event.getTo().getX() || event.getFrom().getY() != event.getTo().getY() || event.getFrom().getZ() != event.getTo().getZ();
        if (!moved) return;
        if (view.paused() && view.rules().freezePaused()) event.setCancelled(true);
        else if (view.rules().startOnMove() && view.timer().state() == TimerEngine.State.READY && getConfig().getString("clock", "TIMER").equals("TIMER")) { UUID id = event.getPlayer().getUniqueId(); scheduler.global(() -> hub.event(id, "move")); }
    }
    @EventHandler(ignoreCancelled = true) public void damage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player p) { var view = views.get(p.getUniqueId()); if (view != null && view.paused() && view.rules().protectPaused()) event.setCancelled(true); }
    }
    @EventHandler public void death(PlayerDeathEvent event) { UUID id = event.getEntity().getUniqueId(); var view = views.get(id); if (view != null && view.rules().spectatorOnDeath()) pendingSpectator.add(id); scheduler.global(() -> hub.event(id, "death")); }
    @EventHandler public void respawn(PlayerRespawnEvent event) { if (pendingSpectator.remove(event.getPlayer().getUniqueId())) scheduler.player(event.getPlayer(), () -> event.getPlayer().setGameMode(GameMode.SPECTATOR)); }
    @EventHandler public void advancement(PlayerAdvancementDoneEvent event) {
        String milestone = switch (event.getAdvancement().getKey().toString()) { case "minecraft:story/enter_the_nether" -> "nether"; case "minecraft:story/enter_the_end" -> "end"; case "minecraft:end/kill_dragon" -> "dragon"; default -> null; };
        if (milestone != null) { UUID id = event.getPlayer().getUniqueId(); scheduler.global(() -> hub.event(id, milestone)); }
    }
    private void saveTimers() { try { TimerArchive.save(archive(), hub); } catch (IOException e) { getLogger().warning("Cannot save timers: " + e.getMessage()); } }
    private String msg(Player player, String key) { String lang = getConfig().getString("language", "auto"); return Messages.text(lang.equals("auto") ? player.getLocale() : lang, key); }
}
