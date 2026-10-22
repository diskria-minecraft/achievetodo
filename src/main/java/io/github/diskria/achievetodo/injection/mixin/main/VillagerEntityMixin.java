package io.github.diskria.achievetodo.injection.mixin.main;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.AbilityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerData;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin {

    @Shadow
    public abstract VillagerData getVillagerData();

    @WrapOperation(
        method = "interactMob",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/village/TradeOfferList;isEmpty()Z"
        )
    )
    public boolean lockVillager(
        TradeOfferList tradeOffers,
        @NotNull Operation<Boolean> original,
        @Local(argsOnly = true) PlayerEntity player
    ) {
        if (original.call(tradeOffers)) {
            return true;
        }
        VillagerEntity villagerEntity = (VillagerEntity) (Object) this;
        return AchieveToDoMod.isTargetInLockedLandmark(player, villagerEntity) ||
            AchieveToDoMod.isAbilityLocked(player, AbilityType.findTradeAbility(getVillagerData().getProfession()));
    }
}
