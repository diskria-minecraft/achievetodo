//package io.github.diskria.achievetodo.injection.mixin.main;
//
//import io.github.diskria.achievetodo.AchieveToDoMod;
//import io.github.diskria.achievetodo.ability.AbilityType;
//import org.jetbrains.annotations.NotNull;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Unique;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//@Mixin(ShovelItem.class)
//public class ShovelItemMixin {
//
//    @Unique
//    private ToolMaterial material;
//
//    @Inject(
//        method = "<init>",
//        at = @At("TAIL")
//    )
//    private void saveMaterial(
//        ToolMaterial material,
//        float attackDamage,
//        float attackSpeed,
//        Item.Settings settings,
//        CallbackInfo ci
//    ) {
//        this.material = material;
//    }
//
//    @Inject(
//        method = "useOnBlock",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V",
//            shift = At.Shift.BEFORE
//        ),
//        cancellable = true
//    )
//    public void lockFlat(@NotNull ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
//        if (AchieveToDoMod.isTargetInLockedLandmark(context) ||
//            AchieveToDoMod.isAbilityLocked(context.getPlayer(), AbilityType.findToolMaterialUsageAbility(material))
//        ) {
//            cir.setReturnValue(ActionResult.PASS);
//        }
//    }
//
//    @Inject(
//        method = "useOnBlock",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/world/World;isClient()Z",
//            shift = At.Shift.BEFORE
//        ),
//        cancellable = true
//    )
//    public void lockCampfireExtinguish(
//        @NotNull ItemUsageContext context,
//        CallbackInfoReturnable<ActionResult> cir
//    ) {
//        if (AchieveToDoMod.isTargetInLockedLandmark(context) ||
//            AchieveToDoMod.isAbilityLocked(context.getPlayer(), AbilityType.findToolMaterialUsageAbility(material))
//        ) {
//            cir.setReturnValue(ActionResult.PASS);
//        }
//    }
//}
