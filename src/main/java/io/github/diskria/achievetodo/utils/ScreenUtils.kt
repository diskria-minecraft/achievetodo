package io.github.diskria.achievetodo.utils

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import io.github.diskria.achievetodo.extensions.Op
import io.github.diskria.achievetodo.extensions.invoke
import io.github.diskria.achievetodo.qol.initSearchFocus
import io.github.diskria.achievetodo.qol.isSearchInputCaptured
import io.github.diskria.achievetodo.qol.repositionSearch
import io.github.diskria.lapis.annotations.*
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen
import javax.lang.model.element.Modifier.PROTECTED

@KMixin<Screen>(Side.Client)
abstract class ScreenUtils {

    @WrapMethod(method = ["resize"])
    fun Screen.overrideResize(width: Int, height: Int, op: Op<Unit>) = when (this) {
        is AdvancementsScreen -> repositionSearch(width, height)
        else -> op(width, height)
    }

    @WrapMethod(method = ["isInputCaptured"])
    fun Screen.overrideIsInputCaptured(op: Op<Boolean>) = when (this) {
        is AdvancementsScreen -> op() || isSearchInputCaptured()
        else -> op()
    }

    @WrapMethod(method = ["setInitialFocus()V"])
    fun Screen.overrideSetInitialFocus(op: Op<Unit>) = when (this) {
        is AdvancementsScreen -> initSearchFocus()
        else -> op()
    }

    @MappingName("addWidget")
    @KShadow(PROTECTED)
    abstract fun <T : GuiEventListener> addWidgetInternal(widget: T): T

    @Extension
    fun <T : GuiEventListener> addWidget(widget: T): T = addWidgetInternal(widget)

    @MappingName("setInitialFocus")
    @KShadow(PROTECTED)
    abstract fun setInitialFocusInternal(target: GuiEventListener)

    @Extension
    fun setInitialFocus(target: GuiEventListener) {
        setInitialFocusInternal(target)
    }
}
