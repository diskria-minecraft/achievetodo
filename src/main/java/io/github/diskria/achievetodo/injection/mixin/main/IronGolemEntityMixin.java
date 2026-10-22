package io.github.diskria.achievetodo.injection.mixin.main;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.diskria.achievetodo.AchieveToDoMod;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IronGolemEntity.class)
public class IronGolemEntityMixin {

    @Inject(
        method = "interactMob",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/IronGolemEntity;heal(F)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockLandmarkTarget(
        PlayerEntity player,
        Hand hand,
        CallbackInfoReturnable<ActionResult> cir,
        @Local float currentHealth
    ) {
        IronGolemEntity ironGolemEntity = (IronGolemEntity) (Object) this;
        if (currentHealth < ironGolemEntity.getMaxHealth() &&
            AchieveToDoMod.isTargetInLockedLandmark(player, ironGolemEntity)
        ) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }
}
