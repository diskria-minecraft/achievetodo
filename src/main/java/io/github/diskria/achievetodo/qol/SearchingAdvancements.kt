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
import io.github.diskria.achievetodo.utils.addWidget
import io.github.diskria.achievetodo.utils.clearChildren
import io.github.diskria.achievetodo.utils.getWidget
import io.github.diskria.achievetodo.utils.setInitialFocus
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

    @Extension
    var searchResultsOriginX: Int = 0

    @Extension
    var hoveredSearchResultWidget: AdvancementWidget? = null

    private val searchResultsRootNode by lazy {
        val rootHolder = buildTechnicalAdvancement(ROOT_ADVANCEMENT_ID) {
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
        AdvancementNode(rootHolder, null)
    }

    private val searchResultsTab by lazy {
        requireNotNull(AdvancementTab.create(Minecraft.getInstance(), screen, 0, searchResultsRootNode))
    }

    private val searchResults = mutableListOf<SearchResult>()

    private var searchBox: EditBox? = null
    private var searchResultsMaxColumns = 0
    private var isSearchActive = false
    private var clickedSearchResultWidget: AdvancementWidget? = null

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
        @Local(name = ["a"], argsOnly = true) alpha: Float,
    )
    fun AdvancementsScreen.drawSearchBox(
        screen: AdvancementsScreen,
        graphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        op: Op<Unit>
    ) {
        val maxItemSize = 28
        val columnsCount = treeWidth / maxItemSize
        val rowWidth = maxItemSize * columnsCount
        val hPad = treeWidth - rowWidth - Widget.FRAME_X_OFFSET
        val originX = hPad / 2
        if (searchResultsMaxColumns != columnsCount || searchResultsOriginX != originX) {
            searchResultsMaxColumns = columnsCount
            searchResultsOriginX = originX
            if (isSearchActive) {
                refreshSearchResults()
            }
        }
        val boxSpriteX = leftPos +
                Window.TREE_HORIZONTAL_MARGIN + treeWidth + Window.TREE_BORDER_SIZE - SearchTexture.BoxPart.WIDTH
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
        if (hoveredSearchResultWidget != null && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            clickedSearchResultWidget = hoveredSearchResultWidget
        }
        return op(event, isDouble)
    }

    @WrapMethod(method = ["mouseScrolled"])
    fun resetClickedResultOnScroll(x: Double, y: Double, scrollX: Double, scrollY: Double, op: Op<Boolean>): Boolean {
        clickedSearchResultWidget = null
        return op(x, y, scrollX, scrollY)
    }

    @WrapMethod(method = ["mouseDragged"])
    fun resetClickedResultOnDrag(event: MouseButtonEvent, dx: Double, dy: Double, op: Op<Boolean>): Boolean {
        clickedSearchResultWidget = null
        return op(event, dx, dy)
    }

    @WrapMethod(method = ["mouseReleased"])
    fun AdvancementsScreen.jumpToClickedResult(event: MouseButtonEvent, op: Op<Boolean>): Boolean {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return op(event)
        val hovered = hoveredSearchResultWidget ?: return op(event)
        val clicked = clickedSearchResultWidget ?: return op(event)
        if (clicked.advancement.id != hovered.advancement.id) return op(event)
        val targetTab = advancements.tree().get(clicked.advancement.id)?.root() ?: return op(event)
        isSearchActive = false
        hoveredSearchResultWidget = null
        clickedSearchResultWidget = null
        searchResultsTab.resetHovered()
        advancements.setSelectedTab(targetTab.holder(), true)
        selectedTab?.getWidget(clicked.advancement.id)?.let { startFlashing(it) }
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
        op: Op<Unit>,
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
        val searchTerm = searchBox.value
        isSearchActive = searchTerm.isNotEmpty()
        searchResults.clear()
        if (!isSearchActive) return
        searchResults.addAll(
            advancements.progress().entries.asSequence().mapNotNull { (holder, progress) ->
                val advancement = holder.value
                if (holder.isAchieveToDoAbility) return@mapNotNull null
                val display = advancement.display.getOrNull() ?: return@mapNotNull null
                if (display.hidden && !progress.isDone) return@mapNotNull null

                val matches = display.title.string.contains(searchTerm, ignoreCase = true) ||
                        display.description.string.contains(searchTerm, ignoreCase = true) ||
                        display.icon.rawHoverName?.contains(searchTerm, ignoreCase = true) == true
                if (!matches) return@mapNotNull null

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

    private fun refreshSearchResults() {
        searchResultsTab.resetSearchResults()
        if (searchResults.isEmpty()) return
        var parentNode = searchResultsRootNode
        var col = 0
        var row = 0
        searchResults.forEach { result ->
            val originalAdvancement = result.holder.value
            val childHolder = buildTechnicalAdvancement(result.holder.id) {
                parent(parentNode.holder())
                display(result.display)
                rewards(originalAdvancement.rewards)
                originalAdvancement.criteria.forEach { (name, criterion) -> addCriterion(name, criterion) }
                requirements(originalAdvancement.requirements)
            }
            val childNode = parentNode.child(childHolder) { setLocation(col.toFloat(), row.toFloat()) }
            searchResultsTab.addWidget(childNode) { setProgress(result.progress) }
            if (col == searchResultsMaxColumns - 1) {
                parentNode = searchResultsRootNode
                col = 0
                row++
            } else {
                parentNode = childNode
                col++
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

    @Extension
    fun resetSearchResults() {
        widgets.clear()
        widgets[root.advancement] = root.apply { clearChildren() }
        resetHovered()
        minX = Int.MAX_VALUE
        minY = Int.MAX_VALUE
        maxX = Int.MIN_VALUE
        maxY = Int.MIN_VALUE
        scrollX = tab.screen.searchResultsOriginX.toDouble()
        scrollY = 0.toDouble()
        centered = true
    }

    @Extension
    fun resetHovered() {
        hovered = null
        fade = 0.0f
    }

    @WrapMethod(method = ["tick"])
    fun AdvancementTab.saveHoveredSearchResult(relativeMouseX: Int, relativeMouseY: Int, op: Op<Unit>) {
        op(relativeMouseX, relativeMouseY)
        screen.hoveredSearchResultWidget = if (isSearchResults) hovered else null
    }

    @WrapMethod(method = ["extractContents"])
    fun AdvancementTab.markHoveredSearchResultAsClickable(
        graphics: GuiGraphicsExtractor,
        windowLeft: Int,
        windowTop: Int,
        op: Op<Unit>
    ) {
        if (screen.hoveredSearchResultWidget != null) {
            graphics.requestCursor(CursorTypes.POINTING_HAND)
        }
        op(graphics, windowLeft, windowTop)
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

    @KShadow(PRIVATE, FINAL)
    abstract val root: AdvancementWidget

    @KShadow(PRIVATE, FINAL)
    abstract val widgets: MutableMap<AdvancementHolder, AdvancementWidget>

    @KShadow(PRIVATE)
    abstract var scrollX: Double

    @KShadow(PRIVATE)
    abstract var scrollY: Double

    @KShadow(PRIVATE)
    abstract var minX: Int

    @KShadow(PRIVATE)
    abstract var minY: Int

    @KShadow(PRIVATE)
    abstract var maxX: Int

    @KShadow(PRIVATE)
    abstract var maxY: Int

    @KShadow(PRIVATE)
    abstract var fade: Float

    @KShadow(PRIVATE)
    abstract var centered: Boolean

    @KShadow(PRIVATE)
    abstract var hovered: AdvancementWidget?
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
