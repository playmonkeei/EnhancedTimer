package dev.enhancedtimer.core;

import java.util.*;

/** Common server API. Platforms serialize access on their server/global thread. */
public final class TimerHub {
    public record Entry(String id, UUID owner, TimerSession.Saved saved, TimerSession.Rules rules) {}
    public record Stored(List<Entry> timers, Map<UUID, String> selected) {}
    private final Map<String, TimerSession> sessions = new LinkedHashMap<>();
    private final Map<String, UUID> owners = new HashMap<>();
    private final Map<String, TimerSession.Rules> rules = new HashMap<>();
    private final Map<UUID, String> selected = new HashMap<>();
    public TimerHub() { sessions.put("global", new TimerSession()); }
    public TimerSession session(String id) {
        var session = sessions.get(id); if (session == null) throw new IllegalArgumentException("Unknown timer: " + id); return session;
    }
    public String selected(UUID player) { return selected.getOrDefault(player, "global"); }
    public String personal(UUID player) {
        String id = "personal:" + player;
        if (!sessions.containsKey(id) && sessions.size() >= 1000) throw new IllegalStateException("Timer limit reached");
        sessions.computeIfAbsent(id, key -> new TimerSession()); owners.put(id, player); return id;
    }
    public boolean canControl(UUID player, String id, boolean administrator) {
        return administrator || player.equals(owners.get(id));
    }
    public void select(UUID player, String id, boolean administrator) {
        if (!selected.containsKey(player) && selected.size() >= 10000) throw new IllegalStateException("Participant limit reached");
        if (id.equals("personal")) id = personal(player);
        if (!sessions.containsKey(id)) throw new IllegalArgumentException("Unknown timer");
        if (id.startsWith("personal:") && !canControl(player, id, administrator)) throw new IllegalArgumentException("Private timer");
        selected.put(player, id);
    }
    public void createTeam(UUID player, String name) {
        if (!name.matches("[a-z0-9_-]{1,24}")) throw new IllegalArgumentException("Use a-z, 0-9, _ or - (24 characters)");
        if (sessions.size() >= 1000) throw new IllegalStateException("Timer limit reached");
        if (owners.entrySet().stream().filter(e -> e.getKey().startsWith("team:") && player.equals(e.getValue())).count() >= 5)
            throw new IllegalStateException("Maximum five owned teams");
        String id = "team:" + name;
        if (sessions.containsKey(id)) throw new IllegalArgumentException("Team already exists");
        sessions.put(id, new TimerSession()); owners.put(id, player); selected.put(player, id);
    }
    public void deleteTeam(UUID player, String name, boolean administrator) {
        String id = "team:" + name; session(id);
        if (!canControl(player, id, administrator)) throw new SecurityException("Only the team owner may delete it");
        sessions.remove(id); owners.remove(id); rules.remove(id); selected.replaceAll((key, value) -> value.equals(id) ? "global" : value);
    }
    public TimerSession.Rules rules(String id) { return rules.getOrDefault(id, TimerSession.Rules.defaults()); }
    public void rules(String id, TimerSession.Rules value) { session(id); rules.put(id, Objects.requireNonNull(value)); }
    public Map<String, TimerSession> sessions() { return Collections.unmodifiableMap(sessions); }
    public void event(UUID player, String event) { String id = selected(player); session(id).event(event, rules(id)); }
    public SyncProtocol.Message state(UUID player, boolean administrator, long sequence) {
        String id = selected(player); var timer = session(id).timer;
        return new SyncProtocol.Message(SyncProtocol.Kind.STATE, id, "", 0, timer.snapshot(), timer.suspended(), canControl(player, id, administrator), sequence, session(id).splits());
    }
    public void request(UUID player, boolean administrator, SyncProtocol.Message message) {
        if (message.kind() == SyncProtocol.Kind.HELLO) return;
        if (message.kind() == SyncProtocol.Kind.SELECT) { select(player, message.scope(), administrator); return; }
        String id = selected(player);
        if (!id.equals(message.scope()) || !canControl(player, id, administrator)) throw new SecurityException("No control permission");
        if (message.kind() == SyncProtocol.Kind.ACTION) {
            if (message.action().startsWith("split:")) session(id).split(message.action().substring(6));
            else if (message.action().startsWith("set:")) session(id).setDisplay(Long.parseLong(message.action().substring(4)));
            else session(id).action(message.action());
        }
        else if (message.kind() == SyncProtocol.Kind.CONFIGURE) {
            var mode = TimerEngine.Mode.valueOf(message.action()); session(id).configure(mode, message.duration());
        } else throw new IllegalArgumentException("Invalid client message");
    }
    public Stored save() {
        var entries = sessions.entrySet().stream().map(e -> new Entry(e.getKey(), owners.get(e.getKey()), e.getValue().save(), rules(e.getKey()))).toList();
        return new Stored(entries, Map.copyOf(selected));
    }
    public void load(Stored stored) {
        if (stored == null || stored.timers() == null || stored.timers().size() > 1000) throw new IllegalArgumentException("Invalid hub");
        var replacement = new TimerHub();
        var seen = new HashSet<String>();
        for (var entry : stored.timers()) {
            if (entry.id() == null || !entry.id().matches("global|personal:[a-fA-F0-9-]{36}|team:[a-z0-9_-]{1,24}")) throw new IllegalArgumentException("Invalid timer id");
            if (!seen.add(entry.id()) || entry.id().equals("global") && entry.owner() != null) throw new IllegalArgumentException("Invalid owner or duplicate timer");
            var session = new TimerSession(); session.load(entry.saved()); replacement.sessions.put(entry.id(), session);
            if (entry.id().startsWith("personal:") && !entry.id().equals("personal:" + entry.owner())) throw new IllegalArgumentException("Invalid personal owner");
            if (entry.owner() != null) replacement.owners.put(entry.id(), entry.owner());
            if (entry.rules() != null) replacement.rules.put(entry.id(), entry.rules());
        }
        if (stored.selected() != null) stored.selected().forEach((player, id) -> {
            if (player != null && replacement.sessions.containsKey(id)
                    && (!id.startsWith("personal:") || player.equals(replacement.owners.get(id)))) replacement.selected.put(player, id);
        });
        sessions.clear(); sessions.putAll(replacement.sessions); owners.clear(); owners.putAll(replacement.owners);
        rules.clear(); rules.putAll(replacement.rules); selected.clear(); selected.putAll(replacement.selected);
    }
}
