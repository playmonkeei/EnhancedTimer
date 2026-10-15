package dev.enhancedtimer.client.mixin;

import dev.enhancedtimer.client.EnhancedTimerClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void enhancedtimer$tick(CallbackInfo ci) { EnhancedTimerClient.get().tick(); }
    @Inject(method = "close", at = @At("HEAD"))
    private void enhancedtimer$close(CallbackInfo ci) { EnhancedTimerClient.get().save(); }
}

