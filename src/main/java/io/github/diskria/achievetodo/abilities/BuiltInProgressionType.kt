package io.github.diskria.achievetodo.abilities

import io.github.diskria.achievetodo.client.AchieveToDoClient
import net.minecraft.network.chat.Component

enum class BuiltInProgressionType {

    EASY,
    NORMAL,
    HARD,
    CHAOS;

    val id: String get() = name.lowercase()

    val displayName: Component
        get() = when (this) {
            CHAOS -> AchieveToDoClient.translate("world_creation_tab.progression.chaos")
            else -> Component.translatable("options.difficulty.$id")
        }

    val tooltipText: Component
        get() = AchieveToDoClient.translate("world_creation_tab.progression.$id.tooltip")

    companion object {
        val DEFAULT = NORMAL
        fun fromId(id: String): BuiltInProgressionType? = entries.find { it.id == id }
    }
}
