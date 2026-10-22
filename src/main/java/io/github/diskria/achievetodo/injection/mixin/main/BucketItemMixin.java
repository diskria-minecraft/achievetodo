package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.AbilityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BucketItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public class BucketItemMixin {

    @Shadow
    @Final
    private Fluid fluid;

    @Inject(
        method = "placeFluid",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/FluidFillable;tryFillWithFluid(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/fluid/FluidState;)Z",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockFillFluid(
        @Nullable PlayerEntity player,
        World world,
        BlockPos pos,
        BlockHitResult hitResult,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (AchieveToDoMod.isTargetInLockedLandmark(player, world, pos) ||
            AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_WATER_BUCKET)
        ) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "placeFluid",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/World;isClient:Z",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockEmptying(
        @Nullable PlayerEntity player,
        World world,
        BlockPos pos,
        BlockHitResult hitResult,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (AchieveToDoMod.isTargetInLockedLandmark(player, world, pos) ||
            fluid == Fluids.WATER && AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_WATER_BUCKET)
        ) {
            cir.setReturnValue(false);
        }
    }
}
