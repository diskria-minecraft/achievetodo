package io.github.diskria.achievetodo.injection.mixin.main;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.AbilityType;
import net.minecraft.entity.passive.ArmadilloEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ArmadilloEntity.class)
public class ArmadilloEntityMixin {

    @WrapOperation(
        method = "interactMob",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/ArmadilloEntity;brushScute()Z"
        )
    )
    public boolean lockBrush(
        @NotNull ArmadilloEntity armadilloEntity,
        Operation<Boolean> original,
        @Local(argsOnly = true) PlayerEntity player
    ) {
        return !armadilloEntity.isBaby() &&
            !AchieveToDoMod.isTargetInLockedLandmark(player, armadilloEntity) &&
            !AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_BRUSH) &&
            original.call(armadilloEntity);
    }
}
