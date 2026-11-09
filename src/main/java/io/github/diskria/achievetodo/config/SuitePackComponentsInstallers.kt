package io.github.diskria.achievetodo.config

import io.github.diskria.achievetodo.client.SuitePackComponent
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component

@Environment(EnvType.CLIENT)
class DownloadSuitePackComponentsScreen(
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
