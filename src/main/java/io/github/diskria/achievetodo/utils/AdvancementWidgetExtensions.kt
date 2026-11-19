package io.github.diskria.achievetodo.utils

import io.github.diskria.lapis.annotations.Extension
import io.github.diskria.lapis.annotations.KMixin
import io.github.diskria.lapis.annotations.KShadow
import io.github.diskria.lapis.annotations.Side
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
}
