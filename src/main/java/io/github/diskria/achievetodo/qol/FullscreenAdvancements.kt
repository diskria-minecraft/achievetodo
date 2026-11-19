package io.github.diskria.achievetodo.qol

import io.github.diskria.achievetodo.qol.AdvancementsGuiConstants.Window
import io.github.diskria.lapis.annotations.*
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen
import javax.lang.model.element.Modifier.PRIVATE
import kotlin.math.abs

@KMixin<AdvancementsScreen>(Side.Client)
abstract class FullscreenAdvancementsScreen(@Origin val screen: AdvancementsScreen) {

    @Extension
    val treeWidth: Int get() = abs(leftPos * 2 - screen.width) - Window.TREE_HORIZONTAL_MARGIN * 2

    @Extension
    val treeHeight: Int get() = abs(topPos * 2 - screen.height) - Window.TREE_TOP_MARGIN - Window.TREE_BOTTOM_MARGIN

    @KShadow(PRIVATE)
    abstract val leftPos: Int

    @KShadow(PRIVATE)
    abstract val topPos: Int
}
