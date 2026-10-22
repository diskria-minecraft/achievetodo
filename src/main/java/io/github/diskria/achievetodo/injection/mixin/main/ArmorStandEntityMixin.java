package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStandEntity.class)
public class ArmorStandEntityMixin {

    @Inject(
        method = "equip",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/decoration/ArmorStandEntity;equipStack(Lnet/minecraft/entity/EquipmentSlot;Lnet/minecraft/item/ItemStack;)V"
        ),
        cancellable = true
    )
    public void lockArmorStandEquip(
        PlayerEntity player,
        EquipmentSlot slot,
        ItemStack stack,
        Hand hand,
        CallbackInfoReturnable<Boolean> cir
    ) {
        ArmorStandEntity armorStandEntity = (ArmorStandEntity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, armorStandEntity)) {
            cir.setReturnValue(false);
        }
    }
}
