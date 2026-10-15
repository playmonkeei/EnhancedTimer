package dev.enhancedtimer.network;

import dev.enhancedtimer.core.SyncProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Raw bytes are identical on mod loaders and Bukkit plugin messaging. */
public record TimerPayload(byte[] bytes) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.parse(SyncProtocol.CHANNEL);
    public static final Type<TimerPayload> TYPE = new Type<>(ID);
    public TimerPayload {
        if (bytes.length > SyncProtocol.MAX_BYTES) throw new IllegalArgumentException("Oversized payload");
        bytes = bytes.clone();
    }
    @Override public Type<TimerPayload> type() { return TYPE; }
    public static <B extends FriendlyByteBuf> StreamCodec<B, TimerPayload> codec() {
        return StreamCodec.of((buffer, payload) -> buffer.writeBytes(payload.bytes()), buffer -> {
            if (buffer.readableBytes() > SyncProtocol.MAX_BYTES) throw new IllegalArgumentException("Oversized payload");
            byte[] bytes = new byte[buffer.readableBytes()]; buffer.readBytes(bytes); return new TimerPayload(bytes);
        });
    }
}
