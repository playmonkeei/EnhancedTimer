package dev.enhancedtimer.client.mixin;

import dev.enhancedtimer.client.EnhancedTimerClient;
import dev.enhancedtimer.core.SyncProtocol;
import dev.enhancedtimer.network.TimerPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
abstract class ClientPayloadMixin {
    @Inject(method = "handleCustomPayload(Lnet/minecraft/network/protocol/common/ClientboundCustomPayloadPacket;)V", at = @At("HEAD"), cancellable = true)
    private void enhancedtimer$receive(ClientboundCustomPayloadPacket packet, CallbackInfo ci) {
        if (packet.payload() instanceof TimerPayload payload) {
            ci.cancel();
            try {
                var message = SyncProtocol.decode(payload.bytes());
                Minecraft.getInstance().execute(() -> EnhancedTimerClient.get().sync.receive(message));
            } catch (java.io.IOException ignored) { }
        }
    }
}
