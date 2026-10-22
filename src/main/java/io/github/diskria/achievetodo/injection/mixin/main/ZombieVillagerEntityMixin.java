package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ZombieVillagerEntity.class)
public class ZombieVillagerEntityMixin {

    @Inject(
        method = "interactMob",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;decrementUnlessCreative(ILnet/minecraft/entity/LivingEntity;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockAnimalsInteract(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ZombieVillagerEntity zombieVillagerEntity = (ZombieVillagerEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, zombieVillagerEntity)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
