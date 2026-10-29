//package io.github.diskria.achievetodo.injection.mixin.main;
//
//import io.github.diskria.achievetodo.AchieveToDoMod;
//import io.github.diskria.achievetodo.ability.AbilityType;
//import org.jetbrains.annotations.NotNull;
//import org.spongepowered.asm.mixin.Final;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Shadow;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//@Mixin(TrapdoorBlock.class)
//public class TrapdoorBlockMixin {
//
//    @Shadow
//    @Final
//    public static BooleanProperty OPEN;
//
//    @Inject(
//        method = "onUse",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/block/TrapdoorBlock;flip(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/PlayerEntity;)V",
//            shift = At.Shift.BEFORE
//        ),
//        cancellable = true
//    )
//    public void lockTrapdoor(
//        @NotNull BlockState state,
//        World world,
//        BlockPos pos,
//        PlayerEntity player,
//        BlockHitResult hit,
//        CallbackInfoReturnable<ActionResult> cir
//    ) {
//        if (!state.get(OPEN) && AchieveToDoMod.isAbilityLocked(player, AbilityType.OPEN_TRAPDOOR)) {
//            cir.setReturnValue(ActionResult.SUCCESS);
//        }
//    }
//}
