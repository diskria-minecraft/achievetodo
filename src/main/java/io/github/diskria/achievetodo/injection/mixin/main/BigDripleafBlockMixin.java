package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import net.minecraft.block.BigDripleafBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BigDripleafBlock.class)
public class BigDripleafBlockMixin {

    @Inject(
        method = "onProjectileHit",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    private void lockTilt(
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
