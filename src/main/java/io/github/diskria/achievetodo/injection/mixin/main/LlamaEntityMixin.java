package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LlamaEntity.class)
public class LlamaEntityMixin {

    @Inject(
        method = "receiveFood",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/LlamaEntity;lovePlayer(Lnet/minecraft/entity/player/PlayerEntity;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract1(PlayerEntity player, ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        LlamaEntity llamaEntity = (LlamaEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, llamaEntity)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "receiveFood",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/LlamaEntity;heal(F)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract2(PlayerEntity player, ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        LlamaEntity llamaEntity = (LlamaEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, llamaEntity)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "receiveFood",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract3(PlayerEntity player, ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        LlamaEntity llamaEntity = (LlamaEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, llamaEntity)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "receiveFood",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/World;isClient:Z",
            shift = At.Shift.BEFORE,
            ordinal = 1
        ),
        cancellable = true
    )
    public void lockInteract4(PlayerEntity player, ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        LlamaEntity llamaEntity = (LlamaEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, llamaEntity)) {
            cir.setReturnValue(false);
        }
    }
}
