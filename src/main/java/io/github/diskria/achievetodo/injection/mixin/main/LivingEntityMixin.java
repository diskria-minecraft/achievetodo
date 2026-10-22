package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.AbilityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(
        method = "canEquip",
        at = @At("HEAD"),
        cancellable = true
    )
    public void lockEquip(ItemStack stack, EquipmentSlot slot, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (livingEntity instanceof PlayerEntity player &&
            AchieveToDoMod.isAbilityLocked(player, AbilityType.findEquipmentEquipAbility(stack.getItem()))
        ) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "setSprinting",
        at = @At("HEAD"),
        cancellable = true
    )
    public void lockSprint(boolean sprinting, CallbackInfo ci) {
        if (sprinting) {
            LivingEntity livingEntity = (LivingEntity) (Object) this;
            if (livingEntity instanceof PlayerEntity player) {
                AbilityType abilityType = player.isSubmergedInWater() ? AbilityType.SWIM : AbilityType.SPRINT;
                if (AchieveToDoMod.isAbilityLocked(player, abilityType)) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(
        method = "canEquipFromDispenser",
        at = @At("HEAD"),
        cancellable = true
    )
    public void lockEquip(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (livingEntity instanceof PlayerEntity player &&
            AchieveToDoMod.isAbilityLocked(player, AbilityType.findEquipmentEquipAbility(stack.getItem()))
        ) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "jump",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/LivingEntity;getVelocity()Lnet/minecraft/util/math/Vec3d;",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockJump(CallbackInfo ci) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (livingEntity instanceof PlayerEntity player &&
            !player.isTouchingWater() &&
            AchieveToDoMod.isAbilityLocked(player, AbilityType.JUMP)
        ) {
            ci.cancel();
        }
    }
}
