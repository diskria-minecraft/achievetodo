package io.github.diskria.achievetodo.qol

import com.llamalad7.mixinextras.injector.ModifyExpressionValue
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.llamalad7.mixinextras.sugar.Local
import io.github.diskria.achievetodo.config.ModServerConfig
import io.github.diskria.achievetodo.extensions.invoke
import io.github.diskria.lapis.annotations.KMixin
import io.github.diskria.lapis.annotations.KShadow
import io.github.diskria.lapis.annotations.Side
import it.unimi.dsi.fastutil.Stack
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.advancements.Advancement
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.advancements.AdvancementNode
import net.minecraft.advancements.AdvancementProgress
import net.minecraft.server.PlayerAdvancements
import net.minecraft.server.advancements.AdvancementVisibilityEvaluator
import org.spongepowered.asm.mixin.injection.At
import java.util.function.Predicate
import javax.lang.model.element.Modifier
import kotlin.jvm.optionals.getOrNull

@KMixin<AdvancementVisibilityEvaluator>(Side.Common)
class AdvancementVisibilityRevealer {
    companion object {
        @ModifyExpressionValue(
            method = [$$"evaluateVisibility(Lnet/minecraft/advancements/AdvancementNode;Ljava/util/function/Predicate;Lnet/minecraft/server/advancements/AdvancementVisibilityEvaluator$Output;)V"],
            at = [At(value = "CONSTANT", args = ["intValue=2"])]
        )
        fun cancelStackPadding(original: Int): Int = if (revealMode != null) -1 else original

        @ModifyExpressionValue(
            method = ["evaluateVisiblityForUnfinishedNode"],
            at = [At(value = "CONSTANT", args = ["intValue=2"])]
        )
        fun resolveRevealDepth(
            original: Int,
            @Local(name = ["ascendants"], argsOnly = true) ascendants: Stack<*>,
        ): Int {
            if (ascendants !is ObjectArrayList<*>) return original
            val revealMode = revealMode ?: return original
            return when {
                revealMode >= 0 -> minOf(revealMode, ascendants.lastIndex)
                else -> minOf(original, ascendants.lastIndex)
            }
        }

        @WrapOperation(
            method = [$$"evaluateVisibility(Lnet/minecraft/advancements/AdvancementNode;Lit/unimi/dsi/fastutil/Stack;Ljava/util/function/Predicate;Lnet/minecraft/server/advancements/AdvancementVisibilityEvaluator$Output;)Z"],
            at = [At(value = "INVOKE", target = "Ljava/util/function/Predicate;test(Ljava/lang/Object;)Z")]
        )
        fun resolveTabReveal(isDoneTest: Predicate<*>, node: Any, original: Operation<Boolean>): Boolean {
            if (original(isDoneTest, node)) return true
            val revealMode = revealMode ?: return false
            if (node !is AdvancementNode) return false
            val rootNode = node.root()
            return when {
                rootNode.holder().id.toString() == "blazeandcave:bacap/root" -> true
                node.holder().id.toString() == "blazeandcave:technical/you_are_a_big_cheater" -> {
                    original(isDoneTest, rootNode)
                }

                !node.isDisplayable -> false
                revealMode == -1 -> rootNode.hasAnyDone { original(isDoneTest, it) }
                else -> revealMode == -2
            }
        }
    }
}

@KMixin<PlayerAdvancements>(Side.Common)
abstract class AdvancementTabsRevealer {

    @WrapOperation(
        method = ["registerListeners(Lnet/minecraft/advancements/AdvancementHolder;)V"],
        at = [At(value = "INVOKE", target = "Lnet/minecraft/advancements/AdvancementProgress;isDone()Z")]
    )
    fun revealTab(
        progress: AdvancementProgress,
        original: Operation<Boolean>,
        @Local(argsOnly = true, name = ["holder"]) advancement: AdvancementHolder,
    ): Boolean {
        if (original(progress)) return true
        val revealMode = revealMode ?: return false
        if (revealMode == -2 && advancement.value.isRoot) {
            markForVisibilityUpdate(advancement)
        }
        return false
    }

    @KShadow(Modifier.PRIVATE)
    abstract fun markForVisibilityUpdate(advancement: AdvancementHolder)
}

val Advancement.isDisplayable: Boolean get() = display.getOrNull()?.hidden == false
val AdvancementHolder.isDisplayable: Boolean get() = value.isDisplayable
val AdvancementNode.isDisplayable: Boolean get() = holder().isDisplayable

fun AdvancementNode.hasAnyDone(isDone: (AdvancementNode) -> Boolean): Boolean =
    isDone(this) || children().any { it.hasAnyDone(isDone) }

private val revealMode get() = ModServerConfig.current?.advancementsRevealMode
