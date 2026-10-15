package dev.enhancedtimer.paper;

import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** Keeps all Paper/Folia linkage behind an optional runtime boundary. */
final class SchedulerBridge {
    private final Plugin plugin;
    private final boolean folia;
    private volatile boolean enabled = true;
    SchedulerBridge(Plugin plugin) {
        this.plugin = plugin; boolean found;
        try { Class.forName("io.papermc.paper.threadedregions.RegionizedServer"); found = true; }
        catch (ClassNotFoundException e) { found = false; }
        folia = found;
    }
    private Object call(String api, Object object, String name, Class<?>[] types, Object... args) {
        try { return Class.forName(api).getMethod(name, types).invoke(object, args); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("Unsupported scheduler API: " + name, e); }
    }
    private Object globalScheduler() { return call("org.bukkit.Server", Bukkit.getServer(), "getGlobalRegionScheduler", new Class[0]); }
    void global(Runnable action) {
        if (!enabled) return;
        Runnable safe = () -> { if (enabled) action.run(); };
        if (folia) {
            if ((Boolean)call("org.bukkit.Bukkit", null, "isGlobalTickThread", new Class[0])) safe.run();
            else call("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler", globalScheduler(), "execute", new Class[]{Plugin.class, Runnable.class}, plugin, safe);
        }
        else if (Bukkit.isPrimaryThread()) safe.run(); else Bukkit.getScheduler().runTask(plugin, safe);
    }
    void player(Player player, Runnable action) {
        if (!enabled) return;
        if (folia) {
            Object scheduler = call("org.bukkit.entity.Entity", player, "getScheduler", new Class[0]);
            call("io.papermc.paper.threadedregions.scheduler.EntityScheduler", scheduler, "execute", new Class[]{Plugin.class, Runnable.class, Runnable.class, long.class},
                    plugin, (Runnable)() -> { if (enabled && player.isOnline()) action.run(); }, (Runnable)() -> {}, 1L);
        } else global(() -> { if (player.isOnline()) action.run(); });
    }
    void repeat(Runnable action, long period) {
        if (folia) call("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler", globalScheduler(), "runAtFixedRate",
                new Class[]{Plugin.class, Consumer.class, long.class, long.class}, plugin, (Consumer<Object>)ignored -> { if (enabled) action.run(); }, 1L, period);
        else Bukkit.getScheduler().runTaskTimer(plugin, action, 1, period);
    }
    void close() {
        enabled = false;
        if (folia) call("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler", globalScheduler(), "cancelTasks", new Class[]{Plugin.class}, plugin);
        else Bukkit.getScheduler().cancelTasks(plugin);
    }
}
