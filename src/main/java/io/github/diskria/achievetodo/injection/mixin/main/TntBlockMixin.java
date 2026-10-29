//package io.github.diskria.achievetodo.injection.mixin.main;
//
//import io.github.diskria.achievetodo.AchieveToDoMod;
//import io.github.diskria.achievetodo.ability.AbilityType;
//import org.jetbrains.annotations.NotNull;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//@Mixin(TntBlock.class)
//public class TntBlockMixin {
//
//    @Inject(
//        method = "onUseWithItem",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/block/TntBlock;primeTnt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/LivingEntity;)V",
//            shift = At.Shift.BEFORE
//        ),
//        cancellable = true
//    )
//    public void lockDirectIgnite(
//        @NotNull ItemStack stack,
//        BlockState state,
//        World world,
//        BlockPos pos,
//        PlayerEntity player,
//        Hand hand,
//        BlockHitResult hit,
//        CallbackInfoReturnable<ActionResult> cir
//    ) {
//        if (AchieveToDoMod.isTargetInLockedLandmark(player, world, pos)) {
//            cir.setReturnValue(ActionResult.SUCCESS);
//            return;
//        }
//        if (stack.isOf(Items.FLINT_AND_STEEL) &&
//            AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_FLINT_AND_STEEL)
//        ) {
//            cir.setReturnValue(ActionResult.SUCCESS);
//            return;
//        }
//        if (AchieveToDoMod.isAbilityLocked(player, AbilityType.IGNITE_TNT)) {
//            cir.setReturnValue(ActionResult.SUCCESS);
//        }
//    }
//
//    @Inject(
//        method = "onProjectileHit",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/block/TntBlock;primeTnt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/LivingEntity;)V",
//            shift = At.Shift.BEFORE
//        ),
//        cancellable = true
//    )
//    public void lockIgniteByProjectile(
//        World world,
//        BlockState state,
//        BlockHitResult hit,
//        @NotNull ProjectileEntity projectile,
//        CallbackInfo ci
//    ) {
//        if (projectile.getOwner() instanceof PlayerEntity player) {
//            if (AchieveToDoMod.isTargetInLockedLandmark(player, world, hit.getBlockPos()) ||
//                AchieveToDoMod.isAbilityLocked(player, AbilityType.IGNITE_TNT)
//            ) {
//                ci.cancel();
//            }
//        }
//    }
//}
