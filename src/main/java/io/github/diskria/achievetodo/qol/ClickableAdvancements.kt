package io.github.diskria.achievetodo.qol

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.platform.cursor.CursorTypes
import io.github.diskria.achievetodo.extensions.Op
import io.github.diskria.achievetodo.extensions.invoke
import io.github.diskria.lapis.annotations.*
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.advancements.AdvancementTab
import net.minecraft.client.gui.screens.advancements.AdvancementWidget
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen
import net.minecraft.client.input.MouseButtonEvent
import javax.lang.model.element.Modifier.PRIVATE

typealias AdvancementClickListener = () -> Unit

@KMixin<AdvancementsScreen>(Side.Client)
abstract class ClickableAdvancementsScreen(@Origin val screen: AdvancementsScreen) {

    private var hoveredInTab: AdvancementTab? = null
    private var hoveredWidget: AdvancementWidget? = null
    private var clickedWidget: AdvancementWidget? = null
    private var onClickListener: AdvancementClickListener? = null
    private var clickStartX = 0.0
    private var clickStartY = 0.0

    @Extension
    fun onWidgetHovered(tab: AdvancementTab, widget: AdvancementWidget) {
        if (hoveredInTab === tab && hoveredWidget === widget) return
        val listener = screen.resolveAdvancementClickListener(tab, widget)
        if (listener != null) {
            hoveredInTab = tab
            hoveredWidget = widget
            onClickListener = listener
        } else {
            resetWidgetClick()
        }
    }

    @Extension
    fun resetWidgetClick() {
        hoveredInTab = null
        hoveredWidget = null
        clickedWidget = null
        onClickListener = null
    }

    @WrapMethod(method = ["extractRenderState"])
    fun markHoveredAsClickable(
        graphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        deltaTicks: Float,
        op: Op<Unit>
    ) {
        op(graphics, mouseX, mouseY, deltaTicks)
        if (hoveredWidget != null && !isScrolling) {
            graphics.requestCursor(CursorTypes.POINTING_HAND)
        }
    }

    @WrapMethod(method = ["mouseClicked"])
    fun switchHoveredToClicked(event: MouseButtonEvent, doubleClick: Boolean, op: Op<Boolean>): Boolean {
        if (hoveredWidget != null && event.isClickButton) {
            clickedWidget = hoveredWidget
            clickStartX = event.x()
            clickStartY = event.y()
        }
        return op(event, doubleClick)
    }

    @WrapMethod(method = ["mouseScrolled"])
    fun cancelClickOnScroll(mouseX: Double, mouseY: Double, dx: Double, dy: Double, op: Op<Boolean>): Boolean {
        clickedWidget = null
        return op(mouseX, mouseY, dx, dy)
    }

    @WrapMethod(method = ["mouseDragged"])
    fun cancelClickOnDrag(event: MouseButtonEvent, dx: Double, dy: Double, op: Op<Boolean>): Boolean {
        if (clickedWidget != null) {
            val distX = event.x - clickStartX
            val distY = event.y - clickStartY
            if (distX * distX + distY * distY > 16.0) {
                clickedWidget = null
            }
        }
        return op(event, dx, dy)
    }

    @WrapMethod(method = ["mouseReleased"])
    fun finalizeClick(event: MouseButtonEvent, op: Op<Boolean>): Boolean {
        if (event.isClickButton) {
            val hovered = hoveredWidget
            val clicked = clickedWidget
            val listener = onClickListener
            if (hovered != null && clicked != null && listener != null) {
                if (clicked.advancement.id == hovered.advancement.id) {
                    listener()
                }
            }
            resetWidgetClick()
        }
        return op(event)
    }

    @WrapMethod(method = ["removed"])
    fun resetOnClose(op: Op<Unit>) {
        resetWidgetClick()
        op()
    }

    @KShadow(PRIVATE)
    abstract val isScrolling: Boolean
}

@KMixin<AdvancementTab>(Side.Client)
abstract class ClickableAdvancementTab {

    @WrapMethod(method = ["tick"])
    fun AdvancementTab.updateHovered(relativeMouseX: Int, relativeMouseY: Int, op: Op<Unit>) {
        op(relativeMouseX, relativeMouseY)
        hovered?.let { screen.onWidgetHovered(this, it) } ?: screen.resetWidgetClick()
    }

    @KShadow(PRIVATE)
    abstract val hovered: AdvancementWidget?
}

private val MouseButtonEvent.isClickButton get() = button() == InputConstants.MOUSE_BUTTON_LEFT
