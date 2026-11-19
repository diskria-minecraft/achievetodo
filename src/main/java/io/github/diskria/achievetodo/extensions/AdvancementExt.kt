package io.github.diskria.achievetodo.extensions

import io.github.diskria.achievetodo.AchieveToDoMod
import io.github.diskria.achievetodo.qol.AdvancementsFogManager
import io.github.diskria.achievetodo.qol.SearchingAdvancementsScreen
import net.minecraft.advancements.Advancement
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.advancements.AdvancementNode
import net.minecraft.advancements.DisplayInfo
import net.minecraft.client.gui.screens.advancements.AdvancementTab
import net.minecraft.client.gui.screens.advancements.AdvancementWidget
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStackTemplate
import kotlin.jvm.optionals.getOrNull

val AdvancementNode.isDisplayable: Boolean
    get() = advancement().display.isPresent

val AdvancementHolder.isFogHint: Boolean
    get() = id.namespace == AdvancementsFogManager.Client.HINT_NAMESPACE

val AdvancementHolder.isSearchResultsRoot: Boolean
    get() = id == SearchingAdvancementsScreen.ROOT_ADVANCEMENT_ID

val AdvancementHolder.isAchieveToDoAbility: Boolean
    get() = id.namespace == AchieveToDoMod.ID

val AdvancementNode.isVisible: Boolean get() = getVisibleDisplay() != null

fun AdvancementNode.getVisibleDisplay(): DisplayInfo? =
    advancement().display.getOrNull()

fun AdvancementNode.hasAnyDone(isDone: (AdvancementNode) -> Boolean): Boolean =
    isDone(this) || children().any { it.hasAnyDone(isDone) }

fun AdvancementNode.countAdvancements(): Int = 1 + children().sumOf { it.countAdvancements() }

val ItemStackTemplate.rawHoverName: String?
    get() = get(DataComponents.CUSTOM_NAME)?.string
        ?: get(DataComponents.WRITTEN_BOOK_CONTENT)?.title?.raw?.takeIf { it.isNotBlank() }
        ?: get(DataComponents.ITEM_NAME)?.string

fun buildTechnicalAdvancement(id: Identifier, block: Advancement.Builder.() -> Unit): AdvancementHolder =
    Advancement.Builder.recipeAdvancement().apply(block).build(id)

fun AdvancementTab.addWidget(node: AdvancementNode, block: AdvancementWidget.() -> Unit = {}): AdvancementWidget {
    addAdvancement(node)
    return requireNotNull(getWidget(node.holder())).apply(block)
}

fun AdvancementNode.child(holder: AdvancementHolder, block: AdvancementNode.() -> Unit = {}): AdvancementNode =
    AdvancementNode(holder, this).apply(block)
