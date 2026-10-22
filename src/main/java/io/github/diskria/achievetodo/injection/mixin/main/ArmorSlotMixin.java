package io.github.diskria.achievetodo.injection.mixin.main;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.AbilityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.ArmorSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorSlot.class)
public class ArmorSlotMixin {

    @Shadow
    @Final
    private LivingEntity entity;

    @Inject(
        method = "canInsert",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    private void lockEquip(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof PlayerEntity player &&
            AchieveToDoMod.isAbilityLocked(player, AbilityType.findEquipmentEquipAbility(stack.getItem()))
        ) {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.closeHandledScreen();
            }
            cir.setReturnValue(false);
        }
    }

    @WrapOperation(
        method = "canTakeItems",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/screen/slot/Slot;canTakeItems(Lnet/minecraft/entity/player/PlayerEntity;)Z"
        )
    )
    private boolean lockEquip(
        ArmorSlot armorSlot,
        PlayerEntity player,
        @NotNull Operation<Boolean> original,
        @Local @NotNull ItemStack stack
    ) {
        if (!original.call(armorSlot, player)) {
            return false;
        }
        if (AchieveToDoMod.isAbilityLocked(player, AbilityType.findEquipmentEquipAbility(stack.getItem()))) {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.closeHandledScreen();
            }
            return false;
        }
        return true;
    }
}
