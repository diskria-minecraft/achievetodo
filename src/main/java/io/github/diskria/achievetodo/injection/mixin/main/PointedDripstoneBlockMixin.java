//package io.github.diskria.achievetodo.injection.mixin.main;
//
//import io.github.diskria.achievetodo.AchieveToDoMod;
//import org.jetbrains.annotations.NotNull;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//@Mixin(PointedDripstoneBlock.class)
//public class PointedDripstoneBlockMixin {
//
//    @Inject(
//        method = "onProjectileHit",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/world/World;breakBlock(Lnet/minecraft/util/math/BlockPos;Z)Z",
//            shift = At.Shift.BEFORE
//        ),
//        cancellable = true
//    )
//    private void lockBreakByProjectile(
//        World world,
//        BlockState state,
//        BlockHitResult hit,
//        @NotNull ProjectileEntity projectile,
//        CallbackInfo ci
//    ) {
//        if (projectile.getOwner() instanceof PlayerEntity player &&
//            AchieveToDoMod.isTargetInLockedLandmark(player, world, hit.getBlockPos())
//        ) {
//            ci.cancel();
//        }
//    }
//}
