@file:OptIn(ExperimentalSerializationApi::class)

package io.github.diskria.achievetodo

import com.google.common.collect.ImmutableList
import com.llamalad7.mixinextras.injector.ModifyReturnValue
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.llamalad7.mixinextras.sugar.Cancellable
import com.llamalad7.mixinextras.sugar.Local
import com.mojang.serialization.Dynamic
import com.mojang.serialization.Lifecycle
import io.github.diskria.achievetodo.client.AchieveToDoClient
import io.github.diskria.achievetodo.client.BacCompatiblePack
import io.github.diskria.achievetodo.client.BacCorePack
import io.github.diskria.achievetodo.client.PackDependency
import io.github.diskria.achievetodo.extensions.DfuPair
import io.github.diskria.achievetodo.extensions.calcSha1OrNull
import io.github.diskria.achievetodo.server.AchieveToDoServerException
import io.github.diskria.lapis.annotations.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.resource.v1.ResourceLoader
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.components.tabs.GridLayoutTab
import net.minecraft.client.gui.components.tabs.MenuTabBar
import net.minecraft.client.gui.components.tabs.Tab
import net.minecraft.client.gui.layouts.GridLayout
import net.minecraft.client.gui.layouts.SpacerElement
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList.WorldListEntry
import net.minecraft.core.LayeredRegistryAccess
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentUtils
import net.minecraft.server.Main
import net.minecraft.server.MinecraftServer
import net.minecraft.server.ReloadableServerResources
import net.minecraft.server.WorldLoader.PackConfig
import net.minecraft.server.dedicated.DedicatedServerSettings
import net.minecraft.server.packs.*
import net.minecraft.server.packs.metadata.pack.PackFormat
import net.minecraft.server.packs.repository.*
import net.minecraft.server.packs.repository.Pack.ResourcesSupplier
import net.minecraft.world.flag.FeatureFlagSet
import net.minecraft.world.level.LevelSettings
import net.minecraft.world.level.WorldDataConfiguration
import net.minecraft.world.level.storage.LevelDataAndDimensions.WorldDataAndGenSettings
import net.minecraft.world.level.storage.LevelStorageSource
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.nio.file.Path
import java.util.*
import java.util.function.Consumer
import javax.lang.model.element.Modifier
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import kotlin.jvm.optionals.getOrNull

object ServerPacksManager {
    fun registerHooks() {
        ServerLifecycleEvents.SERVER_STARTING.register { server ->
            AchieveToDoMod.LOGGER.info("Loading server config...")
            val config = server.modServerConfig
            if (config == null) {
                AchieveToDoMod.LOGGER.warn("  Config not found. Skipping...")
                return@register
            }
            if (!config.modEnabled) {
                AchieveToDoMod.LOGGER.warn("  Disabled via config. Exiting...")
                return@register
            }
            AchieveToDoMod.LOGGER.info("  Done! $config")
        }
    }
}

@Serializable
data class ModServerConfig(val acknowledged: Boolean, val modEnabled: Boolean, val itemRewards: Boolean) {
    companion object {
        val DEDICATED_DEFAULT = ModServerConfig(
            acknowledged = false,
            modEnabled = true,
            itemRewards = false,
        )
        val INTEGRATED_DEFAULT = DEDICATED_DEFAULT.copy(acknowledged = true)
    }
}

@Environment(EnvType.CLIENT)
private class ModServerConfigTab(private val state: WorldCreationUiState) : GridLayoutTab(AchieveToDoMod.COLORED_NAME) {

    private var config: ModServerConfig
        get() = state.modServerConfig
        set(value) {
            state.modServerConfig = value
        }

    init {
        val row = layout.columnSpacing(10).rowSpacing(8).createRowHelper(2)
        val modToggle = row.addChild(
            CycleButton
                .onOffBuilder(config.modEnabled)
                .create(0, 0, 150, 20, Component.literal(AchieveToDoMod.NAME)) { _, value ->
                    config = config.copy(modEnabled = value)
                }
        )
        val itemRewardsToggle = row.addChild(
            CycleButton
                .onOffBuilder(config.itemRewards)
                .withTooltip { Tooltip.create(ITEM_REWARDS_INFO) }
                .create(0, 0, 150, 20, ITEM_REWARDS) { _, value ->
                    config = config.copy(itemRewards = value)
                }
        )
        state.addListener {
            modToggle.value = config.modEnabled
            itemRewardsToggle.value = config.itemRewards
        }
    }

    companion object {
        private val ITEM_REWARDS = AchieveToDoClient.translate("world_creation_tab.rewards.items")
        private val ITEM_REWARDS_INFO = AchieveToDoClient.translate("world_creation_tab.rewards.items.tooltip")
    }
}

@KMixin<CreateWorldScreen>(Side.Client)
abstract class ModServerConfigTabInjector(@Origin val createWorldScreen: CreateWorldScreen) {

    private var isCustomWorldCreation = false
    private var skipExperimentalFeaturesWarning = false

    @Extension
    fun setModServerConfigFromRecreate(config: ModServerConfig) {
        viewState.modServerConfig = config.copy(acknowledged = true)
    }

    private fun postponeWorldCreation() {
        val packRepository = getOrCreatePackSettings(viewState.settings.dataConfiguration)?.second ?: return
        isCustomWorldCreation = true
        with(packRepository) {
            modServerConfig = viewState.modServerConfig
            isHardcore = viewState.isHardcore
        }
        AchieveToDoMod.LOGGER.info("  Apply packs to trigger unfold...")
        applyPackChanges(packRepository, canShowExperimentalFeaturesWarning = false) {
            isCustomWorldCreation = false
            Minecraft.getInstance().gui.setScreen(createWorldScreen)
        }
    }

    private fun finalizeWorldCreation(finishScreen: Screen) {
        if (!isCustomWorldCreation) return
        if (finishScreen !== createWorldScreen) {
            AchieveToDoMod.LOGGER.error("Finished with unexcepted screen: ${finishScreen::class}")
            isCustomWorldCreation = false
            return
        }
        AchieveToDoMod.LOGGER.info("  Finished, calling world creating again...")
        createWorld()
    }

    @WrapOperation(
        method = ["init"],
        at = [At(
            value = "INVOKE",
            target = $$"Lnet/minecraft/client/gui/components/tabs/MenuTabBar$Builder;addTabs([Lnet/minecraft/client/gui/components/tabs/Tab;)Lnet/minecraft/client/gui/components/tabs/MenuTabBar$Builder;"
        )]
    )
    fun injectTab(
        barBuilder: MenuTabBar.Builder,
        tabs: Array<Tab>,
        original: Operation<MenuTabBar.Builder>,
    ): MenuTabBar.Builder {
        val modifiedTabs = tabs + ModServerConfigTab(viewState)
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
    fun saveConfig(
        worldOpenFlows: WorldOpenFlows,
        levelStorage: LevelStorageAccess,
        serverResources: ReloadableServerResources,
        registry: LayeredRegistryAccess<*>,
        worldDataAndGenSettings: WorldDataAndGenSettings,
        gameRules: Optional<*>,
        original: Operation<*>,
        @Cancellable cir: CallbackInfoReturnable<Boolean>,
    ) {
        if (levelStorage.saveModServerConfig(viewState.modServerConfig)) {
            original.call(worldOpenFlows, levelStorage, serverResources, registry, worldDataAndGenSettings, gameRules)
        } else {
            cir.returnValue = false
        }
    }

    @WrapMethod(method = ["onCreate"])
    fun resolveDataPacks(original: Operation<Void>) {
        if (isCustomWorldCreation) {
            AchieveToDoMod.LOGGER.info("World creating started!")
            isCustomWorldCreation = false
            skipExperimentalFeaturesWarning = true
            try {
                original.call()
            } finally {
                skipExperimentalFeaturesWarning = false
            }
            return
        }
        AchieveToDoMod.LOGGER.info("World creating...")
        AchieveToDoMod.LOGGER.info("  Config: ${viewState.modServerConfig}")
        AchieveToDoMod.LOGGER.info("  Hardcore: ${viewState.isHardcore}")
        val requiredPacks = SuitePack.getRequiredPacks(viewState.modServerConfig, viewState.isHardcore).ifEmpty {
            AchieveToDoMod.LOGGER.info("Config is disabled. Skipping...")
            original.call()
            return
        }
        val cacheDir = AchieveToDoClient.configDir / ".datapacks_cache"
        val missingPacks = requiredPacks.filter {
            val cachePack = cacheDir / it.fileName
            cachePack.calcSha1OrNull() != it.download.sha1
        }
        if (missingPacks.isNotEmpty()) {
            AchieveToDoMod.LOGGER.error("Missing packs: ${missingPacks.joinToString { it.id }}")
            return
        }
        postponeWorldCreation()
    }

    @WrapOperation(
        method = ["tryApplyNewDataPacks"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/worldselection/WorldCreationUiState;tryUpdateDataConfiguration(Lnet/minecraft/world/level/WorldDataConfiguration;)Z"
        )]
    )
    fun skipUnchangedPackIdsCheck(
        viewState: WorldCreationUiState,
        newConfig: WorldDataConfiguration,
        original: Operation<Boolean>,
    ) = !isCustomWorldCreation && original.call(viewState, newConfig)

    @WrapOperation(
        method = ["tryApplyNewDataPacks", $$"lambda$applyNewPackConfig$5"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"
        )]
    )
    fun waitPacksApplyResult(gui: Gui, screen: Screen, original: Operation<Void>) {
        original.call(gui, screen)
        finalizeWorldCreation(screen)
    }

    @WrapOperation(
        method = ["onCreate"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/worldselection/WorldOpenFlows;confirmWorldCreation(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;Lcom/mojang/serialization/Lifecycle;Ljava/lang/Runnable;Z)V"
        )]
    )
    fun skipExperimentalFeaturesWarning(
        minecraft: Minecraft,
        screen: CreateWorldScreen,
        lifecycle: Lifecycle,
        onConfirm: Runnable,
        skipWarning: Boolean,
        original: Operation<Void>,
    ) {
        original.call(minecraft, screen, lifecycle, onConfirm, skipExperimentalFeaturesWarning || skipWarning)
    }

    @MappingName("uiState")
    @KShadow(Modifier.PRIVATE, Modifier.FINAL)
    abstract val viewState: WorldCreationUiState

    @MappingName("onCreate")
    @KShadow(Modifier.PRIVATE)
    abstract fun createWorld()

    @MappingName("getDataPackSelectionSettings")
    @KShadow(Modifier.PRIVATE)
    abstract fun getOrCreatePackSettings(dataConfiguration: WorldDataConfiguration): DfuPair<Path, PackRepository>?

    @MappingName("tryApplyNewDataPacks")
    @KShadow(Modifier.PRIVATE)
    abstract fun applyPackChanges(
        packRepository: PackRepository,
        canShowExperimentalFeaturesWarning: Boolean,
        onAbort: Consumer<WorldDataConfiguration>,
    )
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
        setModServerConfigFromRecreate(
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

    private var currentConfig: ModServerConfig? = null

    @Extension
    fun readModServerConfig(): ModServerConfig? {
        if (currentConfig == null) {
            currentConfig = runCatching {
                getConfigPath().inputStream().use { Json.decodeFromStream<ModServerConfig>(it) }
            }.getOrNull()
        }
        return currentConfig
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
            currentConfig = config
        }
        return isSaved
    }

    private fun getConfigPath() = levelStorage.levelDirectory.path / CONFIG_PATH

    companion object {
        const val CONFIG_PATH = "config/${AchieveToDoMod.ID}/server.json"
        private val configJson = Json { prettyPrint = true }
    }
}

object SuitePack {

    private val ID = AchieveToDoMod.getIdentifier("suite")

    fun register() {
        val mod = FabricLoader.getInstance().getModContainer(AchieveToDoMod.ID).getOrNull() ?: return
        ResourceLoader.registerBuiltinPack(ID, mod, AchieveToDoMod.COLORED_NAME, PackActivationType.ALWAYS_ENABLED)
    }

    fun checkId(id: String): Boolean = id == ID.toString()

    fun getRequiredPacks(config: ModServerConfig, isHardcore: Boolean): List<PackDependency> = when {
        !config.modEnabled -> emptyList()
        else -> mutableListOf(BacCorePack.MAIN).apply {
            if (isHardcore) add(BacCorePack.HARDCORE)
        }
    }

    fun unfold(resources: List<PackResources>, config: ModServerConfig, isHardcore: Boolean): List<PackResources> {
        val suiteIndex = resources.indexOfFirst { checkId(it.packId()) }.takeIf { it >= 0 } ?: return resources
        AchieveToDoMod.LOGGER.info("Unfolding suite pack...")
        AchieveToDoMod.LOGGER.info("  Config: $config")
        AchieveToDoMod.LOGGER.info("  Hardcore: $isHardcore")
        val requiredPacks = getRequiredPacks(config, isHardcore).ifEmpty {
            AchieveToDoMod.LOGGER.info("Config is disabled. Skipping...")
            return resources
        }
        val cacheDir = AchieveToDoClient.configDir / ".datapacks_cache"
        val missingPacks = requiredPacks.filter { pack ->
            val cachePack = cacheDir / pack.fileName
            cachePack.calcSha1OrNull() != pack.download.sha1
        }
        if (missingPacks.isNotEmpty()) {
            error("Missing packs: ${missingPacks.joinToString { it.id }}")
        }
        val components = requiredPacks.flatMap { pack ->
            val componentPack = Pack.readMetaAndCreate(
                PackLocationInfo(
                    pack.datapackName,
                    Component.literal(pack.title),
                    PackSource.BUILT_IN,
                    Optional.empty(),
                ),
                FilePackResources.FileResourcesSupplier(cacheDir / pack.fileName),
                PackType.SERVER_DATA,
                PackSelectionConfig(true, Pack.Position.BOTTOM, false),
            )
            componentPack?.open()?.toList() ?: emptyList()
        }
        val unfolded = resources.toMutableList().apply {
            addAll(suiteIndex + 1, components)
        }
        val from = resources.joinToString { it.packId() }
        val to = unfolded.joinToString { it.packId() }
        AchieveToDoMod.LOGGER.info("Suite pack unfolded: [$from] -> [$to]", Throwable())
        return unfolded
    }
}

@KMixin<Pack>(Side.Common)
class SuitePackCustomizer {
    companion object {
        @WrapMethod(method = ["readPackMetadata"])
        fun hardcodeSuitePackMetadata(
            location: PackLocationInfo,
            resources: ResourcesSupplier,
            currentPackVersion: PackFormat,
            type: PackType,
            original: Operation<Pack.Metadata>,
        ): Pack.Metadata {
            if (!SuitePack.checkId(location.id)) {
                return original.call(location, resources, currentPackVersion, type)
            }
            val supportedList = listOf(
                Component.translatable("selectWorld.gameMode.hardcore").withStyle(BacCorePack.HARDCORE.color),
            ) + BacCompatiblePack.ALL.map {
                Component.literal(it.main.title).withStyle(it.main.color)
            }
            val description = Component.translatable(
                "achievetodo.suite.description",
                ComponentUtils.formatList(supportedList, ComponentUtils.DEFAULT_NO_STYLE_SEPARATOR)
            )
            return Pack.Metadata(description, PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), emptyList())
        }
    }
}

@KMixin<Main>(Side.Common)
class DedicatedServerPackRepositoryContext {
    companion object {
        @WrapOperation(
            method = ["main"],
            at = [At(
                value = "INVOKE",
                target = $$"Lnet/minecraft/server/packs/repository/ServerPacksSource;createPackRepository(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;)Lnet/minecraft/server/packs/repository/PackRepository;"
            )]
        )
        fun withModServerConfig(levelStorage: LevelStorageAccess, original: Operation<PackRepository>): PackRepository {
            val config = levelStorage.readModServerConfig()
            if (config == null || !config.acknowledged) {
                if (config == null) {
                    levelStorage.saveModServerConfig(ModServerConfig.DEDICATED_DEFAULT)
                }
                throw AchieveToDoServerException(
                    """
                    Server config is NOT acknowledged!
                    Please review the config and change "acknowledged" from false to true.
                    
                    Config file: <world_folder>/${ModServerConfigStorage.CONFIG_PATH}
                    """.trimIndent()
                )
            }
            return original.call(levelStorage).apply { modServerConfig = config }
        }

        @WrapOperation(
            method = ["main"],
            at = [At(
                value = "INVOKE",
                target = $$"Lnet/minecraft/server/packs/repository/ServerPacksSource;createPackRepository(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;)Lnet/minecraft/server/packs/repository/PackRepository;"
            )]
        )
        fun withHardcore(
            levelStorage: LevelStorageAccess,
            original: Operation<PackRepository>,
            @Local(name = ["settings"]) serverSettings: DedicatedServerSettings,
        ): PackRepository = original.call(levelStorage).apply { isHardcore = serverSettings.properties.hardcore }
    }
}

@KMixin<LevelStorageSource>(Side.Common)
class IntegratedServerPackRepositoryContext {
    companion object {
        @WrapMethod(method = ["getPackConfig"])
        fun withHardcore(
            input: Dynamic<*>,
            packRepository: PackRepository,
            safeMode: Boolean,
            original: Operation<PackConfig>,
        ): PackConfig = original.call(
            input, packRepository.apply { isHardcore = input.parseDifficultySettings().hardcore }, safeMode,
        )

        private fun Dynamic<*>.parseDifficultySettings(): LevelSettings.DifficultySettings =
            LevelSettings.parse(this, WorldDataConfiguration.DEFAULT).difficultySettings
    }
}

@KMixin<ServerPacksSource>(Side.Common)
class ServerPackRepositoryContext {
    companion object {
        @WrapMethod(method = [$$"createPackRepository(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;)Lnet/minecraft/server/packs/repository/PackRepository;"])
        fun withModServerConfig(levelStorage: LevelStorageAccess, original: Operation<PackRepository>): PackRepository =
            original.call(levelStorage).apply { modServerConfig = levelStorage.readModServerConfig() }
    }
}

@KMixin<WorldOpenFlows>(Side.Client)
class CreateWorldPackRepositoryContext {
    companion object {
        @WrapOperation(
            method = ["createLevelFromExistingSettings"],
            at = [At(
                value = "INVOKE",
                target = $$"Lnet/minecraft/server/packs/repository/ServerPacksSource;createPackRepository(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;)Lnet/minecraft/server/packs/repository/PackRepository;"
            )]
        )
        fun withHardcore(
            levelStorage: LevelStorageAccess,
            original: Operation<PackRepository>,
            @Local(argsOnly = true, name = ["worldDataAndGenSettings"]) worldDataHolder: WorldDataAndGenSettings,
        ): PackRepository = original.call(levelStorage).apply { isHardcore = worldDataHolder.data.isHardcore }
    }
}

@KMixin<PackRepository>(Side.Common)
class SuitePackOnOpenUnfolder {

    @Extension
    var modServerConfig: ModServerConfig? = null

    @Extension
    var isHardcore: Boolean? = null

    @ModifyReturnValue(method = ["openAllSelected"], at = [At("RETURN")])
    fun unfoldSuitePackOnOpen(original: List<PackResources>): List<PackResources> {
        val config = modServerConfig ?: return original.also {
            AchieveToDoMod.LOGGER.info("Config not set. Skipping unfolding...", Throwable())
        }
        val isHardcore = isHardcore ?: error("isHardcore must be set when config is set!")
        return SuitePack.unfold(original, config, isHardcore)
    }
}

@KMixin<MinecraftServer>(Side.Common)
abstract class SuitePackOnReloadUnfolder {

    @Extension
    val modServerConfig: ModServerConfig? get() = levelStorage.readModServerConfig()

    @ModifyReturnValue(method = [$$"lambda$reloadResources$0"], at = [At("RETURN")])
    fun MinecraftServer.unfoldSuitePackOnReload(original: ImmutableList<PackResources>): ImmutableList<PackResources> =
        modServerConfig?.let { ImmutableList.copyOf(SuitePack.unfold(original, it, isHardcore)) } ?: original

    @MappingName("storageSource")
    @KShadow(Modifier.PROTECTED, Modifier.FINAL)
    abstract val levelStorage: LevelStorageAccess
}
