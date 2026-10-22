package io.github.diskria.achievetodo.injection.mixin.main;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.client.Utils;
import io.github.diskria.achievetodo.injection.extension.main.LevelInfoExtension;
import io.github.diskria.achievetodo.server.Constants;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.server.Main;
import net.minecraft.server.dedicated.ServerPropertiesHandler;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.level.LevelInfo;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Main.class)
public class MainMixin {

    @WrapOperation(
        method = "method_43613(Lcom/mojang/serialization/Dynamic;Ljoptsimple/OptionSet;Ljoptsimple/OptionSpec;Lnet/minecraft/server/dedicated/ServerPropertiesLoader;Ljoptsimple/OptionSpec;Lnet/minecraft/server/SaveLoading$LoadContextSupplierContext;)Lnet/minecraft/server/SaveLoading$LoadContext;",
        at = @At(
            value = "NEW",
            target = "(Ljava/lang/String;Lnet/minecraft/world/GameMode;ZLnet/minecraft/world/Difficulty;ZLnet/minecraft/world/GameRules;Lnet/minecraft/resource/DataConfiguration;)Lnet/minecraft/world/level/LevelInfo;"
        )
    )
    private static LevelInfo readConfigNameFromServerProperties(
        String name,
        GameMode gameMode,
        boolean hardcore,
        Difficulty difficulty,
        boolean allowCommands,
        GameRules gameRules,
        DataConfiguration dataConfiguration,
        @NotNull Operation<LevelInfo> original,
        @Local @NotNull ServerPropertiesHandler serverPropertiesHandler
    ) {
        String configName = serverPropertiesHandler.getString(Constants.ConfigKey.SERVER_CONFIG_PROPERTY_NAME, "");
        if (configName == null || configName.isEmpty()) {
            throw new IllegalStateException(
                "You must set " + Constants.ConfigKey.SERVER_CONFIG_PROPERTY_NAME +
                    " with selected configuration in your `server.properties` file!" +
                    " Check out the `Server-side setup` section in the mod description: " +
                    Utils.buildModrinthModUrl(AchieveToDoMod.ID)
            );
        }
        LevelInfo levelInfo = original.call(
            name, gameMode, hardcore, difficulty, allowCommands, gameRules, dataConfiguration
        );
        if (levelInfo instanceof LevelInfoExtension levelInfoExtension) {
            levelInfoExtension.achievetodo$setConfigName(configName);
        }
        return levelInfo;
    }
}
