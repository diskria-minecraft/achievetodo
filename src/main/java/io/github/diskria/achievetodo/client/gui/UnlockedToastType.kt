package io.github.diskria.achievetodo.client.gui

import io.github.diskria.achievetodo.client.AchieveToDoClient
import net.minecraft.network.chat.Component

enum class UnlockedToastType {
    ACTION,
    ITEM,
    FOOD,
    TOOL,
    WEAPON,
    EQUIPMENT,
    BLOCK,
    TRADING,
    PORTAL,
    LANDMARK;

    val toastTitle: Component
        get() = AchieveToDoClient.translate("ability_unlocked_toast.$id").append("!")

    private val id: String get() = name.lowercase()
}
