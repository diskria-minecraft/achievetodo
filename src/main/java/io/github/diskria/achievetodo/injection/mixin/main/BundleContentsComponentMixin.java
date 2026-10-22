package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.AbilityType;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BundleContentsComponent.Builder.class)
public class BundleContentsComponentMixin {

    @Inject(
        method = "add(Lnet/minecraft/screen/slot/Slot;Lnet/minecraft/entity/player/PlayerEntity;)I",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    public void lockBundle(
        Slot slot,
        PlayerEntity player,
        CallbackInfoReturnable<Integer> cir
    ) {
        if (AchieveToDoMod.isAbilityLocked(player, AbilityType.PUT_IN_BUNDLE)) {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.closeHandledScreen();
            }
            cir.setReturnValue(0);
        }
    }
}
