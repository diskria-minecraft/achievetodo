//package io.github.diskria.achievetodo.injection.mixin.main;
//
//import io.github.diskria.achievetodo.AchieveToDoMod;
//import io.github.diskria.achievetodo.ability.AbilityType;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//@Mixin(WolfEntity.class)
//public class WolfEntityMixin {
//
//    @Inject(
//        method = "interactMob",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/item/ItemStack;damage(ILnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/EquipmentSlot;)V",
//            shift = At.Shift.BEFORE
//        ),
//        cancellable = true
//    )
//    public void lockShearing(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
//        WolfEntity wolfEntity = (WolfEntity) (Object) this;
//        if (AchieveToDoMod.isTargetInLockedLandmark(player, wolfEntity) ||
//            AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_SHEARS)
//        ) {
//            cir.setReturnValue(ActionResult.SUCCESS);
//        }
//    }
//}
