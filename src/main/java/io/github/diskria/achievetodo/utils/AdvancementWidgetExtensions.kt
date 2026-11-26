package io.github.diskria.achievetodo.utils

import io.github.diskria.lapis.annotations.*
import net.minecraft.client.gui.screens.advancements.AdvancementWidget
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE

@KMixin<AdvancementWidget>(Side.Client)
abstract class AdvancementWidgetExtensions {

    @Extension
    fun clearChildren() {
        children.clear()
    }

    @KShadow(PRIVATE, FINAL)
    abstract val children: MutableList<AdvancementWidget>

    @Extension
    val parent: AdvancementWidget? get() = parentInternal

    @MappingName("parent")
    @KShadow(PRIVATE)
    abstract val parentInternal: AdvancementWidget?
}
