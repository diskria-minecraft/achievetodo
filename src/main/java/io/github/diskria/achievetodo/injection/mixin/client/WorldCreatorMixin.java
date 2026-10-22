package io.github.diskria.achievetodo.injection.mixin.client;

import io.github.diskria.achievetodo.ability.ProgressionModeType;
import io.github.diskria.achievetodo.injection.extension.client.WorldCreatorExtension;
import net.minecraft.client.gui.screen.world.WorldCreator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(WorldCreator.class)
public abstract class WorldCreatorMixin implements WorldCreatorExtension {

    @Unique
    private String configName = ProgressionModeType.getDefaultMode().getName();

    @Unique
    private boolean isItemRewardsEnabled;

    @Unique
    private boolean isExperienceRewardsEnabled;

    @Unique
    private boolean isTrophyRewardsEnabled = true;

    @Unique
    private boolean isTerralithEnabled;

    @Unique
    private boolean isAmplifiedNetherEnabled;

    @Unique
    private boolean isNullscapeEnabled;

    @Unique
    private boolean isCooperativeModeEnabled = true;

    @Override
    public String achievetodo$getConfigName() {
        return configName;
    }

    @Override
    public boolean achievetodo$isItemRewardsEnabled() {
        return isItemRewardsEnabled;
    }

    @Override
    public boolean achievetodo$isExperienceRewardsEnabled() {
        return isExperienceRewardsEnabled;
    }

    @Override
    public boolean achievetodo$isTrophyRewardsEnabled() {
        return isTrophyRewardsEnabled;
    }

    @Override
    public boolean achievetodo$isTerralithEnabled() {
        return isTerralithEnabled;
    }

    @Override
    public boolean achievetodo$isAmplifiedNetherEnabled() {
        return isAmplifiedNetherEnabled;
    }

    @Override
    public boolean achievetodo$isNullscapeEnabled() {
        return isNullscapeEnabled;
    }

    @Override
    public boolean achievetodo$isCooperativeModeEnabled() {
        return isCooperativeModeEnabled;
    }

    @Override
    public void achievetodo$setConfigName(String configName) {
        this.configName = configName;
        update();
    }

    @Override
    public void achievetodo$setItemRewardsEnabled(boolean isItemRewardsEnabled) {
        this.isItemRewardsEnabled = isItemRewardsEnabled;
    }

    @Override
    public void achievetodo$setExperienceRewardsEnabled(boolean isExperienceRewardsEnabled) {
        this.isExperienceRewardsEnabled = isExperienceRewardsEnabled;
    }

    @Override
    public void achievetodo$setTrophyRewardsEnabled(boolean isTrophyRewardsEnabled) {
        this.isTrophyRewardsEnabled = isTrophyRewardsEnabled;
    }

    @Override
    public void achievetodo$setTerralithEnabled(boolean isTerralithEnabled) {
        this.isTerralithEnabled = isTerralithEnabled;
    }

    @Override
    public void achievetodo$setAmplifiedNetherEnabled(boolean isAmplifiedNetherEnabled) {
        this.isAmplifiedNetherEnabled = isAmplifiedNetherEnabled;
    }

    @Override
    public void achievetodo$setNullscapeEnabled(boolean isNullscapeEnabled) {
        this.isNullscapeEnabled = isNullscapeEnabled;
    }

    @Override
    public void achievetodo$setCooperativeModeEnabled(boolean isCooperativeModeEnabled) {
        this.isCooperativeModeEnabled = isCooperativeModeEnabled;
    }

    @Shadow
    public abstract void update();
}
