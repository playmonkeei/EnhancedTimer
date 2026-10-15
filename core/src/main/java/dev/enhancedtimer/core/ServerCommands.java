package dev.enhancedtimer.core;

import java.util.*;

/** Same server command behavior on Bukkit and every mod loader. */
public final class ServerCommands {
    public static final UUID CONSOLE = new UUID(0, 0);
    private final TimerHub hub;
    public ServerCommands(TimerHub hub) { this.hub = hub; }
    public String execute(UUID actor, boolean administrator, String[] args) {
        String scope = hub.selected(actor); var session = hub.session(scope);
        if (args.length == 0) return status(actor);
        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("personal") || action.equals("global")) { hub.select(actor, action.equals("personal") ? "personal" : "global", administrator); return status(actor); }
        if (action.equals("team")) {
            if (args.length == 2 && args[1].equals("leave")) { hub.select(actor, "global", administrator); return status(actor); }
            if (args.length == 2 && args[1].equals("list")) return String.join(", ", hub.sessions().keySet().stream().filter(s -> s.startsWith("team:")).toList());
            if (args.length != 3) throw new IllegalArgumentException("team <create|join> <name> / team list");
            if (args[1].equals("create")) hub.createTeam(actor, args[2]);
            else if (args[1].equals("delete")) hub.deleteTeam(actor, args[2], administrator);
            else if (args[1].equals("join")) hub.select(actor, "team:" + args[2], administrator);
            else throw new IllegalArgumentException("team <create|join> <name>");
            return status(actor);
        }
        if (action.equals("splits")) return String.join(" | ", session.splits().stream().map(s -> s.name() + " " + TimeFormat.PRECISE.format(s.elapsedMillis(), false)).toList());
        if (!hub.canControl(actor, scope, administrator)) throw new SecurityException("No permission to control " + scope);
        switch (action) {
            case "set" -> {
                if (args.length != 2) throw new IllegalArgumentException("set <2d|2h|90m|HH:MM:SS>");
                session.setDisplay(args[1].equals("0") ? 0 : Durations.parse(args[1]));
            }
            case "start", "pause", "stop", "reset", "undo" -> {
                if (args.length != 1) throw new IllegalArgumentException("Unexpected arguments"); session.action(action);
            }
            case "timer" -> { session.configure(TimerEngine.Mode.STOPWATCH, session.timer.durationMillis()); session.action("start"); }
            case "countdown" -> {
                if (args.length != 2) throw new IllegalArgumentException("countdown <2d|2h|90m|HH:MM:SS>");
                session.configure(TimerEngine.Mode.COUNTDOWN, Durations.parse(args[1])); session.action("start");
            }
            case "split" -> session.split(args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Split " + (session.splits().size() + 1));
            case "rules" -> {
                if (!administrator) throw new SecurityException("Only administrators may change challenge rules");
                if (args.length != 3 || !(args[2].equals("true") || args[2].equals("false"))) throw new IllegalArgumentException("rules <start-move|pause-death|stop-dragon|splits|freeze|protect|blind|death-spectator|expiry-spectator> <true|false>");
                var current = hub.rules(scope); boolean value = Boolean.parseBoolean(args[2]);
                boolean[] flags = {current.startOnMove(), current.pauseOnDeath(), current.stopOnDragon(), current.splitMilestones(), current.freezePaused(), current.protectPaused(), current.blindPaused(), current.spectatorOnDeath(), current.spectatorOnExpiry()};
                int index = List.of("start-move", "pause-death", "stop-dragon", "splits", "freeze", "protect", "blind", "death-spectator", "expiry-spectator").indexOf(args[1]);
                if (index < 0) throw new IllegalArgumentException("Unknown rule"); flags[index] = value;
                hub.rules(scope, new TimerSession.Rules(flags[0], flags[1], flags[2], flags[3], flags[4], flags[5], flags[6], flags[7], flags[8]));
            }
            default -> throw new IllegalArgumentException("start | pause | stop | reset | undo | timer | countdown | split | splits | personal | global | team | rules");
        }
        return status(actor);
    }
    public String status(UUID actor) {
        String id = hub.selected(actor); var timer = hub.session(id).timer;
        return id + " · " + TimeFormat.DAYS.format(timer.displayMillis(), timer.mode() == TimerEngine.Mode.COUNTDOWN) + " · " + timer.state();
    }
}
