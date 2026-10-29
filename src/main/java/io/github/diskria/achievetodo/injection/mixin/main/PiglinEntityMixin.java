//package io.github.diskria.achievetodo.injection.mixin.main;
//
//import io.github.diskria.achievetodo.AchieveToDoMod;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//@Mixin(PiglinEntity.class)
//public class PiglinEntityMixin {
//
//    @Inject(
//        method = "interactMob",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/entity/mob/PiglinEntity;getWorld()Lnet/minecraft/world/World;",
//            shift = At.Shift.BEFORE
//        ),
//        cancellable = true
//    )
//    public void lockTrading(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
//        PiglinEntity piglinEntity = (PiglinEntity) (Object) this;
//        if (AchieveToDoMod.isTargetInLockedLandmark(player, piglinEntity)) {
//            cir.setReturnValue(ActionResult.FAIL);
//        }
//    }
//}
