package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.VehicleInventory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VehicleInventory.class)
public interface VehicleInventoryMixin {

    @Shadow
    World getWorld();

    @Shadow
    Box getBoundingBox();

    @Inject(
        method = "open",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    default void lockVehicleInventory(PlayerEntity player, CallbackInfoReturnable<ActionResult> cir) {
        if (AchieveToDoMod.isTargetInLockedLandmark(player, getWorld(), getBoundingBox())) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
