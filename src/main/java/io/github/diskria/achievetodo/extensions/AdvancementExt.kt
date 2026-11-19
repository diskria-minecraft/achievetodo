package io.github.diskria.achievetodo.extensions

import io.github.diskria.achievetodo.qol.AdvancementsFogManager
import io.github.diskria.advancements_search.client.AdvancementsScreenMixin
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.advancements.AdvancementNode
import net.minecraft.advancements.DisplayInfo
import kotlin.jvm.optionals.getOrNull

val AdvancementNode.isDisplayable: Boolean
    get() = advancement().display.isPresent

val AdvancementHolder.isFogHint: Boolean
    get() = id.namespace == AdvancementsFogManager.Client.HINT_NAMESPACE

val AdvancementHolder.isSearchResultsRoot: Boolean
    get() = id == AdvancementsScreenMixin.SEARCH_RESULTS_ROOT_ADVANCEMENT_ID

val AdvancementNode.isVisible: Boolean get() = getVisibleDisplay() != null

fun AdvancementNode.getVisibleDisplay(): DisplayInfo? =
    advancement().display.getOrNull()

fun AdvancementNode.hasAnyDone(isDone: (AdvancementNode) -> Boolean): Boolean =
    isDone(this) || children().any { it.hasAnyDone(isDone) }

fun AdvancementNode.countAdvancements(): Int = 1 + children().sumOf { it.countAdvancements() }
