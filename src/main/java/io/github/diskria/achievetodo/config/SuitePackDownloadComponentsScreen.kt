package io.github.diskria.achievetodo.config

import io.github.diskria.achievetodo.client.SuitePackComponent
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component

@Environment(EnvType.CLIENT)
class SuitePackDownloadComponentsScreen(
    private val components: List<SuitePackComponent>,
    private val callback: (result: Boolean) -> Unit,
) : Screen(Component.literal("Download")) {

    override fun init() {
        super.init()
        addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE) { callback(true) }
                .bounds(width / 2 - 155 + 80, 124, 150, 20).build()
        )
        addRenderableWidget(
            Button.builder(CommonComponents.GUI_CANCEL) { callback(false) }
                .bounds(width / 2 - 155 + 80, 164, 150, 20).build()
        )
    }
}
