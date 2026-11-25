package io.github.diskria.achievetodo.qol

import com.llamalad7.mixinextras.injector.ModifyExpressionValue
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.llamalad7.mixinextras.sugar.Local
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.platform.cursor.CursorType
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import io.github.diskria.achievetodo.extensions.Op
import io.github.diskria.achievetodo.extensions.invoke
import io.github.diskria.achievetodo.extensions.toEven
import io.github.diskria.achievetodo.qol.AdvancementsGuiConstants.Tab
import io.github.diskria.achievetodo.qol.AdvancementsGuiConstants.Tree
import io.github.diskria.achievetodo.qol.AdvancementsGuiConstants.Window
import io.github.diskria.achievetodo.utils.invalidateCentered
import io.github.diskria.lapis.annotations.*
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.screens.advancements.AdvancementTab
import net.minecraft.client.gui.screens.advancements.AdvancementTabType
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.texture.SpriteContents
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.resources.metadata.animation.FrameSize
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling
import net.minecraft.resources.Identifier
import net.minecraft.util.CommonColors
import net.minecraft.util.Mth
import org.spongepowered.asm.mixin.injection.At
import javax.lang.model.element.Modifier.*
import kotlin.math.roundToInt

@KMixin<AdvancementsScreen>(Side.Client)
abstract class ResizableAdvancementsScreen(@Origin private val screen: AdvancementsScreen) {

    @Extension
    val isResized: Boolean get() = windowWidth > minWindowWidth || windowHeight > minWindowHeight

    @Extension
    val windowWidth: Int
        get() = (Mth.lerp(widthRatio, minWindowWidth / 2f, maxWindowWidth / 2f).roundToInt() * 2)
            .coerceIn(minWindowWidth, maxWindowWidth)

    @Extension
    val windowHeight: Int
        get() = (Mth.lerp(heightRatio, minWindowHeight / 2f, maxWindowHeight / 2f).roundToInt() * 2)
            .coerceIn(minWindowHeight, maxWindowHeight)

    @Extension
    val windowHorizontalMargin: Int get() = (screen.width - windowWidth) / 2

    @Extension
    val windowVerticalMargin: Int get() = (screen.height - windowHeight) / 2

    @Extension
    val treeWidth: Int get() = windowWidth - Tree.HORIZONTAL_MARGIN * 2

    @Extension
    val treeHeight: Int get() = windowHeight - Tree.TOP_MARGIN - Tree.BOTTOM_MARGIN

    @Extension
    val resizeRatio: Float get() = ((widthRatio + heightRatio) / 2f).coerceIn(0f, 1f)

    private val rightPos get() = leftPos + windowWidth
    private val bottomPos get() = topPos + windowHeight
    private val minWindowWidth get() = Window.WIDTH
    private val minWindowHeight get() = Window.HEIGHT
    private val maxWindowWidth: Int
        get() {
            val hPad = Tab.HORIZONTAL_PROTRUSION + FULLSCREEN_PADDING
            val remainingWidth = screen.width - hPad * 2
            return remainingWidth.toEven().coerceAtLeast(minWindowWidth)
        }
    private val maxWindowHeight: Int
        get() {
            val vPad = Tab.VERTICAL_PROTRUSION + FULLSCREEN_PADDING
            val remainingHeight = screen.height - vPad * 2
            return remainingHeight.toEven().coerceAtLeast(minWindowHeight)
        }

    private var widthRatio = savedWidthRatio
    private var heightRatio = savedHeightRatio
    private var isDraggingResize = false
    private var dragStartX = 0.0
    private var dragStartY = 0.0
    private var dragStartWidth = 0
    private var dragStartHeight = 0
    private var sprite: TextureAtlasSprite? = null

    @WrapMethod(method = ["mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z"])
    fun startResizing(event: MouseButtonEvent, doubleClick: Boolean, op: Op<Boolean>): Boolean {
        if (isHoveringResizeCorner(event.x.toInt(), event.y.toInt())) {
            isDraggingResize = true
            dragStartX = event.x
            dragStartY = event.y
            dragStartWidth = windowWidth
            dragStartHeight = windowHeight
            return true
        }
        return op(event, doubleClick)
    }

    @WrapMethod(method = ["mouseDragged(Lnet/minecraft/client/input/MouseButtonEvent;DD)Z"])
    fun updateResizing(event: MouseButtonEvent, dx: Double, dy: Double, op: Op<Boolean>): Boolean {
        if (isDraggingResize) {
            val widthRange = maxWindowWidth - minWindowWidth
            if (widthRange > 0) {
                val delta = event.x - dragStartX
                val targetWidth = (delta + dragStartWidth + delta).roundToInt().toFloat()
                widthRatio = ((targetWidth - minWindowWidth) / widthRange).coerceIn(0f, 1f)
            }
            val heightRange = maxWindowHeight - minWindowHeight
            if (heightRange > 0) {
                val delta = event.y - dragStartY
                val targetHeight = (delta + dragStartHeight + delta).roundToInt().toFloat()
                heightRatio = ((targetHeight - minWindowHeight) / heightRange).coerceIn(0f, 1f)
            }
            repositionElements()
            return true
        }
        return op(event, dx, dy)
    }

    @WrapMethod(method = ["mouseReleased(Lnet/minecraft/client/input/MouseButtonEvent;)Z"])
    fun stopResizing(event: MouseButtonEvent, op: Op<Boolean>): Boolean {
        if (isDraggingResize) {
            isDraggingResize = false
            return true
        }
        return op(event)
    }

    @WrapMethod(method = ["repositionElements()V"])
    fun updateLayout(op: Op<Unit>) {
        op()
        val topTabs = if (tabs.values.any { it.type == AdvancementTabType.ABOVE }) Tab.VERTICAL_PROTRUSION else 0
        val bottomTabs = if (tabs.values.any { it.type == AdvancementTabType.BELOW }) Tab.VERTICAL_PROTRUSION else 0
        val leftTabs = if (tabs.values.any { it.type == AdvancementTabType.LEFT }) Tab.HORIZONTAL_PROTRUSION else 0
        val rightTabs = if (tabs.values.any { it.type == AdvancementTabType.RIGHT }) Tab.HORIZONTAL_PROTRUSION else 0
        val clearance = 5
        val topEdge = topPos - topTabs - clearance
        val bottomEdge = bottomPos + bottomTabs + clearance
        val leftEdge = leftPos - leftTabs - clearance
        val rightEdge = rightPos + rightTabs + clearance
        val windowBounds = ScreenRectangle(leftEdge, topEdge, rightEdge - leftEdge, bottomEdge - topEdge)
        layout.visitWidgets { it.visible = !it.rectangle.intersects(windowBounds) }
        tabs.values.forEach { it.invalidateCentered() }
    }

    @WrapMethod(method = ["removed()V"])
    fun saveSizeOnClose(op: Op<Unit>) {
        savedWidthRatio = widthRatio
        savedHeightRatio = heightRatio
        op()
    }

    @WrapOperation(
        method = ["extractWindow(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V",
        )]
    )
    context(
        @Local(argsOnly = true, name = ["mouseX"]) mouseX: Int,
        @Local(argsOnly = true, name = ["mouseY"]) mouseY: Int,
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
        ).also { sprite = it }
        graphics.blitNineSlicedSprite(
            renderPipeline, sprite, NINE_SLICE,
            x, y,
            windowWidth, windowHeight,
            CommonColors.WHITE
        )
        if (isHoveringResizeCorner(mouseX, mouseY) || isDraggingResize) {
            graphics.requestCursor(RESIZE_NWSE)
        }
    }

    private fun isHoveringResizeCorner(mouseX: Int, mouseY: Int): Boolean =
        mouseX in (rightPos - Tree.HORIZONTAL_MARGIN)..rightPos && mouseY in (bottomPos - Tree.BOTTOM_MARGIN)..bottomPos

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
    fun modifyTreeWidth(original: Int) = treeWidth

    @ModifyExpressionValue(
        method = ["extractInside(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"],
        at = [At(value = "CONSTANT", args = ["intValue=${Tree.HEIGHT}"])]
    )
    fun modifyTreeHeight(original: Int) = treeHeight

    @ModifyExpressionValue(
        method = ["extractInside(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"],
        at = [At(value = "CONSTANT", args = ["intValue=${Tree.WIDTH / 2}"])]
    )
    fun modifyTreeCenterX(original: Int) = treeWidth / 2

    @ModifyExpressionValue(
        method = ["extractInside(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"],
        at = [At(value = "CONSTANT", args = ["intValue=${Tree.HEIGHT / 2}"])]
    )
    fun modifyTreeCenterY(original: Int) = treeHeight / 2

    @KShadow(PRIVATE)
    abstract var leftPos: Int

    @KShadow(PRIVATE)
    abstract var topPos: Int

    @KShadow(PRIVATE, FINAL)
    abstract val layout: HeaderAndFooterLayout

    @KShadow(PRIVATE, FINAL)
    abstract val tabs: Map<AdvancementHolder, AdvancementTab>

    @KShadow(PROTECTED)
    abstract fun repositionElements()

    companion object {
        private const val FULLSCREEN_PADDING = 4

        private val RESIZE_NWSE = CursorType.createStandardCursor(5, "resize_nwse", CursorType.DEFAULT)
        private val NINE_SLICE = GuiSpriteScaling.NineSlice(
            Window.WIDTH, Window.HEIGHT,
            GuiSpriteScaling.NineSlice.Border(
                Tree.HORIZONTAL_MARGIN + Tree.SHADOW_VIGNETTE_SIZE,
                Tree.TOP_MARGIN + Tree.SHADOW_VIGNETTE_SIZE,
                Tree.HORIZONTAL_MARGIN + Tree.SHADOW_VIGNETTE_SIZE,
                Tree.BOTTOM_MARGIN + Tree.SHADOW_VIGNETTE_SIZE,
            ),
            true,
        )

        private var savedWidthRatio = 1f
        private var savedHeightRatio = 1f
    }
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

    @WrapOperation(
        method = ["tick(II)V"],
        at = [At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(FFF)F", ordinal = 0)]
    )
    fun smoothFadeIn(value: Float, min: Float, max: Float, op: Op<Float>): Float {
        val distance = max - fade
        val smoothStep = (distance * 0.25f).coerceAtLeast(0.01f)
        val finalValue = Mth.lerp(advancementsScreen.resizeRatio, value, fade + smoothStep)
        return op(finalValue, min, max)
    }

    @WrapOperation(
        method = ["tick(II)V"],
        at = [At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(FFF)F", ordinal = 1)]
    )
    fun smoothFadeOut(value: Float, min: Float, max: Float, op: Op<Float>): Float {
        val distance = fade - min
        val smoothStep = (distance * 0.1f).coerceAtLeast(0.002f)
        val finalValue = Mth.lerp(advancementsScreen.resizeRatio, value, fade - smoothStep)
        return op(finalValue, min, max)
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
    fun modifyRightX(original: Int) = advancementsScreen?.let { it.windowWidth - Tab.OVERLAP } ?: original

    @ModifyExpressionValue(
        method = ["getY(I)I"],
        at = [At(value = "CONSTANT", args = ["intValue=${Window.HEIGHT - Tab.OVERLAP}"])]
    )
    fun modifyBelowY(original: Int) = advancementsScreen?.let { it.windowHeight - Tab.OVERLAP } ?: original
}
