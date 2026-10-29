package io.github.diskria.achievetodo.client.gui

import io.github.diskria.achievetodo.AchieveToDoMod
import io.github.diskria.achievetodo.client.AchieveToDoClient
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.ChatFormatting
import net.minecraft.advancements.AdvancementNode
import net.minecraft.client.gui.screens.advancements.AdvancementTabType
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

enum class AdvancementsTabType {
    ABILITIES,
    BACAP,
    STATISTICS,

    BUILDING,
    FARMING,
    HUSBANDRY,
    BIOMES,
    ADVENTURE,
    MONSTERS,
    WEAPONRY,

    MINING,
    REDSTONE,
    ENCHANTING,
    NETHER,
    POTION,
    END,
    CHALLENGES;

    val mystifiedTabId: Identifier
        get() = AchieveToDoMod.getIdentifier("locked_tab_" + this.name + "/root")

    val mystifiedTabTooltipText: Component
        get() = AchieveToDoClient.translate("advancements_tab_mystified_tooltip." + this.name)
            .withStyle(ChatFormatting.ITALIC)
            .withStyle(ChatFormatting.GRAY)

    val id: String get() = name.lowercase()

    @Environment(EnvType.CLIENT)
    fun getPosition(): AdvancementTabType = when (this) {
        ABILITIES, BACAP, STATISTICS -> AdvancementTabType.LEFT
        BUILDING, FARMING, HUSBANDRY, BIOMES, ADVENTURE, MONSTERS, WEAPONRY -> AdvancementTabType.ABOVE
        else -> AdvancementTabType.BELOW
    }

    @Environment(EnvType.CLIENT)
    fun getOrder(): Int {
        var order = 0
        val position = getPosition()
        for (tab in entries) {
            if (tab.getPosition() == position) {
                if (tab == this) {
                    break
                }
                order++
            }
        }
        return order
    }

    companion object {
        fun fromAdvancement(id: Identifier) = entries.find { it.name == id.namespace.lowercase() }
        fun fromAdvancement(advancement: AdvancementNode) = fromAdvancement(advancement.holder().id)
    }
}
