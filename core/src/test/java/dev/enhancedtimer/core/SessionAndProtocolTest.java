package dev.enhancedtimer.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class SessionAndProtocolTest {
    private final AtomicLong clock = new AtomicLong();
    private final TimerSession session = new TimerSession(new TimerEngine(clock::get));
    private void advance(long millis) { clock.addAndGet(millis * 1_000_000); }

    @Test void daysAndTemplatesCoverLongChallenges() {
        assertEquals(176_523_000L, Durations.parse("2d1h2m3s"));
        assertEquals(TimerEngine.MAX_DURATION, Durations.parse("365d"));
        assertEquals("2d 01:02:03", TimeFormat.DAYS.format(176_523_000L, false));
        assertEquals("3s", TimeFormat.AUTO.format(3000, false));
        assertEquals("2d 1h 2m 3s", TimeFormat.AUTO.format(176_523_000L, false));
        assertEquals("2/49/01/02/03/123", TimeFormat.TEMPLATE.format(176_523_123L, false, "{d}/{H}/{hh}/{mm}/{ss}/{ms}"));
    }
    @Test void conditionalGroupsHideZeroUnitsIncludingTheirStylingAndAffixes() {
        String template = "[?d:<gold>{d} Tage </gold>][?h:{h} Stunden ]{m} Minuten {ss}";
        assertEquals("2 Minuten 03", TimeFormat.TEMPLATE.format(123_000, false, template));
        assertEquals("<gold>1 Tage </gold>1 Stunden 2 Minuten 03", TimeFormat.TEMPLATE.format(90_123_000, false, template));
    }
    @Test void setDisplayedTimeResumesWithoutResetAndUndoRestoresPreviousRun() {
        session.action("start"); advance(2000); session.setDisplay(60_000);
        assertEquals(TimerEngine.State.PAUSED, session.timer.state()); session.action("start"); advance(1000);
        assertEquals(61_000, session.timer.displayMillis());
        session.action("undo"); session.action("undo"); assertEquals(2000, session.timer.elapsedMillis());
        session.configure(TimerEngine.Mode.COUNTDOWN, 30_000); session.setDisplay(60_000); session.action("start"); advance(2000);
        assertEquals(58_000, session.timer.displayMillis());
        assertThrows(IllegalArgumentException.class, () -> session.setDisplay(0));
    }
    @Test void apiListenersObserveChangesAndCanUnsubscribe() throws Exception {
        var changes = new ArrayList<TimerSession.Change>(); var subscription = session.listen(changes::add);
        session.action("start"); session.split("Nether"); session.action("pause");
        assertEquals(List.of("start", "split", "pause"), changes.stream().map(TimerSession.Change::action).toList());
        assertEquals(TimerEngine.State.RUNNING, changes.getFirst().timer().state());
        subscription.close(); session.action("reset"); assertEquals(3, changes.size());
    }
    @Test void resettingAndChangingModeCanBeUndoneIncludingSplits() {
        session.action("start"); advance(1234); session.split("Nether");
        session.configure(TimerEngine.Mode.COUNTDOWN, 5000);
        assertEquals(0, session.splits().size());
        assertTrue(session.undo());
        assertEquals(TimerEngine.Mode.STOPWATCH, session.timer.mode());
        assertEquals(TimerEngine.State.RUNNING, session.timer.state());
        assertEquals("Nether", session.splits().getFirst().name());
        assertEquals(1234, session.timer.elapsedMillis());
        session.action("reset"); assertEquals(0, session.timer.elapsedMillis());
        session.undo(); assertEquals(1234, session.timer.elapsedMillis());
    }
    @Test void warningsAreOneShotAndRearmForNewRun() {
        session.configure(TimerEngine.Mode.COUNTDOWN, 70_000); session.action("start");
        advance(15_000); assertEquals(List.of(60_000L), session.warnings(new long[]{60_000, 10_000}));
        session.action("pause"); advance(10_000); session.action("start");
        assertTrue(session.warnings(new long[]{60_000, 10_000}).isEmpty());
        advance(50_000); assertEquals(List.of(10_000L), session.warnings(new long[]{60_000, 10_000}));
        assertTrue(session.warnings(new long[]{60_000, 10_000}).isEmpty());
        session.action("reset"); session.action("start"); advance(15_000);
        assertEquals(List.of(60_000L), session.warnings(new long[]{60_000}));
    }
    @Test void milestonesOnlySplitOnceAndRulesAreOptIn() {
        session.action("start"); advance(4000);
        session.event("nether", TimerSession.Rules.defaults()); session.event("nether", TimerSession.Rules.defaults());
        assertEquals(1, session.splits().size()); assertEquals(TimerEngine.State.RUNNING, session.timer.state());
        var rules = new TimerSession.Rules(false, true, true, true, false, false, false, false, false);
        session.event("death", rules); assertEquals(TimerEngine.State.PAUSED, session.timer.state());
        session.action("start"); session.event("dragon", rules); assertEquals(TimerEngine.State.STOPPED, session.timer.state());
    }
    @Test void forgettingHistoryPreventsCrossWorldUndo() {
        session.action("start"); advance(1234); session.action("reset"); session.forgetHistory();
        assertFalse(session.canUndo()); assertFalse(session.undo());
    }
    @Test void aSplitCanBeUndoneWithoutResettingTheRunningTimer() {
        session.action("start"); advance(2500); session.split("Nether"); advance(500);
        assertTrue(session.undo()); assertTrue(session.splits().isEmpty());
        assertEquals(TimerEngine.State.RUNNING, session.timer.state()); assertEquals(3000, session.timer.elapsedMillis());
        advance(1000); assertEquals(4000, session.timer.elapsedMillis());
    }
    @Test void globalAndPrivateTimersEnforceOwnershipForNetworkRequests() {
        var hub = new TimerHub(); UUID owner = UUID.randomUUID(), outsider = UUID.randomUUID();
        var startGlobal = new SyncProtocol.Message(SyncProtocol.Kind.ACTION, "global", "start", 0, null, false, true, 0);
        assertThrows(SecurityException.class, () -> hub.request(outsider, false, startGlobal));
        hub.request(owner, true, startGlobal); assertEquals(TimerEngine.State.RUNNING, hub.session("global").timer.state());
        hub.select(owner, "personal", false); String personal = hub.selected(owner);
        assertThrows(IllegalArgumentException.class, () -> hub.select(outsider, personal, false));
        assertThrows(SecurityException.class, () -> hub.request(outsider, false, new SyncProtocol.Message(SyncProtocol.Kind.ACTION, personal, "reset", 0, null, false, true, 0)));
        hub.request(owner, false, new SyncProtocol.Message(SyncProtocol.Kind.ACTION, personal, "start", 0, null, false, false, 0));
        assertEquals(TimerEngine.State.RUNNING, hub.session(personal).timer.state());
    }
    @Test void teamsCanBeViewedByMembersButOnlyControlledByOwnerOrAdmin() {
        var hub = new TimerHub(); UUID owner = UUID.randomUUID(), member = UUID.randomUUID();
        hub.createTeam(owner, "challenge"); hub.select(member, "team:challenge", false);
        assertTrue(hub.canControl(owner, "team:challenge", false)); assertFalse(hub.canControl(member, "team:challenge", false));
        assertFalse(hub.state(member, false, 1).canControl());
        assertThrows(IllegalArgumentException.class, () -> hub.createTeam(owner, "../evil"));
    }
    @Test void personalTimerOwnershipDoesNotGrantWorldRulePermissions() {
        var hub = new TimerHub(); var commands = new ServerCommands(hub); UUID player = UUID.randomUUID();
        commands.execute(player, false, new String[]{"personal"});
        assertThrows(SecurityException.class, () -> commands.execute(player, false, new String[]{"rules", "protect", "true"}));
        commands.execute(player, true, new String[]{"rules", "protect", "true"});
        assertTrue(hub.rules(hub.selected(player)).protectPaused());
    }
    @Test void protocolRoundTripsStateAndDoesNotTrustClientPermissionFlags() throws IOException {
        var snapshot = new TimerEngine.Snapshot(TimerEngine.Mode.COUNTDOWN, TimerEngine.State.RUNNING, 500, 7000);
        var original = new SyncProtocol.Message(SyncProtocol.Kind.STATE, "team:test", "", 0, snapshot, true, true, 42);
        assertEquals(original, SyncProtocol.decode(SyncProtocol.encode(original)));
        var hub = new TimerHub();
        assertThrows(IllegalArgumentException.class, () -> hub.request(UUID.randomUUID(), true, new SyncProtocol.Message(SyncProtocol.Kind.STATE, "global", "", 0, snapshot, false, true, 0)));
    }
    @Test void malformedProtocolIsRejectedBeforeStateChanges() {
        assertThrows(IOException.class, () -> SyncProtocol.decode(new byte[0]));
        assertThrows(IOException.class, () -> SyncProtocol.decode(new byte[SyncProtocol.MAX_BYTES + 1]));
        byte[] valid = SyncProtocol.encode(new SyncProtocol.Message(SyncProtocol.Kind.HELLO, "", "", 0, null, false, false, 0));
        byte[] trailing = Arrays.copyOf(valid, valid.length + 1);
        assertThrows(IOException.class, () -> SyncProtocol.decode(trailing));
        valid[5] = 127; assertThrows(IOException.class, () -> SyncProtocol.decode(valid));
    }
    @Test void diskArchiveRestoresTeamsRulesSplitsAndPausedState(@TempDir Path directory) throws IOException {
        var hub = new TimerHub(); UUID owner = UUID.randomUUID(); hub.createTeam(owner, "test");
        hub.session("team:test").action("start"); hub.session("team:test").split("first");
        var commands = new ServerCommands(hub); commands.execute(owner, true, new String[]{"rules", "freeze", "true"});
        Path file = directory.resolve("timers.dat"); TimerArchive.save(file, hub);
        var restored = new TimerHub(); TimerArchive.load(file, restored);
        assertEquals("team:test", restored.selected(owner)); assertTrue(restored.rules("team:test").freezePaused());
        assertEquals(TimerEngine.State.PAUSED, restored.session("team:test").timer.state());
        assertEquals("first", restored.session("team:test").splits().getFirst().name());
        assertTrue(restored.canControl(owner, "team:test", false));
        Files.write(file, new byte[]{1,2,3}); assertThrows(IOException.class, () -> TimerArchive.load(file, restored));
        assertEquals("team:test", restored.selected(owner));
    }
    @Test void networkSyncKeepsRunningWhileDiskRestorePauses() {
        var snapshot = new TimerEngine.Snapshot(TimerEngine.Mode.STOPWATCH, TimerEngine.State.RUNNING, 5000, 10_000);
        var timer = new TimerEngine(clock::get); timer.synchronize(snapshot, false); advance(500);
        assertEquals(5500, timer.elapsedMillis());
        timer.restore(snapshot); advance(500); assertEquals(5000, timer.elapsedMillis());
    }
    @Test void historyIsChunkedWithinPluginLimitsAndCanBeReassembled() throws IOException {
        var history = new ArrayList<TimerSession.Split>();
        for (int i = 0; i < 256; i++) history.add(new TimerSession.Split("界".repeat(77) + String.format("%03d", i), i * 1000L));
        var state = new SyncProtocol.Message(SyncProtocol.Kind.STATE, "global", "", 0,
                new TimerEngine.Snapshot(TimerEngine.Mode.STOPWATCH, TimerEngine.State.RUNNING, 300_000, 7_200_000), false, false, 8, history);
        var packets = SyncProtocol.encodePackets(state); assertEquals(5, packets.size());
        var received = new TimerSession();
        for (byte[] bytes : packets) {
            assertTrue(bytes.length <= SyncProtocol.MAX_BYTES);
            var decoded = SyncProtocol.decode(bytes);
            if (decoded.kind() == SyncProtocol.Kind.HISTORY) received.remoteHistory((int)decoded.duration(), decoded.splits());
        }
        assertEquals(history, received.splits());
    }
    @Test void deletingATeamDoesNotLeaveMembersPointingToMissingTimers() {
        var hub = new TimerHub(); UUID owner = UUID.randomUUID(), member = UUID.randomUUID();
        hub.createTeam(owner, "test"); hub.select(member, "team:test", false);
        assertThrows(SecurityException.class, () -> hub.deleteTeam(member, "test", false));
        hub.deleteTeam(owner, "test", false);
        assertEquals("global", hub.selected(owner)); assertEquals("global", hub.selected(member));
    }
}
