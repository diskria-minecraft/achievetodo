//package io.github.diskria.achievetodo.injection.mixin.main;
//
//import io.github.diskria.achievetodo.AchieveToDoMod;
//import io.github.diskria.achievetodo.ability.AbilityType;
//import org.objectweb.asm.Opcodes;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//@Mixin(RespawnAnchorBlock.class)
//public class RespawnAnchorBlockMixin {
//
//    @Inject(
//        method = "onUseWithItem",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/block/RespawnAnchorBlock;charge(Lnet/minecraft/entity/Entity;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V",
//            shift = At.Shift.BEFORE
//        ),
//        cancellable = true
//    )
//    public void lockCharge(
//        ItemStack stack,
//        BlockState state,
//        World world,
//        BlockPos pos,
//        PlayerEntity player,
//        Hand hand,
//        BlockHitResult hit,
//        CallbackInfoReturnable<ActionResult> cir
//    ) {
//        if (AchieveToDoMod.isAbilityLocked(player, AbilityType.CHARGE_RESPAWN_ANCHOR)) {
//            cir.setReturnValue(ActionResult.CONSUME);
//        }
//    }
//
//    @Inject(
//        method = "onUse",
//        at = @At(
//            value = "FIELD",
//            target = "Lnet/minecraft/world/World;isClient:Z",
//            ordinal = 1,
//            opcode = Opcodes.GETFIELD,
//            shift = At.Shift.BEFORE
//        ),
//        cancellable = true
//    )
//    public void lockExplode(
//        BlockState state,
//        World world,
//        BlockPos pos,
//        PlayerEntity player,
//        BlockHitResult hit,
//        CallbackInfoReturnable<ActionResult> cir
//    ) {
//        if (AchieveToDoMod.isTargetInLockedLandmark(player, world, pos)) {
//            cir.setReturnValue(ActionResult.CONSUME);
//        }
//    }
//}
