package io.github.diskria.achievetodo.qol

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.llamalad7.mixinextras.sugar.Local
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import io.github.diskria.achievetodo.extensions.Op
import io.github.diskria.achievetodo.extensions.invoke
import io.github.diskria.achievetodo.qol.AdvancementsGuiConstants.Tree
import io.github.diskria.achievetodo.qol.AdvancementsGuiConstants.Widget
import io.github.diskria.achievetodo.utils.minecraft
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
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE

@KMixin<AdvancementsScreen>(Side.Client)
abstract class AdvancementsScreenFixes {
    @WrapOperation(
        method = ["mouseScrolled(DDDD)Z"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementTab;scroll(DD)V"
        )]
    )
    fun AdvancementsScreen.horizontalScrollByShift(tab: AdvancementTab, x: Double, y: Double, op: Op<Unit>) =
        if (minecraft.hasShiftDown()) op(tab, y, 0.0) else op(tab, x, y)
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
        graphics: GuiGraphicsExtractor,
        renderPipeline: RenderPipeline,
        originalTexture: Identifier,
        x: Int, y: Int,
        width: Int, height: Int,
        op: Op<Unit>
    ) {
        val screen = advancementsScreen
        val finalTexture = if (screen?.isResized == true) {
            val isVertical = type == AdvancementTabType.ABOVE || type == AdvancementTabType.BELOW
            val tabPosition = if (isVertical) x else y
            val tabSize = if (isVertical) width else height
            val windowMargin = if (isVertical) screen.windowHorizontalMargin else screen.windowVerticalMargin
            val screenSize = if (isVertical) screen.width else screen.height
            when {
                tabPosition == windowMargin -> sprites.first
                tabPosition + tabSize == screenSize - windowMargin -> sprites.last
                else -> sprites.middle
            }
        } else originalTexture
        op(graphics, renderPipeline, finalTexture, x, y, width, height)
    }
}

@KMixin<AdvancementWidget>(Side.Client)
abstract class AdvancementWidgetFixes {

    private val advancementsScreen get() = minecraft.gui.screen() as? AdvancementsScreen

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
        originalTopMode: Boolean,
        @Local(name = ["titleTop"]) titleTop: Int,
        @Local(name = ["titleBarBottom"]) titleBarBottom: Int,
        @Local(name = ["descriptionHeight"]) descriptionHeight: Int,
    ): Boolean {
        val screen = advancementsScreen
        val finalTopMode = if (screen != null) {
            val tooltipBottom = titleBarBottom + descriptionHeight
            val tooltipTop = titleTop - descriptionHeight
            val screenBottom = screen.treeHeight + Tree.BOTTOM_MARGIN + screen.windowVerticalMargin
            val screenTop = -(screen.windowVerticalMargin + Tree.TOP_MARGIN)
            when {
                tooltipBottom < screen.treeHeight -> false
                tooltipTop >= 0 -> true
                tooltipBottom <= screenBottom -> false
                tooltipTop >= screenTop -> true
                else -> minecraft.hasAltDown()
            }
        } else originalTopMode
        return finalTopMode
    }

    @KShadow(PRIVATE, FINAL)
    abstract val minecraft: Minecraft
}
