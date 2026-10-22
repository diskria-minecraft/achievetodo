package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.BlockAttachedEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockAttachedEntity.class)
public class BlockAttachedEntityMixin {

    @Inject(
        method = "damage",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/decoration/BlockAttachedEntity;kill(Lnet/minecraft/server/world/ServerWorld;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockKillAttachedEntity(
        ServerWorld world,
        @NotNull DamageSource source,
        float amount,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (source.getAttacker() instanceof PlayerEntity player) {
            BlockAttachedEntity blockAttachedEntity = (BlockAttachedEntity) (Object) this;
            if (AchieveToDoMod.isTargetInLockedLandmark(player, blockAttachedEntity)) {
                cir.setReturnValue(false);
            }
        }
    }
}
