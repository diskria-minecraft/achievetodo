package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.AbilityType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.VaultBlockEntity;
import net.minecraft.block.vault.VaultConfig;
import net.minecraft.block.vault.VaultServerData;
import net.minecraft.block.vault.VaultSharedData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VaultBlockEntity.Server.class)
public class VaultBlockEntityServerMixin {

    @Inject(
        method = "tryUnlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/entity/VaultBlockEntity$Server;generateLoot(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/block/vault/VaultConfig;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;)Ljava/util/List;",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private static void lockVault(
        ServerWorld world,
        BlockPos pos,
        BlockState state,
        VaultConfig config,
        VaultServerData serverData,
        VaultSharedData sharedData,
        PlayerEntity player,
        ItemStack stack,
        CallbackInfo ci
    ) {
        if (AchieveToDoMod.isTargetInLockedLandmark(player, world, pos) ||
            AchieveToDoMod.isAbilityLocked(player, AbilityType.UNLOCK_VAULT)
        ) {
            ci.cancel();
        }
    }
}
