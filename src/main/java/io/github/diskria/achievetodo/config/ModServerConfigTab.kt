package io.github.diskria.achievetodo.config

import io.github.diskria.achievetodo.AchieveToDoMod
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.components.tabs.GridLayoutTab
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState
import net.minecraft.network.chat.Component

@Environment(EnvType.CLIENT)
class ModServerConfigTab(private val state: WorldCreationUiState) : GridLayoutTab(AchieveToDoMod.COLORED_NAME) {

    private var config: ModServerConfig
        get() = state.modServerConfig
        set(value) {
            state.modServerConfig = value
        }

    init {
        val row = layout.columnSpacing(10).rowSpacing(8).createRowHelper(2)
        val modToggle = row.addChild(
            CycleButton
                .onOffBuilder(config.modEnabled)
                .create(0, 0, 150, 20, Component.literal(AchieveToDoMod.NAME)) { _, value ->
                    config = config.copy(modEnabled = value)
                }
        )
        val itemRewardsToggle = row.addChild(
            CycleButton
                .onOffBuilder(config.itemRewards)
                .withTooltip { Tooltip.create(ITEM_REWARDS_INFO) }
                .create(0, 0, 150, 20, ITEM_REWARDS) { _, value ->
                    config = config.copy(itemRewards = value)
                }
        )
        state.addListener {
            modToggle.value = config.modEnabled
            itemRewardsToggle.value = config.itemRewards
        }
    }

    companion object {
        private val ITEM_REWARDS = Component.translatable("achievetodo.world_creation_tab.rewards.items")
        private val ITEM_REWARDS_INFO = Component.translatable("achievetodo.world_creation_tab.rewards.items.tooltip")
    }
}
