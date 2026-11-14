package io.github.diskria.achievetodo.qol

import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.llamalad7.mixinextras.sugar.Local
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
        method = ["scroll"],
        at = [At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(DDD)D", ordinal = 0)]
    )
    fun fixScrollX(value: Double, min: Double, max: Double, original: Operation<Double>) =
        original(value, min, -minX.toDouble())

    @WrapOperation(
        method = ["scroll"],
        at = [At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(DDD)D", ordinal = 1)]
    )
    fun fixScrollY(value: Double, min: Double, max: Double, original: Operation<Double>) =
        original(value, min, -minY.toDouble())

    @WrapOperation(
        method = ["addWidget"],
        at = [At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I", ordinal = 0)]
    )
    fun padFrameMinX(
        minX: Int,
        x0: Int,
        original: Operation<Int>,
        @Local(argsOnly = true, name = ["widget"]) widget: AdvancementWidget
    ) = original(minX, widget.x + FRAME_OFFSET_X + widget.display.type.spritePadH - WINDOW_EDGE_GAP)

    @WrapOperation(
        method = ["addWidget"],
        at = [At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I", ordinal = 0)]
    )
    fun padFrameMaxX(
        maxX: Int,
        x1: Int,
        original: Operation<Int>,
        @Local(argsOnly = true, name = ["widget"]) widget: AdvancementWidget
    ) = original(maxX, widget.x + FRAME_OFFSET_X + FRAME_SPRITE_SIZE - widget.display.type.spritePadH + WINDOW_EDGE_GAP)

    @WrapOperation(
        method = ["addWidget"],
        at = [At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I", ordinal = 1)]
    )
    fun padFrameMinY(
        minY: Int,
        y0: Int,
        original: Operation<Int>,
        @Local(argsOnly = true, name = ["widget"]) widget: AdvancementWidget
    ) = original(minY, widget.y + widget.display.type.spritePadV - WINDOW_EDGE_GAP)

    @WrapOperation(
        method = ["addWidget"],
        at = [At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I", ordinal = 1)]
    )
    fun padFrameMaxY(
        maxY: Int,
        y1: Int,
        original: Operation<Int>,
        @Local(argsOnly = true, name = ["widget"]) widget: AdvancementWidget
    ) = original(maxY, widget.y + FRAME_SPRITE_SIZE - widget.display.type.spritePadV + WINDOW_EDGE_GAP)

    @KShadow(Modifier.PRIVATE)
    abstract val minX: Int

    @KShadow(Modifier.PRIVATE)
    abstract val minY: Int

    private val AdvancementType.spritePadH
        get() = when (this) {
            AdvancementType.TASK -> 1
            AdvancementType.GOAL -> 2
            AdvancementType.CHALLENGE -> 0
        }

    private val AdvancementType.spritePadV
        get() = if (this == AdvancementType.TASK) 1 else 0

    companion object {
        private const val FRAME_SPRITE_SIZE = 26
        private const val FRAME_OFFSET_X = 3
        private const val WINDOW_EDGE_GAP = 1
    }
}

@KMixin<AdvancementWidget>(Side.Client)
abstract class AdvancementWidgetFixes {

    @WrapOperation(
        method = ["extractConnectivity"],
        at = [
            At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;horizontalLine(IIII)V",
                ordinal = 0
            ),
            At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;horizontalLine(IIII)V",
                ordinal = 2
            )
        ]
    )
    fun fixConnectivityCorner(
        graphics: GuiGraphicsExtractor,
        x0: Int,
        x1: Int,
        y: Int,
        color: Int,
        original: Operation<Unit>
    ) = original(graphics, x0 + 1, x1, y, color)
}
