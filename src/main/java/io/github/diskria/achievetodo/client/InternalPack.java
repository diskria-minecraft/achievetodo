package io.github.diskria.achievetodo.client;

import io.github.diskria.achievetodo.AchieveToDoMod;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum InternalPack {

    BACAP_OVERRIDE,
    BACAP_HARDCORE_OVERRIDE,
    BACAP_TERRALITH_OVERRIDE,
    BACAP_AMPLIFIED_NETHER_OVERRIDE,
    BACAP_NULLSCAPE_OVERRIDE,
    BACAP_REWARDS_ITEM,
    BACAP_REWARDS_EXPERIENCE,
    BACAP_REWARDS_TROPHY,
    BACAP_COOPERATIVE_MODE;

    public @NotNull String getDatapackName() {
        return AchieveToDoMod.getIdentifier(getName()).toString();
    }

    private @NotNull String getName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
