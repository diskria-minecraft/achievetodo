package io.github.diskria.achievetodo.injection.mixin.main;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.AbilityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BundleItem;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BundleItem.class)
public class BundleItemMixin {

    @WrapOperation(
        method = "onClicked",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/screen/slot/Slot;canTakePartial(Lnet/minecraft/entity/player/PlayerEntity;)Z",
            ordinal = 0
        )
    )
    public boolean lockBundle(
        Slot instance,
        PlayerEntity player,
        @NotNull Operation<Boolean> original
    ) {
        if (!original.call(instance, player)) {
            return false;
        }
        if (AchieveToDoMod.isAbilityLocked(player, AbilityType.PUT_IN_BUNDLE)) {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.closeHandledScreen();
            }
        } else {
            return true;
        }
        return false;
    }
}
