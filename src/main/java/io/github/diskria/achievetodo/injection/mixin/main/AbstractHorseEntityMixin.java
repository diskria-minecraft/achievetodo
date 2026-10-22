package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractHorseEntity.class)
public class AbstractHorseEntityMixin {

    @Inject(
        method = "interactMob",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/AbstractHorseEntity;openInventory(Lnet/minecraft/entity/player/PlayerEntity;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract1(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        AbstractHorseEntity abstractHorseEntity = (AbstractHorseEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, abstractHorseEntity)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

    @Inject(
        method = "interactMob",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/AbstractHorseEntity;equipHorseArmor(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract2(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        AbstractHorseEntity abstractHorseEntity = (AbstractHorseEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, abstractHorseEntity)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

    @Inject(
        method = "interactMob",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/AbstractHorseEntity;putPlayerOnBack(Lnet/minecraft/entity/player/PlayerEntity;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract3(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        AbstractHorseEntity abstractHorseEntity = (AbstractHorseEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, abstractHorseEntity)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

    @Inject(
        method = "receiveFood",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/AbstractHorseEntity;lovePlayer(Lnet/minecraft/entity/player/PlayerEntity;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract4(PlayerEntity player, ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        AbstractHorseEntity abstractHorseEntity = (AbstractHorseEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, abstractHorseEntity)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "receiveFood",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/AbstractHorseEntity;heal(F)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract5(PlayerEntity player, ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        AbstractHorseEntity abstractHorseEntity = (AbstractHorseEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, abstractHorseEntity)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "receiveFood",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract6(PlayerEntity player, ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        AbstractHorseEntity abstractHorseEntity = (AbstractHorseEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, abstractHorseEntity)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "receiveFood",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/AbstractHorseEntity;addTemper(I)I",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract7(PlayerEntity player, ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        AbstractHorseEntity abstractHorseEntity = (AbstractHorseEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, abstractHorseEntity)) {
            cir.setReturnValue(false);
        }
    }
}
