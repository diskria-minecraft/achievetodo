package io.github.diskria.achievetodo.injection.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.diskria.achievetodo.client.AchieveToDoClient;
import io.github.diskria.achievetodo.client.gui.DesignCodePalette;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DeathScreen.class)
public class DeathScreenMixin {

    @Shadow
    @Final
    private boolean isHardcore;

    @WrapOperation(
        method = "init",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/text/Text;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/text/MutableText;"
        )
    )
    private @NotNull MutableText showObtainedCountInsteadScoreInHardcore(
        String key,
        Object[] args,
        @NotNull Operation<MutableText> original
    ) {
        if (isHardcore && !AchieveToDoClient.isNotReady()) {
            return Text.translatable("key.advancements")
                .append(": ")
                .append(
                    Text.literal(String.valueOf(AchieveToDoClient.getObtainedAdvancementsCount()))
                        .formatted(DesignCodePalette.TEXT_COLOR)
                );
        }
        return original.call(key, args);
    }
}
