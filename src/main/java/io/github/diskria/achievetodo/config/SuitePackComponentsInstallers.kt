package io.github.diskria.achievetodo.config

import io.github.diskria.achievetodo.AchieveToDoMod
import io.github.diskria.achievetodo.client.SuitePackComponent
import io.github.diskria.achievetodo.server.AchieveToDoServerException
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component

object SuitePackComponentsInstallers {

    @Environment(EnvType.CLIENT)
    class DownloadScreen(
        private val components: List<SuitePackComponent>,
        private val onComplete: () -> Unit,
        private val onCancel: () -> Unit,
    ) : Screen(Component.literal("Download")) {

        override fun init() {
            super.init()
            addRenderableWidget(
                Button.builder(CommonComponents.GUI_DONE) { onComplete() }
                    .bounds(width / 2 - 155 + 80, 124, 150, 20).build()
            )
            addRenderableWidget(
                Button.builder(CommonComponents.GUI_CANCEL) { onCancel() }
                    .bounds(width / 2 - 155 + 80, 164, 150, 20).build()
            )
        }
    }

    fun failDedicatedServerWithInstallationGuide(components: List<SuitePackComponent>): Nothing {
        val cachePath = AchieveToDoMod.DATAPACKS_CACHE_PATH
        val isWindows = System.getProperty("os.name").contains("win", ignoreCase = true)
        val curlCmd = if (isWindows) "curl.exe" else "curl"
        throw AchieveToDoServerException(buildString {
            appendLine("Missing or corrupted required datapacks!")
            appendLine("All files below must be placed into: <server_folder>/$cachePath")
            appendLine()
            components.forEachIndexed { index, pack ->
                appendLine("${index + 1}. [ ${pack.title} ]")
                appendLine("  Target File : ${pack.fileName}")
                appendLine("  Download    : ${pack.download.url}")
                appendLine("  SHA-1       : ${pack.download.sha1}")
                when (val dl = pack.download) {
                    is SuitePackComponent.Download.Mediafire -> {
                        appendLine("  This is an [UNZIP_ME] archive.")
                        appendLine("    1) Download the archive manually from Mediafire.")
                        appendLine("    2) Extract it on your computer.")
                        appendLine("    3) Rename the inner datapack to: ${pack.fileName}")
                        appendLine("    4) Place '${pack.fileName}' into: $cachePath")
                    }

                    is SuitePackComponent.Download.Modrinth -> {
                        appendLine("  Direct download:")
                        appendLine("    $curlCmd --create-dirs -Lo $cachePath/${pack.fileName} \"${dl.url}\"")
                    }
                }
                appendLine()
            }
            appendLine("After placing all files, restart the server.")
        })
    }
}
