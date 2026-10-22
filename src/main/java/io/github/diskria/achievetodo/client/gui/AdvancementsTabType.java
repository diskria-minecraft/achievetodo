package io.github.diskria.achievetodo.client.gui;

import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.client.AchieveToDoClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.gui.screen.advancement.AdvancementTabType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum AdvancementsTabType {

    ABILITIES,
    BACAP,
    STATISTICS,

    BUILDING,
    FARMING,
    HUSBANDRY,
    BIOMES,
    ADVENTURE,
    MONSTERS,
    WEAPONRY,

    MINING,
    REDSTONE,
    ENCHANTING,
    NETHER,
    POTION,
    END,
    CHALLENGES;

    public static @Nullable AdvancementsTabType findByAdvancement(@NotNull PlacedAdvancement advancement) {
        return findByAdvancement(advancement.getAdvancementEntry().id());
    }

    public static @Nullable AdvancementsTabType findByAdvancement(@NotNull Identifier advancementId) {
        String[] pathSlices = advancementId.getPath().split("/");
        if (pathSlices.length == 2) {
            for (AdvancementsTabType tabType : values()) {
                if (tabType.name().equalsIgnoreCase(pathSlices[0])) {
                    return tabType;
                }
            }
        }
        return null;
    }

    public @NotNull Identifier getMystifiedTabId() {
        return AchieveToDoMod.getIdentifier("locked_tab_" + getName() + "/root");
    }

    public @NotNull Text getMystifiedTabTooltipText() {
        return AchieveToDoClient.translate("advancements_tab_mystified_tooltip." + getName())
            .formatted(Formatting.ITALIC)
            .formatted(Formatting.GRAY);
    }

    public @NotNull String getName() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Environment(EnvType.CLIENT)
    public AdvancementTabType getPosition() {
        if (this == ABILITIES || this == BACAP || this == STATISTICS) {
            return AdvancementTabType.LEFT;
        }
        if (this == BUILDING ||
            this == FARMING ||
            this == HUSBANDRY ||
            this == BIOMES ||
            this == ADVENTURE ||
            this == MONSTERS ||
            this == WEAPONRY
        ) {
            return AdvancementTabType.ABOVE;
        }
        return AdvancementTabType.BELOW;
    }

    @Environment(EnvType.CLIENT)
    public int getOrder() {
        int order = 0;
        AdvancementTabType position = getPosition();
        for (AdvancementsTabType tab : AdvancementsTabType.values()) {
            if (tab.getPosition() == position) {
                if (tab == this) {
                    break;
                }
                order++;
            }
        }
        return order;
    }
}
