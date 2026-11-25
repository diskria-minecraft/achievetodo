package io.github.diskria.achievetodo.qol

import com.llamalad7.mixinextras.injector.ModifyExpressionValue
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import io.github.diskria.achievetodo.extensions.Op
import io.github.diskria.achievetodo.extensions.invoke
import io.github.diskria.achievetodo.qol.AdvancementsGuiConstants.Tab
import io.github.diskria.achievetodo.qol.AdvancementsGuiConstants.Tree
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
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen
import net.minecraft.client.renderer.texture.SpriteContents
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.resources.metadata.animation.FrameSize
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.CommonColors
import org.spongepowered.asm.mixin.injection.At
import java.util.function.Consumer
import javax.lang.model.element.Modifier.*

@KMixin<AdvancementsScreen>(Side.Client)
abstract class ResizableAdvancementsScreen(@Origin private val screen: AdvancementsScreen) {

    @Extension
    val isResized: Boolean get() = FULLSCREEN

    @Extension
    val windowWidth: Int get() = if (FULLSCREEN) screen.width - fullscreenHorizontalMargin * 2 else Window.WIDTH

    @Extension
    val windowHeight: Int get() = if (FULLSCREEN) screen.height - fullscreenVerticalMargin * 2 else Window.HEIGHT

    @Extension
    val horizontalMargin: Int get() = (screen.width - windowWidth) / 2

    @Extension
    val verticalMargin: Int get() = (screen.height - windowHeight) / 2

    @Extension
    val treeWidth: Int get() = windowWidth - Tree.HORIZONTAL_MARGIN * 2

    @Extension
    val treeHeight: Int get() = windowHeight - Tree.TOP_MARGIN - Tree.BOTTOM_MARGIN

    private val fullscreenHorizontalMargin get() = Tab.HORIZONTAL_PROTRUSION + FULLSCREEN_PADDING
    private val fullscreenVerticalMargin get() = Tab.VERTICAL_PROTRUSION + FULLSCREEN_PADDING

    private val nineSlice by lazy {
        GuiSpriteScaling.NineSlice(
            Window.WIDTH, Window.HEIGHT,
            GuiSpriteScaling.NineSlice.Border(
                Tree.HORIZONTAL_MARGIN + Tree.SHADOW_VIGNETTE_SIZE,
                Tree.TOP_MARGIN + Tree.SHADOW_VIGNETTE_SIZE,
                Tree.HORIZONTAL_MARGIN + Tree.SHADOW_VIGNETTE_SIZE,
                Tree.BOTTOM_MARGIN + Tree.SHADOW_VIGNETTE_SIZE,
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
    fun renderWindow(
        graphics: GuiGraphicsExtractor,
        renderPipeline: RenderPipeline,
        texture: Identifier,
        x: Int, y: Int,
        u: Float, v: Float,
        width: Int, height: Int,
        textureWidth: Int, textureHeight: Int,
        op: Op<Unit>
    ) {
        if (!FULLSCREEN) {
            return op(graphics, renderPipeline, texture, x, y, u, v, width, height, textureWidth, textureHeight)
        }
        val sprite = sprite ?: TextureAtlasSprite(
            texture,
            SpriteContents(
                texture,
                FrameSize(Window.WIDTH, Window.HEIGHT),
                NativeImage(textureWidth, textureHeight, false)
            ),
            textureWidth, textureHeight,
            0, 0,
            0,
        ).also { this.sprite = it }
        graphics.blitNineSlicedSprite(
            renderPipeline, sprite, nineSlice,
            x, y,
            windowWidth, windowHeight,
            CommonColors.WHITE
        )
    }

    @WrapMethod(method = ["repositionElements()V"])
    fun calculateOnReposition(op: Op<Unit>) {
        op()
        if (FULLSCREEN) {
            tabs.values.forEach { it.invalidateCentered() }
        }
    }

    @WrapWithCondition(
        method = ["init()V"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;addTitleHeader(Lnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/Font;)V",
        )]
    )
    fun hideTitleHeader(layout: HeaderAndFooterLayout, component: Component, font: Font) = !FULLSCREEN

    @WrapOperation(
        method = ["init()V"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;addToFooter(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
        )]
    )
    fun hideFooter(layout: HeaderAndFooterLayout, child: LayoutElement, op: Operation<LayoutElement>): LayoutElement? =
        if (!FULLSCREEN) op(layout, child) else null

    @WrapWithCondition(
        method = ["init()V"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;visitWidgets(Ljava/util/function/Consumer;)V",
        )]
    )
    fun hideWidgets(layout: HeaderAndFooterLayout, consumer: Consumer<AbstractWidget>) = !FULLSCREEN

    @ModifyExpressionValue(
        method = [
            "repositionElements()V",
            "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z",
        ],
        at = [At(value = "CONSTANT", args = ["intValue=${Window.WIDTH}"])]
    )
    fun modifyWindowWidth(original: Int) = windowWidth

    @ModifyExpressionValue(
        method = [
            "repositionElements()V",
            "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z",
        ],
        at = [At(value = "CONSTANT", args = ["intValue=${Window.HEIGHT}"])]
    )
    fun modifyWindowHeight(original: Int) = windowHeight

    @ModifyExpressionValue(
        method = ["extractInside(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"],
        at = [At(value = "CONSTANT", args = ["intValue=${Tree.WIDTH}"])]
    )
    fun modifyEmptyBackgroundWidth(original: Int) = treeWidth

    @ModifyExpressionValue(
        method = ["extractInside(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"],
        at = [At(value = "CONSTANT", args = ["intValue=${Tree.HEIGHT}"])]
    )
    fun modifyEmptyBackgroundHeight(original: Int) = treeHeight

    @ModifyExpressionValue(
        method = ["extractInside(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"],
        at = [At(value = "CONSTANT", args = ["intValue=${Tree.WIDTH / 2}"])]
    )
    fun modifyEmptyTextX(original: Int) = treeWidth / 2

    @ModifyExpressionValue(
        method = ["extractInside(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"],
        at = [At(value = "CONSTANT", args = ["intValue=${Tree.HEIGHT / 2}"])]
    )
    fun modifyEmptyTextY(original: Int) = treeHeight / 2

    @KShadow(PRIVATE, FINAL)
    abstract val tabs: Map<AdvancementHolder, AdvancementTab>
}

@KMixin<AdvancementTab>(Side.Client)
abstract class ResizableAdvancementTab {

    private val advancementsScreen get() = getScreen()

    @ModifyExpressionValue(
        method = [
            "tick(II)V",
            "extractContents(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V",
            "extractTooltips(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V",
            "scroll(DD)V",
            "canScrollHorizontally()Z",
        ],
        at = [At(value = "CONSTANT", args = ["intValue=${Tree.WIDTH}"])]
    )
    fun modifyTreeWidth(original: Int): Int = advancementsScreen.treeWidth

    @ModifyExpressionValue(
        method = [
            "tick(II)V",
            "extractContents(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V",
            "extractTooltips(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V",
            "scroll(DD)V",
            "canScrollVertically()Z",
        ],
        at = [At(value = "CONSTANT", args = ["intValue=${Tree.HEIGHT}"])]
    )
    fun modifyTreeHeight(original: Int): Int = advancementsScreen.treeHeight

    @ModifyExpressionValue(
        method = ["extractContents(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"],
        at = [At(value = "CONSTANT", args = ["intValue=${Tree.WIDTH / 2}"])]
    )
    fun modifyTreeCenterX(original: Int): Int = advancementsScreen.treeWidth / 2

    @ModifyExpressionValue(
        method = ["extractContents(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"],
        at = [At(value = "CONSTANT", args = ["intValue=${Tree.HEIGHT / 2}"])]
    )
    fun modifyTreeCenterY(original: Int): Int = advancementsScreen.treeHeight / 2

    @ModifyExpressionValue(
        method = ["extractContents(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"],
        at = [At(value = "CONSTANT", args = ["intValue=${AdvancementsScreen.BACKGROUND_TILE_COUNT_X + 1}"])]
    )
    fun modifyBackgroundTileColumns(original: Int): Int =
        advancementsScreen.treeWidth / AdvancementsScreen.BACKGROUND_TILE_WIDTH + 1

    @ModifyExpressionValue(
        method = ["extractContents(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"],
        at = [At(value = "CONSTANT", args = ["intValue=${AdvancementsScreen.BACKGROUND_TILE_COUNT_Y + 1}"])]
    )
    fun modifyBackgroundTileRows(original: Int): Int =
        advancementsScreen.treeHeight / AdvancementsScreen.BACKGROUND_TILE_HEIGHT + 1

    @ModifyExpressionValue(method = ["tick(II)V"], at = [At(value = "CONSTANT", args = ["floatValue=0.06"])])
    fun modifyFadeInSpeed(original: Float): Float {
        if (!FULLSCREEN) return original
        val target = 0.3f
        val distance = target - fade
        return (distance * 0.25f).coerceAtLeast(0.01f)
    }

    @ModifyExpressionValue(method = ["tick(II)V"], at = [At(value = "CONSTANT", args = ["floatValue=0.12"])])
    fun modifyFadeOutSpeed(original: Float): Float {
        if (!FULLSCREEN) return original
        val target = 0.0f
        val distance = fade - target
        return (distance * 0.1f).coerceAtLeast(0.002f)
    }

    @KShadow(PRIVATE)
    abstract val fade: Float

    @KShadow(PUBLIC)
    abstract fun getScreen(): AdvancementsScreen
}

@KMixin<AdvancementTabType>(Side.Client)
abstract class ResizableAdvancementTabType {

    private val advancementsScreen get() = Minecraft.getInstance().gui.screen() as? AdvancementsScreen

    @ModifyExpressionValue(
        method = ["getX(I)I"],
        at = [At(value = "CONSTANT", args = ["intValue=${Window.WIDTH - Tab.OVERLAP}"])]
    )
    fun modifyRightX(original: Int): Int = advancementsScreen?.let { it.windowWidth - Tab.OVERLAP } ?: original

    @ModifyExpressionValue(
        method = ["getY(I)I"],
        at = [At(value = "CONSTANT", args = ["intValue=${Window.HEIGHT - Tab.OVERLAP}"])]
    )
    fun modifyBelowY(original: Int): Int = advancementsScreen?.let { it.windowHeight - Tab.OVERLAP } ?: original
}

private const val FULLSCREEN = true
private const val FULLSCREEN_PADDING = 4
