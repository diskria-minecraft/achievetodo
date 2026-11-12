package io.github.diskria.achievetodo.suite

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import io.github.diskria.achievetodo.AchieveToDoMod
import io.github.diskria.achievetodo.config.ModServerConfig
import io.github.diskria.achievetodo.extensions.calcSha256OrNull
import io.github.diskria.achievetodo.extensions.capitalized
import io.github.diskria.achievetodo.extensions.createPackMetadata
import io.github.diskria.lapis.annotations.KMixin
import io.github.diskria.lapis.annotations.Side
import net.fabricmc.fabric.api.resource.v1.ResourceLoader
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentUtils
import net.minecraft.server.packs.PackLocationInfo
import net.minecraft.server.packs.PackResources
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.metadata.pack.PackFormat
import net.minecraft.server.packs.repository.Pack
import kotlin.io.path.div
import kotlin.jvm.optionals.getOrNull

object SuitePack {

    const val NAME = "suite"
    const val PATH = ".${AchieveToDoMod.ID}/$NAME"

    val id = AchieveToDoMod.getIdentifier(NAME)
    val coloredName = AchieveToDoMod.COLORED_NAME.copy().append(" ").append(NAME.capitalized())
    val dir by lazy { FabricLoader.getInstance().gameDir / PATH }

    fun register() {
        val modContainer = FabricLoader.getInstance().getModContainer(AchieveToDoMod.ID).getOrNull() ?: return
        ResourceLoader.registerBuiltinPack(id, modContainer, coloredName, PackActivationType.ALWAYS_ENABLED)
    }

    fun buildDescription(): Component {
        val supportedList = listOf(
            Component.translatable("selectWorld.gameMode.hardcore").withStyle(BacCorePack.HARDCORE.color),
        ) + BacCompatiblePack.ALL.map {
            Component.literal(it.main.name).withStyle(it.main.color)
        }
        return Component.translatable(
            "achievetodo.suite.description",
            ComponentUtils.formatList(supportedList, ComponentUtils.DEFAULT_NO_STYLE_SEPARATOR)
        )
    }

    fun checkPack(packId: String): Boolean = packId == id.toString()

    fun getRequiredComponents(config: ModServerConfig, isHardcore: Boolean) = when {
        !config.modEnabled -> emptyList()
        else -> mutableListOf(BacCorePack.MAIN).apply {
            if (isHardcore) add(BacCorePack.HARDCORE)
            if (config.terralithEnabled) {
                add(BacCompatiblePack.TERRALITH.main)
            }
            if (config.amplifiedNetherEnabled) {
                add(BacCompatiblePack.AMPLIFIED_NETHER.main)
            }
            if (config.nullscapeEnabled) {
                add(BacCompatiblePack.NULLSCAPE.main)
            }
        }
    }

    fun filterMissingComponents(components: List<SuiteComponent>): List<SuiteComponent> =
        components.filter { it.path.calcSha256OrNull() != it.download.sha256 }

    fun isInstalled(components: List<SuiteComponent>): Boolean =
        filterMissingComponents(components).isEmpty()

    fun validateInstalled(components: List<SuiteComponent>, onDone: () -> Unit, onCancel: () -> Unit): Boolean {
        if (isInstalled(components)) {
            return true
        }
        SuiteInstallers.openScreen(components) { result ->
            if (result) onDone()
            else onCancel()
        }
        return false
    }

    fun unwrapResources(resources: List<PackResources>, components: List<SuiteComponent>): List<PackResources> {
        if (components.isEmpty()) return resources
        val suitePackIndex = resources.indexOfFirst { checkPack(it.packId()) }.takeIf { it >= 0 } ?: return resources
        val missingComponents = filterMissingComponents(components)
        if (missingComponents.isNotEmpty()) {
            error("Missing or corrupted required data packs: ${missingComponents.joinToString { it.fileName }}")
        }
        val componentPackResources = components.flatMap { it.installedPack()?.open()?.toList() ?: emptyList() }
        return resources.toMutableList().apply { addAll(suitePackIndex + 1, componentPackResources) }
    }
}

@KMixin<Pack>(Side.Common)
class SuitePackCustomizer {
    companion object {
        @WrapMethod(method = [$$"readPackMetadata(Lnet/minecraft/server/packs/PackLocationInfo;Lnet/minecraft/server/packs/repository/Pack$ResourcesSupplier;Lnet/minecraft/server/packs/metadata/pack/PackFormat;Lnet/minecraft/server/packs/PackType;)Lnet/minecraft/server/packs/repository/Pack$Metadata;"])
        fun overrideMetadata(
            location: PackLocationInfo,
            resources: Pack.ResourcesSupplier,
            currentPackVersion: PackFormat,
            type: PackType,
            original: Operation<Pack.Metadata>,
        ): Pack.Metadata =
            if (SuitePack.checkPack(location.id)) createPackMetadata(SuitePack.buildDescription())
            else original.call(location, resources, currentPackVersion, type)
    }
}
