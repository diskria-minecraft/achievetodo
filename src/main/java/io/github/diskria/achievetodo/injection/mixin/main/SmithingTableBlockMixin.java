//package io.github.diskria.achievetodo.injection.mixin.main;
//
//import io.github.diskria.achievetodo.AchieveToDoMod;
//import io.github.diskria.achievetodo.ability.AbilityType;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//@Mixin(SmithingTableBlock.class)
//public class SmithingTableBlockMixin {
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
//    public void lockSmithingTable(
//        BlockState state,
//        World world,
//        BlockPos pos,
//        PlayerEntity player,
//        BlockHitResult hit,
//        CallbackInfoReturnable<ActionResult> cir
//    ) {
//        if (AchieveToDoMod.isAbilityLocked(player, AbilityType.OPEN_SMITHING_TABLE)) {
//            cir.setReturnValue(ActionResult.SUCCESS);
//        }
//    }
//}
