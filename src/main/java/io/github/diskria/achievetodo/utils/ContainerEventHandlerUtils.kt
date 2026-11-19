package io.github.diskria.achievetodo.utils

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import io.github.diskria.achievetodo.extensions.Op
import io.github.diskria.achievetodo.extensions.invoke
import io.github.diskria.achievetodo.qol.feedCharToSearch
import io.github.diskria.achievetodo.qol.feedPreeditToSearch
import io.github.diskria.lapis.annotations.KMixin
import io.github.diskria.lapis.annotations.Side
import net.minecraft.client.gui.components.events.ContainerEventHandler
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.PreeditEvent

@KMixin<ContainerEventHandler>(Side.Client)
interface ContainerEventHandlerUtils {

    @WrapMethod(method = ["charTyped"])
    fun ContainerEventHandler.overrideCharTyped(event: CharacterEvent, op: Op<Boolean>) = when (this) {
        is AdvancementsScreen -> feedCharToSearch(event)
        else -> op(event)
    }

    @WrapMethod(method = ["preeditUpdated"])
    fun ContainerEventHandler.overridePreeditUpdated(event: PreeditEvent?, op: Op<Boolean>) = when (this) {
        is AdvancementsScreen -> feedPreeditToSearch(event)
        else -> op(event)
    }
}
