//package io.github.diskria.achievetodo.injection.mixin.main;
//
//import io.github.diskria.achievetodo.AchieveToDoMod;
//import org.jetbrains.annotations.Nullable;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//@Mixin(AbstractCandleBlock.class)
//public class AbstractCandleBlockMixin {
//
//    @Inject(
//        method = "extinguish",
//        at = @At(value = "HEAD"),
//        cancellable = true
//    )
//    private static void lockExtinguish(
//        @Nullable PlayerEntity player,
//        BlockState state,
//        WorldAccess worldAccess,
//        BlockPos pos,
//        CallbackInfo ci
//    ) {
//        if (worldAccess instanceof World world && AchieveToDoMod.isTargetInLockedLandmark(player, world, pos)) {
//            ci.cancel();
//        }
//    }
//}
