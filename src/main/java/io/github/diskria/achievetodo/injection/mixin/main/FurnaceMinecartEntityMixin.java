package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.FurnaceMinecartEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FurnaceMinecartEntity.class)
public class FurnaceMinecartEntityMixin {

    @Inject(
        method = "interact",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;decrementUnlessCreative(ILnet/minecraft/entity/LivingEntity;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockFurnaceMinecart(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        FurnaceMinecartEntity furnaceMinecartEntity = (FurnaceMinecartEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, furnaceMinecartEntity)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
