package io.github.diskria.achievetodo.injection.extension.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface WorldCreatorExtension {
    String achievetodo$getConfigName();

    boolean achievetodo$isItemRewardsEnabled();

    boolean achievetodo$isExperienceRewardsEnabled();

    boolean achievetodo$isTrophyRewardsEnabled();

    boolean achievetodo$isTerralithEnabled();

    boolean achievetodo$isAmplifiedNetherEnabled();

    boolean achievetodo$isNullscapeEnabled();

    boolean achievetodo$isCooperativeModeEnabled();

    void achievetodo$setConfigName(String configName);

    void achievetodo$setItemRewardsEnabled(boolean isItemRewardsEnabled);

    void achievetodo$setExperienceRewardsEnabled(boolean isExperienceRewardsEnabled);

    void achievetodo$setTrophyRewardsEnabled(boolean isTrophyRewardsEnabled);

    void achievetodo$setTerralithEnabled(boolean isTerralithEnabled);

    void achievetodo$setAmplifiedNetherEnabled(boolean isAmplifiedNetherEnabled);

    void achievetodo$setNullscapeEnabled(boolean isNullscapeEnabled);

    void achievetodo$setCooperativeModeEnabled(boolean isCooperativeModeEnabled);
}
