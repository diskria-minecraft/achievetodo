package io.github.diskria.achievetodo.qol

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.llamalad7.mixinextras.sugar.Local
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import io.github.diskria.achievetodo.extensions.*
import io.github.diskria.achievetodo.qol.AdvancementsFogManager.Client.HINT_NAMESPACE
import io.github.diskria.achievetodo.utils.itemTempAlpha
import io.github.diskria.lapis.annotations.*
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.advancements.Advancement
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.advancements.AdvancementNode
import net.minecraft.advancements.AdvancementType
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.advancements.AdvancementTab
import net.minecraft.client.gui.screens.advancements.AdvancementWidget
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.minecraft.server.PlayerAdvancements
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.util.CommonColors
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStackTemplate
import org.spongepowered.asm.mixin.injection.At
import javax.lang.model.element.Modifier

object AdvancementsFogManager {

    fun init() {
        ClientboundUpdateAdvancementsFogPayload.register()
    }

    @Environment(EnvType.CLIENT)
    object Client {

        const val HINT_NAMESPACE = "advancements_fog_hint"

        private val hints: MutableMap<Identifier, List<AdvancementFogHint>> = mutableMapOf()

        fun init() {
            ClientboundUpdateAdvancementsFogPayload.Client.listen { shouldReset, added, removed ->
                if (shouldReset) hints.clear()
                added.forEach { (id, newHints) -> hints[id] = newHints }
                removed.forEach { hints.remove(it) }
            }
            ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> hints.clear() }
        }

        fun getHintsFor(id: Identifier) = hints[id]
    }
}

class ClientboundUpdateAdvancementsFogPayload private constructor(
    private val shouldReset: Boolean,
    private val added: Map<Identifier, List<AdvancementFogHint>>,
    private val removed: Set<Identifier>,
) : CustomPacketPayload {

    override fun type() = TYPE

    object Server {
        context(player: ServerPlayer)
        fun send(shouldReset: Boolean, added: Map<Identifier, List<AdvancementFogHint>>, removed: Set<Identifier>) {
            ServerPlayNetworking.send(player, ClientboundUpdateAdvancementsFogPayload(shouldReset, added, removed))
        }
    }

    @Environment(EnvType.CLIENT)
    object Client {
        fun listen(
            action: ClientPlayNetworking.Context.(
                shouldReset: Boolean,
                added: Map<Identifier, List<AdvancementFogHint>>,
                removed: Set<Identifier>,
            ) -> Unit
        ) {
            ClientPlayNetworking.registerGlobalReceiver(TYPE) { payload, context ->
                context.action(payload.shouldReset, payload.added, payload.removed)
            }
        }
    }

    companion object {
        private typealias Payload = ClientboundUpdateAdvancementsFogPayload

        private val TYPE = payloadTypeOf<Payload>()
        private val CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, Payload::shouldReset,
            mapCodecOf(Identifier.STREAM_CODEC to listCodecOf(AdvancementFogHint.STREAM_CODEC)), Payload::added,
            setCodecOf(Identifier.STREAM_CODEC), Payload::removed,
            ::Payload
        )

        fun register() = PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC)
    }
}

data class AdvancementFogHint(val icon: ItemStackTemplate, val x: Float, val y: Float) {
    companion object {
        val STREAM_CODEC = StreamCodec.composite(
            ItemStackTemplate.STREAM_CODEC, AdvancementFogHint::icon,
            ByteBufCodecs.FLOAT, AdvancementFogHint::x,
            ByteBufCodecs.FLOAT, AdvancementFogHint::y,
            ::AdvancementFogHint
        )
    }
}

@KMixin<PlayerAdvancements>(Side.Common)
abstract class AdvancementsFogTracker {

    @WrapOperation(
        method = ["flushDirty"],
        at = [At(
            "INVOKE",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"
        )]
    )
    context(
        @Local(name = ["added"]) addedNodes: Set<AdvancementNode>,
        @Local(name = ["removed"]) removedNodeIds: Set<Identifier>,
    )
    fun sendFogUpdate(connection: ServerGamePacketListenerImpl, packet: Packet<*>, op: Op<Unit>) {
        val tree = connection.player.level().server.advancements.tree()
        val nodesToCheck = buildSet {
            addedNodes.forEach { node ->
                if (!node.isDisplayable) return@forEach
                add(node)
                node.parent()?.let { parent ->
                    if (parent.isRevealed) add(parent)
                }
            }
            removedNodeIds.forEach { id ->
                val node = tree.get(id) ?: return@forEach
                if (!node.isDisplayable) return@forEach
                node.parent()?.let { parent ->
                    if (parent.isRevealed) add(parent)
                }
            }
        }
        val added = mutableMapOf<Identifier, List<AdvancementFogHint>>()
        val removed = removedNodeIds.filterTo(mutableSetOf()) { id ->
            val node = tree.get(id) ?: return@filterTo true
            node.isDisplayable && node.children().any { it.isDisplayable }
        }
        nodesToCheck.forEach { node ->
            val nodeId = node.holder().id
            val hints = node.children().mapNotNull { child ->
                if (child.isRevealed) return@mapNotNull null
                val childDisplay = child.getVisibleDisplay() ?: return@mapNotNull null
                if (childDisplay.hidden) return@mapNotNull null
                AdvancementFogHint(childDisplay.icon, child.x(), child.y())
            }
            if (hints.isNotEmpty()) {
                added[nodeId] = hints
                removed.remove(nodeId)
            } else if (node !in addedNodes) {
                removed.add(nodeId)
            }
        }
        if (isFirstPacket || added.isNotEmpty() || removed.isNotEmpty()) {
            context(connection.player) {
                ClientboundUpdateAdvancementsFogPayload.Server.send(isFirstPacket, added, removed)
            }
        }
        op(connection, packet)
    }

    @KShadow(Modifier.PRIVATE)
    abstract val isFirstPacket: Boolean

    @MappingName("visible")
    @KShadow(Modifier.PRIVATE, Modifier.FINAL)
    abstract val revealed: Set<AdvancementHolder>

    private val AdvancementNode.isRevealed: Boolean get() = holder() in revealed
}

@KMixin<AdvancementTab>(Side.Client)
abstract class AdvancementTabFog(@Origin val tab: AdvancementTab) {

    @Extension
    fun removeWidget(holder: AdvancementHolder) {
        widgets.remove(holder)
    }

    @WrapMethod(method = ["addWidget"])
    fun onAddWidget(widget: AdvancementWidget, op: Op<Unit>) {
        op(widget)
        if (tab.rootAdvancement.isSearchResultsRoot || widget.advancement.isFogHint) return
        widget.parent?.rebuildFogHints(tab)
        widget.attachFogHintsToParent(tab)
    }

    @KShadow(Modifier.PRIVATE, Modifier.FINAL)
    abstract val widgets: MutableMap<AdvancementHolder, AdvancementWidget>
}

@KMixin<AdvancementWidget>(Side.Client)
abstract class AdvancementWidgetFogHintChildren(@Origin val widget: AdvancementWidget) {

    private val id: Identifier get() = widget.advancement.id

    @Extension
    val fogHints: List<AdvancementWidget> get() = children.filter { it.advancement.isFogHint }

    @Extension
    val parent: AdvancementWidget? get() = parentWidget

    @Extension
    fun attachFogHintsToParent(tab: AdvancementTab) {
        val builderBase = Advancement.Builder.recipeAdvancement().parent(widget.advancement)
        AdvancementsFogManager.Client.getHintsFor(advancementNode.holder().id).orEmpty().forEachIndexed { index, hint ->
            val childHolder = builderBase
                .display(hint.icon, Component.empty(), Component.empty(), AdvancementType.TASK, false, false, false)
                .build(Identifier.fromNamespaceAndPath(HINT_NAMESPACE, "${id.namespace}/${id.path}/$index"))
            val childNode = AdvancementNode(childHolder, advancementNode).apply { setLocation(hint.x, hint.y) }
            tab.addAdvancement(childNode)
        }
        moveFogHintsToFront()
    }

    @Extension
    fun rebuildFogHints(tab: AdvancementTab) {
        val expectedSize = AdvancementsFogManager.Client.getHintsFor(advancementNode.holder().id)?.size ?: 0
        val fogHints = fogHints
        if (fogHints.size == expectedSize) return
        fogHints.forEach { tab.removeWidget(it.advancement) }
        children.removeAll(fogHints)
        attachFogHintsToParent(tab)
    }

    private fun moveFogHintsToFront() {
        children.sortByDescending { it.advancement.isFogHint }
    }

    @KShadow(Modifier.PRIVATE, Modifier.FINAL)
    abstract val advancementNode: AdvancementNode

    @KShadow(Modifier.PRIVATE, Modifier.FINAL)
    abstract val children: MutableList<AdvancementWidget>

    @MappingName("parent")
    @KShadow(Modifier.PRIVATE)
    abstract val parentWidget: AdvancementWidget?
}

@KMixin<AdvancementWidget>(Side.Client)
class AdvancementWidgetFogHintRenderer(@Origin val widget: AdvancementWidget) {

    private val isFogHint get() = widget.advancement.isFogHint

    @WrapOperation(
        method = ["extractConnectivity"],
        at = [At("INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;horizontalLine(IIII)V")]
    )
    context(
        @Local(argsOnly = true, name = ["xo"]) originX: Int,
        @Local(argsOnly = true, name = ["yo"]) originY: Int,
        @Local(argsOnly = true, name = ["background"]) isBorderPass: Boolean,
    )
    fun drawHorizontalZebra(graphics: GuiGraphicsExtractor, x0: Int, x1: Int, y: Int, color: Int, op: Op<Unit>) {
        if (!isFogHint) return op(graphics, x0, x1, y, color)
        if (isBorderPass) return
        (minOf(x0, x1)..maxOf(x0, x1)).forEach { x -> drawChessboardPixel(graphics, x, y, originX, originY) }
    }

    @WrapOperation(
        method = ["extractConnectivity"],
        at = [At("INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;verticalLine(IIII)V")]
    )
    context(
        @Local(argsOnly = true, name = ["xo"]) originX: Int,
        @Local(argsOnly = true, name = ["yo"]) originY: Int,
        @Local(argsOnly = true, name = ["background"]) isBorderPass: Boolean,
    )
    fun drawVerticalZebra(graphics: GuiGraphicsExtractor, x: Int, y0: Int, y1: Int, color: Int, op: Op<Unit>) {
        if (!isFogHint) return op(graphics, x, y0, y1, color)
        if (isBorderPass) return
        (minOf(y0, y1)..maxOf(y0, y1)).forEach { y -> drawChessboardPixel(graphics, x, y, originX, originY) }
    }

    @WrapWithCondition(
        method = ["extractRenderState"],
        at = [At(
            "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"
        )]
    )
    fun hideFogHintFrame(
        graphics: GuiGraphicsExtractor,
        renderPipeline: RenderPipeline,
        location: Identifier,
        x: Int, y: Int,
        width: Int, height: Int,
    ) = !isFogHint

    @WrapOperation(
        method = ["extractRenderState"],
        at = [At(
            "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fakeItem(Lnet/minecraft/world/item/ItemStack;II)V"
        )]
    )
    fun fadeFogHintItem(graphics: GuiGraphicsExtractor, itemStack: ItemStack, x: Int, y: Int, op: Op<Unit>) {
        if (!isFogHint) return op(graphics, itemStack, x, y)
        graphics.itemTempAlpha = 0.8_800_555_35_35.toFloat() / 2
        try {
            op(graphics, itemStack, x, y)
        } finally {
            graphics.itemTempAlpha = null
        }
    }

    @WrapMethod(method = ["isMouseOver"])
    fun disableFogHintHover(originX: Int, originY: Int, mouseX: Int, mouseY: Int, op: Op<Boolean>) =
        !isFogHint && op(originX, originY, mouseX, mouseY)

    private fun drawChessboardPixel(graphics: GuiGraphicsExtractor, x: Int, y: Int, originX: Int, originY: Int) {
        val isWhiteCell = (((x - originX) + (y - originY)) and 1) == 0
        graphics.fill(x, y, x + 1, y + 1, if (isWhiteCell) CommonColors.WHITE else CommonColors.BLACK)
    }
}
