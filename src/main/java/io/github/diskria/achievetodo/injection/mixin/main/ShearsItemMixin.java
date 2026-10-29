//package io.github.diskria.achievetodo.injection.mixin.main;
//
//import io.github.diskria.achievetodo.AchieveToDoMod;
//import io.github.diskria.achievetodo.ability.AbilityType;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//@Mixin(ShearsItem.class)
//public class ShearsItemMixin {
//
//    @Inject(
//        method = "useOnBlock",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/item/ItemUsageContext;getStack()Lnet/minecraft/item/ItemStack;",
//            shift = At.Shift.BEFORE
//        ),
//        cancellable = true
//    )
//    public void lockShearing(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
//        if (AchieveToDoMod.isTargetInLockedLandmark(context) ||
//            AchieveToDoMod.isAbilityLocked(context.getPlayer(), AbilityType.USE_SHEARS)
//        ) {
//            cir.setReturnValue(ActionResult.PASS);
//        }
//    }
//}
