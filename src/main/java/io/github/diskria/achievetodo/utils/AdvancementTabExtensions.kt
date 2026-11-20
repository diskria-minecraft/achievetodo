package io.github.diskria.achievetodo.utils

import io.github.diskria.achievetodo.qol.searchResultsOriginX
import io.github.diskria.lapis.annotations.*
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.client.gui.screens.advancements.AdvancementTab
import net.minecraft.client.gui.screens.advancements.AdvancementWidget
import net.minecraft.resources.Identifier
import javax.lang.model.element.Modifier.*

@KMixin<AdvancementTab>(Side.Client)
abstract class AdvancementTabExtensions(@Origin val tab: AdvancementTab) {

    @Extension
    fun resetSearchResults() {
        widgets.clear()
        widgets[root.advancement] = root.apply { clearChildren() }
        resetHovered()
        minX = Int.MAX_VALUE
        minY = Int.MAX_VALUE
        maxX = Int.MIN_VALUE
        maxY = Int.MIN_VALUE
        scrollX = tab.screen.searchResultsOriginX.toDouble()
        scrollY = 0.toDouble()
        invalidateCentered()
    }

    @Extension
    fun resetHovered() {
        hovered = null
        fade = 0.0f
    }

    @Extension
    fun markCentered() {
        centered = true
    }

    @Extension
    fun invalidateCentered() {
        centered = false
    }

    @Extension
    fun scrollTo(x: Double, y: Double) {
        val dx = -(scrollX + x)
        val dy = -(scrollY + y)
        scrollBy(dx, dy)
    }

    @Extension
    fun getWidget(advancementId: Identifier): AdvancementWidget? =
        widgets.entries.firstOrNull { (advancement, _) -> advancement.id == advancementId }?.value

    @KShadow(PRIVATE, FINAL)
    abstract val root: AdvancementWidget

    @KShadow(PRIVATE, FINAL)
    abstract val widgets: MutableMap<AdvancementHolder, AdvancementWidget>

    @KShadow(PRIVATE)
    abstract var scrollX: Double

    @KShadow(PRIVATE)
    abstract var scrollY: Double

    @KShadow(PRIVATE)
    abstract var minX: Int

    @KShadow(PRIVATE)
    abstract var minY: Int

    @KShadow(PRIVATE)
    abstract var maxX: Int

    @KShadow(PRIVATE)
    abstract var maxY: Int

    @KShadow(PRIVATE)
    abstract var fade: Float

    @KShadow(PRIVATE)
    abstract var centered: Boolean

    @KShadow(PRIVATE)
    abstract var hovered: AdvancementWidget?

    @MappingName("scroll")
    @KShadow(PUBLIC)
    abstract fun scrollBy(dx: Double, dy: Double)
}
