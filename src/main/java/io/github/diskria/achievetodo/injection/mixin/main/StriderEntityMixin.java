package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import net.minecraft.entity.passive.StriderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StriderEntity.class)
public class StriderEntityMixin {

    @Inject(
        method = "interactMob",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/World;isClient:Z",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract1(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        StriderEntity striderEntity = (StriderEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, striderEntity)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
