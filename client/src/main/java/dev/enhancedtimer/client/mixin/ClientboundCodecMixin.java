package dev.enhancedtimer.client.mixin;

import dev.enhancedtimer.network.TimerWireCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientboundCustomPayloadPacket.class)
abstract class ClientboundCodecMixin {
    @Shadow @Final @Mutable public static StreamCodec<RegistryFriendlyByteBuf, ClientboundCustomPayloadPacket> GAMEPLAY_STREAM_CODEC;
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void enhancedtimer$codec(CallbackInfo ci) {
        GAMEPLAY_STREAM_CODEC = TimerWireCodec.wrap(GAMEPLAY_STREAM_CODEC, ClientboundCustomPayloadPacket::payload, ClientboundCustomPayloadPacket::new);
    }
}
