package io.github.diskria.achievetodo.config

import com.google.common.hash.HashCode
import com.google.common.hash.Hashing
import com.mojang.blaze3d.Blaze3D
import io.github.diskria.achievetodo.AchieveToDoMod
import io.github.diskria.achievetodo.client.SuitePackComponent
import io.github.diskria.achievetodo.extensions.RealmsUnit
import io.github.diskria.achievetodo.extensions.calcSha256OrNull
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.components.*
import net.minecraft.client.gui.layouts.FrameLayout
import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.util.HttpUtil
import net.minecraft.util.Util
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.zip.ZipFile
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div

@Environment(EnvType.CLIENT)
class SuitePackDownloadComponentsScreen(
    private val components: List<SuitePackComponent>,
    private val callback: (result: Boolean) -> Unit,
) : Screen(GUI_DOWNLOAD) {

    private val layout by lazy { FrameLayout(0, 0, width, height) }

    private var downloadButton: Button? = null
    private var backButton: Button? = null

    private var currentIndex = 0
    private val currentComponent get() = components.getOrNull(currentIndex)

    private var downloadThread: Thread? = null
    private val isDownloading: Boolean get() = downloadThread != null

    private var isCompleted = false

    override fun init() {
        layout.removeChildren()
        val component = currentComponent ?: return

        val name = Component.literal(component.name).withStyle(component.color).withStyle { style ->
            style.withHoverEvent(
                HoverEvent.ShowText(Component.translatable("achievetodo.suite.dl.open_project"))
            ).withClickEvent(
                ClickEvent.OpenUrl(URI.create(component.pageUrl))
            )
        }
        val title = Component.translatable("achievetodo.suite.dl.title", name).apply {
            if (components.size > 1) {
                append(" (${currentIndex + 1}/${components.size})")
            }
        }
        val titleWidget = FocusableTextWidget.builder(title, font).apply {
            maxWidth(width - 60)
            alwaysShowBorder(false)
            backgroundFill(FocusableTextWidget.BackgroundFill.NEVER)
        }.build().apply {
            setCentered(true)
            setComponentClickHandler { style ->
                val openUrl = style.clickEvent as? ClickEvent.OpenUrl ?: return@setComponentClickHandler
                Blaze3D.openUri(openUrl.uri())
            }
        }
        val description = Component.translatable("achievetodo.suite.${component.id}.description")
        val descriptionWidget = MultiLineTextWidget(description, font).setMaxWidth(width - 60).setCentered(true)
        val content = layout.addChild(LinearLayout.vertical().spacing(8)).apply {
            defaultCellSetting().alignHorizontallyCenter()
            addChild(titleWidget)
            addChild(descriptionWidget)
        }
        val downloadButtonWidget = Button.builder(GUI_DOWNLOAD) {
            if (component.dl.supportsDirect) {
                startDirectDownload(component)
            } else {
                Blaze3D.openUri(URI.create(component.dl.url))
            }
        }.width(140).build().apply {
            setTooltip(
                Tooltip.create(
                    Component.translatable(
                        when (component.dl) {
                            is SuitePackComponent.Download.Modrinth -> "achievetodo.suite.dl.modrinth"
                            is SuitePackComponent.Download.Mediafire -> "achievetodo.suite.dl.mediafire"
                        }
                    )
                )
            )
        }

        content.addChild(LinearLayout.vertical().spacing(8)).apply {
            defaultCellSetting().alignHorizontallyCenter()
            downloadButton = addChild(downloadButtonWidget)
            updateDownloadButtonIdleMessage()
            backButton = addChild(
                Button.builder(CommonComponents.GUI_BACK) { onBackPressed() }.width(140).build()
            )
        }
        val dropNote = Component.translatable("pack.dropInfo").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
        content.addChild(StringWidget(dropNote, font))
        layout.visitWidgets(this::addRenderableWidget)
        repositionElements()
    }

    override fun repositionElements() {

        layout.arrangeElements()
        FrameLayout.centerInRectangle(layout, rectangle)
    }

    override fun shouldCloseOnEsc(): Boolean = false

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.isEscape) {
            onBackPressed()
            return true
        }
        return super.keyPressed(event)
    }

    override fun onClose() {
        callback(isCompleted)
    }

    override fun onFilesDrop(paths: List<Path>) {
        if (isDownloading) return
        val singlePath = paths.singleOrNull() ?: return
        handleFile(singlePath)
    }

    private fun onBackPressed() {
        if (isDownloading) {
            downloadThread?.interrupt()
            downloadThread = null
            resetDownloadUI()
        } else {
            onClose()
        }
    }

    private fun onComponentSuccess() {
        currentIndex++
        if (currentIndex < components.size) {
            rebuildWidgets()
        } else {
            isCompleted = true
            onClose()
        }
    }

    private fun updateDownloadButtonIdleMessage() {
        downloadButton?.message = if (currentComponent?.dl?.supportsDirect == true) {
            GUI_DOWNLOAD
        } else {
            CommonComponents.GUI_OPEN_IN_BROWSER
        }
    }

    private fun resetDownloadUI() {
        downloadThread = null
        downloadButton?.active = true
        updateDownloadButtonIdleMessage()
        backButton?.message = CommonComponents.GUI_BACK
    }

    private fun handleFile(path: Path) {
        val component = currentComponent ?: return
        val download = component.dl
        val sha256 = path.calcSha256OrNull() ?: return
        val isWrapper = download is SuitePackComponent.Download.Mediafire &&
            sha256.equals(download.unzipMeSha256, ignoreCase = true)
        val isDirectMatch = sha256.equals(download.sha256, ignoreCase = true)
        if (!isWrapper && !isDirectMatch) {
            return
        }
        val targetPath = SuitePack.CACHE_DIR / component.fileName
        try {
            SuitePack.CACHE_DIR.createDirectories()
            if (isWrapper) {
                val extracted = extractNestedZip(path, targetPath)
                if (!extracted) {
                    targetPath.deleteIfExists()
                    return
                }
            } else {
                Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING)
            }
            val resultSha256 = targetPath.calcSha256OrNull()
            if (!download.sha256.equals(resultSha256, ignoreCase = true)) {
                targetPath.deleteIfExists()
                return
            }
        } catch (_: Exception) {
            targetPath.deleteIfExists()
            return
        }
        onComponentSuccess()
    }

    private fun startDirectDownload(component: SuitePackComponent) {
        downloadButton?.active = false
        backButton?.message = CommonComponents.GUI_CANCEL

        val targetPath = SuitePack.CACHE_DIR / component.fileName
        val requestedHash = HashCode.fromString(component.dl.sha256.lowercase())

        CompletableFuture.runAsync({
            downloadThread = Thread.currentThread()
            try {
                SuitePack.CACHE_DIR.createDirectories()

                val downloadedHashFile = HttpUtil.downloadFile(
                    SuitePack.CACHE_DIR,
                    URI.create(component.dl.url).toURL(),
                    mapOf("User-Agent" to AchieveToDoMod.USER_AGENT),
                    Hashing.sha256(),
                    requestedHash,
                    0xFA00000,
                    minecraft.proxy,
                    object : HttpUtil.DownloadProgressListener {

                        private var totalBytes = OptionalLong.empty()

                        override fun requestStart() {}

                        override fun downloadStart(size: OptionalLong) {
                            totalBytes = size
                        }

                        override fun downloadedBytes(bytes: Long) {
                            val message = if (totalBytes.isPresent) {
                                Component.translatable(
                                    "download.pack.progress.percent", bytes * 100L / totalBytes.getAsLong()
                                )
                            } else {
                                Component.translatable(
                                    "download.pack.progress.bytes", RealmsUnit.humanReadable(bytes)
                                )
                            }
                            minecraft.execute { downloadButton?.message = message }
                        }

                        override fun requestFinished(success: Boolean) {}
                    }
                )

                if (downloadThread == null) {
                    downloadedHashFile.deleteIfExists()
                    minecraft.execute { resetDownloadUI() }
                    return@runAsync
                }

                Files.move(downloadedHashFile, targetPath, StandardCopyOption.REPLACE_EXISTING)
                minecraft.execute {
                    resetDownloadUI()
                    onComponentSuccess()
                }
            } catch (_: Exception) {
                targetPath.deleteIfExists()
                minecraft.execute {
                    resetDownloadUI()
                }
            } finally {
                downloadThread = null
            }
        }, Util.nonCriticalIoPool())
    }

    companion object {
        private val GUI_DOWNLOAD = Component.translatable("mco.brokenworld.download")
    }
}

private fun extractNestedZip(source: Path, destinationZip: Path): Boolean = runCatching {
    ZipFile(source.toFile()).use { zipFile ->
        val entry = zipFile.entries().asSequence().firstOrNull {
            !it.isDirectory && it.name.endsWith(".zip", ignoreCase = true)
        } ?: return false
        zipFile.getInputStream(entry).use { input ->
            Files.newOutputStream(
                destinationZip,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            ).use { output ->
                input.copyTo(output)
            }
        }
        true
    }
}.isSuccess
