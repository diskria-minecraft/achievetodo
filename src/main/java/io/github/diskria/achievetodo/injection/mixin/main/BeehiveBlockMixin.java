//package io.github.diskria.achievetodo.injection.mixin.main;
//
//import io.github.diskria.achievetodo.AchieveToDoMod;
//import io.github.diskria.achievetodo.ability.AbilityType;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//@Mixin(BeehiveBlock.class)
//public class BeehiveBlockMixin {
//
//    @Inject(
//        method = "onUseWithItem",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V",
//            shift = At.Shift.BEFORE,
//            ordinal = 0
//        ),
//        cancellable = true
//    )
//    public void lockExtractHoneycomb(
//        ItemStack stack,
//        BlockState state,
//        World world,
//        BlockPos pos,
//        PlayerEntity player,
//        Hand hand,
//        BlockHitResult hit,
//        CallbackInfoReturnable<ActionResult> cir
//    ) {
//        if (AchieveToDoMod.isTargetInLockedLandmark(player, world, pos) ||
//            AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_SHEARS)
//        ) {
//            cir.setReturnValue(ActionResult.PASS);
//        }
//    }
//
//    @Inject(
//        method = "onUseWithItem",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/item/ItemStack;decrement(I)V",
//            shift = At.Shift.BEFORE,
//            ordinal = 0
//        ),
//        cancellable = true
//    )
//    public void lockExtractHoneyBottle(
//        ItemStack stack,
//        BlockState state,
//        World world,
//        BlockPos pos,
//        PlayerEntity player,
//        Hand hand,
//        BlockHitResult hit,
//        CallbackInfoReturnable<ActionResult> cir
//    ) {
//        if (AchieveToDoMod.isTargetInLockedLandmark(player, world, pos)) {
//            cir.setReturnValue(ActionResult.PASS);
//        }
//    }
//}
