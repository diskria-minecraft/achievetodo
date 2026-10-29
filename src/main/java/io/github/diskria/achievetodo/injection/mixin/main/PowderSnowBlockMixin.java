//package io.github.diskria.achievetodo.injection.mixin.main;
//
//import io.github.diskria.achievetodo.AchieveToDoMod;
//import org.jetbrains.annotations.Nullable;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//@Mixin(PowderSnowBlock.class)
//public class PowderSnowBlockMixin {
//
//    @Inject(
//        method = "tryDrainFluid",
//        at = @At(value = "HEAD"),
//        cancellable = true
//    )
//    public void lockDrainFluid(
//        @Nullable PlayerEntity player,
//        WorldAccess worldAccess,
//        BlockPos pos,
//        BlockState state,
//        CallbackInfoReturnable<ItemStack> cir
//    ) {
//        if (worldAccess instanceof World world && AchieveToDoMod.isTargetInLockedLandmark(player, world, pos)) {
//            cir.setReturnValue(ItemStack.EMPTY);
//        }
//    }
//}
