package dev.enhancedtimer.client;

import dev.enhancedtimer.core.*;
import dev.enhancedtimer.client.ui.*;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.storage.LevelResource;

public final class EnhancedTimerClient {
    private static EnhancedTimerClient instance;
    public final TimerSession local = new TimerSession();
    public final TimerEngine timer = local.timer;
    public final ClientSync sync = new ClientSync();
    public Settings settings;
    private Settings globalSettings;
    private String previousDimension;
    private net.minecraft.world.phys.Vec3 previousPosition;
    private boolean wasDead;
    private boolean advancementsSeeded;
    private final java.util.Set<String> knownAdvancements = new java.util.HashSet<>();
    private final Path directory;
    private String scope;
    private boolean announced;
    private int ticks;
    private String pendingMenu;

    private EnhancedTimerClient() {
        directory = Minecraft.getInstance().gameDirectory.toPath().resolve("config/enhancedtimer");
        settings = JsonStore.read(directory.resolve("settings.json"), Settings.class, new Settings());
        settings.sanitize();
        globalSettings = JsonStore.copy(settings, Settings.class);
        timer.configure(settings.defaultMode, settings.durationMillis);
    }
    public static EnhancedTimerClient get() {
        if (instance == null) instance = new EnhancedTimerClient();
        return instance;
    }
    public boolean inWorld() { return Minecraft.getInstance().level != null; }
    private String worldKey() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        if (mc.getSingleplayerServer() != null)
            return "world:" + mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        if (mc.getCurrentServer() != null) return "server:" + mc.getCurrentServer().ip.toLowerCase(java.util.Locale.ROOT);
        return "connection:" + mc.getConnection().getConnection().getRemoteAddress();
    }
    private Path sessionPath(String key) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
            return directory.resolve("sessions/" + HexFormat.of().formatHex(hash) + ".json");
        } catch (java.security.NoSuchAlgorithmException e) { throw new AssertionError(e); }
    }
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        String next = worldKey();
        if (!java.util.Objects.equals(scope, next)) {
            timer.pause();
            save();
            scope = next;
            sync.reset();
            previousDimension = null; previousPosition = null; wasDead = false;
            knownAdvancements.clear(); advancementsSeeded = false;
            settings = scope == null ? JsonStore.copy(globalSettings, Settings.class)
                    : JsonStore.read(profilePath(scope), Settings.class, JsonStore.copy(globalSettings, Settings.class));
            settings.sanitize(); settings.language = globalSettings.language;
            local.configure(settings.defaultMode, settings.durationMillis);
            local.forgetHistory();
            if (scope != null) {
                var saved = JsonStore.read(sessionPath(scope), TimerSession.Saved.class, null);
                if (saved != null) try { local.load(saved); }
                catch (IllegalArgumentException | NullPointerException e) {
                    var legacy = JsonStore.read(sessionPath(scope), TimerEngine.Snapshot.class, null);
                    if (legacy != null) try { timer.restore(legacy); } catch (RuntimeException invalid) {
                        System.getLogger("Enhanced Timer").log(System.Logger.Level.WARNING, "Invalid saved timer; resetting", invalid);
                        timer.configure(settings.defaultMode, settings.durationMillis);
                    }
                }
            }
            announced = timer.state() == TimerEngine.State.FINISHED;
        }
        timer.suspend(next == null || (settings.pauseWithGame && mc.isPaused())
                || (settings.pauseInMenus && Platform.screen() != null));
        sync.tick(ticks);
        if (inWorld()) localEvents();
        var active = activeSession();
        var activeTimer = active.timer;
        if (activeTimer.state() != TimerEngine.State.FINISHED) announced = false;
        if (settings.warnings) {
            var warnings = active.warnings(settings.warningMillis);
            if (!warnings.isEmpty()) {
                long value = warnings.stream().mapToLong(Long::longValue).min().orElse(0);
                HudRenderer.notice = I18n.text("warning.remaining").replace("{time}", TimeFormat.AUTO.format(value, true));
                HudRenderer.noticeUntil = System.nanoTime() + 2_000_000_000L;
                if (settings.alertSound) playAlert();
            }
        }
        if (activeTimer.state() == TimerEngine.State.FINISHED && !announced) {
            announced = true;
            HudRenderer.expiredUntil = System.nanoTime() + 6_000_000_000L;
            if (settings.alertSound) playAlert();
            save();
        }
        while (Keys.MENU.consumeClick()) {
            if (!(Platform.screen() instanceof TimerScreen)) Platform.screen(new TimerScreen(Platform.screen()));
        }
        while (Keys.TOGGLE.consumeClick()) if (inWorld() && Platform.screen() == null) action("toggle");
        while (Keys.STOP.consumeClick()) if (inWorld() && Platform.screen() == null) action("stop");
        if (pendingMenu != null) {
            String menu = pendingMenu; pendingMenu = null;
            Platform.screen(menu.equals("edit") ? new LayoutScreen(null) : new TimerScreen(null));
        }
        if (++ticks % 200 == 0 && scope != null) save();
    }
    public void configure(TimerEngine.Mode mode, long duration) {
        if (usingServer()) { sync.configure(mode, duration); return; }
        settings.defaultMode = mode;
        settings.durationMillis = duration;
        local.configure(mode, duration);
        announced = false;
        HudRenderer.expiredUntil = 0;
        save();
    }
    public void action(String action) {
        if (usingServer()) { sync.action(action); return; }
        local.action(action);
        if (action.equals("undo")) { settings.defaultMode = timer.mode(); settings.durationMillis = timer.durationMillis(); }
        if (timer.state() != TimerEngine.State.FINISHED) { announced = false; HudRenderer.expiredUntil = 0; }
        save();
    }
    public void save() {
        if (scope == null) globalSettings = JsonStore.copy(settings, Settings.class);
        else {
            JsonStore.write(profilePath(scope), settings);
            globalSettings.language = settings.language;
            JsonStore.write(sessionPath(scope), local.save());
        }
        JsonStore.write(directory.resolve("settings.json"), globalSettings);
    }
    public void renderHud(GuiGraphicsExtractor graphics) {
        if (inWorld() && settings.visible && !Platform.hudHidden() && !(Platform.screen() instanceof LayoutScreen))
            HudRenderer.draw(graphics, false);
        if (inWorld() && System.nanoTime() < HudRenderer.expiredUntil) HudRenderer.expired(graphics);
        if (inWorld() && System.nanoTime() < HudRenderer.noticeUntil) HudRenderer.notice(graphics);
    }
    public boolean command(String text) {
        String[] args = text.strip().split("\\s+");
        if (!args[0].equals("/et") && !args[0].equals("/enhancedtimer")) return false;
        Minecraft mc = Minecraft.getInstance();
        String action = args.length > 1 ? args[1].toLowerCase(java.util.Locale.ROOT) : "menu";
        // ChatScreen closes itself after this method; defer opening the menu to the next tick.
        if (action.equals("menu") || action.equals("edit")) {
            pendingMenu = action;
        } else if (action.equals("countdown") && args.length == 3) {
            try { configure(TimerEngine.Mode.COUNTDOWN, Durations.parse(args[2])); action("start"); }
            catch (IllegalArgumentException e) { feedback(I18n.text("error.duration")); }
        } else if (action.equals("timer") && args.length == 2) {
            configure(TimerEngine.Mode.STOPWATCH, settings.durationMillis); action("start");
        } else if (action.equals("undo") && args.length == 2) { action("undo");
        } else if (action.equals("set") && args.length == 3) {
            try { setDisplay(args[2].equals("0") ? 0 : Durations.parse(args[2])); }
            catch (IllegalArgumentException e) { feedback(I18n.text("error.duration")); }
        } else if (action.equals("split")) {
            try {
                addSplit(args.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : "Split " + (activeSession().splits().size() + 1));
            } catch (RuntimeException e) { feedback(e.getMessage()); }
        } else if (action.equals("splits")) {
            for (var split : activeSession().splits()) feedback(split.name() + ": " + TimeFormat.PRECISE.format(split.elapsedMillis(), false));
        } else if (action.equals("design") && args.length == 4) {
            try { design(args[2], args[3]); } catch (RuntimeException e) { feedback(e.getMessage()); }
        } else if (action.equals("source") && args.length == 3) {
            if (args[2].equals("server")) sync.select("global");
            else if (args[2].equals("local")) sync.useLocal();
            else feedback("/et source <local|server>");
        } else if (java.util.Set.of("start", "pause", "stop", "reset").contains(action) && args.length == 2) {
            action(action); feedback(I18n.text("state." + activeTimer().state().name().toLowerCase(java.util.Locale.ROOT)));
        } else feedback(I18n.text("commands.help"));
        return true;
    }
    public boolean usingServer() { return settings.serverHud && sync.connected; }
    public TimerSession activeSession() { return usingServer() ? sync.session : local; }
    public TimerEngine activeTimer() { return activeSession().timer; }
    public boolean canControl() { return !usingServer() || sync.canControl; }
    public String profileName() { return scope == null ? I18n.text("profile.defaults") : I18n.text("profile.current"); }
    private Path profilePath(String key) { return directory.resolve("profiles").resolve(sessionPath(key).getFileName()); }
    public void design(String action, String name) {
        if (!name.matches("[a-zA-Z0-9_-]{1,40}")) throw new IllegalArgumentException(I18n.text("error.name"));
        Path path = directory.resolve("designs/" + name + ".json");
        if (action.equals("export")) { var design = new Settings(); design.applyDesign(settings); JsonStore.write(path, design); }
        else if (action.equals("import")) {
            var imported = JsonStore.read(path, Settings.class, null);
            if (imported == null) throw new IllegalArgumentException(I18n.text("error.file"));
            settings.applyDesign(imported); save();
        } else throw new IllegalArgumentException("/et design <import|export> <name>");
        feedback(path.toString());
    }
    public void exportSplits() {
        StringBuilder csv = new StringBuilder("name,elapsed_ms\n");
        for (var split : activeSession().splits()) csv.append('"').append(split.name().replace("\"", "\"\"")).append("\",").append(split.elapsedMillis()).append('\n');
        try {
            var target = directory.resolve("exports/splits-" + System.currentTimeMillis() + ".csv");
            java.nio.file.Files.createDirectories(target.getParent()); java.nio.file.Files.writeString(target, csv); feedback(target.toString());
        } catch (java.io.IOException e) { feedback(e.getMessage()); }
    }
    public void addSplit(String name) {
        if (usingServer()) sync.action("split:" + name);
        else { local.split(name); save(); }
    }
    public void setDisplay(long millis) {
        if (usingServer()) sync.action("set:" + millis);
        else { local.setDisplay(millis); settings.durationMillis = timer.durationMillis(); save(); }
    }
    public void playAlert() {
        var sound = switch (settings.alertTone) {
            case "BELL" -> SoundEvents.NOTE_BLOCK_BELL.value();
            case "CHIME" -> SoundEvents.NOTE_BLOCK_CHIME.value();
            case "LEVELUP" -> SoundEvents.PLAYER_LEVELUP;
            default -> SoundEvents.EXPERIENCE_ORB_PICKUP;
        };
        if (!settings.alertTone.equals("NONE")) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, 1));
    }
    private void localEvents() {
        var mc = Minecraft.getInstance(); if (mc.player == null || mc.level == null) return;
        var position = mc.player.position(); String dimension = mc.level.dimension().identifier().toString();
        if (previousPosition != null && previousPosition.distanceToSqr(position) > 0.0001) local.event("move", settings.rules());
        if (previousDimension != null && !previousDimension.equals(dimension)) {
            if (dimension.equals("minecraft:the_nether")) local.event("nether", settings.rules());
            if (dimension.equals("minecraft:the_end")) local.event("end", settings.rules());
        }
        boolean dead = mc.player.isDeadOrDying(); if (dead && !wasDead) local.event("death", settings.rules());
        previousPosition = position; previousDimension = dimension; wasDead = dead;
        if (ticks % 20 == 0 && mc.getConnection() != null) {
            for (var entry : ((dev.enhancedtimer.client.mixin.AdvancementsAccessor)(Object)mc.getConnection().getAdvancements()).enhancedtimer$progress().entrySet()) if (entry.getValue().isDone()) {
                String id = entry.getKey().id().toString();
                if (knownAdvancements.add(id) && advancementsSeeded && id.equals("minecraft:end/kill_dragon")) local.event("dragon", settings.rules());
            }
            advancementsSeeded = true;
        }
    }
    public static void feedback(String text) {
        var player = Minecraft.getInstance().player;
        if (player != null) player.sendSystemMessage(Component.literal("[Enhanced Timer] " + text));
    }
}

