package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrameEntity.class)
public class ItemFrameEntityMixin {

    @Inject(
        method = "interact",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/decoration/ItemFrameEntity;setHeldItemStack(Lnet/minecraft/item/ItemStack;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockPlace(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ItemFrameEntity itemFrameEntity = (ItemFrameEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, itemFrameEntity)) {
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    @Inject(
        method = "interact",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/decoration/ItemFrameEntity;playSound(Lnet/minecraft/sound/SoundEvent;FF)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockRotation(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ItemFrameEntity itemFrameEntity = (ItemFrameEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, itemFrameEntity)) {
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    @Inject(
        method = "damage",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/decoration/ItemFrameEntity;dropHeldStack(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/Entity;Z)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockDrop(
        ServerWorld world,
        @NotNull DamageSource source,
        float amount,
        CallbackInfoReturnable<Boolean> cir
    ) {
        ItemFrameEntity itemFrameEntity = (ItemFrameEntity) (Object) this;
        if (source.getAttacker() instanceof PlayerEntity player &&
            AchieveToDoMod.isTargetInLockedLandmark(player, itemFrameEntity)
        ) {
            cir.setReturnValue(false);
        }
    }
}
