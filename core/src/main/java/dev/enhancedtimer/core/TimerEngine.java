package dev.enhancedtimer.core;

import java.util.Objects;
import java.util.function.LongSupplier;

/** A monotonic, tick-rate independent clock. Call on its owning game thread. */
public final class TimerEngine {
    public enum Mode { STOPWATCH, COUNTDOWN }
    public enum State { READY, RUNNING, PAUSED, STOPPED, FINISHED }
    public record Snapshot(Mode mode, State state, long elapsedMillis, long durationMillis) {}
    public static final long MAX_DURATION = 365L * 24 * 60 * 60 * 1000;
    private final LongSupplier clock;
    private Mode mode = Mode.STOPWATCH;
    private State state = State.READY;
    private long durationMillis = 7_200_000;
    private long elapsedNanos;
    private long lastNanos;
    private boolean suspended;

    public TimerEngine() { this(System::nanoTime); }
    public TimerEngine(LongSupplier clock) { this.clock = Objects.requireNonNull(clock); }

    private void advance() {
        long now = clock.getAsLong();
        if (state == State.RUNNING && !suspended) {
            long delta = Math.max(0, now - lastNanos);
            elapsedNanos += Math.min(delta, MAX_DURATION * 1_000_000 - elapsedNanos);
            if (mode == Mode.COUNTDOWN && elapsedNanos >= durationMillis * 1_000_000) {
                elapsedNanos = durationMillis * 1_000_000;
                state = State.FINISHED;
            }
        }
        lastNanos = now;
    }

    public void configure(Mode mode, long durationMillis) {
        if (durationMillis <= 0 || durationMillis > MAX_DURATION) throw new IllegalArgumentException("Invalid duration");
        this.mode = Objects.requireNonNull(mode);
        this.durationMillis = durationMillis;
        reset();
    }
    public void start() {
        advance();
        if (state == State.RUNNING) return;
        if (state != State.PAUSED) elapsedNanos = 0;
        state = State.RUNNING;
        lastNanos = clock.getAsLong();
    }
    public void pause() { advance(); if (state == State.RUNNING) state = State.PAUSED; }
    public void stop() { advance(); if (state != State.READY) state = State.STOPPED; }
    public void reset() { elapsedNanos = 0; state = State.READY; lastNanos = clock.getAsLong(); }
    public void toggle() { if (state() == State.RUNNING) pause(); else start(); }
    public void suspend(boolean suspended) { advance(); this.suspended = suspended; }
    public boolean suspended() { return suspended; }
    public Mode mode() { return mode; }
    public State state() { advance(); return state; }
    public long durationMillis() { return durationMillis; }
    public long elapsedMillis() { advance(); return elapsedNanos / 1_000_000; }
    public long displayMillis() {
        long elapsed = elapsedMillis();
        return mode == Mode.COUNTDOWN ? Math.max(0, durationMillis - elapsed) : elapsed;
    }
    public Snapshot snapshot() { advance(); return new Snapshot(mode, state, elapsedNanos / 1_000_000, durationMillis); }
    /** Restored clocks never count offline time or silently start on joining a world. */
    public void restore(Snapshot snapshot) {
        restore(snapshot, false);
    }

    /** Network synchronization is live; disk restoration deliberately pauses running clocks. */
    public void synchronize(Snapshot snapshot, boolean suspended) {
        restore(snapshot, true);
        this.suspended = suspended;
    }

    private void restore(Snapshot snapshot, boolean live) {
        Objects.requireNonNull(snapshot);
        if (snapshot.durationMillis <= 0 || snapshot.durationMillis > MAX_DURATION || snapshot.elapsedMillis < 0
            || snapshot.elapsedMillis > MAX_DURATION) throw new IllegalArgumentException("Invalid snapshot");
        mode = Objects.requireNonNull(snapshot.mode);
        state = Objects.requireNonNull(snapshot.state);
        durationMillis = snapshot.durationMillis;
        elapsedNanos = Math.min(snapshot.elapsedMillis, mode == Mode.COUNTDOWN ? durationMillis : MAX_DURATION) * 1_000_000;
        if (mode == Mode.COUNTDOWN && elapsedNanos >= durationMillis * 1_000_000) state = State.FINISHED;
        else if (state == State.RUNNING && !live) state = State.PAUSED;
        suspended = false;
        lastNanos = clock.getAsLong();
    }
}

