package io.github.diskria.achievetodo.utils

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import io.github.diskria.achievetodo.extensions.Op
import io.github.diskria.achievetodo.extensions.invoke
import io.github.diskria.lapis.annotations.KMixin
import io.github.diskria.lapis.annotations.KShadow
import io.github.diskria.lapis.annotations.Side
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.Hud
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import org.spongepowered.asm.mixin.injection.At
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE

@KMixin<Hud>(Side.Client)
abstract class ScoreboardSidebarHoverSupport {

    @WrapOperation(
        method = ["displayScoreboardSidebar"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
            ordinal = 0
        )]
    )
    fun supportTitleHover(
        graphics: GuiGraphicsExtractor,
        font: Font,
        text: Component,
        x: Int, y: Int,
        color: Int,
        dropShadow: Boolean,
        op: Op<Unit>
    ) {
        op(graphics, font, text, x, y, color, dropShadow)
        if (minecraft.gui.screen() == null || minecraft.mouseHandler.isMouseGrabbed) return
        val mouseX = minecraft.mouseHandler.getScaledXPos(minecraft.window).toInt()
        val mouseY = minecraft.mouseHandler.getScaledYPos(minecraft.window).toInt()
        val textWidth = font.width(text)
        val textHeight = font.lineHeight
        if (mouseX in x..(x + textWidth) && mouseY in y..(y + textHeight)) {
            val hoverComponent = (text.style.hoverEvent as? HoverEvent.ShowText)?.value ?: return
            graphics.setTooltipForNextFrame(font, hoverComponent, mouseX, mouseY)
        }
    }

    @KShadow(PRIVATE, FINAL)
    abstract val minecraft: Minecraft
}
