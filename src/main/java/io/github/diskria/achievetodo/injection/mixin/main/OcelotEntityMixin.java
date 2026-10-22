package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import net.minecraft.entity.passive.OcelotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(OcelotEntity.class)
public class OcelotEntityMixin {

    @Inject(
        method = "interactMob",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/OcelotEntity;eat(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/item/ItemStack;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract1(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        OcelotEntity ocelotEntity = (OcelotEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, ocelotEntity)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
