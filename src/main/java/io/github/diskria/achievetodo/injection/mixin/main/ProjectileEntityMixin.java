package io.github.diskria.achievetodo.injection.mixin.main;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.diskria.achievetodo.AchieveToDoMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ProjectileEntity.class)
public abstract class ProjectileEntityMixin {

    @Shadow
    public abstract @Nullable Entity getOwner();

    @ModifyReturnValue(
        method = "canHit(Lnet/minecraft/entity/Entity;)Z",
        at = @At(value = "TAIL")
    )
    public boolean lockProjectileHit(boolean original, @Local(argsOnly = true) Entity target) {
        if (!original) {
            return false;
        }
        return !(getOwner() instanceof PlayerEntity player) ||
            target == player ||
            !AchieveToDoMod.isTargetInLockedLandmark(player, target);
    }
}
