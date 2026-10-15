package dev.enhancedtimer.core;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.enhancedtimer.core.TimerEngine.*;

class TimerEngineTest {
    private final AtomicLong nanos = new AtomicLong();
    private final TimerEngine timer = new TimerEngine(nanos::get);
    private void advance(long millis) { nanos.addAndGet(millis * 1_000_000); }
    @Test void pauseExcludesTimeAndStartResumesOnlyPausedTimers() {
        timer.start(); advance(1500); timer.pause(); advance(10_000);
        assertEquals(1500, timer.displayMillis());
        timer.start(); advance(500); timer.stop(); assertEquals(2000, timer.displayMillis());
        advance(3000); assertEquals(2000, timer.displayMillis());
        timer.start(); assertEquals(0, timer.displayMillis());
    }
    @Test void countdownStopsExactlyAtZeroEvenAfterLongStall() {
        timer.configure(Mode.COUNTDOWN, 2000); timer.start(); advance(100_000);
        assertEquals(0, timer.displayMillis()); assertEquals(State.FINISHED, timer.state());
        timer.start(); assertEquals(2000, timer.displayMillis());
    }
    @Test void suspensionPreservesRunningIntentWithoutCountingPausedTime() {
        timer.start(); advance(1000); timer.suspend(true); advance(8000);
        assertEquals(State.RUNNING, timer.state()); assertEquals(1000, timer.elapsedMillis());
        timer.suspend(false); advance(500); assertEquals(1500, timer.elapsedMillis());
    }
    @Test void restoringDoesNotCountOfflineTime() {
        timer.start(); advance(1234); var snapshot = timer.snapshot(); advance(60_000);
        var restored = new TimerEngine(nanos::get); restored.restore(snapshot); advance(30_000);
        assertEquals(State.PAUSED, restored.state()); assertEquals(1234, restored.elapsedMillis());
    }
    @Test void finishedSnapshotDoesNotRestart() {
        timer.configure(Mode.COUNTDOWN, 1000); timer.start(); advance(1000);
        var restored = new TimerEngine(nanos::get); restored.restore(timer.snapshot());
        assertEquals(State.FINISHED, restored.state()); assertEquals(0, restored.displayMillis());
    }
    @Test void invalidSnapshotsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> timer.restore(new Snapshot(Mode.COUNTDOWN, State.RUNNING, -1, 1000)));
        assertThrows(IllegalArgumentException.class, () -> timer.configure(Mode.COUNTDOWN, Long.MAX_VALUE));
    }
    @Test void startingAnAlreadyRunningClockDoesNotResetIt() {
        timer.start(); advance(1000); timer.start(); advance(1000); assertEquals(2000, timer.elapsedMillis());
    }
}

