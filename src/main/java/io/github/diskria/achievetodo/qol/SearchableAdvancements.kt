package io.github.diskria.achievetodo.qol

import com.llamalad7.mixinextras.injector.ModifyExpressionValue
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.llamalad7.mixinextras.sugar.Local
import com.mojang.blaze3d.platform.InputConstants
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import io.github.diskria.achievetodo.AchieveToDoMod
import io.github.diskria.achievetodo.extensions.*
import io.github.diskria.achievetodo.qol.AdvancementsGuiConstants.Tree
import io.github.diskria.achievetodo.qol.AdvancementsGuiConstants.Widget
import io.github.diskria.achievetodo.qol.AdvancementsGuiConstants.Window
import io.github.diskria.achievetodo.qol.SearchableAdvancementsScreen.Companion.ROOT_ADVANCEMENT_ID
import io.github.diskria.achievetodo.utils.*
import io.github.diskria.lapis.annotations.*
import net.minecraft.advancements.*
import net.minecraft.client.data.models.model.TextureMapping
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.advancements.AdvancementTab
import net.minecraft.client.gui.screens.advancements.AdvancementWidget
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.PreeditEvent
import net.minecraft.client.multiplayer.ClientAdvancements
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.core.component.DataComponents
import net.minecraft.locale.Language
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.CommonColors
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import org.objectweb.asm.Opcodes
import org.spongepowered.asm.mixin.injection.At
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE
import kotlin.jvm.optionals.getOrNull

private val SEARCH_TITLE = Component.translatable("gui.recipebook.search_hint")

object SearchResultsGridTab {

    val rootNode by lazy {
        val holder = buildTechnicalAdvancement(ROOT_ADVANCEMENT_ID) {
            rootDisplay(
                /* icon = */ Items.STONE,
                /* title = */ SEARCH_TITLE,
                /* description = */ Component.empty(),
                /* background = */ TextureMapping.getBlockTexture(Blocks.STONE).sprite(),
                /* frame = */ AdvancementType.TASK,
                /* showToast = */ false,
                /* announceChat = */ false,
                /* hidden = */ true,
            )
        }
        AdvancementNode(holder, null)
    }
}

@KMixin<AdvancementsScreen>(Side.Client)
abstract class SearchableAdvancementsScreen(@Origin private val screen: AdvancementsScreen) {

    private val gridResultsTab = requireNotNull(
        AdvancementTab.create(screen.minecraft, screen, 0, SearchResultsGridTab.rootNode)
    )

    private val searchResults = mutableListOf<SearchResult>()
    private val displayTextCache = mutableMapOf<DisplayInfo, String>()

    private var searchBox: EditBox? = null
    private var isSearchActive = false

    private var resultsColumns = 0
    private var resultsOffsetX = 0

    private var lastLanguage = Language.getInstance()

    @Extension
    fun resolveAdvancementClickListener(tab: AdvancementTab, widget: AdvancementWidget): AdvancementClickListener? {
        if (tab.rootAdvancement.isSearchResultsRoot) {
            return {
                if (screen.startFlashing(widget.advancement.id, AdvancementFlashingMode.FRAME)) {
                    isSearchActive = false
                    gridResultsTab.resetHovered()
                }
            }
        }
        return null
    }

    @Extension
    fun repositionSearch(width: Int, height: Int) {
        val searchBox = searchBox ?: return
        val oldText = searchBox.value
        screen.init(width, height)
        searchBox.value = oldText
        if (searchBox.value.isNotEmpty()) {
            refreshSearchResults()
        }
    }

    @Extension
    fun initSearchFocus() {
        searchBox?.let { screen.setInitialFocus(it) }
    }

    @Extension
    fun feedCharToSearch(event: CharacterEvent): Boolean {
        val searchBox = searchBox ?: return false
        val oldText = searchBox.value
        val consumed = searchBox.charTyped(event)
        if (consumed && oldText != searchBox.value) {
            processSearch()
        }
        return consumed
    }

    @Extension
    fun feedPreeditToSearch(event: PreeditEvent?): Boolean = searchBox?.preeditUpdated(event) == true

    @Extension
    fun isSearchInputCaptured(): Boolean = searchBox?.capturesInput() == true

    @WrapMethod(method = ["init"])
    fun attachSearchBox(op: Op<Unit>) {
        op()
        val cursorWidth = 6
        searchBox = EditBox(
            /* font = */ screen.font,
            /* x = */ 0,
            /* y = */ 0,
            /* width = */ SearchTexture.BoxPart.WIDTH - SEARCH_BOX_TEXT_PADDING * 2 - cursorWidth,
            /* height = */ screen.font.lineHeight,
            /* narration = */ SEARCH_TITLE,
        ).apply {
            isBordered = false
            @Suppress("UsePropertyAccessSyntax") setMaxLength(50)
            setTextColor(CommonColors.WHITE)
            setInvertHighlightedTextColor(false)
        }.also {
            screen.addWidget(it)
        }
    }

    @WrapOperation(
        method = ["extractRenderState"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementsScreen;extractTooltips(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"
        )]
    )
    context(
        @Local(argsOnly = true, name = ["a"]) deltaTicks: Float,
    )
    fun AdvancementsScreen.drawSearchBox(
        screen: AdvancementsScreen,
        graphics: GuiGraphicsExtractor,
        mouseX: Int, mouseY: Int,
        op: Op<Unit>
    ) {
        val columnsCount = treeWidth / Widget.FRAME_ITEM_MAX_SIZE
        val rowWidth = Widget.FRAME_ITEM_MAX_SIZE * columnsCount
        val offsetX = (treeWidth - rowWidth - Widget.FRAME_X_OFFSET) / 2
        if (resultsColumns != columnsCount || resultsOffsetX != offsetX) {
            resultsColumns = columnsCount
            resultsOffsetX = offsetX
            refreshSearchResults()
        }
        val boxSpriteX = leftPos + Tree.HORIZONTAL_MARGIN + treeWidth + Tree.BORDER_SIZE - SearchTexture.BoxPart.WIDTH
        val boxSpriteY = topPos + Window.BEVEL_SIZE + (Window.HEADER_HEIGHT - SearchTexture.BoxPart.HEIGHT) / 2
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            SearchTexture.ID,
            boxSpriteX, boxSpriteY,
            SearchTexture.BoxPart.U, SearchTexture.BoxPart.V,
            SearchTexture.BoxPart.WIDTH, SearchTexture.BoxPart.HEIGHT,
            SearchTexture.WIDTH, SearchTexture.HEIGHT,
        )
        searchBox?.apply {
            x = boxSpriteX + SEARCH_BOX_TEXT_PADDING
            y = boxSpriteY + SEARCH_BOX_TEXT_PADDING
            extractRenderState(graphics, mouseX, mouseY, deltaTicks)
        }
        op(screen, graphics, mouseX, mouseY)
    }

    @WrapMethod(method = ["mouseClicked"])
    fun openSearchOnBoxClick(event: MouseButtonEvent, doubleClick: Boolean, op: Op<Boolean>): Boolean {
        val searchBox = searchBox
        if (searchBox?.mouseClicked(event, doubleClick) == true) {
            isSearchActive = searchBox.value.isNotEmpty()
            if (isSearchActive) screen.stopFlashing()
            return true
        }
        return op(event, doubleClick)
    }

    @WrapOperation(
        method = ["mouseClicked"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientAdvancements;setSelectedTab(Lnet/minecraft/advancements/AdvancementHolder;Z)V"
        )]
    )
    fun exitSearchOnTabSwitch(
        clientAdvancements: ClientAdvancements,
        selectedTab: AdvancementHolder,
        tellServer: Boolean,
        op: Op<Unit>
    ) {
        isSearchActive = false
        op(clientAdvancements, selectedTab, tellServer)
    }

    @WrapOperation(
        method = ["extractWindow"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"
        )]
    )
    fun AdvancementsScreen.fitTitle(
        graphics: GuiGraphicsExtractor,
        font: Font,
        text: Component,
        left: Int, top: Int,
        color: Int,
        dropShadow: Boolean,
        op: Op<Unit>
    ) {
        val searchSeparatorWidth = left - leftPos - Window.BEVEL_SIZE
        val right = left + treeWidth + Tree.BORDER_SIZE * 2 - SearchTexture.BoxPart.WIDTH - searchSeparatorWidth
        val bottom = top + font.lineHeight
        graphics.enableScissor(left, top, right, bottom)
        op(graphics, font, text, left, top, color, dropShadow)
        graphics.disableScissor()
    }

    @ModifyExpressionValue(
        method = [
            "extractRenderState",
            "extractInside",
            "extractWindow",
            "extractTooltips",
            "tick",
            "mouseScrolled",
            "mouseDragged",
        ],
        at = [At(
            value = "FIELD",
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementsScreen;selectedTab:Lnet/minecraft/client/gui/screens/advancements/AdvancementTab;",
            opcode = Opcodes.GETFIELD
        )]
    )
    fun replaceCurrentTab(original: AdvancementTab?) =
        if (isSearchActive) gridResultsTab else original

    @WrapMethod(method = ["keyPressed"])
    fun feedKeyToSearch(event: KeyEvent, op: Operation<Boolean>): Boolean {
        val searchBox = searchBox ?: return op(event)
        if (InputConstants.isKeyDown(InputConstants.KEY_F3)) return op(event)
        val oldText = searchBox.value
        val consumed = searchBox.keyPressed(event)
        if (consumed && oldText != searchBox.value) {
            processSearch()
        }
        if (consumed || searchBox.capturesInput() && !event.isEscape) {
            return true
        }
        return op(event)
    }

    @WrapMethod(method = ["tick"])
    fun onLanguageChanged(op: Op<Unit>) {
        op()
        val currentLanguage = Language.getInstance()
        if (lastLanguage !== currentLanguage) {
            displayTextCache.clear()
            processSearch(background = true)
            lastLanguage = currentLanguage
        }
    }

    private fun processSearch(background: Boolean = false) {
        val searchBox = searchBox ?: return
        val searchTerm = searchBox.value.lowercase()
        if (!background) {
            isSearchActive = searchTerm.isNotEmpty()
            if (isSearchActive) screen.stopFlashing()
        }
        searchResults.clear()
        if (searchTerm.isEmpty()) return
        searchResults.addAll(
            advancements.nodesWithProgress().mapNotNull { (node, progress) ->
                val holder = node.holder()
                if (holder.isAbility) return@mapNotNull null
                val display = holder.value.display.getOrNull() ?: return@mapNotNull null
                if (display.hidden && !progress.isDone) return@mapNotNull null
                if (!display.matches(searchTerm)) return@mapNotNull null
                SearchResult(holder, display, progress)
            }.sortedWith(
                compareBy<SearchResult> { it.progress.isDone }
                    .thenByDescending { (it.progress.percent * 10).toInt() }
                    .thenBy { result ->
                        if (result.holder.value.isRoot) Int.MIN_VALUE
                        else TYPE_ORDER.indexOf(result.display.type).takeIf { it != -1 } ?: Int.MAX_VALUE
                    }
                    .thenBy { it.holder.id }
            )
        )
        refreshSearchResults()
    }

    private fun DisplayInfo.matches(searchTerm: String): Boolean {
        val cachedText = displayTextCache.getOrPut(this) {
            listOfNotNull(
                title.string,
                description.string,
                icon[DataComponents.ITEM_NAME]?.string,
                icon[DataComponents.CUSTOM_NAME]?.string,
                icon[DataComponents.WRITTEN_BOOK_CONTENT]?.title?.raw,
            ).joinToString("\n")
        }
        return cachedText.contains(searchTerm, ignoreCase = true)
    }

    private fun refreshSearchResults() {
        gridResultsTab.resetContents()
        if (searchResults.isEmpty()) return
        gridResultsTab.setScroll(resultsOffsetX.toDouble(), Tree.PADDING.toDouble())
        gridResultsTab.markCentered()
        var parentNode = SearchResultsGridTab.rootNode
        var x = 0
        var y = 0
        searchResults.forEach { result ->
            val originalAdvancement = result.holder.value
            val childHolder = buildTechnicalAdvancement(result.holder.id) {
                parent(parentNode.holder())
                display(result.display)
                rewards(originalAdvancement.rewards)
                originalAdvancement.criteria.forEach { (name, criterion) -> addCriterion(name, criterion) }
                requirements(originalAdvancement.requirements)
            }
            val childNode = parentNode.child(childHolder) { setLocation(x.toFloat(), y.toFloat()) }
            gridResultsTab.addWidget(childNode) { setProgress(result.progress) }
            if (x == resultsColumns - 1) {
                parentNode = SearchResultsGridTab.rootNode
                x = 0
                y++
            } else {
                parentNode = childNode
                x++
            }
        }
    }

    @KShadow(PRIVATE, FINAL)
    abstract val advancements: ClientAdvancements

    @KShadow(PRIVATE)
    abstract val selectedTab: AdvancementTab?

    @KShadow(PRIVATE)
    abstract val leftPos: Int

    @KShadow(PRIVATE)
    abstract val topPos: Int

    private class SearchResult(
        val holder: AdvancementHolder,
        val display: DisplayInfo,
        val progress: AdvancementProgress,
    )

    companion object {
        val ROOT_ADVANCEMENT_ID = AchieveToDoMod.getIdentifier("advancements_search/root")

        private const val SEARCH_BOX_TEXT_PADDING = SearchTexture.BoxPart.BORDER_SIZE + 1

        private val TYPE_ORDER = listOf(AdvancementType.TASK, AdvancementType.GOAL, AdvancementType.CHALLENGE)
    }
}

@KMixin<AdvancementTab>(Side.Client)
abstract class SearchableAdvancementTab(@Origin val tab: AdvancementTab) {

    private val isSearchResults get() = tab.rootAdvancement.isSearchResultsRoot

    @WrapWithCondition(
        method = ["extractContents"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"
        )]
    )
    fun hideSearchResultsBackground(
        graphics: GuiGraphicsExtractor,
        renderPipeline: RenderPipeline,
        texture: Identifier,
        x: Int, y: Int,
        u: Float, v: Float,
        width: Int, height: Int,
        textureWidth: Int, textureHeight: Int,
    ) = !isSearchResults

    @WrapWithCondition(
        method = ["extractContents"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementWidget;extractConnectivity(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIZ)V"
        )]
    )
    fun hideSearchResultsConnectivity(
        widget: AdvancementWidget,
        graphics: GuiGraphicsExtractor,
        treeX: Int, treeY: Int,
        isBorderPass: Boolean,
    ) = !isSearchResults
}

private object SearchTexture {

    val ID = CreativeModeTab.createTextureLocation("item_search")
    const val WIDTH = 256
    const val HEIGHT = 256

    object BoxPart {
        const val U = 80.toFloat()
        const val V = 4.toFloat()
        const val WIDTH = 90
        const val HEIGHT = 12
        const val BORDER_SIZE = 1
    }
}
