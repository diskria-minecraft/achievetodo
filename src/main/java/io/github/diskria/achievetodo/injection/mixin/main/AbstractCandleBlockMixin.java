package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import net.minecraft.block.AbstractCandleBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractCandleBlock.class)
public class AbstractCandleBlockMixin {

    @Inject(
        method = "extinguish",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    private static void lockExtinguish(
        @Nullable PlayerEntity player,
        BlockState state,
        WorldAccess worldAccess,
        BlockPos pos,
        CallbackInfo ci
    ) {
        if (worldAccess instanceof World world && AchieveToDoMod.isTargetInLockedLandmark(player, world, pos)) {
            ci.cancel();
        }
    }

    @Inject(
        method = "onProjectileHit",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/AbstractCandleBlock;setLit(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Z)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private void lockLit(
        World world,
        BlockState state,
        BlockHitResult hit,
        @NotNull ProjectileEntity projectile,
        CallbackInfo ci
    ) {
        if (projectile.getOwner() instanceof PlayerEntity player &&
            AchieveToDoMod.isTargetInLockedLandmark(player, world, hit.getBlockPos())
        ) {
            ci.cancel();
        }
    }
}
