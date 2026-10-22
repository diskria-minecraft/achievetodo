package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import net.minecraft.entity.passive.AbstractDonkeyEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractDonkeyEntity.class)
public class AbstractDonkeyEntityMixin {

    @Inject(
        method = "interactMob",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/AbstractDonkeyEntity;playAngrySound()V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract1(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        AbstractDonkeyEntity abstractDonkeyEntity = (AbstractDonkeyEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, abstractDonkeyEntity)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

    @Inject(
        method = "interactMob",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/AbstractDonkeyEntity;addChest(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract2(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        AbstractDonkeyEntity abstractDonkeyEntity = (AbstractDonkeyEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, abstractDonkeyEntity)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
