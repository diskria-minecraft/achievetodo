package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.injection.extension.main.LevelInfoExtension;
import io.github.diskria.achievetodo.server.Constants;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelProperties.class)
public class LevelPropertiesMixin {

    @Shadow
    private LevelInfo levelInfo;

    @Inject(
        method = "updateProperties",
        at = @At("RETURN")
    )
    private void saveConfigName(
        DynamicRegistryManager registryManager,
        NbtCompound levelNbt,
        NbtCompound playerNbt,
        CallbackInfo ci
    ) {
        if (levelInfo instanceof LevelInfoExtension levelInfoExtension) {
            String configName = levelInfoExtension.achievetodo$getConfigName();
            if (configName != null) {
                levelNbt.putString(Constants.NbtKey.LEVEL_CONFIG_NAME, configName);
            }
        }
    }
}
