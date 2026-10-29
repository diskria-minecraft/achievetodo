//package io.github.diskria.achievetodo.injection.mixin.main;
//
//import io.github.diskria.achievetodo.AchieveToDoMod;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//@Mixin(ArmorStandEntity.class)
//public class ArmorStandEntityMixin {
//
//    @Inject(
//        method = "equip",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/entity/decoration/ArmorStandEntity;equipStack(Lnet/minecraft/entity/EquipmentSlot;Lnet/minecraft/item/ItemStack;)V",
//            ordinal = 2
//        ),
//        cancellable = true
//    )
//    public void lockExtractArmor(
//        PlayerEntity player,
//        EquipmentSlot slot,
//        ItemStack stack,
//        Hand hand,
//        CallbackInfoReturnable<Boolean> cir
//    ) {
//        ArmorStandEntity armorStandEntity = (ArmorStandEntity) (Object) this;
//        if (AchieveToDoMod.isTargetInLockedLandmark(player, armorStandEntity)) {
//            cir.setReturnValue(false);
//        }
//    }
//}
