package io.github.diskria.achievetodo.ability

import io.github.diskria.achievetodo.client.AchieveToDoClient
import net.minecraft.network.chat.Component

enum class ProgressionModeType(val version: Int) {

    EASY(1),
    NORMAL(1),
    HARD(1),
    CHAOS(1);

    val id: String get() = name.lowercase()

    val displayedText: Component
        get() {
            if (this == CHAOS) {
                return AchieveToDoClient.translate("world_creation_tab.progression.chaos")
            }
            return Component.translatable("options.difficulty.$id")
        }

    val tooltipText: Component
        get() = AchieveToDoClient.translate("world_creation_tab.progression.$id.tooltip")

    companion object {
        val defaultMode: ProgressionModeType get() = NORMAL
        fun fromId(id: String): ProgressionModeType? = entries.find { it.id == id }
    }
}
