package io.github.diskria.achievetodo.qol

import com.llamalad7.mixinextras.injector.ModifyReturnValue
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.llamalad7.mixinextras.sugar.Cancellable
import com.llamalad7.mixinextras.sugar.Local
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import io.github.diskria.achievetodo.AchieveToDoMod
import io.github.diskria.achievetodo.extensions.*
import io.github.diskria.lapis.annotations.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.client.player.LocalPlayer
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.ExecutionCommandSource
import net.minecraft.commands.arguments.ComponentArgument
import net.minecraft.commands.execution.ExecutionContext
import net.minecraft.commands.execution.Frame
import net.minecraft.commands.execution.tasks.CallFunction
import net.minecraft.commands.functions.InstantiatedFunction
import net.minecraft.commands.functions.MacroFunction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.*
import net.minecraft.network.chat.contents.NbtContents
import net.minecraft.network.chat.contents.ObjectContents
import net.minecraft.network.chat.contents.SelectorContents
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket
import net.minecraft.resources.Identifier
import org.spongepowered.asm.mixin.injection.At
import java.util.*
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE
import kotlin.jvm.optionals.getOrNull

object FlashAdvancementClickEvent {
    val CUSTOM_ID = AchieveToDoMod.getIdentifier("action/flash_advancement")

    val clickableFooter = Component.empty()
        .append(CommonComponents.NEW_LINE)
        .append("-> (").append(I18N.CLICK_TO_VIEW).append(")")
        .withFooterStyle()

    fun create(advancementId: Identifier): ClickEvent.Custom? =
        advancementId.toNbtOrNull(Identifier.CODEC)?.let { ClickEvent.Custom(CUSTOM_ID, Optional.of(it)) }
}

@KMixin<Screen>(Side.Client)
abstract class FlashAdvancementActionClickListener {
    companion object {
        @WrapOperation(
            method = ["defaultHandleGameClickEvent"],
            at = [At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"
            )]
        )
        context(
            @Local(argsOnly = true, name = ["minecraft"]) minecraft: Minecraft,
            @Local(argsOnly = true, name = ["activeScreen"]) currentScreen: Screen,
            @Local(name = ["player"]) player: LocalPlayer,
            @Cancellable cancel: Cancel
        )
        fun onFlashAdvancementActionClick(connection: ClientPacketListener, packet: Packet<*>, op: Op<Unit>) {
            if (packet !is ServerboundCustomClickActionPacket || packet.id != FlashAdvancementClickEvent.CUSTOM_ID) {
                return op(connection, packet)
            }
            val advancementId = packet.payload.getOrNull()?.decodeOrNull(Identifier.CODEC) ?: return cancel()
            val clientAdvancements = player.connection.advancements
            val advancementProgress = clientAdvancements.getProgressFor(advancementId) ?: return cancel()
            val returnScreen = if (currentScreen is ChatScreen) null else currentScreen
            val advancementsScreen = AdvancementsScreen(clientAdvancements, returnScreen)
            minecraft.gui.setScreen(advancementsScreen)
            val flashingMode = if (advancementProgress.isDone) {
                AdvancementFlashingMode.OBTAINED_STATUS
            } else {
                AdvancementFlashingMode.FRAME
            }
            advancementsScreen.startFlashing(advancementId, flashingMode)
            return cancel()
        }
    }
}

private class DoneAdvancementFunction<T : Any>(
    delegate: InstantiatedFunction<T>,
    val advancementId: Identifier,
) : InstantiatedFunction<T> by delegate {
    companion object {
        val ID = Identifier.fromNamespaceAndPath("bacap_rewards", "advancement_made_macro")
        const val ADV_ID_ARGUMENT = "adv_id"
    }
}

@KMixin<MacroFunction<*>>(Side.Common)
abstract class AdvancementDoneScopedMacro {
    @WrapMethod(method = ["instantiate"])
    fun MacroFunction<*>.initDoneAdvancementFunction(
        args: CompoundTag,
        dispatcher: CommandDispatcher<*>,
        op: Op<InstantiatedFunction<*>>
    ): InstantiatedFunction<*> {
        if (id() != DoneAdvancementFunction.ID) return op(args, dispatcher)
        var advId = args.getString(DoneAdvancementFunction.ADV_ID_ARGUMENT).getOrNull() ?: return op(args, dispatcher)
        if (advId == "blazeandcave:bacap/benchmarking") {
            advId = "minecraft:story/root"
            args.putString(DoneAdvancementFunction.ADV_ID_ARGUMENT, advId)
        }
        val original = op(args, dispatcher)
        val advancementId = Identifier.tryParse(advId) ?: return original
        return DoneAdvancementFunction(original, advancementId)
    }
}

@KMixin<CallFunction<*>>(Side.Common)
abstract class AdvancementDoneScopedCall {

    @WrapMethod(method = ["execute"])
    fun bindDoneAdvancementId(stack: ExecutionCommandSource<*>, exec: ExecutionContext<*>, frame: Frame, op: Op<Unit>) {
        if (stack !is CommandSourceStack) return op(stack, exec, frame)
        val doneAdvancementFunction = function as? DoneAdvancementFunction ?: return op(stack, exec, frame)
        op(stack.withDoneAdvancementId(doneAdvancementFunction.advancementId), exec, frame)
    }

    @KShadow(PRIVATE, FINAL)
    abstract val function: InstantiatedFunction<*>
}

@KMixin<CommandSourceStack>(Side.Common)
abstract class AdvancementDoneScopedStack(@Origin val current: CommandSourceStack) {

    @Extension
    var doneAdvancementId: Identifier? = null

    @Extension
    fun withDoneAdvancementId(id: Identifier): CommandSourceStack {
        val baseCallback = current.callback()
        return current.withCallback { success, result ->
            baseCallback.onResult(success, result)
        }.apply { doneAdvancementId = id }
    }

    @ModifyReturnValue(
        method = [
            "withSource",
            "withLevel",
            "withEntity",
            "withAnchor",
            "withPosition",
            "withRotation",
            "withPermission",
            "withSigningContext",
            "withSuppressedOutput",
            "withCallback(Lnet/minecraft/commands/CommandResultCallback;)Lnet/minecraft/commands/CommandSourceStack;",
        ],
        at = [At("RETURN")]
    )
    fun preserveDoneAdvancementId(changed: CommandSourceStack): CommandSourceStack =
        changed.apply { doneAdvancementId = current.doneAdvancementId }
}

@KMixin<ComponentArgument>(Side.Common)
abstract class AdvancementDoneMessageCustomizer {
    companion object {
        @ModifyReturnValue(method = ["getRawComponent"], at = [At("RETURN")])
        context(
            @Local(argsOnly = true, name = ["context"]) context: CommandContext<CommandSourceStack>,
        )
        fun customizeAdvancementDoneMessage(original: Component): Component {
            val doneAdvancementId = context.source.doneAdvancementId ?: return original
            val doneAdvancement = context.source.server.advancements[doneAdvancementId] ?: return original
            val flashAdvancement = FlashAdvancementClickEvent.create(doneAdvancementId) ?: return original
            if (doneAdvancement.value.isRoot) {
                val translateKey = (original.contents as? TranslatableContents)?.key ?: return original
                val (tabName) = I18N.TAB_UNLOCKED_REGEX.matchEntire(translateKey)?.destructured ?: return original
                return original.copy().withStyle {
                    val tooltip = I18N.tab(tabName).withFooterStyle().append(FlashAdvancementClickEvent.clickableFooter)
                    val showTooltip = HoverEvent.ShowText(tooltip)
                    it.withHoverEvent(showTooltip).withClickEvent(flashAdvancement)
                }
            }
            return original.withClickableHover(flashAdvancement)
        }

        private fun Component.withClickableHover(clickEvent: ClickEvent): Component {
            val result = MutableComponent.create(contents.transform(clickEvent))
            result.style = run {
                if (style.clickEvent != null) return@run style
                val bacTooltip = (style.hoverEvent as? HoverEvent.ShowText)?.value ?: return@run style
                val atdTooltip = bacTooltip.copy().append(FlashAdvancementClickEvent.clickableFooter)
                val showTooltip = HoverEvent.ShowText(atdTooltip)
                style.withHoverEvent(showTooltip).withClickEvent(clickEvent)
            }
            siblings.forEach { result.append(it.withClickableHover(clickEvent)) }
            return result
        }

        private fun ComponentContents.transform(clickEvent: ClickEvent) = when (this) {
            is TranslatableContents -> {
                val args = args.map { if (it is Component) it.withClickableHover(clickEvent) else it }.toTypedArray()
                TranslatableContents(key, fallback, args)
            }

            is SelectorContents -> {
                val separator = separator.map { it.withClickableHover(clickEvent) }
                SelectorContents(selector, separator)
            }

            is NbtContents -> {
                val separator = separator.map { it.withClickableHover(clickEvent) }
                NbtContents(nbtPath, interpreting, plain, separator, dataSource)
            }

            is ObjectContents -> {
                val fallback = fallback.map { it.withClickableHover(clickEvent) }
                ObjectContents(contents, fallback)
            }

            else -> this
        }
    }
}
