package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import net.minecraft.entity.Bucketable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Bucketable.class)
public interface BucketableMixin {

    @Inject(
        method = "tryBucket",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/LivingEntity;playSound(Lnet/minecraft/sound/SoundEvent;FF)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private static <T extends LivingEntity & Bucketable> void lockInteract1(
        PlayerEntity player,
        Hand hand,
        T entity,
        CallbackInfoReturnable<Optional<ActionResult>> cir
    ) {
        if (AchieveToDoMod.isTargetInLockedLandmark(player, entity)) {
            cir.setReturnValue(Optional.of(ActionResult.FAIL));
        }
    }
}
