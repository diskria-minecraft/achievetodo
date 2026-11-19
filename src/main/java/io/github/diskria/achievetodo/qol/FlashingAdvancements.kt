package io.github.diskria.achievetodo.qol

import com.llamalad7.mixinextras.injector.ModifyExpressionValue
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.llamalad7.mixinextras.sugar.Local
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import io.github.diskria.achievetodo.extensions.Op
import io.github.diskria.achievetodo.extensions.invoke
import io.github.diskria.achievetodo.extensions.isSearchResultsRoot
import io.github.diskria.achievetodo.qol.AdvancementsGuiConstants.Widget
import io.github.diskria.achievetodo.utils.scrollTo
import io.github.diskria.lapis.annotations.Extension
import io.github.diskria.lapis.annotations.KMixin
import io.github.diskria.lapis.annotations.KShadow
import io.github.diskria.lapis.annotations.Side
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.advancements.AdvancementTab
import net.minecraft.client.gui.screens.advancements.AdvancementWidget
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen
import net.minecraft.client.multiplayer.ClientAdvancements
import net.minecraft.resources.Identifier
import org.spongepowered.asm.mixin.injection.At
import javax.lang.model.element.Modifier.PRIVATE

@KMixin<AdvancementsScreen>(Side.Client)
abstract class FlashingAdvancementsScreen {

    @Extension
    var flashingWidget: AdvancementWidget? = null
        private set

    private var flashingTicks = 0
    private var centeredToFlashing = false

    @Extension
    fun startFlashing(widget: AdvancementWidget) {
        if (flashingWidget != null) return
        flashingTicks = FLASHING_BLINKS_COUNT * 2 * FLASHING_POSE_TICKS
        flashingWidget = widget
    }

    @Extension
    fun stopFlashing() {
        flashingTicks = 0
        centeredToFlashing = false
        flashingWidget?.isFrameBlink = false
        flashingWidget = null
    }

    @WrapMethod(method = ["tick"])
    fun countDownFlashingTicks(op: Op<Unit>) {
        op()
        if (flashingTicks <= 0) return
        flashingTicks--
        if (flashingTicks > 0) {
            flashingWidget?.isFrameBlink = (flashingTicks / FLASHING_POSE_TICKS) % 2 == 0
        } else {
            stopFlashing()
        }
    }

    @WrapMethod(method = ["extractInside"])
    fun AdvancementsScreen.centerToFlashingWidget(graphics: GuiGraphicsExtractor, op: Op<Unit>) {
        op(graphics)
        if (centeredToFlashing) return
        val targetWidget = flashingWidget ?: return
        val selectedTab = selectedTab ?: return
        val centerX = (Widget.FRAME_SPRITE_SIZE - treeWidth) / 2
        val centerY = (Widget.FRAME_SPRITE_SIZE - treeHeight) / 2
        selectedTab.scrollTo(
            x = targetWidget.x.toDouble() + Widget.FRAME_X_OFFSET + centerX,
            y = targetWidget.y.toDouble() + centerY
        )
        centeredToFlashing = true
    }

    @WrapOperation(
        method = ["mouseClicked"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientAdvancements;setSelectedTab(Lnet/minecraft/advancements/AdvancementHolder;Z)V"
        )]
    )
    fun stopFlashingOnTabSwitch(
        clientAdvancements: ClientAdvancements,
        selectedTab: AdvancementHolder,
        tellServer: Boolean,
        op: Op<Unit>,
    ) {
        stopFlashing()
        op(clientAdvancements, selectedTab, tellServer)
    }

    @KShadow(PRIVATE)
    abstract val selectedTab: AdvancementTab?
}

@KMixin<AdvancementTab>(Side.Client)
abstract class FlashingAdvancementTab {
    @ModifyExpressionValue(
        method = ["tick"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementWidget;isMouseOver(IIII)Z"
        )]
    )
    context(
        @Local(name = ["widget"]) widget: AdvancementWidget,
    )
    fun AdvancementTab.waitFlashingHovered(original: Boolean): Boolean {
        if (rootAdvancement.isSearchResultsRoot) return original
        val flashingId = screen.flashingWidget?.advancement?.id ?: return original
        if (widget.advancement.id != flashingId) return false
        if (original) screen.stopFlashing()
        return original
    }
}

@KMixin<AdvancementWidget>(Side.Client)
abstract class FlashingAdvancementWidget {

    @Extension
    var isFrameBlink: Boolean = false

    @WrapWithCondition(
        method = ["extractRenderState"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"
        )]
    )
    fun blinkFrame(
        graphics: GuiGraphicsExtractor,
        renderPipeline: RenderPipeline,
        texture: Identifier,
        x: Int, y: Int,
        width: Int, height: Int,
    ) = !isFrameBlink
}

private const val FLASHING_BLINKS_COUNT = 5
private const val FLASHING_POSE_TICKS = 4
