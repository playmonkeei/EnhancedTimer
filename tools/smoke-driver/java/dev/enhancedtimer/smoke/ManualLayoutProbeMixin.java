package dev.enhancedtimer.smoke;

import dev.enhancedtimer.client.ui.LayoutScreen;
import java.nio.file.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observes actual native input delivery without invoking or altering UI handlers. */
@Mixin(value = LayoutScreen.class, remap = false)
abstract class ManualLayoutProbeMixin {
    @Unique private void enhancedtimer$record(String kind, MouseButtonEvent event) {
        if (!Boolean.getBoolean("enhancedtimer.manualProbe")) return;
        try {
            var folder = Minecraft.getInstance().gameDirectory.toPath().resolve("manual-evidence");
            Files.createDirectories(folder);
            Files.writeString(folder.resolve("layout-input.jsonl"),
                    "{\"at\":\"" + java.time.Instant.now() + "\",\"event\":\"" + kind
                    + "\",\"x\":" + event.x() + ",\"y\":" + event.y() + ",\"button\":" + event.button() + "}\n",
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
    }
    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void enhancedtimer$click(MouseButtonEvent event, boolean twice, CallbackInfoReturnable<Boolean> ci) { enhancedtimer$record("click", event); }
    @Inject(method = "mouseDragged", at = @At("HEAD"))
    private void enhancedtimer$drag(MouseButtonEvent event, double dx, double dy, CallbackInfoReturnable<Boolean> ci) { enhancedtimer$record("drag", event); }
    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void enhancedtimer$release(MouseButtonEvent event, CallbackInfoReturnable<Boolean> ci) { enhancedtimer$record("release", event); }
}
