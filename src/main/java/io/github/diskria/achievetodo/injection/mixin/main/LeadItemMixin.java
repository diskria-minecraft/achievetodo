package io.github.diskria.achievetodo.injection.mixin.main;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.diskria.achievetodo.AchieveToDoMod;
import net.minecraft.entity.Leashable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.LeadItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.function.Predicate;

@Mixin(LeadItem.class)
public class LeadItemMixin {

    @WrapOperation(
        method = "attachHeldMobsToBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/LeadItem;collectLeashablesAround(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Ljava/util/function/Predicate;)Ljava/util/List;"
        )
    )
    private static @NotNull List<Leashable> lockAttachHeldMobs(
        World world,
        BlockPos pos,
        Predicate<Leashable> predicate,
        @NotNull Operation<List<Leashable>> original,
        @Local(argsOnly = true) PlayerEntity player
    ) {
        List<Leashable> result = original.call(world, pos, predicate);
        if (!result.isEmpty() && AchieveToDoMod.isTargetInLockedLandmark(player, world, pos)) {
            result.clear();
        }
        return result;
    }
}
