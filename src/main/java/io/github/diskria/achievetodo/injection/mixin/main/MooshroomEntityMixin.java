package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.AbilityType;
import net.minecraft.entity.passive.MooshroomEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MooshroomEntity.class)
public class MooshroomEntityMixin {

    @Inject(
        method = "interactMob",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/entity/passive/MooshroomEntity;stewEffects:Lnet/minecraft/component/type/SuspiciousStewEffectsComponent;",
            shift = At.Shift.BEFORE,
            ordinal = 0
        ),
        cancellable = true
    )
    public void lockInteract1(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        MooshroomEntity mooshroomEntity = (MooshroomEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, mooshroomEntity)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

    @Inject(
        method = "interactMob",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/MooshroomEntity;getWorld()Lnet/minecraft/world/World;",
            shift = At.Shift.BEFORE,
            ordinal = 0
        ),
        cancellable = true
    )
    public void lockInteract2(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        MooshroomEntity mooshroomEntity = (MooshroomEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, mooshroomEntity) ||
            AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_SHEARS)
        ) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }

    @Inject(
        method = "interactMob",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;decrementUnlessCreative(ILnet/minecraft/entity/LivingEntity;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract3(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        MooshroomEntity mooshroomEntity = (MooshroomEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, mooshroomEntity)) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }
}
