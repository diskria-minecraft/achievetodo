package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import net.minecraft.entity.passive.PandaEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PandaEntity.class)
public class PandaEntityMixin {

    @Inject(
        method = "interactMob",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/PandaEntity;eat(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/item/ItemStack;)V",
            shift = At.Shift.BEFORE,
            ordinal = 0
        ),
        cancellable = true
    )
    public void lockInteract1(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        PandaEntity pandaEntity = (PandaEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, pandaEntity)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

    @Inject(
        method = "interactMob",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/PandaEntity;eat(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/item/ItemStack;)V",
            shift = At.Shift.BEFORE,
            ordinal = 1
        ),
        cancellable = true
    )
    public void lockInteract2(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        PandaEntity pandaEntity = (PandaEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, pandaEntity)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

    @Inject(
        method = "interactMob",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/PandaEntity;stop()V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract3(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        PandaEntity pandaEntity = (PandaEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, pandaEntity)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
