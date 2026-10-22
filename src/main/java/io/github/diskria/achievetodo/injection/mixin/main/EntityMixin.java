package io.github.diskria.achievetodo.injection.mixin.main;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.AbilityType;
import net.minecraft.block.Portal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Unique
    private boolean isEndGatewayOnCentralIsland(@NotNull BlockPos pos) {
        return pos.getY() == 75 &&
            pos.getX() >= -96 && pos.getX() <= 96 &&
            pos.getZ() >= -96 && pos.getZ() <= 96;
    }

    @Inject(
        method = "setSneaking",
        at = @At("HEAD"),
        cancellable = true
    )
    public void lockSneaking(boolean isSneaking, CallbackInfo ci) {
        if (isSneaking) {
            Entity entity = (Entity) (Object) this;
            if (entity instanceof PlayerEntity player &&
                player.isOnGround() &&
                AchieveToDoMod.isAbilityLocked(player, AbilityType.SNEAK)
            ) {
                ci.cancel();
            }
        }
    }

    @Inject(
        method = "interact",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Leashable;canLeashAttachTo()Z"
        ),
        cancellable = true
    )
    public void lockLeashAttach(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        Entity entity = (Entity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, entity)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

    @Inject(
        method = "interact",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Leashable;detachLeash()V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockLeashDetach(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        Entity entity = (Entity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, entity)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

    @ModifyReturnValue(
        method = "canAddPassenger",
        at = @At(value = "TAIL")
    )
    public boolean lockMinecart(
        boolean original,
        @Local(argsOnly = true, ordinal = 1) Entity passenger
    ) {
        if (!original) {
            return false;
        }
        Entity entity = (Entity) (Object) this;
        if (passenger instanceof PlayerEntity player && entity instanceof MinecartEntity) {
            return !AchieveToDoMod.isTargetInLockedLandmark(player, entity) &&
                !AchieveToDoMod.isAbilityLocked(player, AbilityType.GET_INTO_MINECART);
        }
        return true;
    }

    @Inject(
        method = "tryUsePortal",
        at = @At("HEAD"),
        cancellable = true
    )
    private void lockPortal(Portal portal, BlockPos pos, CallbackInfo ci) {
        Entity teleportEntity = (Entity) (Object) this;
        EnderPearlEntity enderPearl = null;
        if (teleportEntity instanceof EnderPearlEntity enderPearlEntity) {
            teleportEntity = enderPearlEntity.getOwner();
            enderPearl = enderPearlEntity;
        }
        if (teleportEntity == null) {
            return;
        }
        World currentWorld = enderPearl != null ? enderPearl.getWorld() : teleportEntity.getWorld();
        RegistryKey<World> currentWorldRegistryKey = currentWorld.getRegistryKey();
        AbilityType abilityType = AbilityType.findPortalTeleportAbility(portal);
        if (currentWorldRegistryKey == World.NETHER && abilityType == AbilityType.ENTER_NETHER) {
            return;
        }
        if (currentWorldRegistryKey == World.END) {
            if (abilityType == AbilityType.ENTER_END) {
                return;
            }
            if (abilityType == AbilityType.TELEPORT_OUTER_ISLANDS && !isEndGatewayOnCentralIsland(pos)) {
                return;
            }
        }

        if (teleportEntity instanceof PlayerEntity playerEntity) {
            if (AchieveToDoMod.isTargetInLockedLandmark(playerEntity, currentWorld, pos) ||
                AchieveToDoMod.isAbilityLocked(playerEntity, abilityType)
            ) {
                if (enderPearl != null) {
                    enderPearl.remove(Entity.RemovalReason.DISCARDED);
                }
                ci.cancel();
                return;
            }
        }
        if (enderPearl != null || !teleportEntity.hasPassengers()) {
            return;
        }
        if (teleportEntity.getControllingPassenger() instanceof PlayerEntity controllingPlayer &&
            AchieveToDoMod.isAbilityLocked(controllingPlayer, abilityType)
        ) {
            ci.cancel();
            return;
        }
        for (Entity passengerEntity : teleportEntity.getPassengerList()) {
            if (passengerEntity instanceof PlayerEntity passenger &&
                AchieveToDoMod.isAbilityLocked(passenger, abilityType)
            ) {
                passenger.stopRiding();
            }
        }
    }
}
