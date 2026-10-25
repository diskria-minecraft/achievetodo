package io.github.diskria.achievetodo.injection.extension.main;

import net.minecraft.world.inventory.ArmorSlot;
import net.minecraft.world.item.equipment.ArmorMaterial;

public interface ArmorItemExtension {
    ArmorMaterial achievetodo$getMaterial();

    ArmorSlot achievetodo$getSlot();
}
