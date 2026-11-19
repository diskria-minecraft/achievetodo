package io.github.diskria.achievetodo.suite

import com.llamalad7.mixinextras.injector.ModifyReturnValue
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.llamalad7.mixinextras.sugar.Local
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import io.github.diskria.achievetodo.AchieveToDoMod
import io.github.diskria.achievetodo.extensions.Op
import io.github.diskria.achievetodo.extensions.calcSha256OrNull
import io.github.diskria.achievetodo.extensions.extractNestedZip
import io.github.diskria.achievetodo.extensions.invoke
import io.github.diskria.lapis.annotations.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Button.OnPress
import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.client.gui.screens.ConfirmLinkScreen
import net.minecraft.client.gui.screens.packs.PackSelectionModel
import net.minecraft.client.gui.screens.packs.PackSelectionScreen
import net.minecraft.client.gui.screens.packs.TransferableSelectionList
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.repository.Pack
import net.minecraft.server.packs.repository.PackCompatibility
import net.minecraft.server.packs.repository.PackRepository
import net.minecraft.server.packs.repository.RepositorySource
import net.minecraft.util.Util
import org.objectweb.asm.Opcodes
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Coerce
import java.net.URI
import java.util.function.Consumer
import java.util.function.Function
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.moveTo

object SuiteInstallers {

    fun openScreen(components: List<SuiteComponent>, callback: (result: Boolean) -> Unit) {
        Minecraft.getInstance().gui.setScreen(SuiteInstallerScreen.create(components, callback))
    }

    fun sendDedicatedServerReport(components: List<SuiteComponent>) {
        val curl = if (Util.getPlatform() == Util.OS.WINDOWS) "curl.exe" else "curl"
        AchieveToDoMod.logger.error(buildString {
            appendLine("Missing or corrupted required data packs!")
            appendLine()
            components.forEachIndexed { index, component ->
                appendLine(
                    "${index + 1}. [${component.name}] -> ${component.fileName} (SHA-256:${component.download.sha256})"
                )
                when (val download = component.download) {
                    is SuiteComponent.Download.Modrinth -> {
                        appendLine("Run in console to download with correct name:")

                        val path = "${SuitePack.PATH}/${component.fileName}"
                        appendLine(
                            """
                            $curl -A "${AchieveToDoMod.USER_AGENT}" --create-dirs -Lo "$path" "${download.fileUrl}"
                            """.trimIndent()
                        )
                    }

                    is SuiteComponent.Download.Mediafire -> {
                        appendLine("Open browser and download file (AD): ${download.pageUrl}")
                        appendLine("Unzip it and rename the inner file to: ${component.fileName}")
                    }
                }
                appendLine()
            }
            appendLine("All files must be saved to: <server_root>/${SuitePack.PATH}")
            append("Restart the server once all files are in place.")
        })
    }
}

@KMixin<Pack>(Side.Common)
class SuiteInstallerPackCustomizer {

    @Extension
    var isMissingSuiteComponent: Boolean = false

    @Extension
    var isInstalledSuiteComponent: Boolean = false

    @Extension
    var suiteComponent: SuiteComponent? = null

    @ModifyReturnValue(method = ["getCompatibility"], at = [At("RETURN")])
    fun overrideCompatibility(original: PackCompatibility) =
        if (isInstalledSuiteComponent) PackCompatibility.COMPATIBLE
        else original
}

@KMixin<PackSelectionScreen>(Side.Client)
class SuiteInstallerScreen(@Origin val screen: PackSelectionScreen) {

    @Extension
    var isSuiteInstallerScreen: Boolean = false

    @Extension
    var suiteComponents: List<SuiteComponent> = emptyList()

    @Extension
    var onDone: () -> Unit = {}

    private var doneButton: Button? = null

    @WrapOperation(
        method = ["<init>"],
        at = [At(
            "NEW",
            target = "(Ljava/util/function/Consumer;Ljava/util/function/Function;Lnet/minecraft/server/packs/repository/PackRepository;Ljava/util/function/Consumer;)Lnet/minecraft/client/gui/screens/packs/PackSelectionModel;",
        )]
    )
    fun setModelDownloadCallback(
        onListChanged: Consumer<*>,
        iconGetter: Function<*, *>,
        repository: PackRepository,
        output: Consumer<*>,
        op: Op<PackSelectionModel>
    ) = op(onListChanged, iconGetter, repository, output).apply {
        onInstallClicked = { component ->
            ConfirmLinkScreen.confirmLinkNow(screen, URI.create(component.download.pageUrl))
        }
    }

    @WrapOperation(
        method = ["init"],
        at = [At(
            "NEW",
            target = "(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/packs/PackSelectionScreen;IILnet/minecraft/network/chat/Component;)Lnet/minecraft/client/gui/screens/packs/TransferableSelectionList;",
            ordinal = 0
        )]
    )
    fun setMissingColumnTitle(
        minecraft: Minecraft,
        screen: PackSelectionScreen,
        width: Int,
        height: Int,
        title: Component,
        op: Op<TransferableSelectionList>
    ) = op(minecraft, screen, width, height, if (isSuiteInstallerScreen) MISSING_COLUMN_TITLE else title)

    @WrapOperation(
        method = ["init"],
        at = [At(
            "NEW",
            target = "(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/packs/PackSelectionScreen;IILnet/minecraft/network/chat/Component;)Lnet/minecraft/client/gui/screens/packs/TransferableSelectionList;",
            ordinal = 1
        )]
    )
    fun setInstalledColumnTitle(
        minecraft: Minecraft,
        screen: PackSelectionScreen,
        width: Int,
        height: Int,
        title: Component,
        op: Op<TransferableSelectionList>
    ) = op(minecraft, screen, width, height, if (isSuiteInstallerScreen) INSTALLED_COLUMN_TITLE else title)

    @WrapOperation(
        method = ["init"],
        at = [At(
            value = "INVOKE",
            target = $$"Lnet/minecraft/client/gui/components/Button;builder(Lnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/components/Button$OnPress;)Lnet/minecraft/client/gui/components/Button$Builder;",
            ordinal = 0
        )]
    )
    context(
        @Local(name = ["footer"]) footer: LinearLayout,
    )
    fun insertDoneAndMakeFolderSmaller(message: Component, onPress: OnPress, op: Op<Button.Builder>): Button.Builder {
        val folderButton = op(message, onPress)
        if (isSuiteInstallerScreen) {
            doneButton = footer.addChild(Button.builder(CommonComponents.GUI_DONE) { onDone() }.width(100).build())
            folderButton.width(100)
        }
        return folderButton
    }

    @WrapOperation(
        method = ["init"],
        at = [At(
            value = "INVOKE",
            target = $$"Lnet/minecraft/client/gui/components/Button;builder(Lnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/components/Button$OnPress;)Lnet/minecraft/client/gui/components/Button$Builder;",
            ordinal = 1
        )]
    )
    fun renameDoneToBackAndMakeSmaller(message: Component, onPress: OnPress, op: Op<Button.Builder>) = when {
        isSuiteInstallerScreen -> op(CommonComponents.GUI_BACK, onPress).width(100)
        else -> op(message, onPress)
    }

    @WrapWithCondition(
        method = ["populateLists"],
        at = [At("FIELD", target = "Lnet/minecraft/client/gui/components/Button;active:Z", opcode = Opcodes.PUTFIELD)]
    )
    fun keepBackButtonActive(button: Button, active: Boolean): Boolean = !isSuiteInstallerScreen

    @WrapMethod(method = ["populateLists"])
    fun updateDoneButtonActive(transferredEntry: PackSelectionModel.EntryBase?, op: Op<Unit>) {
        op(transferredEntry)
        doneButton?.active = SuitePack.isInstalled(suiteComponents)
    }

    class ComponentsPackRepository(components: List<SuiteComponent>) : PackRepository(Source(components)) {

        val orderedPackIds = components.map { it.packId }

        class Source(private val components: List<SuiteComponent>) : RepositorySource {

            override fun loadPacks(result: Consumer<Pack>) {
                SuitePack.dir.createDirectories()
                val zips = SuitePack.dir.listDirectoryEntries("*.zip")
                    .mapNotNull { path -> path.calcSha256OrNull()?.let { it to path } }
                    .toMap()
                    .toMutableMap()
                components.forEach { component ->
                    val targetPath = component.path
                    (component.download as? SuiteComponent.Download.Mediafire)?.let { mediafireDownload ->
                        val unzipMePath = zips[mediafireDownload.unzipMeSha256]
                        if (unzipMePath != null) {
                            if (zips[mediafireDownload.sha256] == targetPath) {
                                unzipMePath.deleteIfExists()
                                zips.remove(mediafireDownload.unzipMeSha256)
                            } else if (extractNestedZip(unzipMePath, targetPath)) {
                                unzipMePath.deleteIfExists()
                                zips.remove(mediafireDownload.unzipMeSha256)
                                targetPath.calcSha256OrNull()?.let { zips[it] = targetPath }
                            }
                        }
                    }
                    val path = zips[component.download.sha256]
                    if (path == null) {
                        result.accept(component.missingPack)
                        return@forEach
                    }
                    if (path != targetPath) {
                        path.moveTo(targetPath, overwrite = true)
                    }
                    result.accept(component.installedPack() ?: component.missingPack)
                }
            }
        }
    }

    companion object {
        private val MISSING_COLUMN_TITLE = Component.translatable("recover_world.issue.missing_file")
        private val INSTALLED_COLUMN_TITLE = Component.translatable("mco.brokenworld.downloaded")

        fun create(components: List<SuiteComponent>, callback: (result: Boolean) -> Unit): PackSelectionScreen =
            PackSelectionScreen(
                ComponentsPackRepository(components).apply { reload() },
                { callback(false) },
                SuitePack.dir,
                SuitePack.coloredName,
            ).apply {
                isSuiteInstallerScreen = true
                suiteComponents = components
                onDone = { callback(true) }
            }
    }
}

@KMixin<PackSelectionModel>(Side.Client)
abstract class SuiteInstallerPackColumns {

    @Extension
    var onInstallClicked: (component: SuiteComponent) -> Unit = {}

    @WrapMethod(method = ["findNewPacks"])
    fun regroupComponentPacks(op: Op<Unit>) {
        op()
        val componentsPackRepository = repository as? SuiteInstallerScreen.ComponentsPackRepository ?: return
        val newlyMissing = installedPacks.filter { it.isMissingSuiteComponent }
        if (newlyMissing.isNotEmpty()) {
            installedPacks -= newlyMissing.toSet()
            missingPacks += newlyMissing
        }
        val newlyInstalled = missingPacks.filter { it.isInstalledSuiteComponent }
        if (newlyInstalled.isNotEmpty()) {
            missingPacks -= newlyInstalled.toSet()
            installedPacks += newlyInstalled
        }
        missingPacks.sortBy { componentsPackRepository.orderedPackIds.indexOf(it.id) }
        installedPacks.sortByDescending { componentsPackRepository.orderedPackIds.indexOf(it.id) }
    }

    @KShadow(PRIVATE, FINAL)
    abstract val repository: PackRepository

    @MappingName("unselected")
    @KShadow(PRIVATE, FINAL)
    abstract val missingPacks: MutableList<Pack>

    @MappingName("selected")
    @KShadow(PRIVATE, FINAL)
    abstract val installedPacks: MutableList<Pack>
}

@KMixin<PackSelectionModel.EntryBase>(Side.Client)
abstract class SuiteInstallerPackEntryBase {

    @Extension
    val isMissingSuiteComponent: Boolean get() = pack.isMissingSuiteComponent

    @WrapMethod(method = ["toggleSelection"])
    fun interceptToggleSelection(op: Op<Unit>) {
        val suiteComponent = pack.suiteComponent ?: return op()
        model.onInstallClicked(suiteComponent)
    }

    @KShadow(PRIVATE, FINAL)
    abstract val pack: Pack

    @MappingName("this$0")
    @KShadow(FINAL)
    abstract var model: PackSelectionModel
}

@KMixin<TransferableSelectionList.PackEntry>(Side.Client)
abstract class SuiteInstallerPackEntry(@Origin val entry: TransferableSelectionList.PackEntry) {

    private val isMissingSuiteComponent: Boolean
        get() = (pack as? PackSelectionModel.EntryBase)?.isMissingSuiteComponent == true

    @WrapWithCondition(
        method = ["extractContent"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V",
            ordinal = 0
        )]
    )
    fun hideIcon(
        graphics: GuiGraphicsExtractor,
        renderPipeline: RenderPipeline,
        texture: Identifier,
        x: Int, y: Int, u: Float, v: Float,
        width: Int, height: Int,
        textureWidth: Int, textureHeight: Int,
    ): Boolean = !isMissingSuiteComponent

    @WrapWithCondition(
        method = ["extractContent"],
        at = [At("INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V")]
    )
    fun hideHoveredIconBackground(graphics: GuiGraphicsExtractor, x0: Int, y0: Int, x1: Int, y1: Int, col: Int) =
        !isMissingSuiteComponent

    @WrapOperation(
        method = ["extractContent"],
        at = [At(
            value = "INVOKE",
            target = $$"Lnet/minecraft/client/gui/screens/packs/TransferableSelectionList;getSelected()Lnet/minecraft/client/gui/components/AbstractSelectionList$Entry;"
        )]
    )
    @Coerce // AbstractSelectionList.Entry
    fun forceSelected(list: TransferableSelectionList, op: Op<Any?>) = if (isMissingSuiteComponent) entry else op(list)

    @WrapOperation(
        method = ["extractContent"],
        at = [At("INVOKE", target = "Lnet/minecraft/client/gui/screens/packs/TransferableSelectionList;isFocused()Z")]
    )
    fun forceFocused(list: TransferableSelectionList, op: Op<Boolean>) = isMissingSuiteComponent || op(list)

    @KShadow(PRIVATE, FINAL)
    abstract val pack: PackSelectionModel.Entry
}
