//package io.github.diskria.achievetodo.injection.mixin.main;
//
//import io.github.diskria.achievetodo.AchieveToDoMod;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//@Mixin(AllayEntity.class)
//public class AllayEntityMixin {
//
//    @Inject(
//        method = "interactMob",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/entity/passive/AllayEntity;equipStack(Lnet/minecraft/entity/EquipmentSlot;Lnet/minecraft/item/ItemStack;)V",
//            shift = At.Shift.BEFORE
//        ),
//        cancellable = true
//    )
//    public void lockExtractItem(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
//        AllayEntity allayEntity = (AllayEntity) (Object) this;
//        if (AchieveToDoMod.isTargetInLockedLandmark(player, allayEntity)) {
//            cir.setReturnValue(ActionResult.FAIL);
//        }
//    }
//}
