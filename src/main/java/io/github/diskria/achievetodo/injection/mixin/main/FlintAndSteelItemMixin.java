//package io.github.diskria.achievetodo.injection.mixin.main;
//
//import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//import com.llamalad7.mixinextras.sugar.Local;
//import io.github.diskria.achievetodo.AchieveToDoMod;
//import io.github.diskria.achievetodo.ability.AbilityType;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//
//@Mixin(FlintAndSteelItem.class)
//public class FlintAndSteelItemMixin {
//
//    @WrapOperation(
//        method = "useOnBlock",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/block/AbstractFireBlock;canPlaceAt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Z"
//        )
//    )
//    private boolean lockUseOnBlock(
//        World world,
//        BlockPos pos,
//        Direction direction,
//        Operation<Boolean> original,
//        @Local(argsOnly = true) ItemUsageContext context
//    ) {
//        if (!original.call(world, pos, direction)) {
//            return false;
//        }
//        if (AchieveToDoMod.isAbilityLocked(context.getPlayer(), AbilityType.USE_FLINT_AND_STEEL)) {
//            return false;
//        }
//        return !AchieveToDoMod.isTargetInLockedLandmark(context.getPlayer(), world, pos);
//    }
//}
