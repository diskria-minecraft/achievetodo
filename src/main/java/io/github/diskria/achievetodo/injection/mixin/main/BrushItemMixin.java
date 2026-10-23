package io.github.diskria.achievetodo.injection.mixin.main;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.AbilityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BrushItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrushItem.class)
public class BrushItemMixin {

    @Inject(
        method = "usageTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/BrushableBlock;getBrushingSound()Lnet/minecraft/sound/SoundEvent;",
            shift = At.Shift.BEFORE
        )
    )
    public void lockBrush(
        World world,
        LivingEntity user,
        ItemStack stack,
        int remainingUseTicks,
        CallbackInfo ci,
        @Local PlayerEntity player,
        @Local @NotNull BlockPos blockPos
    ) {
        if (AchieveToDoMod.isTargetInLockedLandmark(player, world, blockPos) ||
            AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_BRUSH)
        ) {
            user.stopUsingItem();
        }
    }
}
