package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParrotEntity.class)
public class ParrotEntityMixin {

    @Inject(
        method = "interactMob",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/ParrotEntity;eat(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/item/ItemStack;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract1(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ParrotEntity parrotEntity = (ParrotEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, parrotEntity)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

    @Inject(
        method = "interactMob",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/World;isClient:Z",
            shift = At.Shift.BEFORE,
            ordinal = 1
        ),
        cancellable = true
    )
    public void lockInteract2(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ParrotEntity parrotEntity = (ParrotEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, parrotEntity)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
