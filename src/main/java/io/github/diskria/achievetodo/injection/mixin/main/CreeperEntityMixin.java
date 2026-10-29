//package io.github.diskria.achievetodo.injection.mixin.main;
//
//import com.llamalad7.mixinextras.sugar.Local;
//import io.github.diskria.achievetodo.AchieveToDoMod;
//import io.github.diskria.achievetodo.ability.AbilityType;
//import org.jetbrains.annotations.NotNull;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//@Mixin(CreeperEntity.class)
//public class CreeperEntityMixin {
//
//    @Inject(
//        method = "interactMob",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V",
//            shift = At.Shift.BEFORE
//        ),
//        cancellable = true
//    )
//    public void lockFlintAndSteel(
//        PlayerEntity player,
//        Hand hand,
//        CallbackInfoReturnable<ActionResult> cir,
//        @Local @NotNull ItemStack stack
//    ) {
//        CreeperEntity creeperEntity = (CreeperEntity) (Object) this;
//        if (AchieveToDoMod.isTargetInLockedLandmark(player, creeperEntity) ||
//            stack.isOf(Items.FLINT_AND_STEEL) && AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_FLINT_AND_STEEL)
//        ) {
//            cir.setReturnValue(ActionResult.FAIL);
//        }
//    }
//}
