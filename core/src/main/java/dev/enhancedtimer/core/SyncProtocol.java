package dev.enhancedtimer.core;

import java.io.*;
import java.util.*;

/** Small bounded, loader-independent plugin-message protocol. Never carries executable commands. */
public final class SyncProtocol {
    public static final String CHANNEL = "enhancedtimer:sync";
    public static final int VERSION = 1, MAX_BYTES = 24_000;
    public enum Kind { HELLO, SELECT, ACTION, CONFIGURE, STATE, ERROR, HISTORY }
    public record Message(Kind kind, String scope, String action, long duration,
                          TimerEngine.Snapshot timer, boolean suspended, boolean canControl, long sequence, List<TimerSession.Split> splits) {
        public Message { splits = splits == null ? List.of() : List.copyOf(splits); }
        public Message(Kind kind, String scope, String action, long duration, TimerEngine.Snapshot timer, boolean suspended, boolean canControl, long sequence) {
            this(kind, scope, action, duration, timer, suspended, canControl, sequence, List.of());
        }
    }
    private SyncProtocol() {}
    public static byte[] encode(Message message) {
        try {
            var bytes = new ByteArrayOutputStream(); var out = new DataOutputStream(bytes);
            out.writeInt(0x4554494D); out.writeByte(VERSION); out.writeByte(message.kind().ordinal());
            writeString(out, message.scope()); writeString(out, message.action()); out.writeLong(message.duration());
            out.writeBoolean(message.timer() != null);
            if (message.timer() != null) {
                out.writeByte(message.timer().mode().ordinal()); out.writeByte(message.timer().state().ordinal());
                out.writeLong(message.timer().elapsedMillis()); out.writeLong(message.timer().durationMillis());
            }
            out.writeBoolean(message.suspended()); out.writeBoolean(message.canControl()); out.writeLong(message.sequence());
            if (message.splits().size() > 64) throw new IllegalArgumentException("Use encodePackets for history");
            out.writeByte(message.splits().size());
            for (var split : message.splits()) { writeString(out, split.name()); out.writeLong(split.elapsedMillis()); }
            if (bytes.size() > MAX_BYTES) throw new IllegalArgumentException("Oversized payload");
            return bytes.toByteArray();
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
    private static void writeString(DataOutputStream out, String value) throws IOException {
        if (value == null) value = "";
        if (value.length() > 160) throw new IllegalArgumentException("Protocol string too long");
        out.writeUTF(value);
    }
    public static Message decode(byte[] bytes) throws IOException {
        if (bytes.length > MAX_BYTES) throw new IOException("Oversized packet");
        try {
            var in = new DataInputStream(new ByteArrayInputStream(bytes));
            if (in.readInt() != 0x4554494D || in.readUnsignedByte() != VERSION) throw new IOException("Protocol mismatch");
            Kind kind = Kind.values()[in.readUnsignedByte()];
            String scope = in.readUTF(), action = in.readUTF(); long duration = in.readLong();
            if (scope.length() > 160 || action.length() > 160) throw new IOException("Oversized field");
            TimerEngine.Snapshot snapshot = null;
            if (in.readBoolean()) {
                snapshot = new TimerEngine.Snapshot(TimerEngine.Mode.values()[in.readUnsignedByte()], TimerEngine.State.values()[in.readUnsignedByte()], in.readLong(), in.readLong());
                new TimerEngine().restore(snapshot);
            }
            boolean suspended = in.readBoolean(), control = in.readBoolean(); long sequence = in.readLong();
            int count = in.readUnsignedByte(); if (count > 64) throw new IOException("Invalid history count");
            var splits = new ArrayList<TimerSession.Split>();
            for (int i = 0; i < count; i++) {
                String name = in.readUTF(); long elapsed = in.readLong();
                if (name.length() > 80 || elapsed < 0 || elapsed > TimerEngine.MAX_DURATION) throw new IOException("Invalid split");
                splits.add(new TimerSession.Split(name, elapsed));
            }
            var message = new Message(kind, scope, action, duration, snapshot, suspended, control, sequence, splits);
            if (in.available() != 0 || (kind == Kind.STATE && snapshot == null)) throw new IOException("Invalid packet");
            return message;
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) { throw new IOException("Invalid packet", e); }
    }
    /** Chunk history to stay below Bukkit's payload limit, including worst-case UTF-8 names. */
    public static List<byte[]> encodePackets(Message message) {
        if (message.kind() != Kind.STATE) return List.of(encode(message));
        var packets = new ArrayList<byte[]>();
        packets.add(encode(new Message(Kind.STATE, message.scope(), message.action(), message.duration(), message.timer(), message.suspended(), message.canControl(), message.sequence())));
        for (int offset = 0; offset < Math.min(256, message.splits().size()); offset += 64)
            packets.add(encode(new Message(Kind.HISTORY, message.scope(), "", offset, null, false, false, message.sequence(),
                    message.splits().subList(offset, Math.min(offset + 64, message.splits().size())))));
        return List.copyOf(packets);
    }
}
