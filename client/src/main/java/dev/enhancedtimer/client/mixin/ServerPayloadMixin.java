package dev.enhancedtimer.client.mixin;

import dev.enhancedtimer.core.SyncProtocol;
import dev.enhancedtimer.network.TimerPayload;
import dev.enhancedtimer.server.VanillaTimerServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonPacketListenerImpl.class)
abstract class ServerPayloadMixin {
    @Shadow @Final protected MinecraftServer server;
    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void enhancedtimer$payload(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        if (packet.payload() instanceof TimerPayload payload) {
            ci.cancel();
            if ((Object)this instanceof ServerGamePacketListenerImpl listener) try {
                var message = SyncProtocol.decode(payload.bytes());
                server.execute(() -> VanillaTimerServer.get(server).receive(listener.player, message));
            } catch (java.io.IOException ignored) { }
        }
    }
}
