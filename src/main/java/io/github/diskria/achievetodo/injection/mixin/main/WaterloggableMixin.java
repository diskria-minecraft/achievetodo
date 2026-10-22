package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.Waterloggable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Waterloggable.class)
public interface WaterloggableMixin {

    @Inject(
        method = "tryDrainFluid",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/WorldAccess;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    default void lockDrainFluid(
        @Nullable PlayerEntity player,
        WorldAccess worldAccess,
        BlockPos pos,
        BlockState state,
        CallbackInfoReturnable<ItemStack> cir
    ) {
        if (worldAccess instanceof World world && AchieveToDoMod.isTargetInLockedLandmark(player, world, pos)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
