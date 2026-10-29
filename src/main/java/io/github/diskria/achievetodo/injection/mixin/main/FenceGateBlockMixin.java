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
//@Mixin(FenceGateBlock.class)
//public class FenceGateBlockMixin {
//
//    @Shadow
//    @Final
//    public static BooleanProperty OPEN;
//
//    @Inject(
//        method = "onUse",
//        at = @At(value = "HEAD"),
//        cancellable = true
//    )
//    public void lockFenceGate(
//        @NotNull BlockState state,
//        World world,
//        BlockPos pos,
//        PlayerEntity player,
//        BlockHitResult hit,
//        CallbackInfoReturnable<ActionResult> cir
//    ) {
//        if (!state.get(OPEN) && AchieveToDoMod.isAbilityLocked(player, AbilityType.OPEN_FENCE_GATE)) {
//            cir.setReturnValue(ActionResult.SUCCESS);
//        }
//    }
//}
