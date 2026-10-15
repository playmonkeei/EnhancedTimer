package dev.enhancedtimer.core;

import java.util.*;

/** Owns undo, splits, one-shot milestones and countdown threshold detection. */
public final class TimerSession {
    public record Change(String action, TimerEngine.Snapshot timer) { }
    private final List<java.util.function.Consumer<Change>> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    /** Register on the owning timer thread. Close the handle when your integration unloads. */
    public AutoCloseable listen(java.util.function.Consumer<Change> listener) {
        listeners.add(Objects.requireNonNull(listener)); return () -> listeners.remove(listener);
    }
    private void changed(String action) {
        var change = new Change(action, timer.snapshot());
        for (var listener : listeners) try { listener.accept(change); }
        catch (RuntimeException e) { System.getLogger("Enhanced Timer").log(System.Logger.Level.WARNING, "Timer listener failed", e); }
    }
    public record Split(String name, long elapsedMillis) {}
    public record Saved(TimerEngine.Snapshot timer, List<Split> splits, Set<String> milestones) {}
    public record Rules(boolean startOnMove, boolean pauseOnDeath, boolean stopOnDragon,
                        boolean splitMilestones, boolean freezePaused, boolean protectPaused,
                        boolean blindPaused, boolean spectatorOnDeath, boolean spectatorOnExpiry) {
        public static Rules defaults() { return new Rules(false, false, false, true, false, false, false, false, false); }
    }
    public final TimerEngine timer;
    private final List<Split> splits = new ArrayList<>();
    private final Set<String> milestones = new LinkedHashSet<>();
    private record Undo(Saved saved, boolean restoreTimer) { }
    private final Deque<Undo> undo = new ArrayDeque<>();
    private long previousRemaining = -1;
    private final Set<Long> warned = new HashSet<>();
    public TimerSession() { this(new TimerEngine()); }
    public TimerSession(TimerEngine timer) { this.timer = Objects.requireNonNull(timer); }
    public Saved save() { return new Saved(timer.snapshot(), List.copyOf(splits), Set.copyOf(milestones)); }
    public void load(Saved saved) {
        Objects.requireNonNull(saved);
        if (saved.splits() != null && (saved.splits().size() > 256 || saved.splits().stream().anyMatch(s -> s == null || s.name() == null
                || s.name().isBlank() || s.name().length() > 80 || s.elapsedMillis() < 0 || s.elapsedMillis() > TimerEngine.MAX_DURATION)))
            throw new IllegalArgumentException("Invalid saved splits");
        if (saved.milestones() != null && (saved.milestones().size() > 256 || saved.milestones().stream().anyMatch(s -> s == null || s.length() > 80)))
            throw new IllegalArgumentException("Invalid saved milestones");
        timer.restore(saved.timer());
        splits.clear(); milestones.clear(); undo.clear(); warned.clear();
        if (saved.splits() != null) splits.addAll(saved.splits().stream().filter(Objects::nonNull).limit(256).toList());
        if (saved.milestones() != null) saved.milestones().stream().filter(Objects::nonNull).limit(256).forEach(milestones::add);
        rearm();
    }
    private void checkpoint() { checkpoint(true); }
    private void checkpoint(boolean restoreTimer) { undo.push(new Undo(save(), restoreTimer)); while (undo.size() > 20) undo.removeLast(); }
    public boolean canUndo() { return !undo.isEmpty(); }
    public void forgetHistory() { undo.clear(); warned.clear(); rearm(); }
    public boolean undo() {
        if (undo.isEmpty()) return false;
        Undo point = undo.pop(); Saved previous = point.saved();
        if (point.restoreTimer()) timer.synchronize(previous.timer(), false);
        splits.clear(); splits.addAll(previous.splits()); milestones.clear(); milestones.addAll(previous.milestones());
        warned.clear(); rearm(); changed("undo"); return true;
    }
    public void configure(TimerEngine.Mode mode, long duration) {
        if (duration <= 0 || duration > TimerEngine.MAX_DURATION) throw new IllegalArgumentException("Invalid duration");
        checkpoint(); timer.configure(mode, duration); splits.clear(); milestones.clear(); warned.clear(); rearm(); changed("configure");
    }
    /** Set the displayed value, paused, so a later start resumes from it. */
    public void setDisplay(long millis) {
        if (millis < 0 || millis > TimerEngine.MAX_DURATION || timer.mode() == TimerEngine.Mode.COUNTDOWN && millis == 0)
            throw new IllegalArgumentException("Invalid time value");
        checkpoint(); boolean countdown = timer.mode() == TimerEngine.Mode.COUNTDOWN;
        timer.synchronize(new TimerEngine.Snapshot(timer.mode(), TimerEngine.State.PAUSED, countdown ? 0 : millis,
                countdown ? millis : timer.durationMillis()), false);
        warned.clear(); rearm(); changed("set");
    }
    public void action(String action) {
        if (!Set.of("start", "pause", "stop", "reset", "toggle", "undo").contains(action)) throw new IllegalArgumentException("Unknown action");
        if (action.equals("undo")) { undo(); return; }
        boolean restart = (action.equals("start") || action.equals("toggle"))
                && timer.state() != TimerEngine.State.RUNNING && timer.state() != TimerEngine.State.PAUSED;
        checkpoint();
        switch (action) {
            case "start" -> timer.start(); case "pause" -> timer.pause(); case "stop" -> timer.stop();
            case "reset" -> timer.reset(); case "toggle" -> timer.toggle();
        }
        if (restart || action.equals("reset")) { splits.clear(); milestones.clear(); warned.clear(); }
        rearm();
        changed(action);
    }
    public Split split(String name) {
        if (splits.size() >= 256) throw new IllegalStateException("Maximum 256 splits");
        if (name == null || name.isBlank() || name.length() > 80) throw new IllegalArgumentException("Invalid split name");
        checkpoint(false);
        var split = new Split(name, timer.elapsedMillis()); splits.add(split); changed("split"); return split;
    }
    public List<Split> splits() { return List.copyOf(splits); }
    public void remoteHistory(int offset, List<Split> page) {
        if (offset == 0) splits.clear();
        if (offset != splits.size() || offset + page.size() > 256) return;
        splits.addAll(page);
    }
    public void event(String event, Rules rules) {
        if (event.equals("move") && rules.startOnMove() && timer.state() == TimerEngine.State.READY) action("start");
        if (event.equals("death") && rules.pauseOnDeath()) action("pause");
        if (event.equals("nether") || event.equals("end") || event.equals("dragon")) {
            if (timer.state() == TimerEngine.State.READY || timer.state() == TimerEngine.State.STOPPED) return;
            if (milestones.add(event) && rules.splitMilestones() && splits.size() < 256) split(event);
            if (event.equals("dragon") && rules.stopOnDragon()) action("stop");
        }
    }
    public List<Long> warnings(long[] thresholds) {
        long remaining = timer.displayMillis();
        var result = new ArrayList<Long>();
        if (timer.mode() == TimerEngine.Mode.COUNTDOWN && timer.state() != TimerEngine.State.READY && previousRemaining >= 0)
            for (long threshold : thresholds) if (threshold > 0 && previousRemaining > threshold && remaining <= threshold && warned.add(threshold)) result.add(threshold);
        previousRemaining = remaining;
        return List.copyOf(result);
    }
    public void rearm() { previousRemaining = timer.displayMillis(); }
}
