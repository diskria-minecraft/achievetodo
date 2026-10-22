package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.AbilityType;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceBlock.class)
public class AbstractFurnaceBlockMixin {

    @Inject(
        method = "onUse",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/AbstractFurnaceBlock;openScreen(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/PlayerEntity;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockFurnace(
        BlockState state,
        World world,
        BlockPos pos,
        PlayerEntity player,
        BlockHitResult hit,
        CallbackInfoReturnable<ActionResult> cir
    ) {
        if (AchieveToDoMod.isTargetInLockedLandmark(player, world, pos)) {
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }
        AbstractFurnaceBlock abstractFurnaceBlock = (AbstractFurnaceBlock) (Object) this;
        AbilityType abilityType = switch (abstractFurnaceBlock) {
            case FurnaceBlock ignored -> AbilityType.OPEN_FURNACE;
            case SmokerBlock ignored -> AbilityType.OPEN_SMOKER;
            case BlastFurnaceBlock ignored -> AbilityType.OPEN_BLAST_FURNACE;
            default -> null;
        };
        if (AchieveToDoMod.isAbilityLocked(player, abilityType)) {
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }
}
