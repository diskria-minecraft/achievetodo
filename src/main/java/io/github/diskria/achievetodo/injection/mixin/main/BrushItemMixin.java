package io.github.diskria.achievetodo.injection.mixin.main;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.AbilityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BrushItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BrushItem.class)
public class BrushItemMixin {

    @Inject(
        method = "useOnBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;setCurrentHand(Lnet/minecraft/util/Hand;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockBrush(
        @NotNull ItemUsageContext context,
        CallbackInfoReturnable<ActionResult> cir,
        @Local PlayerEntity player
    ) {
        if (AchieveToDoMod.isTargetInLockedLandmark(context) ||
            AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_BRUSH)
        ) {
            cir.setReturnValue(ActionResult.CONSUME);
        }
    }

    @Inject(
        method = "usageTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/BrushItem;getMaxUseTime(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/LivingEntity;)I",
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
        @Local @NotNull BlockHitResult blockHitResult
    ) {
        if (AchieveToDoMod.isTargetInLockedLandmark(player, world, blockHitResult.getBlockPos()) ||
            AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_BRUSH)
        ) {
            user.stopUsingItem();
        }
    }
}
