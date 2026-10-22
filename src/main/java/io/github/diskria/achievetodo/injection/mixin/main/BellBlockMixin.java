package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import net.minecraft.block.BellBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BellBlock.class)
public class BellBlockMixin {

    @Inject(
        method = "ring(Lnet/minecraft/entity/Entity;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/entity/BellBlockEntity;activate(Lnet/minecraft/util/math/Direction;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private void lockRing(
        Entity ringer,
        World world,
        BlockPos pos,
        Direction direction,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (ringer instanceof PlayerEntity player && AchieveToDoMod.isTargetInLockedLandmark(player, world, pos)) {
            cir.setReturnValue(false);
        }
    }
}
