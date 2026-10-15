package dev.enhancedtimer.client.mixin;

import dev.enhancedtimer.server.VanillaTimerServer;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin {
    @Inject(method = "die", at = @At("TAIL"))
    private void enhancedtimer$death(DamageSource source, CallbackInfo ci) {
        var self = (ServerPlayer)(Object)this;
        VanillaTimerServer.get(self.level().getServer()).event(self, "death");
    }
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void enhancedtimer$protect(ServerLevel level, DamageSource damage, float amount, CallbackInfoReturnable<Boolean> cir) {
        var self = (ServerPlayer)(Object)this; var service = VanillaTimerServer.get(level.getServer());
        if (service.paused(self) && service.rules(self).protectPaused()) cir.setReturnValue(false);
    }
}
