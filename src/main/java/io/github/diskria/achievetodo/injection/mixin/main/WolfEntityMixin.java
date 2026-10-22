package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.AbilityType;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WolfEntity.class)
public class WolfEntityMixin {

    @Inject(
        method = "interactMob",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/WolfEntity;eat(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/item/ItemStack;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract1(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        WolfEntity wolfEntity = (WolfEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, wolfEntity)) {
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    @Inject(
        method = "interactMob",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/WolfEntity;setCollarColor(Lnet/minecraft/util/DyeColor;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract2(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        WolfEntity wolfEntity = (WolfEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, wolfEntity)) {
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    @Inject(
        method = "interactMob",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/WolfEntity;equipBodyArmor(Lnet/minecraft/item/ItemStack;)V",
            shift = At.Shift.BEFORE,
            ordinal = 0
        ),
        cancellable = true
    )
    public void lockInteract3(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        WolfEntity wolfEntity = (WolfEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, wolfEntity)) {
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    @Inject(
        method = "interactMob",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;damage(ILnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/EquipmentSlot;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract4(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        WolfEntity wolfEntity = (WolfEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, wolfEntity) ||
            AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_SHEARS)
        ) {
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    @Inject(
        method = "interactMob",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;decrement(I)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract5(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        WolfEntity wolfEntity = (WolfEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, wolfEntity)) {
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    @Inject(
        method = "interactMob",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/WolfEntity;setSitting(Z)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract6(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        WolfEntity wolfEntity = (WolfEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, wolfEntity)) {
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    @Inject(
        method = "interactMob",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;decrementUnlessCreative(ILnet/minecraft/entity/LivingEntity;)V",
            shift = At.Shift.BEFORE,
            ordinal = 2
        ),
        cancellable = true
    )
    public void lockInteract7(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        WolfEntity wolfEntity = (WolfEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, wolfEntity)) {
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }
}
