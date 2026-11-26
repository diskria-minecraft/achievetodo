package io.github.diskria.achievetodo.qol

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.llamalad7.mixinextras.sugar.Local
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import io.github.diskria.achievetodo.extensions.*
import io.github.diskria.achievetodo.utils.itemTempAlpha
import io.github.diskria.achievetodo.utils.parent
import io.github.diskria.lapis.annotations.*
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.advancements.AdvancementNode
import net.minecraft.advancements.AdvancementType
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.render.GuiRenderer
import net.minecraft.client.gui.screens.advancements.AdvancementTab
import net.minecraft.client.gui.screens.advancements.AdvancementWidget
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen
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
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE

object AdvancementsFogManager {

    fun init() {
        ClientboundUpdateAdvancementsFogPayload.register()
    }

    @Environment(EnvType.CLIENT)
    object Client {

        private val hints = mutableMapOf<Identifier, List<AdvancementsFogHint>>()

        fun init() {
            ClientboundUpdateAdvancementsFogPayload.Client.listen { shouldReset, added, removed ->
                if (shouldReset) hints.clear()
                added.forEach { (id, newHints) -> hints[id] = newHints }
                removed.forEach { hints.remove(it) }
            }
            ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> hints.clear() }
        }

        fun getHintsFor(advancementId: Identifier) = hints[advancementId]
    }
}

class ClientboundUpdateAdvancementsFogPayload private constructor(
    private val shouldReset: Boolean,
    private val added: Map<Identifier, List<AdvancementsFogHint>>,
    private val removed: Set<Identifier>,
) : CustomPacketPayload {

    override fun type() = TYPE

    object Server {
        context(player: ServerPlayer)
        fun send(shouldReset: Boolean, added: Map<Identifier, List<AdvancementsFogHint>>, removed: Set<Identifier>) {
            if (!ServerPlayNetworking.canSend(player, TYPE)) return
            ServerPlayNetworking.send(player, ClientboundUpdateAdvancementsFogPayload(shouldReset, added, removed))
        }
    }

    @Environment(EnvType.CLIENT)
    object Client {
        fun listen(
            action: ClientPlayNetworking.Context.(
                shouldReset: Boolean,
                added: Map<Identifier, List<AdvancementsFogHint>>,
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
            mapCodecOf(Identifier.STREAM_CODEC to listCodecOf(AdvancementsFogHint.STREAM_CODEC)), Payload::added,
            setCodecOf(Identifier.STREAM_CODEC), Payload::removed,
            ::Payload
        )

        fun register() = PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC)
    }
}

data class AdvancementsFogHint(val icon: ItemStackTemplate, val x: Float, val y: Float) {
    companion object {
        const val ID_NAMESPACE = "advancements_fog_hint"

        val STREAM_CODEC = StreamCodec.composite(
            ItemStackTemplate.STREAM_CODEC, AdvancementsFogHint::icon,
            ByteBufCodecs.FLOAT, AdvancementsFogHint::x,
            ByteBufCodecs.FLOAT, AdvancementsFogHint::y,
            ::AdvancementsFogHint
        )

        fun idFor(parentId: Identifier, hintIndex: Int): Identifier =
            Identifier.fromNamespaceAndPath(ID_NAMESPACE, "${parentId.namespace}/${parentId.path}/$hintIndex")
    }
}

@KMixin<PlayerAdvancements>(Side.Common)
abstract class AdvancementsFogTracker {

    @WrapOperation(
        method = ["flushDirty"],
        at = [At(
            value = "INVOKE",
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
                val node = tree[id] ?: return@forEach
                if (!node.isDisplayable) return@forEach
                node.parent()?.let { parent ->
                    if (parent.isRevealed) add(parent)
                }
            }
        }
        val added = mutableMapOf<Identifier, List<AdvancementsFogHint>>()
        val removed = removedNodeIds.filterTo(mutableSetOf()) { id ->
            val node = tree[id] ?: return@filterTo true
            node.isDisplayable && node.children().any { it.isDisplayable }
        }
        nodesToCheck.forEach { node ->
            val nodeId = node.holder().id
            val hints = node.children().mapNotNull { child ->
                if (child.isRevealed) return@mapNotNull null
                val childDisplay = child.getVisibleDisplay() ?: return@mapNotNull null
                if (childDisplay.hidden) return@mapNotNull null
                AdvancementsFogHint(childDisplay.icon, child.x(), child.y())
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

    @KShadow(PRIVATE)
    abstract val isFirstPacket: Boolean

    @MappingName("visible")
    @KShadow(PRIVATE, FINAL)
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

    @KShadow(PRIVATE, FINAL)
    abstract val widgets: MutableMap<AdvancementHolder, AdvancementWidget>
}

@KMixin<AdvancementWidget>(Side.Client)
abstract class AdvancementWidgetFogHintChildren(@Origin val widget: AdvancementWidget) {

    private val id: Identifier get() = widget.advancement.id

    @Extension
    val fogHints: List<AdvancementWidget> get() = children.filter { it.advancement.isFogHint }

    @Extension
    fun attachFogHintsToParent(tab: AdvancementTab) {
        AdvancementsFogManager.Client.getHintsFor(advancementNode.holder().id).orEmpty().forEachIndexed { index, hint ->
            val childHolder = buildTechnicalAdvancement(AdvancementsFogHint.idFor(id, index)) {
                parent(widget.advancement)
                display(hint.icon, Component.empty(), Component.empty(), AdvancementType.TASK, false, false, false)
            }
            tab.addAdvancement(advancementNode.child(childHolder) { setLocation(hint.x, hint.y) })
        }
        children.sortByDescending { it.advancement.isFogHint }
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

    @KShadow(PRIVATE, FINAL)
    abstract val advancementNode: AdvancementNode

    @KShadow(PRIVATE, FINAL)
    abstract val children: MutableList<AdvancementWidget>
}

@KMixin<AdvancementWidget>(Side.Client)
abstract class AdvancementWidgetFogHintRenderer(@Origin val widget: AdvancementWidget) {

    private val isFogHint get() = widget.advancement.isFogHint

    @WrapOperation(
        method = ["extractConnectivity"],
        at = [At("INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;horizontalLine(IIII)V")]
    )
    context(
        @Local(argsOnly = true, name = ["xo"]) offsetX: Int,
        @Local(argsOnly = true, name = ["yo"]) offsetY: Int,
        @Local(argsOnly = true, name = ["background"]) isBorderPass: Boolean,
    )
    fun drawHorizontalZebra(graphics: GuiGraphicsExtractor, x0: Int, x1: Int, y: Int, color: Int, op: Op<Unit>) {
        if (!isFogHint) return op(graphics, x0, x1, y, color)
        if (isBorderPass) return
        (minOf(x0, x1)..maxOf(x0, x1)).forEach { x -> drawChessboardCell(graphics, x, y, offsetX, offsetY) }
    }

    @WrapOperation(
        method = ["extractConnectivity"],
        at = [At("INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;verticalLine(IIII)V")]
    )
    context(
        @Local(argsOnly = true, name = ["xo"]) offsetX: Int,
        @Local(argsOnly = true, name = ["yo"]) offsetY: Int,
        @Local(argsOnly = true, name = ["background"]) isBorderPass: Boolean,
    )
    fun drawVerticalZebra(graphics: GuiGraphicsExtractor, x: Int, y0: Int, y1: Int, color: Int, op: Op<Unit>) {
        if (!isFogHint) return op(graphics, x, y0, y1, color)
        if (isBorderPass) return
        (minOf(y0, y1)..maxOf(y0, y1)).forEach { y -> drawChessboardCell(graphics, x, y, offsetX, offsetY) }
    }

    @WrapWithCondition(
        method = ["extractRenderState"],
        at = [At(
            value = "INVOKE",
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
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fakeItem(Lnet/minecraft/world/item/ItemStack;II)V"
        )]
    )
    fun fadeFogHintItem(graphics: GuiGraphicsExtractor, itemStack: ItemStack, itemX: Int, itemY: Int, op: Op<Unit>) {
        if (!isFogHint) return op(graphics, itemStack, itemX, itemY)
        graphics.itemTempAlpha = 0.45f
        try {
            op(graphics, itemStack, itemX, itemY)
        } finally {
            graphics.itemTempAlpha = null
        }
        val screen = minecraft.gui.screen() as? AdvancementsScreen ?: return
        val mouseX = minecraft.mouseHandler.getScaledXPos(minecraft.window).toInt()
        val mouseY = minecraft.mouseHandler.getScaledYPos(minecraft.window).toInt()
        val relativeMouseX = mouseX - screen.treeX
        val relativeMouseY = mouseY - screen.treeY
        val itemSize = GuiRenderer.DEFAULT_ITEM_SIZE
        if (relativeMouseX in itemX..(itemX + itemSize) && relativeMouseY in itemY..(itemY + itemSize)) {
            graphics.setTooltipForNextFrame(minecraft.font, Component.literal("???"), mouseX, mouseY)
        }
    }

    @WrapMethod(method = ["isMouseOver"])
    fun disableFogHintHover(offsetX: Int, offsetY: Int, mouseX: Int, mouseY: Int, op: Op<Boolean>) =
        !isFogHint && op(offsetX, offsetY, mouseX, mouseY)

    private fun drawChessboardCell(graphics: GuiGraphicsExtractor, px: Int, py: Int, offsetX: Int, offsetY: Int) {
        val isWhiteCell = ((px - offsetX) + (py - offsetY)).isEven()
        graphics.fill(px, py, px + 1, py + 1, if (isWhiteCell) CommonColors.WHITE else CommonColors.BLACK)
    }

    @KShadow(PRIVATE, FINAL)
    abstract val minecraft: Minecraft
}

val AdvancementHolder.isFogHint get() = id.namespace == AdvancementsFogHint.ID_NAMESPACE
