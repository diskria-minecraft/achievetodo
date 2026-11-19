package io.github.diskria.achievetodo.qol

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.llamalad7.mixinextras.sugar.Local
import io.github.diskria.achievetodo.extensions.Op
import io.github.diskria.achievetodo.extensions.invoke
import io.github.diskria.lapis.annotations.KMixin
import io.github.diskria.lapis.annotations.KShadow
import io.github.diskria.lapis.annotations.Side
import net.minecraft.advancements.AdvancementType
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.advancements.AdvancementTab
import net.minecraft.client.gui.screens.advancements.AdvancementWidget
import org.spongepowered.asm.mixin.injection.At
import javax.lang.model.element.Modifier

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
        op(minX, widget.x + FRAME_OFFSET_X + widget.frameSpritePadH - WINDOW_EDGE_GAP)

    @WrapOperation(method = ["addWidget"], at = [At("INVOKE", target = "Ljava/lang/Math;max(II)I", ordinal = 0)])
    context(
        @Local(argsOnly = true, name = ["widget"]) widget: AdvancementWidget,
    )
    fun fixVisualMaxX(maxX: Int, x1: Int, op: Op<Int>) =
        op(maxX, widget.x + FRAME_OFFSET_X + FRAME_SPRITE_SIZE - widget.frameSpritePadH + WINDOW_EDGE_GAP)

    @WrapOperation(method = ["addWidget"], at = [At("INVOKE", target = "Ljava/lang/Math;min(II)I", ordinal = 1)])
    context(
        @Local(argsOnly = true, name = ["widget"]) widget: AdvancementWidget,
    )
    fun fixVisualMinY(minY: Int, y0: Int, op: Op<Int>) =
        op(minY, widget.y + widget.frameSpritePadV - WINDOW_EDGE_GAP)

    @WrapOperation(method = ["addWidget"], at = [At("INVOKE", target = "Ljava/lang/Math;max(II)I", ordinal = 1)])
    context(
        @Local(argsOnly = true, name = ["widget"]) widget: AdvancementWidget,
    )
    fun fixVisualMaxY(maxY: Int, y1: Int, op: Op<Int>) =
        op(maxY, widget.y + FRAME_SPRITE_SIZE - widget.frameSpritePadV + WINDOW_EDGE_GAP)

    @KShadow(Modifier.PRIVATE)
    abstract val minX: Int

    @KShadow(Modifier.PRIVATE)
    abstract val minY: Int

    private val AdvancementWidget.frameSpritePadH
        get() = when (display.type) {
            AdvancementType.TASK -> 1
            AdvancementType.GOAL -> 2
            AdvancementType.CHALLENGE -> 0
        }

    private val AdvancementWidget.frameSpritePadV
        get() = when (display.type) {
            AdvancementType.TASK -> 1
            else -> 0
        }

    companion object {
        private const val FRAME_SPRITE_SIZE = 26
        private const val FRAME_OFFSET_X = 3
        private const val WINDOW_EDGE_GAP = 1
    }
}

@KMixin<AdvancementWidget>(Side.Client)
class AdvancementWidgetFixes {

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
}
