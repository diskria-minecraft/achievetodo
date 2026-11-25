package io.github.diskria.achievetodo.qol

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.llamalad7.mixinextras.sugar.Local
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import io.github.diskria.achievetodo.extensions.Op
import io.github.diskria.achievetodo.extensions.invoke
import io.github.diskria.achievetodo.qol.AdvancementsGuiConstants.Tree
import io.github.diskria.achievetodo.qol.AdvancementsGuiConstants.Widget
import io.github.diskria.lapis.annotations.KMixin
import io.github.diskria.lapis.annotations.KShadow
import io.github.diskria.lapis.annotations.Origin
import io.github.diskria.lapis.annotations.Side
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.advancements.AdvancementTab
import net.minecraft.client.gui.screens.advancements.AdvancementTabType
import net.minecraft.client.gui.screens.advancements.AdvancementTabType.Sprites
import net.minecraft.client.gui.screens.advancements.AdvancementWidget
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen
import net.minecraft.resources.Identifier
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.ModifyVariable
import javax.lang.model.element.Modifier.PRIVATE

@KMixin<AdvancementsScreen>(Side.Client)
class AdvancementsScreenFixes {
    @WrapOperation(
        method = ["mouseScrolled(DDDD)Z"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementTab;scroll(DD)V"
        )]
    )
    fun horizontalScrollByShift(tab: AdvancementTab, x: Double, y: Double, op: Op<Unit>) =
        if (Minecraft.getInstance().hasShiftDown()) op(tab, y, 0.0) else op(tab, x, y)
}

@KMixin<AdvancementTab>(Side.Client)
abstract class AdvancementTabFixes {

    @WrapOperation(
        method = ["scroll"], at = [At("INVOKE", target = "Lnet/minecraft/util/Mth;clamp(DDD)D", ordinal = 0)]
    )
    fun fixScrollXClamp(value: Double, min: Double, max: Double, op: Op<Double>) = op(value, min, -minX.toDouble())

    @WrapOperation(
        method = ["scroll"], at = [At("INVOKE", target = "Lnet/minecraft/util/Mth;clamp(DDD)D", ordinal = 1)]
    )
    fun fixScrollYClamp(value: Double, min: Double, max: Double, op: Op<Double>) = op(value, min, -minY.toDouble())

    @WrapOperation(method = ["addWidget"], at = [At("INVOKE", target = "Ljava/lang/Math;min(II)I", ordinal = 0)])
    context(
        @Local(argsOnly = true, name = ["widget"]) widget: AdvancementWidget,
    )
    fun fixVisualMinX(minX: Int, x0: Int, op: Op<Int>) =
        op(minX, widget.x + Widget.FRAME_X_OFFSET + widget.frameSpritePadH - Tree.PADDING)

    @WrapOperation(method = ["addWidget"], at = [At("INVOKE", target = "Ljava/lang/Math;max(II)I", ordinal = 0)])
    context(
        @Local(argsOnly = true, name = ["widget"]) widget: AdvancementWidget,
    )
    fun fixVisualMaxX(maxX: Int, x1: Int, op: Op<Int>) =
        op(maxX, widget.x + Widget.FRAME_X_OFFSET + Widget.FRAME_SPRITE_SIZE - widget.frameSpritePadH + Tree.PADDING)

    @WrapOperation(method = ["addWidget"], at = [At("INVOKE", target = "Ljava/lang/Math;min(II)I", ordinal = 1)])
    context(
        @Local(argsOnly = true, name = ["widget"]) widget: AdvancementWidget,
    )
    fun fixVisualMinY(minY: Int, y0: Int, op: Op<Int>) =
        op(minY, widget.y + widget.frameSpritePadV - Tree.PADDING)

    @WrapOperation(method = ["addWidget"], at = [At("INVOKE", target = "Ljava/lang/Math;max(II)I", ordinal = 1)])
    context(
        @Local(argsOnly = true, name = ["widget"]) widget: AdvancementWidget,
    )
    fun fixVisualMaxY(maxY: Int, y1: Int, op: Op<Int>) =
        op(maxY, widget.y + Widget.FRAME_SPRITE_SIZE - widget.frameSpritePadV + Tree.PADDING)

    @KShadow(PRIVATE)
    abstract val minX: Int

    @KShadow(PRIVATE)
    abstract val minY: Int
}

@KMixin<AdvancementTabType>(Side.Client)
abstract class AdvancementTabTypeFixes(@Origin private val type: AdvancementTabType) {

    private val advancementsScreen get() = Minecraft.getInstance().gui.screen() as? AdvancementsScreen

    @WrapOperation(
        method = ["extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIZI)V"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
        )]
    )
    context(
        @Local(name = ["sprites"]) sprites: Sprites,
    )
    fun fixSpriteAlignment(
        instance: GuiGraphicsExtractor,
        renderPipeline: RenderPipeline,
        location: Identifier,
        x: Int, y: Int,
        width: Int, height: Int,
        op: Op<Unit>
    ) {
        val screen = advancementsScreen
        if (screen == null || !screen.isResized) {
            return op(instance, renderPipeline, location, x, y, width, height)
        }
        val isVertical = type == AdvancementTabType.ABOVE || type == AdvancementTabType.BELOW
        val tabPosition = if (isVertical) x else y
        val tabSize = if (isVertical) width else height
        val screenMargin = if (isVertical) screen.horizontalMargin else screen.verticalMargin
        val screenSize = if (isVertical) screen.width else screen.height
        val sprite = when {
            tabPosition == screenMargin -> sprites.first
            tabPosition + tabSize == screenSize - screenMargin -> sprites.last
            else -> sprites.middle
        }
        op(instance, renderPipeline, sprite, x, y, width, height)
    }
}

@KMixin<AdvancementWidget>(Side.Client)
class AdvancementWidgetFixes {

    private val advancementsScreen get() = Minecraft.getInstance().gui.screen() as? AdvancementsScreen

    @WrapOperation(
        method = ["extractConnectivity"],
        at = [
            At("INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;horizontalLine(IIII)V", ordinal = 0),
            At("INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;horizontalLine(IIII)V", ordinal = 2),
        ]
    )
    fun fixConnectivityBorderCorner(
        graphics: GuiGraphicsExtractor,
        x0: Int, x1: Int,
        y: Int,
        color: Int,
        op: Op<Unit>
    ) = op(graphics, x0 + 1, x1, y, color)

    @ModifyVariable(
        method = ["extractHover(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIFIII)V"],
        name = ["topSide"],
        at = [At(value = "STORE")]
    )
    fun fixHoverOutOfScreen(
        topSide: Boolean,
        @Local(name = ["titleTop"]) titleTop: Int,
        @Local(name = ["titleBarBottom"]) titleBarBottom: Int,
        @Local(name = ["descriptionTextHeight"]) descriptionTextHeight: Int,
        @Local(name = ["descriptionHeight"]) descriptionHeight: Int,
    ): Boolean {
        val screen = advancementsScreen ?: return topSide
        val hoverBottom = titleBarBottom + descriptionHeight
        val hoverTop = titleTop - descriptionTextHeight + 1
        val backgroundTop = descriptionHeight - descriptionTextHeight
        val windowBottom = screen.treeHeight + Tree.TOP_MARGIN + screen.verticalMargin
        val windowTop = -(Tree.HORIZONTAL_MARGIN + screen.verticalMargin)
        return when {
            hoverBottom < screen.treeHeight -> false
            hoverTop >= backgroundTop -> true
            hoverBottom <= windowBottom -> false
            hoverTop >= windowTop -> true
            else -> Minecraft.getInstance().hasAltDown()
        }
    }
}
