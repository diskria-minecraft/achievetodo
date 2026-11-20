package io.github.diskria.achievetodo.qol

import com.llamalad7.mixinextras.injector.ModifyExpressionValue
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.llamalad7.mixinextras.sugar.Local
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import io.github.diskria.achievetodo.extensions.Op
import io.github.diskria.achievetodo.extensions.invoke
import io.github.diskria.achievetodo.qol.AdvancementsGuiConstants.Window
import io.github.diskria.achievetodo.utils.invalidateCentered
import io.github.diskria.lapis.annotations.*
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout
import net.minecraft.client.gui.layouts.LayoutElement
import net.minecraft.client.gui.screens.advancements.AdvancementTab
import net.minecraft.client.gui.screens.advancements.AdvancementTabType
import net.minecraft.client.gui.screens.advancements.AdvancementTabType.Sprites
import net.minecraft.client.gui.screens.advancements.AdvancementWidget
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen
import net.minecraft.client.renderer.texture.SpriteContents
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.resources.metadata.animation.FrameSize
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.CommonColors
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.ModifyVariable
import java.util.function.Consumer
import javax.lang.model.element.Modifier.*
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

@KMixin<AdvancementsScreen>(Side.Client)
abstract class AdvancementsScreenMixin(@Origin private val screen: AdvancementsScreen) {

    @Extension
    val fullscreenHorizontalMargin: Int
        get() = horizontalTabWidth - horizontalTabOffset + SCREEN_MARGIN

    @Extension
    val fullscreenVerticalMargin: Int
        get() = verticalTabHeight - verticalTabOffset + SCREEN_MARGIN

    @Extension
    val fullscreenWindowWidth: Int
        get() = screen.width - fullscreenHorizontalMargin * 2

    @Extension
    val fullscreenWindowHeight: Int
        get() = screen.height - fullscreenVerticalMargin * 2

    @Extension
    val fullscreenTreeWidth: Int get() = fullscreenWindowWidth - (Window.TREE_HORIZONTAL_MARGIN * 2)

    @Extension
    val fullscreenTreeHeight: Int get() = fullscreenWindowHeight - Window.TREE_TOP_MARGIN - Window.TREE_BOTTOM_MARGIN

    @Extension
    val horizontalTabOffset: Int get() = AdvancementTabType.LEFT.getX(0) + horizontalTabWidth

    @Extension
    val verticalTabOffset: Int get() = AdvancementTabType.ABOVE.getY(0) + verticalTabHeight

    private val horizontalTabWidth: Int get() = AdvancementTabType.LEFT.width
    private val verticalTabHeight: Int get() = AdvancementTabType.ABOVE.height

    private val nineSlice: GuiSpriteScaling.NineSlice by lazy {
        val shadowDepth = 6
        GuiSpriteScaling.NineSlice(
            AdvancementsScreen.WINDOW_WIDTH, AdvancementsScreen.WINDOW_HEIGHT,
            GuiSpriteScaling.NineSlice.Border(
                Window.TREE_HORIZONTAL_MARGIN + shadowDepth,
                Window.TREE_TOP_MARGIN + shadowDepth,
                Window.TREE_HORIZONTAL_MARGIN + shadowDepth,
                Window.TREE_BOTTOM_MARGIN + shadowDepth,
            ),
            true,
        )
    }

    private var sprite: TextureAtlasSprite? = null

    @WrapOperation(
        method = ["extractWindow(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V",
        )]
    )
    fun renderFullscreenWindow(
        graphics: GuiGraphicsExtractor,
        renderPipeline: RenderPipeline,
        texture: Identifier,
        x: Int, y: Int,
        u: Float, v: Float,
        width: Int, height: Int,
        textureWidth: Int, textureHeight: Int,
        op: Op<Unit>
    ) {
        val sprite = sprite ?: TextureAtlasSprite(
            texture,
            SpriteContents(
                texture,
                FrameSize(AdvancementsScreen.WINDOW_WIDTH, AdvancementsScreen.WINDOW_HEIGHT),
                NativeImage(textureWidth, textureHeight, false)
            ),
            textureWidth, textureHeight,
            0, 0,
            0,
        ).also { sprite = it }
        graphics.blitNineSlicedSprite(
            renderPipeline, sprite, nineSlice,
            x, y,
            fullscreenWindowWidth, fullscreenWindowHeight,
            CommonColors.WHITE
        )
    }

    @WrapMethod(method = ["repositionElements()V"])
    fun calculateOnReposition(op: Op<Unit>) {
        op()
        tabs.values.forEach { it.invalidateCentered() }
    }

    @WrapOperation(
        method = ["init()V"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;addTitleHeader(Lnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/Font;)V",
        )]
    )
    fun hideTitleHeader(layout: HeaderAndFooterLayout, component: Component, font: Font, op: Op<Unit>) {
    }

    @WrapOperation(
        method = ["init()V"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;addToFooter(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
        )]
    )
    fun hideFooter(layout: HeaderAndFooterLayout, child: LayoutElement, op: Operation<LayoutElement>): LayoutElement? =
        null

    @WrapOperation(
        method = ["init()V"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;visitWidgets(Ljava/util/function/Consumer;)V",
        )]
    )
    fun hideWidgets(layout: HeaderAndFooterLayout, consumer: Consumer<AbstractWidget>, op: Op<Unit>) {
    }

    @ModifyExpressionValue(
        method = ["repositionElements()V"],
        at = [At(value = "CONSTANT", args = ["intValue=${AdvancementsScreen.WINDOW_WIDTH}"])]
    )
    fun overrideWindowX(original: Int) = fullscreenWindowWidth

    @ModifyExpressionValue(
        method = ["repositionElements()V"],
        at = [At(value = "CONSTANT", args = ["intValue=${AdvancementsScreen.WINDOW_HEIGHT}"])]
    )
    fun overrideWindowY(original: Int) = fullscreenWindowHeight

    @ModifyExpressionValue(
        method = ["mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z"],
        at = [At(value = "CONSTANT", args = ["intValue=${AdvancementsScreen.WINDOW_WIDTH}"])]
    )
    fun overrideClickableAreaX(original: Int) = fullscreenWindowWidth

    @ModifyExpressionValue(
        method = ["mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z"],
        at = [At(value = "CONSTANT", args = ["intValue=${AdvancementsScreen.WINDOW_HEIGHT}"])]
    )
    fun overrideClickableAreaY(original: Int) = fullscreenWindowHeight

    @ModifyExpressionValue(
        method = ["extractInside(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"],
        at = [At(value = "CONSTANT", args = ["intValue=${AdvancementsScreen.WINDOW_INSIDE_WIDTH}"])]
    )
    fun overrideEmptyBackgroundWidth(original: Int) = fullscreenTreeWidth

    @ModifyExpressionValue(
        method = ["extractInside(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"],
        at = [At(value = "CONSTANT", args = ["intValue=${AdvancementsScreen.WINDOW_INSIDE_HEIGHT}"])]
    )
    fun overrideEmptyBackgroundHeight(original: Int) = fullscreenTreeHeight

    @ModifyExpressionValue(
        method = ["extractInside(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"],
        at = [At(value = "CONSTANT", args = ["intValue=${AdvancementsScreen.WINDOW_INSIDE_WIDTH / 2}"])]
    )
    fun overrideEmptyLabelsX(original: Int) = fullscreenTreeWidth / 2

    @ModifyExpressionValue(
        method = ["extractInside(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"],
        at = [At(value = "CONSTANT", args = ["intValue=${AdvancementsScreen.WINDOW_INSIDE_HEIGHT / 2}"])]
    )
    fun overrideNoAdvancementsLabelY(original: Int) = fullscreenTreeHeight / 2

    @KShadow(PRIVATE, FINAL)
    abstract val tabs: Map<AdvancementHolder, AdvancementTab>

    companion object {
        private const val SCREEN_MARGIN: Int = 4
    }
}

@KMixin<AdvancementTab>(Side.Client)
abstract class FullscreenAdvancementTab {

    private val advancementsScreen: AdvancementsScreen get() = getScreen()

    @ModifyExpressionValue(
        method = ["scroll(DD)V"],
        at = [At(value = "CONSTANT", args = ["intValue=${AdvancementsScreen.WINDOW_INSIDE_WIDTH}"])]
    )
    fun overrideScrollXLimit(original: Int): Int = advancementsScreen.fullscreenTreeWidth

    @ModifyExpressionValue(
        method = ["scroll(DD)V"],
        at = [At(value = "CONSTANT", args = ["intValue=${AdvancementsScreen.WINDOW_INSIDE_HEIGHT}"])]
    )
    fun overrideScrollYLimit(original: Int): Int = advancementsScreen.fullscreenTreeHeight

    @ModifyExpressionValue(
        method = ["tick(II)V"],
        at = [At(value = "CONSTANT", args = ["intValue=${AdvancementsScreen.WINDOW_INSIDE_WIDTH}"])]
    )
    fun overrideTickXLimit(original: Int): Int = advancementsScreen.fullscreenTreeWidth

    @ModifyExpressionValue(
        method = ["tick(II)V"],
        at = [At(value = "CONSTANT", args = ["intValue=${AdvancementsScreen.WINDOW_INSIDE_HEIGHT}"])]
    )
    fun overrideTickYLimit(original: Int): Int = advancementsScreen.fullscreenTreeHeight

    @ModifyExpressionValue(method = ["tick(II)V"], at = [At(value = "CONSTANT", args = ["floatValue=0.06"])])
    fun overrideFadeInSpeed(original: Float): Float {
        val target = 0.3f
        val distance = target - fade
        return (distance * 0.25f).coerceAtLeast(0.01f)
    }

    @ModifyExpressionValue(method = ["tick(II)V"], at = [At(value = "CONSTANT", args = ["floatValue=0.12"])])
    fun overrideFadeOutSpeed(original: Float): Float {
        val target = 0.0f
        val distance = fade - target
        return (distance * 0.1f).coerceAtLeast(0.002f)
    }

    @ModifyExpressionValue(
        method = ["canScrollHorizontally()Z"],
        at = [At(value = "CONSTANT", args = ["intValue=${AdvancementsScreen.WINDOW_INSIDE_WIDTH}"])]
    )
    fun overrideScrollXLimitCheck(original: Int): Int = advancementsScreen.fullscreenTreeWidth

    @ModifyExpressionValue(
        method = ["canScrollVertically()Z"],
        at = [At(value = "CONSTANT", args = ["intValue=${AdvancementsScreen.WINDOW_INSIDE_HEIGHT}"])]
    )
    fun overrideScrollYLimitCheck(original: Int): Int = advancementsScreen.fullscreenTreeHeight

    @ModifyExpressionValue(
        method = ["extractTooltips(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"],
        at = [At(value = "CONSTANT", args = ["intValue=${AdvancementsScreen.WINDOW_INSIDE_WIDTH}"])]
    )
    fun overrideHoverOverlayWidth(original: Int): Int = advancementsScreen.fullscreenTreeWidth

    @ModifyExpressionValue(
        method = ["extractTooltips(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"],
        at = [At(value = "CONSTANT", args = ["intValue=${AdvancementsScreen.WINDOW_INSIDE_HEIGHT}"])]
    )
    fun overrideHoverOverlayHeight(original: Int): Int = advancementsScreen.fullscreenTreeHeight

    @ModifyExpressionValue(
        method = ["extractContents(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"],
        at = [At(value = "CONSTANT", args = ["intValue=${AdvancementsScreen.WINDOW_INSIDE_WIDTH}"])]
    )
    fun overrideBackgroundWidth(original: Int): Int = advancementsScreen.fullscreenTreeWidth

    @ModifyExpressionValue(
        method = ["extractContents(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"],
        at = [At(value = "CONSTANT", args = ["intValue=${AdvancementsScreen.WINDOW_INSIDE_HEIGHT}"])]
    )
    fun overrideBackgroundHeight(original: Int): Int = advancementsScreen.fullscreenTreeHeight

    @ModifyExpressionValue(
        method = ["extractContents(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"],
        at = [At(value = "CONSTANT", args = ["intValue=${AdvancementsScreen.WINDOW_INSIDE_WIDTH / 2}"])]
    )
    fun overrideBackgroundX(original: Int): Int = advancementsScreen.fullscreenTreeWidth / 2

    @ModifyExpressionValue(
        method = ["extractContents(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"],
        at = [At(value = "CONSTANT", args = ["intValue=${AdvancementsScreen.WINDOW_INSIDE_HEIGHT / 2}"])]
    )
    fun overrideBackgroundY(original: Int): Int = advancementsScreen.fullscreenTreeHeight / 2

    @ModifyExpressionValue(
        method = ["extractContents(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"],
        at = [At(value = "CONSTANT", args = ["intValue=${AdvancementsScreen.BACKGROUND_TILE_COUNT_X + 1}"])]
    )
    fun overrideBackgroundColumns(original: Int): Int = advancementsScreen.fullscreenTreeWidth / 16 + 1

    @ModifyExpressionValue(
        method = ["extractContents(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"],
        at = [At(value = "CONSTANT", args = ["intValue=${AdvancementsScreen.BACKGROUND_TILE_COUNT_Y + 1}"])]
    )
    fun overrideBackgroundRows(original: Int): Int = advancementsScreen.fullscreenTreeHeight / 16 + 1

    @KShadow(PRIVATE)
    abstract val fade: Float

    @KShadow(PUBLIC)
    abstract fun getScreen(): AdvancementsScreen
}

@KMixin<AdvancementTabType>(Side.Client)
abstract class FullscreenAdvancementTabType(@Origin private val type: AdvancementTabType) {

    private val advancementsScreen: AdvancementsScreen?
        get() = Minecraft.getInstance().gui.screen() as? AdvancementsScreen

    @ModifyExpressionValue(
        method = ["getX(I)I"],
        at = [At(value = "CONSTANT", args = ["intValue=${AdvancementsScreen.WINDOW_WIDTH - 4}"])]
    )
    fun overrideRightX(original: Int) =
        advancementsScreen?.let { it.fullscreenWindowWidth - it.horizontalTabOffset } ?: original

    @ModifyExpressionValue(
        method = ["getY(I)I"],
        at = [At(value = "CONSTANT", args = ["intValue=${AdvancementsScreen.WINDOW_HEIGHT - 4}"])]
    )
    fun overrideBelowY(original: Int) =
        advancementsScreen?.let { it.fullscreenWindowHeight - it.verticalTabOffset } ?: original

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
        renderPipeline: RenderPipeline, location: Identifier, x: Int, y: Int, width: Int, height: Int,
        op: Op<Unit>
    ) {
        val sprite = advancementsScreen?.let {
            val isVertical = type == AdvancementTabType.ABOVE || type == AdvancementTabType.BELOW
            val tabPosition = if (isVertical) x else y
            val tabSize = if (isVertical) width else height
            val screenMargin = if (isVertical) it.fullscreenHorizontalMargin else it.fullscreenVerticalMargin
            val screenSize = if (isVertical) it.width else it.height
            when {
                tabPosition == screenMargin -> sprites.first
                tabPosition + tabSize == screenSize - screenMargin -> sprites.last
                else -> sprites.middle
            }
        } ?: location
        op(instance, renderPipeline, sprite, x, y, width, height)
    }
}

@KMixin<AdvancementWidget>(Side.Client)
abstract class FullscreenAdvancementWidget {

    private val advancementsScreen: AdvancementsScreen?
        get() = Minecraft.getInstance().gui.screen() as? AdvancementsScreen

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
        val windowBottom = screen.fullscreenTreeHeight + Window.TREE_TOP_MARGIN + screen.fullscreenVerticalMargin
        val windowTop = -(Window.TREE_HORIZONTAL_MARGIN + screen.fullscreenVerticalMargin)
        return when {
            hoverBottom < screen.fullscreenTreeHeight -> false
            hoverTop >= backgroundTop -> true
            hoverBottom <= windowBottom -> false
            hoverTop >= windowTop -> true
            else -> Minecraft.getInstance().hasAltDown()
        }
    }
}
