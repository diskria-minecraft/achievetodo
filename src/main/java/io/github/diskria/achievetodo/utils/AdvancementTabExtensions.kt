package io.github.diskria.achievetodo.utils

import io.github.diskria.lapis.annotations.*
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.client.gui.screens.advancements.AdvancementTab
import net.minecraft.client.gui.screens.advancements.AdvancementWidget
import net.minecraft.resources.Identifier
import javax.lang.model.element.Modifier.*

@KMixin<AdvancementTab>(Side.Client)
abstract class AdvancementTabExtensions(@Origin val tab: AdvancementTab) {

    @Extension
    fun resetContents() {
        widgets.clear()
        widgets[root.advancement] = root.apply { clearChildren() }
        resetHovered()
        setMin(Int.MAX_VALUE, Int.MAX_VALUE)
        setMax(Int.MIN_VALUE, Int.MIN_VALUE)
        setScroll(0.toDouble(), 0.toDouble())
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
    fun setMin(x: Int?, y: Int?) {
        x?.let { minX = it }
        y?.let { minY = it }
    }

    @Extension
    fun setMax(x: Int?, y: Int?) {
        x?.let { maxX = it }
        y?.let { maxY = it }
    }

    @Extension
    fun setScroll(x: Double?, y: Double?) {
        x?.let { scrollX = it }
        y?.let { scrollY = it }
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
