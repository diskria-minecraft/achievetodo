//package io.github.diskria.achievetodo.injection.mixin.main;
//
//import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//import com.llamalad7.mixinextras.sugar.Local;
//import io.github.diskria.achievetodo.AchieveToDoMod;
//import io.github.diskria.achievetodo.ability.AbilityType;
//import org.jetbrains.annotations.NotNull;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//
//@Mixin(ShulkerBoxBlock.class)
//public class ShulkerBoxBlockMixin {
//
//    @WrapOperation(
//        method = "onUse",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/block/ShulkerBoxBlock;canOpen(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/ShulkerBoxBlockEntity;)Z"
//        )
//    )
//    private boolean lockOpening(
//        BlockState state,
//        World world,
//        BlockPos pos,
//        @NotNull ShulkerBoxBlockEntity blockEntity,
//        @NotNull Operation<Boolean> original,
//        @Local(argsOnly = true) PlayerEntity player
//    ) {
//        if (!original.call(state, world, pos, blockEntity)) {
//            return false;
//        }
//        if (AchieveToDoMod.isTargetInLockedLandmark(player, world, pos)) {
//            return false;
//        }
//        if (blockEntity.getAnimationStage() != ShulkerBoxBlockEntity.AnimationStage.CLOSED) {
//            return true;
//        }
//        return !AchieveToDoMod.isAbilityLocked(player, AbilityType.OPEN_SHULKER_BOX);
//    }
//}
