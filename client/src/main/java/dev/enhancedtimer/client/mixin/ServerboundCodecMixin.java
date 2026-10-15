package dev.enhancedtimer.client.mixin;

import dev.enhancedtimer.network.TimerWireCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerboundCustomPayloadPacket.class)
abstract class ServerboundCodecMixin {
    @Shadow @Final @Mutable public static StreamCodec<FriendlyByteBuf, ServerboundCustomPayloadPacket> STREAM_CODEC;
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void enhancedtimer$codec(CallbackInfo ci) {
        STREAM_CODEC = TimerWireCodec.wrap(STREAM_CODEC, ServerboundCustomPayloadPacket::payload, ServerboundCustomPayloadPacket::new);
    }
}
