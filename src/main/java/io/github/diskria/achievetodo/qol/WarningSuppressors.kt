package io.github.diskria.achievetodo.qol

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.mojang.serialization.Lifecycle
import io.github.diskria.achievetodo.config.ModServerConfig
import io.github.diskria.achievetodo.config.modServerConfig
import io.github.diskria.achievetodo.config.readModServerConfig
import io.github.diskria.achievetodo.extensions.Op
import io.github.diskria.achievetodo.extensions.invoke
import io.github.diskria.lapis.annotations.KMixin
import io.github.diskria.lapis.annotations.Side
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess
import org.spongepowered.asm.mixin.injection.At

private val SUPPRESS_BACKUP_WARNINGS get() = true
private val SUPPRESS_EXPERIMENTAL_FEATURES_WARNINGS get() = true

@KMixin<CreateWorldScreen>(Side.Client)
class WorldCreateWarningSuppressors {
    @WrapOperation(
        method = ["onCreate"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/worldselection/WorldOpenFlows;confirmWorldCreation(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;Lcom/mojang/serialization/Lifecycle;Ljava/lang/Runnable;Z)V"
        )]
    )
    fun CreateWorldScreen.suppressExperimentalFeaturesWarning(
        minecraft: Minecraft,
        thisScreen: CreateWorldScreen,
        lifecycle: Lifecycle,
        onConfirm: Runnable,
        skipWarn: Boolean,
        op: Op<Unit>,
    ) {
        val finalSkipWarn = skipWarn || uiState.modServerConfig.canSuppress(SUPPRESS_EXPERIMENTAL_FEATURES_WARNINGS)
        op(minecraft, thisScreen, lifecycle, onConfirm, finalSkipWarn)
    }
}

@KMixin<WorldOpenFlows>(Side.Client)
class WorldOpenWarningSuppressors {
    @WrapMethod(method = ["askForBackup"])
    fun suppressBackupWarning(
        levelStorage: LevelStorageAccess,
        oldCustomized: Boolean,
        onProceed: Runnable,
        onCancel: Runnable,
        op: Op<Unit>,
    ) {
        if (levelStorage.readModServerConfig()?.canSuppress(SUPPRESS_BACKUP_WARNINGS) == true) {
            onProceed.run()
        } else {
            op(levelStorage, oldCustomized, onProceed, onCancel)
        }
    }
}

private fun ModServerConfig.canSuppress(wanted: Boolean): Boolean = wanted && modEnabled
