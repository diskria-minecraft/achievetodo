//package io.github.diskria.achievetodo.injection.mixin.main;
//
//import io.github.diskria.achievetodo.AchieveToDoMod;
//import io.github.diskria.achievetodo.ability.AbilityType;
//import org.jetbrains.annotations.NotNull;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//@Mixin(EquippableComponent.class)
//public class EquippableComponentMixin {
//
//    @Inject(
//        method = "equip",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/entity/player/PlayerEntity;getWorld()Lnet/minecraft/world/World;",
//            shift = At.Shift.BEFORE
//        ),
//        cancellable = true
//    )
//    public void lockEquip(
//        @NotNull ItemStack stack,
//        PlayerEntity player,
//        CallbackInfoReturnable<ActionResult> cir
//    ) {
//        if (AchieveToDoMod.isAbilityLocked(player, AbilityType.findEquipmentEquipAbility(stack.getItem()))) {
//            cir.setReturnValue(ActionResult.PASS);
//        }
//    }
//}
