package io.github.diskria.achievetodo.ability.generation;

import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.AbilityType;
import io.github.diskria.achievetodo.client.AchieveToDoClient;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.advancement.AdvancementRewards;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.advancement.criterion.ImpossibleCriterion;
import net.minecraft.advancement.criterion.TickCriterion;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AbilityAdvancementsGenerator extends FabricAdvancementProvider {

    public static final String DEMYSTIFIED_CRITERION = "demystified";
    public static final String UNLOCKED_CRITERION = "unlocked";

    public static final String ABILITY_PATH_PREFIX = "abilities/";

    public static final Block TAB_BACKGROUND = Blocks.PALE_MOSS_BLOCK;

    protected AbilityAdvancementsGenerator(
        FabricDataOutput output,
        CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup
    ) {
        super(output, registryLookup);
    }

    public static @NotNull Identifier buildAdvancementId(@NotNull AbilityType ability) {
        return buildAdvancementId(ability.getName());
    }

    private static @NotNull Identifier buildAdvancementId(String suffix) {
        return AchieveToDoMod.getIdentifier(ABILITY_PATH_PREFIX + suffix);
    }

    @Override
    public void generateAdvancement(
        RegistryWrapper.WrapperLookup registryLookup,
        @NotNull Consumer<AdvancementEntry> consumer
    ) {
        AdvancementEntry rootAdvancement = Advancement.Builder
            .createUntelemetered()
            .display(
                Items.BARRIER,
                Text.literal(AchieveToDoMod.NAME),
                AchieveToDoClient.translate("description"),
                Identifier.ofVanilla("textures/block/" + Registries.BLOCK.getId(TAB_BACKGROUND).getPath() + ".png"),
                AdvancementFrame.TASK,
                false,
                false,
                false
            )
            .criterion("tick", TickCriterion.Conditions.createTick())
            .build(buildAdvancementId("root"));
        consumer.accept(rootAdvancement);

        for (AbilityType abilityType : AbilityType.values()) {
            Identifier advancementId = buildAdvancementId(abilityType);
            consumer.accept(Advancement.Builder
                .createUntelemetered()
                .parent(rootAdvancement)
                .display(
                    abilityType.getIcon(),
                    abilityType.getTitle(),
                    abilityType.getDescription(),
                    null,
                    AdvancementFrame.TASK,
                    true,
                    false,
                    false
                )
                .rewards(AdvancementRewards.Builder.function(advancementId))
                .criterion(
                    DEMYSTIFIED_CRITERION,
                    Criteria.IMPOSSIBLE.create(new ImpossibleCriterion.Conditions())
                )
                .criterion(
                    UNLOCKED_CRITERION,
                    Criteria.IMPOSSIBLE.create(new ImpossibleCriterion.Conditions())
                )
                .build(advancementId)
            );
        }
    }
}
