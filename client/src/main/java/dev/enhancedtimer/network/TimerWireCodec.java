package dev.enhancedtimer.network;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Decorates only the gameplay packet boundary; other loaders retain their own payload registry. */
public final class TimerWireCodec {
    private TimerWireCodec() { }
    public static <B extends FriendlyByteBuf, P> StreamCodec<B, P> wrap(StreamCodec<B, P> original,
            Function<P, CustomPacketPayload> payload, Function<TimerPayload, P> packet) {
        return StreamCodec.of((buffer, value) -> {
            var data = payload.apply(value);
            if (data instanceof TimerPayload timer) {
                buffer.writeIdentifier(TimerPayload.ID);
                TimerPayload.<B>codec().encode(buffer, timer);
            } else if (data instanceof ChannelRegistration) {
                buffer.writeIdentifier(ChannelRegistration.TYPE.id());
                buffer.writeBytes(dev.enhancedtimer.core.SyncProtocol.CHANNEL.getBytes(StandardCharsets.UTF_8));
            } else original.encode(buffer, value);
        }, buffer -> {
            int index = buffer.readerIndex();
            var id = buffer.readIdentifier();
            if (id.equals(TimerPayload.ID)) return packet.apply(TimerPayload.<B>codec().decode(buffer));
            buffer.readerIndex(index);
            return original.decode(buffer);
        });
    }
}
