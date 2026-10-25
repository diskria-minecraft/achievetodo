package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.injection.extension.main.ArmorItemExtension;
import net.minecraft.world.inventory.ArmorSlot;
import net.minecraft.world.item.equipment.ArmorMaterial;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorItem.class)
public class ArmorItemMixin implements ArmorItemExtension {

    @Unique
    private ArmorMaterial material;

    @Unique
    private ArmorSlot equipmentType;

    @Override
    public ArmorMaterial achievetodo$getMaterial() {
        return material;
    }

    @Override
    public ArmorSlot achievetodo$getSlot() {
        return equipmentType;
    }

    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    public void saveMaterial(ArmorMaterial material, EquipmentType type, Item.Settings settings, CallbackInfo ci) {
        this.material = material;
        this.equipmentType = type;
    }
}
