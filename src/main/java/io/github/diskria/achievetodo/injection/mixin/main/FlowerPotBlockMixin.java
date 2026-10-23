package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.FlowerPotBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlowerPotBlock.class)
public class FlowerPotBlockMixin {

    @Inject(
        method = "onUse",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;giveItemStack(Lnet/minecraft/item/ItemStack;)Z",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private void lockExtract(
        BlockState state,
        World world,
        BlockPos pos,
        PlayerEntity player,
        BlockHitResult hit,
        CallbackInfoReturnable<ActionResult> cir
    ) {
        if (AchieveToDoMod.isTargetInLockedLandmark(player, world, hit.getBlockPos())) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
