package io.github.diskria.achievetodo.injection.mixin.main;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.diskria.achievetodo.AchieveToDoMod;
import net.minecraft.item.FireChargeItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FireChargeItem.class)
public class FireChargeItemMixin {

    @WrapOperation(
        method = "useOnBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/AbstractFireBlock;canPlaceAt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Z"
        )
    )
    public boolean lockUseOnBlock(
        World world,
        BlockPos pos,
        Direction direction,
        Operation<Boolean> original,
        @Local(argsOnly = true) ItemUsageContext context
    ) {
        if (!original.call(world, pos, direction)) {
            return false;
        }
        return !AchieveToDoMod.isTargetInLockedLandmark(context.getPlayer(), world, pos);
    }
}
