package dev.enhancedtimer.client.mixin;

import dev.enhancedtimer.client.Keys;
import java.util.Arrays;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
abstract class OptionsMixin {
    @Shadow @Final @Mutable public KeyMapping[] keyMappings;
    @Inject(method = "load", at = @At("HEAD"))
    private void enhancedtimer$keys(CallbackInfo ci) {
        if (Arrays.asList(keyMappings).contains(Keys.MENU)) return;
        int n = keyMappings.length;
        keyMappings = Arrays.copyOf(keyMappings, n + 3);
        keyMappings[n] = Keys.MENU;
        keyMappings[n + 1] = Keys.TOGGLE;
        keyMappings[n + 2] = Keys.STOP;
    }
}

