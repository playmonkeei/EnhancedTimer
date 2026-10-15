package dev.enhancedtimer.client.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPacketListener.class)
abstract class SuggestionsMixin {
    @Inject(method = "getCommands", at = @At("RETURN"))
    private void enhancedtimer$suggest(CallbackInfoReturnable<CommandDispatcher<ClientSuggestionProvider>> cir) {
        var dispatcher = cir.getReturnValue();
        for (String alias : new String[]{"et", "enhancedtimer"}) {
            if (dispatcher.getRoot().getChild(alias) != null) continue;
            var root = LiteralArgumentBuilder.<ClientSuggestionProvider>literal(alias).executes(c -> 1);
            for (String action : new String[]{"start", "pause", "stop", "reset", "undo", "timer", "menu", "edit", "help", "splits"})
                root.then(LiteralArgumentBuilder.<ClientSuggestionProvider>literal(action).executes(c -> 1));
            root.then(LiteralArgumentBuilder.<ClientSuggestionProvider>literal("split").executes(c -> 1)
                    .then(RequiredArgumentBuilder.<ClientSuggestionProvider, String>argument("name", StringArgumentType.greedyString()).executes(c -> 1)));
            var source = LiteralArgumentBuilder.<ClientSuggestionProvider>literal("source");
            for (String value : new String[]{"local", "server"}) source.then(LiteralArgumentBuilder.<ClientSuggestionProvider>literal(value).executes(c -> 1));
            root.then(source);
            root.then(LiteralArgumentBuilder.<ClientSuggestionProvider>literal("set")
                    .then(RequiredArgumentBuilder.<ClientSuggestionProvider, String>argument("duration", StringArgumentType.word()).executes(c -> 1)));
            var design = LiteralArgumentBuilder.<ClientSuggestionProvider>literal("design");
            for (String value : new String[]{"import", "export"}) design.then(LiteralArgumentBuilder.<ClientSuggestionProvider>literal(value)
                    .then(RequiredArgumentBuilder.<ClientSuggestionProvider, String>argument("name", StringArgumentType.word()).executes(c -> 1)));
            root.then(design);
            root.then(LiteralArgumentBuilder.<ClientSuggestionProvider>literal("countdown")
                    .then(RequiredArgumentBuilder.<ClientSuggestionProvider, String>argument("duration", StringArgumentType.word())
                    .suggests((context, builder) -> { builder.suggest("2h"); builder.suggest("30m"); return builder.buildFuture(); })
                    .executes(c -> 1)));
            dispatcher.register(root);
        }
    }
}
