package dev.enhancedtimer.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ChannelRegistration() implements CustomPacketPayload {
    public static final Type<ChannelRegistration> TYPE = new Type<>(Identifier.parse("minecraft:register"));
    @Override public Type<ChannelRegistration> type() { return TYPE; }
}
