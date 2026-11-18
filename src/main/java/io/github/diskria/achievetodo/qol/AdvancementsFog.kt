package io.github.diskria.achievetodo.qol

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.llamalad7.mixinextras.sugar.Local
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import io.github.diskria.achievetodo.extensions.*
import io.github.diskria.achievetodo.qol.AdvancementsFogManager.Client.HINT_NAMESPACE
import io.github.diskria.achievetodo.utils.tempAlpha
import io.github.diskria.advancements_search.client.AdvancementsScreenMixin
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
import kotlin.jvm.optionals.getOrNull

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

        fun getHintsFor(widget: AdvancementWidget) = hints[widget.advancement.id]
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
                if (node.advancement().display.isEmpty) return@forEach
                add(node)
                node.parent()?.let { parent ->
                    if (parent.holder().isVisible) add(parent)
                }
            }
            removedNodeIds.forEach { id ->
                val node = tree.get(id) ?: return@forEach
                if (node.advancement().display.isEmpty) return@forEach
                node.parent()?.let { parent ->
                    if (parent.holder().isVisible) add(parent)
                }
            }
        }
        val added = mutableMapOf<Identifier, List<AdvancementFogHint>>()
        val removed = removedNodeIds.filterTo(mutableSetOf()) { id ->
            val node = tree.get(id) ?: return@filterTo true
            node.advancement().display.isPresent && node.children().any { it.advancement().display.isPresent }
        }
        nodesToCheck.forEach { node ->
            val nodeId = node.holder().id
            val hints = node.children().mapNotNull { child ->
                if (child.holder().isVisible) return@mapNotNull null
                val childDisplay = child.advancement().display.getOrNull() ?: return@mapNotNull null
                if (childDisplay.hidden) return@mapNotNull null
                AdvancementFogHint(
                    icon = childDisplay.icon,
                    x = child.x(),
                    y = child.y(),
                )
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

    private val AdvancementHolder.isVisible: Boolean get() = this in visible

    @KShadow(Modifier.PRIVATE)
    abstract val isFirstPacket: Boolean

    @KShadow(Modifier.PRIVATE, Modifier.FINAL)
    abstract val visible: Set<AdvancementHolder>
}

@KMixin<AdvancementTab>(Side.Client)
abstract class AdvancementTabFog {

    @WrapMethod(method = ["addWidget"])
    fun AdvancementTab.onAddWidget(widget: AdvancementWidget, op: Op<Unit>) {
        if (rootAdvancement.id == AdvancementsScreenMixin.SEARCH_RESULTS_ROOT_ADVANCEMENT_ID || widget.isFogHint) {
            return op(widget)
        }
        op(widget)
        widget.parentWidget?.let { parent ->
            val fogHints = parent.fogHints
            val expectedSize = AdvancementsFogManager.Client.getHintsFor(parent)?.size ?: 0
            if (fogHints.size != expectedSize) {
                fogHints.forEach { widgets.remove(it.advancement) }
                parent.removeFogHints()
                addFogHintsFor(parent)
            }
            parent.moveFogHintsToFront()
        }
        addFogHintsFor(widget)
    }

    private fun AdvancementTab.addFogHintsFor(source: AdvancementWidget) {
        val sourceId = source.advancement.id
        AdvancementsFogManager.Client.getHintsFor(source)?.forEachIndexed { index, hint ->
            val uniquePath = "${sourceId.namespace}/${sourceId.path}/$index"
            val advancement = Advancement.Builder
                .recipeAdvancement()
                .display(hint.icon, Component.empty(), Component.empty(), AdvancementType.TASK, false, false, false)
                .parent(source.advancement)
                .build(Identifier.fromNamespaceAndPath(HINT_NAMESPACE, uniquePath))
            val node = AdvancementNode(advancement, source.advancementNode).apply { setLocation(hint.x, hint.y) }
            addAdvancement(node)
        }
    }

    @KShadow(Modifier.PRIVATE, Modifier.FINAL)
    abstract val widgets: MutableMap<AdvancementHolder, AdvancementWidget>
}

@KMixin<AdvancementWidget>(Side.Client)
abstract class AdvancementWidgetFogChildren {

    @Extension
    val fogHints: List<AdvancementWidget> get() = children.filter { it.isFogHint }

    @Extension
    val parentWidget: AdvancementWidget? get() = parent

    @Extension
    fun removeFogHints() {
        children.removeAll(fogHints)
    }

    @Extension
    fun moveFogHintsToFront() {
        children.sortByDescending { it.isFogHint }
    }

    @KShadow(Modifier.PRIVATE, Modifier.FINAL)
    abstract val children: MutableList<AdvancementWidget>

    @KShadow(Modifier.PRIVATE)
    abstract val parent: AdvancementWidget?
}

@KMixin<AdvancementWidget>(Side.Client)
class AdvancementWidgetFogRenderer(@Origin val widget: AdvancementWidget) {

    @Extension
    val isFogHint: Boolean get() = widget.advancement.id.namespace == HINT_NAMESPACE

    @WrapMethod(method = ["isMouseOver"])
    fun disableFogHintHover(originX: Int, originY: Int, mouseX: Int, mouseY: Int, op: Op<Boolean>) =
        !isFogHint && op(originX, originY, mouseX, mouseY)

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
        graphics.tempAlpha = 0.625f
        try {
            op(graphics, itemStack, x, y)
        } finally {
            graphics.tempAlpha = null
        }
    }

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

    private fun drawChessboardPixel(graphics: GuiGraphicsExtractor, x: Int, y: Int, originX: Int, originY: Int) {
        val isWhiteCell = (((x - originX) + (y - originY)) and 1) == 0
        graphics.fill(x, y, x + 1, y + 1, if (isWhiteCell) CommonColors.WHITE else CommonColors.BLACK)
    }
}
