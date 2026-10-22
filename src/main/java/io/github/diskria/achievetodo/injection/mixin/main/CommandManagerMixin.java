package io.github.diskria.achievetodo.injection.mixin.main;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.diskria.achievetodo.client.AchieveToDoClient;
import io.github.diskria.achievetodo.client.Utils;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;

@Mixin(CommandManager.class)
public class CommandManagerMixin {

    @Inject(
        method = "checkCommand",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/command/ServerCommandSource;sendError(Lnet/minecraft/text/Text;)V",
            shift = At.Shift.BEFORE,
            ordinal = 0
        ),
        cancellable = true
    )
    private static void suggestInstallAdvancementsSearchMod(
        ParseResults<ServerCommandSource> parseResults,
        @NotNull String command,
        @NotNull ServerCommandSource source,
        @NotNull CallbackInfoReturnable<ContextChain<ServerCommandSource>> cir,
        @Local @NotNull CommandSyntaxException commandSyntaxException
    ) {
        String advancementsSearchModName = "AdvancementsSearch";
        if (command.startsWith(advancementsSearchModName.toLowerCase(Locale.ROOT) + " ")) {
            ServerPlayerEntity player = source.getPlayer();
            if (player != null) {
                player.sendMessage(
                    AchieveToDoClient.translate("suggest_install_advancements_search_mod")
                        .append(Text.literal(advancementsSearchModName).styled(style -> style
                            .withClickEvent(new ClickEvent(
                                ClickEvent.Action.OPEN_URL,
                                Utils.buildModrinthModUrl(advancementsSearchModName))
                            )
                            .withUnderline(true)
                            .withColor(Formatting.GOLD)
                        ))
                );
            }
            cir.setReturnValue(null);
        }
    }
}
