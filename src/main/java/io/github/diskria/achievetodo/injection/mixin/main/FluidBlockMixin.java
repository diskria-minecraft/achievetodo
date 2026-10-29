//package io.github.diskria.achievetodo.injection.mixin.main;
//
//import io.github.diskria.achievetodo.AchieveToDoMod;
//import org.jetbrains.annotations.Nullable;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//@Mixin(FluidBlock.class)
//public class FluidBlockMixin {
//
//    @Inject(
//        method = "tryDrainFluid",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/world/WorldAccess;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z",
//            shift = At.Shift.BEFORE
//        ),
//        cancellable = true
//    )
//    private void lockDrainFluid(
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
