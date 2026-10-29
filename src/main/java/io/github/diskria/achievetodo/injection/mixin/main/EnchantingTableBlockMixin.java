//package io.github.diskria.achievetodo.injection.mixin.main;
//
//import io.github.diskria.achievetodo.AchieveToDoMod;
//import io.github.diskria.achievetodo.ability.AbilityType;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//@Mixin(EnchantingTableBlock.class)
//public class EnchantingTableBlockMixin {
//
//    @Inject(
//        method = "onUse",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/entity/player/PlayerEntity;openHandledScreen(Lnet/minecraft/screen/NamedScreenHandlerFactory;)Ljava/util/OptionalInt;",
//            shift = At.Shift.BEFORE
//        ),
//        cancellable = true
//    )
//    public void lockEnchantingTable(
//        BlockState state,
//        World world,
//        BlockPos pos,
//        PlayerEntity player,
//        BlockHitResult hit,
//        CallbackInfoReturnable<ActionResult> cir
//    ) {
//        if (AchieveToDoMod.isAbilityLocked(player, AbilityType.OPEN_ENCHANTING_TABLE)) {
//            cir.setReturnValue(ActionResult.PASS);
//        }
//    }
//}
