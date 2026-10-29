//package io.github.diskria.achievetodo.injection.mixin.main;
//
//import io.github.diskria.achievetodo.AchieveToDoMod;
//import io.github.diskria.achievetodo.ability.AbilityType;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//@Mixin(CampfireBlock.class)
//public class CampfireBlockMixin {
//
//    @Inject(
//        method = "onUseWithItem",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/block/entity/CampfireBlockEntity;addItem(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;)Z",
//            shift = At.Shift.BEFORE
//        ),
//        cancellable = true
//    )
//    public void lockAddItem(
//        ItemStack stack,
//        BlockState state,
//        World world,
//        BlockPos pos,
//        PlayerEntity player,
//        Hand hand,
//        BlockHitResult hit,
//        CallbackInfoReturnable<ActionResult> cir
//    ) {
//        if (AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_CAMPFIRE)) {
//            cir.setReturnValue(ActionResult.CONSUME);
//        }
//    }
//}
