package dev.enhancedtimer.client.mixin;
import dev.enhancedtimer.client.EnhancedTimerClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Hud.class)
abstract class HudMixin {
    @Inject(method="extractRenderState", at=@At("TAIL"))
    private void enhancedtimer$hud(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        EnhancedTimerClient.get().renderHud(graphics);
    }
}
