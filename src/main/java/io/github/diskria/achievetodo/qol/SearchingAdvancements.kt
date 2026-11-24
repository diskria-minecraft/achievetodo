package io.github.diskria.achievetodo.qol

import com.llamalad7.mixinextras.injector.ModifyExpressionValue
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.llamalad7.mixinextras.sugar.Local
import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.platform.cursor.CursorTypes
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import io.github.diskria.achievetodo.extensions.*
import io.github.diskria.achievetodo.qol.AdvancementsGuiConstants.Widget
import io.github.diskria.achievetodo.qol.AdvancementsGuiConstants.Window
import io.github.diskria.achievetodo.utils.*
import io.github.diskria.lapis.annotations.*
import net.minecraft.advancements.*
import net.minecraft.client.Minecraft
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
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.CommonColors
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Items
import org.objectweb.asm.Opcodes
import org.spongepowered.asm.mixin.injection.At
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE
import kotlin.jvm.optionals.getOrNull

@KMixin<AdvancementsScreen>(Side.Client)
abstract class SearchingAdvancementsScreen(@Origin private val screen: AdvancementsScreen) {

    private val searchResultsRoot by lazy {
        val holder = buildTechnicalAdvancement(ROOT_ADVANCEMENT_ID) {
            rootDisplay(
                /* icon = */ Items.STONE,
                /* title = */ SEARCH_TAB_TITLE,
                /* description = */ Component.empty(),
                /* background = */ Identifier.withDefaultNamespace("gui/advancements/backgrounds/stone"),
                /* frame = */ AdvancementType.TASK,
                /* showToast = */ false,
                /* announceChat = */ false,
                /* hidden = */ true,
            )
        }
        AdvancementNode(holder, null)
    }

    private val searchResultsTab by lazy {
        requireNotNull(AdvancementTab.create(Minecraft.getInstance(), screen, 0, searchResultsRoot))
    }

    private val searchResults = mutableListOf<SearchResult>()
    private val searchTextCache = HashMap<DisplayInfo, String>()

    private var searchBox: EditBox? = null
    private var isSearchActive = false

    private var resultsColumns = 0
    private var resultsOriginX = 0

    private var hoveredResultWidget: AdvancementWidget? = null
    private var clickedResultWidget: AdvancementWidget? = null

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
    fun onSearchResultHovered(widget: AdvancementWidget?) {
        hoveredResultWidget = widget
    }

    @Extension
    fun initSearchFocus() {
        searchBox?.let { screen.setInitialFocus(it) }
    }

    @Extension
    fun feedCharToSearch(event: CharacterEvent): Boolean {
        val searchBox = searchBox ?: return false
        val oldText = searchBox.value
        val processed = searchBox.charTyped(event)
        if (processed && oldText != searchBox.value) {
            processSearch()
        }
        return processed
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
            /* narration = */ SEARCH_TAB_TITLE,
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
        @Local(argsOnly = true, name = ["a"]) alpha: Float,
    )
    fun AdvancementsScreen.drawSearchBox(
        screen: AdvancementsScreen,
        graphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        op: Op<Unit>
    ) {
        val columnsCount = treeWidth / Widget.FRAME_ITEM_MAX_SIZE
        val rowWidth = Widget.FRAME_ITEM_MAX_SIZE * columnsCount
        val hPad = treeWidth - rowWidth - Widget.FRAME_X_OFFSET
        val originX = hPad / 2
        if (resultsColumns != columnsCount || resultsOriginX != originX) {
            resultsColumns = columnsCount
            resultsOriginX = originX
            if (isSearchActive) {
                refreshSearchResults()
            }
        }
        if (hoveredResultWidget != null) {
            graphics.requestCursor(CursorTypes.POINTING_HAND)
        }
        val boxSpriteX = leftPos +
                Window.TREE_HORIZONTAL_MARGIN + treeWidth + Window.TREE_BORDER_SIZE -
                SearchTexture.BoxPart.WIDTH
        val boxSpriteY = topPos + Window.BEVEL_SIZE + (Window.HEADER_HEIGHT - SearchTexture.BoxPart.HEIGHT) / 2
        graphics.blit(
            /* renderPipeline = */ RenderPipelines.GUI_TEXTURED,
            /* texture = */ SearchTexture.ID,
            /* x = */ boxSpriteX,
            /* y = */ boxSpriteY,
            /* u = */ SearchTexture.BoxPart.U,
            /* v = */ SearchTexture.BoxPart.V,
            /* width = */ SearchTexture.BoxPart.WIDTH,
            /* height = */ SearchTexture.BoxPart.HEIGHT,
            /* textureWidth = */ SearchTexture.WIDTH,
            /* textureHeight = */ SearchTexture.HEIGHT,
        )
        searchBox?.apply {
            x = boxSpriteX + SEARCH_BOX_TEXT_PADDING
            y = boxSpriteY + SEARCH_BOX_TEXT_PADDING
            extractRenderState(graphics, mouseX, mouseY, alpha)
        }
        op(screen, graphics, mouseX, mouseY)
    }

    @WrapMethod(method = ["mouseClicked"])
    fun AdvancementsScreen.onResultClick(event: MouseButtonEvent, isDouble: Boolean, op: Op<Boolean>): Boolean {
        val searchBox = searchBox
        if (searchBox?.mouseClicked(event, isDouble) == true) {
            isSearchActive = searchBox.value.isNotEmpty()
            if (isSearchActive) stopFlashing()
            return true
        }
        if (hoveredResultWidget != null && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            clickedResultWidget = hoveredResultWidget
        }
        return op(event, isDouble)
    }

    @WrapMethod(method = ["mouseScrolled"])
    fun resetClickedResultOnScroll(x: Double, y: Double, scrollX: Double, scrollY: Double, op: Op<Boolean>): Boolean {
        clickedResultWidget = null
        return op(x, y, scrollX, scrollY)
    }

    @WrapMethod(method = ["mouseDragged"])
    fun resetClickedResultOnDrag(event: MouseButtonEvent, dx: Double, dy: Double, op: Op<Boolean>): Boolean {
        clickedResultWidget = null
        return op(event, dx, dy)
    }

    @WrapMethod(method = ["mouseReleased"])
    fun AdvancementsScreen.jumpToClickedResult(event: MouseButtonEvent, op: Op<Boolean>): Boolean {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return op(event)
        val hovered = hoveredResultWidget ?: return op(event)
        val clicked = clickedResultWidget ?: return op(event)
        if (clicked.advancement.id != hovered.advancement.id) return op(event)
        if (startFlashing(clicked.advancement.id, AdvancementFlashingMode.FRAME)) {
            isSearchActive = false
            hoveredResultWidget = null
            clickedResultWidget = null
            searchResultsTab.resetHovered()
        }
        return op(event)
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
        val right = left + treeWidth + Window.TREE_BORDER_SIZE * 2 - SearchTexture.BoxPart.WIDTH - searchSeparatorWidth
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
        if (isSearchActive) searchResultsTab else original

    @WrapMethod(method = ["keyPressed"])
    fun feedKeyToSearch(event: KeyEvent, op: Operation<Boolean>): Boolean {
        val searchBox = searchBox ?: return op(event)
        val oldText = searchBox.value
        val processed = searchBox.keyPressed(event)
        if (processed && oldText != searchBox.value) {
            processSearch()
        }
        if (processed || searchBox.capturesInput() && !event.isEscape) {
            return true
        }
        return op(event)
    }

    private fun processSearch() {
        val searchBox = searchBox ?: return
        val searchTerm = searchBox.value.lowercase()
        isSearchActive = searchTerm.isNotEmpty()
        if (isSearchActive) screen.stopFlashing()
        searchResults.clear()
        if (!isSearchActive) return
        searchResults.addAll(
            advancements.nodesWithProgress().mapNotNull { (node, progress) ->
                val holder = node.holder()
                if (holder.isAchieveToDoAbility) return@mapNotNull null
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
        val cachedText = searchTextCache.getOrPut(this) {
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
        searchResultsTab.resetContents()
        if (searchResults.isEmpty()) return
        searchResultsTab.setScroll(resultsOriginX.toDouble(), Window.TREE_PADDING.toDouble())
        searchResultsTab.markCentered()
        var parentNode = searchResultsRoot
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
            searchResultsTab.addWidget(childNode) { setProgress(result.progress) }
            if (x == resultsColumns - 1) {
                parentNode = searchResultsRoot
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
        val ROOT_ADVANCEMENT_ID = Identifier.fromNamespaceAndPath("advancements_search", "advancements_search/root")
        private val SEARCH_TAB_TITLE = Component.translatable("gui.recipebook.search_hint")

        private val SEARCH_BOX_TEXT_PADDING = run {
            val bevel = 1
            val inner = 1
            bevel + inner
        }

        private val TYPE_ORDER = listOf(AdvancementType.TASK, AdvancementType.GOAL, AdvancementType.CHALLENGE)
    }
}

@KMixin<AdvancementTab>(Side.Client)
abstract class SearchingAdvancementTab(@Origin val tab: AdvancementTab) {

    private val isSearchResults get() = tab.rootAdvancement.isSearchResultsRoot

    @WrapMethod(method = ["tick"])
    fun AdvancementTab.onWidgetHovered(relativeMouseX: Int, relativeMouseY: Int, op: Op<Unit>) {
        op(relativeMouseX, relativeMouseY)
        screen.onSearchResultHovered(if (isSearchResults) hovered else null)
    }

    @WrapWithCondition(
        method = ["extractContents"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"
        )]
    )
    fun hideSearchResultsBackground(
        graphics: GuiGraphicsExtractor,
        renderPipeline: RenderPipeline, texture: Identifier,
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
        originX: Int, originY: Int,
        isBorderPass: Boolean,
    ) = !isSearchResults

    @KShadow(PRIVATE)
    abstract val hovered: AdvancementWidget?
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
    }
}
