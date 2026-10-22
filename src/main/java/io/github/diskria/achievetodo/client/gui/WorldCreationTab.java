package io.github.diskria.achievetodo.client.gui;

import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.ProgressionModeType;
import io.github.diskria.achievetodo.client.AchieveToDoClient;
import io.github.diskria.achievetodo.injection.extension.client.WorldCreatorExtension;
import io.github.diskria.achievetodo.server.Constants;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.WorldCreator;
import net.minecraft.client.gui.screen.world.WorldScreenOptionGrid;
import net.minecraft.client.gui.tab.GridScreenTab;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class WorldCreationTab extends GridScreenTab {

    private static final Identifier CONTAINER_BACKGROUND_TEXTURE =
        Identifier.ofVanilla("textures/gui/menu_list_background.png");

    private CyclingButtonWidget<ProgressionConfig> configSelector;

    private WorldScreenOptionGrid rewardsSection;
    private WorldScreenOptionGrid customGenerationSection;

    private GridWidget rewardsContainer;
    private GridWidget customGenerationContainer;

    public WorldCreationTab(CreateWorldScreen screen) {
        super(Text.literal(AchieveToDoMod.NAME));
        if (screen == null || screen.client == null) {
            return;
        }
        WorldCreator worldCreator = screen.getWorldCreator();
        WorldCreatorExtension worldCreatorExtension = (WorldCreatorExtension) worldCreator;

        grid.getMainPositioner().alignHorizontalCenter();

        GridWidget.Adder rootContainer = grid.setColumnSpacing(10).setRowSpacing(8).createAdder(2);

        List<ProgressionConfig> progressionConfigs = new ArrayList<>();
        ProgressionConfig defaultProgressionConfig = null;
        for (ProgressionModeType progressionModeType : ProgressionModeType.values()) {
            ProgressionConfig progressionConfig = ProgressionConfig.fromProgressionMode(progressionModeType);
            progressionConfigs.add(progressionConfig);
            if (progressionModeType == ProgressionModeType.getDefaultMode()) {
                defaultProgressionConfig = progressionConfig;
            }
        }
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve(AchieveToDoMod.ID);
        if (Files.exists(configDir)) {
            try (Stream<Path> stream = Files.list(configDir)) {
                stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(Constants.FileExtension.TOML))
                    .map(path -> StringUtils.removeEnd(path.getFileName().toString(), Constants.FileExtension.TOML))
                    .filter(fileName -> !fileName.startsWith(ProgressionModeType.CHAOS.getName() + "_") &&
                        ProgressionModeType.findByName(fileName) == null
                    )
                    .forEach(fileName -> progressionConfigs.add(ProgressionConfig.fromCustomName(fileName)));
            } catch (IOException ignored) {
            }
        }

        configSelector = CyclingButtonWidget
            .builder(ProgressionConfig::getDisplayedText)
            .values(progressionConfigs)
            .build(
                0, 0, 150, 20,
                Text.translatable("options.difficulty"),
                (button, progressionConfig) -> worldCreatorExtension.achievetodo$setConfigName(progressionConfig.getConfigName())
            );
        configSelector.setValue(defaultProgressionConfig);
        for (ProgressionConfig progressionConfig : progressionConfigs) {
            if (progressionConfig.getConfigName().equals(worldCreatorExtension.achievetodo$getConfigName())) {
                configSelector.setValue(progressionConfig);
                break;
            }
        }
        updateConfigSelectorTooltip();
        rootContainer.add(configSelector, grid.copyPositioner().marginTop(2));

        CyclingButtonWidget<Boolean> cooperativeModeButton = CyclingButtonWidget
            .onOffBuilder()
            .tooltip(value ->
                Tooltip.of(AchieveToDoClient.translate("world_creation_tab.cooperative_mode.tooltip"))
            )
            .build(
                0, 0, 150, 20,
                AchieveToDoClient.translate("world_creation_tab.cooperative_mode"),
                (button, value) -> worldCreatorExtension.achievetodo$setCooperativeModeEnabled(value)
            );
        cooperativeModeButton.setValue(worldCreatorExtension.achievetodo$isCooperativeModeEnabled());
        rootContainer.add(cooperativeModeButton, grid.copyPositioner().marginTop(2));

        GridWidget.Adder rewardsTitleContainer = new GridWidget().createAdder(1);
        rewardsTitleContainer.add(new TextWidget(
            AchieveToDoClient.translate("world_creation_tab.rewards.title")
                .formatted(DesignCodePalette.TEXT_COLOR),
            screen.client.textRenderer
        ));
        WorldScreenOptionGrid.Builder rewardsSectionBuilder = WorldScreenOptionGrid.builder(130);
        rewardsSectionBuilder.add(
            AchieveToDoClient.translate("world_creation_tab.rewards.items"),
            worldCreatorExtension::achievetodo$isItemRewardsEnabled,
            worldCreatorExtension::achievetodo$setItemRewardsEnabled
        ).tooltip(AchieveToDoClient.translate("world_creation_tab.rewards.items.tooltip"));
        rewardsSectionBuilder.add(
            AchieveToDoClient.translate("world_creation_tab.rewards.experience"),
            worldCreatorExtension::achievetodo$isExperienceRewardsEnabled,
            worldCreatorExtension::achievetodo$setExperienceRewardsEnabled
        ).tooltip(AchieveToDoClient.translate("world_creation_tab.rewards.experience.tooltip"));
        rewardsSectionBuilder.add(
            AchieveToDoClient.translate("world_creation_tab.rewards.trophy"),
            worldCreatorExtension::achievetodo$isTrophyRewardsEnabled,
            worldCreatorExtension::achievetodo$setTrophyRewardsEnabled
        ).tooltip(AchieveToDoClient.translate("world_creation_tab.rewards.trophy.tooltip"));
        rewardsContainer = new GridWidget();
        rewardsContainer.add(rewardsTitleContainer.getGridWidget(), 0, 0, grid.copyPositioner());
        rewardsSection = rewardsSectionBuilder.build();
        rewardsContainer.add(rewardsSection.getLayout(), 0, 0, grid.copyPositioner().marginTop(14));
        rootContainer.add(rewardsContainer, 1, grid.copyPositioner().marginTop(14));

        GridWidget.Adder customGenerationTitleContainer = new GridWidget().createAdder(1);
        customGenerationTitleContainer.add(new TextWidget(
            AchieveToDoClient.translate("world_creation_tab.generation.title")
                .formatted(DesignCodePalette.TEXT_COLOR),
            screen.client.textRenderer
        ));
        WorldScreenOptionGrid.Builder customGenerationSectionBuilder = WorldScreenOptionGrid.builder(130);
        customGenerationSectionBuilder.add(
            AchieveToDoClient.translate("world_creation_tab.generation.overworld"),
            worldCreatorExtension::achievetodo$isTerralithEnabled,
            worldCreatorExtension::achievetodo$setTerralithEnabled
        ).tooltip(AchieveToDoClient.translate("world_creation_tab.generation.overworld.tooltip"));
        customGenerationSectionBuilder.add(
            AchieveToDoClient.translate("world_creation_tab.generation.nether"),
            worldCreatorExtension::achievetodo$isAmplifiedNetherEnabled,
            worldCreatorExtension::achievetodo$setAmplifiedNetherEnabled
        ).tooltip(AchieveToDoClient.translate("world_creation_tab.generation.nether.tooltip"));
        customGenerationSectionBuilder.add(
            AchieveToDoClient.translate("world_creation_tab.generation.end"),
            worldCreatorExtension::achievetodo$isNullscapeEnabled,
            worldCreatorExtension::achievetodo$setNullscapeEnabled
        ).tooltip(AchieveToDoClient.translate("world_creation_tab.generation.end.tooltip"));
        customGenerationContainer = new GridWidget();
        customGenerationContainer.add(customGenerationTitleContainer.getGridWidget(), 0, 0, grid.copyPositioner());
        customGenerationSection = customGenerationSectionBuilder.build();
        customGenerationContainer.add(customGenerationSection.getLayout(), 0, 0, grid.copyPositioner().marginTop(14));
        rootContainer.add(customGenerationContainer, 1, grid.copyPositioner().marginTop(14));

        worldCreator.addListener(creator -> {
            rewardsSection.refresh();
            customGenerationSection.refresh();
            updateConfigSelectorTooltip();
        });
        grid.refreshPositions();
    }

    public void render(DrawContext context) {
        renderContainerBackground(context, rewardsContainer);
        renderContainerBackground(context, customGenerationContainer);
    }

    private void updateConfigSelectorTooltip() {
        configSelector.setTooltip(Tooltip.of(configSelector.getValue().getTooltipText()));
    }

    private void renderContainerBackground(DrawContext context, GridWidget container) {
        if (container == null) {
            return;
        }
        int padding = 9;
        int lineHeight = 2;
        int textureSize = 32;
        int x = container.getX() - padding;
        int y = container.getY() - padding;
        int width = container.getWidth() + padding * 2;
        int height = container.getHeight() + padding * 2;
        context.drawTexture(
            RenderLayer::getGuiTextured,
            Screen.HEADER_SEPARATOR_TEXTURE,
            x,
            y - lineHeight,
            0.0f,
            0.0f,
            width,
            lineHeight,
            textureSize,
            lineHeight
        );
        context.drawTexture(
            RenderLayer::getGuiTextured,
            CONTAINER_BACKGROUND_TEXTURE,
            x,
            y,
            x + width,
            y + height,
            width,
            height,
            textureSize,
            textureSize
        );
        context.drawTexture(
            RenderLayer::getGuiTextured,
            Screen.FOOTER_SEPARATOR_TEXTURE,
            x,
            y + height,
            0.0f,
            0.0f,
            width,
            lineHeight,
            textureSize,
            lineHeight
        );
    }

    private record ProgressionConfig(ProgressionModeType builtInMode, String customName) {

        public static @NotNull WorldCreationTab.ProgressionConfig fromProgressionMode(ProgressionModeType type) {
            return new ProgressionConfig(type, null);
        }

        public static @NotNull WorldCreationTab.ProgressionConfig fromCustomName(String customName) {
            return new ProgressionConfig(null, customName);
        }

        public Text getDisplayedText() {
            return builtInMode != null ? builtInMode.getDisplayedText() : Text.literal(customName);
        }

        public Text getTooltipText() {
            return builtInMode != null ? builtInMode.getTooltipText()
                : AchieveToDoClient.translate("world_creation_tab.progression.custom.tooltip");
        }

        public String getConfigName() {
            return builtInMode != null ? builtInMode.getName() : customName;
        }
    }
}
