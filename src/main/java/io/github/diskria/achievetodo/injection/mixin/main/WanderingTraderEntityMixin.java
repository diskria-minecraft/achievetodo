package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.AbilityType;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WanderingTraderEntity.class)
public abstract class WanderingTraderEntityMixin {

    @Inject(
        method = "interactMob",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/WanderingTraderEntity;getOffers()Lnet/minecraft/village/TradeOfferList;",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockWanderingTrader(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        WanderingTraderEntity wanderingTraderEntity = (WanderingTraderEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, wanderingTraderEntity) ||
            AchieveToDoMod.isAbilityLocked(player, AbilityType.TRADE_WITH_WANDERING_TRADER)
        ) {
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }
}
