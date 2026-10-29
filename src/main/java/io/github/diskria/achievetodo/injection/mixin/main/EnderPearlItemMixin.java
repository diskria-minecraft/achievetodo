//package io.github.diskria.achievetodo.injection.mixin.main;
//
//import io.github.diskria.achievetodo.AchieveToDoMod;
//import io.github.diskria.achievetodo.ability.AbilityType;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//@Mixin(EnderPearlItem.class)
//public class EnderPearlItemMixin {
//
//    @Inject(
//        method = "use",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/entity/player/PlayerEntity;getStackInHand(Lnet/minecraft/util/Hand;)Lnet/minecraft/item/ItemStack;"
//        ),
//        cancellable = true
//    )
//    public void lockThrow(
//        World world,
//        PlayerEntity player,
//        Hand hand,
//        CallbackInfoReturnable<ActionResult> cir
//    ) {
//        if (AchieveToDoMod.isAbilityLocked(player, AbilityType.THROW_ENDER_PEARL)) {
//            cir.setReturnValue(ActionResult.PASS);
//        }
//    }
//}
