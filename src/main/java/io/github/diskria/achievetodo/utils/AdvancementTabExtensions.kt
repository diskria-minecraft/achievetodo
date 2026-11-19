package io.github.diskria.achievetodo.utils

import io.github.diskria.lapis.annotations.*
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.client.gui.screens.advancements.AdvancementTab
import net.minecraft.client.gui.screens.advancements.AdvancementWidget
import net.minecraft.resources.Identifier
import javax.lang.model.element.Modifier.*

@KMixin<AdvancementTab>(Side.Client)
abstract class AdvancementTabExtensions {

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
    abstract val widgets: Map<AdvancementHolder, AdvancementWidget>

    @KShadow(PRIVATE)
    abstract var scrollX: Double

    @KShadow(PRIVATE)
    abstract var scrollY: Double

    @MappingName("scroll")
    @KShadow(PUBLIC)
    abstract fun scrollBy(dx: Double, dy: Double)
}
