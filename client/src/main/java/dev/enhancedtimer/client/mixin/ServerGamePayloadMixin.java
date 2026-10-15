package dev.enhancedtimer.client.mixin;

import dev.enhancedtimer.core.SyncProtocol;
import dev.enhancedtimer.network.TimerPayload;
import dev.enhancedtimer.server.VanillaTimerServer;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The gameplay listener overrides the common handler, including on vanilla and Forge. */
@Mixin(ServerGamePacketListenerImpl.class)
abstract class ServerGamePayloadMixin {
    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void enhancedtimer$receive(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        if (packet.payload() instanceof TimerPayload payload) {
            ci.cancel();
            var listener = (ServerGamePacketListenerImpl)(Object)this;
            var server = listener.player.level().getServer();
            try {
                var message = SyncProtocol.decode(payload.bytes());
                server.execute(() -> VanillaTimerServer.get(server).receive(listener.player, message));
            } catch (java.io.IOException ignored) { }
        }
    }
}
