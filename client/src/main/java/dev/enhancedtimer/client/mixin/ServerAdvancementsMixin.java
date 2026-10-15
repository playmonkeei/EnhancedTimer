package dev.enhancedtimer.client.mixin;

import dev.enhancedtimer.server.VanillaTimerServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.advancements.AdvancementHolder;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancements.class)
abstract class ServerAdvancementsMixin {
    @Shadow private ServerPlayer player;
    @Inject(method = "award", at = @At("RETURN"))
    private void enhancedtimer$milestone(AdvancementHolder advancement, String criterion, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || !player.getAdvancements().getOrStartProgress(advancement).isDone()) return;
        String event = switch (advancement.id().toString()) {
            case "minecraft:story/enter_the_nether" -> "nether";
            case "minecraft:story/enter_the_end" -> "end";
            case "minecraft:end/kill_dragon" -> "dragon";
            default -> null;
        };
        if (event != null) VanillaTimerServer.get(player.level().getServer()).event(player, event);
    }
}
