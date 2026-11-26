package io.github.diskria.achievetodo.qol

import net.minecraft.advancements.AdvancementType
import net.minecraft.client.gui.screens.advancements.AdvancementTabType
import net.minecraft.client.gui.screens.advancements.AdvancementWidget
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen

object AdvancementsGuiConstants {

    object Window {
        const val WIDTH = AdvancementsScreen.WINDOW_WIDTH
        const val HEIGHT = AdvancementsScreen.WINDOW_HEIGHT
        const val BEVEL_SIZE = 3
        const val HEADER_HEIGHT = Tree.TOP_MARGIN - BEVEL_SIZE - Tree.BORDER_SIZE
    }

    object Tree {
        const val WIDTH = AdvancementsScreen.WINDOW_INSIDE_WIDTH
        const val HEIGHT = AdvancementsScreen.WINDOW_INSIDE_HEIGHT
        const val BORDER_SIZE = 1
        const val HORIZONTAL_MARGIN = 9
        const val BOTTOM_MARGIN = 9
        const val TOP_MARGIN = 18
        const val SHADOW_VIGNETTE_SIZE = 6
        const val PADDING = 2
    }

    object Tab {
        const val OVERLAP = 4
        val HORIZONTAL_PROTRUSION = AdvancementTabType.LEFT.width - OVERLAP
        val VERTICAL_PROTRUSION = AdvancementTabType.ABOVE.height - OVERLAP
    }

    object Widget {
        const val FRAME_SPRITE_SIZE = 26
        const val FRAME_ITEM_MAX_SIZE = 28
        const val FRAME_X_OFFSET = 3
    }
}

val AdvancementWidget.frameSpritePadH
    get() = when (display.type) {
        AdvancementType.TASK -> 1
        AdvancementType.GOAL -> 2
        AdvancementType.CHALLENGE -> 0
    }

val AdvancementWidget.frameSpritePadV
    get() = when (display.type) {
        AdvancementType.TASK -> 1
        else -> 0
    }
