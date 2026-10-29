//package io.github.diskria.achievetodo.injection.mixin.main;
//
//import io.github.diskria.achievetodo.AchieveToDoMod;
//import io.github.diskria.achievetodo.ability.AbilityType;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//@Mixin(JukeboxPlayableComponent.class)
//public class JukeboxPlayableComponentMixin {
//
//    @Inject(
//        method = "tryPlayStack",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/item/ItemStack;splitUnlessCreative(ILnet/minecraft/entity/LivingEntity;)Lnet/minecraft/item/ItemStack;",
//            shift = At.Shift.BEFORE
//        ),
//        cancellable = true
//    )
//    private static void lockJukebox(
//        World world,
//        BlockPos pos,
//        ItemStack stack,
//        PlayerEntity player,
//        CallbackInfoReturnable<ActionResult> cir
//    ) {
//        if (AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_JUKEBOX)) {
//            cir.setReturnValue(ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION);
//        }
//    }
//}
