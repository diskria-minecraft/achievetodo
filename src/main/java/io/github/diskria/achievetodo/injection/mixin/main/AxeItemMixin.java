//package io.github.diskria.achievetodo.injection.mixin.main;
//
//import io.github.diskria.achievetodo.AchieveToDoMod;
//import io.github.diskria.achievetodo.ability.AbilityType;
//import org.jetbrains.annotations.Nullable;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Unique;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//import java.util.Optional;
//
//@Mixin(AxeItem.class)
//public class AxeItemMixin {
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
//        method = "tryStrip",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V",
//            shift = At.Shift.BEFORE
//        ),
//        cancellable = true
//    )
//    public void lockStrip(
//        World world,
//        BlockPos pos,
//        @Nullable PlayerEntity player,
//        BlockState state,
//        CallbackInfoReturnable<Optional<BlockState>> cir
//    ) {
//        if (AchieveToDoMod.isTargetInLockedLandmark(player, world, pos) ||
//            AchieveToDoMod.isAbilityLocked(player, AbilityType.findToolMaterialUsageAbility(material))
//        ) {
//            cir.setReturnValue(Optional.empty());
//        }
//    }
//}
