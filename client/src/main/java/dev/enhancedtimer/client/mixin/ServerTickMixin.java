package dev.enhancedtimer.client.mixin;

import dev.enhancedtimer.server.VanillaTimerServer;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
abstract class ServerTickMixin {
    @Inject(method = "tickServer", at = @At("TAIL"))
    private void enhancedtimer$tick(CallbackInfo ci) { VanillaTimerServer.get((MinecraftServer)(Object)this).tick(); }
    @Inject(method = "stopServer", at = @At("HEAD"))
    private void enhancedtimer$stop(CallbackInfo ci) { VanillaTimerServer.get((MinecraftServer)(Object)this).close(); }
}
