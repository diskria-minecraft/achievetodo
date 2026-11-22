package io.github.diskria.achievetodo.qol

import com.llamalad7.mixinextras.injector.ModifyExpressionValue
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.llamalad7.mixinextras.sugar.Local
import io.github.diskria.achievetodo.config.ModServerConfig
import io.github.diskria.achievetodo.extensions.Op
import io.github.diskria.achievetodo.extensions.hasAnyDone
import io.github.diskria.achievetodo.extensions.invoke
import io.github.diskria.achievetodo.extensions.isVisible
import io.github.diskria.lapis.annotations.KMixin
import io.github.diskria.lapis.annotations.KShadow
import io.github.diskria.lapis.annotations.Side
import it.unimi.dsi.fastutil.Stack
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.advancements.AdvancementNode
import net.minecraft.advancements.AdvancementProgress
import net.minecraft.server.PlayerAdvancements
import net.minecraft.server.advancements.AdvancementVisibilityEvaluator
import org.spongepowered.asm.mixin.injection.At
import java.util.function.Predicate
import javax.lang.model.element.Modifier.PRIVATE

@KMixin<AdvancementVisibilityEvaluator>(Side.Common)
class AdvancementsRevealer {
    companion object {
        @ModifyExpressionValue(
            method = [$$"evaluateVisibility(Lnet/minecraft/advancements/AdvancementNode;Ljava/util/function/Predicate;Lnet/minecraft/server/advancements/AdvancementVisibilityEvaluator$Output;)V"],
            at = [At("CONSTANT", args = ["intValue=2"])]
        )
        fun cancelStackPadding(original: Int): Int = if (revealMode != null) -1 else original

        @Suppress("SpellCheckingInspection")
        @ModifyExpressionValue(
            method = ["evaluateVisiblityForUnfinishedNode"], at = [At("CONSTANT", args = ["intValue=2"])]
        )
        context(
            @Local(argsOnly = true, name = ["ascendants"]) ascendants: Stack<*>,
        )
        fun resolveRevealDepth(original: Int): Int {
            if (ascendants !is ObjectArrayList<*>) return original
            val revealMode = revealMode ?: return original
            val candidate = if (revealMode >= 0) revealMode else original
            return minOf(candidate, ascendants.lastIndex)
        }

        @WrapOperation(
            method = [$$"evaluateVisibility(Lnet/minecraft/advancements/AdvancementNode;Lit/unimi/dsi/fastutil/Stack;Ljava/util/function/Predicate;Lnet/minecraft/server/advancements/AdvancementVisibilityEvaluator$Output;)Z"],
            at = [At("INVOKE", target = "Ljava/util/function/Predicate;test(Ljava/lang/Object;)Z")]
        )
        fun resolveTabReveal(isDone: Predicate<*>, node: Any, op: Op<Boolean>): Boolean {
            if (op(isDone, node)) return true
            val revealMode = revealMode ?: return false
            if (node !is AdvancementNode) return false
            val rootNode = node.root()
            return when {
                rootNode.holder().id.toString() == "blazeandcave:bacap/root" -> true
                node.holder().id.toString() == "blazeandcave:technical/you_are_a_big_cheater" -> op(isDone, rootNode)
                !node.isVisible -> false
                revealMode == -1 -> rootNode.hasAnyDone { op(isDone, it) }
                else -> revealMode == -2
            }
        }
    }
}

@KMixin<PlayerAdvancements>(Side.Common)
abstract class AdvancementTabsRevealer {

    @WrapOperation(
        method = ["registerListeners(Lnet/minecraft/advancements/AdvancementHolder;)V"],
        at = [At("INVOKE", target = "Lnet/minecraft/advancements/AdvancementProgress;isDone()Z")]
    )
    context(
        @Local(argsOnly = true, name = ["holder"]) advancement: AdvancementHolder,
    )
    fun revealTab(progress: AdvancementProgress, op: Op<Boolean>): Boolean {
        if (op(progress)) return true
        val revealMode = revealMode ?: return false
        if (revealMode == -2 && advancement.value.isRoot) {
            markForVisibilityUpdate(advancement)
        }
        return false
    }

    @KShadow(PRIVATE)
    abstract fun markForVisibilityUpdate(advancement: AdvancementHolder)
}

private val revealMode get() = ModServerConfig.current?.advancementsRevealMode
