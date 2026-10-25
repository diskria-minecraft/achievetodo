package io.github.diskria.achievetodo.etxensions

import net.minecraft.core.component.DataComponents
import net.minecraft.tags.BlockTags
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.Item
import net.minecraft.world.item.ToolMaterial
import net.minecraft.world.item.equipment.ArmorMaterial
import net.minecraft.world.item.equipment.ArmorMaterials
import kotlin.jvm.optionals.getOrNull

private val toolMaterials = listOf(
    ToolMaterial.WOOD,
    ToolMaterial.STONE,
    ToolMaterial.COPPER,
    ToolMaterial.IRON,
    ToolMaterial.DIAMOND,
    ToolMaterial.GOLD,
    ToolMaterial.NETHERITE,
)

private val armorMaterials = listOf(
    ArmorMaterials.LEATHER,
    ArmorMaterials.COPPER,
    ArmorMaterials.CHAINMAIL,
    ArmorMaterials.IRON,
    ArmorMaterials.GOLD,
    ArmorMaterials.DIAMOND,
    ArmorMaterials.NETHERITE,
)

val Item.toolMaterial: ToolMaterial?
    get() {
        val tool = components().get(DataComponents.TOOL) ?: return null
        val deniesDropsRule = tool.rules.find { it.correctForDrops.getOrNull() == false } ?: return null
        val tagKey = deniesDropsRule.blocks.unwrapKey().getOrNull() ?: return null
        return toolMaterials.firstOrNull { it.incorrectBlocksForDrops() == tagKey }
    }

val Item.isPickaxe: Boolean
    get() {
        val tool = components().get(DataComponents.TOOL) ?: return false
        val minesAndDropsRule = tool.rules.find { it.correctForDrops.getOrNull() == true } ?: return false
        val tagKey = minesAndDropsRule.blocks.unwrapKey().getOrNull() ?: return false
        return tagKey == BlockTags.MINEABLE_WITH_PICKAXE
    }

val Item.armorMaterial: ArmorMaterial?
    get() {
        val repairable = components().get(DataComponents.REPAIRABLE) ?: return null
        val tagKey = repairable.items.unwrapKey().getOrNull() ?: return null
        return armorMaterials.firstOrNull { it.repairIngredient == tagKey }
    }

val Item.isHumanoidChestplateArmor: Boolean
    get() {
        val equipmentSlot = components().get(DataComponents.EQUIPPABLE)?.slot ?: return false
        return equipmentSlot.type == EquipmentSlot.Type.HUMANOID_ARMOR && equipmentSlot == EquipmentSlot.CHEST
    }
