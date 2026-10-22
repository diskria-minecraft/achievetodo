package io.github.diskria.achievetodo.injection.mixin.client;

import io.github.diskria.achievetodo.ability.AbilityType;
import io.github.diskria.achievetodo.client.AchieveToDoClient;
import net.minecraft.client.tutorial.PunchTreeTutorialStepHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PunchTreeTutorialStepHandler.class)
public class PunchTreeTutorialStepHandlerMixin {

    @ModifyConstant(
        method = "tick",
        constant = @Constant(intValue = 600)
    )
    private int showToastFaster(int constant) {
        return 50;
    }

    @Inject(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/tutorial/TutorialManager;getClient()Lnet/minecraft/client/MinecraftClient;",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void scheduleUntilBlockBreakingAbilityUnlocked(CallbackInfo ci) {
        if (AchieveToDoClient.isAbilityLocked(AbilityType.BREAK_BLOCKS, true)) {
            ci.cancel();
        }
    }
}
