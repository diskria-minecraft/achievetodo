package io.github.diskria.achievetodo.injection.extension.main;

import io.github.diskria.achievetodo.abilities.AbilityType;

import java.util.Map;

public interface LevelInfoExtension {
    String achievetodo$getConfigName();

    void achievetodo$setConfigName(String configName);

    Map<AbilityType, Integer> achievetodo$getAbilitiesConfiguration(long seed);
}
