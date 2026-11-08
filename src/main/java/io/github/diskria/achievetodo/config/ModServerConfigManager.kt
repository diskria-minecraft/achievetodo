@file:OptIn(ExperimentalSerializationApi::class)

package io.github.diskria.achievetodo.config

import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.llamalad7.mixinextras.sugar.Cancellable
import com.llamalad7.mixinextras.sugar.Local
import io.github.diskria.achievetodo.AchieveToDoMod
import io.github.diskria.achievetodo.server.AchieveToDoServerException
import io.github.diskria.lapis.annotations.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.tabs.GridLayoutTab
import net.minecraft.client.gui.components.tabs.MenuTabBar
import net.minecraft.client.gui.components.tabs.Tab
import net.minecraft.client.gui.layouts.GridLayout
import net.minecraft.client.gui.layouts.SpacerElement
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList.WorldListEntry
import net.minecraft.core.LayeredRegistryAccess
import net.minecraft.server.MinecraftServer
import net.minecraft.server.ReloadableServerResources
import net.minecraft.world.level.LevelSettings
import net.minecraft.world.level.storage.LevelDataAndDimensions.WorldDataAndGenSettings
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.nio.file.Path
import java.util.*
import javax.lang.model.element.Modifier
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

object ModServerConfigManager {
    fun registerHooks() {
        ServerLifecycleEvents.SERVER_STARTING.register { server ->
            AchieveToDoMod.logger.info("Loading server config...")
            val config = server.levelStorage.readModServerConfig()
            if (config == null) {
                AchieveToDoMod.logger.warn("  Config not found. Skipping...")
                return@register
            }
            if (!config.modEnabled) {
                AchieveToDoMod.logger.warn("  Disabled via config. Exiting...")
                return@register
            }
            AchieveToDoMod.logger.info("Done! $config")
        }
    }
}

@Serializable
data class ModServerConfig(
    val acknowledged: Boolean,
    val modEnabled: Boolean,
    val itemRewards: Boolean,
    val terralithEnabled: Boolean,
    val amplifiedNetherEnabled: Boolean,
    val nullscapeEnabled: Boolean,
) {
    companion object {
        val DEDICATED_DEFAULT = ModServerConfig(
            acknowledged = false,
            modEnabled = true,
            itemRewards = false,
            terralithEnabled = false,
            amplifiedNetherEnabled = false,
            nullscapeEnabled = false,
        )
        val INTEGRATED_DEFAULT = DEDICATED_DEFAULT.copy(acknowledged = true)
    }
}

@KMixin<CreateWorldScreen>(Side.Client)
abstract class ModServerConfigTabInjector(@Origin val createWorldScreen: CreateWorldScreen) {

    @Extension
    fun setModServerConfig(config: ModServerConfig) {
        createWorldScreen.uiState.modServerConfig = config.copy(acknowledged = true)
        createWorldScreen.updateInitialSuitePackComponents()
    }

    @WrapOperation(
        method = ["init"],
        at = [At(
            value = "INVOKE",
            target = $$"Lnet/minecraft/client/gui/components/tabs/MenuTabBar$Builder;addTabs([Lnet/minecraft/client/gui/components/tabs/Tab;)Lnet/minecraft/client/gui/components/tabs/MenuTabBar$Builder;"
        )]
    )
    fun CreateWorldScreen.injectModServerConfigTab(
        barBuilder: MenuTabBar.Builder,
        tabs: Array<Tab>,
        original: Operation<MenuTabBar.Builder>,
    ): MenuTabBar.Builder {
        val modifiedTabs = tabs + ModServerConfigTab(uiState)
        GridLayoutsHeightFix.apply(modifiedTabs.mapNotNull { (it as? GridLayoutTab)?.layout as? GridLayout })
        return original.call(barBuilder, modifiedTabs)
    }

    @WrapOperation(
        method = ["createNewWorld"],
        at = [At(
            value = "INVOKE",
            target = $$"Lnet/minecraft/client/gui/screens/worldselection/WorldOpenFlows;createLevelFromExistingSettings(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;Lnet/minecraft/server/ReloadableServerResources;Lnet/minecraft/core/LayeredRegistryAccess;Lnet/minecraft/world/level/storage/LevelDataAndDimensions$WorldDataAndGenSettings;Ljava/util/Optional;)V"
        )]
    )
    fun CreateWorldScreen.saveModServerConfig(
        worldOpenFlows: WorldOpenFlows,
        levelStorage: LevelStorageAccess,
        serverResources: ReloadableServerResources,
        registry: LayeredRegistryAccess<*>,
        worldDataAndGenSettings: WorldDataAndGenSettings,
        gameRules: Optional<*>,
        original: Operation<*>,
        @Cancellable cir: CallbackInfoReturnable<Boolean>,
    ) {
        if (levelStorage.saveModServerConfig(uiState.modServerConfig)) {
            original.call(worldOpenFlows, levelStorage, serverResources, registry, worldDataAndGenSettings, gameRules)
        } else {
            cir.returnValue = false
        }
    }
}

@KMixin<WorldListEntry>(Side.Client)
class ModServerConfigBridgeOnRecreateWorld {

    @WrapOperation(
        method = ["recreateWorld"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;createFromExisting(Lnet/minecraft/client/Minecraft;Ljava/lang/Runnable;Lnet/minecraft/world/level/LevelSettings;Lnet/minecraft/client/gui/screens/worldselection/WorldCreationContext;Ljava/nio/file/Path;)Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;"
        )]
    )
    fun copyConfig(
        minecraft: Minecraft,
        onClose: Runnable,
        levelSettings: LevelSettings,
        creationContext: WorldCreationContext,
        tempDataPackDir: Path?,
        original: Operation<CreateWorldScreen>,
        @Local(name = ["access"]) levelStorage: LevelStorageAccess,
    ): CreateWorldScreen = original.call(minecraft, onClose, levelSettings, creationContext, tempDataPackDir).apply {
        setModServerConfig(
            levelStorage.readModServerConfig() ?: ModServerConfig.INTEGRATED_DEFAULT.copy(modEnabled = false)
        )
    }
}

@KMixin<GridLayout>(Side.Client)
abstract class GridLayoutsHeightFix(@Origin val layout: GridLayout) {

    private var lastOccupiedRow: Int = 0

    @Extension
    fun calculateHeight(): Int = layout.apply { arrangeElements() }.height

    @Extension
    fun padHeightTo(target: Int) {
        val current = layout.height
        val diff = target - current
        val spacerHeight = diff - rowSpacing
        if (spacerHeight <= 0) return
        layout.addChild(
            /* child = */ SpacerElement.height(spacerHeight),
            /* row = */ lastOccupiedRow + 1,
            /* column = */ 0,
            /* rows = */ 1,
            /* columns = */ 1,
        )
    }

    @Inject(method = ["arrangeElements"], at = [At("TAIL")])
    fun updateLastOccupiedRow(ci: CallbackInfo, @Local(name = ["maxRow"]) maxRow: Int) {
        lastOccupiedRow = maxRow
    }

    @KShadow(Modifier.PRIVATE)
    abstract val rowSpacing: Int

    companion object {
        fun apply(layouts: List<GridLayout>) {
            val maxHeight = layouts.maxOfOrNull { it.calculateHeight() } ?: return
            layouts.forEach { it.padHeightTo(maxHeight) }
        }
    }
}

@KMixin<WorldCreationUiState>(Side.Client)
class ModServerConfigViewState(@Origin val state: WorldCreationUiState) {
    @Extension
    var modServerConfig: ModServerConfig = ModServerConfig.INTEGRATED_DEFAULT
        set(value) {
            if (field == value) return
            field = value
            state.onChanged()
        }
}

@KMixin<LevelStorageAccess>(Side.Common)
class ModServerConfigStorage(@Origin val levelStorage: LevelStorageAccess) {

    private var config: ModServerConfig? = null

    @Extension
    fun readModServerConfig(): ModServerConfig? {
        if (config == null) {
            config = runCatching {
                getConfigPath().inputStream().use { Json.decodeFromStream<ModServerConfig>(it) }
            }.getOrNull()
        }
        return config
    }

    @Extension
    fun requireAcknowledgedModServerConfig(): ModServerConfig {
        val config = readModServerConfig()
        if (config == null || !config.acknowledged) {
            if (config == null) {
                saveModServerConfig(ModServerConfig.DEDICATED_DEFAULT)
            }
            throw AchieveToDoServerException(
                """
                Server config is not acknowledged!
                Please review the config and change "acknowledged" from false to true.
                
                Config file: <world_folder>/$CONFIG_PATH
                """.trimIndent()
            )
        }
        return config
    }

    @Extension
    fun saveModServerConfig(config: ModServerConfig): Boolean {
        val isSaved = runCatching {
            with(getConfigPath()) {
                parent.createDirectories()
                outputStream().use { configJson.encodeToStream(config, it) }
            }
        }.isSuccess
        if (isSaved) {
            this@ModServerConfigStorage.config = config
        }
        return isSaved
    }

    private fun getConfigPath() = levelStorage.levelDirectory.path / CONFIG_PATH

    companion object {
        const val CONFIG_PATH = "config/${AchieveToDoMod.ID}/server.json"
        private val configJson = Json { prettyPrint = true }
    }
}

@KMixin<MinecraftServer>(Side.Common)
abstract class ModServerConfigServerShortcut {

    @Extension
    val levelStorage: LevelStorageAccess get() = storageSource

    @MappingName("storageSource")
    @KShadow(Modifier.PROTECTED, Modifier.FINAL)
    abstract val storageSource: LevelStorageAccess
}
