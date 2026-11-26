package io.github.diskria.achievetodo.qol

import net.minecraft.ChatFormatting
import net.minecraft.ChatFormatting.*
import net.minecraft.network.chat.Component

data class BacapScoreboardMode(
    val objectiveName: String,
    private val tooltipText: String,
    private val tooltipTextColor: ChatFormatting,
    val isPoints: Boolean = false,
) {
    val title get() = if (isPoints) I18N.ADVANCEMENT_POINTS else I18N.ADVANCEMENTS
    val tooltip get() = Component.translatable(tooltipText).withStyle(tooltipTextColor)

    companion object {
        val ALL = listOf(
            BacapScoreboardMode("bac_advancements", "Advancements Count", GREEN),
            BacapScoreboardMode("bac_advfirst", "First Advancements Count", YELLOW),
            BacapScoreboardMode("bac_advancements_team", "Team Advancements Count", AQUA),
            BacapScoreboardMode("bac_advfirst_team_sum", "Total Team Advancements Count", DARK_AQUA),
            BacapScoreboardMode("bac_advfirst_sum", "Team First Advancements Count", BLUE),
            BacapScoreboardMode("bac_advfirst_team", "First Advancements in Team Count", DARK_BLUE),
            BacapScoreboardMode("bac_advancements_points", "Advancement Points", DARK_GREEN, isPoints = true),
            BacapScoreboardMode("bac_advancements_team_points", "Team Advancement Points", DARK_AQUA, isPoints = true),
        ).associateBy { it.objectiveName }
    }
}
