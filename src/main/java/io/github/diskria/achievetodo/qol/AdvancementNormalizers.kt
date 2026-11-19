package io.github.diskria.achievetodo.qol

import com.llamalad7.mixinextras.injector.ModifyReturnValue
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import io.github.diskria.achievetodo.extensions.Op
import io.github.diskria.achievetodo.extensions.countAdvancements
import io.github.diskria.achievetodo.extensions.invoke
import io.github.diskria.lapis.annotations.Extension
import io.github.diskria.lapis.annotations.KMixin
import io.github.diskria.lapis.annotations.Origin
import io.github.diskria.lapis.annotations.Side
import net.minecraft.advancements.AdvancementNode
import org.spongepowered.asm.mixin.injection.At

@KMixin<AdvancementNode>(Side.Common)
class AdvancementChildrenNormalizer(@Origin val current: AdvancementNode) {

    private var normalizedChildren: List<AdvancementNode>? = null

    @Extension
    fun invalidateNormalizedChildren() {
        normalizedChildren = null
    }

    @WrapMethod(method = ["addChild"])
    fun invalidateAncestors(child: AdvancementNode, op: Op<Unit>) {
        op(child)
        generateSequence(current) { it.parent() }.forEach { it.invalidateNormalizedChildren() }
    }

    @ModifyReturnValue(method = ["children"], at = [At("RETURN")])
    fun normalizeChildren(original: Iterable<AdvancementNode>): Iterable<AdvancementNode> {
        normalizedChildren?.let { return it }
        val list = original.toList()
        val result = if (list.size > 1) {
            val sorted = list.sortedBy { it.holder().id }.sortedBy { it.countAdvancements() }
            val pyramid = ArrayDeque<AdvancementNode>(sorted.size).apply {
                sorted.asReversed().forEachIndexed { index, node ->
                    if (index % 2 == 0) addFirst(node) else addLast(node)
                }
            }
            pyramid
        } else {
            list
        }
        return result.also { normalizedChildren = it }
    }
}
