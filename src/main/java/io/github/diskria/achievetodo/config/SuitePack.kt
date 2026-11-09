package io.github.diskria.achievetodo.config

import com.google.common.collect.ImmutableList
import com.llamalad7.mixinextras.injector.ModifyReturnValue
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.llamalad7.mixinextras.sugar.Cancellable
import com.llamalad7.mixinextras.sugar.Local
import com.mojang.serialization.Dynamic
import io.github.diskria.achievetodo.AchieveToDoMod
import io.github.diskria.achievetodo.client.BacCompatiblePack
import io.github.diskria.achievetodo.client.BacCorePack
import io.github.diskria.achievetodo.client.SuitePackComponent
import io.github.diskria.achievetodo.config.ModServerConfigStorage.Companion.CONFIG_PATH
import io.github.diskria.achievetodo.extensions.DfuPair
import io.github.diskria.achievetodo.extensions.calcSha1OrNull
import io.github.diskria.lapis.annotations.*
import net.fabricmc.fabric.api.resource.v1.ResourceLoader
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentUtils
import net.minecraft.server.Main
import net.minecraft.server.MinecraftServer
import net.minecraft.server.WorldLoader.*
import net.minecraft.server.WorldStem
import net.minecraft.server.dedicated.DedicatedServerSettings
import net.minecraft.server.packs.*
import net.minecraft.server.packs.metadata.pack.PackFormat
import net.minecraft.server.packs.repository.Pack
import net.minecraft.server.packs.repository.Pack.ResourcesSupplier
import net.minecraft.server.packs.repository.PackCompatibility
import net.minecraft.server.packs.repository.PackRepository
import net.minecraft.server.packs.repository.PackSource
import net.minecraft.util.Util
import net.minecraft.world.flag.FeatureFlagSet
import net.minecraft.world.level.LevelSettings
import net.minecraft.world.level.WorldDataConfiguration
import net.minecraft.world.level.storage.LevelDataAndDimensions.WorldDataAndGenSettings
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import java.nio.file.Path
import java.util.*
import java.util.function.Consumer
import javax.lang.model.element.Modifier
import kotlin.io.path.div
import kotlin.jvm.optionals.getOrNull

object SuitePack {

    private val ID = AchieveToDoMod.getIdentifier("suite")

    fun register() {
        val mod = FabricLoader.getInstance().getModContainer(AchieveToDoMod.ID).getOrNull() ?: return
        ResourceLoader.registerBuiltinPack(ID, mod, AchieveToDoMod.COLORED_NAME, PackActivationType.ALWAYS_ENABLED)
    }

    fun checkId(id: String): Boolean = id == ID.toString()

    fun getRequiredComponents(config: ModServerConfig, isHardcore: Boolean) = when {
        !config.modEnabled -> emptyList()
        else -> mutableListOf(BacCorePack.MAIN).apply {
            if (isHardcore) add(BacCorePack.HARDCORE)
            if (config.terralithEnabled) {
                add(BacCompatiblePack.TERRALITH.main)
                 add(BacCompatiblePack.TERRALITH.compat)
            }
            if (config.amplifiedNetherEnabled) {
                add(BacCompatiblePack.AMPLIFIED_NETHER.main)
                 add(BacCompatiblePack.AMPLIFIED_NETHER.compat)
            }
            if (config.nullscapeEnabled) {
                add(BacCompatiblePack.NULLSCAPE.main)
                 add(BacCompatiblePack.NULLSCAPE.compat)
            }
        }
    }

    fun filterMissingComponents(components: List<SuitePackComponent>): List<SuitePackComponent> =
        components.filter {
            val cachePack = AchieveToDoMod.datapacksCacheDir / it.fileName
            cachePack.calcSha1OrNull() != it.dl.sha1
        }

    fun unfoldComponents(resources: List<PackResources>, components: List<SuitePackComponent>): List<PackResources> {
        if (components.isEmpty()) return resources
        val suiteIndex = resources.indexOfFirst { checkId(it.packId()) }.takeIf { it >= 0 } ?: return resources
        val missingComponents = filterMissingComponents(components)
        if (missingComponents.isNotEmpty()) {
            error("Missing or corrupted required datapacks: ${missingComponents.joinToString { it.fileName }}")
        }
        val componentResources = components.flatMap { component ->
            val componentPack = Pack.readMetaAndCreate(
                PackLocationInfo(
                    component.datapackName,
                    Component.literal(component.title),
                    PackSource.BUILT_IN,
                    Optional.empty(),
                ),
                FilePackResources.FileResourcesSupplier(AchieveToDoMod.datapacksCacheDir / component.fileName),
                PackType.SERVER_DATA,
                PackSelectionConfig(true, Pack.Position.BOTTOM, false),
            )
            componentPack?.open()?.toList() ?: emptyList()
        }
        return resources.toMutableList().apply { addAll(suiteIndex + 1, componentResources) }
    }
}

@KMixin<CreateWorldScreen>(Side.Client)
abstract class SuitePackCreateWorldChecker(@Origin val screen: CreateWorldScreen) {

    private var isInUnfolding = false
    private var initialComponents = emptyList<SuitePackComponent>()

    @Extension
    fun updateInitialSuitePackComponents() {
        initialComponents = SuitePack.getRequiredComponents(screen.uiState.modServerConfig, screen.uiState.isHardcore)
    }

    private fun unfoldSuitePack(uiState: WorldCreationUiState, components: List<SuitePackComponent>) {
        isInUnfolding = true
        val packRepository = getOrCreatePackSettings(uiState.settings.dataConfiguration)?.second ?: return
        packRepository.suitePackComponents = components
        applyPackChanges(packRepository, canShowExperimentalFeaturesWarning = false) {
            exitToCreateWorldScreen()
            isInUnfolding = false
        }
    }

    private fun finalizeSuitePackUnfolding(finishScreen: Screen) {
        if (!isInUnfolding) return
        if (finishScreen !== screen) {
            isInUnfolding = false
            return
        }
        createWorld()
    }

    private fun exitToCreateWorldScreen() {
        Minecraft.getInstance().gui.setScreen(screen)
    }

    @WrapMethod(method = ["onCreate"])
    fun CreateWorldScreen.resolveSuitePackUnfolding(original: Operation<Void>) {
        if (isInUnfolding) {
            isInUnfolding = false
            original.call()
            return
        }
        val components = SuitePack.getRequiredComponents(uiState.modServerConfig, uiState.isHardcore)
        if (components == initialComponents) {
            original.call()
            return
        }
        val missingComponents = SuitePack.filterMissingComponents(components)
        if (missingComponents.isNotEmpty()) {
            Minecraft.getInstance().gui.setScreen(
                DownloadSuitePackComponentsScreen(
                    components = missingComponents,
                    onComplete = ::createWorld,
                    onCancel = ::exitToCreateWorldScreen,
                )
            )
            return
        }
        unfoldSuitePack(uiState, components)
    }

    @WrapOperation(
        method = ["tryApplyNewDataPacks"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/worldselection/WorldCreationUiState;tryUpdateDataConfiguration(Lnet/minecraft/world/level/WorldDataConfiguration;)Z"
        )]
    )
    fun skipUnchangedPackIdsCheck(
        uiState: WorldCreationUiState,
        newConfig: WorldDataConfiguration,
        original: Operation<Boolean>,
    ) = !isInUnfolding && original.call(uiState, newConfig)

    @WrapOperation(
        method = ["tryApplyNewDataPacks", $$"lambda$applyNewPackConfig$5"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"
        )]
    )
    fun waitPacksApplyResult(gui: Gui, screen: Screen, original: Operation<Void>) {
        original.call(gui, screen)
        finalizeSuitePackUnfolding(screen)
    }

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

@KMixin<Pack>(Side.Common)
class SuitePackCustomizer {
    companion object {
        @WrapMethod(method = [$$"readPackMetadata(Lnet/minecraft/server/packs/PackLocationInfo;Lnet/minecraft/server/packs/repository/Pack$ResourcesSupplier;Lnet/minecraft/server/packs/metadata/pack/PackFormat;Lnet/minecraft/server/packs/PackType;)Lnet/minecraft/server/packs/repository/Pack$Metadata;"])
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

@KMixin<PackRepository>(Side.Common)
class SuitePackOnOpenUnfolder {

    @Extension
    var suitePackComponents: List<SuitePackComponent> = emptyList()

    @ModifyReturnValue(method = ["openAllSelected"], at = [At("RETURN")])
    fun unfoldSuitePackOnOpen(resources: List<PackResources>): List<PackResources> =
        SuitePack.unfoldComponents(resources, suitePackComponents)
}

@KMixin<MinecraftServer>(Side.Common)
class SuitePackOnReloadUnfolder {
    @ModifyReturnValue(method = [$$"lambda$reloadResources$0"], at = [At("RETURN")])
    fun MinecraftServer.unfoldSuitePackOnReload(resources: ImmutableList<PackResources>): ImmutableList<PackResources> =
        levelStorage.readModServerConfig()
            ?.let { config ->
                val requiredComponents = SuitePack.getRequiredComponents(config, isHardcore)
                ImmutableList.copyOf(SuitePack.unfoldComponents(resources, requiredComponents))
            }
            ?: resources
}

@KMixin<WorldOpenFlows>(Side.Client)
abstract class SuitePackIntegratedServerSync {

    @WrapOperation(
        method = ["createLevelFromExistingSettings"],
        at = [At(
            value = "INVOKE",
            target = $$"Lnet/minecraft/server/packs/repository/ServerPacksSource;createPackRepository(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;)Lnet/minecraft/server/packs/repository/PackRepository;"
        )]
    )
    fun syncSuitePackOnCreate(
        levelStorage: LevelStorageAccess,
        original: Operation<PackRepository>,
        @Local(argsOnly = true, name = ["worldDataAndGenSettings"]) worldDataAndGenSettings: WorldDataAndGenSettings,
    ): PackRepository = original.call(levelStorage).apply {
        levelStorage.readModServerConfig()?.let { config ->
            suitePackComponents = SuitePack.getRequiredComponents(config, worldDataAndGenSettings.data.isHardcore)
        }
    }

    @WrapOperation(
        method = ["recreateWorldData"],
        at = [At(
            value = "INVOKE",
            target = $$"Lnet/minecraft/client/gui/screens/worldselection/WorldOpenFlows;loadWorldDataBlocking(Lnet/minecraft/server/WorldLoader$PackConfig;Lnet/minecraft/server/WorldLoader$WorldDataSupplier;Lnet/minecraft/server/WorldLoader$ResultFactory;)Ljava/lang/Object;"
        )]
    )
    fun syncSuitePackOnRecreate(
        worldOpenFlows: WorldOpenFlows,
        packConfig: PackConfig,
        worldDataGetter: WorldDataSupplier<*>,
        worldDataSupplier: ResultFactory<*, *>,
        original: Operation<*>,
        @Local(argsOnly = true, name = ["levelSourceAccess"]) levelStorage: LevelStorageAccess,
        @Local(name = ["levelDataTag"]) levelDat: Dynamic<*>,
        @Local(name = ["packRepository"]) packRepository: PackRepository,
    ): Any {
        levelStorage.readModServerConfig()?.let { config ->
            val isHardcore = levelDat.parseDifficultySettings().hardcore
            packRepository.suitePackComponents = SuitePack.getRequiredComponents(config, isHardcore)
        }
        return original.call(worldOpenFlows, packConfig, worldDataGetter, worldDataSupplier)
    }

    @WrapOperation(
        method = ["openWorldLoadLevelStem"],
        at = [At(
            value = "INVOKE",
            target = $$"Lnet/minecraft/client/gui/screens/worldselection/WorldOpenFlows;loadWorldStem(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;Lcom/mojang/serialization/Dynamic;ZLnet/minecraft/server/packs/repository/PackRepository;)Lnet/minecraft/server/WorldStem;"
        )]
    )
    fun syncSuitePackOnLoad(
        worldOpenFlows: WorldOpenFlows,
        levelStorage: LevelStorageAccess,
        levelDat: Dynamic<*>,
        safeMode: Boolean,
        packRepository: PackRepository,
        original: Operation<WorldStem>,
        @Local(argsOnly = true, name = ["onCancel"]) onCancel: Runnable,
        @Cancellable ci: CallbackInfo,
    ): WorldStem? {
        levelStorage.readModServerConfig()?.let { config ->
            val components = SuitePack.getRequiredComponents(config, levelDat.parseDifficultySettings().hardcore)
            val missingComponents = SuitePack.filterMissingComponents(components)
            if (missingComponents.isNotEmpty()) {
                Minecraft.getInstance().gui.setScreen(
                    DownloadSuitePackComponentsScreen(
                        components = missingComponents,
                        onComplete = { openWorldLoadLevelStem(levelStorage, levelDat, safeMode, onCancel) },
                        onCancel = {
                            levelStorage.safeClose()
                            onCancel.run()
                        },
                    )
                )
                ci.cancel()
                return null
            }
            packRepository.suitePackComponents = components
        }
        return original.call(worldOpenFlows, levelStorage, levelDat, safeMode, packRepository)
    }

    @KShadow(Modifier.PRIVATE)
    abstract fun openWorldLoadLevelStem(
        levelStorage: LevelStorageAccess,
        levelDat: Dynamic<*>,
        safeMode: Boolean,
        onCancel: Runnable,
    )
}

@KMixin<Main>(Side.Common)
class SuitePackDedicatedServerSync {
    companion object {
        @WrapOperation(
            method = ["main"],
            at = [At(
                value = "INVOKE",
                target = $$"Lnet/minecraft/server/packs/repository/ServerPacksSource;createPackRepository(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;)Lnet/minecraft/server/packs/repository/PackRepository;"
            )]
        )
        fun syncSuitePackOnStart(
            levelStorage: LevelStorageAccess,
            original: Operation<PackRepository>,
            @Local(name = ["settings"]) settings: DedicatedServerSettings,
            @Cancellable ci: CallbackInfo,
        ): PackRepository? {
            val config = levelStorage.readModServerConfig()
            if (config == null || !config.acknowledged) {
                if (config == null) {
                    levelStorage.saveModServerConfig(ModServerConfig.DEDICATED_DEFAULT)
                }
                AchieveToDoMod.logger.error(
                    """
                    Server config is not acknowledged!
                    Please review the config and change "acknowledged" from false to true.
                    
                    Config file: <world_folder>/$CONFIG_PATH
                    """.trimIndent()
                )
                ci.cancel()
                return null
            }
            val components = SuitePack.getRequiredComponents(config, settings.properties.hardcore)
            val missingComponents = SuitePack.filterMissingComponents(components)
            if (missingComponents.isNotEmpty()) {
                logInvalidComponents(missingComponents)
                ci.cancel()
                return null
            }
            return original.call(levelStorage).apply { suitePackComponents = components }
        }

        private fun logInvalidComponents(components: List<SuitePackComponent>) {
            val cachePath = AchieveToDoMod.DATAPACKS_CACHE_PATH
            val curl = if (Util.getPlatform() == Util.OS.WINDOWS) "curl.exe" else "curl"
            AchieveToDoMod.logger.error(buildString {
                appendLine("Missing or corrupted required datapacks!")
                appendLine()
                components.forEachIndexed { index, pack ->
                    appendLine("${index + 1}. [${pack.title}] -> ${pack.fileName} (SHA-1:${pack.dl.sha1})")
                    when (val dl = pack.dl) {
                        is SuitePackComponent.Download.Modrinth -> appendLine(
                            """
                            Run in console to download with correct name:
                            $curl -A "${AchieveToDoMod.USER_AGENT}" --create-dirs -Lo "$cachePath/${pack.fileName}" "${dl.url}"
                            """.trimIndent()
                        )

                        is SuitePackComponent.Download.Mediafire -> appendLine(
                            """
                            Open browser and download file (AD): ${dl.url}
                            Unzip it and rename the inner file to: ${pack.fileName}
                            """.trimIndent()
                        )
                    }
                    appendLine()
                }
                appendLine("All files must be saved to: <server_folder>/$cachePath")
                append("Restart the server once all files are in place.")
            })
        }
    }
}

private fun Dynamic<*>.parseDifficultySettings(): LevelSettings.DifficultySettings =
    LevelSettings.parse(this, WorldDataConfiguration.DEFAULT).difficultySettings
