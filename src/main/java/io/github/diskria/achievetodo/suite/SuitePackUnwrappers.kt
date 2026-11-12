package io.github.diskria.achievetodo.suite

import com.google.common.collect.ImmutableList
import com.llamalad7.mixinextras.injector.ModifyReturnValue
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.llamalad7.mixinextras.sugar.Cancellable
import com.llamalad7.mixinextras.sugar.Local
import com.mojang.serialization.Dynamic
import io.github.diskria.achievetodo.config.modServerConfig
import io.github.diskria.achievetodo.config.readModServerConfig
import io.github.diskria.achievetodo.config.validateDedicatedModServerConfig
import io.github.diskria.lapis.annotations.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows
import net.minecraft.server.Main
import net.minecraft.server.MinecraftServer
import net.minecraft.server.WorldLoader.*
import net.minecraft.server.WorldStem
import net.minecraft.server.dedicated.DedicatedServerSettings
import net.minecraft.server.packs.PackResources
import net.minecraft.server.packs.repository.PackRepository
import net.minecraft.world.level.LevelSettings
import net.minecraft.world.level.WorldDataConfiguration
import net.minecraft.world.level.storage.LevelDataAndDimensions.WorldDataAndGenSettings
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import java.nio.file.Path
import java.util.function.Consumer
import javax.lang.model.element.Modifier
import kotlin.io.path.createDirectories

@KMixin<PackRepository>(Side.Common)
class SuitePackOpenUnwrapper {

    @Extension
    var suiteComponents: List<SuiteComponent> = emptyList()

    @ModifyReturnValue(method = ["openAllSelected"], at = [At("RETURN")])
    fun unwrapResources(resources: List<PackResources>) = SuitePack.unwrapResources(resources, suiteComponents)
}

@KMixin<MinecraftServer>(Side.Common)
abstract class SuitePackReloadUnwrapper {

    @ModifyReturnValue(method = [$$"lambda$reloadResources$0"], at = [At("RETURN")])
    fun MinecraftServer.unwrapResources(resources: ImmutableList<PackResources>): ImmutableList<PackResources> =
        modServerConfig?.let { config ->
            val requiredComponents = SuitePack.getRequiredComponents(config, isHardcore)
            ImmutableList.copyOf(SuitePack.unwrapResources(resources, requiredComponents))
        } ?: resources
}

@KMixin<CreateWorldScreen>(Side.Client)
abstract class SuitePackWorldCreateInterceptor(@Origin val screen: CreateWorldScreen) {

    private var initialRequiredComponents = emptyList<SuiteComponent>()
    private var dataPacksApplying = false

    @Extension
    fun onModServerConfigChanged() {
        initialRequiredComponents = resolveRequiredComponents()
    }

    @WrapMethod(method = ["onCreate"])
    fun validateComponentsOnCreate(original: Operation<Void>) {
        if (dataPacksApplying) {
            dataPacksApplying = false
            original.call()
            return
        }
        val components = resolveRequiredComponents()
        if (components == initialRequiredComponents) {
            original.call()
            return
        }
        val onRetry = { createWorld() }
        val onExit = { exitToWorldCreatorScreen() }
        if (!SuitePack.validateInstalled(components, onRetry, onExit)) {
            return
        }
        startApplyDataPacks(components)
    }

    private fun startApplyDataPacks(components: List<SuiteComponent>) {
        dataPacksApplying = true
        val tempDataPackRepository = getOrCreateTempPack(screen.uiState.settings.dataConfiguration)?.second ?: return
        tempDataPackRepository.suiteComponents = components
        applyDataPacks(tempDataPackRepository, canShowExperimentalFeaturesWarning = false) {
            exitToWorldCreatorScreen()
            dataPacksApplying = false
        }
    }

    @WrapOperation(
        method = ["tryApplyNewDataPacks"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/worldselection/WorldCreationUiState;tryUpdateDataConfiguration(Lnet/minecraft/world/level/WorldDataConfiguration;)Z"
        )]
    )
    fun forceApplyDataPacks(
        uiState: WorldCreationUiState,
        dataConfiguration: WorldDataConfiguration,
        original: Operation<Boolean>,
    ) = !dataPacksApplying && original.call(uiState, dataConfiguration)

    @WrapOperation(
        method = ["tryApplyNewDataPacks", $$"lambda$applyNewPackConfig$5"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"
        )]
    )
    fun waitApplyDataPacks(gui: Gui, screen: Screen, original: Operation<Void>) {
        original.call(gui, screen)
        finishApplyDataPacks(screen)
    }

    private fun finishApplyDataPacks(finishScreen: Screen) {
        if (!dataPacksApplying) return
        if (finishScreen !== screen) {
            dataPacksApplying = false
            return
        }
        createWorld()
    }

    private fun exitToWorldCreatorScreen() {
        Minecraft.getInstance().gui.setScreen(screen)
    }

    private fun resolveRequiredComponents() = with(screen.uiState) {
        SuitePack.getRequiredComponents(modServerConfig, isHardcore)
    }

    @MappingName("onCreate")
    @KShadow(Modifier.PRIVATE)
    abstract fun createWorld()

    @MappingName("getDataPackSelectionSettings")
    @KShadow(Modifier.PRIVATE)
    abstract fun getOrCreateTempPack(dataConfiguration: WorldDataConfiguration): DfuPair<Path, PackRepository>?

    @MappingName("tryApplyNewDataPacks")
    @KShadow(Modifier.PRIVATE)
    abstract fun applyDataPacks(
        packRepository: PackRepository,
        canShowExperimentalFeaturesWarning: Boolean,
        onAbort: Consumer<WorldDataConfiguration>,
    )

    typealias DfuPair<F, S> = com.mojang.datafixers.util.Pair<F, S>
}

@KMixin<WorldOpenFlows>(Side.Client)
abstract class SuitePackWorldOpenResolvers {

    @WrapOperation(
        method = ["createLevelFromExistingSettings"],
        at = [At(
            value = "INVOKE",
            target = $$"Lnet/minecraft/server/packs/repository/ServerPacksSource;createPackRepository(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;)Lnet/minecraft/server/packs/repository/PackRepository;"
        )]
    )
    fun resolveComponentsOnCreate(
        levelStorage: LevelStorageAccess,
        original: Operation<PackRepository>,
        @Local(argsOnly = true, name = ["worldDataAndGenSettings"]) worldDataAndGenSettings: WorldDataAndGenSettings,
    ): PackRepository = original.call(levelStorage).apply {
        levelStorage.readModServerConfig()?.let { config ->
            val isHardcore = worldDataAndGenSettings.data.isHardcore
            suiteComponents = SuitePack.getRequiredComponents(config, isHardcore)
        }
    }

    @WrapOperation(
        method = ["recreateWorldData"],
        at = [At(
            value = "INVOKE",
            target = $$"Lnet/minecraft/client/gui/screens/worldselection/WorldOpenFlows;loadWorldDataBlocking(Lnet/minecraft/server/WorldLoader$PackConfig;Lnet/minecraft/server/WorldLoader$WorldDataSupplier;Lnet/minecraft/server/WorldLoader$ResultFactory;)Ljava/lang/Object;"
        )]
    )
    fun resolveComponentsOnRecreate(
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
            packRepository.suiteComponents = SuitePack.getRequiredComponents(config, isHardcore)
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
    fun validateAndResolveComponentsOnOpen(
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
            val isHardcore = levelDat.parseDifficultySettings().hardcore
            val components = SuitePack.getRequiredComponents(config, isHardcore)
            val onRetry = { openWorldLoadLevelStem(levelStorage, levelDat, safeMode, onCancel) }
            val onExit = {
                levelStorage.safeClose()
                onCancel.run()
            }
            if (!SuitePack.validateInstalled(components, onRetry, onExit)) {
                ci.cancel()
                return null
            }
            packRepository.suiteComponents = components
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

    private fun Dynamic<*>.parseDifficultySettings(): LevelSettings.DifficultySettings =
        LevelSettings.parse(this, WorldDataConfiguration.DEFAULT).difficultySettings
}

@KMixin<Main>(Side.Common)
class SuitePackDedicatedServerResolver {
    companion object {
        @WrapOperation(
            method = ["main"],
            at = [At(
                value = "INVOKE",
                target = $$"Lnet/minecraft/server/packs/repository/ServerPacksSource;createPackRepository(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;)Lnet/minecraft/server/packs/repository/PackRepository;"
            )]
        )
        fun validateAndResolveComponentsOnStart(
            levelStorage: LevelStorageAccess,
            original: Operation<PackRepository>,
            @Local(name = ["settings"]) settings: DedicatedServerSettings,
            @Cancellable ci: CallbackInfo,
        ): PackRepository? {
            val config = levelStorage.validateDedicatedModServerConfig()
            if (config == null) {
                ci.cancel()
                return null
            }
            val isHardcore = settings.properties.hardcore
            val components = SuitePack.getRequiredComponents(config, isHardcore)
            val missingComponents = SuitePack.filterMissingComponents(components)
            if (missingComponents.isNotEmpty()) {
                SuiteInstallers.sendDedicatedServerReport(missingComponents)
                SuitePack.dir.createDirectories()
                ci.cancel()
                return null
            }
            return original.call(levelStorage).apply { suiteComponents = components }
        }
    }
}
