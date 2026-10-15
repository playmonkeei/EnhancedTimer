package dev.enhancedtimer.client;

import dev.enhancedtimer.core.SyncProtocol;
import dev.enhancedtimer.network.*;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class NetworkCodecTest {
    @BeforeAll static void bootstrap() { net.minecraft.SharedConstants.tryDetectVersion(); net.minecraft.server.Bootstrap.bootStrap(); }
    private static net.minecraft.network.codec.StreamCodec<FriendlyByteBuf, ServerboundCustomPayloadPacket> codec() {
        return TimerWireCodec.wrap(ServerboundCustomPayloadPacket.STREAM_CODEC, ServerboundCustomPayloadPacket::payload, ServerboundCustomPayloadPacket::new);
    }
    @Test void timerPacketsUseIdenticalPluginMessagingBytes() throws Exception {
        var message = new SyncProtocol.Message(SyncProtocol.Kind.HELLO, "", "", 0, null, false, false, 0);
        byte[] bytes = SyncProtocol.encode(message);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            codec().encode(buffer, new ServerboundCustomPayloadPacket(new TimerPayload(bytes)));
            assertEquals(TimerPayload.ID, buffer.readIdentifier());
            byte[] body = new byte[buffer.readableBytes()]; buffer.readBytes(body); assertArrayEquals(bytes, body);
            buffer.readerIndex(0);
            var restored = (TimerPayload)codec().decode(buffer).payload();
            assertEquals(message, SyncProtocol.decode(restored.bytes())); assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); }
    }
    @Test void otherPayloadsRetainOriginalCodec() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            codec().encode(buffer, new ServerboundCustomPayloadPacket(new BrandPayload("Other mod loader")));
            assertEquals("Other mod loader", ((BrandPayload)codec().decode(buffer).payload()).brand());
            assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); }
    }
    @Test void channelRegistrationIsAStandardVanillaPayload() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            codec().encode(buffer, new ServerboundCustomPayloadPacket(new ChannelRegistration()));
            assertEquals(ChannelRegistration.TYPE.id(), buffer.readIdentifier());
            assertEquals(SyncProtocol.CHANNEL, buffer.toString(java.nio.charset.StandardCharsets.UTF_8));
        } finally { buffer.release(); }
    }
    @Test void oversizedTimerPayloadCannotAllocateUnboundedMemory() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeIdentifier(TimerPayload.ID); buffer.writeZero(SyncProtocol.MAX_BYTES + 1);
            assertThrows(IllegalArgumentException.class, () -> codec().decode(buffer));
        } finally { buffer.release(); }
    }
}
