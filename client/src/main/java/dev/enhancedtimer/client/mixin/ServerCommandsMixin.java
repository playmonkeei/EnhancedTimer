package dev.enhancedtimer.client.mixin;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import dev.enhancedtimer.server.VanillaTimerServer;
import net.minecraft.commands.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
abstract class ServerCommandsMixin {
    @Shadow @Final private CommandDispatcher<CommandSourceStack> dispatcher;
    @Inject(method = "<init>", at = @At("TAIL"))
    private void enhancedtimer$commands(CallbackInfo ci) {
        for (String name : new String[]{"ets", "servertimer", "enhancedtimer-server"}) dispatcher.register(Commands.literal(name)
                .executes(context -> VanillaTimerServer.get(context.getSource().getServer()).command(context.getSource(), ""))
                .then(Commands.argument("action", StringArgumentType.greedyString())
                .executes(context -> VanillaTimerServer.get(context.getSource().getServer()).command(context.getSource(), StringArgumentType.getString(context, "action")))));
    }
}
