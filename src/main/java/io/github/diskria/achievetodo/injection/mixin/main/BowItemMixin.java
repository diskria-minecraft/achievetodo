//package io.github.diskria.achievetodo.injection.mixin.main;
//
//import io.github.diskria.achievetodo.AchieveToDoMod;
//import io.github.diskria.achievetodo.ability.AbilityType;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//@Mixin(BowItem.class)
//public class BowItemMixin {
//
//    @Inject(
//        method = "use",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/entity/player/PlayerEntity;setCurrentHand(Lnet/minecraft/util/Hand;)V",
//            shift = At.Shift.BEFORE
//        ),
//        cancellable = true
//    )
//    public void lockBowShoot(
//        World world,
//        PlayerEntity player,
//        Hand hand,
//        CallbackInfoReturnable<ActionResult> cir
//    ) {
//        if (AchieveToDoMod.isAbilityLocked(player, AbilityType.SHOOT_BOW)) {
//            cir.setReturnValue(ActionResult.FAIL);
//        }
//    }
//}
