package io.github.diskria.achievetodo.client.gui;

import io.github.diskria.achievetodo.client.AchieveToDoClient;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum AbilityUnlockedToastType {

    ACTION,
    ITEM,
    FOOD,
    TOOL,
    WEAPON,
    EQUIPMENT,
    BLOCK,
    TRADING,
    PORTAL,
    LANDMARK;

    public @NotNull Text getToastTitle() {
        return AchieveToDoClient.translate("ability_unlocked_toast." + getName())
            .append("!");
    }

    private @NotNull String getName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
