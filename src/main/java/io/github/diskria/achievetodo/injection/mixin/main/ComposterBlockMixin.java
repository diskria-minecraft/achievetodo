package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.AbilityType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ComposterBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ComposterBlock.class)
public class ComposterBlockMixin {

    @Inject(
        method = "emptyFullComposter",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    private static void lockExtractBoneMeal(
        Entity user, BlockState state, World world, BlockPos pos, CallbackInfoReturnable<BlockState> cir
    ) {
        if (user instanceof PlayerEntity player) {
            if (AchieveToDoMod.isTargetInLockedLandmark(player, world, pos) ||
                AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_COMPOSTER)
            ) {
                cir.setReturnValue(state);
            }
        }
    }
}
