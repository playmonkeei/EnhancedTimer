package dev.enhancedtimer.client.mixin;

import dev.enhancedtimer.server.VanillaTimerServer;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IntegratedServer.class)
abstract class IntegratedPauseMixin {
    @Inject(method = "tickServer", at = @At("HEAD"))
    private void enhancedtimer$pause(CallbackInfo ci) {
        var server = (IntegratedServer)(Object)this;
        if (server.isPaused()) for (var session : VanillaTimerServer.get(server).hub.sessions().values()) session.timer.suspend(true);
    }
}
