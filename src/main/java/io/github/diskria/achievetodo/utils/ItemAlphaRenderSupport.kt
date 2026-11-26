package io.github.diskria.achievetodo.utils

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.llamalad7.mixinextras.sugar.Local
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import io.github.diskria.achievetodo.extensions.Op
import io.github.diskria.achievetodo.extensions.invoke
import io.github.diskria.lapis.annotations.Extension
import io.github.diskria.lapis.annotations.KMixin
import io.github.diskria.lapis.annotations.Side
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.GuiRenderer
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.state.gui.BlitRenderState
import net.minecraft.client.renderer.state.gui.GuiItemRenderState
import net.minecraft.client.renderer.state.gui.GuiRenderState
import net.minecraft.util.ARGB
import org.joml.Matrix3x2fc
import org.spongepowered.asm.mixin.injection.At

@KMixin<GuiGraphicsExtractor>(Side.Client)
abstract class ItemAlphaSupportGraphics {

    @Extension
    var itemTempAlpha: Float? = null

    @WrapOperation(
        method = ["item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/state/gui/GuiRenderState;addItem(Lnet/minecraft/client/renderer/state/gui/GuiItemRenderState;)V"
        )]
    )
    fun attachItemAlpha(guiState: GuiRenderState, guiItemState: GuiItemRenderState, op: Op<Unit>) {
        guiItemState.tempAlpha = itemTempAlpha
        op(guiState, guiItemState)
    }
}

@KMixin<GuiItemRenderState>(Side.Client)
abstract class ItemAlphaSupportRenderState {
    @Extension
    var tempAlpha: Float? = null
}

@KMixin<GuiRenderer>(Side.Client)
abstract class ItemAlphaSupportRenderer {

    @WrapOperation(
        method = ["submitBlitFromItemAtlas"],
        at = [At("NEW", target = "net/minecraft/client/renderer/state/gui/BlitRenderState")]
    )
    context(
        @Local(argsOnly = true, name = ["itemState"]) guiItemState: GuiItemRenderState,
    )
    fun applyItemTempAlpha(
        pipeline: RenderPipeline,
        textureSetup: TextureSetup,
        pose: Matrix3x2fc,
        x0: Int, y0: Int,
        x1: Int, y1: Int,
        u0: Float, u1: Float,
        v0: Float, v1: Float,
        color: Int,
        scissorArea: ScreenRectangle?,
        bounds: ScreenRectangle?,
        op: Op<BlitRenderState>
    ): BlitRenderState {
        val finalColor = guiItemState.tempAlpha?.let { ARGB.colorFromFloat(it, it, it, it) } ?: color
        return op(pipeline, textureSetup, pose, x0, y0, x1, y1, u0, u1, v0, v1, finalColor, scissorArea, bounds)
    }
}
