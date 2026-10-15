package dev.enhancedtimer.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Bounded, versioned server storage; no dependency on a loader's JSON/YAML implementation. */
public final class TimerArchive {
    private TimerArchive() {}
    public static void save(Path path, TimerHub hub) throws IOException {
        var data = hub.save(); var bytes = new ByteArrayOutputStream(); var out = new DataOutputStream(bytes);
        out.writeInt(0x42544131); out.writeInt(data.timers().size());
        for (var entry : data.timers()) {
            out.writeUTF(entry.id()); out.writeUTF(entry.owner() == null ? "" : entry.owner().toString());
            var saved = entry.saved(); var timer = saved.timer();
            out.writeUTF(timer.mode().name()); out.writeUTF(timer.state().name()); out.writeLong(timer.elapsedMillis()); out.writeLong(timer.durationMillis());
            out.writeInt(saved.splits().size()); for (var split : saved.splits()) { out.writeUTF(split.name()); out.writeLong(split.elapsedMillis()); }
            out.writeInt(saved.milestones().size()); for (String milestone : saved.milestones()) out.writeUTF(milestone);
            var rules = entry.rules();
            for (boolean flag : new boolean[]{rules.startOnMove(), rules.pauseOnDeath(), rules.stopOnDragon(), rules.splitMilestones(), rules.freezePaused(), rules.protectPaused(), rules.blindPaused(), rules.spectatorOnDeath(), rules.spectatorOnExpiry()}) out.writeBoolean(flag);
        }
        out.writeInt(data.selected().size()); for (var entry : data.selected().entrySet()) { out.writeUTF(entry.getKey().toString()); out.writeUTF(entry.getValue()); }
        if (bytes.size() > 64_000_000) throw new IOException("Timer archive too large");
        Files.createDirectories(path.getParent()); Path temporary = Files.createTempFile(path.getParent(), "enhancedtimer-", ".tmp");
        try {
            Files.write(temporary, bytes.toByteArray());
            try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
    private static int count(DataInputStream in, int max) throws IOException {
        int count = in.readInt(); if (count < 0 || count > max) throw new IOException("Invalid archive count"); return count;
    }
    public static void load(Path path, TimerHub hub) throws IOException {
        if (!Files.exists(path)) return;
        if (Files.size(path) > 64_000_000) throw new IOException("Timer archive too large");
        try (var in = new DataInputStream(Files.newInputStream(path))) {
            if (in.readInt() != 0x42544131) throw new IOException("Unknown archive version");
            int count = count(in, 1000); var entries = new ArrayList<TimerHub.Entry>();
            for (int i = 0; i < count; i++) {
                String id = in.readUTF(), owner = in.readUTF();
                var snapshot = new TimerEngine.Snapshot(TimerEngine.Mode.valueOf(in.readUTF()), TimerEngine.State.valueOf(in.readUTF()), in.readLong(), in.readLong());
                var splits = new ArrayList<TimerSession.Split>(); int splitCount = count(in, 256);
                for (int j = 0; j < splitCount; j++) {
                    String name = in.readUTF(); long elapsed = in.readLong();
                    if (name.length() > 80 || elapsed < 0 || elapsed > TimerEngine.MAX_DURATION) throw new IOException("Invalid split");
                    splits.add(new TimerSession.Split(name, elapsed));
                }
                var milestones = new LinkedHashSet<String>(); int milestoneCount = count(in, 256);
                for (int j = 0; j < milestoneCount; j++) { String name = in.readUTF(); if (name.length() > 80) throw new IOException("Invalid milestone"); milestones.add(name); }
                var rules = new TimerSession.Rules(in.readBoolean(), in.readBoolean(), in.readBoolean(), in.readBoolean(), in.readBoolean(), in.readBoolean(), in.readBoolean(), in.readBoolean(), in.readBoolean());
                entries.add(new TimerHub.Entry(id, owner.isEmpty() ? null : UUID.fromString(owner), new TimerSession.Saved(snapshot, splits, milestones), rules));
            }
            int selectedCount = count(in, 10000); var selected = new HashMap<UUID, String>();
            for (int i = 0; i < selectedCount; i++) selected.put(UUID.fromString(in.readUTF()), in.readUTF());
            if (in.read() != -1) throw new IOException("Trailing archive data");
            hub.load(new TimerHub.Stored(entries, selected));
        } catch (RuntimeException e) { throw new IOException("Invalid timer archive", e); }
    }
}
