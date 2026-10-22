package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CamelEntity.class)
public class CamelEntityMixin {

    @Inject(
        method = "interactMob",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/CamelEntity;putPlayerOnBack(Lnet/minecraft/entity/player/PlayerEntity;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract1(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        CamelEntity camelEntity = (CamelEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, camelEntity)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

    @Inject(
        method = "receiveFood",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/CamelEntity;heal(F)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract2(PlayerEntity player, ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        CamelEntity camelEntity = (CamelEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, camelEntity)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "receiveFood",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/CamelEntity;lovePlayer(Lnet/minecraft/entity/player/PlayerEntity;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract3(PlayerEntity player, ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        CamelEntity camelEntity = (CamelEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, camelEntity)) {
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
    public void lockInteract4(PlayerEntity player, ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        CamelEntity camelEntity = (CamelEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, camelEntity)) {
            cir.setReturnValue(false);
        }
    }
}
