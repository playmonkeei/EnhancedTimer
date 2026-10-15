package dev.enhancedtimer.client.mixin;

import dev.enhancedtimer.client.EnhancedTimerClient;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only typed chat input is intercepted; server click events cannot start a local timer. */
@Mixin(ChatScreen.class)
abstract class ChatMixin {
    @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
    private void enhancedtimer$command(String input, boolean addToHistory, CallbackInfo ci) {
        if (EnhancedTimerClient.get().command(input)) {
            if (addToHistory) dev.enhancedtimer.client.Platform.history(input);
            ci.cancel();
        }
    }
}

