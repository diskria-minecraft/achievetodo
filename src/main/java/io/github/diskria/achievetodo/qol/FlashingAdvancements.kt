package io.github.diskria.achievetodo.qol

import com.llamalad7.mixinextras.injector.ModifyExpressionValue
import com.llamalad7.mixinextras.injector.ModifyReceiver
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.llamalad7.mixinextras.sugar.Local
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import io.github.diskria.achievetodo.extensions.Op
import io.github.diskria.achievetodo.extensions.invoke
import io.github.diskria.achievetodo.extensions.isSearchResultsRoot
import io.github.diskria.achievetodo.qol.AdvancementsGuiConstants.Widget
import io.github.diskria.achievetodo.utils.getWidget
import io.github.diskria.achievetodo.utils.scrollTo
import io.github.diskria.lapis.annotations.*
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.advancements.AdvancementType
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.advancements.AdvancementTab
import net.minecraft.client.gui.screens.advancements.AdvancementWidget
import net.minecraft.client.gui.screens.advancements.AdvancementWidgetType
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen
import net.minecraft.client.multiplayer.ClientAdvancements
import net.minecraft.resources.Identifier
import org.spongepowered.asm.mixin.injection.At
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE

@KMixin<AdvancementsScreen>(Side.Client)
abstract class FlashingAdvancementsScreen(@Origin val screen: AdvancementsScreen) {

    @Extension
    var flashingWidget: AdvancementWidget? = null
        private set

    private var flashingTicks = 0
    private var centeredToFlashing = false

    @Extension
    fun startFlashing(id: Identifier, mode: AdvancementFlashingMode): Boolean {
        stopFlashing()
        val targetTab = advancements.tree().get(id)?.root() ?: return false
        advancements.setSelectedTab(targetTab.holder(), true)
        val targetWidget = selectedTab?.getWidget(id) ?: return false
        flashingTicks = mode.totalTicks
        flashingWidget = targetWidget.apply { activeFlashingMode = mode }
        return true
    }

    @Extension
    fun stopFlashing() {
        flashingTicks = 0
        centeredToFlashing = false
        flashingWidget?.activeFlashingMode = null
        flashingWidget?.isFlashingInBlinkPose = false
        flashingWidget = null
    }

    @WrapMethod(method = ["tick"])
    fun countDownFlashingTicks(op: Op<Unit>) {
        op()
        val mode = flashingWidget?.activeFlashingMode ?: return
        if (flashingTicks <= 0) return
        flashingTicks--
        if (flashingTicks > 0) {
            flashingWidget?.isFlashingInBlinkPose = mode.isBlinkPose(flashingTicks)
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
        tab: AdvancementHolder,
        tellServer: Boolean,
        op: Op<Unit>
    ) {
        stopFlashing()
        op(clientAdvancements, tab, tellServer)
    }

    @KShadow(PRIVATE, FINAL)
    abstract val advancements: ClientAdvancements

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
    var activeFlashingMode: AdvancementFlashingMode? = null

    @Extension
    var isFlashingInBlinkPose: Boolean = false

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
    ) = !(activeFlashingMode == AdvancementFlashingMode.FRAME && isFlashingInBlinkPose)

    @ModifyReceiver(
        method = ["extractRenderState"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementWidgetType;frameSprite(Lnet/minecraft/advancements/AdvancementType;)Lnet/minecraft/resources/Identifier;"
        )]
    )
    fun blinkObtainedStatus(original: AdvancementWidgetType, advancementType: AdvancementType): AdvancementWidgetType =
        if (activeFlashingMode == AdvancementFlashingMode.OBTAINED_STATUS && isFlashingInBlinkPose) {
            AdvancementWidgetType.UNOBTAINED
        } else {
            original
        }
}

enum class AdvancementFlashingMode(
    val blinksCount: Int,
    val poseTicks: Int,
) {
    FRAME(blinksCount = 5, poseTicks = 4),
    OBTAINED_STATUS(blinksCount = 7, poseTicks = 3);

    val totalTicks get() = blinksCount * 2 * poseTicks

    fun isBlinkPose(remainingTicks: Int) = (remainingTicks / poseTicks) % 2 != 0
}
